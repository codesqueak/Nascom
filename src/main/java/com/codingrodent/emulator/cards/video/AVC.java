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
import java.awt.event.*;
import java.awt.image.MemoryImageSource;

public class AVC extends BaseCard implements ActionListener {

    private static final int SCALE_SMALL = 2;
    private static final int SCALE_LARGE = 1;
    private final static int AVC_BLACK = 0xFF000000;
    private final static int AVC_RED = 0xFFFF0000;
    private final static int AVC_GREEN = 0xFF00FF00;
    private final static int AVC_BLUE = 0xFF0000FF;
    private final static int avcRows = 256;
    private final static int avcColumnsSmall = 384;
    private final static int avcColumnsLarge = avcColumnsSmall * 2;
    private final static int memoryOffset = 2;                                    // zero pixel is at 8002H
    private final static int bytesPerLine = avcColumnsSmall / 8;
    private final static int memorySize = 0x4000;
    private final static int highResBit = 0x08;
    //
    private final int[] red = new int[memorySize];
    private final int[] green = new int[memorySize];
    private final int[] blue = new int[memorySize];
    private final int[] smallColourBuffer = new int[avcRows * avcColumnsSmall];
    private final int[] largeColourBuffer = new int[avcRows * avcColumnsLarge];
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
    private int displaySelected = 0;
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

