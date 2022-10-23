/*
 * MIT License
 *
 * Copyright (c) 2018 PeriataTech
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

package com.codingrodent.emulator.cards.bankswitch;

import com.codingrodent.emulator.cards.common.BaseCard;
import com.codingrodent.emulator.utilities.*;

import java.io.IOException;
import java.util.Arrays;

/**
 * Implementation of a generic bank-switching EPROM/RAM card.
 * Base addresses are required to be aligned to the size of the card (e.g. 4K cards
 * must be aligned at a 4K boundary, and so on). Cards with 4K, 8K and 16K of address
 * space are supported.  Contents are chunked into 4K divisions, and either an
 * entire bank or just a 4K division of a bank may be assigned to RAM or an EPROM.
 * Divisions of a bank may be set up to mirror the same location in a lower-numbered
 * bank (e.g. so that a bank with some EPROM's but not enough to fill the address space
 * may fill the remaining space with RAM from a different bank, or to allow an EPROM to
 * exist in multiple banks).
 * <p>
 * Config entries:
 * <ul><li>BaseAddress - hexadecimal base address for memory block (default C000)</li>
 *     <li>Size - 4K, 8K, 16K (default 8K)</li>
 *     <li>BankSelPort - IO port for selecting banks (default 8)</li>
 *     <li>BankSelBitShift - number of bits to shift values output to the IO port to the right, thus allowing
 *         multiple cards to share the same port (default 0)</li>
 *     <li>BankCount - number of banks supported; must be either 2, 4, 8 or 16 (default 8).
 *         Additional bits in the value output to the selection port are ignored.</li>
 *     <li>Bank<i>nn</i>.RAMEnabled - if "true" causes RAM to be allocated to the entire bank</li>
 *     <li>Bank<i>nn</i>.ROMEnabled - if "true" causes an EPROM to be loaded for the entire bank</li>
 *     <li>Bank<i>nn</i>.ROM - filename for EPROM used for entire bank</li>
 *     <li>Bank<i>nn</i>.Div<i>nn</i>.*** - same options as for just Bank<i>nn</i>, but applying only to a single 4K
 *         division.</li>
 *     <li>Bank<i>nn</i>.Div<i>nn</i>.MirrorBank - specifies that this bank division mirrors the content of the
 *         same division in a bank with a lower number.</li>
 * </ul>
 * <p>
 * Bank and division numbers are both zero-based.
 */
public class BankSwitcher extends BaseCard {

    private int addressBits, addressBitsMask;
    private int bankSelPort, bankSelBitMask, bankSelBitShift, bankCount;
    private int currentBank;

    private short[][][] memory;
    private boolean[][] ramValid;
    private boolean[][] romValid;
    private boolean[] romValidAnyBank;

    private final static int DIV_SIZE = 4096;
    private final static int DIV_MASK = DIV_SIZE - 1;

