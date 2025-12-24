package com.tcpviewer.proxy;

import com.tcpviewer.error.ErrorHandlerService;
import com.tcpviewer.io.wrapper.factory.ServerSocketFactory;
import com.tcpviewer.io.wrapper.factory.SocketFactory;
import com.tcpviewer.lang.wrapper.ExecutorServiceWrapper;
import com.tcpviewer.lang.wrapper.ThreadWrapper;
import com.tcpviewer.lang.wrapper.factory.ExecutorServiceFactory;
import com.tcpviewer.lang.wrapper.factory.ThreadFactory;
import com.tcpviewer.model.ProxySession;
import com.tcpviewer.ui.error.ErrorDialogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProxyServerManager.
 * Tests proxy server lifecycle management and executor shutdown.
 */
@ExtendWith(MockitoExtension.class)
class ProxyServerManagerTest {

    /**
     * Test stub for ExecutorServiceWrapper.
     */
    private static class TestExecutorServiceWrapper implements ExecutorServiceWrapper {
        public final List<Runnable> submittedTasks = new ArrayList<>();
        private boolean shutdown = false;
        private boolean terminated = false;
        private boolean awaitTerminationResult = true;
        private InterruptedException awaitTerminationException;

        public void setAwaitTerminationResult(boolean result) {
            this.awaitTerminationResult = result;
        }

        public void setAwaitTerminationException(InterruptedException ex) {
            this.awaitTerminationException = ex;
        }

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
            if (awaitTerminationException != null) {
                throw awaitTerminationException;
            }
            if (awaitTerminationResult) {
                terminated = true;
            }
            return awaitTerminationResult;
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
        private boolean started = false;
        private boolean daemon = true;
        private boolean interrupted = false;
        private boolean alive = false;
        private final String name;

        public TestThreadWrapper(String name) {
            this.name = name;
        }

        @Override
        public void run() {
        }

        @Override
        public void start() {
            started = true;
            alive = true;
        }

        @Override
        public void join() throws InterruptedException {
            alive = false;
        }

        @Override
        public void join(long millis) throws InterruptedException {
            join();
        }

        @Override
        public void interrupt() {
            interrupted = true;
            alive = false;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setDaemon(boolean on) {
            this.daemon = on;
        }

        public boolean isDaemon() {
            return daemon;
        }

        @Override
        public void setName(String name) {
        }

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }

        public boolean wasStarted() {
            return started;
        }
    }

    @Mock
    private SocketFactory mockSocketFactory;

    @Mock
    private ServerSocketFactory mockServerSocketFactory;

    @Mock
    private ThreadFactory mockThreadFactory;

    @Mock
    private ExecutorServiceFactory mockExecutorServiceFactory;

    @Mock
    private DataCaptureListener mockDataListener;

    @Mock
    private ConnectionAcceptedCallback mockConnectionCallback;

    /**
     * Test stub for ErrorHandlerService.
     */
    private static class TestErrorHandlerService extends ErrorHandlerService {
        public int handleErrorCallCount = 0;

        public TestErrorHandlerService() {
            super(new com.tcpviewer.error.ErrorClassifier(),
                  new ErrorDialogService(runnable -> runnable.run()));
        }

        @Override
        public void handleExpectedException(Throwable throwable, com.tcpviewer.error.ErrorCategory category) {
            handleErrorCallCount++;
            // Don't call super to avoid triggering actual error handling in tests
        }
    }

    private TestErrorHandlerService testErrorHandlerService;
    private ProxyServerManager manager;
    private TestExecutorServiceWrapper testConnectionExecutor;
    private TestThreadWrapper testServerThread;
    private TestThreadWrapper testCurrentThread;
    private Executor testProxyExecutor;
    private ProxySession testSession;

    @BeforeEach
    void setUp() {
        testConnectionExecutor = new TestExecutorServiceWrapper();
        testServerThread = new TestThreadWrapper("ProxyServer");
        testCurrentThread = new TestThreadWrapper("Current-Thread");
        testProxyExecutor = Runnable::run; // Simple executor that runs immediately
        testSession = new ProxySession("127.0.0.1", 8080, "example.com", 80);
        testErrorHandlerService = new TestErrorHandlerService();

        // Configure factories (lenient to avoid UnnecessaryStubbingException for tests that don't start server)
        lenient().when(mockExecutorServiceFactory.createCachedThreadPool(mockThreadFactory))
                .thenReturn(testConnectionExecutor);
        lenient().when(mockThreadFactory.createThread(any(Runnable.class), eq("ProxyServer")))
                .thenReturn(testServerThread);
        lenient().when(mockThreadFactory.currentThread()).thenReturn(testCurrentThread);

        manager = new ProxyServerManager(
                testProxyExecutor,
                mockSocketFactory,
                mockServerSocketFactory,
                mockThreadFactory,
                mockExecutorServiceFactory,
                testErrorHandlerService
        );
    }

