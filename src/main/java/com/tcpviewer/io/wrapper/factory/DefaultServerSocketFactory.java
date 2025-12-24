package com.tcpviewer.io.wrapper.factory;

import com.tcpviewer.io.wrapper.ServerSocketWrapper;
import com.tcpviewer.io.wrapper.impl.DefaultServerSocketWrapper;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Default implementation of ServerSocketFactory.
 * Creates DefaultServerSocketWrapper instances that delegate to java.net.ServerSocket.
 */
public class DefaultServerSocketFactory implements ServerSocketFactory {

    private final SocketFactory socketFactory;

    /**
     * Creates a new DefaultServerSocketFactory.
     *
     * @param socketFactory factory for wrapping accepted sockets
     */
    public DefaultServerSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    @Override
    public ServerSocketWrapper createServerSocket() throws IOException {
        return new DefaultServerSocketWrapper(new ServerSocket(), socketFactory);
    }
}
