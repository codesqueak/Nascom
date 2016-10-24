package com.codingrodent.emulator.cards.nascommemory;

import com.codingrodent.emulator.cards.ram.Nascom32KRAMA;
import org.junit.*;

import java.util.*;

import static com.codingrodent.emulator.nas80Bus.INasBus.NO_MEMORY_PRESENT;
import static org.junit.Assert.*;

/**
 *
 */
public class Nascom32KRAMATest {

    private Map<String, String> cardProperties;
    private Nascom32KRAMA ramA;

    @Before
    public void setUp() throws Exception {
        cardProperties = new HashMap<String, String>();
        cardProperties.put("StartAddress", "1000");
        cardProperties.put("Size", "32K");
        cardProperties.put("ROMEnabled", "true");
        cardProperties.put("ROM", "resources/hexdumpImages/ROM/test/TEST_4K.nas");
        cardProperties.put("ROMAddress", "D000");
        cardProperties.put("EPROMType", "2708");
        //
        ramA = new Nascom32KRAMA();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void initialise() throws Exception {
        ramA.setCardProperties(cardProperties);
        ramA.initialise();
        //
        // Check RAM
        assertFalse(ramA.isRAM(0X0FFF));
        assertTrue(ramA.isRAM(0X1000));
        assertTrue(ramA.isRAM(0X8FFF));
        assertFalse(ramA.isRAM(0X9000));
        //
        // Check ROM
        assertFalse(ramA.isROM(0XCFFF));
        assertTrue(ramA.isROM(0XD000));
        assertTrue(ramA.isROM(0XDFFF));
        assertFalse(ramA.isROM(0XE000));
        //
        // Check I/O
        assertFalse(ramA.isInputPort(0x00));
        assertFalse(ramA.isOutputPort(0x00));
        //
        // Card info
        ramA.setCardName("Test RAM 'A'");
        assertEquals("Test RAM 'A'", ramA.getCardName());
        assertEquals("Nascom RAM 'A' Card - Version 1.0", ramA.getCardDetails());
        //
        // Not a CPU card
        assertFalse(ramA.isCPU());
        //
        // RAMDIS
        assertFalse(ramA.assertRAMDIS(0x0000));
        assertFalse(ramA.assertRAMDIS(0x1000));
        assertFalse(ramA.assertRAMDIS(0xCFFF));
        assertTrue(ramA.assertRAMDIS(0xD000));
        assertFalse(ramA.assertRAMDIS(0xE000));
        //
        // RAMDIS Capable
        assertFalse(ramA.assertRAMDISCapable(0x0000));
        assertFalse(ramA.assertRAMDISCapable(0x1000));
        assertFalse(ramA.assertRAMDISCapable(0xCFFF));
        assertTrue(ramA.assertRAMDISCapable(0xD000));
        assertFalse(ramA.assertRAMDISCapable(0xE000));
    }

    @Test
    public void setNasBus() throws Exception {

    }

    @Test
    public void memoryReadWrite() throws Exception {
        ramA.setCardProperties(cardProperties);
        ramA.initialise();
        //
        // No RAMDIS assert
        assertEquals(0x007F, ramA.memoryRead(0x0000));
        assertEquals(0x0000, ramA.memoryRead(0x1000));
        assertFalse(ramA.memoryWrite(0x0000, 0x0012, false));
        assertTrue(ramA.memoryWrite(0x1000, 0x0012, false));
        assertFalse(ramA.memoryWrite(0xCFFF, 0x0012, false));
        assertFalse(ramA.memoryWrite(0xD000, 0x0012, false));
        assertEquals(0x0012, ramA.memoryRead(0x1000));
        //
        // RAMDIS with read
        assertEquals(0x0012, ramA.memoryRead(0x1000, false));
        assertEquals(NO_MEMORY_PRESENT, ramA.memoryRead(0x1000, true));
        //
        // RAMDIS assert
        assertEquals(0x007F, ramA.memoryRead(0x0000));
        assertEquals(0x0000, ramA.memoryRead(0x1001));
        assertFalse(ramA.memoryWrite(0x0000, 0x0034, true));
        assertFalse(ramA.memoryWrite(0x1001, 0x0034, true));
        assertFalse(ramA.memoryWrite(0xCFFF, 0x0034, true));
        assertFalse(ramA.memoryWrite(0xD000, 0x0034, true));
        assertEquals(0x0000, ramA.memoryRead(0x1001));
    }

    @Test
    public void noEffect() throws Exception {
        ramA.reset();
        ramA.actionPerformed(null);
        ramA.ioRead(0x00);
        ramA.ioWrite(0x00, 0x00);
        ramA.halt();
        assertEquals(-1, ramA.getClock());
        ramA.setClock(0);
        ramA.setNasBus(null);
    }

}