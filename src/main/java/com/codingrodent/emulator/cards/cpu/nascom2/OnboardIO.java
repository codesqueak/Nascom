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

import com.codingrodent.emulator.emulator.SystemContext;
import com.codingrodent.emulator.nas80Bus.INasBus;
import com.codingrodent.microprocessor.IBaseDevice;

class OnboardIO implements IBaseDevice {
    private static final int NMIFlag = 0x08;
    private final CassetteTape cassetteTape;
    private final SystemContext context;
    private Keyboard keyboard;
    private INasBus nasBus;
    private Nascom2CPUCard cpuCard;
    private IBaseDevice pioDevice;

    OnboardIO() {
        keyboard = null;
        context = SystemContext.createInstance();
        cassetteTape = new CassetteTape();
        //
        try {
            pioDevice = new DefaultPIO();
        } catch (Exception ex) {
            String msg = "Unable to load Nascom 2 PIO I/O device class <" + ex.getMessage() + ">";
            context.logFatalEvent(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Read data from an I/O port
     *
     * @param address The port to be read from
     * @return The 8 bit value at the request port address
     */
    @Override
    public int IORead(int address) {
        int localAddress = address & 0x00FF; // remember in (C) is actually
        //
        switch (localAddress) {
            case 0x00: {
                return keyboard.IORead(0);
            }
            case 0x01: {
                return cassetteTape.readDataFromUART();
            }
            case 0x02: {
                return cassetteTape.readStatusFromUART();
            }
            case 0x03: {
                return 0x00;
            }
            case 0x04: {
                return pioDevice.IORead(localAddress);
            }
            case 0x05: {
                return pioDevice.IORead(localAddress);
            }
            case 0x06: {
                return pioDevice.IORead(localAddress);
            }
            case 0x07: {
                return pioDevice.IORead(localAddress);
            }
            default:
                return nasBus.ioRead(address);
        }
    }

    /**
     * Write data to an I/O port
     *
     * @param address The port to be written to
     * @param data    The 8 bit value to be written
     */
    @Override
    public void IOWrite(int address, int data) {
        // System.out.println("Writing to : "+utilities.getByte(address));
        int localAddress = address & 0x00FF; // remember in (C) is actually
        // in (BC)
        //
        switch (localAddress) {
            case 0x00: {
                keyboard.IOWrite(localAddress, data);
                cassetteTape.controlLED(data);
                if ((data & NMIFlag) != 0) {
                    context.logDebugEvent("NMI request");
                    cpuCard.requestSingleStepNMI();
                }
                return;
            }
            case 0x01: {
                cassetteTape.writeDataToUART(data);
                return;
            }
            case 0x02: {
                return;
            }
            case 0x03: {
                return;
            }
            case 0x04: {
                pioDevice.IOWrite(localAddress, data);
                return;
            }
            case 0x05: {
                pioDevice.IOWrite(localAddress, data);
                return;
            }
            case 0x06: {
                pioDevice.IOWrite(localAddress, data);
                return;
            }
            case 0x07: {
                pioDevice.IOWrite(localAddress, data);
                return;
            }
            default:
                nasBus.ioWrite(address, data);
        }
    }

    /**
     * Add the keyboard device on port 0
     *
     * @param keyboard Keyboard device
     */
    void setKeyboard(Keyboard keyboard) {
        this.keyboard = keyboard;
    }

    /**
     * Identify the NAS BUS to the card
     *
     * @param nasBus The NAS BUS controller object
     */
    void setNasBus(INasBus nasBus) {
        this.nasBus = nasBus;
    }

    /**
     * Tell the I/O system about the N2 card. Neede for a call-back to start the single step NMI
     *
     * @param cpuCard The owing N2 cpu card
     */
    void setCPUCard(Nascom2CPUCard cpuCard) {
        this.cpuCard = cpuCard;
    }

    /**
     * Load a new tape image
     */
    void loadNewTape() {
        cassetteTape.loadNewTape();
    }

    /**
     * Set a new tape image to write to
     */
    void saveNewTape() {
        cassetteTape.saveNewTape();
    }

    /**
     * Stop playback of loading tape
     */
    void stopLoading() {
        cassetteTape.closeInput();
    }

    /**
     * Stop recording to saving tape
     */
    void stopSaving() {
        cassetteTape.closeOutput();
    }

}