    /**
     * One off initialisation carried out after card object creation
     */
    @Override
    public void initialise() {
        addressBits = Utilities.getHexValue(cardProperties.getOrDefault("BaseAddress", "C000"));
        String size = cardProperties.getOrDefault("Size", "8K");
        int bankDivisions;
        switch (size) {
            case "4K" -> {
                addressBitsMask = ~0x0FFF;
                bankDivisions = 1;
            }
            case "8K" -> {
                addressBitsMask = ~0x1FFF;
                bankDivisions = 2;
            }
            case "16K" -> {
                addressBitsMask = ~0x3FFF;
                bankDivisions = 4;
            }
            default -> {
                String msg = "Unknown size for bank switcher: " + size;
                systemContext.logFatalEvent(getCardDetails() + ": " + msg);
                throw new RuntimeException(msg);
            }
        }
        bankSelPort = Utilities.getHexValue(cardProperties.getOrDefault("BankSelPort", "8"));
        bankSelBitShift = Utilities.getHexValue(cardProperties.getOrDefault("BankSelBitShift", "0"));
        bankCount = Utilities.getHexValue(cardProperties.getOrDefault("BankCount", "8"));
        memory = new short[bankCount][][];
        ramValid = new boolean[bankCount][];
        romValid = new boolean[bankCount][];

        switch (bankCount) {
            case 2 -> bankSelBitMask = 1 << bankSelBitShift;
            case 4 -> bankSelBitMask = 3 << bankSelBitShift;
            case 8 -> bankSelBitMask = 7 << bankSelBitShift;
            case 16 -> bankSelBitMask = 15 << bankSelBitShift;
            default -> {
                String msg = "Invalid number of banks in bank switcher: " + bankCount;
                systemContext.logFatalEvent(getCardDetails() + ": " + msg);
                throw new RuntimeException(msg);
            }
        }
        if (bankSelBitMask > 255) {
            String msg = "Illegal shift value for bank switcher with " + bankCount + " banks: " + bankSelBitShift;
            systemContext.logFatalEvent(getCardDetails() + ": " + msg);
            throw new RuntimeException(msg);
        }

        for (int bank = 0; bank < bankCount; bank++) {
            ramValid[bank] = new boolean[bankDivisions];
            romValid[bank] = new boolean[bankDivisions];
            memory[bank] = new short[bankDivisions][];
            if ("true".equalsIgnoreCase(cardProperties.get("Bank" + bank + ".RAMEnabled"))) {
                for (int div = 0; div < bankDivisions; div++) {
                    ramValid[bank][div] = true;
                    memory[bank][div] = new short[DIV_SIZE];
                }
                systemContext.logInfoEvent(getCardDetails() + " bank " + bank + " RAM enabled");
                continue;
            }
            if ("true".equalsIgnoreCase(cardProperties.get("Bank" + bank + ".ROMEnabled"))) {
                short[][] romContent = loadRom(cardProperties.get("Bank" + bank + ".ROM"));
                if (romContent.length != bankDivisions) {
                    String msg = "EPROM content should be exactly " + bankDivisions * DIV_SIZE + " bytes but was " + romContent.length * romContent[0].length;
                    systemContext.logFatalEvent(getCardDetails() + ": " + msg);
                    throw new RuntimeException(msg);
                }
                for (int div = 0; div < bankDivisions; div++) {
                    romValid[bank][div] = true;
                    memory[bank][div] = romContent[div];
                }
                systemContext.logInfoEvent(getCardDetails() + " bank " + bank + " EPROM enabled");
                continue;
            }
            for (int div = 0; div < bankDivisions; div++) {
                String base = "Bank" + bank + ".Div" + div + ".";
                if ("true".equalsIgnoreCase(cardProperties.get(base + "RAMEnabled"))) {
                    ramValid[bank][div] = true;
                    memory[bank][div] = new short[DIV_SIZE];
                    systemContext.logInfoEvent(getCardDetails() + " bank " + bank + " div " + div + " RAM enabled");
                    continue;
                }
                if ("true".equalsIgnoreCase(cardProperties.get(base + "ROMEnabled"))) {
                    short[][] romContent = loadRom(cardProperties.get(base + "ROM"));
                    if (romContent.length != 1) {
                        String msg = "EPROM content should be exactly " + DIV_SIZE + " bytes but was " + romContent.length * romContent[0].length;
                        systemContext.logFatalEvent(getCardDetails() + ": " + msg);
                        throw new RuntimeException(msg);
                    }
                    romValid[bank][div] = true;
                    memory[bank][div] = romContent[0];
                    systemContext.logInfoEvent(getCardDetails() + " bank " + bank + " div " + div + " EPROM enabled");
                    continue;
                }
                if (cardProperties.get(base + "MirrorBank") != null) {
                    int srcBank = Integer.parseInt(cardProperties.get(base + "MirrorBank"));
                    if (srcBank < 0 || srcBank >= bank) {
                        String msg = "Memory can only be mirrored from banks with lower numbers (bank " + bank + " div " + div + " cannot mirror " + srcBank + ")";
                        systemContext.logFatalEvent(getCardDetails() + ": " + msg);
                        throw new RuntimeException(msg);
                    }
                    romValid[bank][div] = romValid[srcBank][div];
                    ramValid[bank][div] = ramValid[srcBank][div];
                    memory[bank][div] = memory[srcBank][div];
                    systemContext.logInfoEvent(getCardDetails() + " bank " + bank + " div " + div + " mirroring bank " + srcBank);
                }
            }
        }


        // calculate whether each div may contain ROM regardless of bank setting,
        // i.e. true if a ROM may be switched in to the location.
        romValidAnyBank = new boolean[bankDivisions];
        for (int div = 0; div < bankDivisions; div++)
            for (int bank = 0; !romValidAnyBank[div] && bank < bankCount; bank++)
                romValidAnyBank[div] = romValid[bank][div];
        reset();
    }

