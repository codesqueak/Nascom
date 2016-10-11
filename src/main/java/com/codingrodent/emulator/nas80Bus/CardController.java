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

package com.codingrodent.emulator.nas80Bus;

import com.codingrodent.emulator.cards.*;
import com.codingrodent.emulator.emulator.*;
import com.codingrodent.emulator.utilities.*;

import javax.swing.*;
import java.io.*;
import java.util.List;

public class CardController {

    private final static int MAXIMUM_CARDS = 10;
    private final static int SEGMENT_SIZE = 1024;
    private final static int MEMORY_SLOTS = 65536 / SEGMENT_SIZE;
    private final static int ACTIVE_PORTS = 256;
    private final Object[] cardSlots = new Object[MAXIMUM_CARDS];
    private final SystemContext systemContext;
    private final NasBus nasBus;
    private int cardsLoaded;

    /**
     * Standard constructor for the card frame
     */
    public CardController() {
        systemContext = SystemContext.createInstance();
        //
        nasBus = new NasBus();
        //
        /* set all the card slots and memory slots to empty */
        for (int i = 0; i < MAXIMUM_CARDS; i++) {
            cardSlots[i] = null;
        }
    }

    /**
     * Load the cards into the bus in sequence as defined in emulatorInfo.xml
     */
    public void insertCards() {
        cardsLoaded = 0;
        List<CardData> cards = systemContext.getAllCards();
        if (cards.isEmpty()) {
            String msg = "No cards defined in emulatorInfo.xml";
            systemContext.logErrorEvent(msg);
            throw new RuntimeException(msg);
        } else {
            int slot = 0;
            for (final CardData cardData : cards) {
                String className = cardData.getClazz();
                try {
                    Class<?> card = Class.forName(className);
                    ICard genericCard = (ICard) card.newInstance();
                    genericCard.setCardName(cardData.getName());
                    genericCard.setCardProperties(cardData.getProperties());
                    cardData.setDetails(genericCard.getCardDetails());
                    genericCard.setNasBus(nasBus);
                    genericCard.initialise();
                    if ((0 == slot) && (!genericCard.isCPU())) {
                        String msg = "Card 0 must be a CPU, claims to be a <" + ((ICard) cards.get(0)).getCardName() + ">";
                        systemContext.logErrorEvent(msg);
                        throw new RuntimeException(msg);
                    }
                    insertCard(genericCard, slot++);
                    cardsLoaded++;
                } catch (Exception e) {
                    String msg = "Failed to load card, <" + e.getMessage() + ">";
                    systemContext.logErrorEvent(msg);
                    throw new RuntimeException(msg);
                }
            }
        }
        nasBus.initialise(this);
    }

    /**
     * Load a card into the card frame
     *
     * @param card The card to load
     * @param slot Frame card slot to load
     */
    private void insertCard(ICard card, int slot) {
        /* load the card */
        cardSlots[slot] = card;
        systemContext.logInfoEvent("Loading " + card.getCardName() + " into bus slot " + slot);
    }

    /**
     * Load a program into the emulator
     *
     * @param fileName The full file access path
     */
    public void loadProgram(String fileName) {
        try {
            FileHandler fileHandler = new FileHandler();
            MemoryChunk temp = fileHandler.readHexDumpFile(fileName);
            short[] tempMemory = temp.getMemoryChunk();
            int base = temp.getBase();
            int length = temp.getSize();
            for (int address = 0; address < length; address++) {
                nasBus.memoryWriteAll(base + address, tempMemory[address]);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Dump the whole 64K as a single image
     *
     * @param fileName String File name to dump to
     * @param frame    JFrame Frame in which to display error dialog
     */
    public void dumpMemory(String fileName, JFrame frame) {
        INasBus cpuCard = (INasBus) cardSlots[0];
        int checksum;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
            for (int address = 0; address < 65536; address = address + 8) {
                checksum = (address >> 8) + (address & 0x00FF);
                writer.write(Utilities.getWord(address) + " ");
                for (int column = 0; column < 8; column++) {
                    int data = cpuCard.memoryRead(address + column);
                    checksum = checksum + data;
                    writer.write(Utilities.getByte(data) + " ");
                }
                writer.write(Utilities.getByte(checksum & 0x00FF));
                writer.write(0);
                writer.write(0);
                writer.write(13);
            }
            writer.write('.');
            writer.close();
        } catch (Exception e) {
            System.out.println("Unable to write file " + e);
            JOptionPane.showMessageDialog(frame, "Unable to write the file:\n" + fileName, "Write File", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Attach the card set to the GUI
     */
    public void attachCardsToGUI() {
        systemContext.getPrimaryDisplay().displayMenu();
    }

    /**
     * Get the card loaded into a slot
     *
     * @param cardNumber Bus slot
     * @return ICard The standard card interface
     */
    public ICard getCard(int cardNumber) {
        return (ICard) cardSlots[cardNumber];
    }

    /**
     * Get the CPU card
     *
     * @return ICPU The CPU card interface
     */
    public ICPUControl getCPU() {
        return (ICPUControl) cardSlots[0];
    }

    /**
     * Discover the number of cards loaded into the system
     *
     * @return Card count
     */
    public int getCardsLoaded() {
        return cardsLoaded;
    }

    /**
     * Discover the number of slots the memory map has been split into
     *
     * @return The number of slots memory is broken into
     */
    public int getMemorySlots() {
        return MEMORY_SLOTS;
    }

    /**
     * Get the card loaded into a slot
     *
     * @param cardNumber Bus slot
     * @return The 80-Bus interface for the card
     */
    public INasBus getCardNasBus(int cardNumber) {
        return (INasBus) cardSlots[cardNumber];
    }

    /**
     * Discover the number of ports active in the memory map
     *
     * @return The number of ports
     */
    public int getActivePorts() {
        return ACTIVE_PORTS;
    }

    /**
     * Discover the segment size for the memory map
     *
     * @return Segment size
     */
    public int getSegmentSize() {
        return SEGMENT_SIZE;
    }
}