package com.tcpviewer.proxy;

import com.tcpviewer.model.Direction;

import java.util.UUID;

/**
 * Interface for TCP connection lifecycle events.
 * Implementations receive callbacks for data capture and connection closure.
 */
public interface DataCaptureListener {

    /**
     * Called when data is captured from a TCP connection.
     *
     * @param connectionId The unique identifier of the connection
     * @param data         The captured byte array
     * @param direction    The direction of data flow (CLIENT_TO_SERVER or SERVER_TO_CLIENT)
     */
    void onDataCaptured(UUID connectionId, byte[] data, Direction direction);

    /**
     * Called when a TCP connection is closed.
     * This is invoked after both forwarder threads complete and sockets are closed.
     *
     * @param connectionId The unique identifier of the connection that closed
     */
    void onConnectionClosed(UUID connectionId);
}
