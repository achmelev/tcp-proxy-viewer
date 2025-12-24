package com.tcpviewer.error;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.BindException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for classifying errors and determining their severity.
 * Analyzes exception types, error categories, and context to determine whether
 * an error is fatal (requires shutdown) or recoverable (application can continue).
 */
@Service
public class ErrorClassifier {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Classifies an error and creates a complete ErrorContext.
     *
     * @param throwable the exception that occurred
     * @param category the category of the error
     * @return complete ErrorContext with severity, messages, and technical details
     */
    public ErrorContext classify(Throwable throwable, ErrorCategory category) {
        ErrorSeverity severity = determineSeverity(throwable, category);
        String userMessage = generateUserMessage(throwable, category);
        String technicalDetails = generateTechnicalDetails(throwable);

        return ErrorContext.builder()
                .throwable(throwable)
                .category(category)
                .severity(severity)
                .userMessage(userMessage)
                .technicalDetails(technicalDetails)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Determines the severity of an error based on exception type and category.
     *
     * @param throwable the exception
     * @param category the error category
     * @return FATAL if application should shutdown, RECOVERABLE otherwise
     */
    ErrorSeverity determineSeverity(Throwable throwable, ErrorCategory category) {
        // JVM Errors are always fatal
        if (throwable instanceof OutOfMemoryError ||
            throwable instanceof StackOverflowError ||
            throwable instanceof VirtualMachineError) {
            return ErrorSeverity.FATAL;
        }

        // ThreadDeath is special - usually means intentional thread termination
        if (throwable instanceof ThreadDeath) {
            return ErrorSeverity.RECOVERABLE;
        }

        // Category-based classification
        switch (category) {
            case INITIALIZATION:
                // Any initialization failure is fatal - app can't start properly
                return ErrorSeverity.FATAL;

            case PROXY_SERVER:
                // Server binding failures are fatal (port in use, permission denied)
                if (throwable instanceof BindException ||
                    (throwable instanceof IOException &&
                     throwable.getMessage() != null &&
                     (throwable.getMessage().contains("Address already in use") ||
                      throwable.getMessage().contains("Permission denied")))) {
                    return ErrorSeverity.FATAL;
                }
                // Other proxy server errors might be recoverable
                return ErrorSeverity.RECOVERABLE;

            case CONNECTION_HANDLING:
            case DATA_PROCESSING:
            case NETWORK_IO:
                // Individual connection/data errors are recoverable
                return ErrorSeverity.RECOVERABLE;

            case UI_OPERATION:
                // UI errors are generally recoverable unless during initialization
                if (isInitializationPhase(throwable)) {
                    return ErrorSeverity.FATAL;
                }
                return ErrorSeverity.RECOVERABLE;

            case SYSTEM_RESOURCE:
                // System resource errors are typically fatal
                return ErrorSeverity.FATAL;

            default:
                // Default to recoverable to be safe
                return ErrorSeverity.RECOVERABLE;
        }
    }

    /**
     * Generates a user-friendly error message based on the exception and category.
     *
     * @param throwable the exception
     * @param category the error category
     * @return user-friendly error message
     */
    String generateUserMessage(Throwable throwable, ErrorCategory category) {
        // Check for specific exception types first
        if (throwable instanceof OutOfMemoryError) {
            return "The application has run out of memory and cannot continue. " +
                   "Please restart the application and consider increasing available memory.";
        }

        if (throwable instanceof StackOverflowError) {
            return "A stack overflow error occurred. The application cannot continue.";
        }

        // Check for BindException or port already in use
        if (throwable instanceof BindException ||
            (throwable.getMessage() != null &&
             throwable.getMessage().contains("Address already in use"))) {
            return "Failed to start proxy server. The port is already in use by another application. " +
                   "Please choose a different port or stop the other application.";
        }

        // Check for permission denied
        if (throwable.getMessage() != null &&
            throwable.getMessage().contains("Permission denied")) {
            return "Failed to start proxy server. Permission denied. " +
                   "You may need administrator privileges to bind to this port.";
        }

        // Category-specific messages
        switch (category) {
            case INITIALIZATION:
                return "Failed to initialize the application. " +
                       "Please check the log files for details and restart the application.";

            case PROXY_SERVER:
                return "An error occurred while starting the proxy server: " +
                       getSimpleMessage(throwable);

            case CONNECTION_HANDLING:
                return "A connection error occurred: " + getSimpleMessage(throwable) + ". " +
                       "The application will continue running.";

            case DATA_PROCESSING:
                return "An error occurred while processing data: " + getSimpleMessage(throwable) + ". " +
                       "Some data may not be displayed correctly.";

            case UI_OPERATION:
                return "A user interface error occurred: " + getSimpleMessage(throwable);

            case NETWORK_IO:
                return "A network error occurred: " + getSimpleMessage(throwable);

            case SYSTEM_RESOURCE:
                return "A system resource error occurred: " + getSimpleMessage(throwable) + ". " +
                       "The application may not function correctly.";

            default:
                return "An unexpected error occurred: " + getSimpleMessage(throwable);
        }
    }

    /**
     * Generates technical details including exception type, message, and stack trace.
     *
     * @param throwable the exception
     * @return formatted technical details
     */
    String generateTechnicalDetails(Throwable throwable) {
        StringBuilder details = new StringBuilder();

        // Exception type
        details.append("Exception Type: ").append(throwable.getClass().getName()).append("\n\n");

        // Exception message
        details.append("Exception Message:\n");
        details.append(throwable.getMessage() != null ? throwable.getMessage() : "(no message)");
        details.append("\n\n");

        // Stack trace
        details.append("Stack Trace:\n");
        details.append(getStackTrace(throwable));

        // Caused by (if present)
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            details.append("\n\nCaused By:\n");
            details.append("Type: ").append(cause.getClass().getName()).append("\n");
            details.append("Message: ").append(cause.getMessage() != null ? cause.getMessage() : "(no message)");
            details.append("\n");
            details.append(getStackTrace(cause));
        }

        return details.toString();
    }

    /**
     * Gets a simplified error message from the throwable.
     */
    private String getSimpleMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            // Truncate very long messages
            if (message.length() > 200) {
                return message.substring(0, 197) + "...";
            }
            return message;
        }
        return throwable.getClass().getSimpleName();
    }

    /**
     * Converts stack trace to string.
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Determines if we're in initialization phase by examining stack trace.
     */
    private boolean isInitializationPhase(Throwable throwable) {
        String stackTrace = getStackTrace(throwable);
        return stackTrace.contains("StageInitializer") ||
               stackTrace.contains("JavaFxApplication.init") ||
               stackTrace.contains("SpringApplication.run");
    }
}
