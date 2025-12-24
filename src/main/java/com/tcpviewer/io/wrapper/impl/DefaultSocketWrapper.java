package com.tcpviewer.io.wrapper.impl;

import com.tcpviewer.io.wrapper.InputStreamWrapper;
import com.tcpviewer.io.wrapper.OutputStreamWrapper;
import com.tcpviewer.io.wrapper.SocketWrapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Default implementation of SocketWrapper that delegates to java.net.Socket.
 * Provides zero-overhead delegation to the underlying socket.
 */
public class DefaultSocketWrapper implements SocketWrapper {

    private final Socket delegate;
    private InputStreamWrapper inputStreamWrapper;
    private OutputStreamWrapper outputStreamWrapper;

    /**
     * Creates a new DefaultSocketWrapper that wraps the given socket.
     *
     * @param delegate the socket to wrap
     */
    public DefaultSocketWrapper(Socket delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Socket delegate cannot be null");
        }
        this.delegate = delegate;
    }

    @Override
    public InputStreamWrapper getInputStream() throws IOException {
        if (inputStreamWrapper == null) {
            inputStreamWrapper = new DefaultInputStreamWrapper(delegate.getInputStream());
        }
        return inputStreamWrapper;
    }

    @Override
    public OutputStreamWrapper getOutputStream() throws IOException {
        if (outputStreamWrapper == null) {
            outputStreamWrapper = new DefaultOutputStreamWrapper(delegate.getOutputStream());
        }
        return outputStreamWrapper;
    }

    @Override
    public void setTcpNoDelay(boolean on) throws IOException {
        delegate.setTcpNoDelay(on);
    }

    @Override
    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
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
