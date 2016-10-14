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

/**
 *
 */
public abstract class MemoryCard extends BaseCard {

    protected final static int MEMORY_SIZE = 64 * 1024;

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
     * Will a read to an address may cause RAMDIS (i.e. ROM) to be asserted.
     * For example, a paged out ROM will cause RAMDIS to be asserted. That is, RAMDIS may occur at this address.
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDIScapable(int address) {
        return false;
    }

}
