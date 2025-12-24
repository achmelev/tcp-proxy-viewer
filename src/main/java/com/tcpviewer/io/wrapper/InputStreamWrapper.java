package com.tcpviewer.io.wrapper;

import java.io.IOException;

/**
 * Mockable wrapper interface for java.io.InputStream.
 * Provides abstraction over InputStream for testability without JDK 25 Mockito limitations.
 */
public interface InputStreamWrapper extends AutoCloseable {

    /**
     * Reads some number of bytes from the input stream and stores them into the buffer array.
     * The number of bytes actually read is returned as an integer.
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     *         because the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    int read(byte[] b) throws IOException;

    /**
     * Returns an estimate of the number of bytes that can be read without blocking.
     *
     * @return the number of bytes available to read without blocking
     * @throws IOException if an I/O error occurs
     */
    int available() throws IOException;

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    void close() throws IOException;
}
