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

package com.codingrodent.emulator.utilities;

import com.codingrodent.emulator.cards.common.FDC17xx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class DiskImage {
    private final static int MAX_TRACKS = 256;
    private final static int MAX_SECTORS = 256;
    private final BTree[] diskSide0 = new BTree[MAX_TRACKS];
    private final BTree[] diskSide1 = new BTree[MAX_TRACKS];

    /**
     * Create a blank disk image
     */
    public DiskImage() {
        resetDiskImage();
    }

    /**
     * Emulator entry point
     *
     * @param args Not used
     */
    public static void main(String[] args) {
        DiskImage reader = new DiskImage();
        short[] ram = new short[65536];
        MemoryChunk memory = new MemoryChunk(ram);
        try {
            memory.setBase(0xC000);
            reader.dumpDiskToMemory(memory, 1, 1, 0x20);
            reader.dumpDiskToMemory(memory, 2, 0xF, 0x20);
            memory.setBase(0x0000);
            //
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dump.nas"), StandardCharsets.UTF_8));
            FileHandler fileHandler = new FileHandler();
            fileHandler.writeHexDumpFile(memory, writer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Clear a disk image
     */
    private void resetDiskImage() {
        for (int track = 0; track < diskSide0.length; track++) {
            diskSide0[track] = new BTree();
            diskSide1[track] = new BTree();
        }
    }

    /**
     * Simulate a disk removal and erase + tidy up any remaining data
     */
    public void ejectDisk() {
        resetDiskImage();
    }

    /**
     * disk reader for files in ANADisk format
     *
     * @param file File handle to the ANADisk file
     */
    public void loadANADisk(File file) {
        byte[] headerBlock = new byte[8];
        byte[] sectorBlock;

        try {
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            while (fis.read(headerBlock) != -1) {
                int cylinder = headerBlock[0];
                int side = headerBlock[1];
                int sector = headerBlock[4];
                int count = headerBlock[6] + headerBlock[7] << 8;

				/*
                 * int lengthCode = headerBlock[5]; System.out.print("Cylinder = " + util.getByte(cylinder));
				 * System.out.print(" Side = " + util.getByte(side)); System.out.print(" Sector = " +
				 * util.getByte(sector)); System.out.print(" LengthCode=" + util.getByte(lengthCode));
				 * System.out.println(" Count = " + util.getWord(count));
				 */
                if (count < 0) {
                    count = 128;
                }
                sectorBlock = new byte[count];
                if (-1 != fis.read(sectorBlock, 0, count)) {
                    putSector(cylinder, sector, side, sectorBlock);
                }
            }
            fis.close();

        } catch (Exception e) {
            throw new RuntimeException("Unable to load disk image. " + e.getMessage());
        }
    }

    /**
     * Read a binary dump of a double sided disk in .dmp format side...track...sector
     *
     * @param file       Dump file to read
     * @param tracks     Tracks to read
     * @param sectors    Sectors to read
     * @param sectorSize Size of the sectors
     * @param sides      Number of sides (0 or 1)
     */
    public void diskDumpReader(File file, int tracks, int sectors, int sectorSize, int sides) {
        try {
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            for (int track = 0; track < tracks; track++) {
                for (int sector = 1; sector <= sectors; sector++) {
                    byte[] sectorData = new byte[sectorSize];
                    for (int position = 0; position < sectorSize; position++) {
                        sectorData[position] = (byte) fis.read();
                    }
                    putSector(track, sector, 0, sectorData);
                }
                if (1 == sides) {
                    for (int sector = 1; sector <= sectors; sector++) {
                        byte[] sectorData = new byte[sectorSize];
                        for (int position = 0; position < sectorSize; position++) {
                            sectorData[position] = (byte) fis.read();
                        }
                        putSector(track, sector, 1, sectorData);
                    }
                }
            }
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read a sector as a byte array from a specified disk
     *
     * @param track  Track to read
     * @param sector Sector to read
     * @param side   Side to read
     * @return The sector as a byte array
     */
    public byte[] getSector(int track, int sector, int side) {
        try {
            if (0 == side) {
                return ((Sector) diskSide0[track].getNode(sector).getData()).getData();
            } else {
                return ((Sector) diskSide1[track].getNode(sector).getData()).getData();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Put a sector into the B tree representing the disk image
     *
     * @param track      The track to write
     * @param sector     The sector to write
     * @param side       The side to write
     * @param sectorData The data to write
     */
    public void putSector(int track, int sector, int side, byte[] sectorData) {
        // System.out.println("Disk Image : "+side+" / "+track + " / " + sector );
        Sector sectorNode = new Sector(sector);
        BTreeNode node = new BTreeNode();
        sectorNode.setData(sectorData);
        node.setData(sectorNode);
        if (0 == side) {
            diskSide0[track].insertNode(node);
        } else {
            diskSide1[track].insertNode(node);
        }
    }

    /**
     * Erase all the sectors in a track
     *
     * @param track int The track to erase
     * @param side  int The side to erase
     */
    public void eraseTrack(int track, int side) {
        if (0 == side) {
            diskSide0[track].erase();
        } else {
            diskSide1[track].erase();
        }
    }

    /**
     * Read a byte from a sector
     *
     * @param sector   The sector to read from
     * @param location Location in sector
     * @return The byte as an int (so we don't get any negative values)
     */
    private int getByte(byte[] sector, int location) {
        int p1 = sector[location];
        if (p1 < 0) {
            p1 = p1 + 256;
        }
        return p1;
    }

    /**
     * Read a word from a sector
     *
     * @param sector   The sector to read from
     * @param location Location in sector
     * @return The byte as an int (so we don't get any negative values)
     */
    private int getWord(byte[] sector, int location) {
        int p1 = sector[location];
        if (p1 < 0) {
            p1 = p1 + 256;
        }
        int p2 = sector[location + 1];
        if (p2 < 0) {
            p2 = p2 + 256;
        }
        return (p2 << 8) + p1;
    }

    /**
     * Print out a sector as if it contains Nas-Dos directory data
     *
     * @param sector Sector data
     */
    public void printNasDosDirectorySector(byte[] sector) {
        String fileName;
        int executionAddress;
        int loadAddress;
        int startTrack;
        int startSector;
        int length;
        //
        for (int i = 0; i < sector.length; i = i + 16) {
            if (sector[i] >= 0) {
                fileName = new String(Arrays.copyOfRange(sector, i, i + 8), StandardCharsets.UTF_8);
                executionAddress = getWord(sector, i + 8);
                loadAddress = getWord(sector, i + 10);
                startTrack = getByte(sector, i + 12);
                startSector = getByte(sector, i + 13);
                length = getWord(sector, i + 14);

                System.out.print(fileName + " ");
                System.out.print(Utilities.getWord(executionAddress) + " ");
                System.out.print(Utilities.getWord(loadAddress) + " ");
                System.out.print(Utilities.getWord(startTrack) + " ");
                System.out.print(Utilities.getWord(startSector) + " ");
                System.out.print(Utilities.getWord(length) + " ");
                System.out.println();
            }
        }
    }

    /**
     * Dump a sector to screen in NAS SYS tab format
     *
     * @param sector The sector to be dumped
     */
    private void dumpSector(byte[] sector) {
        int value;
        for (int i = 0; i < sector.length; i = i + 16) {
            for (int j = 0; j < 16; j++) {
                value = sector[i + j];
                if (value < 0) {
                    value = value + 256;
                }
                System.out.print(Utilities.getByte(value) + " ");
            }
            for (int j = 0; j < 16; j++) {
                value = sector[i + j];
                if (value < 0) {
                    value = value + 256;
                }
                if ((value > 31) && (value < 128)) {
                    System.out.print((char) value);
                } else {
                    System.out.print(".");
                }
            }
            System.out.println();
        }
    }

    /**
     * Dump a file to screen in NAS SYS tab format
     *
     * @param fileName The file to be dumped
     */
    public void dumpFile(String fileName) {
        byte[] buffer = new byte[16];
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(fileName))) {
            while (-1 != fis.read(buffer)) {
                int value;
                for (int j = 0; j < 16; j++) {
                    value = buffer[j];
                    if (value < 0) {
                        value = value + 256;
                    }
                    System.out.print(Utilities.getByte(value) + " ");
                }
                for (int j = 0; j < 16; j++) {
                    value = buffer[j];
                    if (value < 0) {
                        value = value + 256;
                    }
                    if ((value > 31) && (value < 128)) {
                        System.out.print((char) value);
                    } else {
                        System.out.print(".");
                    }
                }
                System.out.println();
                fis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dump a disk in ANADisk image format
     *
     * @param fileName File to produce
     * @param fdc      Controller chip
     */
    public void dumpANADiskToFile(String fileName, FDC17xx.FDC_CHIP fdc) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            byte[] sectorData;
            //
            for (int side = 0; side < 2; side++) {
                for (int track = 0; track < MAX_TRACKS; track++) {
                    for (int sector = 0; sector < MAX_SECTORS; sector++) {
                        sectorData = getSector(track, sector, side);
                        // write the ANADISK header block
                        if (null != sectorData) {
                            int length = sectorData.length;
                            fos.write(track);
                            fos.write(side);
                            fos.write(track);
                            fos.write(side);
                            fos.write(sector);
                            fos.write(FDC17xx.getLengthCode(fdc, length)); // length code
                            fos.write(length & 0x00FF);
                            fos.write((length & 0xFF00) >> 8);
                            fos.write(sectorData, 0, length);
                        }
                    }
                }
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dump two images to a file (double sided disk)
     *
     * @param diskImage0 Side 0
     * @param diskImage1 Side 1
     * @param fileName   Name of file to write to
     */
    public void dumpDiskToFile(byte[][][] diskImage0, byte[][][] diskImage1, String fileName) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            // dump side 0
            for (int track = 0; track < diskImage0.length; track++) {
                // dump side 0
                for (int sector = 0; sector < diskImage0[0].length; sector++) {
                    fos.write(diskImage0[track][sector], 0, diskImage0[0][0].length);
                }
                // dump side 1
                for (int sector = 0; sector < diskImage1[0].length; sector++) {
                    fos.write(diskImage1[track][sector], 0, diskImage1[0][0].length);
                }
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dumpDiskToMemory(MemoryChunk memory, int track, int sector, int size) {
        for (int i = 0; i < size; i++) {
            byte[] data = getSector(track, sector, 0);
            dumpSector(data);
            for (byte b : data) {
                memory.setByte(b);
            }
            if (18 == sector) {
                sector = 1;
                track++;
            } else {
                sector++;
            }
        }
    }

}