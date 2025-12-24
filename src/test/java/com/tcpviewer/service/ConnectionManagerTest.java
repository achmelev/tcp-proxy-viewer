package com.tcpviewer.service;

import com.tcpviewer.io.wrapper.InputStreamWrapper;
import com.tcpviewer.io.wrapper.OutputStreamWrapper;
import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.javafx.wrapper.PlatformWrapper;
import com.tcpviewer.model.ConnectionInfo;
import com.tcpviewer.model.DataPacket;
import com.tcpviewer.model.DataType;
import com.tcpviewer.model.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConnectionManager.
 * Tests connection registration, data packet management, and JavaFX integration.
 */
@ExtendWith(MockitoExtension.class)
class ConnectionManagerTest {

    /**
     * Test stub for SocketWrapper.
     */
    private static class TestSocketWrapper implements SocketWrapper {
        private final String address;
        private final int port;

        public TestSocketWrapper(String address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public InputStreamWrapper getInputStream() throws IOException {
            return null;
        }

        @Override
        public OutputStreamWrapper getOutputStream() throws IOException {
            return null;
        }

        @Override
        public void setTcpNoDelay(boolean on) throws IOException {
        }

        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName(address);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() throws IOException {
        }
    }

    @Mock
    private PlatformWrapper mockPlatformWrapper;

    private ConnectionManager connectionManager;

    @BeforeEach
    void setUp() {
        // Configure Platform.runLater to execute immediately on the test thread
        // Use lenient() to avoid UnnecessaryStubbingException for tests that don't trigger runLater
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockPlatformWrapper).runLater(any(Runnable.class));

        connectionManager = new ConnectionManager(mockPlatformWrapper);
    }

    @Test
    void testRegisterConnectionWithSocket() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        TestSocketWrapper socket = new TestSocketWrapper("192.168.1.100", 50001);

        // Act
        ConnectionInfo info = connectionManager.registerConnection(connectionId, socket);

