package com.tcpviewer.proxy;

import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.io.wrapper.factory.SocketFactory;
import com.tcpviewer.model.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Handles a single TCP proxy connection.
 * Manages bidirectional data flow between client and target server.
 */
public class ProxyConnectionHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProxyConnectionHandler.class);

    private final SocketWrapper clientSocket;
    private final String targetHost;
    private final int targetPort;
    private final DataCaptureListener listener;
    private final UUID connectionId;
    private final ExecutorService executorService;
    private final SocketFactory socketFactory;

    public ProxyConnectionHandler(SocketWrapper clientSocket, String targetHost, int targetPort,
                                   DataCaptureListener listener, UUID connectionId,
                                   ExecutorService executorService, SocketFactory socketFactory) {
        this.clientSocket = clientSocket;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.listener = listener;
        this.connectionId = connectionId;
        this.executorService = executorService;
        this.socketFactory = socketFactory;
    }

    @Override
    public void run() {
        SocketWrapper targetSocket = null;
        try {
            logger.info("Connecting to target {}:{} for connection {}",
                       targetHost, targetPort, connectionId);

            // Connect to target server
            targetSocket = socketFactory.createSocket(targetHost, targetPort);
            targetSocket.setTcpNoDelay(true);

            logger.info("Connected to target for connection {}", connectionId);

            // Create bidirectional forwarders
            TcpForwarder clientToTarget = new TcpForwarder(
                    clientSocket.getInputStream(),
                    targetSocket.getOutputStream(),
                    listener,
                    connectionId,
                    Direction.CLIENT_TO_SERVER,
                    "Client→Target"
            );

            TcpForwarder targetToClient = new TcpForwarder(
                    targetSocket.getInputStream(),
                    clientSocket.getOutputStream(),
                    listener,
                    connectionId,
                    Direction.SERVER_TO_CLIENT,
                    "Target→Client"
            );

            // Start forwarding in both directions
            Thread clientToTargetThread = new Thread(clientToTarget, "Forwarder-C2T-" + connectionId);
            Thread targetToClientThread = new Thread(targetToClient, "Forwarder-T2C-" + connectionId);

            clientToTargetThread.start();
            targetToClientThread.start();

            // Wait for both threads to complete
            clientToTargetThread.join();
            targetToClientThread.join();

            logger.info("Connection {} closed", connectionId);

        } catch (IOException e) {
            logger.error("Error handling connection {}: {}", connectionId, e.getMessage());
        } catch (InterruptedException e) {
            logger.warn("Connection handler interrupted for {}", connectionId);
            Thread.currentThread().interrupt();
        } finally {
            closeSocket(clientSocket);
            closeSocket(targetSocket);

            // Notify listener that connection has closed
            if (listener != null) {
                try {
                    listener.onConnectionClosed(connectionId);
                } catch (Exception e) {
                    // Log but don't rethrow - connection is already closed
                    logger.error("Error in connection closed callback for {}: {}",
                            connectionId, e.getMessage());
                }
            }
        }
    }

    /**
     * Closes a socket without throwing exceptions.
     */
    private void closeSocket(SocketWrapper socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.trace("Error closing socket: {}", e.getMessage());
            }
        }
    }
}
