package com.tcpviewer.io.wrapper;

import java.io.IOException;

/**
 * Mockable wrapper interface for java.io.OutputStream.
 * Provides abstraction over OutputStream for testability without JDK 25 Mockito limitations.
 */
public interface OutputStreamWrapper extends AutoCloseable {

    /**
     * Writes len bytes from the specified byte array starting at offset off to this output stream.
     *
     * @param b   the data to write
     * @param off the start offset in the data
     * @param len the number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    void write(byte[] b, int off, int len) throws IOException;

    /**
     * Flushes this output stream and forces any buffered output bytes to be written out.
     *
     * @throws IOException if an I/O error occurs
     */
    void flush() throws IOException;

    /**
     * Closes this output stream and releases any system resources associated with this stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    void close() throws IOException;
}
