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
import com.codingrodent.microprocessor.IMemory;
import com.codingrodent.microprocessor.ProcessorException;
import com.codingrodent.microprocessor.Z80.Z80Core;

class Z80CPU extends Z80Core {
    /* throttling constants */
    private final static long TIME_SLICE_MS = 1;
    private final static double TWO_NUPS = 9225;
    private long nupTime;
    private long tStatesPerMS;
    private boolean maxSpeed;
    private boolean nupMode;
    private long lastTime;

    /**
     * Standard constructor
     *
     * @param ram Interface to the memory architecture
     * @param io  Interface to the i/o port architecture
     */
    Z80CPU(IMemory ram, IBaseDevice io) {
        super(ram, io);
        setMHz(-1);
    }

	/*
     * Public interfaces to processor control functions
	 */

    /**
     * The MHz rating of the cpu
     *
     * @param mhz Speed in 1 MHz steps. Will default to flat out if a value of less than one is used
     */
    synchronized void setMHz(int mhz) {
        if (mhz <= 0) {
            maxSpeed = true;
            tStatesPerMS = 8000L; // 8MHz
        } else {
            maxSpeed = false;
            tStatesPerMS = (long) (mhz * 1000);
        }
        lastTime = System.nanoTime();
        resetTStates();
    }

    /**
     * Start / stop the performance counting mode
     *
     * @param nupMode Mode set flag
     */
    synchronized void setNUPMode(boolean nupMode) {
        this.nupMode = nupMode;
    }

    /**
     * Execute a single instruction at the present program counter (PC) then return
     *
     * @throws ProcessorException Thrown if an unexpected state arises
     */
    synchronized void execute() throws ProcessorException {
        if (nupMode) {
            if (0x1000 == getProgramCounter()) {
                System.out.println("nup Tracking start");
                setMHz(-1);
                nupTime = System.currentTimeMillis();
            } else {
                if (this.getHalt()) {
                    long total = (System.currentTimeMillis() - nupTime) / 1000;
                    System.out.println("Time taken: " + total + " Seconds");
                    double units = TWO_NUPS / total * 2;
                    System.out.println("NUPS      : " + units);
                    throw new RuntimeException("NUP check finished");
                }
            }
        }
        // if (reg_PC == 0x0042)
        // {
        // //System.out.println("Execution transfer");
        // }
        // if (reg_PC < 0xF000)
        // System.out.println( utilities.getWord( reg_PC ));
        if (!maxSpeed) {
            // put a delay (if required) per time slice
            long timePassed = (System.nanoTime() - lastTime) / 1_000_000;
            if (timePassed >= TIME_SLICE_MS) {
                // how much time to waste ?
                long cpuTimePassed = getTStates() / tStatesPerMS;
                //
                // if we are going too fast, snooze for a bit
                if (cpuTimePassed > timePassed) {
                    // Too fast
                    try {
                        long delay = cpuTimePassed - timePassed;
                        // System.out.println("Delay :
                        // "+timePassed+"/"+cpuTimePassed+"/"+delay+"/"+tStates+"/"+tStatesPerMS);
                        if (delay < 1000) {
                            // System.out.println("-------------");
                            Thread.sleep(delay);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                lastTime = System.nanoTime();
                resetTStates();
            }
        }
        executeOneInstruction();
    }
}
