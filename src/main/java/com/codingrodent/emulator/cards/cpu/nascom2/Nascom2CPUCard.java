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

import com.codingrodent.emulator.cards.*;
import com.codingrodent.emulator.emulator.SystemContext;
import com.codingrodent.emulator.nas80Bus.INasBus;
import com.codingrodent.emulator.utilities.*;
import com.codingrodent.microprocessor.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Map;

public class Nascom2CPUCard implements ICard, ICPUControl, INasBus {
    private final static String TAPE = "Tape";
    private final static String LOAD_TAPE = "Load Tape";
    private final static String SAVE_TAPE = "Save Tape";
    private final static String STOP_LOADING = "Stop Loading Tape";
    private final static String STOP_SAVING = "Stop Saving Tape";
    private final IMemory memory;
    private final IBaseDevice ioDevices;
    private final Z80CPU processor;
    private final SystemContext systemContext = SystemContext.createInstance();
    private final KeyboardHandler keyboardHandler;
    private String cardName;
    private Map<String, String> cardProperties;
    private boolean run;
    private int NMICounter;
    private boolean nupMode = false;

    /**
     * Standard constructor to produce a Nascom 2 CPU card
     */
    public Nascom2CPUCard() {
        memory = new OnboardMemory();
        ioDevices = new OnboardIO();
        processor = new Z80CPU(memory, ioDevices);
        //
        Keyboard keyboard = new Keyboard();
        keyboardHandler = new KeyboardHandler(keyboard);
        ((OnboardIO) ioDevices).setKeyboard(keyboard);
        ((OnboardIO) ioDevices).setCPUCard(this);
        //
        // t = 0;
        run = true;
        //
        // Add the controls for the tape
        JFrame screenFrame = systemContext.getPrimaryDisplay();
        JMenuBar menuBar = screenFrame.getJMenuBar();
        JMenu menu = new JMenu(TAPE);
        menuBar.add(menu);
        JMenuItem loadMenuItem = new JMenuItem(LOAD_TAPE);
        menu.add(loadMenuItem);
        loadMenuItem.addActionListener(this);
        JMenuItem stopLoadMenuItem = new JMenuItem(STOP_LOADING);
        menu.add(stopLoadMenuItem);
        stopLoadMenuItem.addActionListener(this);
        JMenuItem saveMenuItem = new JMenuItem(SAVE_TAPE);
        menu.add(saveMenuItem);
        saveMenuItem.addActionListener(this);
        JMenuItem stopSaveMenuItem = new JMenuItem(STOP_SAVING);
        menu.add(stopSaveMenuItem);
        stopSaveMenuItem.addActionListener(this);
    }

    /**
     * One off initialisation carried out after card object creation
     */
    @Override
    public void initialise() {
        StandardDisplayDevice display;
        try {
            FileHandler fileHandler = new FileHandler();
            MemoryChunk videoROM = fileHandler.readHexDumpFile(cardProperties.get("VideoROM"));
            display = new StandardDisplayDevice(videoROM);
        } catch (IOException ex) {
            String msg = "Unable to load the video ROM, <" + ex.getMessage() + ">";
            systemContext.logFatalEvent(msg);
            throw new RuntimeException(msg);
        }
        ((OnboardMemory) memory).setDisplayDevice(display);
        ((OnboardMemory) memory).setCardProperties(cardProperties);
        ((OnboardMemory) memory).initialise();
        display.addKeyboardHandler(keyboardHandler);
    }

    /**
     * Set any card specific parameters
     *
     * @param cardProperties Property list
     */
    @Override
    public void setCardProperties(Map<String, String> cardProperties) {
        this.cardProperties = cardProperties;
        //
        String property = cardProperties.get("nup");
        if (null != property) {
            nupMode = "true".equals(property);
        }
    }

    /**
     * Does the card support RAM at the address specified
     *
     * @param address The address to test
     * @return True is RAM, else false
     */
    @Override
    public boolean isRAM(int address) {
        return ((OnboardMemory) memory).isRAM(address);
    }

    /**
     * Does the card support ROM at the address specified
     *
     * @param address The address to test
     * @return True is ROM, else false
     */
    @Override
    public boolean isROM(int address) {
        return ((OnboardMemory) memory).isROM(address);
    }

    /**
     * Does the card support input at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isInputPort(int address) {
        return (address >= 0) && (address <= 7);
    }

    /**
     * Does the card support output at the address specified
     *
     * @param address The address to test
     * @return True is port, else false
     */
    @Override
    public boolean isOutputPort(int address) {
        return (address >= 0) && (address <= 7);
    }

    /**
     * Get a human-readable name for the card
     *
     * @return Card name string
     */
    @Override
    public String getCardName() {
        return cardName;
    }

    /**
     * Set a human-readable name for the card
     *
     * @param cardName Card name string
     */
    @Override
    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    /**
     * Get the details of the card by the author
     *
     * @return Card name string
     */
    @Override
    public String getCardDetails() {
        return "Nascom 2 4MHz CPU Card - Version 1.1";
    }

    /**
     * Report if the card is a CPU card
     *
     * @return True if a cpu, else false
     */
    @Override
    public boolean isCPU() {
        return true;
    }

    /**
     * Reset the card
     */
    @Override
    public void reset() {
        processor.reset();
    }