    private short[][] loadRom(String filename) {
        try {
            FileHandler fileHandler = new FileHandler();
            MemoryChunk eprom = fileHandler.readHexDumpFile(filename);
            short[] rom = eprom.getMemoryChunk();
            int length = eprom.getSize();
            if (length % DIV_SIZE != 0) {
                String msg = "The EPROM (" + filename + ") is not a multiple of 4K bytes (" + length + " bytes found)";
                systemContext.logFatalEvent(msg);
                throw new RuntimeException(msg);
            }
            short[][] chunks = new short[length / DIV_SIZE][];
            for (int i = 0; i < chunks.length; i++)
                chunks[i] = Arrays.copyOfRange(rom, i * DIV_SIZE, (i + 1) * DIV_SIZE);

            return chunks;
        } catch (IOException ex) {
            String msg = "Unable to load the EPROM (" + filename + "), <" + ex.getMessage() + ">";
            systemContext.logFatalEvent(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Get the details of the card by the author
     *
     * @return Card name string
     */
    @Override
    public String getCardDetails() {
        return "Bank Switcher @" + Integer.toHexString(addressBits);
    }

    /**
     * Does the card support RAM at the address specified
     *
     * @param address The address to test
     * @return True is RAM, else false
     */
    @Override
    public boolean isRAM(int address) {
        int div = divForAddress(address);
        if (div < 0) return false;
        // this result is cached, so return true if any bank has RAM at this address
        for (int i = 0; i < bankCount; i++)
            if (ramValid[i][div]) return true;
        return false;
    }

    /**
     * Does the card support ROM at the address specified
     *
     * @param address The address to test
     * @return True is ROM, else false
     */
    @Override
    public boolean isROM(int address) {
        int div = divForAddress(address);
        if (div < 0)
            return false;
        return romValidAnyBank[div];
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
        int div = divForAddress(address);
        if (div >= 0 && (!ramdis) && ramValid[currentBank][div]) {
            memory[currentBank][div][address & DIV_MASK] = (short) data;
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
        int div = divForAddress(address);
        if (div >= 0 && (((!ramdis) && ramValid[currentBank][div]) || (romValid[currentBank][div]))) {
            return memory[currentBank][div][address & DIV_MASK];
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
        int div = divForAddress(address);
        if (div >= 0 && (ramValid[currentBank][div] || romValid[currentBank][div])) {
            return memory[currentBank][div][address & DIV_MASK];
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
        int div = divForAddress(address);
        return div >= 0 && romValid[currentBank][div];
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
        int div = divForAddress(address);
        return div >= 0 && romValidAnyBank[div];
    }

    /**
     * Determine the division number for a given address, or -1 if the address
     * is not decoded by this card.
     */
    private int divForAddress(int address) {
        //System.out.println ("divForAddress: " + Integer.toHexString(address) + " " + Integer.toHexString(addressBitsMask) + " " + Integer.toHexString(address & addressBitsMask) + " " + Integer.toHexString(addressBits));
        if ((address & addressBitsMask) != addressBits)
            return -1;
        else
            return (address & ~addressBitsMask) / DIV_SIZE;
    }

    /**
     * Does the card support output at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isOutputPort(int address) {
        return address == bankSelPort;
    }

    /**
     * Write data to the io bus
     *
     * @param address Address to write to
     * @param data    Data to be written
     */
    @Override
    public void ioWrite(int address, int data) {
        if (address == bankSelPort) {
            currentBank = (data & bankSelBitMask) >> bankSelBitShift;
            systemContext.logInfoEvent(getCardDetails() + " selected bank " + currentBank);
        }
    }


}
