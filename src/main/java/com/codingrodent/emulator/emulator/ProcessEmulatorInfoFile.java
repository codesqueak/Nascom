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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * This class holds an internal representation of the machineInfo.xml document.
 */
class ProcessEmulatorInfoFile {

    private static final String EMULATOR_INFO_FILE = "EmulatorInfo.json";
    private List<CardData> cardInfo;

    /**
     * Standard constructor - Loads the machineInfo.xml document and resets
     * system ready for use. Will wait and poll until the document becomes
     * available.
     *
     * @throws ProcessEmulatorInfoFileException Thrown if unable to recover the document
     */
    ProcessEmulatorInfoFile() throws ProcessEmulatorInfoFileException {
        try (Reader reader = new InputStreamReader(new FileInputStream(EMULATOR_INFO_FILE), StandardCharsets.UTF_8)) {
            Gson gson = new GsonBuilder().create();
            CardData[] cardData = gson.fromJson(reader, CardData[].class);
            Arrays.sort(cardData, Comparator.comparingInt(CardData::getOrder));
            cardInfo = Arrays.asList(cardData);
        } catch (IOException e) {
            throw new ProcessEmulatorInfoFileException(e);
        }
    }

    /**
     * Get the card set as derived from the emulatorInfo.xml
     *
     * @return An array holding all cards identified in the emulatorInfo.xml
     */
    List<CardData> getAllCards() {
        return cardInfo;
    }
}