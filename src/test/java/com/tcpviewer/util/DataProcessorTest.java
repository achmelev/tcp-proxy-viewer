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
 * Uses REAL TextDetector and HexDumpFormatter instances (not mocks).
 */
class DataProcessorTest {

    private DataProcessor dataProcessor;
    private TextDetector textDetector;
    private HexDumpFormatter hexDumpFormatter;

    @BeforeEach
    void setUp() {
        // Use real utility instances as per guideline
        textDetector = new TextDetector();
        hexDumpFormatter = new HexDumpFormatter();
        dataProcessor = new DataProcessor(textDetector, hexDumpFormatter);
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
        assertEquals(DataType.BINARY, packet.getDataType());
        // Should be formatted as hex dump
        assertTrue(packet.getDisplayText().contains("0000:"));
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
        // Text data but force BINARY type
        byte[] data = "Hello".getBytes();

        DataPacket packet = dataProcessor.processWithType(data, Direction.CLIENT_TO_SERVER, DataType.BINARY);

        assertEquals(DataType.BINARY, packet.getDataType());
        // Should be formatted as hex dump even though it's text
        assertTrue(packet.getDisplayText().contains("0000:"));
        assertTrue(packet.getDisplayText().contains("Hello")); // ASCII section
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

        // Test 1: Text data uses TextDetector.decodeText()
        String textInput = "Test message";
        byte[] textData = textInput.getBytes(StandardCharsets.UTF_8);
        DataPacket textPacket = dataProcessor.process(textData, Direction.CLIENT_TO_SERVER);

        assertEquals(DataType.TEXT, textPacket.getDataType());
        assertEquals(textInput, textPacket.getDisplayText());

        // Test 2: Binary data uses HexDumpFormatter.format()
        byte[] binaryData = {0x00, 0x01, 0x02};
        DataPacket binaryPacket = dataProcessor.process(binaryData, Direction.SERVER_TO_CLIENT);

        assertEquals(DataType.BINARY, binaryPacket.getDataType());
        String hexOutput = hexDumpFormatter.format(binaryData);
        assertEquals(hexOutput, binaryPacket.getDisplayText());
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
}
