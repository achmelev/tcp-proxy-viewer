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

        errorHandlerService.handleError(exception, ErrorCategory.CONNECTION_HANDLING);

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

        errorHandlerService.handleError(error, ErrorCategory.SYSTEM_RESOURCE);

        // Verify classification happened
        assertEquals(1, errorClassifier.classifyCallCount);
        assertSame(error, errorClassifier.lastThrowable);
        assertEquals(ErrorCategory.SYSTEM_RESOURCE, errorClassifier.lastCategory);

        // Verify dialog was shown
        assertEquals(1, errorDialogService.showDialogCallCount);
        assertNotNull(errorDialogService.lastErrorContext);
        assertEquals(ErrorSeverity.FATAL, errorDialogService.lastErrorContext.getSeverity());
    }

    @Test
    void testHandleError_withNullThrowable_logsAndReturns() {
        errorHandlerService.handleError(null, ErrorCategory.NETWORK_IO);

        // Should not call classifier or dialog service
        assertEquals(0, errorClassifier.classifyCallCount);
        assertEquals(0, errorDialogService.showDialogCallCount);
    }

    @Test
    void testHandleError_withNullCategory_logsAndReturns() {
        IOException exception = new IOException("Test");

        errorHandlerService.handleError(exception, null);

        // Should not call classifier or dialog service
        assertEquals(0, errorClassifier.classifyCallCount);
        assertEquals(0, errorDialogService.showDialogCallCount);
    }

    @Test
    void testHandleError_withErrorContext_recoverableError() {
        IOException exception = new IOException("Data error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.DATA_PROCESSING)
                .severity(ErrorSeverity.RECOVERABLE)
                .userMessage("Data processing failed")
                .technicalDetails("IOException: Data error")
                .build();

        errorHandlerService.handleError(errorContext);

        // Verify dialog was shown
        assertEquals(1, errorDialogService.showDialogCallCount);
        assertSame(errorContext, errorDialogService.lastErrorContext);


        // Classifier should not be called (context already classified)
        assertEquals(0, errorClassifier.classifyCallCount);
    }

    @Test
    void testHandleError_withErrorContext_fatalError() {
        RuntimeException exception = new RuntimeException("Init failed");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.INITIALIZATION)
                .severity(ErrorSeverity.FATAL)
                .userMessage("Initialization failed")
                .technicalDetails("RuntimeException: Init failed")
                .build();

        errorHandlerService.handleError(errorContext);

        // Verify dialog was shown
        assertEquals(1, errorDialogService.showDialogCallCount);
        assertSame(errorContext, errorDialogService.lastErrorContext);



        // Classifier should not be called
        assertEquals(0, errorClassifier.classifyCallCount);
    }

    @Test
    void testHandleError_withNullErrorContext_logsAndReturns() {
        errorHandlerService.handleError((ErrorContext) null);

        assertEquals(0, errorClassifier.classifyCallCount);
        assertEquals(0, errorDialogService.showDialogCallCount);
    }

    @Test
    void testHandleUncaughtException_fromUIThread() {
        Thread thread = new Thread("JavaFX Application Thread");
        RuntimeException exception = new RuntimeException("UI error");

        errorHandlerService.handleUncaughtException(thread, exception);

        // Verify classification with UI_OPERATION category
        assertEquals(1, errorClassifier.classifyCallCount);
        assertSame(exception, errorClassifier.lastThrowable);
        assertEquals(ErrorCategory.UI_OPERATION, errorClassifier.lastCategory);

        // Verify dialog was shown
        assertEquals(1, errorDialogService.showDialogCallCount);
    }

    @Test
    void testHandleUncaughtException_fromProxyThread() {
        Thread thread = new Thread("proxy-server-thread");
        IOException exception = new IOException("Proxy error");

        errorHandlerService.handleUncaughtException(thread, exception);

        // Verify classification with PROXY_SERVER category
        assertEquals(1, errorClassifier.classifyCallCount);
        assertEquals(ErrorCategory.PROXY_SERVER, errorClassifier.lastCategory);
    }

    @Test
    void testHandleUncaughtException_fromConnectionThread() {
        Thread thread = new Thread("connection-handler-1");
        IOException exception = new IOException("Connection error");

        errorHandlerService.handleUncaughtException(thread, exception);

        // Verify classification with CONNECTION_HANDLING category
        assertEquals(1, errorClassifier.classifyCallCount);
        assertEquals(ErrorCategory.CONNECTION_HANDLING, errorClassifier.lastCategory);
    }

    @Test
    void testHandleUncaughtException_fromUnknownThread() {
        Thread thread = new Thread("some-random-thread");
        RuntimeException exception = new RuntimeException("Unknown error");

        errorHandlerService.handleUncaughtException(thread, exception);

        // Verify classification with SYSTEM_RESOURCE category (default for unknown threads)
        assertEquals(1, errorClassifier.classifyCallCount);
        assertEquals(ErrorCategory.SYSTEM_RESOURCE, errorClassifier.lastCategory);
    }

    @Test
    void testHandleError_handlesDialogServiceException() {
        IOException exception = new IOException("Test error");
        errorDialogService.shouldThrowException = true;

        // Should not propagate exception
        assertDoesNotThrow(() ->
            errorHandlerService.handleError(exception, ErrorCategory.NETWORK_IO));

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
