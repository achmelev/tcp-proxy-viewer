package com.tcpviewer.proxy;

import com.tcpviewer.io.wrapper.InputStreamWrapper;
import com.tcpviewer.io.wrapper.OutputStreamWrapper;
import com.tcpviewer.model.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TcpForwarder.
 * Tests data forwarding, listener notification, and resource cleanup.
 */
@ExtendWith(MockitoExtension.class)
class TcpForwarderTest {

    /**
     * Test stub for InputStreamWrapper to avoid Mockito issues with AutoCloseable on JDK 25.
     */
    private static class TestInputStreamWrapper implements InputStreamWrapper {
        private final List<byte[]> dataToReturn = new ArrayList<>();
        private int readIndex = 0;
        private IOException readException;
        private IOException closeException;
        public boolean wasClosed = false;
        public int readCallCount = 0;

        public void addData(byte[] data) {
            dataToReturn.add(data);
        }

        public void setReadException(IOException ex) {
            this.readException = ex;
        }

        public void setCloseException(IOException ex) {
            this.closeException = ex;
        }

        @Override
        public int read(byte[] b) throws IOException {
            readCallCount++;
            if (readException != null) {
                throw readException;
            }
            if (readIndex >= dataToReturn.size()) {
                return -1; // EOF
            }
            byte[] data = dataToReturn.get(readIndex++);
            System.arraycopy(data, 0, b, 0, data.length);
            return data.length;
        }

        @Override
        public void close() throws IOException {
            wasClosed = true;
            if (closeException != null) {
                throw closeException;
            }
        }
    }

    /**
     * Test stub for OutputStreamWrapper to avoid Mockito issues with AutoCloseable on JDK 25.
     */
    private static class TestOutputStreamWrapper implements OutputStreamWrapper {
        public final List<byte[]> writtenData = new ArrayList<>();
        public int flushCount = 0;
        public int writeCallCount = 0;
        public boolean wasClosed = false;
        private IOException writeException;
        private IOException closeException;

        public void setWriteException(IOException ex) {
            this.writeException = ex;
        }

        public void setCloseException(IOException ex) {
            this.closeException = ex;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            writeCallCount++;
            if (writeException != null) {
                throw writeException;
            }
            byte[] copy = new byte[len];
            System.arraycopy(b, off, copy, 0, len);
            writtenData.add(copy);
        }

        @Override
        public void flush() throws IOException {
            flushCount++;
        }

        @Override
        public void close() throws IOException {
            wasClosed = true;
            if (closeException != null) {
                throw closeException;
            }
        }
    }

    @Mock
    private DataCaptureListener mockListener;

    private UUID connectionId;
    private TestInputStreamWrapper testSource;
    private TestOutputStreamWrapper testDestination;

    @BeforeEach
    void setUp() {
        connectionId = UUID.randomUUID();
        testSource = new TestInputStreamWrapper();
        testDestination = new TestOutputStreamWrapper();
    }

    @Test
    void testForwardsDataFromSourceToDestination() throws IOException {
        // Arrange
        byte[] testData = "Hello World".getBytes();
        testSource.addData(testData);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert
        assertEquals(2, testSource.readCallCount); // One read with data, one read returns -1
        assertEquals(1, testDestination.writeCallCount);
        assertEquals(1, testDestination.flushCount);
        assertEquals(1, testDestination.writtenData.size());
        assertArrayEquals(testData, testDestination.writtenData.get(0));
    }

    @Test
    void testNotifiesListenerWithCapturedData() throws IOException {
        // Arrange
        byte[] testData = "Test Data".getBytes();
        testSource.addData(testData);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.SERVER_TO_CLIENT, "Test");

        // Act
        forwarder.run();

