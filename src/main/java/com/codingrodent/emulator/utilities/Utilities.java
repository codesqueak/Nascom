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

package com.codingrodent.emulator.utilities;

public class Utilities {
    private final static String HEX_CHAR = "0123456789ABCDEF";

    /*
      Constructor
     */
    private Utilities() {
    }

    /*
      turn a 4 bit value into its equivalent hex digit
     */
    private static char getHexCharacter(short value) {
        return HEX_CHAR.charAt(value);
    }

    /*
      turn a byte into two hex digits
     */
    public static String getByte(int value) {

        char[] byteText = new char[2];
        value = value & 0x00FF;
        byteText[0] = getHexCharacter((short) (value >> 4));
        byteText[1] = getHexCharacter((short) (value & 0x0F));
        return new String(byteText);
    }

    /*
      turn a word into four hex digits
     */
    public static String getWord(int value) {
        return getByte(value >>> 8) + getByte(value & 0x00FF);
    }

    /*
      convert a hex digit into an int
     */
    private static int getHexDigit(char hex) {
        int i;
        for (i = 0; i < HEX_CHAR.length(); i++) {
            if (HEX_CHAR.charAt(i) == hex)
                break;
        }
        return i;
    }

    /*
      convert a hex string into an integer
     */
    public static int getHexValue(String hex) {
        int total = 0;
        for (int i = 0; i < hex.length(); i++) {
            total = total * 16 + getHexDigit(hex.charAt(i));
        }
        return total;
    }

}
