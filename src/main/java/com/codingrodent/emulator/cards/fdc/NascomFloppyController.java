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

package com.codingrodent.emulator.cards.fdc;

import com.codingrodent.emulator.cards.common.FDC17xx;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static com.codingrodent.emulator.cards.common.FDC17xx.FDC_CHIP.FDC_1793;

// import static net.sleepymouse.emulator.cards.common.FDC17xx.FDC_CHIP.FDC_1793;

public class NascomFloppyController extends FDC17xx {
    private final static int statusPort = 0xE0;
    private final static int commandPort = 0xE0;
    private final static int trackPort = 0xE1;
    private final static int sectorPort = 0xE2;
    private final static int dataPort = 0xE3;
    private final static int drivePort = 0xE4;
    private final static int intrqPort = 0xE5;

    //    private final static int XEBEC_DATA = 0xE7;
    //    private final static int XEBEC_CTRL = 0xE6;
    //    private final static int NOTREADY        = 0x80;
    //    private final static int WRITEPROTECT    = 0x40;
    private final static int HEAD = 0x20;
    //    private final static int SEEKERROR       = 0x10;
    private final static int RNF = 0x10;
    //    private final static int CRCERROR        = 0x08;
    private final static int TRACK0 = 0x04;
    private final static int LOST_DATA = 0x04;
    //    private final static int INDEX           = 0x02;
    private final static int DRQ = 0x02;
    private final static int BUSY = 0x01;
    private final static int INTRQ = 0x01;
    private final static int DRQB = 0x80;
    private final static int RESTORE = 0;
    private final static int SEEK = 1;
    private final static int STEP = 2;
    private final static int STEP_IN = 3;
    private final static int STEP_OUT = 4;
    private final static int READ_SECTOR = 5;
    private final static int WRITE_SECTOR = 6;
    private final static int READ_ADDRESS = 7;
    private final static int READ_TRACK = 8;
    private final static int WRITE_TRACK = 9;
    private final static int FORCE_INTERRUPT = 10;
    private final static int IDLE = 255;
    private final static int maxTrack = 80;
    private final static int TRACK_LENGTH = 0x1856;
    private int statusRegister;
    private int commandRegister;
    private int trackRegister;
    private int sectorRegister;
    private int dataRegister;
    private int driveRegister;
    private int intrqRegister;
    private int track;
    private int sector;
    private int side;
    private boolean stepIn;
    //    private boolean runMotor;
    //    private boolean highDensity;
    //    private boolean multiple;

    private int command;
    private int idByteCounter;
    private int[] readBuffer;
    private byte[] writeBuffer;
    private int bufferPosition;
    private int sectorPosition;
    private byte[] sectorBuffer;
    private int formatTrack;
    private int formatSector;
    private int formatSide;
    private int formatLength;
    private boolean trackWriteStarted;
    private long commandTime;

    /*
     * constructor forces general reset
     */
    public NascomFloppyController() {
        attachGUIComponents();
        reset();
    }

    /**
     * One off initialisation carried out after card object creation
     */
    @Override
    public void initialise() {
        // Load files in Anadisk format (.dmp)
        processANADiskImages();
        // Load files in disk dump format (.dsk)
        processDumpDiskImages();
    }

    /**
     * Get the details of the card by the author
     *
     * @return Card name string
     */
    @Override
    public String getCardDetails() {
        return "Nascom Floppy Controller Card - Version 1.0";
    }

    /**
     * Reset the card
     */
    @Override
    public void reset() {
        statusRegister = 0;
        trackRegister = 0;
        sectorRegister = 1;
        dataRegister = 0;
        driveRegister = 0;
        intrqRegister = 0;
        //
        track = 0;
        sector = 1;
        side = 0;
        command = IDLE;
        //
        stepIn = true;
        //        runMotor = false;
        //        highDensity = true;
        //        multiple = false;
        //
        bufferPosition = -1;
        readBuffer = new int[0];
        selectedDisk = disk0;
        //
        idByteCounter = 0;
    }

