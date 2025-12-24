package com.tcpviewer.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for formatting byte data as text using ISO-8859-1 encoding.
 */
@Component
public class TextFormatter {

    /**
     * Converts byte array to display text using ISO-8859-1 encoding.
     * Replaces control characters with '?' except for TAB, LF, and CR.
     *
     * @param data The byte array to convert
     * @return Display text with control characters replaced
     */
    public String convertToDisplayText(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        // Decode using ISO-8859-1 (preserves all byte values 0x00-0xFF)
        String decoded = new String(data, StandardCharsets.ISO_8859_1);

        // Replace control characters with '?'
        StringBuilder result = new StringBuilder(decoded.length());
        for (char c : decoded.toCharArray()) {
            if (shouldReplaceWithQuestionMark(c)) {
                result.append('?');
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Determines if a character should be replaced with '?'.
     * Control characters except TAB (0x09), LF (0x0A), and CR (0x0D) are replaced.
     */
    private boolean shouldReplaceWithQuestionMark(char c) {
        int value = (int) c;

        // C0 controls (0x00-0x1F) except TAB, LF, CR
        if (value <= 0x1F) {
            return value != 0x09 && value != 0x0A && value != 0x0D;
        }

        // DEL (0x7F) and C1 controls (0x80-0x9F)
        if (value == 0x7F || (value >= 0x80 && value <= 0x9F)) {
            return true;
        }

        return false;
    }
}
