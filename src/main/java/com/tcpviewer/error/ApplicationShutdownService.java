package com.tcpviewer.error;

import com.tcpviewer.javafx.wrapper.PlatformWrapper;
import com.tcpviewer.lang.wrapper.SystemWrapper;
import com.tcpviewer.service.ProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service responsible for gracefully shutting down the application.
 * Ensures proper cleanup of resources before terminating the JVM.
 */
@Service
public class ApplicationShutdownService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownService.class);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final ProxyService proxyService;
    private final ConfigurableApplicationContext applicationContext;
    private final PlatformWrapper platformWrapper;
    private final SystemWrapper systemWrapper;

    public ApplicationShutdownService(@Lazy ProxyService proxyService,
                                     ConfigurableApplicationContext applicationContext,
                                     PlatformWrapper platformWrapper,
                                     SystemWrapper systemWrapper) {
        this.proxyService = proxyService;
        this.applicationContext = applicationContext;
        this.platformWrapper = platformWrapper;
        this.systemWrapper = systemWrapper;
    }

    /**
     * Initiates a graceful shutdown of the application.
     * This method performs the following steps:
     * 1. Stops the proxy session (if active)
     * 2. Waits for connections to close (with timeout)
     * 3. Closes the Spring application context
     * 4. Exits the JavaFX platform
     * 5. Terminates the JVM
     *
     * @param errorContext the error context that triggered the shutdown
     */
    public void initiateGracefulShutdown(ErrorContext errorContext) {
        logger.error("Initiating graceful shutdown due to fatal error: {} - {}",
                     errorContext.getCategory().getDisplayName(),
                     errorContext.getUserMessage());

        // Run shutdown in a separate thread to avoid blocking if called from UI thread
        Thread shutdownThread = new Thread(() -> performShutdown(errorContext), "shutdown-thread");
        shutdownThread.setDaemon(false); // Ensure it completes before JVM exits
        shutdownThread.start();
    }

    /**
     * Performs the actual shutdown sequence.
     */
    private void performShutdown(ErrorContext errorContext) {
        try {
            // Step 1: Stop proxy session if active
            logger.info("Step 1: Stopping proxy session...");
            try {
                if (proxyService.isSessionActive()) {
                    proxyService.stopProxySession();
                    // Give connections time to close gracefully
                    logger.info("Waiting up to {} seconds for connections to close...",
                                SHUTDOWN_TIMEOUT_SECONDS);
                    TimeUnit.SECONDS.sleep(Math.min(SHUTDOWN_TIMEOUT_SECONDS, 3));
                }
            } catch (Exception e) {
                logger.warn("Error stopping proxy session during shutdown: {}", e.getMessage());
            }

            // Step 2: Close Spring application context
            logger.info("Step 2: Closing Spring application context...");
            try {
                applicationContext.close();
            } catch (Exception e) {
                logger.warn("Error closing Spring context during shutdown: {}", e.getMessage());
            }

            // Step 3: Exit JavaFX platform (must be done on FX Application Thread or after context is closed)
            logger.info("Step 3: Exiting JavaFX platform...");
            try {
                // Platform.exit() should be called on the JavaFX thread, but since we're shutting down
                // we'll call it directly. This might log warnings but ensures cleanup.
                platformWrapper.runLater(() -> {
                    try {
                        javafx.application.Platform.exit();
                    } catch (Exception e) {
                        logger.warn("Error exiting JavaFX platform: {}", e.getMessage());
                    }
                });

                // Give JavaFX time to exit
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (Exception e) {
                logger.warn("Error during JavaFX exit: {}", e.getMessage());
            }

            // Step 4: Log final shutdown message
            logger.info("Application shutdown complete. Exiting JVM...");

        } catch (Exception e) {
            logger.error("Unexpected error during shutdown: {}", e.getMessage(), e);
        } finally {
            // Step 5: Force JVM exit with error code
            // This is the final step and will terminate the application
            systemWrapper.exit(1);
        }
    }
}
