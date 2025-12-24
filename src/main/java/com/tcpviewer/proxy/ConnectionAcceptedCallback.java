package com.tcpviewer.proxy;

import java.net.Socket;
import java.util.UUID;

/**
 * Callback interface for connection accepted events.
 */
@FunctionalInterface
public interface ConnectionAcceptedCallback {

    /**
     * Called when a new client connection is accepted.
     *
     * @param connectionId   The unique connection identifier
     * @param clientSocket   The client socket
     */
    void onConnectionAccepted(UUID connectionId, Socket clientSocket);
}
