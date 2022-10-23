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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class MemoryChunkTest {

    @Test
    public void getMemoryChunk() {
        MemoryChunk mc = new MemoryChunk();
        assertEquals(0, mc.getBase());
        assertEquals(0, mc.getSize());
        assertEquals(0, mc.getMemoryChunk().length);
    }

    @Test
    public void writeByte() {
        MemoryChunk mc = new MemoryChunk();
        mc.setBase(0x2000);
        int data = 0x00;
        for (int address = 0x2000; address < 0x3000; address++) {
            mc.writeByte(data++);
            data = data & 0x00FF;
        }
        //
        data = 0x00;
        for (int address = 0x2000; address < 0x3000; address++) {
            assertEquals(mc.readByte(address), data++);
            data = data & 0x00FF;
        }
        //
        assertEquals(0x1000, mc.getSize());
        short[] chunk = mc.getMemoryChunk();
        assertEquals(0x1000, chunk.length);
        data = 0x00;
        for (short i : chunk) {
            assertEquals(i, data++);
            data = data & 0x00FF;
        }
    }
}