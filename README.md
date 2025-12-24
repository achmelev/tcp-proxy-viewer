# TCP Proxy Viewer

TCP Proxy Viewer is a desktop application that intercepts TCP connections and displays their contents in real time.
It acts as a man-in-the-middle (MITM) proxy, transparently forwarding traffic between clients and target servers while capturing and visualizing all transmitted data.
The application is useful for debugging, protocol analysis, and inspecting raw TCP communication.

## Features

* Intercepts TCP connections as a proxy
* Forwards traffic between client and server without modification
* Displays transmitted data in real time
* Desktop-based user interface
* Distributed as a single executable JAR file

## Requirements

* Java Runtime Environment (JRE) 25 or newer
* Operating system support depends on the Java runtime (Windows, macOS, Linux)


## Installation

* No installation is required.
* Download the last version of the jar file
* Ensure Java 25 or newer is installed 
* Run the application using the command line

## Usage

Start the application using the -jar option:

java -jar tcp-proxy-viewer.jar

After starting the application, configure the listening port and the target server in the user interface.
Clients should then connect to the proxy address instead of directly to the target server.

## How It Works

* A client connects to TCP Proxy Viewer rather than the target server
* The application opens a corresponding connection to the target server
* Data is forwarded in both directions
* All transmitted data is captured and displayed in real time

This approach allows full visibility into TCP streams without requiring changes to either the client or the server.

## Use Cases

* Debugging custom TCP-based protocols
* Analyzing network communication
* Troubleshooting client/server integrations
* Inspecting raw TCP traffic during development

## Limitations and Notes

* The application operates at the TCP level and does not automatically decode higher-level protocols
* Encrypted connections (e.g. TLS) will appear as binary data

## Development

The project is written in Java and packaged as a single JAR file.

Contributions are welcome.
Please open an issue or submit a pull request if you would like to improve the project.

## License

Apache 2.0

## Disclaimer

This software is intended for educational and debugging purposes only.
Intercepting network traffic without authorization may be illegal in some jurisdictions.
