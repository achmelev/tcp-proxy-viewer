package com.tcpviewer.io.wrapper.impl;

import com.tcpviewer.io.wrapper.ServerSocketWrapper;
import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.io.wrapper.factory.SocketFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;

/**
 * Default implementation of ServerSocketWrapper that delegates to java.net.ServerSocket.
 * Provides zero-overhead delegation for production use.
 */
public class DefaultServerSocketWrapper implements ServerSocketWrapper {

    private final ServerSocket delegate;
    private final SocketFactory socketFactory;

    /**
     * Creates a new ServerSocketWrapper wrapping a ServerSocket.
     *
     * @param delegate      the ServerSocket to wrap
     * @param socketFactory factory for wrapping accepted sockets
     */
    public DefaultServerSocketWrapper(ServerSocket delegate, SocketFactory socketFactory) {
        this.delegate = delegate;
        this.socketFactory = socketFactory;
    }

    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        delegate.bind(endpoint);
    }

    @Override
    public SocketWrapper accept() throws IOException {
        return socketFactory.wrapSocket(delegate.accept());
    }

    @Override
    public void setReuseAddress(boolean on) throws IOException {
        delegate.setReuseAddress(on);
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
