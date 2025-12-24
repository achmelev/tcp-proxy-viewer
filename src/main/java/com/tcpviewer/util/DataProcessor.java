package com.tcpviewer.util;

import com.tcpviewer.model.DataPacket;
import com.tcpviewer.model.DataType;
import com.tcpviewer.model.Direction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Processes raw byte data and creates formatted DataPacket objects.
 * All data is converted to text using ISO-8859-1 encoding with control character replacement.
 */
@Component
public class DataProcessor {

    private final TextFormatter textFormatter;

    public DataProcessor(TextFormatter textFormatter) {
        this.textFormatter = textFormatter;
    }

    /**
     * Processes raw data and creates a DataPacket with ISO-8859-1 text formatting.
     * All data is displayed as text with control characters replaced.
     *
     * @param data      The raw byte array
     * @param direction The direction of data flow
     * @return DataPacket with text formatting
     */
    public DataPacket process(byte[] data, Direction direction) {
        LocalDateTime timestamp = LocalDateTime.now();
        String displayText = textFormatter.convertToDisplayText(data);
        return new DataPacket(timestamp, direction, data, DataType.TEXT, displayText);
    }

    /**
     * Creates a DataPacket with manually specified data type.
     * Note: BINARY type is deprecated; all data is now displayed as text.
     *
     * @param data      The raw byte array
     * @param direction The direction of data flow
     * @param dataType  The data type (ignored, always uses TEXT)
     * @return DataPacket with text formatting
     * @deprecated DataType parameter is ignored; use {@link #process(byte[], Direction)} instead
     */
    @Deprecated
    public DataPacket processWithType(byte[] data, Direction direction, DataType dataType) {
        // Always process as text regardless of dataType parameter
        return process(data, direction);
    }
}
