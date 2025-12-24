package com.tcpviewer.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Central service for handling all application errors.
 * Orchestrates error classification, logging, user notification, and shutdown.
 * This is the main entry point for all error handling in the application.
 */
@Service
public class ErrorHandlerService {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerService.class);

    private final ErrorClassifier errorClassifier;
    private final ErrorDialogService errorDialogService;

    public ErrorHandlerService(ErrorClassifier errorClassifier,
                              ErrorDialogService errorDialogService) {
        if (errorClassifier == null) {
            throw new NullPointerException("errorClassifier cannot be null");
        }
        if (errorDialogService == null) {
            throw new NullPointerException("errorDialogService cannot be null");
        }
        this.errorClassifier = errorClassifier;
        this.errorDialogService = errorDialogService;
    }

    /**
     * Handles an exoected exception
     *
     * @param throwable the exception that occurred
     * @param category the category of the error
     */
    public void handleExpectedException(Throwable throwable, ErrorCategory category) {
        handleError(throwable, category);
    }

    /**
     * Handles an error by classifying it, logging it, showing a dialog, and
     * potentially shutting down the application.
     *
     * @param throwable the exception that occurred
     * @param category the category of the error
     */
    private void handleError(Throwable throwable, ErrorCategory category) {
        if (throwable == null || category == null) {
            logger.error("handleError called with null parameter: throwable={}, category={}",
                        throwable, category);
            return;
        }

        try {
            // Classify the error
            ErrorContext errorContext = errorClassifier.classify(throwable, category);

            // Log the error with full context
            logError(errorContext);

            // Show error dialog to user
            errorDialogService.showErrorDialog(errorContext);



        } catch (Throwable e) {
            // Failsafe: If error handling itself fails, log to console
            logger.error("CRITICAL: Error handler failed while processing error", e);
            logger.error("Original error was: {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * Handles uncaught exceptions from threads.
     * This method is called by the global uncaught exception handler.
     *
     * @param thread the thread where the exception occurred
     * @param throwable the uncaught exception
     */
    public void handleUncaughtException(Thread thread, Throwable throwable) {
        logger.error("Uncaught exception in thread '{}': {}",
                    thread.getName(), throwable.getMessage(), throwable);

        // Handle the error
        handleError(throwable, ErrorCategory.UNCAUGHT);
    }

    /**
     * Logs an error with full context information.
     */
    private void logError(ErrorContext errorContext) {
        String severityPrefix = errorContext.getSeverity() == ErrorSeverity.FATAL ? "FATAL" : "ERROR";

        logger.error("[{}] {} Error: {}",
                    severityPrefix,
                    errorContext.getCategory().getDisplayName(),
                    errorContext.getUserMessage());

        logger.error("Exception: {}",
                    errorContext.getThrowable().getMessage(),
                    errorContext.getThrowable());

        // Log additional context if present
        if (!errorContext.getAdditionalContext().isEmpty()) {
            logger.error("Additional context: {}", errorContext.getAdditionalContext());
        }
    }


}
