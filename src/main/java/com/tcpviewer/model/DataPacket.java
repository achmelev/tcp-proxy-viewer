package com.tcpviewer.model;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Represents a single data packet captured from a TCP connection.
 */
public class DataPacket {
    private final LocalDateTime timestamp;
    private final Direction direction;
    private final byte[] rawData;
    private final DataType dataType;
    private final String displayText;

    public DataPacket(LocalDateTime timestamp, Direction direction, byte[] rawData,
                      DataType dataType, String displayText) {
        this.timestamp = timestamp;
        this.direction = direction;
        this.rawData = Arrays.copyOf(rawData, rawData.length);
        this.dataType = dataType;
        this.displayText = displayText;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Direction getDirection() {
        return direction;
    }

    public byte[] getRawData() {
        return Arrays.copyOf(rawData, rawData.length);
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getDisplayText() {
        return displayText;
    }

    public int getSize() {
        return rawData.length;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] %s - %d bytes",
                timestamp, direction, dataType, rawData.length);
    }
}
