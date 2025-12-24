package com.tcpviewer.proxy;

import com.tcpviewer.io.wrapper.ServerSocketWrapper;
import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.io.wrapper.factory.ServerSocketFactory;
import com.tcpviewer.io.wrapper.factory.SocketFactory;
import com.tcpviewer.lang.wrapper.ExecutorServiceWrapper;
import com.tcpviewer.lang.wrapper.factory.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP proxy server that accepts client connections and forwards them to a target.
 * Runs in a dedicated thread and notifies listeners of new connections.
 */
public class ProxyServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private final String localIp;
    private final int localPort;
    private final String targetHost;
    private final int targetPort;
    private final DataCaptureListener dataCaptureListener;
    private final ConnectionAcceptedCallback connectionAcceptedCallback;
    private final ExecutorServiceWrapper executorService;
    private final SocketFactory socketFactory;
    private final ServerSocketFactory serverSocketFactory;
    private final ThreadFactory threadFactory;

    private ServerSocketWrapper serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ProxyServer(String localIp, int localPort, String targetHost, int targetPort,
                       DataCaptureListener dataCaptureListener,
                       ConnectionAcceptedCallback connectionAcceptedCallback,
                       ExecutorServiceWrapper executorService,
                       SocketFactory socketFactory,
                       ServerSocketFactory serverSocketFactory,
                       ThreadFactory threadFactory) {
        this.localIp = localIp;
        this.localPort = localPort;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.dataCaptureListener = dataCaptureListener;
        this.connectionAcceptedCallback = connectionAcceptedCallback;
        this.executorService = executorService;
        this.socketFactory = socketFactory;
        this.serverSocketFactory = serverSocketFactory;
        this.threadFactory = threadFactory;
    }

    @Override
    public void run() {
        try {
            serverSocket = serverSocketFactory.createServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(localIp, localPort));
            running.set(true);

            logger.info("Proxy server started on {}:{}, forwarding to {}:{}",
                       localIp, localPort, targetHost, targetPort);

            while (running.get() && !threadFactory.currentThread().isInterrupted()) {
                try {
                    SocketWrapper clientSocket = serverSocket.accept();
                    handleClientConnection(clientSocket);
                } catch (SocketException e) {
                    if (running.get()) {
                        logger.error("Socket error in accept loop: {}", e.getMessage());
                    }
                    // If not running, this is expected during shutdown
                    break;
                } catch (IOException e) {
                    logger.error("Error accepting connection: {}", e.getMessage());
                }
            }

        } catch (IOException e) {
            logger.error("Failed to start proxy server: {}", e.getMessage());
            throw new RuntimeException("Failed to start proxy server", e);
        } finally {
            running.set(false);
            closeServerSocket();
            logger.info("Proxy server stopped");
        }
    }

    /**
     * Handles a new client connection.
     */
    private void handleClientConnection(SocketWrapper clientSocket) {
        try {
            UUID connectionId = UUID.randomUUID();
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort();

            logger.info("Accepted connection from {}:{} (ID: {})",
                       clientAddress, clientPort, connectionId);

            // Notify callback of new connection
            if (connectionAcceptedCallback != null) {
                connectionAcceptedCallback.onConnectionAccepted(connectionId, clientSocket);
            }

            // Create and submit connection handler
            ProxyConnectionHandler handler = new ProxyConnectionHandler(
                    clientSocket, targetHost, targetPort,
                    dataCaptureListener, connectionId, socketFactory, threadFactory
            );

            executorService.submit(handler);

        } catch (Exception e) {
            logger.error("Error handling client connection: {}", e.getMessage());
            closeSocket(clientSocket);
        }
    }

    /**
     * Stops the proxy server.
     */
    public void stop() {
        logger.info("Stopping proxy server...");
        running.set(false);
        closeServerSocket();
    }

    /**
     * Closes the server socket.
     */
    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.warn("Error closing server socket: {}", e.getMessage());
            }
        }
    }

    /**
     * Closes a client socket.
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

    public boolean isRunning() {
        return running.get();
    }
}
