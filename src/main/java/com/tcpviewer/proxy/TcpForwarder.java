package com.tcpviewer.proxy;

import com.tcpviewer.model.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Handles unidirectional TCP data forwarding with capture capability.
 * Reads from source stream, writes to destination stream, and notifies listener.
 */
public class TcpForwarder implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TcpForwarder.class);
    private static final int BUFFER_SIZE = 8192;

    private final InputStream source;
    private final OutputStream destination;
    private final DataCaptureListener listener;
    private final UUID connectionId;
    private final Direction direction;
    private final String name;

    public TcpForwarder(InputStream source, OutputStream destination,
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
        try {
            int bytesRead;
            while ((bytesRead = source.read(buffer)) != -1) {
                // Forward data to destination
                destination.write(buffer, 0, bytesRead);
                destination.flush();

                // Capture data for display
                byte[] capturedData = Arrays.copyOf(buffer, bytesRead);
                if (listener != null) {
                    listener.onDataCaptured(connectionId, capturedData, direction);
                }

                logger.trace("{} forwarded {} bytes", name, bytesRead);
            }

            logger.debug("{} reached end of stream", name);
        } catch (IOException e) {
            // Connection closed or error occurred
            logger.debug("{} connection closed: {}", name, e.getMessage());
        } finally {
            closeQuietly(source);
            closeQuietly(destination);
        }
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
