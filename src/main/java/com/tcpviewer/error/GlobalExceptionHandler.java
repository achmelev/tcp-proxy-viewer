package com.tcpviewer.error;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Global exception handler that sets up uncaught exception handlers for all threads.
 * This ensures that any unhandled exceptions in any thread are caught and processed
 * by the ErrorHandlerService.
 */
@Component
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorHandlerService errorHandlerService;

    public GlobalExceptionHandler(ErrorHandlerService errorHandlerService) {
        if (errorHandlerService == null) {
            throw new NullPointerException("errorHandlerService cannot be null");
        }
        this.errorHandlerService = errorHandlerService;
    }

    /**
     * Sets up global uncaught exception handlers.
     * This method is automatically called by Spring after the bean is constructed.
     */
    @PostConstruct
    public void setupExceptionHandlers() {
        logger.info("Setting up global exception handlers");

        // Set default uncaught exception handler for all threads
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("Uncaught exception in thread '{}': {}",
                        thread.getName(), throwable.getMessage());
            errorHandlerService.handleUncaughtException(thread, throwable);
        });

        // Set exception handler for the current thread (main/startup thread)
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("Uncaught exception in main thread: {}", throwable.getMessage());
            errorHandlerService.handleUncaughtException(thread, throwable);
        });

        logger.info("Global exception handlers configured successfully");
    }
}
