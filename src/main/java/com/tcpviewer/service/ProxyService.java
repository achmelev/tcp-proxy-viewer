package com.tcpviewer.service;

import com.tcpviewer.model.ConnectionInfo;
import com.tcpviewer.model.DataPacket;
import com.tcpviewer.model.Direction;
import com.tcpviewer.model.ProxySession;
import com.tcpviewer.proxy.DataCaptureListener;
import com.tcpviewer.util.DataProcessor;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.Socket;
import java.util.UUID;

/**
 * Main service for TCP proxy operations.
 * Orchestrates ProxyServerManager, ConnectionManager, and DataProcessor.
 */
@Service
public class ProxyService implements DataCaptureListener {

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);

    private final ProxyServerManager serverManager;
    private final ConnectionManager connectionManager;
    private final DataProcessor dataProcessor;

    private ProxySession currentSession;

    public ProxyService(ProxyServerManager serverManager,
                       ConnectionManager connectionManager,
                       DataProcessor dataProcessor) {
        this.serverManager = serverManager;
        this.connectionManager = connectionManager;
        this.dataProcessor = dataProcessor;
    }

    /**
     * Starts a new proxy session.
     *
     * @param session The proxy session configuration
     * @throws IllegalStateException if a session is already active
     */
    public void startProxySession(ProxySession session) {
        if (currentSession != null && currentSession.isActive()) {
            throw new IllegalStateException("A proxy session is already active");
        }

        // Clear previous connections
        connectionManager.clear();

        // Start proxy server
        serverManager.startServer(
                session,
                this, // ProxyService implements DataCaptureListener
                (connectionId, clientSocket) -> onConnectionAccepted(connectionId, clientSocket)
        );

        currentSession = session;
        logger.info("Proxy session started: {}", session.getDisplayInfo());
    }

    /**
     * Stops the current proxy session.
     */
    public void stopProxySession() {
        if (currentSession != null) {
            serverManager.stopServer();
            currentSession.setActive(false);
            logger.info("Proxy session stopped");
        }
    }

    /**
     * Checks if a proxy session is currently active.
     *
     * @return true if session is active, false otherwise
     */
    public boolean isSessionActive() {
        return currentSession != null && currentSession.isActive() && serverManager.isRunning();
    }

    /**
     * Gets the current proxy session.
     *
     * @return The current ProxySession or null if none active
     */
    public ProxySession getCurrentSession() {
        return currentSession;
    }

    /**
     * Gets the observable list of active connections.
     *
     * @return ObservableList of ConnectionInfo objects
     */
    public ObservableList<ConnectionInfo> getActiveConnections() {
        return connectionManager.getConnectionList();
    }

    /**
     * Gets connection data for a specific connection.
     *
     * @param connectionId The connection identifier
     * @return ConnectionInfo or null if not found
     */
    public ConnectionInfo getConnectionData(UUID connectionId) {
        return connectionManager.getConnection(connectionId);
    }

    /**
     * Gets the count of active connections.
     *
     * @return Number of active connections
     */
    public int getActiveConnectionCount() {
        return connectionManager.getActiveConnectionCount();
    }

    /**
     * Callback when a new connection is accepted.
     */
    private void onConnectionAccepted(UUID connectionId, Socket clientSocket) {
        // Register connection with full socket information
        connectionManager.registerConnection(connectionId, clientSocket);
        logger.debug("Connection accepted and registered: {}", connectionId);
    }

    /**
     * Implementation of DataCaptureListener.onDataCaptured
     * Called by TcpForwarder when data is captured.
     */
    @Override
    public void onDataCaptured(UUID connectionId, byte[] data, Direction direction) {
        try {
            // Connection should already be registered by onConnectionAccepted
            ConnectionInfo connection = connectionManager.getConnection(connectionId);
            if (connection == null) {
                logger.warn("Received data for unregistered connection: {}", connectionId);
                return;
            }

            // Process data and create packet
            DataPacket packet = dataProcessor.process(data, direction);

            // Add to connection
            connectionManager.addDataPacket(connectionId, packet);

            logger.trace("Data captured for connection {}: {} bytes, direction: {}",
                    connectionId, data.length, direction);

        } catch (Exception e) {
            logger.error("Error processing captured data for connection {}: {}",
                    connectionId, e.getMessage(), e);
        }
    }
}
