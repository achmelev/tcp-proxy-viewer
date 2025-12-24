package com.tcpviewer.service;

import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.model.ConnectionInfo;
import com.tcpviewer.model.DataPacket;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active TCP connections and their data.
 * Thread-safe registry with JavaFX UI integration.
 */
@Service
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final ConcurrentHashMap<UUID, ConnectionInfo> connections = new ConcurrentHashMap<>();
    private final ObservableList<ConnectionInfo> connectionList = FXCollections.observableArrayList();

    /**
     * Registers a new connection.
     *
     * @param connectionId   The unique connection identifier
     * @param clientSocket   The client socket
     * @return The created ConnectionInfo object
     */
    public ConnectionInfo registerConnection(UUID connectionId, SocketWrapper clientSocket) {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        int clientPort = clientSocket.getPort();

        ConnectionInfo connectionInfo = new ConnectionInfo(connectionId, clientAddress, clientPort);
        connections.put(connectionId, connectionInfo);

        // Update UI on JavaFX thread
        Platform.runLater(() -> connectionList.add(connectionInfo));

        logger.info("Registered connection: {}", connectionInfo.getDisplayName());
        return connectionInfo;
    }

    /**
     * Registers a new connection without a socket (for callback-based registration).
     */
    public ConnectionInfo registerConnection(UUID connectionId, String clientAddress, int clientPort) {
        ConnectionInfo connectionInfo = new ConnectionInfo(connectionId, clientAddress, clientPort);
        connections.put(connectionId, connectionInfo);

        // Update UI on JavaFX thread
        Platform.runLater(() -> connectionList.add(connectionInfo));

        logger.info("Registered connection: {}", connectionInfo.getDisplayName());
        return connectionInfo;
    }

    /**
     * Adds a data packet to a connection.
     *
     * @param connectionId The connection identifier
     * @param packet       The data packet to add
     */
    public void addDataPacket(UUID connectionId, DataPacket packet) {
        ConnectionInfo connection = connections.get(connectionId);
        if (connection != null) {
            // Update UI on JavaFX thread
            Platform.runLater(() -> connection.addDataPacket(packet));
        } else {
            logger.warn("Attempted to add data to unknown connection: {}", connectionId);
        }
    }

    /**
     * Marks a connection as closed.
     *
     * @param connectionId The connection identifier
     */
    public void closeConnection(UUID connectionId) {
        ConnectionInfo connection = connections.get(connectionId);
        if (connection != null) {
            Platform.runLater(() -> connection.setActive(false));
            logger.info("Connection closed: {}", connection.getDisplayName());
        }
    }

    /**
     * Gets a connection by ID.
     *
     * @param connectionId The connection identifier
     * @return The ConnectionInfo or null if not found
     */
    public ConnectionInfo getConnection(UUID connectionId) {
        return connections.get(connectionId);
    }

    /**
     * Returns the observable list of connections for UI binding.
     *
     * @return ObservableList of ConnectionInfo objects
     */
    public ObservableList<ConnectionInfo> getConnectionList() {
        return connectionList;
    }

    /**
     * Gets the count of active connections.
     *
     * @return Number of active connections
     */
    public int getActiveConnectionCount() {
        return (int) connections.values().stream()
                .filter(ConnectionInfo::isActive)
                .count();
    }

    /**
     * Gets the total connection count (active and closed).
     *
     * @return Total number of connections
     */
    public int getTotalConnectionCount() {
        return connections.size();
    }

    /**
     * Clears all connections.
     */
    public void clear() {
        connections.clear();
        Platform.runLater(connectionList::clear);
        logger.info("All connections cleared");
    }
}
