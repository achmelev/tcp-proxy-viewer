package com.tcpviewer.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents metadata and data for a single TCP connection.
 * Uses JavaFX ObservableList for automatic UI updates.
 */
public class ConnectionInfo {
    private final UUID connectionId;
    private final String clientAddress;
    private final int clientPort;
    private final LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
    private final SimpleBooleanProperty active;
    private final ObservableList<DataPacket> dataPackets;

    public ConnectionInfo(UUID connectionId, String clientAddress, int clientPort) {
        this.connectionId = connectionId;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.connectedAt = LocalDateTime.now();
        this.active = new SimpleBooleanProperty(true);
        this.dataPackets = FXCollections.observableArrayList();
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public LocalDateTime getDisconnectedAt() {
        return disconnectedAt;
    }

    public boolean isActive() {
        return active.get();
    }

    public void setActive(boolean active) {
        this.active.set(active);
        if (!active) {
            this.disconnectedAt = LocalDateTime.now();
        }
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public ObservableList<DataPacket> getDataPackets() {
        return dataPackets;
    }

    public void addDataPacket(DataPacket packet) {
        dataPackets.add(packet);
    }

    public String getDisplayName() {
        return String.format("%s:%d", clientAddress, clientPort);
    }

    public long getTotalBytes() {
        return dataPackets.stream()
                .mapToLong(DataPacket::getSize)
                .sum();
    }

    @Override
    public String toString() {
        return String.format("Connection[%s - %s - %s]",
                getDisplayName(),
                active.get() ? "ACTIVE" : "CLOSED",
                connectedAt);
    }
}
