# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TCP Proxy Viewer is a desktop application that intercepts TCP connections and displays their contents in real-time. It acts as a man-in-the-middle proxy, forwarding traffic between clients and target servers while capturing and visualizing all transmitted data.

## Tech Stack

- **Language**: Java 17
- **Build Tool**: Maven
- **Frameworks**:
  - Spring Boot 3.2.0 (dependency injection and application framework)
  - JavaFX 21.0.1 (GUI)
- **Architecture**: Spring Boot + JavaFX hybrid application

## Build and Run Commands

```bash
# Compile the project
mvn clean compile

# Run tests
mvn clean test

# Package the application (creates JAR in target/)
mvn clean package

# Run the application
mvn spring-boot:run
# OR using JavaFX plugin
mvn javafx:run
```

##Unit Tests Guidelines

*  Write unit tests for all new classes except for:  Model classes (package com.tcpviewer.model), UI classes (package com.tcpviewer.ui) and and wrapper classes (all **.wrapper packages)
* Always extend unit tests when you change a class. 
*  Always modify unit tests when the behaviour of a class changes.
* Run unit tests after every change
* When a unit test fails after a change, always verify at first, whether the reason is the change. Only change the test if you are absolutely sure the failure reason is in the test itself.
* Follow the following guidelines, when writing unit test:
  * Use Mockito as unit testing framework
  * Write one unit test class for every application class
  * Place unit test classes in the same package as  the the tested class
  * Unit test class should only test the function of one class. So use mocks for the referenced classes.
  * Only implement mocks for the classes from the codebase. If the class to test uses some class from the JDK or an external library, implement a wrapper.
  * Use test stubs instead of Mockito mocks (JDK 25 compatibility issue)

## Architecture Overview

### Application Bootstrap

The application uses a hybrid Spring Boot + JavaFX architecture:

1. **Entry Point**: `TCPViewerApplication.main()` launches `JavaFxApplication`
2. **Spring Context**: `JavaFxApplication.init()` creates Spring Boot context via `SpringApplicationBuilder`
3. **JavaFX Stage**: When JavaFX stage is ready, `JavaFxApplication` publishes a `StageReadyEvent`
4. **UI Initialization**: `StageInitializer` listens for `StageReadyEvent` and configures the primary stage with FXML

This integration allows JavaFX controllers to be Spring beans with dependency injection.

### Core Components

**Proxy Layer** (`com.tcpviewer.proxy`):
- `ProxyServer`: Accepts incoming client connections on a local port
- `ProxyConnectionHandler`: Manages individual client-server proxy connections
- `TcpForwarder`: Handles unidirectional data streaming (client→server or server→client) with capture capability
- Each connection uses two `TcpForwarder` instances for bidirectional forwarding

**Service Layer** (`com.tcpviewer.service`):
- `ProxyService`: Main orchestrator, coordinates ProxyServerManager, ConnectionManager, and DataProcessor
- `ProxyServerManager`: Manages proxy server lifecycle (start/stop)
- `ConnectionManager`: Thread-safe registry of active connections with JavaFX UI integration

**Data Flow**:
1. User configures proxy via start dialog (local IP/port, target host/port)
2. `ProxyService.startProxySession()` creates a `ProxyServer` that binds to local port
3. When a client connects, `ProxyConnectionHandler` establishes connection to target server
4. Two `TcpForwarder` instances relay data bidirectionally while calling `DataCaptureListener.onDataCaptured()`
5. `ProxyService` implements `DataCaptureListener`, processes captured data via `DataProcessor`, and stores in `ConnectionManager`
6. `ConnectionManager` uses `Platform.runLater()` to update JavaFX UI on correct thread

**UI Layer** (`com.tcpviewer.ui`):
- `MainController`: Main window with connection list (left pane) and packet view (right pane)
- `StartDialogController`: Modal dialog for proxy configuration
- Controllers are Spring beans injected with services
- Uses JavaFX ObservableList for reactive UI updates

**Model** (`com.tcpviewer.model`):
- `ProxySession`: Proxy configuration (local/target IP and ports)
- `ConnectionInfo`: Per-connection metadata with ObservableList of DataPackets
- `DataPacket`: Individual captured data packet with timestamp, direction, and display text
- `Direction`: Enum (CLIENT_TO_SERVER, SERVER_TO_CLIENT)
- `DataType`: Enum (TEXT, BINARY)

**Utilities** (`com.tcpviewer.util`):
- `DataProcessor`: Detects data type (text/binary) and formats for display
- `TextDetector`: Heuristic text vs binary detection
- `HexDumpFormatter`: Formats binary data as hex dump

**Error Handling** (`com.tcpviewer.error`):
- `ErrorHandlerService`: Handles exceptions
- `ErrorClassifier`: Heuristically classifies exceptions
- `GlobalExceptionHandler`: Sets up global exception handlers

### Threading Model

- Proxy server runs in dedicated thread managed by `ProxyServerManager`
- Each connection handler runs in thread pool executor
- Each `TcpForwarder` runs in its own thread (two per connection)
- UI updates are synchronized to JavaFX Application Thread via `Platform.runLater()`
- `ConnectionManager` uses `ConcurrentHashMap` for thread-safe connection registry

## Configuration

- `src/main/resources/application.properties`: Spring Boot configuration
- FXML files in `src/main/resources/fxml/`: UI layouts
- CSS in `src/main/resources/css/style.css`

## Project Structure

- `com.tcpviewer.config`: Spring and JavaFX configuration beans
- `com.tcpviewer.proxy`: Low-level TCP proxy implementation
- `com.tcpviewer.service`: Business logic layer
- `com.tcpviewer.ui`: JavaFX UI components and controllers
- `com.tcpviewer.model`: Data models
- `com.tcpviewer.util`: Utility classes for data processing
