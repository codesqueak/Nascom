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

package com.codingrodent.emulator.cards.cpu.nascom2;

import com.codingrodent.microprocessor.IBaseDevice;

class Keyboard implements IBaseDevice {

    private final static int resetMask = 0x0002;
    private final static int incMask = 0x0001;
    private final int[] buffer = {0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff};
    private final int[] portBuffer = {0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff};
    private int position;

    /*
     * The Nascom keyboard is painful.  A keystroke is a set of byte query / returns to
     * map a row and column
     */
    Keyboard() {
    }

    /**
     * Read data from an I/O port
     *
     * @param address The port to be read from
     * @return The 8 bit value at the request port address
     */
    @Override
    public int IORead(int address) {
        if (address == 0) {
            return portBuffer[position];
        }
        return 0;
    }

    /**
     * Write data to an I/O port
     *
     * @param address The port to be written to
     * @param data    The 8 bit value to be written
     */
    @Override
    public void IOWrite(int address, int data) {
        if (address == 0) {
            if ((resetMask & data) == resetMask) {
                resetBuffer();
            } else {
                if ((incMask & data) == incMask) {
                    position = (position + 1) % buffer.length;
                }
            }
        }
    }

    /**
     * Reset keyboard byte buffer
     */
    private void resetBuffer() {
        synchronized (buffer) {
            System.arraycopy(buffer, 0, portBuffer, 0, buffer.length);
        }
        position = 0;
    }

    /**
     * translate a keystroke into a table for the keyboard port to place bytes into local buffer
     *
     * @param keyData Key stroke data
     */
    void setKeyStroke(int[] keyData) {
        synchronized (buffer) {
            System.arraycopy(keyData, 0, buffer, 0, buffer.length);
        }
    }

}
