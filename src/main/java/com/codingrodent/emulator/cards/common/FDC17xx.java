/*
 * MIT License
 *
 * Copyright (c) 2016
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.codingrodent.emulator.cards.common;

import com.codingrodent.emulator.utilities.DiskImage;

import javax.swing.*;
import java.io.File;

public abstract class FDC17xx extends BaseCard {
    protected final static int HEAD_BIT = 0x08;
    protected final DiskImage disk0 = new DiskImage();
    protected final DiskImage disk1 = new DiskImage();
    protected final DiskImage disk2 = new DiskImage();
    protected final DiskImage disk3 = new DiskImage();
    //
    protected DiskImage selectedDisk;

    //
    public enum FDC_CHIP {
        FDC_1771, FDC_1791, FDC_1793, FDC_1795, FDC_1797
    }

    protected FDC17xx() {
    }

    /**
     * Length codes are a bit tricky depending on which 17xx series devices is being used
     *
     * @param fdc    The chup being used
     * @param length The number of bytes in the sector
     * @return The length code of 0xFF if unknown
     */
    public static int getLengthCode(FDC_CHIP fdc, int length) {
        switch (fdc) {
            case FDC_1771:
            case FDC_1791:
            case FDC_1793:
                switch (length) {
                    case 128:
                        return 0x00;
                    case 256:
                        return 0x01;
                    case 512:
                        return 0x10;
                    case 1024:
                        return 0x11;
                }
            case FDC_1795:
            case FDC_1797:
                switch (length) {
                    case 256:
                        return 0x00;
                    case 512:
                        return 0x01;
                    case 1024:
                        return 0x10;
                    case 128:
                        return 0x11;
                }
        }
        return 0xFF;
    }

    /**
     * Check for any ANADisk images and load if available
     */
    protected void processANADiskImages() {
        if (null != cardProperties) {
            String ANADisk = cardProperties.get("ANADisk0");
            if (null != ANADisk) {
                systemContext.logInfoEvent("Loading an ANADisk image into drive 0 - " + ANADisk);
                disk0.loadANADisk(new File(ANADisk));
            }
            ANADisk = cardProperties.get("ANADisk1");
            if (null != ANADisk) {
                systemContext.logInfoEvent("Loading an ANADisk image into drive 1 - " + ANADisk);
                disk1.loadANADisk(new File(ANADisk));
            }
            ANADisk = cardProperties.get("ANADisk2");
            if (null != ANADisk) {
                systemContext.logInfoEvent("Loading an ANADisk image into drive 2 - " + ANADisk);
                disk2.loadANADisk(new File(ANADisk));
            }
            ANADisk = cardProperties.get("ANADisk3");
            if (null != ANADisk) {
                systemContext.logInfoEvent("Loading an ANADisk image into drive 3 - " + ANADisk);
                disk3.loadANADisk(new File(ANADisk));
            }
        }
    }

    /**
     * Query the user for a floppy disk file name and save
     *
     * @param drive int Drive select 0 through 3
     * @param fdc   Controller chip
     */
    protected void saveDisk(int drive, FDC_CHIP fdc) {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setFileFilter(new dskFileFilter());
        int returnValue = fc.showSaveDialog(systemContext.getPrimaryDisplay());
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String fileName = file.getAbsolutePath();
            systemContext.logDebugEvent("Saving disk image to " + fileName);
            switch (drive) {
                default:
                    disk0.dumpANADiskToFile(fileName, fdc);
                    break;
                case 1:
                    disk1.dumpANADiskToFile(fileName, fdc);
                    break;
                case 2:
                    disk2.dumpANADiskToFile(fileName, fdc);
                    break;
                case 3:
                    disk3.dumpANADiskToFile(fileName, fdc);
                    break;
            }
        }
    }

    /**
     * Query the user for a floppy disk image and load it
     *
     * @param drive int Drive select 0 through 3
     */
    protected void loadDisk(int drive) {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setFileFilter(new dskFileFilter());
        int returnValue = fc.showOpenDialog(systemContext.getPrimaryDisplay());
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String fileName = file.getAbsolutePath();
            systemContext.logDebugEvent("Loading disk file from " + fileName);
            switch (drive) {
                default:
                    disk0.ejectDisk();
                    disk0.loadANADisk(new File(fileName));
                    break;
                case 1:
                    disk1.ejectDisk();
                    disk1.loadANADisk(new File(fileName));
                    break;
                case 2:
                    disk2.ejectDisk();
                    disk2.loadANADisk(new File(fileName));
                    break;
                case 3:
                    disk3.ejectDisk();
                    disk3.loadANADisk(new File(fileName));
                    break;
            }
        }
    }

    /**
     * Check for any disk dump images and load if available
     */
    protected void processDumpDiskImages() {
        String dumpDisk = cardProperties.get("Dump0");
        if (null != dumpDisk) {
            int tracks = Integer.parseInt(cardProperties.getOrDefault("Dump0_Tracks", "-1"));
            int sectors = Integer.parseInt(cardProperties.getOrDefault("Dump0_Sectors", "-1"));
            int sides = Integer.parseInt(cardProperties.getOrDefault("Dump0_Sides", "-1"));
            int size = Integer.parseInt(cardProperties.getOrDefault("Dump0_Size", "-1"));
            loadDumpDiskImage(dumpDisk, tracks, sectors, sides, size, 0);
        }
        dumpDisk = cardProperties.get("Dump1");
        if (null != dumpDisk) {
            int tracks = Integer.parseInt(cardProperties.getOrDefault("Dump1_Tracks", "-1"));
            int sectors = Integer.parseInt(cardProperties.getOrDefault("Dump1_Sectors", "-1"));
            int sides = Integer.parseInt(cardProperties.getOrDefault("Dump1_Sides", "-1"));
            int size = Integer.parseInt(cardProperties.getOrDefault("Dump1_Size", "-1"));
            loadDumpDiskImage(dumpDisk, tracks, sectors, sides, size, 1);
        }
        dumpDisk = cardProperties.get("Dump2");
        if (null != dumpDisk) {
            int tracks = Integer.parseInt(cardProperties.getOrDefault("Dump2_Tracks", "-1"));
            int sectors = Integer.parseInt(cardProperties.getOrDefault("Dump2_Sectors", "-1"));
            int sides = Integer.parseInt(cardProperties.getOrDefault("Dump2_Sides", "-1"));
            int size = Integer.parseInt(cardProperties.getOrDefault("Dump2_Size", "-1"));
            loadDumpDiskImage(dumpDisk, tracks, sectors, sides, size, 2);
        }
        dumpDisk = cardProperties.get("Dump3");
        if (null != dumpDisk) {
            int tracks = Integer.parseInt(cardProperties.getOrDefault("Dump3_Tracks", "-1"));
            int sectors = Integer.parseInt(cardProperties.getOrDefault("Dump3_Sectors", "-1"));
            int sides = Integer.parseInt(cardProperties.getOrDefault("Dump3_Sides", "-1"));
            int size = Integer.parseInt(cardProperties.getOrDefault("Dump3_Size", "-1"));
            loadDumpDiskImage(dumpDisk, tracks, sectors, sides, size, 3);
        }
    }

    /**
     * Load a dump format disk
     *
     * @param fileName Name of the file
     * @param tracks   Tracks per side
     * @param sectors  Sectors per track
     * @param sides    Number of sides
     * @param size     Size of sectors
     * @param disk     Drive number
     */
    private void loadDumpDiskImage(String fileName, int tracks, int sectors, int sides, int size, int disk) {
        systemContext.logInfoEvent("Loading a disk dump image image into drive " + disk + " - " + fileName);
        if ((tracks < 1) || (tracks > 255)) {
            systemContext.logInfoEvent("Illegal track value : " + tracks);
            return;
        }
        if ((sectors < 1) || (sectors > 255)) {
            systemContext.logInfoEvent("Illegal sector value : " + sectors);
            return;
        }
        sides--;
        if ((sides < 0) || (sides > 1)) {
            systemContext.logInfoEvent("Illegal sides value : " + sides);
            return;
        }
        if ((size < 128) || (size > 1024)) {
            systemContext.logInfoEvent("Illegal sector size value : " + size);
            return;
        }
        //
        try {
            switch (disk) {
                default:
                    disk0.diskDumpReader(new File(fileName), tracks, sectors, size, sides);
                    break;
                case 1:
                    disk1.diskDumpReader(new File(fileName), tracks, sectors, size, sides);
                    break;
                case 2:
                    disk2.diskDumpReader(new File(fileName), tracks, sectors, size, sides);
                    break;
                case 3:
                    disk3.diskDumpReader(new File(fileName), tracks, sectors, size, sides);
                    break;
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load disk image. " + e.getMessage());
        }
    }

    /**
     * inner class for the file filter (.dsk)
     */
    private static class dskFileFilter extends javax.swing.filechooser.FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            } else {
                int place = f.getName().lastIndexOf('.');
                if (place == -1) {
                    return false;
                }
                String fileType = f.getName().substring(place).toLowerCase();
                return (0 == fileType.compareTo(".dmp"));
            }
        }

        @Override
        public String getDescription() {
            return "Floppy disk image file";
        }
    }

}