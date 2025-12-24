package com.tcpviewer.io.wrapper.impl;

import com.tcpviewer.io.wrapper.InputStreamWrapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Default implementation of InputStreamWrapper that delegates to java.io.InputStream.
 * Provides zero-overhead delegation to the underlying input stream.
 */
public class DefaultInputStreamWrapper implements InputStreamWrapper {

    private final InputStream delegate;

    /**
     * Creates a new DefaultInputStreamWrapper that wraps the given input stream.
     *
     * @param delegate the input stream to wrap
     */
    public DefaultInputStreamWrapper(InputStream delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("InputStream delegate cannot be null");
        }
        this.delegate = delegate;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
