package com.tcpviewer.error;

/**
 * Severity level of an error, determining whether the application should continue or shutdown.
 */
public enum ErrorSeverity {
    /**
     * Fatal error - application must shutdown immediately.
     * Examples: OutOfMemoryError, initialization failures, critical resource unavailability.
     */
    FATAL,

    /**
     * Recoverable error - application can continue operating.
     * Examples: connection failures, data processing errors, network timeouts.
     */
    RECOVERABLE
}
