package com.tcpviewer.model;

import java.time.LocalDateTime;

/**
 * Represents the configuration for a proxy session.
 */
public class ProxySession {
    private final String localIp;
    private final int localPort;
    private final String targetHost;
    private final int targetPort;
    private final LocalDateTime startTime;
    private boolean active;

    public ProxySession(String localIp, int localPort, String targetHost, int targetPort) {
        this.localIp = localIp;
        this.localPort = localPort;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.startTime = LocalDateTime.now();
        this.active = false;
    }

    public String getLocalIp() {
        return localIp;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDisplayInfo() {
        return String.format("%s:%d → %s:%d",
                localIp, localPort, targetHost, targetPort);
    }

    @Override
    public String toString() {
        return String.format("ProxySession[%s - %s]",
                getDisplayInfo(),
                active ? "ACTIVE" : "STOPPED");
    }
}
