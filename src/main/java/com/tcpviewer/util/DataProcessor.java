package com.tcpviewer.util;

import com.tcpviewer.model.DataPacket;
import com.tcpviewer.model.DataType;
import com.tcpviewer.model.Direction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Processes raw byte data and creates formatted DataPacket objects.
 * Integrates TextDetector and HexDumpFormatter.
 */
@Component
public class DataProcessor {

    private final TextDetector textDetector;
    private final HexDumpFormatter hexDumpFormatter;

    public DataProcessor(TextDetector textDetector, HexDumpFormatter hexDumpFormatter) {
        this.textDetector = textDetector;
        this.hexDumpFormatter = hexDumpFormatter;
    }

    /**
     * Processes raw data and creates a DataPacket with formatted display text.
     *
     * @param data      The raw byte array
     * @param direction The direction of data flow
     * @return DataPacket with appropriate formatting
     */
    public DataPacket process(byte[] data, Direction direction) {
        LocalDateTime timestamp = LocalDateTime.now();
        DataType dataType = textDetector.detect(data);

        String displayText;
        if (dataType == DataType.TEXT) {
            displayText = textDetector.decodeText(data);
        } else {
            displayText = hexDumpFormatter.format(data);
        }

        return new DataPacket(timestamp, direction, data, dataType, displayText);
    }

    /**
     * Creates a DataPacket with manually specified data type.
     *
     * @param data      The raw byte array
     * @param direction The direction of data flow
     * @param dataType  The data type to use
     * @return DataPacket with appropriate formatting
     */
    public DataPacket processWithType(byte[] data, Direction direction, DataType dataType) {
        LocalDateTime timestamp = LocalDateTime.now();

        String displayText;
        if (dataType == DataType.TEXT) {
            displayText = textDetector.decodeText(data);
        } else {
            displayText = hexDumpFormatter.format(data);
        }

        return new DataPacket(timestamp, direction, data, dataType, displayText);
    }
}
