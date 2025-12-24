package com.tcpviewer.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HexDumpFormatter.
 */
class HexDumpFormatterTest {

    private HexDumpFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new HexDumpFormatter();
    }

    @Test
    void testFormatNull() {
        String result = formatter.format(null);
        assertEquals("", result);
    }

    @Test
    void testFormatEmpty() {
        String result = formatter.format(new byte[0]);
        assertEquals("", result);
    }

    @Test
    void testFormatSingleByte() {
        byte[] data = {0x48}; // 'H'
        String result = formatter.format(data);

        // Expected: "0000: 48                                               H\n"
        assertTrue(result.startsWith("0000: 48 "));
        assertTrue(result.endsWith("H\n"));
        assertTrue(result.contains("                "), "Should have padding spaces");
    }

    @Test
    void testFormatSixteenBytes() {
        // "Hello World!!!!!" (16 bytes)
        byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x57, 0x6F,
                       0x72, 0x6C, 0x64, 0x21, 0x21, 0x21, 0x21, 0x21};

        String result = formatter.format(data);

        // Should have one complete line
        String[] lines = result.split("\n");
        assertEquals(1, lines.length);
        assertTrue(lines[0].startsWith("0000: "));
        assertTrue(lines[0].endsWith("Hello World!!!!!"));
    }

    @Test
    void testFormatMultipleLines() {
        // 32 bytes should create 2 lines
        byte[] data = new byte[32];
        for (int i = 0; i < 32; i++) {
            data[i] = (byte) (65 + (i % 26)); // A-Z repeated
        }

        String result = formatter.format(data);

        String[] lines = result.split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("0000: "));
        assertTrue(lines[1].startsWith("0010: "), "Second line should start with offset 0010");
    }

    @Test
    void testFormatIncompleteLastLine() {
        // 20 bytes: complete first line + 4 bytes on second line
        byte[] data = new byte[20];
        for (int i = 0; i < 20; i++) {
            data[i] = (byte) 65; // 'A'
        }

        String result = formatter.format(data);

        String[] lines = result.split("\n");
        assertEquals(2, lines.length);

        // Second line should have padding for 12 missing bytes (36 spaces: 3 per byte)
        String secondLine = lines[1];
        assertTrue(secondLine.contains("   "), "Should have padding spaces for missing bytes");
    }

    @Test
    void testFormatMiddleSpacing() {
        // Test that there's extra space after the 8th byte
        byte[] data = new byte[16];
        for (int i = 0; i < 16; i++) {
            data[i] = (byte) 0x41; // 'A'
        }

        String result = formatter.format(data);

        // After 8 hex bytes (8 * 3 = 24 chars) + offset (6 chars) + extra space = double space
        assertTrue(result.contains("41 41 41 41 41 41 41 41  41"),
                   "Should have double space after 8th byte");
    }

    @Test
    void testFormatASCIIPrintable() {
        // Test printable ASCII range (32-126)
        byte[] data = {0x20, 0x41, 0x5A, 0x61, 0x7A, 0x7E}; // space, A, Z, a, z, ~

        String result = formatter.format(data);

        assertTrue(result.endsWith(" AZaz~\n"), "Printable ASCII should appear as characters");
    }

    @Test
    void testFormatASCIINonPrintable() {
        // Test non-printable bytes (should render as dots)
        byte[] data = {0x00, 0x1F, 0x7F, (byte) 0xFF};

        String result = formatter.format(data);

        // All should be dots in ASCII section
        assertTrue(result.endsWith("....\n"), "Non-printable bytes should render as dots");
    }

    @Test
    void testFormatOffsetIncrement() {
        // Test that offset increments correctly
        byte[] data = new byte[48]; // 3 lines

        String result = formatter.format(data);

        String[] lines = result.split("\n");
        assertEquals(3, lines.length);
        assertTrue(lines[0].startsWith("0000: "));
        assertTrue(lines[1].startsWith("0010: "));
        assertTrue(lines[2].startsWith("0020: "));
    }

    @Test
    void testFormatCompactNull() {
        String result = formatter.formatCompact(null);
        assertEquals("", result);
    }

    @Test
    void testFormatCompactEmpty() {
        String result = formatter.formatCompact(new byte[0]);
        assertEquals("", result);
    }

    @Test
    void testFormatCompactValid() {
        // "Hello" should format as "48656C6C6F"
        byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F};

        String result = formatter.formatCompact(data);

        assertEquals("48656C6C6F", result);
    }

    @Test
    void testFormatCompactMixedBytes() {
        byte[] data = {0x00, (byte) 0xFF, 0x0A, (byte) 0xAB};

        String result = formatter.formatCompact(data);

        assertEquals("00FF0AAB", result);
    }
}
