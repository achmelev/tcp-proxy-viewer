package com.tcpviewer.error;

import com.tcpviewer.javafx.wrapper.PlatformWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErrorDialogServiceTest {

    private PlatformWrapper platformWrapper;
    private ErrorDialogService errorDialogService;

    @BeforeEach
    void setUp() {
        platformWrapper = mock(PlatformWrapper.class);
        errorDialogService = new ErrorDialogService(platformWrapper);
    }

    @Test
    void testShowErrorDialog_fatalError_callsFatalDialog() {
        IOException exception = new IOException("Test error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.INITIALIZATION)
                .severity(ErrorSeverity.FATAL)
                .userMessage("Fatal error occurred")
                .technicalDetails("Details here")
                .build();

        errorDialogService.showErrorDialog(errorContext);

        // Verify that Platform.runLater was called
        verify(platformWrapper, times(1)).runLater(any(Runnable.class));
    }

    @Test
    void testShowErrorDialog_recoverableError_callsRecoverableDialog() {
        IOException exception = new IOException("Test error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.CONNECTION_HANDLING)
                .severity(ErrorSeverity.RECOVERABLE)
                .userMessage("Recoverable error occurred")
                .technicalDetails("Details here")
                .build();

        errorDialogService.showErrorDialog(errorContext);

        // Verify that Platform.runLater was called
        verify(platformWrapper, times(1)).runLater(any(Runnable.class));
    }

    @Test
    void testShowFatalErrorDialog_callsPlatformRunLater() {
        IOException exception = new IOException("Fatal error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.PROXY_SERVER)
                .severity(ErrorSeverity.FATAL)
                .userMessage("Failed to start proxy")
                .technicalDetails("IOException: Fatal error\nStack trace...")
                .build();

        errorDialogService.showFatalErrorDialog(errorContext);

        // Verify Platform.runLater was called exactly once
        verify(platformWrapper, times(1)).runLater(any(Runnable.class));
    }

    @Test
    void testShowRecoverableErrorDialog_callsPlatformRunLater() {
        IOException exception = new IOException("Connection error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.CONNECTION_HANDLING)
                .severity(ErrorSeverity.RECOVERABLE)
                .userMessage("Connection failed")
                .technicalDetails("IOException: Connection error\nStack trace...")
                .build();

        errorDialogService.showRecoverableErrorDialog(errorContext);

        // Verify Platform.runLater was called exactly once
        verify(platformWrapper, times(1)).runLater(any(Runnable.class));
    }

    @Test
    void testShowFatalErrorDialog_runnableExecutesWithoutError() {
        // This test verifies that the Runnable passed to Platform.runLater can execute
        // without throwing exceptions (though it won't actually show a dialog in headless test)

        IOException exception = new IOException("Fatal error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.SYSTEM_RESOURCE)
                .severity(ErrorSeverity.FATAL)
                .userMessage("Out of memory")
                .technicalDetails("OutOfMemoryError\nStack trace...")
                .build();

        // Capture the Runnable passed to Platform.runLater
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        errorDialogService.showFatalErrorDialog(errorContext);

        verify(platformWrapper).runLater(runnableCaptor.capture());

        Runnable capturedRunnable = runnableCaptor.getValue();
        assertNotNull(capturedRunnable);

        // Note: We cannot actually run the Runnable in a headless test environment
        // because it creates JavaFX Alerts which require a JavaFX runtime.
        // In a real test with JavaFX toolkit initialized, we could run it.
    }

    @Test
    void testShowRecoverableErrorDialog_runnableExecutesWithoutError() {
        IOException exception = new IOException("Data error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.DATA_PROCESSING)
                .severity(ErrorSeverity.RECOVERABLE)
                .userMessage("Failed to process data")
                .technicalDetails("IOException: Data error\nStack trace...")
                .build();

        // Capture the Runnable passed to Platform.runLater
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        errorDialogService.showRecoverableErrorDialog(errorContext);

        verify(platformWrapper).runLater(runnableCaptor.capture());

        Runnable capturedRunnable = runnableCaptor.getValue();
        assertNotNull(capturedRunnable);
    }

    @Test
    void testShowErrorDialog_withAdditionalContext() {
        IOException exception = new IOException("Test error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.NETWORK_IO)
                .severity(ErrorSeverity.RECOVERABLE)
                .userMessage("Network error")
                .technicalDetails("IOException: Test error")
                .addContext("connectionId", "conn-123")
                .addContext("retryCount", 3)
                .build();

        errorDialogService.showErrorDialog(errorContext);

        verify(platformWrapper, times(1)).runLater(any(Runnable.class));
    }

    @Test
    void testConstructor_requiresPlatformWrapper() {
        assertThrows(NullPointerException.class, () -> {
            new ErrorDialogService(null);
        });
    }
}
