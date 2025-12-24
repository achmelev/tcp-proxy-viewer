package com.tcpviewer.util;

import com.tcpviewer.model.DataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TextDetector.
 */
class TextDetectorTest {

    private TextDetector detector;

    @BeforeEach
    void setUp() {
        detector = new TextDetector();
    }

    @Test
    void testDetectNull() {
        DataType result = detector.detect(null);
        assertEquals(DataType.TEXT, result, "Null should be detected as TEXT");
    }

    @Test
    void testDetectEmpty() {
        DataType result = detector.detect(new byte[0]);
        assertEquals(DataType.TEXT, result, "Empty array should be detected as TEXT");
    }

    @Test
    void testDetectValidUTF8() {
        String text = "Hello, World! 你好世界";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        DataType result = detector.detect(data);

        assertEquals(DataType.TEXT, result, "Valid UTF-8 should be detected as TEXT");
    }

    @Test
    void testDetectPureASCII() {
        String text = "Hello World 123!";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        DataType result = detector.detect(data);

        assertEquals(DataType.TEXT, result, "Pure ASCII should be detected as TEXT");
    }

    @Test
    void testDetectBinaryData() {
        // Random binary data (low printable ratio)
        byte[] data = {0x00, 0x01, 0x02, 0x03, (byte) 0xFF, (byte) 0xFE, (byte) 0xFD};

        DataType result = detector.detect(data);

        assertEquals(DataType.BINARY, result, "Binary data should be detected as BINARY");
    }

    @Test
    void testDetectAboveThreshold() {
        // 71% printable (above 0.7 threshold): 10 printable out of 14 bytes
        byte[] data = {
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, // 10 printable ('A'-'J')
            0x00, 0x01, 0x02, 0x03  // 4 non-printable
        };

        DataType result = detector.detect(data);

        assertEquals(DataType.TEXT, result, "Data above 70% threshold should be TEXT");
    }

    @Test
    void testDetectBelowThreshold() {
        // 50% printable (below 0.7 threshold): 5 printable out of 10 bytes
        byte[] data = {
            0x41, 0x42, 0x43, 0x44, 0x45, // 5 printable
            0x00, 0x01, 0x02, 0x03, 0x04  // 5 non-printable
        };

        DataType result = detector.detect(data);

        assertEquals(DataType.BINARY, result, "Data below 70% threshold should be BINARY");
    }

    @Test
    void testDetectAtThreshold() {
        // Exactly 70% printable: 7 printable out of 10 bytes
        byte[] data = {
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, // 7 printable
            0x00, 0x01, 0x02  // 3 non-printable
        };

        DataType result = detector.detect(data);

        assertEquals(DataType.TEXT, result, "Data at exactly 70% threshold should be TEXT");
    }

    @Test
    void testDetectWithWhitespace() {
        // Whitespace characters (tab, newline, CR) should count as text
        byte[] data = {
            0x48, 0x65, 0x6C, 0x6C, 0x6F, // "Hello"
            0x09, // tab
            0x0A, // newline
            0x0D  // carriage return
        };

        DataType result = detector.detect(data);

        assertEquals(DataType.TEXT, result, "Data with whitespace should be TEXT");
    }

    @Test
    void testIsTextEncodingTrue() {
        String text = "This is text";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        boolean result = detector.isTextEncoding(data);

        assertTrue(result, "isTextEncoding should return true for text data");
    }

    @Test
    void testIsTextEncodingFalse() {
        // Mostly non-printable binary data
        byte[] data = {0x00, 0x01, 0x02, 0x03, (byte) 0xFF, (byte) 0xFE};

        boolean result = detector.isTextEncoding(data);

        assertFalse(result, "isTextEncoding should return false for binary data");
    }

    @Test
    void testDecodeTextUTF8() {
        String original = "Hello UTF-8! 你好";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);

        String result = detector.decodeText(data);

        assertEquals(original, result, "Valid UTF-8 should be decoded as UTF-8");
    }

    @Test
    void testDecodeTextISO88591Fallback() {
        // Create invalid UTF-8 data (should fall back to ISO-8859-1)
        byte[] data = {(byte) 0xC0, (byte) 0xC1, (byte) 0xF5}; // Invalid UTF-8 sequences

        String result = detector.decodeText(data);

        // Should not throw exception, should fall back to ISO-8859-1
        assertNotNull(result);
        // ISO-8859-1 preserves byte values as-is
        assertEquals(3, result.length());
    }

    @Test
    void testPrintableASCIIRange() {
        // Test boundaries: 32 (space) and 126 (~)
        byte[] spaceTilde = {0x20, 0x7E}; // space and ~
        byte[] belowAbove = {0x1F, 0x7F}; // just below and above range

        assertTrue(detector.isTextEncoding(spaceTilde),
                   "Bytes 32 and 126 should be printable");
        assertFalse(detector.isTextEncoding(belowAbove),
                    "Bytes 31 and 127 should not be printable");
    }

    @Test
    void testEmptyIsTextEncoding() {
        boolean result = detector.isTextEncoding(new byte[0]);
        assertTrue(result, "Empty array should return true for isTextEncoding");
    }

    @Test
    void testMixedASCIIAndBinary() {
        // Mix of printable and non-printable to test threshold logic
        byte[] data = {
            // Printable: A-Z (26 bytes)
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A,
            0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54,
            0x55, 0x56, 0x57, 0x58, 0x59, 0x5A,
            // Non-printable: 8 bytes (total 34 bytes, 76% printable > 70%)
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07
        };

        DataType result = detector.detect(data);

        assertEquals(DataType.TEXT, result, "Mixed data above threshold should be TEXT");
    }
}