    /**
     * @param e ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Disk Menu
        String menuCommand = e.getActionCommand();
        if ("Save Disk 0".equals(menuCommand)) {
            saveDisk(0, FDC_1793);
        } else {
            if ("Save Disk 1".equals(menuCommand)) {
                saveDisk(1, FDC_1793);
            } else {
                if ("Save Disk 2".equals(menuCommand)) {
                    saveDisk(2, FDC_1793);
                } else {
                    if ("Save Disk 3".equals(menuCommand)) {
                        saveDisk(3, FDC_1793);
                    } else {
                        if ("Load Disk 0".equals(menuCommand)) {
                            loadDisk(0);
                        } else {
                            if ("Load Disk 1".equals(menuCommand)) {
                                loadDisk(1);
                            } else {
                                if ("Load Disk 2".equals(menuCommand)) {
                                    loadDisk(2);
                                } else {
                                    if ("Load Disk 3".equals(menuCommand)) {
                                        loadDisk(3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Does the card support input at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isInputPort(int address) {
        switch (address) {
            case statusPort:
                return true;
            case trackPort:
                return true;
            case sectorPort:
                return true;
            case dataPort:
                return true;
            case drivePort:
                return true;
            case intrqPort:
                return true;

            case 0xE6:
                return true;
            case 0xE7:
                return true;
        }
        return false;
    }

    /**
     * Does the card support output at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isOutputPort(int address) {
        switch (address) {
            case commandPort:
                return true;
            case trackPort:
                return true;
            case sectorPort:
                return true;
            case dataPort:
                return true;
            case drivePort:
                return true;
            case 0xE6:
                return true;
            case 0xE7:
                return true;

        }
        return false;
    }

    /**
     * Write to the disk controller card I/O ports
     *
     * @param address Address being written to
     * @param data    Data being written
     */
    @Override
    public void ioWrite(int address, int data) {
        switch (address) {
            case commandPort:
                writeCommand(data);
                break;

            case trackPort:
                writeTrack(data);
                break;

            case sectorPort:
                writeSector(data);
                break;

            case dataPort:
                writeData(data);
                break;

            case drivePort:
                writeDrive(data);
                break;
            //
            //            case XEBEC_CTRL:
            //                xebecController.ioWrite(address, data);
            //                break;
            //
            //            case XEBEC_DATA:
            //                xebecController.ioWrite(address, data);
            //                break;
            default:
        }
    }

    /**
     * Read from the disk controller card
     *
     * @param address Address to read from
     * @return Value read from the port
     */
    @Override
    public int ioRead(int address) {
        return ioRead2(address);
    }

    /**
     * Recover the number of T states executed by the CPU
     *
     * @return long
     */
    @Override
    public long getClock() {
        return -1;
    }

    /**
     * Set the number of T states executed by the CPU
     *
     * @param t long
     */
    @Override
    public void setClock(long t) {
    }

    /**
     *
     */
    private void attachGUIComponents() {
        JFrame screenFrame = systemContext.getPrimaryDisplay();
        JMenuBar menuBar = screenFrame.getJMenuBar();
        // save a floppy disk
        JMenu menu = new JMenu("Save Disk");
        //
        JMenuItem disk0Menu = new JMenuItem("Save Disk 0");
        disk0Menu.addActionListener(this);
        menu.add(disk0Menu);

        JMenuItem disk1Menu = new JMenuItem("Save Disk 1");
        disk1Menu.addActionListener(this);
        menu.add(disk1Menu);

        JMenuItem disk2Menu = new JMenuItem("Save Disk 2");
        disk2Menu.addActionListener(this);
        menu.add(disk2Menu);

        JMenuItem disk3Menu = new JMenuItem("Save Disk 3");
        disk3Menu.addActionListener(this);
        menu.add(disk3Menu);
        //
        menuBar.add(menu);

        // load a floppy disk
        menu = new JMenu("Load Disk");
        //
        JMenuItem diskLoad0 = new JMenuItem("Load Disk 0");
        diskLoad0.addActionListener(this);
        menu.add(diskLoad0);

        JMenuItem diskLoad1 = new JMenuItem("Load Disk 1");
        diskLoad1.addActionListener(this);
        menu.add(diskLoad1);

        JMenuItem diskLoad2 = new JMenuItem("Load Disk 2");
        diskLoad2.addActionListener(this);
        menu.add(diskLoad2);

        JMenuItem diskLoad3 = new JMenuItem("Load Disk 3");
        diskLoad3.addActionListener(this);
        menu.add(diskLoad3);
        //
        menuBar.add(menu);
    }

