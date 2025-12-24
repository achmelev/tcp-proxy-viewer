package com.tcpviewer.ui;

import com.tcpviewer.TCPViewerApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * JavaFX Application wrapper that integrates with Spring Boot.
 * This class creates the Spring application context and publishes a StageReadyEvent
 * when the JavaFX stage is ready.
 */
public class JavaFxApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(JavaFxApplication.class);

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        try {
            this.context = new SpringApplicationBuilder(TCPViewerApplication.class)
                    .run(getParameters().getRaw().toArray(new String[0]));
        } catch (Exception e) {
            logger.error("Failed to initialize Spring application context", e);
            // Can't use ErrorHandlerService since Spring context failed to create
            // Show error dialog manually and exit
            Platform.runLater(() -> showFatalErrorAndExit(e));
            throw e; // Re-throw to stop initialization
        }
    }

    @Override
    public void start(Stage primaryStage) {
        context.publishEvent(new StageReadyEvent(this, primaryStage));
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }

    /**
     * Shows a fatal error dialog and exits the application.
     * Used when Spring context fails to initialize.
     */
    private void showFatalErrorAndExit(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fatal Error");
        alert.setHeaderText("Application Initialization Error");
        alert.setContentText("Failed to initialize the application. The application cannot continue and will now close.\n\n" +
                             "Error: " + e.getMessage());

        // Add expandable stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setExpandableContent(textArea);
        alert.showAndWait();

        // Exit the application
        System.exit(1);
    }

    /**
     * Custom event published when the JavaFX Stage is ready to be configured.
     */
    public static class StageReadyEvent extends ApplicationEvent {
        private final Stage stage;

        public StageReadyEvent(Object source, Stage stage) {
            super(source);
            this.stage = stage;
        }

        public Stage getStage() {
            return stage;
        }
    }
}
