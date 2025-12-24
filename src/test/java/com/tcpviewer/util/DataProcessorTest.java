package com.tcpviewer.util;

import com.tcpviewer.model.DataPacket;
import com.tcpviewer.model.DataType;
import com.tcpviewer.model.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataProcessor.
 * Uses REAL TextFormatter instance (not mocks).
 */
class DataProcessorTest {

    private DataProcessor dataProcessor;
    private TextFormatter textFormatter;

    @BeforeEach
    void setUp() {
        // Use real utility instances as per guideline
        textFormatter = new TextFormatter();
        dataProcessor = new DataProcessor(textFormatter);
    }

    @Test
    void testProcessTextData() {
        String text = "Hello, World!";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertNotNull(packet);
        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals(text, packet.getDisplayText());
        assertEquals(Direction.CLIENT_TO_SERVER, packet.getDirection());
        assertArrayEquals(data, packet.getRawData());
    }

    @Test
    void testProcessBinaryData() {
        byte[] data = {0x00, 0x01, 0x02, (byte) 0xFF};

        DataPacket packet = dataProcessor.process(data, Direction.SERVER_TO_CLIENT);

        assertNotNull(packet);
        assertEquals(DataType.TEXT, packet.getDataType());
        // 0x00-0x02 are control characters (replaced with '?'), 0xFF is 'ÿ' in ISO-8859-1
        assertEquals("???ÿ", packet.getDisplayText());
        assertEquals(Direction.SERVER_TO_CLIENT, packet.getDirection());
        assertArrayEquals(data, packet.getRawData());
    }

    @Test
    void testProcessPreservesDirection() {
        byte[] data = "test".getBytes();

        DataPacket clientToServer = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);
        DataPacket serverToClient = dataProcessor.process(data, Direction.SERVER_TO_CLIENT);

        assertEquals(Direction.CLIENT_TO_SERVER, clientToServer.getDirection());
        assertEquals(Direction.SERVER_TO_CLIENT, serverToClient.getDirection());
    }

    @Test
    void testProcessCreatesTimestamp() {
        byte[] data = "test".getBytes();

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertNotNull(packet.getTimestamp(), "Timestamp should be created");
    }

    @Test
    void testProcessCopiesRawData() {
        byte[] data = "original".getBytes();

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertNotNull(packet.getRawData());
        assertArrayEquals(data, packet.getRawData());
        // Verify it's a copy (not the same instance)
        assertNotSame(data, packet.getRawData());
    }

    @Test
    void testProcessWithTypeText() {
        // Binary data but force TEXT type
        byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F}; // "Hello"

        DataPacket packet = dataProcessor.processWithType(data, Direction.CLIENT_TO_SERVER, DataType.TEXT);

        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("Hello", packet.getDisplayText());
    }

    @Test
    void testProcessWithTypeBinary() {
        // Text data but force BINARY type (which is now ignored)
        byte[] data = "Hello".getBytes();

        DataPacket packet = dataProcessor.processWithType(data, Direction.CLIENT_TO_SERVER, DataType.BINARY);

        // BINARY type is deprecated and ignored - always returns TEXT
        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("Hello", packet.getDisplayText());
    }

    @Test
    void testProcessClientToServer() {
        byte[] data = "request".getBytes();

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertEquals(Direction.CLIENT_TO_SERVER, packet.getDirection());
        assertEquals("request", packet.getDisplayText());
    }

    @Test
    void testProcessServerToClient() {
        byte[] data = "response".getBytes();

        DataPacket packet = dataProcessor.process(data, Direction.SERVER_TO_CLIENT);

        assertEquals(Direction.SERVER_TO_CLIENT, packet.getDirection());
        assertEquals("response", packet.getDisplayText());
    }

    @Test
    void testIntegrationWithUtilities() {
        // End-to-end test with real utilities

        // Test 1: Text data uses TextFormatter.convertToDisplayText()
        String textInput = "Test message";
        byte[] textData = textInput.getBytes(StandardCharsets.UTF_8);
        DataPacket textPacket = dataProcessor.process(textData, Direction.CLIENT_TO_SERVER);

        assertEquals(DataType.TEXT, textPacket.getDataType());
        assertEquals(textInput, textPacket.getDisplayText());

        // Test 2: Binary data also converted to text with control chars replaced
        byte[] binaryData = {0x00, 0x01, 0x02};
        DataPacket binaryPacket = dataProcessor.process(binaryData, Direction.SERVER_TO_CLIENT);

        assertEquals(DataType.TEXT, binaryPacket.getDataType());
        assertEquals("???", binaryPacket.getDisplayText());
    }

    @Test
    void testProcessEmptyData() {
        byte[] data = new byte[0];

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertNotNull(packet);
        // Empty data detected as TEXT
        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("", packet.getDisplayText());
    }

    @Test
    void testProcessNullData() {
        // DataPacket constructor doesn't handle null gracefully, so process() will throw NPE
        // This is a known limitation - null data should be handled before calling process()
        assertThrows(NullPointerException.class,
                     () -> dataProcessor.process(null, Direction.CLIENT_TO_SERVER),
                     "Processing null data should throw NPE");
    }

    @Test
    void testProcessControlCharactersReplaced() {
        // Test C0 controls (excluding TAB, LF, CR)
        byte[] data = {0x00, 0x01, 0x08, 0x0B, 0x0C, 0x0E, 0x1F, 0x7F};

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("????????", packet.getDisplayText());
    }

    @Test
    void testProcessPreservesWhitespace() {
        // TAB (0x09), LF (0x0A), CR (0x0D) should be preserved
        byte[] data = {0x48, 0x09, 0x65, 0x0A, 0x6C, 0x0D, 0x6F}; // "H\te\nl\ro"

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("H\te\nl\ro", packet.getDisplayText());
    }

    @Test
    void testProcessExtendedLatin1Characters() {
        // Test extended Latin-1 range (0xA0-0xFF printable)
        byte[] data = {(byte) 0xC4, (byte) 0xD6, (byte) 0xDC}; // Ä, Ö, Ü

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("ÄÖÜ", packet.getDisplayText());
    }

    @Test
    void testProcessC1ControlCharactersReplaced() {
        // C1 controls (0x80-0x9F) should be replaced
        byte[] data = {(byte) 0x80, (byte) 0x85, (byte) 0x9F};

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("???", packet.getDisplayText());
    }

    @Test
    void testProcessUsesISO88591Encoding() {
        // Verify ISO-8859-1 encoding preserves byte values
        byte[] data = {(byte) 0xE0, (byte) 0xE9, (byte) 0xFC}; // à, é, ü in ISO-8859-1

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("àéü", packet.getDisplayText());
    }

    @Test
    void testProcessMixedContent() {
        // Mix of printable, whitespace, and control characters
        byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x00, 0x09, 0x57, 0x6F, 0x72, 0x6C, 0x64};
        // "Hello" + NULL + TAB + "World"

        DataPacket packet = dataProcessor.process(data, Direction.CLIENT_TO_SERVER);

        assertEquals(DataType.TEXT, packet.getDataType());
        assertEquals("Hello?\tWorld", packet.getDisplayText());
    }
}