    /**
     * Write a byte into ram
     *
     * @param address The address to be written to
     * @param data    The byte to be written
     * @return True if ???
     */
    @Override
    public boolean memoryWrite(int address, int data, boolean ramdis) {
        return false;
    }

    /**
     * Read data from the memory bus taking into account the RAMDIS signal
     *
     * @param address The address to read from
     * @param ramdis  RAMDIS bus signal
     * @return The byte read
     */
    @Override
    public int memoryRead(int address, boolean ramdis) {
        return NO_MEMORY_PRESENT;
    }

    /**
     * Read a byte from memory
     *
     * @param address Address to read from
     * @return Byte of data
     */
    @Override
    public int memoryRead(int address) {
        return NO_MEMORY_PRESENT;
    }

    /**
     * Will a read to an address cause RAMDIS (i.e. ROM) to be asserted
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDIS(int address) {
        return false;
    }

    /**
     * Will a read to an address may cause RAMDIS (i.e. ROM) to be asserted. For example, a paged out ROM will cause RAMDIS to be asserted. That is, RAMDIS may occur at this address.
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDIScapable(int address) {
        return false;
    }

    /**
     * Read from the disk controller card
     *
     * @param address Address to read from
     * @return Value read from the port
     */
    private int ioRead2(int address) {

        switch (address) {
            case statusPort:
                return readStatus();

            case trackPort:
                return readTrack();

            case sectorPort:
                return readSector();

            case dataPort:
                return readData();

            case drivePort:
                return readDrive();

            case intrqPort:
                return readIntrq();
        }
        return 0;
    }

    /**
     * Read the stsus port
     *
     * @return Status port value
     */
    private int readStatus() {
        //System.out.println("Read status : "+util.getByte(statusRegister));
        testTimeout();
        return statusRegister;
    }

    private void testTimeout() {
        long t = nasBus.getClock() - commandTime;

        if ((!(IDLE == command)) && (t > 100000)) {
            //System.out.println(t);
            statusRegister = LOST_DATA;
            intrqRegister = INTRQ; //  INTRQ
            command = IDLE;
            //
            sectorPosition = -1;
            bufferPosition = -1;
        }
    }

    /**
     * Read the track register
     *
     * @return Track value
     */
    private int readTrack() {
        //System.out.println("Read track : "+util.getByte(trackRegister));
        return trackRegister;
    }

    /**
     * Read the sector register
     *
     * @return Sector value
     */
    private int readSector() {
        //System.out.println("Read sector : "+util.getByte(sectorRegister));
        return sectorRegister;
    }

    /**
     * Read the drive register
     *
     * @return Drive register value
     */
    private int readDrive() {
        //System.out.println("Read drive : "+util.getByte(driveRegister));
        return driveRegister;
    }

    /**
     * Read a byte from a sector. If no data avaialble, flag the error.
     *
     * @return The data value from the sector
     */
    private int readData() {
        commandTime = nasBus.getClock();
        if (-1 != bufferPosition) {
            dataRegister = readBuffer[bufferPosition++];
            intrqRegister = DRQB; // data ready (DRQ)
        }
        if (bufferPosition == readBuffer.length) {
            bufferPosition = -1; //  no data available
            statusRegister = 0; //  reset status register
            intrqRegister = INTRQ; //  INTRQ
        }
        //System.out.println(dataRegister);
        return dataRegister;
    }

