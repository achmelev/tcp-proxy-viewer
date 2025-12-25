package com.tcpviewer.io.wrapper.factory;

import com.tcpviewer.io.wrapper.ServerSocketWrapper;

import java.io.IOException;
import java.security.KeyStore;

/**
 * Factory interface for creating server socket wrappers.
 * Centralizes server socket creation for testability and dependency injection.
 */
public interface ServerSocketFactory {

    /**
     * Creates a new unbound server socket.
     *
     * @param  keyStore - if not null, a Wrapper around a SSL ServerSocket is created
     *
     * @return a ServerSocketWrapper wrapping a new unbound ServerSocket
     * @throws IOException if an I/O error occurs when opening the socket
     */
    ServerSocketWrapper createServerSocket(KeyStore keyStore) throws IOException;
}
