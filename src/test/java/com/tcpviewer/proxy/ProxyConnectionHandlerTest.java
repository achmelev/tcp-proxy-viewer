package com.tcpviewer.proxy;

import com.tcpviewer.io.wrapper.InputStreamWrapper;
import com.tcpviewer.io.wrapper.OutputStreamWrapper;
import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.io.wrapper.factory.SocketFactory;
import com.tcpviewer.lang.wrapper.ThreadWrapper;
import com.tcpviewer.lang.wrapper.factory.ThreadFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProxyConnectionHandler.
 * Tests bidirectional TCP connection handling and thread coordination.
 */
@ExtendWith(MockitoExtension.class)
class ProxyConnectionHandlerTest {

    /**
     * Test stub for SocketWrapper to avoid Mockito issues with AutoCloseable on JDK 25.
     */
    private static class TestSocketWrapper implements SocketWrapper {
        private final InputStreamWrapper inputStream;
        private final OutputStreamWrapper outputStream;
        private boolean closed = false;
        private boolean tcpNoDelay = false;
        private IOException closeException;

        public TestSocketWrapper(InputStreamWrapper inputStream, OutputStreamWrapper outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        public void setCloseException(IOException ex) {
            this.closeException = ex;
        }

        @Override
        public InputStreamWrapper getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public OutputStreamWrapper getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void setTcpNoDelay(boolean on) throws IOException {
            this.tcpNoDelay = on;
        }

        public boolean getTcpNoDelay() {
            return tcpNoDelay;
        }

        @Override
        public java.net.InetAddress getInetAddress() {
            return null; // Not needed for these tests
        }

        @Override
        public int getPort() {
            return 0; // Not needed for these tests
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            if (closeException != null) {
                throw closeException;
            }
        }
    }

    @Mock
    private SocketFactory mockSocketFactory;

    @Mock
    private ThreadFactory mockThreadFactory;

    /**
     * Simple test stub for InputStreamWrapper.
     */
    private static class TestInputStreamWrapper implements InputStreamWrapper {
        @Override
        public int read(byte[] b) throws IOException {
            return -1; // EOF
        }

        @Override
        public void close() throws IOException {
        }
    }

    /**
     * Simple test stub for OutputStreamWrapper.
     */
    private static class TestOutputStreamWrapper implements OutputStreamWrapper {
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    /**
     * Test stub for ThreadWrapper to avoid Mockito issues with Runnable on JDK 25.
     */
    private static class TestThreadWrapper implements ThreadWrapper {
        private boolean started = false;
        private boolean joined = false;
        private InterruptedException joinException;
        private final String name;

        public TestThreadWrapper(String name) {
            this.name = name;
        }

        public void setJoinException(InterruptedException ex) {
            this.joinException = ex;
        }

        @Override
        public void run() {
            // Not needed for these tests
        }

        @Override
        public void start() {
            started = true;
        }

        @Override
        public void join() throws InterruptedException {
            joined = true;
            if (joinException != null) {
                throw joinException;
            }
        }

        @Override
        public void join(long millis) throws InterruptedException {
            join();
        }

        @Override
        public void interrupt() {
        }

        @Override
        public boolean isAlive() {
            return started && !joined;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setDaemon(boolean on) {
        }

        @Override
        public void setName(String name) {
        }

        @Override
        public boolean isInterrupted() {
            return false;
        }

        public boolean wasStarted() {
            return started;
        }

        public boolean wasJoined() {
            return joined;
        }
    }

    @Mock
    private DataCaptureListener mockListener;

    private TestSocketWrapper clientSocket;
    private TestSocketWrapper targetSocket;
    private UUID connectionId;
    private String targetHost;
    private int targetPort;
    private TestThreadWrapper testThread1;
    private TestThreadWrapper testThread2;
    private TestThreadWrapper testCurrentThread;

