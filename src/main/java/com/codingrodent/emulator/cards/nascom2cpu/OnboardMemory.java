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

package com.codingrodent.emulator.cards.nascom2cpu;

import com.codingrodent.emulator.emulator.SystemContext;
import com.codingrodent.emulator.nas80Bus.INasBus;
import com.codingrodent.emulator.utilities.*;
import com.codingrodent.microprocessor.IMemory;

import java.io.IOException;
import java.util.Map;

class OnboardMemory implements IMemory {

    private final static int MAX_MEMORY = 65536;
    private final static int MAX_ADDRESS = MAX_MEMORY - 1;
    //
    // Pre-calculated access flags for performance
    private final short[] memory = new short[MAX_MEMORY];
    private final boolean[] onboard = new boolean[MAX_MEMORY];
    private final boolean[] bankAValid = new boolean[MAX_MEMORY];
    private final boolean[] bankBValid = new boolean[MAX_MEMORY];
    private final boolean[] monitorValid = new boolean[MAX_MEMORY];
    private final boolean[] videoValid = new boolean[MAX_MEMORY];
    private final boolean[] basicROMValid = new boolean[MAX_MEMORY];
    private final boolean[] workspaceValid = new boolean[MAX_MEMORY];
    private final boolean[] ramValid = new boolean[MAX_MEMORY];
    private final boolean[] romValid = new boolean[MAX_MEMORY];
    private final SystemContext systemContext;
    //
    private StandardDisplayDevice displayDevice;
    private Map<String, String> cardProperties;
    private INasBus nasBus;
    // Nascom 2 Onboard Memory
    private boolean basicROMInstalled;
    private boolean bankARAMInstalled;
    private boolean bankAROMInstalled;
    private boolean bankBROMInstalled;
    private boolean bankBRAMInstalled;
    private boolean monitorROMInstalled;
    //
    private int bankABase;
    private int bankAEnd;
    private int bankBBase;
    private int bankBEnd;
    private int monitorROMBase;
    private int monitorROMEnd;
    private int videoRAMBase;
    private int videoRAMEnd;
    private int scratchpadRAMBase;
    private int scratchpadRAMEnd;
    private int basicROMBase;
    private int basicROMEnd;

    /*
     * Simulate the Nascom 2 main board memory
     */
    OnboardMemory() {
        displayDevice = null;
        systemContext = SystemContext.createInstance();
    }

    /**
     * Set any card specific parameters
     *
     * @param cardProperties Property list
     */
    void setCardProperties(Map<String, String> cardProperties) {
        this.cardProperties = cardProperties;
    }

