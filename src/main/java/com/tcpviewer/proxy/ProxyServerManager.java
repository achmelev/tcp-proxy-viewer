package com.tcpviewer.proxy;

import com.tcpviewer.error.ErrorHandlerService;
import com.tcpviewer.io.wrapper.factory.ServerSocketFactory;
import com.tcpviewer.io.wrapper.factory.SocketFactory;
import com.tcpviewer.lang.wrapper.ExecutorServiceWrapper;
import com.tcpviewer.lang.wrapper.ThreadWrapper;
import com.tcpviewer.lang.wrapper.factory.ExecutorServiceFactory;
import com.tcpviewer.lang.wrapper.factory.ThreadFactory;
import com.tcpviewer.model.ProxySession;
import com.tcpviewer.ssl.ServerCertificateGeneratorService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Manages the lifecycle of the ProxyServer.
 * Handles starting, stopping, and cleanup of proxy server instances.
 */
@Service
public class ProxyServerManager {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServerManager.class);

    private final Executor proxyExecutor;
    private final SocketFactory socketFactory;
    private final ServerSocketFactory serverSocketFactory;
    private final ThreadFactory threadFactory;
    private final ExecutorServiceFactory executorServiceFactory;
    private final ErrorHandlerService errorHandlerService;
    private final ServerCertificateGeneratorService serverCertificateGeneratorService;

    private ProxyServer currentServer;
    private ThreadWrapper serverThread;
    private ExecutorServiceWrapper connectionExecutor;

    public ProxyServerManager(@Qualifier("proxyExecutor") Executor proxyExecutor,
                              SocketFactory socketFactory,
                              ServerSocketFactory serverSocketFactory,
                              ThreadFactory threadFactory,
                              ExecutorServiceFactory executorServiceFactory,
                              ErrorHandlerService errorHandlerService,
                              ServerCertificateGeneratorService serverCertificateGeneratorService) {
        this.proxyExecutor = proxyExecutor;
        this.socketFactory = socketFactory;
        this.serverSocketFactory = serverSocketFactory;
        this.threadFactory = threadFactory;
        this.executorServiceFactory = executorServiceFactory;
        this.errorHandlerService = errorHandlerService;
        this.serverCertificateGeneratorService = serverCertificateGeneratorService;
    }

    /**
     * Starts a new proxy server with the given session configuration.
     *
     * @param session                     The proxy session configuration
     * @param dataCaptureListener         Listener for captured data
     * @param connectionAcceptedCallback  Callback when a connection is accepted
     * @throws IllegalStateException if a server is already running
     */
    public void startServer(ProxySession session,
                           DataCaptureListener dataCaptureListener,
                           ConnectionAcceptedCallback connectionAcceptedCallback) {
        if (currentServer != null && currentServer.isRunning()) {
            throw new IllegalStateException("Proxy server is already running");
        }

        // Create dedicated executor for connection handlers
        connectionExecutor = executorServiceFactory.createCachedThreadPool(threadFactory);

        // Create and start proxy server
        currentServer = new ProxyServer(
                session.isSsl(),
                session.getLocalIp(),
                session.getLocalPort(),
                session.getTargetHost(),
                session.getSslHostName(),
                session.getTargetPort(),
                dataCaptureListener,
                connectionAcceptedCallback,
                connectionExecutor,
                socketFactory,
                serverSocketFactory,
                threadFactory,
                errorHandlerService,
                serverCertificateGeneratorService
        );

        serverThread = threadFactory.createThread(currentServer, "ProxyServer");
        serverThread.setDaemon(false);
        serverThread.start();

        session.setActive(true);
        logger.info("Proxy server started: {}", session.getDisplayInfo());
    }

    /**
     * Stops the currently running proxy server.
     */
    public void stopServer() {
        if (currentServer != null) {
            logger.info("Stopping proxy server...");
            currentServer.stop();

            // Interrupt server thread if still running
            if (serverThread != null && serverThread.isAlive()) {
                serverThread.interrupt();
            }

            // Shutdown connection executor
            if (connectionExecutor != null) {
                shutdownExecutor(connectionExecutor);
            }

            currentServer = null;
            serverThread = null;
            connectionExecutor = null;

            logger.info("Proxy server stopped");
        }
    }

    public ProxyServer getCurrentServer() {
        return currentServer;
    }

    /**
     * Checks if a proxy server is currently running.
     *
     * @return true if server is running, false otherwise
     */
    public boolean isRunning() {
        return currentServer != null && currentServer.isRunning();
    }

    /**
     * Gracefully shuts down an executor service.
     */
    private void shutdownExecutor(ExecutorServiceWrapper executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("Executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor shutdown");
            executor.shutdownNow();
            threadFactory.currentThread().interrupt();
        }
    }

    /**
     * Cleanup on application shutdown.
     */
    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up ProxyServerManager");
        stopServer();
    }
}
