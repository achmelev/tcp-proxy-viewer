package com.tcpviewer.io.wrapper;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Mockable wrapper interface for java.net.ServerSocket.
 * Provides abstraction over ServerSocket for testability without JDK 25 Mockito limitations.
 * Enables testing of server socket operations without actual network binding.
 */
public interface ServerSocketWrapper extends AutoCloseable {

    /**
     * Binds the ServerSocket to a specific address (IP address and port number).
     *
     * @param endpoint the IP address and port number to bind to
     * @throws IOException if the bind operation fails
     */
    void bind(SocketAddress endpoint) throws IOException;

    /**
     * Listens for a connection to be made to this socket and accepts it.
     * The method blocks until a connection is made.
     *
     * @return a SocketWrapper for the accepted connection
     * @throws IOException if an I/O error occurs when waiting for a connection
     */
    SocketWrapper accept() throws IOException;

    /**
     * Enable/disable the SO_REUSEADDR socket option.
     *
     * @param on whether to enable or disable the socket option
     * @throws IOException if there is an error in the underlying protocol
     */
    void setReuseAddress(boolean on) throws IOException;

    /**
     * Returns the closed state of the ServerSocket.
     *
     * @return true if the socket has been closed
     */
    boolean isClosed();

    /**
     * Closes this socket.
     *
     * @throws IOException if an I/O error occurs when closing this socket
     */
    @Override
    void close() throws IOException;
}
