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

import com.codingrodent.emulator.cards.ICard;
import com.codingrodent.emulator.emulator.SystemContext;
import com.codingrodent.emulator.nas80Bus.INasBus;

import java.awt.event.ActionEvent;
import java.util.Map;

/**
 *
 */
public abstract class BaseCard implements ICard, INasBus {
    protected final static int BUS_FLOAT = 0x7F;
    //
    protected final SystemContext systemContext = SystemContext.createInstance();
    protected Map<String, String> cardProperties;
    protected INasBus nasBus;
    //
    private String cardName;

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
        this.nasBus = nasBus;
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
     * Does the card support RAM at the address specified
     *
     * @param address The address to test
     * @return True is RAM, else false
     */
    @Override
    public boolean isRAM(int address) {
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
        return false;
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
        return BUS_FLOAT;
    }

    /**
     * A processor halt as occurred
     */
    @Override
    public void halt() {
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
