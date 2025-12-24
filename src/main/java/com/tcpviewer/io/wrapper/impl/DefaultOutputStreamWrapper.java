package com.tcpviewer.io.wrapper.impl;

import com.tcpviewer.io.wrapper.OutputStreamWrapper;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Default implementation of OutputStreamWrapper that delegates to java.io.OutputStream.
 * Provides zero-overhead delegation to the underlying output stream.
 */
public class DefaultOutputStreamWrapper implements OutputStreamWrapper {

    private final OutputStream delegate;

    /**
     * Creates a new DefaultOutputStreamWrapper that wraps the given output stream.
     *
     * @param delegate the output stream to wrap
     */
    public DefaultOutputStreamWrapper(OutputStream delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("OutputStream delegate cannot be null");
        }
        this.delegate = delegate;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
