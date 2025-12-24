package com.tcpviewer.error;

/**
 * Category of an error, indicating which component or operation failed.
 * This helps in determining error severity and generating appropriate user messages.
 */
public enum ErrorCategory {
    INITIALIZATION("Application Initialization"),
    PROXY_SERVER("Proxy Server"),
    CONNECTION_HANDLING("Connection Handling"),
    DATA_PROCESSING("Data Processing"),
    UI_OPERATION("User Interface"),
    NETWORK_IO("Network I/O"),
    UNCAUGHT("Uncaught"),
    ;


    private final String displayName;

    ErrorCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the user-friendly display name for this error category.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
