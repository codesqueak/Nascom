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

package com.codingrodent.emulator.cards.nascom2cpu;

import com.codingrodent.emulator.emulator.SystemContext;
import com.codingrodent.emulator.emulator.display.WindowHandler;
import com.codingrodent.emulator.utilities.MemoryChunk;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

/*
 * convert video memory writes into bit displays
 */
class StandardDisplayDevice implements ActionListener {

    private final static int rowBits = 8;                                // 8 bits per row
    private final static int columnBits = 16;                                // 16 bits per column
    private final static int rows = 16;                                // screen rows
    private final static int columns = 48;                                // screen columns
    private final static int characters = 256;                                // character cells
    private final static int scale = 3;                                    // scale up character to make
    private final static int leftMargin = 0x000A;                            // non displayed cells on the
    private final static int rightMargin = 0x0007;                            // non displayed cells on the
    private final static int lineLength = 0x0040;                            // 64 bytes per line
    private final static int rightBorder = lineLength - rightMargin;
    private final Image[] icons = new Image[characters];        // pre-rendered each character
    private final JFrame screenFrame;                                        // standard video
    private final int[] shadowRAM = new int[1024];
    private final short[] rom;
    private final VolatileImage imageBufferVolatile;
    private final Graphics2D imageBufferGVolatile;

    /*
     * put up windows to hold the video display and register display
     */
    StandardDisplayDevice(MemoryChunk romFile) {
        int size = romFile.getSize();
        rom = new short[size];
        System.arraycopy(romFile.getMemoryChunk(), 0, rom, 0, size);
        /* the nascom 48*16 video display */
        screenFrame = SystemContext.createInstance().getPrimaryDisplay();
        //
        screenFrame.getContentPane().setPreferredSize(new Dimension(columns * rowBits * scale, rows * columnBits * scale));
        screenFrame.addWindowListener(new WindowHandler());
        // Set up the panel, enable this close and enable event handling
        // screenFrame.enableEvents(AWTEvent.WINDOW_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.ACTION_EVENT_MASK);
        // and show the window
        screenFrame.pack();
        screenFrame.setResizable(false);
        screenFrame.setVisible(true);
        //
        imageBufferVolatile = screenFrame.getContentPane().createVolatileImage(columns * rowBits * scale, rows * columnBits * scale);
        imageBufferGVolatile = imageBufferVolatile.createGraphics();
        //
        reset();
        //
        Timer timer = new Timer(50, this);
        timer.setCoalesce(true);
        timer.setRepeats(true);
        timer.start();
    }

    /**
     * Generate an image matching a character cell from the video ROM.
     *
     * @param cell The character cell to be used, 0..255
     * @return Generate an Image class representing one 8 x 16 pixel character
     */
    private Image getImageIcon(int cell) {
        int romAddress = cell * 16; // 16 bytes per character
        int offset = 0; // offset into byte buffer
        int[] pic = new int[rowBits * columnBits]; // pixel data byte buffer
        int charValue; // one byte from the ROM
        int bitMask;
        int pixelOff = 0x00000000; // xxRRGGBB
        int pixelOn = 0xFF00FF00; // xxRRGGBB
        for (int i = 0; i < columnBits; i++) {
            bitMask = 0x80; // scan 8 bits, mask = 10000000, shift right
            charValue = rom[romAddress + i];
            for (int bit = 0; bit < rowBits; bit++) {
                if ((bitMask & charValue) == 0) {
                    pic[offset++] = pixelOff;
                } else {
                    pic[offset++] = pixelOn;
                }
                bitMask = bitMask >>> 1;
            }
        }
        Image baseImage = screenFrame.createImage(new MemoryImageSource(rowBits, columnBits, pic, 0, rowBits));
        baseImage = baseImage.getScaledInstance(rowBits * scale, columnBits * scale, Image.SCALE_DEFAULT);
        return baseImage;
    }

    /**
     * Paint a character (int) at (row, column)
     *
     * @param row       The character row 0..15
     * @param column    The character column 0..47
     * @param character The character byte to write
     */
    private void printByte(int row, int column, int character) {
        row = (row + 1) % rows;
        imageBufferGVolatile.drawImage(icons[character], column * rowBits * scale, row * columnBits * scale, Color.BLACK, null);
    }

    /**
     * Reset the video RAM to all zero's then fill the visible area with test characters
     */
    private void reset() {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                printByte(row, column, 65); // A character
            }
        }
        for (int character = 0; character < characters; character++) {
            icons[character] = getImageIcon(character);
        }
    }

    /**
     * Write a byte into video ram. Address must be in the range 0..1023
     *
     * @param address Address of the character cell
     * @param data    The byte to write into video ram
     */
    void writeByte(int address, int data) {
        int column, row;
        if (shadowRAM[address] != data) {
            shadowRAM[address] = data;
            column = address & 0x003F;
            if (!((column < leftMargin) || (column > rightBorder))) {
                row = (address >>> 6);
                column = column - leftMargin;
                printByte(row, column, data);
            }
        }
    }

    /**
     * Add the keyboard handler to the mainboard
     *
     * @param keyboardHandler handler for key input
     */
    void addKeyboardHandler(KeyboardHandler keyboardHandler) {
        screenFrame.addKeyListener(keyboardHandler);
        screenFrame.addWindowFocusListener(keyboardHandler);
    }

    /**
     * Action for the timer. Paint the graphics buffer to the screen
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        screenFrame.getContentPane().getGraphics().drawImage(imageBufferVolatile, 0, 0, null);
    }
}