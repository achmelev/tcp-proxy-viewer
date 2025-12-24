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
        private final List<Integer> availableAfterEachRead = new ArrayList<>();
        private int readIndex = 0;
        private IOException readException;
        private IOException closeException;
        private IOException availableException;
        public boolean wasClosed = false;
        public int readCallCount = 0;
        public int availableCallCount = 0;

        public void addData(byte[] data) {
            addData(data, 0);
        }

        public void addData(byte[] data, int availableAfter) {
            dataToReturn.add(data);
            availableAfterEachRead.add(availableAfter);
        }

        public void setReadException(IOException ex) {
            this.readException = ex;
        }

        public void setCloseException(IOException ex) {
            this.closeException = ex;
        }

        public void setAvailableException(IOException ex) {
            this.availableException = ex;
        }

        @Override
        public int read(byte[] b) throws IOException {
            readCallCount++;
            if (readIndex >= dataToReturn.size()) {
                // Throw exception after all data has been read (if set)
                if (readException != null) {
                    throw readException;
                }
                return -1; // EOF
            }
            byte[] data = dataToReturn.get(readIndex++);
            System.arraycopy(data, 0, b, 0, data.length);
            return data.length;
        }

        @Override
        public int available() throws IOException {
            availableCallCount++;
            if (availableException != null) {
                throw availableException;
            }
            if (readIndex > 0 && readIndex <= availableAfterEachRead.size()) {
                return availableAfterEachRead.get(readIndex - 1);
            }
            return 0;
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
        testSource.addData(chunk1, 0);
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
        assertTrue(testSource.wasClosed);
        assertTrue(testDestination.wasClosed);
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

    @Test
    void testDrainThePipe_MultipleChunksConsolidated() throws Exception {
        // Arrange - multiple chunks with available() eventually returning 0
        byte[] chunk1 = "Hello ".getBytes();
        byte[] chunk2 = "World".getBytes();
        byte[] chunk3 = "!".getBytes();
        testSource.addData(chunk1, 100); // more data available
        testSource.addData(chunk2, 50);  // more data available
        testSource.addData(chunk3, 0);   // no more data, will trigger packet

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert - all data forwarded but only one listener call
        assertEquals(4, testSource.readCallCount); // 3 reads with data, 1 EOF
        assertEquals(3, testDestination.writeCallCount);
        assertEquals(3, testDestination.flushCount);

        // Verify single consolidated packet
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockListener, times(1)).onDataCaptured(
                eq(connectionId), dataCaptor.capture(), eq(Direction.CLIENT_TO_SERVER)
        );

        byte[] capturedData = dataCaptor.getValue();
        String expected = "Hello World!";
        assertArrayEquals(expected.getBytes(), capturedData);
    }

    @Test
    void testDrainThePipe_TimeoutWaitsForMoreData() throws Exception {
        // Arrange - single chunk with available() returning 0, timeout should occur
        byte[] testData = "Single chunk".getBytes();
        testSource.addData(testData, 0);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        long startTime = System.currentTimeMillis();

        // Act
        forwarder.run();

        long elapsedTime = System.currentTimeMillis() - startTime;

        // Assert - timeout should have occurred (at least 50ms)
        assertTrue(elapsedTime >= 50, "Expected at least 50ms delay, got: " + elapsedTime);

        verify(mockListener, times(1)).onDataCaptured(
                eq(connectionId), any(byte[].class), eq(Direction.CLIENT_TO_SERVER)
        );
    }

    @Test
    void testDrainThePipe_SingleChunkImmediate() throws Exception {
        // Arrange - single chunk, should still work with drain logic
        byte[] testData = "Test Data".getBytes();
        testSource.addData(testData, 0);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert
        assertEquals(1, testDestination.writeCallCount);
        verify(mockListener, times(1)).onDataCaptured(
                eq(connectionId), any(byte[].class), eq(Direction.CLIENT_TO_SERVER)
        );
    }

    @Test
    void testDrainThePipe_EOFWithAccumulated() throws Exception {
        // Arrange - multiple chunks accumulated, then EOF
        byte[] chunk1 = "First".getBytes();
        byte[] chunk2 = "Second".getBytes();
        testSource.addData(chunk1, 100); // more data indicated
        testSource.addData(chunk2, 100); // more data indicated but EOF follows

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        forwarder.run();

        // Assert - accumulated data sent at EOF
        assertEquals(3, testSource.readCallCount); // 2 reads with data, 1 EOF
        assertEquals(2, testDestination.writeCallCount);

        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockListener, times(1)).onDataCaptured(
                eq(connectionId), dataCaptor.capture(), eq(Direction.CLIENT_TO_SERVER)
        );

        byte[] capturedData = dataCaptor.getValue();
        String expected = "FirstSecond";
        assertArrayEquals(expected.getBytes(), capturedData);
    }

    @Test
    void testDrainThePipe_ErrorWithAccumulated() throws Exception {
        // Arrange - successfully forward and accumulate first chunk, then error on read
        byte[] chunk1 = "Data1".getBytes();
        testSource.addData(chunk1, 100); // Indicates more data available

        // Set read exception for second read - this allows first chunk to be accumulated
        testSource.setReadException(new IOException("Connection reset"));

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act
        assertDoesNotThrow(() -> forwarder.run());

        // Assert - first chunk should be captured in catch block before closing
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockListener, times(1)).onDataCaptured(
                eq(connectionId), dataCaptor.capture(), eq(Direction.CLIENT_TO_SERVER)
        );

        byte[] capturedData = dataCaptor.getValue();
        assertArrayEquals("Data1".getBytes(), capturedData);
    }

    @Test
    void testDrainThePipe_AvailableThrowsException() throws Exception {
        // Arrange - available() throws IOException
        byte[] testData = "Test".getBytes();
        testSource.addData(testData, 0);
        testSource.setAvailableException(new IOException("Available failed"));

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act - should handle gracefully and create packet
        assertDoesNotThrow(() -> forwarder.run());

        // Assert - data still forwarded and captured (treating as available==0)
        assertEquals(1, testDestination.writeCallCount);
        verify(mockListener, times(1)).onDataCaptured(
                eq(connectionId), any(byte[].class), eq(Direction.CLIENT_TO_SERVER)
        );
    }

    @Test
    void testDrainThePipe_InterruptedDuringSleep() throws Exception {
        // Arrange
        byte[] testData = "Test".getBytes();
        testSource.addData(testData, 0);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, mockListener,
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act - run in thread and interrupt during sleep
        Thread forwarderThread = new Thread(forwarder);
        forwarderThread.start();
        Thread.sleep(25); // Interrupt during the 50ms timeout
        forwarderThread.interrupt();
        forwarderThread.join(1000); // Wait up to 1 second

        // Assert - should complete gracefully despite interrupt
        assertTrue(testSource.wasClosed);
        assertTrue(testDestination.wasClosed);
        verify(mockListener, times(1)).onDataCaptured(
                eq(connectionId), any(byte[].class), eq(Direction.CLIENT_TO_SERVER)
        );
    }

    @Test
    void testDrainThePipe_NullListenerNoException() throws Exception {
        // Arrange - multiple chunks with null listener
        byte[] chunk1 = "Hello".getBytes();
        byte[] chunk2 = "World".getBytes();
        testSource.addData(chunk1, 100);
        testSource.addData(chunk2, 0);

        TcpForwarder forwarder = new TcpForwarder(
                testSource, testDestination, null, // null listener
                connectionId, Direction.CLIENT_TO_SERVER, "Test");

        // Act - should not throw NPE
        assertDoesNotThrow(() -> forwarder.run());

        // Assert - data still forwarded
        assertEquals(2, testDestination.writeCallCount);
        assertEquals(2, testDestination.flushCount);
    }
}
