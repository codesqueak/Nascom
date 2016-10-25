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
import com.codingrodent.emulator.utilities.*;

import java.io.IOException;

public class Nascom32KRAMA extends MemoryCard {

    private final short[] memory = new short[MEMORY_SIZE];
    private final boolean[] ramValid = new boolean[MEMORY_SIZE];
    private final boolean[] romValid = new boolean[MEMORY_SIZE];

    public Nascom32KRAMA() {
    }

    /**
     * One off initialisation carried out after card object creation
     */
    @Override
    public void initialise() {
        boolean romInstalled = false;
        int baseAddress, topAddress;
        baseAddress = Utilities.getHexValue(cardProperties.getOrDefault("BaseAddress", "1000"));
        String size = cardProperties.getOrDefault("Size", "32K");
        //
        if (size.equals("16K")) {
            topAddress = baseAddress + 16 * 1024;
        } else {
            topAddress = baseAddress + 32 * 1024;
        }
        //
        if (topAddress > 0xFFFF) {
            topAddress = 0xFFFF;
        }
        // EPROM ?
        int epromBase = 0;
        int epromTopAddress = 0;
        if ("true".equalsIgnoreCase(cardProperties.get("ROMEnabled"))) {
            try {
                romInstalled = true;
                FileHandler fileHandler = new FileHandler();
                String filename = cardProperties.get("ROM");
                MemoryChunk eprom = fileHandler.readHexDumpFile(filename);
                short[] rom = eprom.getMemoryChunk();
                epromBase = Utilities.getHexValue(cardProperties.getOrDefault("ROMAddress", "D000"));
                int length = eprom.getSize();
                epromTopAddress = epromBase + length;
                if (4096 != length) {
                    String msg = "The EPROM is not 4K bytes (" + length + " bytes found)";
                    systemContext.logFatalEvent(msg);
                    throw new RuntimeException(msg);
                }
                System.arraycopy(rom, 0, memory, epromBase, length);
                systemContext.logInfoEvent("Loaded a file for EPROM, " + filename);

            } catch (IOException ex) {
                String msg = "Unable to load the ROM, <" + ex.getMessage() + ">";
                systemContext.logFatalEvent(msg);
                throw new RuntimeException(msg);
            }
        }
        // Set RAM / ROM flags for faster access
        for (int address = 0; address < MEMORY_SIZE; address++) {
            romValid[address] = romInstalled && (epromBase <= address) && (address < epromTopAddress);
            ramValid[address] = (address >= baseAddress) && (address < topAddress);
        }
        reset();
    }

    /**
     * Get the details of the card by the author
     *
     * @return Card name string
     */
    @Override
    public String getCardDetails() {
        return "Nascom RAM 'A' Card - Version 1.0";
    }

    /**
     * Does the card support RAM at the address specified
     *
     * @param address The address to test
     * @return True is RAM, else false
     */
    @Override
    public boolean isRAM(int address) {
        return ramValid[address];
    }

    /**
     * Does the card support ROM at the address specified
     *
     * @param address The address to test
     * @return True is ROM, else false
     */
    @Override
    public boolean isROM(int address) {
        return romValid[address];
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
        if ((!ramdis) && ramValid[address]) {
            memory[address] = (short) data;
            return true;
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
        if (((!ramdis) && ramValid[address]) || (romValid[address])) {
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
        if (ramValid[address] || romValid[address]) {
            return memory[address];
        } else {
            return BUS_FLOAT;
        }
    }

    /**
     * Will a read to an address cause RAMDIS (i.e. ROM) to be asserted
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDIS(int address) {
        return romValid[address];
    }

    /**
     * Will a read to an address may cause RAMDIS (i.e. ROM) to be asserted.
     * For example, a paged out ROM will cause RAMDIS to be asserted. That is, RAMDIS may occur at this address.
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDISCapable(int address) {
        return romValid[address];
    }

}