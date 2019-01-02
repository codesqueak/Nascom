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

package com.codingrodent.emulator.cards.video;

import com.codingrodent.emulator.cards.common.BaseCard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.MemoryImageSource;

public class AVC extends BaseCard implements ActionListener {

    private final static int SCALE_SMALL = 3;
    private final static int SCALE_LARGE = 2;
    private final static int AVC_BLACK = 0xFF000000;
    private final static int AVC_RED = 0xFFFF0000;
    private final static int AVC_GREEN = 0xFF00FF00;
    private final static int AVC_BLUE = 0xFF0000FF;
    private final static int AVC_ROWS = 256;
    private final static int AVC_COLUMNS_SMALL = 384;
    private final static int AVC_COLUMNS_LARGE = AVC_COLUMNS_SMALL * 2;
    private final static int MEMORY_OFFSET = 2;                                    // zero pixel is at 8002H
    private final static int BYTES_PER_LINE = AVC_COLUMNS_SMALL / 8;
    private final static int MEMORY_SIZE = 0x4000;
    private final static int hHIGH_RES_BIT = 0x08;
    //
    private final int[] red = new int[MEMORY_SIZE];
    private final int[] green = new int[MEMORY_SIZE];
    private final int[] blue = new int[MEMORY_SIZE];
    private final int[] smallColourBuffer = new int[AVC_ROWS * AVC_COLUMNS_SMALL];
    private final int[] largeColourBuffer = new int[AVC_ROWS * AVC_COLUMNS_LARGE];
    private final int[] palette = new int[8];
    private final int[] CRTCRegisters = new int[255];                        // CRTC mirror registers
    //
    private boolean redSelected = false;
    private boolean greenSelected = false;
    private boolean blueSelected = false;
    private boolean redDisplay = false;
    private boolean greenDisplay = false;
    private boolean blueDisplay = false;
    private boolean pagedIn = false;
    private int memorySelected = 0;
    private boolean highResSelected = false;
    private int lastB2 = 0x80;
    private Image smallImage, largeImage;
    private int CRTCRegister = 0;                                    // register selected
    private AVCFrame avcFrame;

    /*
     * constructor forces general reset
     */
    public AVC() {
        reset();
    }

