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

public interface INasBus {

    int NO_MEMORY_PRESENT = -1;

    /**
     * Write data to the memory bus
     *
     * @param address Address to write to
     * @param data    Data to be written
     * @param ramdis  RAMDIS signal to support ROM overlapping RAM
     * @return True signals memory write abort
     */
    boolean memoryWrite(int address, int data, boolean ramdis);

    /**
     * Read data from the memory bus taking into account the RAMDIS signal
     *
     * @param address Address to read from
     * @param ramdis  RAMDIS signal to support ROM overlapping RAM
     * @return Byte of data
     */
    int memoryRead(int address, boolean ramdis);

    /**
     * Read data from the memory bus
     *
     * @param address Address to read from
     * @return Byte of data
     */
    int memoryRead(int address);

    /**
     * Write data to the io bus
     *
     * @param address Address to write to
     * @param data    Data to be written
     */
    void ioWrite(int address, int data);

    /**
     * Read data from the io bus
     *
     * @param address Address to read from
     * @return Byte of data
     */
    int ioRead(int address);

    /**
     * Execute a card reset
     */
    void reset();

    /**
     * A processor halt as occurred
     */
    void halt();

    /**
     * Will a read to an address will cause RAMDIS (i.e. ROM) to be asserted. For example, a paged out ROM
     * will not cause RAMDIS to be asserted.
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    boolean assertRAMDIS(int address);

    /**
     * Will a read to an address may cause RAMDIS (i.e. ROM) to be asserted. For example, a paged out ROM
     * will cause RAMDIS to be asserted. That is, RAMDIS may occur at this address.
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    boolean assertRAMDIScapable(int address);

    /**
     * Recover the number of T states executed by the CPU
     *
     * @return long
     */
    long getClock();

    /**
     * Set the number of T states executed by the CPU
     *
     * @param t long
     */
    void setClock(long t);

}