    @BeforeEach
    void setUp() {
        connectionId = UUID.randomUUID();
        targetHost = "example.com";
        targetPort = 80;

        // Create test sockets with test stream wrappers
        clientSocket = new TestSocketWrapper(
                new TestInputStreamWrapper(),
                new TestOutputStreamWrapper()
        );
        targetSocket = new TestSocketWrapper(
                new TestInputStreamWrapper(),
                new TestOutputStreamWrapper()
        );

        // Create test threads
        testThread1 = new TestThreadWrapper("Thread-1");
        testThread2 = new TestThreadWrapper("Thread-2");
        testCurrentThread = new TestThreadWrapper("Current-Thread");
    }

    @Test
    void testSuccessfulConnectionHandling() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);

        // Capture the runnables passed to thread factory
        AtomicReference<Runnable> clientToTargetRunnable = new AtomicReference<>();
        AtomicReference<Runnable> targetToClientRunnable = new AtomicReference<>();

        when(mockThreadFactory.createThread(any(Runnable.class), contains("C2T")))
                .thenAnswer(invocation -> {
                    clientToTargetRunnable.set(invocation.getArgument(0));
                    return testThread1;
                });

        when(mockThreadFactory.createThread(any(Runnable.class), contains("T2C")))
                .thenAnswer(invocation -> {
                    targetToClientRunnable.set(invocation.getArgument(0));
                    return testThread2;
                });

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act
        handler.run();

        // Assert
        verify(mockSocketFactory).createSocket(targetHost, targetPort);
        assertTrue(targetSocket.getTcpNoDelay());
        assertNotNull(clientToTargetRunnable.get());
        assertNotNull(targetToClientRunnable.get());
        assertTrue(testThread1.wasStarted());
        assertTrue(testThread2.wasStarted());
        assertTrue(testThread1.wasJoined());
        assertTrue(testThread2.wasJoined());
        assertTrue(clientSocket.isClosed());
        assertTrue(targetSocket.isClosed());
        verify(mockListener).onConnectionClosed(connectionId);
    }