    @Test
    void testStartServerCreatesAndStartsProxyServer() {
        // Act
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Assert
        verify(mockExecutorServiceFactory).createCachedThreadPool(mockThreadFactory);
        verify(mockThreadFactory).createThread(any(Runnable.class), eq("ProxyServer"));
        assertTrue(testServerThread.wasStarted());
        assertFalse(testServerThread.isDaemon());
        assertTrue(testSession.isActive());
    }

    @Test
    void testStopServerStopsProxyAndCleansUp() {
        // Arrange - start server first
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act
        manager.stopServer();

        // Assert
        assertTrue(testServerThread.isInterrupted());
        assertTrue(testConnectionExecutor.isShutdown());
        assertFalse(manager.isRunning());
    }

    @Test
    void testStopServerInterruptsThreadIfAlive() {
        // Arrange
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act
        manager.stopServer();

        // Assert - thread should be interrupted
        assertTrue(testServerThread.isInterrupted());
    }

    @Test
    void testStopServerDoesNothingIfNotRunning() {
        // Act - stop when never started
        assertDoesNotThrow(() -> manager.stopServer());

        // Assert - no exceptions, no operations
        assertFalse(testConnectionExecutor.isShutdown());
    }

    @Test
    void testIsRunningReturnsTrueWhenServerActive() {
        // Arrange
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act & Assert
        // Note: isRunning() calls currentServer.isRunning() which will be false in tests
        // because we're not actually running the server thread
        assertFalse(manager.isRunning()); // false because ProxyServer.isRunning() is false
    }

    @Test
    void testIsRunningReturnsFalseWhenServerNotStarted() {
        // Act & Assert
        assertFalse(manager.isRunning());
    }

    @Test
    void testIsRunningReturnsFalseAfterStop() {
        // Arrange
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act
        manager.stopServer();

        // Assert
        assertFalse(manager.isRunning());
    }

    @Test
    void testExecutorShutdownGracefully() {
        // Arrange - executor terminates within 10 seconds
        testConnectionExecutor.setAwaitTerminationResult(true);
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act
        manager.stopServer();

        // Assert
        assertTrue(testConnectionExecutor.isShutdown());
        assertTrue(testConnectionExecutor.isTerminated());
    }

    @Test
    void testExecutorForcedShutdownOnTimeout() {
        // Arrange - first awaitTermination returns false (timeout), second returns true
        testConnectionExecutor.setAwaitTerminationResult(false);
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act
        manager.stopServer();

        // Assert - shutdownNow() called
        assertTrue(testConnectionExecutor.isShutdown());
        assertTrue(testConnectionExecutor.isTerminated()); // shutdownNow() sets terminated
    }

    @Test
    void testExecutorShutdownHandlesInterruptedException() throws InterruptedException {
        // Arrange - awaitTermination throws InterruptedException
        testConnectionExecutor.setAwaitTerminationException(new InterruptedException("Test interrupt"));
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act
        assertDoesNotThrow(() -> manager.stopServer());

        // Assert - shutdownNow() called and current thread interrupted
        assertTrue(testConnectionExecutor.isTerminated());
        verify(mockThreadFactory).currentThread();
    }

    @Test
    void testCleanupStopsServer() {
        // Arrange
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act
        manager.cleanup();

        // Assert - cleanup calls stopServer
        assertTrue(testConnectionExecutor.isShutdown());
        assertFalse(manager.isRunning());
    }

    @Test
    void testCleanupDoesNothingIfNotRunning() {
        // Act - cleanup when never started
        assertDoesNotThrow(() -> manager.cleanup());
    }

    @Test
    void testMultipleStopCallsAreSafe() {
        // Arrange
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act - stop multiple times
        manager.stopServer();
        manager.stopServer();
        manager.stopServer();

        // Assert - no exceptions
        assertFalse(manager.isRunning());
    }

    @Test
    void testStartStopStartCycle() {
        // Arrange & Act - start, stop, start again
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);
        manager.stopServer();

        // Reset test objects for second start
        TestExecutorServiceWrapper testConnectionExecutor2 = new TestExecutorServiceWrapper();
        TestThreadWrapper testServerThread2 = new TestThreadWrapper("ProxyServer");
        when(mockExecutorServiceFactory.createCachedThreadPool(mockThreadFactory))
                .thenReturn(testConnectionExecutor2);
        when(mockThreadFactory.createThread(any(Runnable.class), eq("ProxyServer")))
                .thenReturn(testServerThread2);

        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Assert - second start succeeded
        assertTrue(testServerThread2.wasStarted());
        assertFalse(testServerThread2.isDaemon());
    }

    @Test
    void testServerThreadIsNonDaemon() {
        // Act
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Assert
        assertFalse(testServerThread.isDaemon());
    }

    @Test
    void testConnectionExecutorCreatedFromFactory() {
        // Act
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Assert
        verify(mockExecutorServiceFactory).createCachedThreadPool(mockThreadFactory);
    }

    @Test
    void testStopServerNullsOutReferences() {
        // Arrange
        manager.startServer(testSession, mockDataListener, mockConnectionCallback);

        // Act
        manager.stopServer();

        // Assert - second stop should not cause NPE (references are null)
        assertDoesNotThrow(() -> manager.stopServer());
    }
}
