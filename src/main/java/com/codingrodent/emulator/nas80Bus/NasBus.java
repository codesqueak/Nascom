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

package com.codingrodent.emulator.nas80Bus;

import com.codingrodent.emulator.cards.ICard;
import com.codingrodent.emulator.emulator.SystemContext;
import com.codingrodent.emulator.utilities.Utilities;

class NasBus implements INasBus {

    private final SystemContext context;
    private final INasBus[] ramDisPool;
    private final INasBus[] cardPool;
    private final INasBus[] memoryPool;
    private INasBus[][] ioRead;
    private INasBus[][] ioWrite;
    private int cardsLoaded;
    private int ramDisCards;
    private int memoryCards;

    NasBus() {
        context = SystemContext.createInstance();
        ramDisPool = new INasBus[16];
        cardPool = new INasBus[16];
        memoryPool = new INasBus[16];

        cardsLoaded = 0;
        ramDisCards = 0;
        memoryCards = 0;
    }

    /**
     * Initialise the bus controller. It extracts information from the loaded card set in the CardController class.
     *
     * @param cardController Information on the card set loaded into the bus
     */
    void initialise(CardController cardController) {
        //
        cardsLoaded = cardController.getCardsLoaded();
        int memorySlots = cardController.getMemorySlots();
        int activePorts = cardController.getActivePorts();
        int segmentSize = cardController.getSegmentSize();
        //
        ioRead = new INasBus[cardsLoaded][activePorts];
        ioWrite = new INasBus[cardsLoaded][activePorts];
        //
        /* set the RAM decode slots. Any memory location may be processed by multiple cards */
        context.logDebugEvent("Card memory and I/O configuration");
        context.logDebugEvent("");
        for (int slot = 0; slot < cardsLoaded; slot++) {
            ICard card = cardController.getCard(slot);
            INasBus cardBus = cardController.getCardNasBus(slot);
            cardPool[slot] = cardBus;
            char[] memory = new char[memorySlots];
            StringBuilder ramdis = new StringBuilder(32);
            context.logDebugEvent("Card slot " + slot + " (" + card.getCardDetails() + ") supports the following:");
            context.logDebugEvent("Memory, R=ROM, W=RAM");
            context.logDebugEvent("0123456789012345678901234567890123456789012345678901234567890123");
            context.logDebugEvent("0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F   ");
            //
            boolean ramDisPresent = false;
            boolean memoryPresent = false;
            for (int i = 0; i < memorySlots; i++) {
                memory[i] = '-';
                if (card.isRAM(i * segmentSize)) {
                    memory[i] = 'W';
                    memoryPresent = true;
                }
                if (card.isROM(i * segmentSize)) {
                    memory[i] = 'R';
                    memoryPresent = true;
                }
                if (cardBus.assertRAMDISCapable(i * segmentSize)) {
                    ramdis.append('*');
                    ramDisPresent = true;
                } else {
                    ramdis.append('-');
                }
            }
            if (ramDisPresent) {
                ramDisPool[ramDisCards++] = cardBus;
            }
            if (0 != slot) {
                if (memoryPresent) {
                    memoryPool[memoryCards++] = cardBus;
                }
            }
            //
            context.logDebugEvent(new String(memory));
            context.logDebugEvent("RAMDIS signal asserted at blocks");
            context.logDebugEvent(ramdis.toString());

            /* set the I/O decode slots. */
            StringBuilder inputPorts = new StringBuilder("Input Ports  : ");
            StringBuilder outputPorts = new StringBuilder("Output Ports : ");
            for (int port = 0; port < activePorts; port++) {
                if (card.isInputPort(port)) {
                    ioRead[slot][port] = cardBus;
                    inputPorts.append(Utilities.getByte(port)).append(' ');
                }
                if (card.isOutputPort(port)) {
                    ioWrite[slot][port] = cardBus;
                    outputPorts.append(Utilities.getByte(port)).append(' ');
                }
            }
            context.logDebugEvent(inputPorts.toString());
            context.logDebugEvent(outputPorts.toString());
            context.logDebugEvent(" ");
        }
    }

