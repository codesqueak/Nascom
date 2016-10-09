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

package com.codingrodent.emulator.emulator;

import java.util.Properties;

public class CardData {

    private final Properties properties;
    private String cardDetails;
    private String cardName;
    private String className;

    CardData() {
        properties = new Properties();
        cardName = "-- Undefined --";
        className = null;
    }

    public String getName() {
        return cardName;
    }

    void setName(String name) {
        cardName = name;
    }

    public String getDetails() {
        return cardDetails;
    }

    public void setDetails(String details) {
        cardDetails = details;
    }

    void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public String getClassName() {
        return className;
    }

    void setClassName(String name) {
        className = name;
    }

    public Properties getProperties() {
        return properties;
    }

}