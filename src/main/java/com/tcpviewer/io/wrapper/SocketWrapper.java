package com.tcpviewer.io.wrapper;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Mockable wrapper interface for java.net.Socket.
 * Provides abstraction over Socket for testability without JDK 25 Mockito limitations.
 */
public interface SocketWrapper extends AutoCloseable {

    /**
     * Returns an input stream wrapper for this socket.
     *
     * @return the input stream wrapper for reading from this socket
     * @throws IOException if an I/O error occurs when creating the input stream
     */
    InputStreamWrapper getInputStream() throws IOException;

    /**
     * Returns an output stream wrapper for this socket.
     *
     * @return the output stream wrapper for writing to this socket
     * @throws IOException if an I/O error occurs when creating the output stream
     */
    OutputStreamWrapper getOutputStream() throws IOException;

    /**
     * Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
     *
     * @param on true to enable TCP_NODELAY, false to disable
     * @throws IOException if there is an error in the underlying protocol
     */
    void setTcpNoDelay(boolean on) throws IOException;

    /**
     * Returns the address to which the socket is connected.
     *
     * @return the remote IP address to which this socket is connected
     */
    InetAddress getInetAddress();

    /**
     * Returns the remote port number to which this socket is connected.
     *
     * @return the remote port number to which this socket is connected
     */
    int getPort();

    /**
     * Returns the closed state of the socket.
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
