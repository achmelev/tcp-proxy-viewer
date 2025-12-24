package com.tcpviewer.proxy;

import com.tcpviewer.io.wrapper.InputStreamWrapper;
import com.tcpviewer.io.wrapper.OutputStreamWrapper;
import com.tcpviewer.model.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Handles unidirectional TCP data forwarding with capture capability.
 * Reads from source stream, writes to destination stream, and notifies listener.
 */
public class TcpForwarder implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TcpForwarder.class);
    private static final int BUFFER_SIZE = 8192;
    private static final int DRAIN_TIMEOUT_MS = 50;

    private final InputStreamWrapper source;
    private final OutputStreamWrapper destination;
    private final DataCaptureListener listener;
    private final UUID connectionId;
    private final Direction direction;
    private final String name;

    public TcpForwarder(InputStreamWrapper source, OutputStreamWrapper destination,
                        DataCaptureListener listener, UUID connectionId,
                        Direction direction, String name) {
        this.source = source;
        this.destination = destination;
        this.listener = listener;
        this.connectionId = connectionId;
        this.direction = direction;
        this.name = name;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        List<byte[]> accumulatedChunks = new ArrayList<>();
        int accumulatedSize = 0;

        try {
            int bytesRead;
            while ((bytesRead = source.read(buffer)) != -1) {
                // Forward data immediately (no latency)
                destination.write(buffer, 0, bytesRead);
                destination.flush();

                // Accumulate chunk for capture
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                accumulatedChunks.add(chunk);
                accumulatedSize += bytesRead;

                logger.trace("{} forwarded {} bytes", name, bytesRead);

                // Drain the pipe: keep reading while data is immediately available
                boolean drained = false;
                while (!drained) {
                    int available = getAvailableBytes();
                    if (available > 0) {
                        bytesRead = source.read(buffer);
                        if (bytesRead == -1) {
                            break; // EOF reached
                        }
                        // Forward data immediately (no latency)
                        destination.write(buffer, 0, bytesRead);
                        destination.flush();
                        // Accumulate chunk for capture
                        chunk = Arrays.copyOf(buffer, bytesRead);
                        accumulatedChunks.add(chunk);
                        accumulatedSize += bytesRead;
                        logger.trace("{} forwarded {} bytes (draining)", name, bytesRead);
                    } else {
                        // Check if more data is available
                        available = getAvailableBytes();
                        if (available == 0) {
                            // If no data available, wait and try again
                            try {
                                Thread.sleep(DRAIN_TIMEOUT_MS);
                            } catch (InterruptedException e) {
                                //ignore
                            }
                            available = getAvailableBytes();
                            //if avalable == 0 after wait, mark  the pipe drained
                            drained = true;
                        }
                    }
                }

                if (!accumulatedChunks.isEmpty() && listener != null) {
                    byte[] packetData = combineChunks(accumulatedChunks, accumulatedSize);
                    listener.onDataCaptured(connectionId, packetData, direction);
                    accumulatedChunks.clear();
                    accumulatedSize = 0;
                    logger.trace("{} created packet with {} bytes", name, packetData.length);
                }

                // If we reached EOF while draining, break out
                if (bytesRead == -1) {
                    break;
                }
            }

            // Handle any remaining accumulated data at EOF
            if (!accumulatedChunks.isEmpty() && listener != null) {
                byte[] packetData = combineChunks(accumulatedChunks, accumulatedSize);
                listener.onDataCaptured(connectionId, packetData, direction);
                accumulatedChunks.clear();
                accumulatedSize = 0;
                logger.trace("{} created final packet with {} bytes", name, packetData.length);
            }

            logger.debug("{} reached end of stream", name);
        } catch (IOException e) {
            // Handle any remaining data before closing
            if (!accumulatedChunks.isEmpty() && listener != null) {
                byte[] packetData = combineChunks(accumulatedChunks, accumulatedSize);
                listener.onDataCaptured(connectionId, packetData, direction);
                accumulatedChunks.clear();
                accumulatedSize = 0;
                logger.trace("{} created packet before error: {} bytes", name, packetData.length);
            }
            logger.debug("{} connection closed: {}", name, e.getMessage());
        } finally {
            closeQuietly(source);
            closeQuietly(destination);
        }
    }

    /**
     * Gets the number of bytes available to read without blocking.
     * Handles IOException by treating it as 0 available bytes.
     *
     * @return the number of bytes available, or 0 if an error occurs
     */
    private int getAvailableBytes() {
        try {
            return source.available();
        } catch (IOException e) {
            logger.warn("{} available() threw exception, treating as 0: {}", name, e.getMessage());
            return 0;
        }
    }

    /**
     * Combines multiple byte array chunks into a single byte array.
     *
     * @param chunks    List of byte arrays to combine
     * @param totalSize Total size of all chunks combined
     * @return Single byte array containing all chunk data
     */
    private byte[] combineChunks(List<byte[]> chunks, int totalSize) {
        byte[] result = new byte[totalSize];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }

    /**
     * Closes a closeable resource without throwing exceptions.
     */
    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.trace("Error closing resource: {}", e.getMessage());
            }
        }
    }
}
