package com.tcpviewer.model;

/**
 * Enum representing the direction of data flow in a TCP connection.
 */
public enum Direction {
    /**
     * Data flowing from client to target server
     */
    CLIENT_TO_SERVER,

    /**
     * Data flowing from target server to client
     */
    SERVER_TO_CLIENT
}
