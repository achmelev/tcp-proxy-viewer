package com.tcpviewer;

import com.tcpviewer.ui.JavaFxApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the TCP Proxy Viewer application.
 * This class bootstraps both Spring Boot and JavaFX.
 */
@SpringBootApplication
public class TCPViewerApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
