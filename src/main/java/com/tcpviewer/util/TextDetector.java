package com.tcpviewer.util;

import com.tcpviewer.model.DataType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for detecting whether byte data is text or binary.
 */
@Component
public class TextDetector {

    private static final double TEXT_THRESHOLD = 0.7;

    /**
     * Detects whether the given data is text or binary.
     *
     * @param data The byte array to analyze
     * @return DATA_TYPE.TEXT if data appears to be text, DATA_TYPE.BINARY otherwise
     */
    public DataType detect(byte[] data) {
        if (data == null || data.length == 0) {
            return DataType.TEXT;
        }

        if (isTextEncoding(data)) {
            return DataType.TEXT;
        }

        return DataType.BINARY;
    }

    /**
     * Determines if data is likely text based on heuristics.
     *
     * @param data The byte array to analyze
     * @return true if data appears to be text
     */
    public boolean isTextEncoding(byte[] data) {
        if (data.length == 0) {
            return true;
        }

        // Check if data is valid UTF-8
        if (isValidUTF8(data)) {
            return true;
        }

        // Check for high ratio of printable ASCII characters
        int printableCount = 0;
        for (byte b : data) {
            if (isPrintableASCII(b) || isWhitespace(b)) {
                printableCount++;
            }
        }

        double ratio = (double) printableCount / data.length;
        return ratio >= TEXT_THRESHOLD;
    }

    /**
     * Checks if data is valid UTF-8.
     */
    private boolean isValidUTF8(byte[] data) {
        try {
            String str = new String(data, StandardCharsets.UTF_8);
            byte[] reencoded = str.getBytes(StandardCharsets.UTF_8);

            if (data.length != reencoded.length) {
                return false;
            }

            // Check if most characters are printable
            int printableCount = 0;
            for (char c : str.toCharArray()) {
                if (Character.isWhitespace(c) || !Character.isISOControl(c)) {
                    printableCount++;
                }
            }

            double ratio = (double) printableCount / str.length();
            return ratio >= TEXT_THRESHOLD;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a byte represents a printable ASCII character (32-126).
     */
    private boolean isPrintableASCII(byte b) {
        return b >= 32 && b <= 126;
    }

    /**
     * Checks if a byte represents common whitespace characters.
     */
    private boolean isWhitespace(byte b) {
        return b == 9 || b == 10 || b == 13; // tab, newline, carriage return
    }

    /**
     * Decodes byte array to text string.
     * Tries UTF-8 first, falls back to ISO-8859-1 to preserve byte values.
     *
     * @param data The byte array to decode
     * @return Decoded string
     */
    public String decodeText(byte[] data) {
        if (isValidUTF8(data)) {
            return new String(data, StandardCharsets.UTF_8);
        } else {
            return new String(data, StandardCharsets.ISO_8859_1);
        }
    }
}
