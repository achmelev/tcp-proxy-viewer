# TCP Proxy Viewer

TCP Proxy Viewer is a desktop application that intercepts TCP connections and displays their contents in real time.
It acts as a man-in-the-middle (MITM) proxy, transparently forwarding traffic between clients and target servers while capturing and visualizing all transmitted data.

Originally designed for plain TCP connections, TCP Proxy Viewer now also supports **SSL/TLS-encrypted connections** by dynamically decrypting and re-encrypting traffic.

The application is useful for debugging, protocol analysis, and inspecting both raw and encrypted TCP communication.

## Features

* Intercepts plain TCP connections as a proxy
* Intercepts SSL/TLS connections using MITM decryption
* Dynamically generates and presents a self-signed server certificate to clients
* Establishes a secure SSL/TLS connection to the target server
* Forwards traffic between client and server without modification
* Decrypts, captures, and displays transmitted data in real time
* Desktop-based user interface
* Distributed as a single executable JAR file

## Requirements

* Java Runtime Environment (JRE) 25 or newer
* Operating system support depends on the Java runtime (Windows, macOS, Linux)

## Installation

* No installation is required
* Download the latest version of the JAR file
* Ensure Java 25 or newer is installed
* Run the application using the command line

## Usage

Start the application using the `-jar` option:

```bash
java -jar tcp-proxy-viewer.jar
```

After starting the application:

1. Configure the listening port in the user interface
2. Configure the target server address and port
3. Select whether the connection should be handled as **plain TCP** or **SSL/TLS**
4. Configure SSL/TLS options if required

Clients should connect to the proxy address instead of directly to the target server.

### Using SSL/TLS Connections

When SSL/TLS mode is enabled:

* The proxy presents a **self-generated server certificate** to the client
* Traffic between the client and the proxy is decrypted
* Traffic between the proxy and the target server is encrypted using a standard SSL/TLS connection
* All decrypted application data is displayed in real time

⚠️ **Important:**  
For clients to accept the proxy certificate without warnings or errors, the generated certificate (or its issuing CA, if applicable) must be trusted by the client system or application.

## How It Works

### Plain TCP Mode

* A client connects to TCP Proxy Viewer rather than the target server
* The application opens a corresponding TCP connection to the target server
* Data is forwarded in both directions
* All transmitted data is captured and displayed in real time

### SSL/TLS Mode

* A client connects to TCP Proxy Viewer using SSL/TLS
* The application generates and presents a self-signed server certificate
* The proxy establishes a separate SSL/TLS connection to the target server
* Encrypted traffic is decrypted, inspected, and re-encrypted transparently
* All application-level data is captured and displayed in real time

This approach allows full visibility into encrypted TCP streams without requiring changes to the client or server, aside from trusting the proxy certificate.

## Use Cases

* Debugging custom TCP-based protocols
* Inspecting SSL/TLS-encrypted application protocols
* Analyzing network communication
* Troubleshooting client/server integrations
* Inspecting raw and encrypted TCP traffic during development

## Limitations and Notes

* The application operates at the TCP level and does not automatically decode higher-level protocols
* SSL/TLS interception requires client trust in the proxy certificate
* Certificate pinning in clients may prevent SSL/TLS interception
* Not suitable for intercepting traffic without proper authorization

## Development

The project is written in Java and packaged as a single JAR file.

Contributions are welcome.
Please open an issue or submit a pull request if you would like to improve the project.

## License

Apache 2.0

## Disclaimer

This software is intended for educational and debugging purposes only.
Intercepting network traffic without authorization may be illegal in some jurisdictions.
Always ensure you have explicit permission before intercepting or inspecting network communications.
