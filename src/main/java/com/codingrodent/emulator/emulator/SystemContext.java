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

import com.codingrodent.emulator.cards.ICPUControl;
import com.codingrodent.emulator.emulator.display.PrimaryDisplay;
import com.codingrodent.emulator.nas80Bus.CardController;
import org.apache.logging.log4j.*;

import java.util.List;

/**
 * Provides for a standardized system wide representation of the system preferences (systemPrefs.xml).
 */
public class SystemContext {

    private static final Logger logger = LogManager.getLogger(SystemContext.class);
    private static SystemContext instance;
    private ProcessEmulatorInfoFile emulatorInfo;
    private PrimaryDisplay primaryDisplay;
    private CardController cardController;

    /**
     * Standard constructor. Only one copy needed per VM so obtain via reference via createInstance(). Any failure here will cause a system exit().
     */
    private SystemContext() {
        try {
            // system settings
            emulatorInfo = new ProcessEmulatorInfoFile();
        } catch (Exception e) {
            String msg = "System failed to start in SystemContext : " + e.getMessage();
            logFatalEvent(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Obtain a references to a single instance of the class required throughout the whole application. If first reference, create the instance.
     *
     * @return Instance of a class
     */
    public static synchronized SystemContext createInstance() {
        if (null == instance) {
            instance = new SystemContext();
        }
        return instance;
    }

    /**
     * Log a debug event
     *
     * @param event Event string
     */
    public void logDebugEvent(String event) {
        logger.debug(event);
    }

    /**
     * Log an info event
     *
     * @param event Event string
     */
    public void logInfoEvent(String event) {
        logger.info(event);
    }

    /**
     * Log a warn event
     *
     * @param event Event string
     */
    public void logWarnEvent(String event) {
        logger.warn(event);
    }

    /**
     * Log an error event
     *
     * @param event Event string
     */
    public void logErrorEvent(String event) {
        logger.error(event);
    }

    /**
     * Log a fatal event
     *
     * @param event Event string
     */
    public void logFatalEvent(String event) {
        logger.fatal(event);
    }

    /**
     * Get the card set as derived from the emulatorInfo.xml
     *
     * @return An array holding all cards identified in the emulatorInfo.xml
     */
    public List<CardData> getAllCards() {
        return emulatorInfo.getAllCards();
    }

    /**
     * Get the primary display device
     *
     * @return The primary display
     */
    public PrimaryDisplay getPrimaryDisplay() {
        return primaryDisplay;
    }

    /**
     * Set the primary display device
     *
     * @param primaryDisplay Primary display
     */
    public void setPrimaryDisplay(PrimaryDisplay primaryDisplay) {
        this.primaryDisplay = primaryDisplay;
    }

    /**
     * Get the system card controller
     *
     * @return The card controller
     */
    public CardController getCardController() {
        return cardController;
    }

    /**
     * Set the primary display device
     *
     * @param cardController Card controller
     */
    void setCardController(CardController cardController) {
        this.cardController = cardController;
    }

    /**
     * Return the card in slot 0 which must be the CPU
     *
     * @return ICard Interface to the card
     */
    public ICPUControl getCPUCard() {
        return cardController.getCPU();
    }

}