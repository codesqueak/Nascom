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

package com.codingrodent.emulator.cards;

import com.codingrodent.emulator.nas80Bus.INasBus;

import java.awt.event.ActionListener;
import java.util.Properties;

public interface ICard extends ActionListener {

    /**
     * One off initialisation carried out after card object creation
     */
    void initialise();

    /**
     * Set any card specific parameters
     *
     * @param cardProperties Property list
     */
    void setCardProperties(Properties cardProperties);

    /**
     * Identify the NAS BUS to the card
     *
     * @param nasBus The bus controller emulator
     */
    void setNasBus(INasBus nasBus);

    /**
     * Does the card support RAM at the address specified
     *
     * @param address The address to test
     * @return True is RAM, else false
     */
    boolean isRAM(int address);

    /**
     * Does the card support ROM at the address specified
     *
     * @param address The address to test
     * @return True is ROM, else false
     */
    boolean isROM(int address);

    /**
     * Does the card support input at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    boolean isInputPort(int address);

    /**
     * Does the card support output at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    boolean isOutputPort(int address);

    /**
     * Get a human readable name for the card
     *
     * @return Card name string
     */
    String getCardName();

    /**
     * Set a human readable name for the card
     *
     * @param cardName The card name string
     */
    void setCardName(String cardName);

    /**
     * Get the details of the card by the author
     *
     * @return Card name string
     */
    String getCardDetails();

    /**
     * Report if the card is a CPU card
     *
     * @return True if a cpu, else false
     */
    boolean isCPU();

    /**
     * Reset the card
     */
    void reset();

}
