package com.tcpviewer.proxy;

import com.tcpviewer.error.ErrorHandlerService;
import com.tcpviewer.io.wrapper.InputStreamWrapper;
import com.tcpviewer.io.wrapper.OutputStreamWrapper;
import com.tcpviewer.io.wrapper.ServerSocketWrapper;
import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.io.wrapper.factory.ServerSocketFactory;
import com.tcpviewer.io.wrapper.factory.SocketFactory;
import com.tcpviewer.lang.wrapper.ExecutorServiceWrapper;
import com.tcpviewer.lang.wrapper.ThreadWrapper;
import com.tcpviewer.lang.wrapper.factory.ThreadFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProxyServer.
 * Tests server socket acceptance loop, connection handling, and lifecycle.
 */
@ExtendWith(MockitoExtension.class)
class ProxyServerTest {

    /**
     * Test stub for ServerSocketWrapper to control accept() loop.
     */
    private static class TestServerSocketWrapper implements ServerSocketWrapper {
        private final List<SocketWrapper> socketsToAccept = new ArrayList<>();
        private int acceptIndex = 0;
        private boolean closed = false;
        private boolean reuseAddress = false;
        private IOException bindException;
        private SocketAddress boundAddress;

        public void addSocketToAccept(SocketWrapper socket) {
            socketsToAccept.add(socket);
        }

        public void setBindException(IOException ex) {
            this.bindException = ex;
        }

        @Override
        public void bind(SocketAddress endpoint) throws IOException {
            if (bindException != null) {
                throw bindException;
            }
            this.boundAddress = endpoint;
        }

        @Override
        public SocketWrapper accept() throws IOException {
            if (closed) {
                throw new SocketException("Socket closed");
            }
            if (acceptIndex >= socketsToAccept.size()) {
                // End of configured sockets - throw SocketException to break loop
                throw new SocketException("No more sockets");
            }
            return socketsToAccept.get(acceptIndex++);
        }

        @Override
        public void setReuseAddress(boolean on) throws IOException {
            this.reuseAddress = on;
        }

        public boolean getReuseAddress() {
            return reuseAddress;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    /**
     * Test stub for SocketWrapper.
     */
    private static class TestSocketWrapper implements SocketWrapper {
        private final String address;
        private final int port;
        private boolean closed = false;

        public TestSocketWrapper(String address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public InputStreamWrapper getInputStream() throws IOException {
            return null;
        }

        @Override
        public OutputStreamWrapper getOutputStream() throws IOException {
            return null;
        }

        @Override
        public void setTcpNoDelay(boolean on) throws IOException {
        }

        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName(address);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    /**
     * Test stub for ExecutorServiceWrapper.
     */
    private static class TestExecutorServiceWrapper implements ExecutorServiceWrapper {
        public final List<Runnable> submittedTasks = new ArrayList<>();
        private boolean shutdown = false;
        private boolean terminated = false;

        @Override
        public void submit(Runnable task) {
            submittedTasks.add(task);
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            terminated = true;
            return new ArrayList<>(submittedTasks);
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            terminated = true;
            return true;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return terminated;
        }
    }

    /**
     * Test stub for ThreadWrapper.
     */
    private static class TestThreadWrapper implements ThreadWrapper {
        private boolean interrupted = false;

        @Override
        public void run() {
        }

        @Override
        public void start() {
        }

        @Override
        public void join() throws InterruptedException {
        }

        @Override
        public void join(long millis) throws InterruptedException {
        }

        @Override
        public void interrupt() {
            interrupted = true;
        }

        @Override
        public boolean isAlive() {
            return false;
        }

        @Override
        public String getName() {
            return "test-thread";
        }

        @Override
        public void setDaemon(boolean on) {
        }

        @Override
        public void setName(String name) {
        }

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }
    }

    @Mock
    private DataCaptureListener mockDataListener;

    @Mock
    private ConnectionAcceptedCallback mockConnectionCallback;

    @Mock
    private SocketFactory mockSocketFactory;

    @Mock
    private ServerSocketFactory mockServerSocketFactory;

    @Mock
    private ThreadFactory mockThreadFactory;

    /**
     * Test stub for ErrorHandlerService.
     */
    private static class TestErrorHandlerService extends ErrorHandlerService {
        public int handleErrorCallCount = 0;

        public TestErrorHandlerService() {
            super(new com.tcpviewer.error.ErrorClassifier(),
                  new com.tcpviewer.error.ErrorDialogService(runnable -> runnable.run()));
        }

        @Override
        public void handleError(Throwable throwable, com.tcpviewer.error.ErrorCategory category) {
            handleErrorCallCount++;
            // Don't call super to avoid triggering actual error handling in tests
        }
    }

    private TestErrorHandlerService testErrorHandlerService;
    private TestServerSocketWrapper testServerSocket;
    private TestExecutorServiceWrapper testExecutor;
    private TestThreadWrapper testCurrentThread;
    private String localIp;
    private int localPort;
    private String targetHost;
    private int targetPort;

    @BeforeEach
    void setUp() {
        localIp = "127.0.0.1";
        localPort = 8080;
        targetHost = "example.com";
        targetPort = 80;

        testServerSocket = new TestServerSocketWrapper();
        testExecutor = new TestExecutorServiceWrapper();
        testCurrentThread = new TestThreadWrapper();
        testErrorHandlerService = new TestErrorHandlerService();
    }

    @Test
    void testServerStartsAndBinds() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act
        server.run();

        // Assert
        verify(mockServerSocketFactory).createServerSocket();
        assertTrue(testServerSocket.getReuseAddress());
        assertNotNull(testServerSocket.boundAddress);
        assertTrue(testServerSocket.isClosed()); // Closed in finally
    }

    @Test
    void testAcceptsMultipleConnections() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        TestSocketWrapper client1 = new TestSocketWrapper("192.168.1.100", 50001);
        TestSocketWrapper client2 = new TestSocketWrapper("192.168.1.101", 50002);

        testServerSocket.addSocketToAccept(client1);
        testServerSocket.addSocketToAccept(client2);

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act
        server.run();

        // Assert - two connections accepted
        assertEquals(2, testExecutor.submittedTasks.size());
    }

