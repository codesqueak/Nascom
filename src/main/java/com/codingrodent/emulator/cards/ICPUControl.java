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

public interface ICPUControl {

    /**
     * If the card contains a CPU, start execution
     */
    void start();

    /**
     * If the card contains a CPU, stop execution
     */
    void stop();

    /**
     * Restart the processor
     */
    void restart();

    /**
     * Set the CPU maximum speed in MHz. -1 gives maximum speed
     *
     * @param mhz int
     */
    void setSpeedMHz(int mhz);

    /**
     * Toggle the NMI line on the CPU
     */
    void toggleNMI();

    /**
     * Indicate when a block move is in progress, LDIR, CPDR etc. May be sampled during repetitive cycles of the instruction
     *
     * @return true represents a block move, else false if not executing
     */
    boolean blockMoveInProgress();
}