    /**
     * Get the value in the intrq register
     *
     * @return Register value
     */
    private int readIntrq() {
        testTimeout();
        return intrqRegister;
    }

    /**
     * Write to the command port on the card
     *
     * @param data Data being written
     */
    private void writeCommand(int data) {
        statusRegister = 0;
        commandRegister = data;
        switch (commandRegister & 0xE0) {
            case 0x00:
                if ((commandRegister & 0x10) == 0) {
                    command = RESTORE;
                    restoreCMD();
                } else {
                    command = SEEK;
                    seekCMD();
                }
                break;

            case 0x20:
                command = STEP;
                step();
                break;

            case 0x40:
                command = STEP_IN;
                stepIn();
                break;

            case 0x60:
                command = STEP_OUT;
                stepOut();
                break;

            case 0x80:
                commandTime = nasBus.getClock();
                command = READ_SECTOR;
                readSectorCMD();
                break;

            case 0xA0:
                commandTime = nasBus.getClock();
                command = WRITE_SECTOR;
                writeSectorCMD();
                break;

            case 0xC0:
                if ((commandRegister & 0x10) == 0) {
                    commandTime = nasBus.getClock();
                    command = READ_ADDRESS;
                    ReadAddressCMD();
                } else {
                    command = FORCE_INTERRUPT;
                    InterruptCMD();
                }
                break;

            case 0xE0:
                if ((commandRegister & 0x10) == 0) {
                    commandTime = nasBus.getClock();
                    command = READ_TRACK;
                    ReadTrackCMD();
                } else {
                    commandTime = nasBus.getClock();
                    command = WRITE_TRACK;
                    writeTrackCMD();
                }
                break;
            default:
        }
    }

    private void seekCMD() {
        track = dataRegister;
        trackRegister = dataRegister;
        if ((commandRegister & HEAD_BIT) == 0) {
            statusRegister = TRACK0;
        } else {
            statusRegister = HEAD | TRACK0;
        }
        intrqRegister = INTRQ; //  INTRQ
        command = IDLE;
    }

    private void restoreCMD() {
        track = 0;
        trackRegister = 0;
        if ((commandRegister & HEAD_BIT) == 0) {
            statusRegister = TRACK0;
        } else {
            statusRegister = HEAD | TRACK0;
        }
        intrqRegister = INTRQ; //  INTRQ
        command = IDLE;
    }

    /**
     *
     */
    private void stepIn() {
        if (track <= maxTrack) {
            trackRegister = (track++) & 0xFF;
        }
        trackRegister = track;
        //
        if ((commandRegister & HEAD_BIT) != 0) {
            statusRegister = HEAD;
        } else {
            statusRegister = 0;
        }
        intrqRegister = INTRQ; //  INTRQ
        command = IDLE;
    }

    /**
     *
     */
    private void stepOut() {
        if (track > 0) {
            track--;
        }
        trackRegister = track;
        //
        if (track == 0) {
            statusRegister = TRACK0;
        } else {
            statusRegister = 0;
        }
        if ((commandRegister & HEAD_BIT) != 0) {
            statusRegister = statusRegister | HEAD;
        }
        intrqRegister = INTRQ; //  INTRQ
        command = IDLE;
    }

    /**
     *
     */
    private void step() {
        if ((stepIn) && (track <= maxTrack)) {
            track++;
        } else {
            if (track > 0) {
                track--;
            }
        }
        trackRegister = track;
        //
        if (track == 0) {
            statusRegister = TRACK0;
        } else {
            statusRegister = 0;
        }
        if ((commandRegister & HEAD_BIT) != 0) {
            statusRegister = statusRegister | HEAD;
        }
        intrqRegister = INTRQ; //  INTRQ
        command = IDLE;
    }

