package com.tcpviewer.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TextFormatter.
 */
class TextFormatterTest {

    private TextFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new TextFormatter();
    }

    @Test
    void testConvertToDisplayTextNull() {
        String result = formatter.convertToDisplayText(null);
        assertEquals("", result, "Null data should return empty string");
    }

    @Test
    void testConvertToDisplayTextEmpty() {
        String result = formatter.convertToDisplayText(new byte[0]);
        assertEquals("", result, "Empty data should return empty string");
    }

    @Test
    void testConvertToDisplayTextPrintableASCII() {
        String original = "Hello, World!";
        byte[] data = original.getBytes(StandardCharsets.ISO_8859_1);

        String result = formatter.convertToDisplayText(data);

        assertEquals(original, result, "Printable ASCII should remain unchanged");
    }

    @Test
    void testConvertToDisplayTextReplacesC0Controls() {
        // All C0 controls except TAB, LF, CR should become '?'
        byte[] data = {0x00, 0x01, 0x08, 0x0B, 0x0C, 0x0E, 0x1F};

        String result = formatter.convertToDisplayText(data);

        assertEquals("???????", result, "C0 control characters should be replaced with '?'");
    }

    @Test
    void testConvertToDisplayTextPreservesWhitespace() {
        byte[] data = {0x09, 0x0A, 0x0D}; // TAB, LF, CR

        String result = formatter.convertToDisplayText(data);

        assertEquals("\t\n\r", result, "TAB, LF, and CR should be preserved");
    }

    @Test
    void testConvertToDisplayTextReplacesDeleteAndC1Controls() {
        // DEL (0x7F) and C1 controls (0x80-0x9F)
        byte[] data = {0x7F, (byte) 0x80, (byte) 0x85, (byte) 0x9F};

        String result = formatter.convertToDisplayText(data);

        assertEquals("????", result, "DEL and C1 controls should be replaced with '?'");
    }

    @Test
    void testConvertToDisplayTextPreservesExtendedLatin1() {
        // Characters 0xA0-0xFF (printable extended Latin-1)
        byte[] data = {(byte) 0xA0, (byte) 0xC4, (byte) 0xE9, (byte) 0xFF};
        // nbsp, Ä, é, ÿ

        String result = formatter.convertToDisplayText(data);

        assertEquals("\u00A0\u00C4\u00E9\u00FF", result,
                     "Extended Latin-1 characters should be preserved");
    }

    @Test
    void testConvertToDisplayTextMixedContent() {
        // Mix of printable, whitespace, and control characters
        byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x00, 0x09, 0x57, 0x6F, 0x72, 0x6C, 0x64, 0x7F};
        // "Hello" + NULL + TAB + "World" + DEL

        String result = formatter.convertToDisplayText(data);

        assertEquals("Hello?\tWorld?", result,
                     "Mixed content should have controls replaced and whitespace preserved");
    }

    @Test
    void testConvertToDisplayTextAllC0ControlsExceptWhitespace() {
        // Test all C0 controls (0x00-0x1F) individually
        for (int i = 0; i <= 0x1F; i++) {
            byte[] data = {(byte) i};
            String result = formatter.convertToDisplayText(data);

            if (i == 0x09 || i == 0x0A || i == 0x0D) {
                // TAB, LF, CR should be preserved
                assertNotEquals("?", result,
                               String.format("Byte 0x%02X should be preserved", i));
            } else {
                // All other C0 controls should become '?'
                assertEquals("?", result,
                            String.format("Byte 0x%02X should be replaced with '?'", i));
            }
        }
    }

    @Test
    void testConvertToDisplayTextAllC1Controls() {
        // Test all C1 controls (0x80-0x9F) should become '?'
        for (int i = 0x80; i <= 0x9F; i++) {
            byte[] data = {(byte) i};
            String result = formatter.convertToDisplayText(data);

            assertEquals("?", result,
                        String.format("Byte 0x%02X should be replaced with '?'", i));
        }
    }

    @Test
    void testConvertToDisplayTextExtendedLatin1Range() {
        // Test extended Latin-1 printable range (0xA0-0xFF) should be preserved
        for (int i = 0xA0; i <= 0xFF; i++) {
            byte[] data = {(byte) i};
            String result = formatter.convertToDisplayText(data);

            assertEquals(1, result.length(),
                        String.format("Byte 0x%02X should produce one character", i));
            assertNotEquals("?", result,
                           String.format("Byte 0x%02X should not be replaced with '?'", i));
        }
    }

    @Test
    void testConvertToDisplayTextUsesISO88591Encoding() {
        // Verify ISO-8859-1 encoding preserves byte values for printable chars
        byte[] data = {(byte) 0xC4, (byte) 0xD6, (byte) 0xDC}; // Ä, Ö, Ü in ISO-8859-1

        String result = formatter.convertToDisplayText(data);

        assertEquals("ÄÖÜ", result, "ISO-8859-1 characters should be decoded correctly");
    }

    @Test
    void testConvertToDisplayTextLongData() {
        // Test with larger data to ensure performance is reasonable
        byte[] data = new byte[1000];
        for (int i = 0; i < 1000; i++) {
            data[i] = (byte) (0x41 + (i % 26)); // Cycle through A-Z
        }

        String result = formatter.convertToDisplayText(data);

        assertEquals(1000, result.length(), "Long data should be fully converted");
        assertFalse(result.contains("?"), "Printable ASCII should not be replaced");
    }
}