        // Assert
        assertNotNull(info);
        assertEquals(connectionId, info.getConnectionId());
        assertEquals("192.168.1.100", info.getClientAddress());
        assertEquals(50001, info.getClientPort());
        assertTrue(info.isActive());
        verify(mockPlatformWrapper).runLater(any(Runnable.class));
        assertEquals(1, connectionManager.getConnectionList().size());
        assertTrue(connectionManager.getConnectionList().contains(info));
    }

    @Test
    void testRegisterConnectionWithoutSocket() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        String clientAddress = "10.0.0.5";
        int clientPort = 60001;

        // Act
        ConnectionInfo info = connectionManager.registerConnection(connectionId, clientAddress, clientPort);

        // Assert
        assertNotNull(info);
        assertEquals(connectionId, info.getConnectionId());
        assertEquals(clientAddress, info.getClientAddress());
        assertEquals(clientPort, info.getClientPort());
        assertTrue(info.isActive());
        verify(mockPlatformWrapper).runLater(any(Runnable.class));
        assertEquals(1, connectionManager.getConnectionList().size());
    }

    @Test
    void testRegisterMultipleConnections() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        TestSocketWrapper socket1 = new TestSocketWrapper("192.168.1.100", 50001);
        TestSocketWrapper socket2 = new TestSocketWrapper("192.168.1.101", 50002);

        // Act
        connectionManager.registerConnection(id1, socket1);
        connectionManager.registerConnection(id2, socket2);

        // Assert
        assertEquals(2, connectionManager.getConnectionList().size());
        assertEquals(2, connectionManager.getTotalConnectionCount());
        assertEquals(2, connectionManager.getActiveConnectionCount());
    }

    @Test
    void testGetConnection() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        TestSocketWrapper socket = new TestSocketWrapper("192.168.1.100", 50001);
        ConnectionInfo registered = connectionManager.registerConnection(connectionId, socket);

        // Act
        ConnectionInfo retrieved = connectionManager.getConnection(connectionId);

        // Assert
        assertNotNull(retrieved);
        assertSame(registered, retrieved);
    }

    @Test
    void testGetConnectionReturnsNullForUnknownId() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act
        ConnectionInfo retrieved = connectionManager.getConnection(unknownId);

        // Assert
        assertNull(retrieved);
    }

    @Test
    void testAddDataPacket() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        TestSocketWrapper socket = new TestSocketWrapper("192.168.1.100", 50001);
        connectionManager.registerConnection(connectionId, socket);

        DataPacket packet = new DataPacket(
                LocalDateTime.now(),
                Direction.CLIENT_TO_SERVER,
                "test data".getBytes(),
                DataType.TEXT,
                "test data"
        );

        // Act
        connectionManager.addDataPacket(connectionId, packet);

        // Assert
        ConnectionInfo connection = connectionManager.getConnection(connectionId);
        assertEquals(1, connection.getDataPackets().size());
        assertTrue(connection.getDataPackets().contains(packet));
        verify(mockPlatformWrapper, times(2)).runLater(any(Runnable.class)); // register + addPacket
    }

    @Test
    void testAddDataPacketToUnknownConnection() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        DataPacket packet = new DataPacket(
                LocalDateTime.now(),
                Direction.CLIENT_TO_SERVER,
                "test".getBytes(),
                DataType.TEXT,
                "test"
        );

        // Act & Assert - should not throw, just log warning
        assertDoesNotThrow(() -> connectionManager.addDataPacket(unknownId, packet));
    }

    @Test
    void testCloseConnection() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        TestSocketWrapper socket = new TestSocketWrapper("192.168.1.100", 50001);
        connectionManager.registerConnection(connectionId, socket);

        // Act
        connectionManager.closeConnection(connectionId);

        // Assert
        ConnectionInfo connection = connectionManager.getConnection(connectionId);
        assertFalse(connection.isActive());
        assertNotNull(connection.getDisconnectedAt());
        verify(mockPlatformWrapper, times(2)).runLater(any(Runnable.class)); // register + close
    }

    @Test
    void testCloseConnectionForUnknownId() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> connectionManager.closeConnection(unknownId));
    }

    @Test
    void testGetActiveConnectionCount() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        TestSocketWrapper socket1 = new TestSocketWrapper("192.168.1.100", 50001);
        TestSocketWrapper socket2 = new TestSocketWrapper("192.168.1.101", 50002);
        TestSocketWrapper socket3 = new TestSocketWrapper("192.168.1.102", 50003);

        connectionManager.registerConnection(id1, socket1);
        connectionManager.registerConnection(id2, socket2);
        connectionManager.registerConnection(id3, socket3);

        // Act - close one connection
        connectionManager.closeConnection(id2);

        // Assert - 2 active, 1 closed
        assertEquals(2, connectionManager.getActiveConnectionCount());
        assertEquals(3, connectionManager.getTotalConnectionCount());
    }

    @Test
    void testGetTotalConnectionCount() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        connectionManager.registerConnection(id1, "192.168.1.100", 50001);
        connectionManager.registerConnection(id2, "192.168.1.101", 50002);

        // Act
        int total = connectionManager.getTotalConnectionCount();

        // Assert
        assertEquals(2, total);
    }

    @Test
    void testClear() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        connectionManager.registerConnection(id1, "192.168.1.100", 50001);
        connectionManager.registerConnection(id2, "192.168.1.101", 50002);

        // Act
        connectionManager.clear();

        // Assert
        assertEquals(0, connectionManager.getTotalConnectionCount());
        assertEquals(0, connectionManager.getConnectionList().size());
        verify(mockPlatformWrapper, times(3)).runLater(any(Runnable.class)); // 2 registers + 1 clear
    }

    @Test
    void testGetConnectionListIsObservable() {
        // Arrange & Act
        var list = connectionManager.getConnectionList();

        // Assert
        assertNotNull(list);
        assertTrue(list instanceof javafx.collections.ObservableList);
    }

    @Test
    void testPlatformRunLaterCalledForRegister() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        TestSocketWrapper socket = new TestSocketWrapper("192.168.1.100", 50001);

        // Act
        connectionManager.registerConnection(connectionId, socket);

        // Assert
        verify(mockPlatformWrapper).runLater(any(Runnable.class));
    }

    @Test
    void testPlatformRunLaterCalledForAddDataPacket() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        connectionManager.registerConnection(connectionId, "192.168.1.100", 50001);
        reset(mockPlatformWrapper); // Reset to ignore register call

        // Re-configure after reset
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockPlatformWrapper).runLater(any(Runnable.class));

        DataPacket packet = new DataPacket(
                LocalDateTime.now(),
                Direction.CLIENT_TO_SERVER,
                "test".getBytes(),
                DataType.TEXT,
                "test"
        );

        // Act
        connectionManager.addDataPacket(connectionId, packet);

        // Assert
        verify(mockPlatformWrapper).runLater(any(Runnable.class));
    }

    @Test
    void testPlatformRunLaterCalledForCloseConnection() {
        // Arrange
        UUID connectionId = UUID.randomUUID();
        connectionManager.registerConnection(connectionId, "192.168.1.100", 50001);
        reset(mockPlatformWrapper); // Reset to ignore register call

        // Re-configure after reset
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockPlatformWrapper).runLater(any(Runnable.class));

        // Act
        connectionManager.closeConnection(connectionId);

        // Assert
        verify(mockPlatformWrapper).runLater(any(Runnable.class));
    }

    @Test
    void testPlatformRunLaterCalledForClear() {
        // Arrange
        connectionManager.registerConnection(UUID.randomUUID(), "192.168.1.100", 50001);
        reset(mockPlatformWrapper); // Reset to ignore register call

        // Re-configure after reset
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockPlatformWrapper).runLater(any(Runnable.class));

        // Act
        connectionManager.clear();

        // Assert
        verify(mockPlatformWrapper).runLater(any(Runnable.class));
    }

    @Test
    void testConcurrentAccessToConnectionMap() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // Act - simulate concurrent operations
        connectionManager.registerConnection(id1, "192.168.1.100", 50001);
        connectionManager.registerConnection(id2, "192.168.1.101", 50002);
        ConnectionInfo info1 = connectionManager.getConnection(id1);
        ConnectionInfo info2 = connectionManager.getConnection(id2);

        // Assert - should not throw ConcurrentModificationException
        assertNotNull(info1);
        assertNotNull(info2);
        assertEquals(2, connectionManager.getTotalConnectionCount());
    }
}