    /**
     * Read a sector from the disk and copy it into an internal buffer for reading
     */
    private void readSectorCMD() {
        int value;
        byte[] localSectorBuffer = selectedDisk.getSector(track, sector, side);
        if (null == localSectorBuffer) {
            command = IDLE;
            bufferPosition = -1; //  no data available
            statusRegister = RNF; //  set Record not found bit
            intrqRegister = INTRQ; //  INTRQ
        } else {
            int length = localSectorBuffer.length;
            readBuffer = new int[length];
            for (int position = 0; position < length; position++) {
                value = localSectorBuffer[position];
                if (value < 0) {
                    value = value + 256;
                }
                readBuffer[position] = value;
            }
            bufferPosition = 0;
            statusRegister = DRQ | BUSY; //  set busy and drq bits
            intrqRegister = DRQB; // drq bit
        }
    }

    /**
     *
     */
    private void writeSectorCMD() {
        byte[] loadedSector = selectedDisk.getSector(track, sector, side);
        if (null == loadedSector) {
            statusRegister = 0x10; // set record not found bit
            intrqRegister = INTRQ; //  INTRQ
        } else {
            writeBuffer = new byte[loadedSector.length];
            bufferPosition = 0;
            statusRegister = statusRegister | DRQ | BUSY; //  set busy and drq bits
            intrqRegister = DRQB; // drq
        }
    }

    private void InterruptCMD() {
        statusRegister = 0x00; // All clear
        intrqRegister = INTRQ; // INTRQ
        command = IDLE;
    }

    private void ReadAddressCMD() {
        // read track to find first valid sector (if any)
        int length;
        sector = -1;
        for (int i = 0; i < 255; i++) {
            if (null != selectedDisk.getSector(track, i, side)) {
                sector = i;
                break;
            }
        }
        if (-1 != sector) {
            switch (selectedDisk.getSector(track, sector, side).length) {
                default:
                    length = 0x00;
                    break;
                case 256:
                    length = 0x01;
                    break;
                case 512:
                    length = 0x02;
                    break;
                case 1024:
                    length = 0x03;
                    break;
            }
            bufferPosition = 0;
            readBuffer = new int[6];
            readBuffer[0] = track;
            readBuffer[1] = side;
            readBuffer[2] = sector;
            readBuffer[3] = length;
            readBuffer[4] = 0;
            readBuffer[5] = 0;
            //
            // The 1793 writes the track value into the sector register for a Read Address command. Weird!
            sectorRegister = track;
            //
            statusRegister = DRQ; //  set busy & drq bits
            intrqRegister = DRQB;
            //
            //System.out.println("Address :: "+side+"/"+trackRegister+"/"+sectorRegister+"/"+length);
        } else {
            command = IDLE;
            bufferPosition = -1; //  no data available
            statusRegister = RNF; //  set record not found
            intrqRegister = INTRQ;
            //System.out.println("<< No Address Data>>");
        }
    }

    /**
     *
     */
    private void ReadTrackCMD() {
        intrqRegister = INTRQ; // INTRQ
        command = IDLE;
    }

    /**
     *
     */
    private void writeTrackCMD() {
        writeBuffer = new byte[TRACK_LENGTH + 1024];
        bufferPosition = 0;
        statusRegister = DRQ | BUSY; //  set busy & drq bits
        intrqRegister = DRQB; //  data request
        sectorPosition = -1;
        //
        trackWriteStarted = false;
    }

    /**
     * Write to the track register
     *
     * @param data The data written to the port
     */
    private void writeTrack(int data) {
        trackRegister = data;
        //
        if (track == 0) {
            statusRegister = TRACK0;
        } else {
            statusRegister = 0;
        }
        if ((commandRegister & HEAD_BIT) != 0) {
            statusRegister = statusRegister | HEAD;
        }
    }

