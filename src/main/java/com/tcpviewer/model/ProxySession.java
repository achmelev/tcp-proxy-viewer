package com.tcpviewer.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents the configuration for a proxy session.
 */
public class ProxySession {
    private final String localIp;
    private final int localPort;
    private final String targetHost;
    private final int targetPort;
    private final LocalDateTime startTime;
    private final boolean ssl;
    private final String sslHostName;
    private boolean active;

    public ProxySession(String localIp, int localPort, String targetHost, int targetPort, boolean ssl, String sslHostName) {
        this.localIp = localIp;
        this.localPort = localPort;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.startTime = LocalDateTime.now();
        this.ssl = ssl;
        this.sslHostName = sslHostName;
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

    public boolean isSsl() {
        return ssl;
    }

    public String getSslHostName() {
        return sslHostName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDisplayInfo() {
        if (!ssl) {
            return String.format("Plain %s:%d → %s:%d",
                    localIp, localPort, targetHost, targetPort);
        } else {
            return String.format("SSL %s:%d → %s(%s):%d",
                    localIp, localPort, sslHostName, targetHost, targetPort);
        }
    }

    @Override
    public String toString() {
        return String.format("ProxySession[%s - %s]",
                getDisplayInfo(),
                active ? "ACTIVE" : "STOPPED");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProxySession that = (ProxySession) o;
        return localPort == that.localPort && targetPort == that.targetPort && ssl == that.ssl && active == that.active && Objects.equals(localIp, that.localIp) && Objects.equals(targetHost, that.targetHost) && Objects.equals(startTime, that.startTime) && Objects.equals(sslHostName, that.sslHostName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localIp, localPort, targetHost, targetPort, startTime, ssl, sslHostName, active);
    }
}