    /**
     * Write data to the memory bus, not including slot 0 which is supporting the CPU
     *
     * @param address Address to write to
     * @param data    Data to be written
     * @return signals memory write abort
     */
    @Override
    public boolean memoryWrite(int address, int data, boolean ramdis) {
        boolean writeAbort = false;

        ramdis = getRAMDIS(address);
        for (int slot = 0; slot < memoryCards; slot++) {
            writeAbort = memoryPool[slot].memoryWrite(address, data, ramdis);
            if (writeAbort) {
                break;
            }
        }
        return writeAbort;
    }

    /**
     * Read data from the memory bus
     *
     * @param address Address to read from
     * @param ramdis  RAMDIS bus signal
     * @return Byte of data
     */
    @Override
    public int memoryRead(int address, boolean ramdis) {
        int lastValue;
        int readValue = 0x7F;
        boolean memoryFound = false;
        //
        for (int slot = 0; slot < memoryCards; slot++) {
            lastValue = memoryPool[slot].memoryRead(address, ramdis);
            // see if two cards have responded
            if (NO_MEMORY_PRESENT != lastValue) {
                if (memoryFound) {
                    return 0xFF; // two cards responded
                } else {
                    memoryFound = true;
                    readValue = lastValue;
                }
            }
        }
        // only zero or one cards responded
        return readValue;
    }

    /**
     * Read data from the memory bus
     *
     * @param address Address to read from
     * @return Byte of data
     */
    @Override
    public int memoryRead(int address) {
        return memoryRead(address, getRAMDIS(address));
    }

    /**
     * Write data to the io bus
     *
     * @param address Address to write to
     * @param data    Data to be written
     */
    @Override
    public void ioWrite(int address, int data) {
        address = address & 0x00FF;
        for (int slot = 1; slot < cardsLoaded; slot++) {
            INasBus bus = ioWrite[slot][address];
            if (null != bus) {
                bus.ioWrite(address, data);
            }
        }
    }

    /**
     * Read data from the io bus
     *
     * @param address Address to read from
     * @return Byte of data
     */
    @Override
    public int ioRead(int address) {
        address = address & 0x00FF;
        int value = NO_MEMORY_PRESENT;
        int readValue;

        //System.out.println("I/O read : " + utilities.getWord(address));

        for (int slot = 1; slot < cardsLoaded; slot++) {
            INasBus bus = ioRead[slot][address];
            if (null != bus) {
                readValue = bus.ioRead(address);
                // see if two cards have responded
                if (NO_MEMORY_PRESENT != readValue) {
                    if (NO_MEMORY_PRESENT == value) {
                        value = readValue;
                    } else {
                        return 0x7F; // two cards responded so duff i/o value
                    }
                }
            }
        }
        //  zero or one cards responded
        if (NO_MEMORY_PRESENT == value) {
            //System.out.println("Unclaimed I/O read : " + utilities.getWord(address));
            return 0x7F;
        } else {
            return value;
        }
    }

    /**
     * Execute a card reset
     */
    @Override
    public void reset() {
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
        return false;
    }

    /**
     * Will a read to an address cause RAMDIS (i.e. ROM) to be asserted
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDISCapable(int address) {
        return false;
    }

    /**
     * Recover the number of T states executed by the CPU
     *
     * @return long
     */
    @Override
    public long getClock() {
        return cardPool[0].getClock();
    }

    /**
     * Set the number of T states executed by the CPU
     *
     * @param t long
     */
    @Override
    public void setClock(long t) {
        cardPool[0].setClock(t);
    }

    /**
     * Write data to the memory bus
     *
     * @param address Address to write to
     * @param data    Data to be written
     */
    void memoryWriteAll(int address, int data) {
        for (int slot = 0; slot < cardsLoaded; slot++) {
            cardPool[slot].memoryWrite(address, data, false);
        }
    }

    /**
     * See if any card will assert the RAMDIS signal for the address
     *
     * @param address The address to check
     * @return True if RAMDIS will be asserted, else false
     */
    private boolean getRAMDIS(int address) {
        for (int slot = 0; slot < ramDisCards; slot++) {
            if (ramDisPool[slot].assertRAMDIS(address)) {
                return true;
            }
        }
        return false;
    }

}