    /**
     * Write to the sector register
     *
     * @param data The data written to the port
     */
    private void writeSector(int data) {
        sectorRegister = data;
        sector = data;
    }

    /**
     * Write to the data register
     *
     * @param data The data written to the port
     */
    private void writeData(int data) {
        commandTime = nasBus.getClock();
        dataRegister = data;
        if ((-1 != bufferPosition) && (IDLE != command)) {
            if (WRITE_TRACK != command) {
                writeBuffer[bufferPosition++] = (byte) data;
                if (bufferPosition == writeBuffer.length) {
                    bufferPosition = -1; //  no data available
                    statusRegister = 0; //  reset status register
                    intrqRegister = INTRQ; //  INTRQ
                    selectedDisk.putSector(track, sector, side, writeBuffer);
                    command = IDLE;
                }
            } else {
                if (-1 != sectorPosition) {
                    if (data > 127) {
                        data = data - 256;
                    }
                    sectorBuffer[sectorPosition++] = (byte) data;
                    if (formatLength == sectorPosition) {
                        sectorPosition = -1;
                        if (!trackWriteStarted) {
                            selectedDisk.eraseTrack(formatTrack, formatSide);
                            trackWriteStarted = true;
                        }
                        selectedDisk.putSector(formatTrack, formatSector, formatSide, sectorBuffer);
                    }
                } else {
                    if (0xFE == data) {
                        idByteCounter = 4;
                    } else {
                        if (0xFB == data) {
                            sectorPosition = 0;
                        } else {
                            if (0 != idByteCounter) {
                                switch (idByteCounter--) {
                                    case 2:
                                        formatSector = data;
                                        break;
                                    case 3:
                                        formatSide = data;
                                        break;
                                    case 4:
                                        formatTrack = track;
                                        break;
                                    case 1:
                                        switch (data) {
                                            default:
                                                sectorBuffer = new byte[128];
                                                break;
                                            case 0x01:
                                                sectorBuffer = new byte[256];
                                                break;
                                            case 0x02:
                                                sectorBuffer = new byte[512];
                                                break;
                                            case 0x03:
                                                sectorBuffer = new byte[1024];
                                                break;
                                        }
                                        formatLength = sectorBuffer.length;
                                        break;
                                }
                            }
                        }
                    }
                }
                bufferPosition++;
                if (TRACK_LENGTH == bufferPosition) {
                    bufferPosition = -1; //  no data available
                    statusRegister = 0; //  reset status register
                    intrqRegister = INTRQ; //  INTRQ
                    command = IDLE;
                }
            }
        }
    }

    /**
     * Write to the drive control register
     *
     * @param data The data written to the port
     */

    private void writeDrive(int data) {
        driveRegister = data;
        switch (data & 0x1F) {
            case 0x01:
                selectedDisk = disk0;
                side = 0;
                break;
            case 0x11:
                selectedDisk = disk0;
                side = 1;
                break;
            case 0x02:
                selectedDisk = disk1;
                side = 0;
                break;
            case 0x12:
                selectedDisk = disk1;
                side = 1;
                break;
            //
            case 0x04:
                selectedDisk = disk2;
                side = 0;
                break;
            case 0x14:
                selectedDisk = disk2;
                side = 1;
                break;
            case 0x08:
                selectedDisk = disk3;
                side = 0;
                break;
            case 0x18:
                selectedDisk = disk3;
                side = 1;
                break;
            default:
                System.out.println("Illegal drive selection");
        }
        // which side to select
        if ((data & 0x10) == 0) {
            side = 0;
        } else {
            side = 1;
            // motor select
        }
        //        if ((data & 0x20) == 0) {
        //            runMotor = false;
        //        } else {
        //            runMotor = true;
        //            // density select
        //        }
        //        if ((data & 0x40) == 0) {
        //            highDensity = false;
        //        } else {
        //            highDensity = true;
        //        }
    }

}