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

import com.codingrodent.emulator.emulator.*;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/*
 * event listeners for processing GUI events
 */

class GUIListener implements ActionListener, ItemListener {

    private final JFrame frame;
    private final SystemContext context;

    GUIListener(JFrame frame) {
        this.frame = frame;
        context = SystemContext.createInstance();
    }

    /**
     * @param e ActionEvent
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("DM_EXIT")
    public void actionPerformed(ActionEvent e) {
        String menuCommand = e.getActionCommand();
        PrimaryDisplay screenFrame = SystemContext.createInstance().getPrimaryDisplay();
        // File menu

        // Execute Menu

        if ("Single Step".equals(menuCommand)) {
            screenFrame.setSaveFile(false);
            screenFrame.setLoadFileRAM(false);
            //
            screenFrame.setRunUntil(false);
            screenFrame.setSingleStep(false);
            screenFrame.setHalt(true);
            screenFrame.setReset(false);
            //computer.singleStep();
            screenFrame.setRestart(false);
        } else {
            if ("Stop".equals(menuCommand)) {
                context.getCPUCard().stop();
                screenFrame.setSaveFile(true);
                screenFrame.setLoadFileRAM(true);
                //
                screenFrame.set2MHz();
                screenFrame.set4MHz();
                screenFrame.set6MHz();
                screenFrame.setMHzMax();
                //
                screenFrame.setRunUntil(true);
                screenFrame.setSingleStep(true);
                screenFrame.setHalt(false);
                screenFrame.setReset(true);
                screenFrame.setRestart(true);
            } else {
                if ("Restart".equals(menuCommand)) {
                    screenFrame.setSaveFile(false);
                    screenFrame.setLoadFileRAM(false);
                    //
                    screenFrame.setRunUntil(false);
                    screenFrame.setSingleStep(false);
                    screenFrame.setHalt(true);
                    screenFrame.setReset(true);
                    screenFrame.setRestart(true);
                    //
                    context.getCPUCard().restart();
                } else {
                    if ("2 MHz".equals(menuCommand)) {
                        context.getCPUCard().setSpeedMHz(2);
                        screenFrame.enable2MHz();
                    } else {
                        if ("4 MHz".equals(menuCommand)) {
                            context.getCPUCard().setSpeedMHz(4);
                            screenFrame.enable4MHz();
                        } else {
                            if ("6 MHz".equals(menuCommand)) {
                                context.getCPUCard().setSpeedMHz(6);
                                screenFrame.enable6MHz();
                            } else {
                                if ("Maximum".equals(menuCommand)) {
                                    context.getCPUCard().setSpeedMHz(-1);
                                    screenFrame.enableMHzMax();
                                } else {
                                    if ("Reset".equals(menuCommand)) {
                                        context.getCardController().getCard(0).reset();
                                    } else {
                                        if ("NMI".equals(menuCommand)) {
                                            context.getCPUCard().toggleNMI();
                                        } else {
                                            if ("Exit".equals(menuCommand)) {
                                                System.exit(0);
                                            } else {
                                                if ("Load File (RAM)".equals(menuCommand)) {
                                                    JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
                                                    fc.setFileFilter(new nasFileFilter());
                                                    int returnValue = fc.showOpenDialog(frame);
                                                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                                                        File file = fc.getSelectedFile();
                                                        String fileName = file.getAbsolutePath();
                                                        context.logDebugEvent("Processing file : " + fileName);
                                                        context.getCardController().loadProgram(fileName);
                                                    }
                                                } else {
                                                    if ("Save Memory Image".equals(menuCommand)) {
                                                        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
                                                        fc.setFileFilter(new nasFileFilter());
                                                        int returnValue = fc.showSaveDialog(frame);
                                                        if (returnValue == JFileChooser.APPROVE_OPTION) {
                                                            File file = fc.getSelectedFile();
                                                            String fileName = file.getAbsolutePath();
                                                            context.logDebugEvent("Processing file : " + fileName);
                                                            context.getCardController().dumpMemory(fileName, frame);
                                                        }
                                                    } else {
                                                        if ("Information".equals(menuCommand)) {
                                                            JOptionPane.showMessageDialog(frame, "80-BUS Modular Emulator\n\n", "Information...", JOptionPane.INFORMATION_MESSAGE, null);
                                                        } else {
                                                            if ("About".equals(menuCommand)) {
                                                                StringBuilder aboutString = new StringBuilder("Cards loaded:\n\n");
                                                                List<CardData> cards = context.getAllCards();
                                                                for (CardData card : cards) {
                                                                    aboutString.append(card.getDetails()).append('\n');
                                                                }
                                                                aboutString.append("\nShare and enjoy....\n\n");
                                                                JOptionPane.showMessageDialog(frame, aboutString.toString(), "About...", JOptionPane.INFORMATION_MESSAGE, null);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //System.out.println( e.paramString() );
        //System.out.println( e.getActionCommand() );
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        //System.out.println( e.paramString() );
    }

    /**
     * inner class for the file filter
     */
    private static class nasFileFilter extends javax.swing.filechooser.FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            } else {
                int place = f.getName().lastIndexOf('.');
                if (place == -1) {
                    return false;
                }
                String fileType = f.getName().substring(place).toLowerCase();
                return (0 == fileType.compareTo(".nas"));
            }
        }

        @Override
        public String getDescription() {
            return "nas hex dump file";
        }
    }

}