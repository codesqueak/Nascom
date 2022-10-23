/*
 * MIT License
 *
 * Copyright (c) 2017
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

import java.awt.event.*;
import java.util.Arrays;

class KeyboardHandler implements KeyListener, WindowFocusListener {

    private final int[] buffer = {0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff, 0x00ff};
    private final Keyboard keyboard;
    private boolean shiftEnabled;
    private boolean ctrlEnabled;
    private boolean graphEnabled;
    private long timeKeyPressed;

    /*
     * attach the keyboard device
     */
    KeyboardHandler(Keyboard keyboard) {
        this.keyboard = keyboard;
        shiftEnabled = false;
        ctrlEnabled = false;
        graphEnabled = false;
    }

    /**
     * Invoked when the Window is set to be the focused Window, which means that
     * the Window, or one of its subcomponents, will receive keyboard events.
     */
    @Override
    public void windowGainedFocus(WindowEvent e) {
    }

    /**
     * Invoked when the Window is no longer the focused Window, which means that
     * keyboard events will no longer be delivered to the Window or any of its
     * subcomponents.
     */
    @Override
    public void windowLostFocus(WindowEvent e) {
        Arrays.fill(buffer, 0x00FF);
        keyboard.setKeyStroke(buffer);
        shiftEnabled = false;
        ctrlEnabled = false;
        graphEnabled = false;
    }

    /**
     * @param e Typed key
     */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /*
     * send the keystroke to the keyboard device
     */
    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (buffer) {
            timeKeyPressed = System.nanoTime() / 1000000;
            //
            int keyCode = e.getKeyCode();
            //
            // Check for control keys, shift, ctrl and alt-graph
            if (KeyEvent.VK_SHIFT == keyCode) {
                shiftEnabled = true;
            }
            if (KeyEvent.VK_CONTROL == keyCode) {
                ctrlEnabled = true;
            }
            if (KeyEvent.VK_ALT_GRAPH == keyCode) {
                graphEnabled = true;
            }
            SetControlKeyBits();
            //
            if (shiftEnabled) {

                switch (keyCode) {
                    case KeyEvent.VK_6 -> keyCode = KeyEvent.VK_0;
                    case KeyEvent.VK_7 -> keyCode = KeyEvent.VK_6;
                    case KeyEvent.VK_8 -> keyCode = KeyEvent.VK_COLON;
                    case KeyEvent.VK_9 -> keyCode = KeyEvent.VK_8;
                    case KeyEvent.VK_0 -> keyCode = KeyEvent.VK_9;
                    case KeyEvent.VK_MINUS -> keyCode = KeyEvent.VK_CLOSE_BRACKET;
                    case KeyEvent.VK_SEMICOLON -> {
                        keyCode = KeyEvent.VK_COLON;
                        setSHIFT();
                    }
                    case KeyEvent.VK_EQUALS -> {
                        keyCode = KeyEvent.VK_SEMICOLON;
                        resetSHIFT();
                    }
                    case KeyEvent.VK_QUOTE -> {
                        keyCode = KeyEvent.VK_AT;
                        resetSHIFT();
                    }
                    case KeyEvent.VK_OPEN_BRACKET -> keyCode = KeyEvent.VK_BRACELEFT;
                    case KeyEvent.VK_CLOSE_BRACKET -> keyCode = KeyEvent.VK_BRACERIGHT;
                    default -> {
                    }
                }
            }

            //
            switch (keyCode) {
                // Control keys
                case KeyEvent.VK_ALT_GRAPH -> resetBit(5, 6);
                case KeyEvent.VK_CONTROL -> resetCTRL();
                case KeyEvent.VK_LEFT -> resetBit(2, 6);
                case KeyEvent.VK_UP -> resetBit(1, 6);
                case KeyEvent.VK_SPACE -> resetBit(7, 4);
                case KeyEvent.VK_DOWN -> resetBit(3, 6);
                case KeyEvent.VK_RIGHT -> resetBit(4, 6);
                case KeyEvent.VK_ENTER -> resetBit(0, 1);

                // case KeyEvent.VK_LF : { break; }
                case KeyEvent.VK_BACK_SPACE -> resetBit(0, 0);

                //
                // Other keys
                case KeyEvent.VK_COMMA -> // ,
                        resetBit(4, 1);
                case KeyEvent.VK_PERIOD -> // .
                        resetBit(5, 1);
                case KeyEvent.VK_SLASH -> resetBit(6, 1);
                case KeyEvent.VK_SEMICOLON -> resetBit(5, 0);
                case KeyEvent.VK_COLON -> resetBit(6, 0);
                case KeyEvent.VK_QUOTE -> { // @
                    resetSHIFT();
                    resetBit(3, 2);
                }
                case KeyEvent.VK_MINUS -> resetBit(0, 2);
                case KeyEvent.VK_EQUALS -> {
                    resetSHIFT();
                    resetBit(0, 2);
                }
                case KeyEvent.VK_OPEN_BRACKET -> resetBit(6, 6);
                case KeyEvent.VK_CLOSE_BRACKET -> resetBit(7, 6);
                case KeyEvent.VK_BACK_SLASH -> {
                    resetSHIFT(); // \
                    resetBit(6, 6);
                }
                case KeyEvent.VK_AT -> resetBit(0, 5);
                case KeyEvent.VK_BRACELEFT -> {
                    resetCTRL();
                    setSHIFT();
                    resetBit(5, 0);
                }
                case KeyEvent.VK_BRACERIGHT -> {
                    resetCTRL();
                    resetSHIFT();
                    resetBit(0, 2);
                }

                // A..Z
                case KeyEvent.VK_A -> resetBit(4, 4);
                case KeyEvent.VK_B -> resetBit(1, 1);
                case KeyEvent.VK_C -> resetBit(7, 3);
                case KeyEvent.VK_D -> resetBit(2, 3);
                case KeyEvent.VK_E -> resetBit(3, 3);
                case KeyEvent.VK_F -> resetBit(1, 3);
                case KeyEvent.VK_G -> resetBit(7, 0);
                case KeyEvent.VK_H -> resetBit(1, 0);
                case KeyEvent.VK_I -> resetBit(4, 5);
                case KeyEvent.VK_J -> resetBit(2, 0);
                case KeyEvent.VK_K -> resetBit(3, 0);
                case KeyEvent.VK_L -> resetBit(4, 0);
                case KeyEvent.VK_M -> resetBit(3, 1);
                case KeyEvent.VK_N -> resetBit(2, 1);
                case KeyEvent.VK_O -> resetBit(5, 5);
                case KeyEvent.VK_P -> resetBit(6, 5);
                case KeyEvent.VK_Q -> resetBit(5, 4);
                case KeyEvent.VK_R -> resetBit(7, 5);
                case KeyEvent.VK_S -> resetBit(3, 4);
                case KeyEvent.VK_T -> resetBit(1, 5);
                case KeyEvent.VK_U -> resetBit(3, 5);
                case KeyEvent.VK_V -> resetBit(7, 1);
                case KeyEvent.VK_W -> resetBit(4, 3);
                case KeyEvent.VK_X -> resetBit(1, 4);
                case KeyEvent.VK_Y -> resetBit(2, 5);
                case KeyEvent.VK_Z -> resetBit(2, 4);

                //
                case KeyEvent.VK_0 -> resetBit(6, 2);
                case KeyEvent.VK_1 -> resetBit(6, 4);
                case KeyEvent.VK_2 -> resetBit(6, 3);
                case KeyEvent.VK_3 -> resetBit(5, 3);
                case KeyEvent.VK_4 -> resetBit(7, 2);
                case KeyEvent.VK_5 -> resetBit(1, 2);
                case KeyEvent.VK_6 -> resetBit(2, 2);
                case KeyEvent.VK_7 -> resetBit(3, 2);
                case KeyEvent.VK_8 -> resetBit(4, 2);
                case KeyEvent.VK_9 -> resetBit(5, 2);


                // Special non-standard keys
                case KeyEvent.VK_ESCAPE -> {
                    resetBit(0, 1);
                    resetSHIFT();
                }
            }
            keyboard.setKeyStroke(buffer);
        }
    }

    private void resetCTRL() {
        resetBit(0, 3);
    }

    private void setCTRL() {
        setBit(0, 3);
    }

    private void resetSHIFT() {
        resetBit(0, 4);
    }

    private void setSHIFT() {
        setBit(0, 4);
    }

    /*
     * release the key press
     */
    @Override
    public void keyReleased(KeyEvent e) {
        synchronized (buffer) {
            // Put a delay in the keyboard routine so that key presses are not
            // eaten by the de-bounce s/w
            long time = (System.nanoTime() / 1000000) - timeKeyPressed;
            if (time < 25) {
                try {
                    Thread.sleep(25 - time);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            //
            int keyCode = e.getKeyCode();
            //
            // Check for control keys, shift, ctrl and alt-graph
            if (KeyEvent.VK_SHIFT == keyCode) {
                shiftEnabled = false;
                Arrays.fill(buffer, 0xFF);
            }
            if (KeyEvent.VK_CONTROL == keyCode) {
                ctrlEnabled = false;
            }
            if (KeyEvent.VK_ALT_GRAPH == keyCode) {
                graphEnabled = false;
            }
            //
            if (shiftEnabled) {
                switch (keyCode) {
                    case KeyEvent.VK_6 -> keyCode = KeyEvent.VK_0;
                    case KeyEvent.VK_7 -> keyCode = KeyEvent.VK_6;
                    case KeyEvent.VK_8, KeyEvent.VK_SEMICOLON -> keyCode = KeyEvent.VK_COLON;
                    case KeyEvent.VK_9 -> keyCode = KeyEvent.VK_8;
                    case KeyEvent.VK_0 -> keyCode = KeyEvent.VK_9;
                    case KeyEvent.VK_MINUS -> keyCode = KeyEvent.VK_CLOSE_BRACKET;
                    case KeyEvent.VK_EQUALS -> keyCode = KeyEvent.VK_SEMICOLON;
                    case KeyEvent.VK_QUOTE -> keyCode = KeyEvent.VK_AT;
                    case KeyEvent.VK_OPEN_BRACKET -> keyCode = KeyEvent.VK_BRACELEFT;
                    case KeyEvent.VK_CLOSE_BRACKET -> keyCode = KeyEvent.VK_BRACERIGHT;
                    default -> {
                    }
                }
            }

            //
            switch (keyCode) {
                // Control keys
                case KeyEvent.VK_ALT_GRAPH -> setBit(5, 6);
                case KeyEvent.VK_CONTROL -> setCTRL();
                case KeyEvent.VK_LEFT -> setBit(2, 6);
                case KeyEvent.VK_UP -> setBit(1, 6);
                case KeyEvent.VK_SPACE -> setBit(7, 4);
                case KeyEvent.VK_DOWN -> setBit(3, 6);
                case KeyEvent.VK_RIGHT -> setBit(4, 6);
                case KeyEvent.VK_ENTER -> setBit(0, 1);

                // case KeyEvent.VK_LF : { break; }
                case KeyEvent.VK_BACK_SPACE -> setBit(0, 0);

                //
                // Other keys
                case KeyEvent.VK_COMMA -> // ,
                        setBit(4, 1);
                case KeyEvent.VK_PERIOD -> // .
                        setBit(5, 1);
                case KeyEvent.VK_SLASH -> setBit(6, 1);
                case KeyEvent.VK_SEMICOLON -> setBit(5, 0);
                case KeyEvent.VK_COLON -> setBit(6, 0);
                case KeyEvent.VK_QUOTE -> { // @
                    setSHIFT();
                    setBit(3, 2);
                }
                case KeyEvent.VK_MINUS -> setBit(0, 2);
                case KeyEvent.VK_EQUALS -> {
                    setSHIFT();
                    setBit(0, 2);
                }
                case KeyEvent.VK_OPEN_BRACKET -> {
                    setSHIFT();
                    setBit(6, 6);
                }
                case KeyEvent.VK_CLOSE_BRACKET -> {
                    setSHIFT();
                    setBit(7, 6);
                }
                case KeyEvent.VK_BACK_SLASH -> {
                    setSHIFT(); // [
                    setBit(6, 6);
                }
                case KeyEvent.VK_AT -> setBit(0, 5);
                case KeyEvent.VK_BRACELEFT -> {
                    setCTRL();
                    setBit(5, 0);
                }
                case KeyEvent.VK_BRACERIGHT -> {
                    setCTRL();
                    setSHIFT();
                    setBit(0, 2);
                }

                // A..Z
                case KeyEvent.VK_A -> setBit(4, 4);
                case KeyEvent.VK_B -> setBit(1, 1);
                case KeyEvent.VK_C -> setBit(7, 3);
                case KeyEvent.VK_D -> setBit(2, 3);
                case KeyEvent.VK_E -> setBit(3, 3);
                case KeyEvent.VK_F -> setBit(1, 3);
                case KeyEvent.VK_G -> setBit(7, 0);
                case KeyEvent.VK_H -> setBit(1, 0);
                case KeyEvent.VK_I -> setBit(4, 5);
                case KeyEvent.VK_J -> setBit(2, 0);
                case KeyEvent.VK_K -> setBit(3, 0);
                case KeyEvent.VK_L -> setBit(4, 0);
                case KeyEvent.VK_M -> setBit(3, 1);
                case KeyEvent.VK_N -> setBit(2, 1);
                case KeyEvent.VK_O -> setBit(5, 5);
                case KeyEvent.VK_P -> setBit(6, 5);
                case KeyEvent.VK_Q -> setBit(5, 4);
                case KeyEvent.VK_R -> setBit(7, 5);
                case KeyEvent.VK_S -> setBit(3, 4);
                case KeyEvent.VK_T -> setBit(1, 5);
                case KeyEvent.VK_U -> setBit(3, 5);
                case KeyEvent.VK_V -> setBit(7, 1);
                case KeyEvent.VK_W -> setBit(4, 3);
                case KeyEvent.VK_X -> setBit(1, 4);
                case KeyEvent.VK_Y -> setBit(2, 5);
                case KeyEvent.VK_Z -> setBit(2, 4);

                //
                case KeyEvent.VK_0 -> setBit(6, 2);
                case KeyEvent.VK_1 -> setBit(6, 4);
                case KeyEvent.VK_2 -> setBit(6, 3);
                case KeyEvent.VK_3 -> setBit(5, 3);
                case KeyEvent.VK_4 -> setBit(7, 2);
                case KeyEvent.VK_5 -> setBit(1, 2);
                case KeyEvent.VK_6 -> setBit(2, 2);
                case KeyEvent.VK_7 -> setBit(3, 2);
                case KeyEvent.VK_8 -> setBit(4, 2);
                case KeyEvent.VK_9 -> setBit(5, 2);


                // Special non-standard keys
                case KeyEvent.VK_ESCAPE -> setBit(0, 1);
            }
            SetControlKeyBits();
            keyboard.setKeyStroke(buffer);
        }
    }

    /**
     * Process the bits for the control keys
     */
    private void SetControlKeyBits() {
        if (shiftEnabled) {
            resetSHIFT();
        } else {
            setSHIFT();
        }
        if (ctrlEnabled) {
            resetCTRL();
        } else {
            setCTRL();
        }
        if (graphEnabled) {
            resetBit(5, 6);
        } else {
            setBit(5, 6);
        }
    }

    /**
     * Set a keyboard bit corresponding to the key press
     *
     * @param sequenceByte int Which byte in the sequence, 0..7
     * @param bit          int Which bit in the byte
     */
    private void setBit(int sequenceByte, int bit) {
        int item = buffer[sequenceByte];
        item = switch (bit) {
            default -> item | 0x01;
            case 1 -> item | 0x02;
            case 2 -> item | 0x04;
            case 3 -> item | 0x08;
            case 4 -> item | 0x10;
            case 5 -> item | 0x20;
            case 6 -> item | 0x40;
            case 7 -> item | 0x80;
        };
        buffer[sequenceByte] = item;
    }

    /**
     * Set a keyboard bit corresponding to the key press
     *
     * @param sequenceByte int Which byte in the sequence, 0..7
     * @param bit          int Which bit in the byte
     */
    private void resetBit(int sequenceByte, int bit) {
        int item = buffer[sequenceByte];
        item = switch (bit) {
            default -> item & 0xFE;
            case 1 -> item & 0xFD;
            case 2 -> item & 0xFB;
            case 3 -> item & 0xF7;
            case 4 -> item & 0xEF;
            case 5 -> item & 0xDF;
            case 6 -> item & 0xBF;
            case 7 -> item & 0x7F;
        };
        buffer[sequenceByte] = item;
    }
}