    @Override
    public void initialise() {
        /* paint the AVC frame (small) */
        avcFrame = new AVCFrame("Nascom 2 AVC Model B");
        avcFrame.getContentPane().setBackground(Color.BLACK);
        //
        palette[0] = AVC_BLACK;
        palette[1] = AVC_BLUE;
        palette[2] = AVC_GREEN;
        palette[3] = 0xFF00FFFF;
        palette[4] = AVC_RED;
        palette[5] = 0xFFFF00FF;
        palette[6] = 0xFFFFFF00;
        palette[7] = 0xFFFFFFFF;
        //
        for (int row = 0; row < avcRows; row++) {
            for (int column = 0; column < avcColumnsSmall; column++) {
                smallColourBuffer[row * avcColumnsSmall + column] = 0xFF000000 | row * column; // row * column;
            }
        }
        //
        for (int row = 0; row < avcRows; row++) {
            for (int column = 0; column < avcColumnsLarge; column++) {
                largeColourBuffer[row * avcColumnsLarge + column] = 0xFF000000 | row * column; // row * column;
            }
        }
        //
        smallImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(avcColumnsSmall, avcRows, smallColourBuffer, 0, avcColumnsSmall));
        largeImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(avcColumnsLarge, avcRows, largeColourBuffer, 0, avcColumnsLarge));
        //
        avcFrame.getContentPane().setPreferredSize(new Dimension(avcColumnsSmall * 2, avcRows * 2));
        smallImage.setAccelerationPriority(1.0f);
        //
        avcFrame.setResizable(false);
        avcFrame.pack();
        avcFrame.setVisible(true);
        //
        Timer timer = new Timer(50, this);
        timer.setCoalesce(true);
        timer.setRepeats(true);
        timer.start();
    }

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
    public boolean isRAM(int address) {
        return (address >= 0x8000) && (address < 0xC000);
    }

    /**
     * Does the card support input at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isInputPort(int address) {
        switch (address) {
            case 0xB0:
                return true;
            case 0xB1:
                return true;
            case 0xB2:
                return true;
        }
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
        displaySelected = 0;
        pagedIn = false;
        // clear the colour memory planes
        for (int i = 0; i < red.length; i++) {
            red[i] = 0;
            green[i] = 0;
            blue[i] = 0;
        }
        // if (PropertyStatus.getInstance().getLogging())
        // System.out.println("AVC Reset");
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (highResSelected) {
            largeImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(avcColumnsLarge, avcRows, largeColourBuffer, 0, avcColumnsLarge));
            largeImage.setAccelerationPriority(1.0f);
            Image doubleImage = largeImage.getScaledInstance(avcColumnsLarge * SCALE_LARGE, avcRows * SCALE_LARGE, Image.SCALE_FAST);
            avcFrame.getContentPane().getGraphics().drawImage(doubleImage, 0, 0, null);
        } else {
            smallImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(avcColumnsSmall, avcRows, smallColourBuffer, 0, avcColumnsSmall));
            smallImage.setAccelerationPriority(1.0f);
            Image doubleImage = smallImage.getScaledInstance(avcColumnsSmall * SCALE_SMALL, avcRows * SCALE_SMALL, Image.SCALE_FAST);
            avcFrame.getContentPane().getGraphics().drawImage(doubleImage, 0, 0, null);
        }
    }

    @Override
    public void ioWrite(int address, int data) {
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

    // read from a control port
    @Override
    public int ioRead(int address) {
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
        }
        return 0;
    }

    /**
     *
     */
    @Override
    public boolean memoryWrite(int address, final int data, final boolean ramdis) {
        if ((address >= 0x8000) && (address < 0xC000)) {
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
            return false;
        }
        return false;
    }

    /**
     * Read data from the memory bus taking into account the RAMDIS signal
     *
     * @param address The address to read from
     * @param ramdis  RAMDIS bus signal
     * @return The byte read
     */
    @Override
    public int memoryRead(final int address, final boolean ramdis) {
        if ((address >= 0x8000) && (address < 0xC000)) {
            return memoryRead(address);
        } else {
            return NO_MEMORY_PRESENT;
        }
    }

    @Override
    public final int memoryRead(int address) {
        if ((address < 0x8000) || (address >= 0xC000)) {
            return NO_MEMORY_PRESENT;
        }
        address = address & 0x3FFF;
        if (memorySelected != 1) {
            return NO_MEMORY_PRESENT;
        } else if (redSelected)
            return red[address];
        else if (greenSelected)
            return green[address];
        else
            return blue[address];
    }

    /**
     * Will a read to an address cause RAMDIS (i.e. ROM) to be asserted
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDIS(int address) {
        return (address >= 0x8000) && (address < 0xC000) && pagedIn;
    }

    /**
     * Will a read to an address cause RAMDIS (i.e. ROM) to be asserted
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDISCapable(int address) {
        return (address >= 0x8000) && (address < 0xC000);
    }

    /**
     *
     *
     */
    private void resetMemoryDisplay() {
        for (int address = 0; address < memorySize; address++) {
            if (highResSelected) {
                updatePixelByteHighRes(address);
            } else {
                updatePixelByteLowRes(address);
            }
        }
    }

    /**
     * update a single byte in the colour planes
     *
     * @param address The address the data was written to
     */
    private void updatePixelByteLowRes(final int address) {
        // calculate the (x,y) coordinates and update image buffer
        int column = address & 0x003F;
        int row = (address >> 6) & 0x00FF;
        // out of border check
        if ((column < memoryOffset) || (column >= (bytesPerLine + memoryOffset))) {
            return;
        }
        column = column - memoryOffset; // starts at 8002H for some unknown reason....
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
     * update a single byte in the colour planes
     *
     * @param address The address the data was written to
     */
    private void updatePixelByteHighRes(final int address) {
        // calculate the (x,y) coordinates and update image buffer
        int column = address & 0x003F;
        int row = (address >> 6) & 0x00FF;
        // out of border check
        if ((column < memoryOffset) || (column >= (bytesPerLine + memoryOffset))) {
            return;
        }
        column = column - memoryOffset; // starts at 8002H for some unknown reason....
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
                // System.out.println(row + " : " + column);
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
    private void CRTCControlWrite(int data) {
        CRTCRegister = data;
    }

    /* write to the data port */
    private void CRTCDataWrite(int data) {
        CRTCRegisters[CRTCRegister] = data;
    }

    /* read from the page control port */
    private void swapPagesWrite(int data) {
        // System.out.println("Write to page control port B2:"+data);
        memorySelected = 0;
        displaySelected = 0;
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
                memorySelected = 0;
                pagedIn = false;
                break;
            }
            case 1: {
                memorySelected = 1;
                pagedIn = true;
                break;
            }
            case 2: {
                memorySelected = 1;
                pagedIn = true;
                break;
            }
            case 4: {
                memorySelected = 1;
                pagedIn = true;
                break;
            }
            case 3: {
                memorySelected = 2;
                pagedIn = true;
                break;
            }
            case 5: {
                memorySelected = 2;
                pagedIn = true;
                break;
            }
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
        //
        temp = (data >> 4) & 0x07;
        // number of memory planes displayed
        switch (temp) {
            case 0: {
                displaySelected = 0;
                break;
            }
            case 1: {
                displaySelected = 1;
                break;
            }
            case 2: {
                displaySelected = 1;
                break;
            }
            case 4: {
                displaySelected = 1;
                break;
            }
            case 3: {
                displaySelected = 2;
                break;
            }
            case 5: {
                displaySelected = 2;
                break;
            }
            case 6: {
                displaySelected = 2;
                break;
            }
            case 7: {
                displaySelected = 3;
                break;
            }
            default:
                System.out.println("Bad AVC memory display value");
        }
        // if the display has changed, reselect the image
        if ((data & 0x78) != (lastB2 & 0x78)) {
            highResSelected = (data & highResBit) != 0;
            if (highResSelected) {
                avcFrame.getContentPane().setPreferredSize(new Dimension(avcColumnsLarge * SCALE_LARGE, avcRows * SCALE_LARGE));
                avcFrame.getContentPane().setSize(new Dimension(avcColumnsLarge * SCALE_LARGE, avcRows * SCALE_LARGE));
            } else {
                avcFrame.getContentPane().setPreferredSize(new Dimension(avcColumnsSmall * SCALE_SMALL, avcRows * SCALE_SMALL));
                avcFrame.getContentPane().setSize(new Dimension(avcColumnsSmall * SCALE_SMALL, avcRows * SCALE_SMALL));
            }
            avcFrame.pack();
            resetMemoryDisplay();
        }
        lastB2 = data;
    }

    /**
     * is AVC memory selected for read ?
     *
     * @return True if AVC selected, else false
     */
    public boolean AVCMemorySelected() {
        return pagedIn;
    }

    /**
     * is AVC memory selected for write ?
     *
     * @return True if AVC display selected, else false
     */
    public boolean AVCDisplaySelected() {
        return !(displaySelected == 0);
    }

    /**
     * update the complete 384 * 256 image
     *
     * @param row        Row to set
     * @param column     Column to set
     * @param pixelValue Pixel value to set translated through a palette lookup
     */
    private void displaySmallAVCImageByte(final int row, final int column, final int pixelValue) {
        smallColourBuffer[row * avcColumnsSmall + column] = palette[pixelValue];
        //
    }

    /**
     * update the complete 768 * 256 image
     *
     * @param row        Row to set
     * @param column     Column to set
     * @param pixelValue Pixel value to set translated through a palette lookup
     */
    private void displayLargeAVCImageByte(final int row, final int column, final int pixelValue) {
        // if (0 != pixelValue) System.out.println(row + "/" + column + "/" + pixelValue);
        try {
            int position = row * avcColumnsLarge + column;
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
        // System.out.println(row+"/"+column);
        try {
            int position = row * avcColumnsLarge + column;
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