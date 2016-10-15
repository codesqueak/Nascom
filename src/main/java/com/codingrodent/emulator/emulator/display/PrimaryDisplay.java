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

package com.codingrodent.emulator.emulator.display;

import javax.swing.*;

/*
 * convert video memory writes into bit displays
 */
public class PrimaryDisplay extends JFrame {

    private static final long serialVersionUID = 3832623997442863920L;
    //
    private final JMenuBar menuBar;
    private final JMenuItem loadFileRAM;
    private final JMenuItem saveFile;
    private final JCheckBoxMenuItem MHz2;
    private final JCheckBoxMenuItem MHz4;
    private final JCheckBoxMenuItem MHz6;
    private final JCheckBoxMenuItem MHzMax;
    private final JMenuItem runUntil;
    private final JMenuItem singleStep;
    private final JMenuItem halt;
    private final JMenuItem reset;
    private final JMenuItem restart;
    private final GUIListener guiListener;
    private JMenu menu;

    /*
     * put up windows to hold the video display and register display
     */
    public PrimaryDisplay() {
        super("Primary Display");
        /* the nascom 48*16 video display */
        setBounds(0, 0, 800, 640);
        //
        addWindowListener(new WindowHandler());
        // screenFrame.addKeyListener(new KeyboardHandler(keyboard));
        //
        // add in all the menu items etc
        guiListener = new GUIListener(this);
        // context.setGUIListener(guiListener);
        menuBar = new JMenuBar();
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        setJMenuBar(menuBar);
        // set the file items
        menu = new JMenu("File");
        menuBar.add(menu);
        loadFileRAM = new JMenuItem("Load File (RAM)");
        loadFileRAM.addActionListener(guiListener);
        menu.add(loadFileRAM);
        saveFile = new JMenuItem("Save Memory Image");
        saveFile.addActionListener(guiListener);
        menu.add(saveFile);
        menu.addSeparator();
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(guiListener);
        menu.add(exit);
        // execute options
        menu = new JMenu("Execute");
        menuBar.add(menu);
        runUntil = new JMenuItem("Run Until");
        runUntil.setEnabled(false);
        runUntil.addActionListener(guiListener);
        menu.add(runUntil);
        singleStep = new JMenuItem("Single Step");
        singleStep.addActionListener(guiListener);
        singleStep.setEnabled(false);
        menu.add(singleStep);
        halt = new JMenuItem("Stop");
        halt.addActionListener(guiListener);
        halt.setEnabled(true);
        menu.add(halt);
        reset = new JMenuItem("Reset");
        reset.addActionListener(guiListener);
        menu.add(reset);
        restart = new JMenuItem("Restart");
        restart.addActionListener(guiListener);
        restart.setEnabled(false);
        menu.add(restart);
        JMenuItem nmi = new JMenuItem("NMI");
        nmi.addActionListener(guiListener);
        menu.add(nmi);
        // performance options
        menu = new JMenu("Performance");
        menuBar.add(menu);
        MHz2 = new JCheckBoxMenuItem("2 MHz");
        MHz2.addActionListener(guiListener);
        menu.add(MHz2);
        MHz4 = new JCheckBoxMenuItem("4 MHz");
        MHz4.addActionListener(guiListener);
        menu.add(MHz4);
        MHz6 = new JCheckBoxMenuItem("6 MHz");
        MHz6.addActionListener(guiListener);
        menu.add(MHz6);
        MHz4.setState(true);
        MHzMax = new JCheckBoxMenuItem("Maximum");
        MHzMax.addActionListener(guiListener);
        menu.add(MHzMax);
    }

    /**
     * Add the final menu items
     */
    public void displayMenu() {
        // set the help items
        menu = new JMenu("Help");
        menuBar.add(menu);
        JMenuItem information = new JMenuItem("Information");
        information.addActionListener(guiListener);
        menu.add(information);
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(guiListener);
        menu.add(about);
        // Set up the panel, enable this close and enable event handling
        // and show the window
        setVisible(true);
        setResizable(false);
    }

    void setLoadFileRAM(boolean state) {
        loadFileRAM.setEnabled(state);
    }

    void setSaveFile(boolean state) {
        saveFile.setEnabled(state);
    }

    //
    void set2MHz() {
        MHz2.setEnabled(true);
    }

    void enable2MHz() {
        MHz2.setState(true);
        MHz4.setState(false);
        MHz6.setState(false);
        MHzMax.setState(false);
    }

    void set4MHz() {
        MHz4.setEnabled(true);
    }

    void enable4MHz() {
        MHz2.setState(false);
        MHz4.setState(true);
        MHz6.setState(false);
        MHzMax.setState(false);
    }

    void setMHzMax() {
        MHzMax.setEnabled(true);
    }

    void enableMHzMax() {
        MHz2.setState(false);
        MHz4.setState(false);
        MHz6.setState(false);
        MHzMax.setState(true);
    }

    void set6MHz() {
        MHz6.setEnabled(true);
    }

    void enable6MHz() {
        MHz2.setState(false);
        MHz4.setState(false);
        MHz6.setState(true);
        MHzMax.setState(false);
    }

    //
    void setRunUntil(boolean state) {
        runUntil.setEnabled(state);
    }

    void setSingleStep(boolean state) {
        singleStep.setEnabled(state);
    }

    void setHalt(boolean state) {
        halt.setEnabled(state);
    }

    void setReset(boolean state) {
        reset.setEnabled(state);
    }

    void setRestart(boolean state) {
        restart.setEnabled(state);
    }

}
