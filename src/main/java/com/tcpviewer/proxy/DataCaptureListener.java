package com.tcpviewer.proxy;

import com.tcpviewer.model.Direction;

import java.util.UUID;

/**
 * Interface for capturing data from TCP connections.
 * Implementations receive callbacks when data is forwarded through the proxy.
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
}