    /**
     * Identify the NAS BUS to the card
     *
     * @param nasBus The NAS BUS controller object
     */
    @Override
    public void setNasBus(INasBus nasBus) {
        ((OnboardMemory) memory).setNasBus(nasBus);
        ((OnboardIO) ioDevices).setNasBus(nasBus);
    }

    /**
     * @param e ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String menuCommand = e.getActionCommand();
        if (LOAD_TAPE.equals(menuCommand)) {
            System.out.println("-- Load --");
            ((OnboardIO) ioDevices).loadNewTape();
        } else if (SAVE_TAPE.equals(menuCommand)) {
            System.out.println("-- Save --");
            ((OnboardIO) ioDevices).saveNewTape();
        } else if (STOP_LOADING.equals(menuCommand)) {
            System.out.println("-- Stop loading --");
            ((OnboardIO) ioDevices).stopLoading();
        } else if (STOP_SAVING.equals(menuCommand)) {
            System.out.println("-- Stop saving --");
            ((OnboardIO) ioDevices).stopSaving();
        }
    }

    /**
     * If the card contains a CPU, start execution
     */
    @Override
    public void start() {
        systemContext.logInfoEvent("Start Execution");
        processor.reset();
        run = true;
        int startAddress = Utilities.getHexValue(cardProperties.getOrDefault("StartAddress", "0000"));
        if ((startAddress < 0) | (startAddress) > 0xFFFF) {
            systemContext.logErrorEvent("Start address is not in the range 0x0000 to 0xFFFF");
            return;
        }
        processor.setProgramCounter(startAddress);
        processor.setResetAddress(startAddress);
        processor.setMHz(4);
        processor.setNUPMode(nupMode);
        while (true) {
            if (run) {
                processNascomNMI();
                processor.execute();
            } else {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ex1) {
                    systemContext.logErrorEvent("Sleep in main execution thread interrupted - weird!");
                }
            }
        }

    }

    /**
     * If the card contains a CPU, stop execution
     */
    @Override
    public void stop() {
        systemContext.logInfoEvent("Stop Execution");
        run = false;
    }

    /**
     * Restart the card
     */
    @Override
    public void restart() {
        systemContext.logInfoEvent("Restart Execution");
        run = true;
    }

    /**
     * Set the CPU maximum speed in MHz. -1 gives maximum speed
     *
     * @param mhz int
     */
    @Override
    public void setSpeedMHz(int mhz) {
        processor.setMHz(mhz);
    }

    /**
     * Toggle the NMI line on the CPU
     */
    @Override
    public void toggleNMI() {
        requestSingleStepNMI();
    }

    /**
     * Indicate when a block move is in progress, LDIR, CPDR etc. May be sampled during repetitive cycles of the
     * instruction
     *
     * @return true represents a block move, else false if not executing
     */
    @Override
    public boolean blockMoveInProgress() {
        return processor.blockMoveInProgress();
    }

    /**
     * the single step logic waits 4 instructions before causing an NMI
     */
    void requestSingleStepNMI() {
        NMICounter = 4;
    }

    /**
     * process and NMI request in progress
     */
    private void processNascomNMI() {
        // check NMI single step counter
        if (NMICounter != 0) {
            NMICounter--;
            if (0 == NMICounter) {
                processor.setNMI();
            }
        }
    }

    /**
     * Write data to the memory bus
     *
     * @param address Address to write to
     * @param data    Data to be written
     * @return Always false
     */
    @Override
    public boolean memoryWrite(int address, int data, boolean ramdis) {
        memory.writeByte(address, data);
        return false;
    }

    /**
     * ead data from the memory bus taking into account the RAMDIS signal
     *
     * @param address Address to read from
     * @param ramdis  RAMDIS bus signal
     * @return Byte of data
     */
    @Override
    public int memoryRead(int address, boolean ramdis) {
        return memory.readByte(address);
    }

    /**
     * Read data from the memory bus
     *
     * @param address Address to read from
     * @return Byte of data
     */
    @Override
    public int memoryRead(int address) {
        return memory.readByte(address);
    }

    /**
     * Write data to the io bus
     *
     * @param address Address to write to
     * @param data    Data to be written
     */
    @Override
    public void ioWrite(int address, int data) {
        ioDevices.IOWrite(address, data);
    }

    /**
     * Read data from the io bus
     *
     * @param address Address to read from
     * @return Byte of data
     */
    @Override
    public int ioRead(int address) {
        return ioDevices.IORead(address);
    }

    /**
     * A processor halt as occurred
     */
    @Override
    public void halt() {
    }

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
     * Will a read to an address may cause RAMDIS (i.e. ROM) to be asserted. For example, a paged out ROM will cause
     * RAMDIS to be asserted. That is, RAMDIS may occur at this address.
     *
     * @param address The address to read from
     * @return true if RAMDIS is to be asserted, else false
     */
    @Override
    public boolean assertRAMDISCapable(int address) {
        return false;
    }

    /**
     * Recover the number of T states executed by the CPU
     *
     * @return long
     */
    @Override
    public long getClock() {
        return processor.getTStates();
    }

    /**
     * Set the number of T states executed by the CPU
     *
     * @param t long
     */
    @Override
    public void setClock(long t) {
    }
}
