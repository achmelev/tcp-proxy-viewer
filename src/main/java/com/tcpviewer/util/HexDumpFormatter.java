package com.tcpviewer.util;

import org.springframework.stereotype.Component;

/**
 * Utility class for formatting binary data as hex dump.
 */
@Component
public class HexDumpFormatter {

    private static final int BYTES_PER_LINE = 16;
    private static final String HEX_DIGITS = "0123456789ABCDEF";

    /**
     * Formats byte array as hex dump with offset and ASCII sidebar.
     * Format: "0000: 48 65 6C 6C 6F 20 57 6F 72 6C 64 21          Hello World!"
     *
     * @param data The byte array to format
     * @return Formatted hex dump string
     */
    public String format(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int offset = 0;

        while (offset < data.length) {
            // Offset column
            sb.append(String.format("%04X: ", offset));

            // Hex bytes
            int bytesInLine = Math.min(BYTES_PER_LINE, data.length - offset);
            for (int i = 0; i < BYTES_PER_LINE; i++) {
                if (i < bytesInLine) {
                    byte b = data[offset + i];
                    sb.append(toHex(b)).append(' ');
                } else {
                    sb.append("   "); // padding for missing bytes
                }

                // Extra space in the middle for readability
                if (i == 7) {
                    sb.append(' ');
                }
            }

            // ASCII representation
            sb.append(" ");
            for (int i = 0; i < bytesInLine; i++) {
                byte b = data[offset + i];
                if (b >= 32 && b <= 126) {
                    sb.append((char) b);
                } else {
                    sb.append('.');
                }
            }

            sb.append('\n');
            offset += bytesInLine;
        }

        return sb.toString();
    }

    /**
     * Formats a single byte as two-character hex string.
     */
    private String toHex(byte b) {
        int value = b & 0xFF;
        return "" + HEX_DIGITS.charAt(value >> 4) + HEX_DIGITS.charAt(value & 0x0F);
    }

    /**
     * Formats byte array as compact hex string (no spaces or formatting).
     *
     * @param data The byte array to format
     * @return Hex string like "48656C6C6F"
     */
    public String formatCompact(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(toHex(b));
        }
        return sb.toString();
    }
}