    /**
     * Reset the card to power on conditions
     */
    @Override
    public void initialise() {
        avcFrame = new AVCFrame("Nascom 2 AVC Model B");
        avcFrame.getContentPane().setBackground(Color.BLACK);
        //
        // Block colour palette settings
        palette[0] = AVC_BLACK;
        palette[1] = AVC_BLUE;
        palette[2] = AVC_GREEN;
        palette[3] = 0xFF00FFFF;
        palette[4] = AVC_RED;
        palette[5] = 0xFFFF00FF;
        palette[6] = 0xFFFFFF00;
        palette[7] = 0xFFFFFFFF; // white
        //
        //  Fill small colour buffer with default pattern
        for (int row = 0; row < AVC_ROWS; row++) {
            for (int column = 0; column < AVC_COLUMNS_SMALL; column++) {
                smallColourBuffer[row * AVC_COLUMNS_SMALL + column] = AVC_BLACK | row * column; // row * column;
            }
        }
        //  Fill large colour buffer with default pattern
        for (int row = 0; row < AVC_ROWS; row++) {
            for (int column = 0; column < AVC_COLUMNS_LARGE; column++) {
                largeColourBuffer[row * AVC_COLUMNS_LARGE + column] = AVC_BLACK | row * column; // row * column;
            }
        }
        // Create images for display
        smallImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(AVC_COLUMNS_SMALL, AVC_ROWS, smallColourBuffer, 0, AVC_COLUMNS_SMALL));
        largeImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(AVC_COLUMNS_LARGE, AVC_ROWS, largeColourBuffer, 0, AVC_COLUMNS_LARGE));
        //
        avcFrame.getContentPane().setPreferredSize(new Dimension(AVC_COLUMNS_SMALL * SCALE_SMALL, AVC_ROWS * SCALE_SMALL));
        smallImage.setAccelerationPriority(1.0f);
        //
        avcFrame.setResizable(false);
        avcFrame.pack();
        avcFrame.setVisible(true);
        //
        // Set image redisplay timer
        Timer timer = new Timer(50, this);
        timer.setCoalesce(true);
        timer.setRepeats(true);
        timer.start();
    }

    /**
     * Get the details of the card by the author
     *
     * @return Card name string
     */
    @Override
    public String getCardDetails() {
        return "Nascom AVC Model B - Version 3.0";
    }

    /**
     * Does the card support RAM at the address specified
     *
     * @param address The address to test
     * @return True is RAM, else false
     */
    @Override
    public boolean isRAM(final int address) {
        return (address >= 0x8000) && (address < 0xC000);
    }

    /**
     * Does the card support input at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isInputPort(final int address) {
        return (address == 0xB0) || (address == 0xB1) || (address == 0xB2);
    }

    /**
     * Does the card support output at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isOutputPort(final int address) {
        return isInputPort(address);
    }

    /*
     * Reset io mapped device
     */
    @Override
    public void reset() {
        // reset the control register mirrors
        for (int i = 0; i < CRTCRegisters.length; i++) {
            CRTCRegisters[i] = 0;
        }
        redSelected = false;
        greenSelected = false;
        blueSelected = false;
        redDisplay = false;
        greenDisplay = false;
        blueDisplay = false;
        memorySelected = 0;
        pagedIn = false;
        // clear the colour memory planes
        for (int i = 0; i < red.length; i++) {
            red[i] = 0;
            green[i] = 0;
            blue[i] = 0;
        }
    }

    /**
     * Repaint AVC image on demand - scheduled defined by timer
     * (non-Javadoc)
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (highResSelected) {
            largeImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(AVC_COLUMNS_LARGE, AVC_ROWS, largeColourBuffer, 0, AVC_COLUMNS_LARGE));
            largeImage.setAccelerationPriority(1.0f);
            Image doubleImage = largeImage.getScaledInstance(AVC_COLUMNS_LARGE * SCALE_LARGE, AVC_ROWS * SCALE_LARGE, Image.SCALE_FAST);
            avcFrame.getContentPane().getGraphics().drawImage(doubleImage, 0, 0, null);
        } else {
            smallImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(AVC_COLUMNS_SMALL, AVC_ROWS, smallColourBuffer, 0, AVC_COLUMNS_SMALL));
            smallImage.setAccelerationPriority(1.0f);
            Image doubleImage = smallImage.getScaledInstance(AVC_COLUMNS_SMALL * SCALE_SMALL, AVC_ROWS * SCALE_SMALL, Image.SCALE_FAST);
            avcFrame.getContentPane().getGraphics().drawImage(doubleImage, 0, 0, null);
        }
    }

    /**
     * Write data to CRTC controller
     *
     * @param address Address to write to
     * @param data    Data to be written
     */
    @Override
    public void ioWrite(final int address, final int data) {
        switch (address) {
            case 0xB0: {
                CRTCControlWrite(data);
                break;
            }
            case 0xB1: {
                CRTCDataWrite(data);
                break;
            }
            case 0xB2: {
                swapPagesWrite(data);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Read data from the CRTC controller
     *
     * @param address Address to read from
     * @return Byte of data
     */
    @Override
    public int ioRead(final int address) {
        switch (address) {
            case 0xB0: {
                return CRTCControlRead();
            }
            case 0xB1: {
                return CRTCDataRead();
            }
            case 0xB2: {
                return swapPagesRead();
            }
            default:
                return 0;
        }
    }

    /**
     * Write data to the AVC memory pages
     *
     * @param address Address to write to
     * @param data    Data to be written
     * @param ramdis  RAMDIS signal to support ROM overlapping RAM
     * @return True signals memory write abort
     */
    @Override
    public boolean memoryWrite(int address, final int data, final boolean ramdis) {
        if (isRAM(address)) {
            if (pagedIn) {
                // write the data to selected memory pages
                address = address & 0x3FFF; // map address to zero base
                if (redSelected)
                    red[address] = data;
                if (greenSelected)
                    green[address] = data;
                if (blueSelected)
                    blue[address] = data;
                //
                if (highResSelected)
                    updatePixelByteHighRes(address);
                else
                    updatePixelByteLowRes(address);
                return true;
            }
        }
        return false;
    }

    /**
     * Read data from the AVC memory (If paged in)
     *
     * @param address The address to read from
     * @param ramdis  RAMDIS bus signal
     * @return The byte read (If available)
     */
    @Override
    public int memoryRead(final int address, final boolean ramdis) {
        if (isRAM(address)) {
            return memoryRead(address);
        } else {
            return NO_MEMORY_PRESENT;
        }
    }

    /**
     * Read data from the AVC memory (If paged in)
     *
     * @param address The address to read from
     * @return The byte read
     */
    @Override
    public final int memoryRead(int address) {
        if (isRAM(address)) {
            address = address & 0x3FFF;
            if (memorySelected != 1) {
                return NO_MEMORY_PRESENT;
            } else if (redSelected)
                return red[address];
            else if (greenSelected)
                return green[address];
            else
                return blue[address];
        } else
            return NO_MEMORY_PRESENT;
    }

    /**
     * Will a read to an address cause RAMDIS (i.e. ROM) to be asserted
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDIS(final int address) {
        return (address >= 0x8000) && (address < 0xC000) && pagedIn;
    }

    /**
     * Will a read to an address cause RAMDIS (i.e. ROM) to be asserted
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDISCapable(final int address) {
        return isRAM(address);
    }

    /**
     * Repaint all pixels into the display image for display
     */
    private void resetMemoryDisplay() {
        for (int address = 0; address < MEMORY_SIZE; address++) {
            if (highResSelected) {
                updatePixelByteHighRes(address);
            } else {
                updatePixelByteLowRes(address);
            }
        }
    }

    /**
     * update a single byte in the colour planes for low res display
     *
     * @param address The address the data was written to
     */
    private void updatePixelByteLowRes(final int address) {
        // calculate the (x,y) coordinates and update image buffer
        int column = address & 0x003F;
        int row = (address >> 6) & 0x00FF;
        // out of border check
        if ((column < MEMORY_OFFSET) || (column >= (BYTES_PER_LINE + MEMORY_OFFSET))) {
            return;
        }
        column = column - MEMORY_OFFSET; // starts at 8002H for some unknown reason....
        column = column * 8; // convert byte to pixel
        //
        int redByte, greenByte, blueByte;
        if (redDisplay)
            redByte = red[address];
        else
            redByte = 0x00;
        if (greenDisplay)
            greenByte = green[address];
        else
            greenByte = 0x00;
        if (blueDisplay)
            blueByte = blue[address];
        else
            blueByte = 0x00;
        //
        int bitPosition = 0x80;
        int pixelValue;
        //
        for (int i = 0; i < 8; i++) {
            pixelValue = 0x00;
            //
            if (0 != (bitPosition & redByte)) {
                pixelValue = pixelValue + 4;
            }
            if (0 != (bitPosition & greenByte)) {
                pixelValue = pixelValue + 2;
            }
            if (0 != (bitPosition & blueByte)) {
                pixelValue = pixelValue + 1;
            }
            displaySmallAVCImageByte(row, column + i, pixelValue);
            bitPosition = bitPosition >> 1;
        }
    }

    /**
     * update a single byte in the colour planes for high res display
     *
     * @param address The address the data was written to
     */
    private void updatePixelByteHighRes(final int address) {
        // calculate the (x,y) coordinates and update image buffer
        int column = address & 0x003F;
        int row = (address >> 6) & 0x00FF;
        // out of border check
        if ((column < MEMORY_OFFSET) || (column >= (BYTES_PER_LINE + MEMORY_OFFSET))) {
            return;
        }
        column = column - MEMORY_OFFSET; // starts at 8002H for some unknown reason....
        column = column * 8; // convert byte to pixel
        //
        int redByte, greenByte, blueByte;
        if (greenDisplay)
            redByte = red[address];
        else
            redByte = 0x00;
        if (greenDisplay)
            greenByte = green[address];
        else
            greenByte = 0x00;
        if (blueDisplay)
            blueByte = blue[address];
        else
            blueByte = 0x00;
        //
        int bitPosition = 0x80;
        int redPixelValue;
        int greenPixelValue;
        //
        for (int i = 0; i < 8; i++) {
            redPixelValue = 0x00;
            greenPixelValue = 0x00;
            //
            if (0 != (bitPosition & redByte)) {
                redPixelValue = 2;
            }
            if (0 != (bitPosition & greenByte)) {
                greenPixelValue = 2;
            }
            int pixelColumn = column * 2 + i;
            displayLargeAVCImageByte(row, pixelColumn + 8, redPixelValue);
            displayLargeAVCImageByte(row, pixelColumn, greenPixelValue);
            if (0 != (bitPosition & blueByte)) {
                displayLargeAVCImageByteAddBlue(row, pixelColumn + i, 1);
            } else {
                displayLargeAVCImageByteAddBlue(row, pixelColumn + i, 0);
            }
            bitPosition = bitPosition >> 1;
        }
    }

    /* read from the control port */
    private int CRTCControlRead() {
        return CRTCRegister;
    }

    /* read from the data port */
    private int CRTCDataRead() {
        return CRTCRegisters[CRTCRegister];
    }

    /* read from the page control port */
    private int swapPagesRead() {
        return 0;
    }

    /* write to the control port */
    private void CRTCControlWrite(final int data) {
        CRTCRegister = data;
    }

    /* write to the data port */
    private void CRTCDataWrite(final int data) {
        CRTCRegisters[CRTCRegister] = data;
    }

    /* write to the page control port */
    private void swapPagesWrite(final int data) {
        memorySelected = 0;
        pagedIn = false;
        //
        redSelected = (data & 0x01) != 0;
        greenSelected = (data & 0x02) != 0;
        blueSelected = (data & 0x04) != 0;
        //
        redDisplay = (data & 0x10) != 0;
        greenDisplay = (data & 0x20) != 0;
        blueDisplay = (data & 0x40) != 0;
        //
        int temp = data & 0x07;
        // number of memory planes selected
        switch (temp) {
            case 0: {
                break;
            }
            case 1:
            case 2:
            case 4: {
                memorySelected = 1;
                pagedIn = true;
                break;
            }
            case 3:
            case 5:
            case 6: {
                memorySelected = 2;
                pagedIn = true;
                break;
            }
            case 7: {
                memorySelected = 3;
                pagedIn = true;
                break;
            }
            default:
                break;
        }
        // if the display has changed, reselect the image
        if ((data & 0x78) != (lastB2 & 0x78)) {
            highResSelected = (data & hHIGH_RES_BIT) != 0;
            if (highResSelected) {
                avcFrame.getContentPane().setPreferredSize(new Dimension(AVC_COLUMNS_LARGE * SCALE_LARGE, AVC_ROWS * SCALE_LARGE));
                avcFrame.getContentPane().setSize(new Dimension(AVC_COLUMNS_LARGE * SCALE_LARGE, AVC_ROWS * SCALE_LARGE));
            } else {
                avcFrame.getContentPane().setPreferredSize(new Dimension(AVC_COLUMNS_SMALL * SCALE_SMALL, AVC_ROWS * SCALE_SMALL));
                avcFrame.getContentPane().setSize(new Dimension(AVC_COLUMNS_SMALL * SCALE_SMALL, AVC_ROWS * SCALE_SMALL));
            }
            avcFrame.pack();
            resetMemoryDisplay();
        }
        lastB2 = data;
    }

    /**
     * update the complete 384 * 256 image
     *
     * @param row        Row to set
     * @param column     Column to set
     * @param pixelValue Pixel value to set translated through a palette lookup
     */
    private void displaySmallAVCImageByte(final int row, final int column, final int pixelValue) {
        smallColourBuffer[row * AVC_COLUMNS_SMALL + column] = palette[pixelValue];
    }

    /**
     * update the complete 768 * 256 image
     *
     * @param row        Row to set
     * @param column     Column to set
     * @param pixelValue Pixel value to set translated through a palette lookup
     */
    private void displayLargeAVCImageByte(final int row, final int column, final int pixelValue) {
        try {
            int position = row * AVC_COLUMNS_LARGE + column;
            largeColourBuffer[position] = largeColourBuffer[position] & AVC_BLUE;
            largeColourBuffer[position] = largeColourBuffer[position] | palette[pixelValue];
        } catch (Exception e) {
            System.out.println("Error (green) @ " + row + "/" + column);
        }
    }

    /**
     * Add the blue component to the high resolution image
     *
     * @param row        Row to set
     * @param column     Column to set
     * @param pixelValue Pixel value to set translated through a palette lookup
     */
    private void displayLargeAVCImageByteAddBlue(final int row, final int column, final int pixelValue) {
        try {
            int position = row * AVC_COLUMNS_LARGE + column;
            largeColourBuffer[position] = largeColourBuffer[position] & 0xFFFFFF00;
            largeColourBuffer[position] = largeColourBuffer[position] | palette[pixelValue];
            //
            largeColourBuffer[position + 1] = largeColourBuffer[position + 1] & 0xFFFFFF00;
            largeColourBuffer[position + 1] = largeColourBuffer[position + 1] | palette[pixelValue];
        } catch (Exception e) {
            System.out.println("Error (blue) @ " + row + "/" + column);
        }
    }
}