    @Test
    void testConnectionCallbackInvoked() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        TestSocketWrapper client = new TestSocketWrapper("192.168.1.100", 50001);
        testServerSocket.addSocketToAccept(client);

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act
        server.run();

        // Assert
        verify(mockConnectionCallback).onConnectionAccepted(any(), eq(client));
    }

    @Test
    void testHandlerSubmittedToExecutor() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        TestSocketWrapper client = new TestSocketWrapper("192.168.1.100", 50001);
        testServerSocket.addSocketToAccept(client);

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act
        server.run();

        // Assert - one handler submitted
        assertEquals(1, testExecutor.submittedTasks.size());
        assertTrue(testExecutor.submittedTasks.get(0) instanceof ProxyConnectionHandler);
    }

    @Test
    void testNullCallbackDoesNotCauseNPE() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        TestSocketWrapper client = new TestSocketWrapper("192.168.1.100", 50001);
        testServerSocket.addSocketToAccept(client);

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, null, // null callback
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act & Assert - should not throw NPE
        assertDoesNotThrow(() -> server.run());
        assertEquals(1, testExecutor.submittedTasks.size());
    }

    @Test
    void testStopMethodSetsRunningToFalse() throws Exception {
        // Arrange
        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act - call stop before run (sets running to false)
        server.stop();

        // Assert
        assertFalse(server.isRunning());
    }

    @Test
    void testSocketExceptionDuringAcceptBreaksLoop() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        // No sockets added, so accept() will throw SocketException immediately

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act & Assert - should not throw, should exit gracefully
        assertDoesNotThrow(() -> server.run());
        assertFalse(server.isRunning());
    }

    @Test
    void testIOExceptionDuringBindLogsAndReturns() throws Exception {
        // Arrange
        testServerSocket.setBindException(new IOException("Address already in use"));
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act - Should not throw exception, just log and return
        assertDoesNotThrow(() -> server.run());

        // Assert - Server should not be running after bind failure
        assertFalse(server.isRunning());
    }

    @Test
    void testRunningFlagSetCorrectly() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        TestSocketWrapper client = new TestSocketWrapper("192.168.1.100", 50001);
        testServerSocket.addSocketToAccept(client);

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act
        assertFalse(server.isRunning()); // Before run
        server.run();
        assertFalse(server.isRunning()); // After run completes
    }

    @Test
    void testServerSocketClosedInFinally() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act
        server.run();

        // Assert - server socket closed even after normal completion
        assertTrue(testServerSocket.isClosed());
    }

    @Test
    void testExceptionInCallbackDoesNotStopServer() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);
        when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        TestSocketWrapper client1 = new TestSocketWrapper("192.168.1.100", 50001);
        TestSocketWrapper client2 = new TestSocketWrapper("192.168.1.101", 50002);
        testServerSocket.addSocketToAccept(client1);
        testServerSocket.addSocketToAccept(client2);

        // First callback throws exception
        doThrow(new RuntimeException("Callback error"))
                .doNothing()
                .when(mockConnectionCallback).onConnectionAccepted(any(), any());

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act
        assertDoesNotThrow(() -> server.run());

        // Assert - second connection still handled despite first callback failing
        // Only one task submitted because exception in callback prevents handler creation
        assertEquals(1, testExecutor.submittedTasks.size());
    }

    @Test
    void testThreadInterruptionBreaksLoop() throws Exception {
        // Arrange
        when(mockServerSocketFactory.createServerSocket()).thenReturn(testServerSocket);

        // Simulate thread interruption
        TestThreadWrapper interruptedThread = new TestThreadWrapper();
        interruptedThread.interrupt();
        when(mockThreadFactory.currentThread()).thenReturn(interruptedThread);

        // Add sockets but loop should exit due to interruption
        testServerSocket.addSocketToAccept(new TestSocketWrapper("192.168.1.100", 50001));
        testServerSocket.addSocketToAccept(new TestSocketWrapper("192.168.1.101", 50002));

        ProxyServer server = new ProxyServer(
                localIp, localPort, targetHost, targetPort,
                mockDataListener, mockConnectionCallback,
                testExecutor, mockSocketFactory, mockServerSocketFactory, mockThreadFactory, testErrorHandlerService
        );

        // Act
        server.run();

        // Assert - loop exits immediately, no connections handled
        assertEquals(0, testExecutor.submittedTasks.size());
    }
}