    /**
     * One off initialisation carried out after card object creation. Note that properties are available at this point.
     */
    void initialise() {
        basicROMInstalled = false;
        bankARAMInstalled = false;
        bankAROMInstalled = false;
        boolean bankAInstalled = false;
        bankBRAMInstalled = false;
        bankBROMInstalled = false;
        monitorROMInstalled = false;
        boolean bankBInstalled = false;
        //
        try {
            FileHandler fileHandler = new FileHandler();
            //
            String property = cardProperties.get("OperatingSystem");
            if (null != property) {
                MemoryChunk nasSys = fileHandler.readHexDumpFile(property);
                short[] rom = nasSys.getMemoryChunk();
                int base = nasSys.getBase();
                int length = nasSys.getSize();
                System.arraycopy(rom, 0, memory, base, length);
                monitorROMInstalled = true;
                monitorROMEnd = base + length;
                monitorROMBase = base;
                if (2048 != length) {
                    systemContext.logWarnEvent("The monitor ROM is not 2K bytes (" + length + " bytes found)");
                }
            }
            //
            property = cardProperties.get("8KROM");
            if (null != property) {
                MemoryChunk romBasic = fileHandler.readHexDumpFile(property);
                short[] rom = romBasic.getMemoryChunk();
                int base = 0xE000;
                int length = romBasic.getSize();
                if (8192 != length) {
                    String msg = "The 8K ROM is not 8K bytes (" + length + " bytes found)";
                    systemContext.logFatalEvent(msg);
                    throw new RuntimeException(msg);
                }
                System.arraycopy(rom, 0, memory, base, length);
                basicROMInstalled = true;
                basicROMBase = base;
                basicROMEnd = basicROMBase + 8192;
            }
            // Default video memory locations
            videoRAMBase = Utilities.getHexValue(cardProperties.getOrDefault("VideoRAMAddress", "0800"));
            videoRAMEnd = videoRAMBase + 1024;
            //
            // Default scratchpad memory locations
            scratchpadRAMBase = Utilities.getHexValue(cardProperties.getOrDefault("ScratchpadAddress", "0C00"));
            scratchpadRAMEnd = scratchpadRAMBase + 1024;
            //
            // Bank A memory
            String epromAType = null;
            String bankAEnabled = cardProperties.get("BankAEnabled");
            if (bankAEnabled.equalsIgnoreCase("true")) {
                bankABase = Utilities.getHexValue(cardProperties.getOrDefault("BankAAddress", "C000"));
                epromAType = cardProperties.get("BankAType");
                if (null != epromAType) {
                    if ("2708".equals(epromAType)) {
                        bankAEnd = bankABase + 4096;
                        bankAROMInstalled = true;
                        bankARAMInstalled = false;
                    } else {
                        if ("2716".equals(epromAType)) {
                            bankAEnd = bankABase + 8192;
                            bankAROMInstalled = true;
                            bankARAMInstalled = false;
                        } else {
                            if ("4118".equals(epromAType)) {
                                bankAEnd = bankABase + 4096;
                                bankAROMInstalled = false;
                                bankARAMInstalled = true;
                            } else {
                                if ("6116".equals(epromAType)) {
                                    bankAEnd = bankABase + 8192;
                                    bankAROMInstalled = false;
                                    bankARAMInstalled = true;
                                } else {
                                    systemContext.logWarnEvent("Unknown memory device type specified in bank A - " + epromAType);
                                }
                            }
                        }
                    }
                    //
                    property = cardProperties.get("BankAFile");
                    if (null != property) {
                        MemoryChunk bankAfile = fileHandler.readHexDumpFile(property);
                        short[] rom = bankAfile.getMemoryChunk();
                        int base = bankAfile.getBase();
                        int length = bankAfile.getSize();
                        System.arraycopy(rom, 0, memory, base, length);
                        systemContext.logInfoEvent("Loaded a file for bank A, " + property);
                    }
                    //
                    bankAInstalled = bankAROMInstalled | bankARAMInstalled;
                }
            } else {
                bankAInstalled = false;
            }
            //
            // Bank B memory
            String epromBType = null;
            String bankBEnabled = cardProperties.get("BankBEnabled");
            if (bankBEnabled.equalsIgnoreCase("true")) {
                bankBBase = Utilities.getHexValue(cardProperties.getOrDefault("BankBAddress", "D000"));
                epromBType = cardProperties.get("BankBType");
                if (null != epromBType) {
                    if ("2708".equals(epromBType)) {
                        bankBEnd = bankBBase + 4096;
                        bankBROMInstalled = true;
                        bankBRAMInstalled = false;
                    } else {
                        if ("2716".equals(epromBType)) {
                            bankBEnd = bankBBase + 8192;
                            bankBROMInstalled = true;
                            bankBRAMInstalled = false;
                        } else {
                            if ("4118".equals(epromBType)) {
                                bankBEnd = bankBBase + 4096;
                                bankBROMInstalled = false;
                                bankBRAMInstalled = true;
                            } else {
                                if ("6116".equals(epromBType)) {
                                    bankBEnd = bankBBase + 8192;
                                    bankBROMInstalled = false;
                                    bankBRAMInstalled = true;
                                } else {
                                    systemContext.logWarnEvent("Unknown memory device type specified in bank B - " + epromBType);
                                }
                            }
                        }
                    }
                    //
                    property = cardProperties.get("BankBFile");
                    if (null != property) {
                        MemoryChunk bankBfile = fileHandler.readHexDumpFile(property);
                        short[] rom = bankBfile.getMemoryChunk();
                        int base = bankBfile.getBase();
                        int length = bankBfile.getSize();
                        System.arraycopy(rom, 0, memory, base, length);
                        systemContext.logInfoEvent("Loaded a file for bank B, " + property);
                    }
                    //
                    bankBInstalled = bankBROMInstalled | bankBRAMInstalled;
                }
            } else {
                bankBInstalled = false;
            }
            //
            systemContext.logInfoEvent("Nascom 2 Mainboard Memory Configuration");
            if (monitorROMInstalled) {
                systemContext.logInfoEvent("Memory range " + Utilities.getWord(monitorROMBase) + " to " + Utilities.getWord(monitorROMEnd - 1) + " installed as monitor");
            }
            if (basicROMInstalled) {
                systemContext.logInfoEvent("Memory range " + Utilities.getWord(basicROMBase) + " to " + Utilities.getWord(basicROMEnd - 1) + " installed as 8K device");
            }
            systemContext.logInfoEvent("Memory range " + Utilities.getWord(videoRAMBase) + " to " + Utilities.getWord(videoRAMEnd - 1) + " installed as video");
            systemContext.logInfoEvent("Memory range " + Utilities.getWord(scratchpadRAMBase) + " to " + Utilities.getWord(scratchpadRAMEnd - 1) + " installed as scratchpad");
            if (bankAInstalled) {
                systemContext.logInfoEvent("Memory range bank A " + Utilities.getWord(bankABase) + " to " + Utilities.getWord(bankAEnd - 1) + " using " + epromAType + " devices");
            }
            if (bankBInstalled) {
                systemContext.logInfoEvent("Memory range bank B " + Utilities.getWord(bankBBase) + " to " + Utilities.getWord(bankBEnd - 1) + " using " + epromBType + " devices");
            }
            //
            // precalculate onboard memory
            //
            for (int address = 0; address < MAX_MEMORY; address++) {
                bankAValid[address] = isBankARAM(address) || isBankAROM(address);
                bankBValid[address] = isBankBRAM(address) || isBankBROM(address);
                monitorValid[address] = isMonitorROM(address);
                videoValid[address] = isVideoRAM(address);
                basicROMValid[address] = isBasicROM(address);
                workspaceValid[address] = isWorkspaceRAM(address);
                ramValid[address] = isBankARAM(address) || isBankBRAM(address) || videoValid[address] || workspaceValid[address];
                romValid[address] = isBankAROM(address) || isBankBROM(address) || monitorValid[address] || basicROMValid[address];
                onboard[address] = bankAValid[address] || bankBValid[address] || monitorValid[address] || videoValid[address] || basicROMValid[address] || workspaceValid[address];
            }
        } catch (IOException ex) {
            systemContext.logErrorEvent("Error loading file into memory, <" + ex.getMessage() + ">");
        }
    }

