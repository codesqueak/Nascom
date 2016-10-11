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

package com.codingrodent.emulator.utilities;

public class MemoryChunk {
    private final short[] memory;
    private int start, size, address;

    /**
     * A blank memory chunk, ready to be filled
     */
    public MemoryChunk() {
        memory = new short[65536];
        start = 0;
        size = 0;
        address = 0;
    }

    /**
     * Get the block of memory represented by this object
     *
     * @return The memory block
     */
    public short[] getMemoryChunk() {
        short[] returnMemory = new short[size];
        System.arraycopy(memory, start, returnMemory, 0, size);
        return returnMemory;
    }

    /**
     * Recover the start address of the memory block
     *
     * @return Address
     */
    public int getBase() {
        return start;
    }

    /**
     * Set the memory chunk base address
     *
     * @param address The base address
     */
    public void setBase(int address) {
        start = address;
        this.address = address;
    }

    /**
     * Recover the size of the memory block
     *
     * @return Size in bytes
     */
    public int getSize() {
        return size;
    }

    /**
     * Write a byte of data to a memory location.  The address and size is automatically incremented.
     *
     * @param data Data to write
     */
    public void writeByte(int data) {
        memory[address++] = (short) data;
        size++;
    }

    /**
     * Read a byte of data from memory
     *
     * @param fetchAddress The address to read from
     * @return The data byte
     */
    public short readByte(int fetchAddress) {
        return memory[fetchAddress];
    }

}
