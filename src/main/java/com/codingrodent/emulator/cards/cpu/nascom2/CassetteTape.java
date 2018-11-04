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

import javax.swing.*;
import java.io.*;

class CassetteTape {
    private static final int TAPE_DRIVE_LED = 0x010;
    private final SystemContext systemContext;
    private boolean tapeLED;
    private FileInputStream tapeFileInput;
    private FileOutputStream tapeFileOutput;
    private String readFileName;
    private int readAheadChar;

    CassetteTape() {
        systemContext = SystemContext.createInstance();
        tapeLED = false;
        tapeFileInput = null;
        tapeFileOutput = null;
    }

    /**
     * Control the tape LED
     *
     * @param data Byte written to port 0
     */
    void controlLED(int data) {
        tapeLED = 0 != (data & TAPE_DRIVE_LED);
    }

    /**
     * Write data to the UART data register, port 1
     *
     * @param data UART data
     */
    void writeDataToUART(int data) {
	try {
	    if (null != tapeFileOutput) {
		tapeFileOutput.write(data);
	    }
	} catch (Exception e) {
	    systemContext.logErrorEvent("Problem encountered writing to tape file. " + e.getMessage());
	}
    }

    /**
     * Read data from the UART data register, port 1
     *
     * @return data UART Data
     */
    int readDataFromUART() {
	int oneChar = readAheadChar;
	fillReadAhead ();    
	return oneChar;
    }

    /**
     * Store a char from input in readAheadChar; close input stream if we reach the end.
     */
    void fillReadAhead () {
	try {
	    if (tapeFileInput == null) return;
	    readAheadChar = tapeFileInput.read ();
	    if (readAheadChar < 0) { // EOF reached
		tapeFileInput.close ();
		tapeFileInput = null;
	    }
	} catch (IOException e) {
	    systemContext.logErrorEvent ("Problem encountered reading tape file " + e.getMessage ());
	    try {
		tapeFileInput.close();
	    } catch (IOException ignored) { }
	    tapeFileInput = null;
	}
    }

    /**
     * Read status from the UART status register, port 2
     *
     * @return Data received flag if an input file exists; transmit buffer should always be empty
     */
    int readStatusFromUART() {
        int status = 0x40;
        if (null != tapeFileInput ) {
            status = status | 0x80;
        }
        return status;
    }

    /**
     * Set the input stream ready for reading
     *
     * @param readFileName The file to read from
     */
    private void setTapeRead(String readFileName) {
        if (null != tapeFileInput) {
            try {
                tapeFileInput.close();
            } catch (IOException e) {
                systemContext.logErrorEvent("Problem encountered reading tape file. " + e.getMessage());
            }
        }
        tapeFileInput = null;
        try {
            systemContext.logInfoEvent("Looking for tape to read: " + readFileName);
            tapeFileInput = new FileInputStream(readFileName);
            this.readFileName = readFileName;
        } catch (FileNotFoundException e) {
            systemContext.logErrorEvent("Unable to find tape to read: " + readFileName);
        }
    }

    /**
     * Set the output stream ready for writing
     *
     * @param writeFileName The file to write to
     */
    private void setTapeWrite(String writeFileName) {
        if (null != tapeFileOutput) {
            try {
                tapeFileOutput.close();
            } catch (Exception e) {
                systemContext.logErrorEvent("Problem encountered writing to tape file. " + e.getMessage());
            }
        }
        tapeFileOutput = null;
        try {
            systemContext.logInfoEvent("Looking for tape to write: " + readFileName);
            tapeFileOutput = new FileOutputStream(writeFileName);
        } catch (FileNotFoundException e) {
            systemContext.logErrorEvent("Unable to find tape to write: " + readFileName);
        }
    }

    /**
     * Load a new tape image
     */
    void loadNewTape() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setFileFilter(new dskFileFilter());
        int returnValue = fc.showOpenDialog(systemContext.getPrimaryDisplay());
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String fileName = file.getAbsolutePath();
            systemContext.logDebugEvent("Loading tape file from " + fileName);
            setTapeRead(fileName);
        }
    }
    /**
     * Close input tape
     */
    void closeInput () {
	if (tapeFileInput != null) {
	    try {
		tapeFileInput.close ();
	    } catch (IOException ignored) { }
	    tapeFileInput = null;
	}
    }
    /**
     * Return true if an input tape is open currently
     */
    boolean isInputOpen () {
	return tapeFileInput != null;
    }
    /**
     * Set a new tape image to write to
     */
    void saveNewTape() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setFileFilter(new dskFileFilter());
        int returnValue = fc.showSaveDialog(systemContext.getPrimaryDisplay());
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String fileName = file.getAbsolutePath();
            systemContext.logDebugEvent("Saving tape file to " + fileName);
            setTapeWrite(fileName);
        }
    }
    /**
     * Close output tape
     */
    void closeOutput () {
	if (tapeFileOutput != null) {
	    try {
		tapeFileOutput.close ();
	    } catch (IOException ignored) { }
	    tapeFileOutput = null;
	}
    }
    /**
     * Return true if an output tape is open currently
     */
    boolean isOutputOpen () {
	return tapeFileOutput != null;
    }

    /**
     * inner class for the file filter (.cas / .bas)
     */
    private static class dskFileFilter extends javax.swing.filechooser.FileFilter {
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
                return ((0 == fileType.compareTo(".cas")) || (0 == fileType.compareTo(".bas")));
            }
        }

        /**
         * Description for file type in dialog box
         */
        @Override
        public String getDescription() {
            return "Cassette Image File";
        }
    }
}