        // Assert
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockListener).onDataCaptured(
                eq(connectionId),
                dataCaptor.capture(),
                eq(Direction.SERVER_TO_CLIENT)
        );

        byte[] capturedData = dataCaptor.getValue();
        assertArrayEquals(testData, capturedData);
    }

    @Test
    void testDefensiveCopyOfCapturedData() throws IOException {
        // Arrange
        byte[] testData = "Original".getBytes();
        testSource.addData(testData);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert - verify the captured data is a copy, not the buffer
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockListener).onDataCaptured(
                eq(connectionId),
                dataCaptor.capture(),
                eq(Direction.CLIENT_TO_SERVER)
        );

        byte[] capturedData = dataCaptor.getValue();

        // Should contain the same data
        assertArrayEquals(testData, capturedData);

        // Should be exactly testData.length, not buffer size (8192)
        assertEquals(testData.length, capturedData.length);
    }

    @Test
    void testForwardsMultipleChunks() throws IOException {
        // Arrange
        byte[] chunk1 = "First ".getBytes();
        byte[] chunk2 = "Second".getBytes();
        testSource.addData(chunk1);
        testSource.addData(chunk2);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert
        assertEquals(3, testSource.readCallCount); // Two reads with data, one read returns -1
        assertEquals(2, testDestination.writeCallCount);
        assertEquals(2, testDestination.flushCount);
        verify(mockListener, times(2)).onDataCaptured(
                eq(connectionId), any(byte[].class), eq(Direction.CLIENT_TO_SERVER)
        );
    }

    @Test
    void testHandlesEndOfStream() throws IOException {
        // Arrange - no data added, so immediate EOF

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert - no writes or listener calls
        assertEquals(1, testSource.readCallCount);
        assertEquals(0, testDestination.writeCallCount);
        assertEquals(0, testDestination.flushCount);
        verify(mockListener, never()).onDataCaptured(any(), any(), any());
    }

    @Test
    void testHandlesNullListener() throws IOException {
        // Arrange
        byte[] testData = "Test".getBytes();
        testSource.addData(testData);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, null, // null listener
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act - should not throw NPE
        assertDoesNotThrow(() -> forwarder.run());

        // Assert - data still forwarded
        assertEquals(1, testDestination.writeCallCount);
        assertEquals(1, testDestination.flushCount);
    }

    @Test
    void testHandlesIOExceptionDuringRead() throws IOException {
        // Arrange
        testSource.setReadException(new IOException("Connection reset"));

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act - should not propagate exception
        assertDoesNotThrow(() -> forwarder.run());

        // Assert - streams closed
        assertTrue(testSource.wasClosed);
        assertTrue(testDestination.wasClosed);
    }

    @Test
    void testHandlesIOExceptionDuringWrite() throws IOException {
        // Arrange
        byte[] testData = "Test".getBytes();
        testSource.addData(testData);
        testDestination.setWriteException(new IOException("Broken pipe"));

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act - should not propagate exception
        assertDoesNotThrow(() -> forwarder.run());

        // Assert - streams closed
        assertTrue(testSource.wasClosed);
        assertTrue(testDestination.wasClosed);
    }

    @Test
    void testStreamsClosedInFinallyBlock() throws IOException {
        // Arrange
        byte[] testData = "Test".getBytes();
        testSource.addData(testData);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert - both streams closed even in success case
        assertTrue(testSource.wasClosed);
        assertTrue(testDestination.wasClosed);
    }

    @Test
    void testStreamsClosedEvenWhenCloseThrowsException() throws Exception {
        // Arrange
        testSource.setCloseException(new IOException("Close failed"));

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act - should not propagate exception from close()
        assertDoesNotThrow(() -> forwarder.run());

        // Assert - both close() attempts made (closeQuietly swallows exceptions)
        assertTrue(testSource.wasClosed);
        assertTrue(testDestination.wasClosed);
    }

    @Test
    void testFlushCalledAfterEachWrite() throws IOException {
        // Arrange
        byte[] testData = "Data".getBytes();
        testSource.addData(testData);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert - flush called once (for one write)
        assertEquals(1, testDestination.flushCount);
        assertEquals(1, testDestination.writeCallCount);
    }

    @Test
    void testCorrectDirectionPassedToListener() throws IOException {
        // Arrange
        byte[] testData = "Test".getBytes();
        testSource.addData(testData);

        // Test CLIENT_TO_SERVER
        TcpForwarder forwarder1 = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "C2S");

        forwarder1.run();

        verify(mockListener).onDataCaptured(
                eq(connectionId), any(byte[].class), eq(Direction.CLIENT_TO_SERVER)
        );

        // Reset for second test
        reset(mockListener);
        TestInputStreamWrapper testSource2 = new TestInputStreamWrapper();
        TestOutputStreamWrapper testDestination2 = new TestOutputStreamWrapper();
        testSource2.addData(testData);

        // Test SERVER_TO_CLIENT
        TcpForwarder forwarder2 = new TcpForwarder(
                testSource2, testDestination2, mockListener,
                connectionId, Direction.SERVER_TO_CLIENT, "S2C");

        forwarder2.run();

        verify(mockListener).onDataCaptured(
                eq(connectionId), any(byte[].class), eq(Direction.SERVER_TO_CLIENT)
        );
    }
}
