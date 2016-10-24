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

package com.codingrodent.emulator.cards.ram;

import com.codingrodent.emulator.cards.common.MemoryCard;
import com.codingrodent.emulator.utilities.Utilities;

public class Gemini64KRAM extends MemoryCard {

    private final short[] memory = new short[MEMORY_SIZE];
    private final boolean[] valid = new boolean[MEMORY_SIZE];
    private int page;
    private int readMask, writeMask;
    private boolean pageModeReadEnabled;
    private boolean pageModeWriteEnabled;

    /**
     * One off initialisation carried out after card object creation
     */
    public void initialise() {
        int baseAddress, topAddress;
        for (int address = 0; address < MEMORY_SIZE; address++) {
            memory[address] = 0;
        }
        baseAddress = Utilities.getHexValue(cardProperties.getOrDefault("BaseAddress", "1000"));
        String size = cardProperties.getOrDefault("Size", "64K");
        char pageID = cardProperties.getOrDefault("Page", "1").charAt(0);
        page = 1;
        readMask = 0x0001;
        writeMask = 0x0010;
        switch (pageID) {
            default: {
                page = 1;
                readMask = 0x0001;
                writeMask = 0x0010;
                break;
            }
            case '2': {
                page = 2;
                readMask = 0x0002;
                writeMask = 0x0020;
                break;
            }
            case '3': {
                page = 3;
                readMask = 0x0004;
                writeMask = 0x0040;
                break;
            }
            case '4': {
                page = 3;
                readMask = 0x0008;
                writeMask = 0x0080;
                break;
            }
        }
        //
        if (size.equals("16K")) {
            topAddress = baseAddress + 16 * 1024;
        } else {
            if (size.equals("32K")) {
                topAddress = baseAddress + 32 * 1024;
            } else {
                if (size.equals("48K")) {
                    topAddress = baseAddress + 48 * 1024;
                } else {
                    topAddress = baseAddress + 64 * 1024;
                }
            }
        }
        if (topAddress > 0xFFFF) {
            topAddress = 0xFFFF;
        }
        for (int address = 0; address < MEMORY_SIZE; address++) {
            memory[address] = 0;
            valid[address] = (baseAddress <= address) && (address < topAddress);
        }
        reset();
    }

    /**
     * Does the card support RAM at the address specified
     *
     * @param address The address to test
     * @return True is RAM, else false
     */
    @Override
    public boolean isRAM(int address) {
        return (valid[address]);
    }

    /**
     * Get the details of the card by the author
     *
     * @return Card name string
     */
    @Override
    public final String getCardDetails() {
        return "Gemini 64K RAM Card (G802) - Version 1.0";
    }

    /**
     * Reset the card
     */
    @Override
    public void reset() {
        if (1 == page) {
            pageModeReadEnabled = true;
            pageModeWriteEnabled = true;
        } else {
            pageModeReadEnabled = false;
            pageModeWriteEnabled = false;
        }
    }

    /**
     * Does the card support output at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isOutputPort(int address) {
        return (0xFF == address);
    }

    /**
     * Write a byte into ram
     *
     * @param address The address to be written to
     * @param data    The byte to be written
     * @return True if no more memory writes to be performed
     */
    @Override
    public boolean memoryWrite(int address, int data, boolean ramdis) {
        if ((!ramdis) && (pageModeWriteEnabled) && (valid[address])) {
            memory[address] = (short) data;
        }
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
        if ((!ramdis) && pageModeReadEnabled && valid[address]) {
            return memory[address];
        } else {
            return NO_MEMORY_PRESENT;
        }
    }

    /**
     * Read a byte from memory
     *
     * @param address Address to read from
     * @return Byte of data
     */
    @Override
    public int memoryRead(int address) {
        if (valid[address]) {
            return memory[address];
        } else {
            return NO_MEMORY_PRESENT;
        }
    }

    /**
     * Write data to the io bus
     *
     * @param address Address to write to
     * @param data    Data to be written
     */
    @Override
    public void ioWrite(int address, int data) {
        // page mode control
        pageModeReadEnabled = (0 != (data & readMask));
        pageModeWriteEnabled = (0 != (data & writeMask));
        systemContext.logDebugEvent("Page mode read  " + pageModeReadEnabled);
        systemContext.logDebugEvent("Page mode write " + pageModeWriteEnabled);
    }

}