    @Test
    void testIOExceptionDuringTargetConnection() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort))
                .thenThrow(new IOException("Connection refused"));

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act
        assertDoesNotThrow(() -> handler.run());

        // Assert
        verify(mockSocketFactory).createSocket(targetHost, targetPort);
        verify(mockThreadFactory, never()).createThread(any(), anyString());
        assertTrue(clientSocket.isClosed());
        verify(mockListener).onConnectionClosed(connectionId);
    }

    @Test
    void testInterruptedExceptionDuringJoin() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);

        when(mockThreadFactory.createThread(any(Runnable.class), contains("C2T")))
                .thenReturn(testThread1);
        when(mockThreadFactory.createThread(any(Runnable.class), contains("T2C")))
                .thenReturn(testThread2);

        // Simulate InterruptedException during join
        testThread1.setJoinException(new InterruptedException("Thread interrupted"));
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act
        assertDoesNotThrow(() -> handler.run());

        // Assert
        assertTrue(testThread1.wasStarted());
        assertTrue(testThread2.wasStarted());
        assertTrue(clientSocket.isClosed());
        assertTrue(targetSocket.isClosed());
        verify(mockListener).onConnectionClosed(connectionId);
    }

    @Test
    void testNullListenerDoesNotCauseNPE() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);
        when(mockThreadFactory.createThread(any(Runnable.class), anyString()))
                .thenReturn(testThread1)
                .thenReturn(testThread2);

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, null, // null listener
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act & Assert - should not throw NPE
        assertDoesNotThrow(() -> handler.run());
        assertTrue(clientSocket.isClosed());
        assertTrue(targetSocket.isClosed());
    }

    @Test
    void testExceptionInListenerCallbackIsHandled() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);
        when(mockThreadFactory.createThread(any(Runnable.class), anyString()))
                .thenReturn(testThread1)
                .thenReturn(testThread2);

        doThrow(new RuntimeException("Listener error"))
                .when(mockListener).onConnectionClosed(connectionId);

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act & Assert - exception in listener should be caught and logged, not propagated
        assertDoesNotThrow(() -> handler.run());
        verify(mockListener).onConnectionClosed(connectionId);
    }

    @Test
    void testSocketsClosedEvenWhenCloseThrowsException() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);
        when(mockThreadFactory.createThread(any(Runnable.class), anyString()))
                .thenReturn(testThread1)
                .thenReturn(testThread2);

        clientSocket.setCloseException(new IOException("Close failed"));

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act & Assert - exception during close should be swallowed
        assertDoesNotThrow(() -> handler.run());
        assertTrue(clientSocket.isClosed());
        assertTrue(targetSocket.isClosed());
    }

    @Test
    void testBothSocketsClosedInFinally() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);
        when(mockThreadFactory.createThread(any(Runnable.class), anyString()))
                .thenReturn(testThread1)
                .thenReturn(testThread2);

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act
        handler.run();

        // Assert - both sockets closed
        assertTrue(clientSocket.isClosed());
        assertTrue(targetSocket.isClosed());
    }

    @Test
    void testTcpNoDelaySetOnTargetSocket() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);
        when(mockThreadFactory.createThread(any(Runnable.class), anyString()))
                .thenReturn(testThread1)
                .thenReturn(testThread2);

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act
        handler.run();

        // Assert
        assertTrue(targetSocket.getTcpNoDelay());
    }

    @Test
    void testTwoThreadsCreatedForBidirectionalForwarding() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);

        AtomicReference<String> thread1Name = new AtomicReference<>();
        AtomicReference<String> thread2Name = new AtomicReference<>();

        when(mockThreadFactory.createThread(any(Runnable.class), anyString()))
                .thenAnswer(invocation -> {
                    thread1Name.set(invocation.getArgument(1));
                    return testThread1;
                })
                .thenAnswer(invocation -> {
                    thread2Name.set(invocation.getArgument(1));
                    return testThread2;
                });

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act
        handler.run();

        // Assert - two threads created with different names
        verify(mockThreadFactory, times(2)).createThread(any(Runnable.class), anyString());
        assertNotNull(thread1Name.get());
        assertNotNull(thread2Name.get());
        assertTrue(thread1Name.get().contains("C2T") || thread1Name.get().contains("T2C"));
        assertTrue(thread2Name.get().contains("C2T") || thread2Name.get().contains("T2C"));
        assertNotEquals(thread1Name.get(), thread2Name.get());
    }

    @Test
    void testThreadsStartedBeforeJoin() throws Exception {
        // Arrange
        when(mockSocketFactory.createSocket(targetHost, targetPort)).thenReturn(targetSocket);
        when(mockThreadFactory.createThread(any(Runnable.class), anyString()))
                .thenReturn(testThread1)
                .thenReturn(testThread2);

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act
        handler.run();

        // Assert - both threads started and joined
        assertTrue(testThread1.wasStarted());
        assertTrue(testThread2.wasStarted());
        assertTrue(testThread1.wasJoined());
        assertTrue(testThread2.wasJoined());
    }

    @Test
    void testTargetSocketNotClosedIfNullDueToConnectionFailure() throws Exception {
        // Arrange - target socket creation fails
        when(mockSocketFactory.createSocket(targetHost, targetPort))
                .thenThrow(new IOException("Connection refused"));

        ProxyConnectionHandler handler = new ProxyConnectionHandler(
                clientSocket, targetHost, targetPort, mockListener,
                connectionId, mockSocketFactory, mockThreadFactory
        );

        // Act
        assertDoesNotThrow(() -> handler.run());

        // Assert - client socket closed, no NPE when trying to close null target socket
        assertTrue(clientSocket.isClosed());
        verify(mockListener).onConnectionClosed(connectionId);
    }
}
