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

package com.codingrodent.emulator.cards.nascommemory;

import com.codingrodent.emulator.cards.ICard;
import com.codingrodent.emulator.emulator.SystemContext;
import com.codingrodent.emulator.nas80Bus.INasBus;
import com.codingrodent.emulator.utilities.*;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Map;

public class Nascom32KRAMA implements ICard, INasBus {

    private final static int MEMORY_SIZE = 64 * 1024;
    private final short[] memory = new short[MEMORY_SIZE];
    private final boolean[] ramValid = new boolean[MEMORY_SIZE];
    private final boolean[] romValid = new boolean[MEMORY_SIZE];
    private final SystemContext systemContext = SystemContext.createInstance();
    private String cardName;
    private Map<String, String> cardProperties;
    private boolean romInstalled = false;

    public Nascom32KRAMA() {
    }

    /**
     * One off initialisation carried out after card object creation
     */
    @Override
    public void initialise() {
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
                epromBase = eprom.getBase();
                int length = eprom.getSize();
                epromTopAddress = epromBase + length;
                if (4096 != length) {
                    systemContext.logWarnEvent("The EPROM is not 4K bytes (" + length + " bytes found)");
                }
                System.arraycopy(rom, 0, memory, epromBase, length);
                systemContext.logInfoEvent("Loaded a file for EPROM, " + filename);

            } catch (IOException ex) {
                String msg = "Unable to load the video ROM, <" + ex.getMessage() + ">";
                systemContext.logFatalEvent(msg);
                throw new RuntimeException(msg);
            }
        }
        if (romInstalled) {
            for (int address = baseAddress; address < MEMORY_SIZE; address++) {
                romValid[address] = romInstalled && (epromBase <= address) && (address < epromTopAddress);
                ramValid[address] = (baseAddress <= address) && (address < topAddress);
            }
        }
        reset();
    }

    /**
     * Set any card specific parameters
     *
     * @param cardProperties Property list
     */
    @Override
    public void setCardProperties(Map<String, String> cardProperties) {
        this.cardProperties = cardProperties;
    }

    /**
     * Identify the NAS BUS to the card
     *
     * @param nasBus The NAS BUS controller object
     */
    @Override
    public void setNasBus(INasBus nasBus) {
        //this.nasBus = nasBus;
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
     * Does the card support input at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isInputPort(int address) {
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
        return false;
    }

    /**
     * Get a human readable name for the card
     *
     * @return Card name string
     */
    @Override
    public String getCardName() {
        return cardName;
    }

    /**
     * Set a human readable name for the card
     *
     * @param cardName Card name string
     */
    @Override
    public void setCardName(String cardName) {
        this.cardName = cardName;
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
     * Report if the card is a CPU card
     *
     * @return True if a cpu, else false
     */
    @Override
    public boolean isCPU() {
        return false;
    }

    /**
     * Reset the card page mode setup
     */
    @Override
    public void reset() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    /**
     * Write a byte into ram
     *
     * @param address The address to be written to
     * @param data    The byte to be written
     */
    @Override
    public boolean memoryWrite(int address, int data, boolean ramdis) {
        if ((!ramdis) && ramValid[address]) {
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
            return 0x7F;
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
    }

    /**
     * Read data from the io bus
     *
     * @param address Address to read from
     * @return Byte of data
     */
    @Override
    public int ioRead(int address) {
        return 0X7F;
    }

    /**
     * A processor halt as occurred
     */
    @Override
    public void halt() {
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
    public boolean assertRAMDIScapable(int address) {
        return romValid[address];
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

}