    private boolean isWorkspaceRAM(int address) {
        return (scratchpadRAMBase <= address) && (address < scratchpadRAMEnd);
    }

    private boolean isVideoRAM(int address) {
        return (videoRAMBase <= address) && (address < videoRAMEnd);
    }

    private boolean isBasicROM(int address) {
        return basicROMInstalled && (basicROMBase <= address) && (address < basicROMEnd);
    }

    private boolean isMonitorROM(int address) {
        return monitorROMInstalled && (monitorROMBase <= address) && (address < monitorROMEnd);
    }

    private boolean isBankARAM(int address) {
        return bankARAMInstalled && (bankABase <= address) && (address < bankAEnd);
    }

    private boolean isBankAROM(int address) {
        return bankAROMInstalled && (bankABase <= address) && (address < bankAEnd);
    }

    private boolean isBankBRAM(int address) {
        return bankBRAMInstalled && (bankBBase <= address) && (address < bankBEnd);
    }

    private boolean isBankBROM(int address) {
        return bankBROMInstalled && (bankBBase <= address) && (address < bankBEnd);
    }

    /**
     * Tell the memory subsystem about the standard 1K display
     *
     * @param displayDevice The 1K display device
     */
    void setDisplayDevice(StandardDisplayDevice displayDevice) {
        this.displayDevice = displayDevice;
    }

    /*
     * read a byte from memory
     */
    @Override
    public final int readByte(int address) {
        if (onboard[address]) {
            return memory[address];
        } else {
            return nasBus.memoryRead(address);
        }
    }

    /*
     * read a word from memory, LSB, MSB order
     */
    @Override
    public final int readWord(int address) {
        return readByte(address) + readByte((address + 1) & MAX_ADDRESS) * 256;
    }

    /*
     * Write a byte into ram
     */
    @Override
    public final void writeByte(int address, int data) {
        if (onboard[address]) {
            if (ramValid[address]) {
                memory[address] = (short) data;
            }
            if (videoValid[address]) {
                displayDevice.writeByte(address - videoRAMBase, data);
            }
        } else {
            nasBus.memoryWrite(address, data, false);
        }
    }

    /*
     * write a word into memory, LSB, MSB order
     */
    @Override
    public final void writeWord(int address, int data) {
        writeByte(address, (data & 0x00FF));
        address = (address + 1) & MAX_ADDRESS;
        data = (data >>> 8);
        writeByte(address, data);
    }

    /**
     * Will a read to an address cause RAMDIS (i.e. ROM) to be asserted
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    boolean assertRAMDIS(int address) {
        return false;
    }

    /**
     * Does the card support RAM at the address specified
     *
     * @param address The address to test
     * @return True is RAM, else false
     */
    boolean isRAM(int address) {
        return ramValid[address];
    }

    /**
     * Does the card support ROM at the address specified
     *
     * @param address The address to test
     * @return True is ROM, else false
     */
    boolean isROM(int address) {
        return romValid[address];
    }

    /**
     * Identify the NAS BUS to the card
     *
     * @param nasBus The NAS BUS controller object
     */
    void setNasBus(INasBus nasBus) {
        this.nasBus = nasBus;
    }
}