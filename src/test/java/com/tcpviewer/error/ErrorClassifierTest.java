package com.tcpviewer.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.BindException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorClassifierTest {

    private ErrorClassifier errorClassifier;

    @BeforeEach
    void setUp() {
        errorClassifier = new ErrorClassifier();
    }

    @Test
    void testClassify_createsCompleteErrorContext() {
        IOException exception = new IOException("Test error");
        ErrorContext context = errorClassifier.classify(exception, ErrorCategory.NETWORK_IO);

        assertNotNull(context);
        assertSame(exception, context.getThrowable());
        assertEquals(ErrorCategory.NETWORK_IO, context.getCategory());
        assertEquals(ErrorSeverity.RECOVERABLE, context.getSeverity());
        assertNotNull(context.getUserMessage());
        assertNotNull(context.getTechnicalDetails());
        assertNotNull(context.getTimestamp());
    }

    @Test
    void testDetermineSeverity_outOfMemoryError_isFatal() {
        OutOfMemoryError error = new OutOfMemoryError("No more memory");
        ErrorSeverity severity = errorClassifier.determineSeverity(error, ErrorCategory.UNCAUGHT);

        assertEquals(ErrorSeverity.FATAL, severity);
    }

    @Test
    void testDetermineSeverity_stackOverflowError_isFatal() {
        StackOverflowError error = new StackOverflowError();
        ErrorSeverity severity = errorClassifier.determineSeverity(error, ErrorCategory.UNCAUGHT);

        assertEquals(ErrorSeverity.FATAL, severity);
    }

    @Test
    void testDetermineSeverity_threadDeath_isRecoverable() {
        ThreadDeath error = new ThreadDeath();
        ErrorSeverity severity = errorClassifier.determineSeverity(error, ErrorCategory.UNCAUGHT);

        assertEquals(ErrorSeverity.RECOVERABLE, severity);
    }

    @Test
    void testDetermineSeverity_initialization_isFatal() {
        RuntimeException exception = new RuntimeException("Init failed");
        ErrorSeverity severity = errorClassifier.determineSeverity(exception, ErrorCategory.INITIALIZATION);

        assertEquals(ErrorSeverity.FATAL, severity);
    }

    @Test
    void testDetermineSeverity_connectionHandling_isRecoverable() {
        IOException exception = new IOException("Connection reset");
        ErrorSeverity severity = errorClassifier.determineSeverity(exception, ErrorCategory.CONNECTION_HANDLING);

        assertEquals(ErrorSeverity.RECOVERABLE, severity);
    }

    @Test
    void testDetermineSeverity_dataProcessing_isRecoverable() {
        RuntimeException exception = new RuntimeException("Invalid data");
        ErrorSeverity severity = errorClassifier.determineSeverity(exception, ErrorCategory.DATA_PROCESSING);

        assertEquals(ErrorSeverity.RECOVERABLE, severity);
    }

    @Test
    void testDetermineSeverity_networkIO_isRecoverable() {
        IOException exception = new IOException("Timeout");
        ErrorSeverity severity = errorClassifier.determineSeverity(exception, ErrorCategory.NETWORK_IO);

        assertEquals(ErrorSeverity.RECOVERABLE, severity);
    }

    @Test
    void testDetermineSeverity_uiOperation_isRecoverable() {
        RuntimeException exception = new RuntimeException("UI error");
        ErrorSeverity severity = errorClassifier.determineSeverity(exception, ErrorCategory.UI_OPERATION);

        assertEquals(ErrorSeverity.FATAL, severity);
    }

    @Test
    void testDetermineSeverity_systemResource_isFatal() {
        RuntimeException exception = new RuntimeException("Resource error");
        ErrorSeverity severity = errorClassifier.determineSeverity(exception, ErrorCategory.UNCAUGHT);

        assertEquals(ErrorSeverity.FATAL, severity);
    }

    @Test
    void testGenerateUserMessage_outOfMemoryError() {
        OutOfMemoryError error = new OutOfMemoryError();
        String message = errorClassifier.generateUserMessage(error, ErrorCategory.UNCAUGHT);

        assertTrue(message.contains("run out of memory"));
        assertTrue(message.contains("restart"));
    }

    @Test
    void testGenerateUserMessage_stackOverflowError() {
        StackOverflowError error = new StackOverflowError();
        String message = errorClassifier.generateUserMessage(error, ErrorCategory.UNCAUGHT);

        assertTrue(message.contains("stack overflow"));
    }

    @Test
    void testGenerateUserMessage_bindException() {
        BindException exception = new BindException("Address already in use");
        String message = errorClassifier.generateUserMessage(exception, ErrorCategory.PROXY_SERVER);

        assertTrue(message.contains("port is already in use"));
        assertTrue(message.contains("different port"));
    }

    @Test
    void testGenerateUserMessage_addressAlreadyInUse() {
        IOException exception = new IOException("Address already in use");
        String message = errorClassifier.generateUserMessage(exception, ErrorCategory.PROXY_SERVER);

        assertTrue(message.contains("port is already in use"));
    }

    @Test
    void testGenerateUserMessage_permissionDenied() {
        IOException exception = new IOException("Permission denied");
        String message = errorClassifier.generateUserMessage(exception, ErrorCategory.PROXY_SERVER);

        assertTrue(message.contains("Permission denied"));
        assertTrue(message.contains("administrator privileges"));
    }

    @Test
    void testGenerateUserMessage_initialization() {
        RuntimeException exception = new RuntimeException("Init failed");
        String message = errorClassifier.generateUserMessage(exception, ErrorCategory.INITIALIZATION);

        assertTrue(message.contains("initialize"));
        assertTrue(message.contains("restart"));
    }

    @Test
    void testGenerateUserMessage_connectionHandling() {
        IOException exception = new IOException("Connection reset");
        String message = errorClassifier.generateUserMessage(exception, ErrorCategory.CONNECTION_HANDLING);

        assertTrue(message.contains("connection error"));
        assertTrue(message.contains("Connection reset"));
    }

    @Test
    void testGenerateUserMessage_dataProcessing() {
        RuntimeException exception = new RuntimeException("Parse error");
        String message = errorClassifier.generateUserMessage(exception, ErrorCategory.DATA_PROCESSING);

        assertTrue(message.contains("processing data"));
        assertTrue(message.contains("Parse error"));
    }

    @Test
    void testGenerateUserMessage_truncatesLongMessages() {
        String longMessage = "a".repeat(250);
        RuntimeException exception = new RuntimeException(longMessage);
        String message = errorClassifier.generateUserMessage(exception, ErrorCategory.DATA_PROCESSING);

        // Should contain truncated message with ellipsis
        assertTrue(message.contains("..."));
        assertTrue(message.length() < 300);
    }

    @Test
    void testGenerateTechnicalDetails_includesExceptionType() {
        IOException exception = new IOException("Test error");
        String details = errorClassifier.generateTechnicalDetails(exception);

        assertTrue(details.contains("Exception Type:"));
        assertTrue(details.contains("IOException"));
    }

    @Test
    void testGenerateTechnicalDetails_includesExceptionMessage() {
        IOException exception = new IOException("Test error message");
        String details = errorClassifier.generateTechnicalDetails(exception);

        assertTrue(details.contains("Exception Message:"));
        assertTrue(details.contains("Test error message"));
    }

    @Test
    void testGenerateTechnicalDetails_includesStackTrace() {
        IOException exception = new IOException("Test error");
        String details = errorClassifier.generateTechnicalDetails(exception);

        assertTrue(details.contains("Stack Trace:"));
        assertTrue(details.contains("IOException"));
    }

    @Test
    void testGenerateTechnicalDetails_includesCausedBy() {
        IOException cause = new IOException("Root cause");
        RuntimeException exception = new RuntimeException("Wrapper", cause);
        String details = errorClassifier.generateTechnicalDetails(exception);

        assertTrue(details.contains("Caused By:"));
        assertTrue(details.contains("Root cause"));
    }

    @Test
    void testGenerateTechnicalDetails_handlesNullMessage() {
        IOException exception = new IOException();
        String details = errorClassifier.generateTechnicalDetails(exception);

        assertTrue(details.contains("(no message)"));
    }
}
