package com.codingrodent.emulator.utilities;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class UtilitiesTest {
    @Test
    public void getByte() {
        assertEquals("00", Utilities.getByte(0));
        assertEquals("7F", Utilities.getByte(127));
        assertEquals("80", Utilities.getByte(-128));
        assertEquals("FF", Utilities.getByte(-1));
    }

    @Test
    public void getWord() {
        assertEquals("0000", Utilities.getWord(0));
        assertEquals("777F", Utilities.getWord(0x777F));
        assertEquals("8000", Utilities.getWord(0x8000));
        assertEquals("FFFF", Utilities.getWord(-1));
    }

    @Test
    public void getHexValue() {
        assertEquals(0, Utilities.getHexValue("00"));
        assertEquals(127, Utilities.getHexValue("7F"));
        assertEquals(128, Utilities.getHexValue("80"));
        assertEquals(255, Utilities.getHexValue("FF"));
    }

}