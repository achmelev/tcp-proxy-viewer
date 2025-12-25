package com.tcpviewer.io.wrapper.factory;

import com.tcpviewer.io.wrapper.SocketWrapper;

import java.io.IOException;
import java.net.Socket;

/**
 * Factory interface for creating and wrapping sockets.
 * Centralizes socket creation for testability and dependency injection.
 */
public interface SocketFactory {

    /**
     * Creates a new socket and connects it to the specified host and port.
     *
     * @param  host the host name or ip adress to connect to
     * @param  port the port number to connect to
     * @param  ssl if true, am ssl socket will be created
     * @param  sniHostName the host name to use as SNI parameter
     * @return a SocketWrapper wrapping the newly created socket
     * @throws IOException if an I/O error occurs when creating the socket
     */
    SocketWrapper createSocket(String host, int port, boolean ssl, String sniHostName) throws IOException;

    /**
     * Wraps an existing socket (e.g., from ServerSocket.accept()) in a SocketWrapper.
     *
     * @param socket the socket to wrap
     * @return a SocketWrapper wrapping the given socket
     */
    SocketWrapper wrapSocket(Socket socket);
}
