package com.tcpviewer.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorHandlerService.
 * Note: Using test stubs instead of Mockito mocks due to JDK 25 compatibility issues.
 */
class ErrorHandlerServiceTest {

    /**
     * Test stub for ErrorClassifier.
     */
    private static class TestErrorClassifier extends ErrorClassifier {
        public int classifyCallCount = 0;
        public Throwable lastThrowable = null;
        public ErrorCategory lastCategory = null;
        public ErrorContext returnContext = null;

        @Override
        public ErrorContext classify(Throwable throwable, ErrorCategory category) {
            classifyCallCount++;
            lastThrowable = throwable;
            lastCategory = category;
            return returnContext != null ? returnContext : super.classify(throwable, category);
        }
    }

    /**
     * Test stub for ErrorDialogService.
     */
    private static class TestErrorDialogService extends ErrorDialogService {
        public int showDialogCallCount = 0;
        public ErrorContext lastErrorContext = null;
        public boolean shouldThrowException = false;

        public TestErrorDialogService() {
            super(runnable -> runnable.run());
        }

        @Override
        public void showErrorDialog(ErrorContext errorContext) {
            showDialogCallCount++;
            lastErrorContext = errorContext;
            if (shouldThrowException) {
                throw new RuntimeException("Dialog failed");
            }
            // Don't actually show a dialog in tests
        }
    }



    private TestErrorClassifier errorClassifier;
    private TestErrorDialogService errorDialogService;
    private ErrorHandlerService errorHandlerService;

    @BeforeEach
    void setUp() {
        errorClassifier = new TestErrorClassifier();
        errorDialogService = new TestErrorDialogService();
        errorHandlerService = new ErrorHandlerService(errorClassifier, errorDialogService);
    }

    @Test
    void testHandleError_recoverableError_doesNotShutdown() {
        IOException exception = new IOException("Connection failed");

        errorHandlerService.handleExpectedException(exception, ErrorCategory.CONNECTION_HANDLING);

        // Verify classification happened
        assertEquals(1, errorClassifier.classifyCallCount);
        assertSame(exception, errorClassifier.lastThrowable);
        assertEquals(ErrorCategory.CONNECTION_HANDLING, errorClassifier.lastCategory);

        // Verify dialog was shown
        assertEquals(1, errorDialogService.showDialogCallCount);
        assertNotNull(errorDialogService.lastErrorContext);
        assertEquals(ErrorSeverity.RECOVERABLE, errorDialogService.lastErrorContext.getSeverity());
    }

    @Test
    void testHandleError_fatalError_triggersShutdown() {
        OutOfMemoryError error = new OutOfMemoryError("Out of memory");

        errorHandlerService.handleUncaughtException(Thread.currentThread(), error);

        // Verify classification happened
        assertEquals(1, errorClassifier.classifyCallCount);
        assertSame(error, errorClassifier.lastThrowable);
        assertEquals(ErrorCategory.UNCAUGHT, errorClassifier.lastCategory);

        // Verify dialog was shown
        assertEquals(1, errorDialogService.showDialogCallCount);
        assertNotNull(errorDialogService.lastErrorContext);
        assertEquals(ErrorSeverity.FATAL, errorDialogService.lastErrorContext.getSeverity());
    }

    @Test
    void testHandleError_withNullThrowable_logsAndReturns() {
        errorHandlerService.handleExpectedException(null, ErrorCategory.NETWORK_IO);

        // Should not call classifier or dialog service
        assertEquals(0, errorClassifier.classifyCallCount);
        assertEquals(0, errorDialogService.showDialogCallCount);
    }

    @Test
    void testHandleError_withNullCategory_logsAndReturns() {
        IOException exception = new IOException("Test");

        errorHandlerService.handleExpectedException(exception, null);

        // Should not call classifier or dialog service
        assertEquals(0, errorClassifier.classifyCallCount);
        assertEquals(0, errorDialogService.showDialogCallCount);
    }


    @Test
    void testHandleUncaughtException() {
        Thread thread = new Thread("Some Thread");
        RuntimeException exception = new RuntimeException("Some uncaught error");

        errorHandlerService.handleUncaughtException(thread, exception);

        // Verify classification with UI_OPERATION category
        assertEquals(1, errorClassifier.classifyCallCount);
        assertSame(exception, errorClassifier.lastThrowable);
        assertEquals(ErrorCategory.UNCAUGHT, errorClassifier.lastCategory);

        // Verify dialog was shown
        assertEquals(1, errorDialogService.showDialogCallCount);
    }

    @Test
    void testHandleError_handlesDialogServiceException() {
        IOException exception = new IOException("Test error");
        errorDialogService.shouldThrowException = true;

        // Should not propagate exception
        assertDoesNotThrow(() ->
            errorHandlerService.handleExpectedException(exception, ErrorCategory.NETWORK_IO));

        // Dialog service should have been called
        assertEquals(1, errorDialogService.showDialogCallCount);
    }

    @Test
    void testConstructor_requiresErrorClassifier() {
        assertThrows(NullPointerException.class, () ->
            new ErrorHandlerService(null, errorDialogService));
    }

    @Test
    void testConstructor_requiresErrorDialogService() {
        assertThrows(NullPointerException.class, () ->
            new ErrorHandlerService(errorClassifier, null));
    }

}
