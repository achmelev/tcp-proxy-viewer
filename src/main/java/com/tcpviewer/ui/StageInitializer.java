package com.tcpviewer.ui;

import com.tcpviewer.config.JavaFxConfig;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Initializes the JavaFX Stage when the application is ready.
 * Listens for StageReadyEvent and sets up the main window.
 */
@Component
public class StageInitializer implements ApplicationListener<JavaFxApplication.StageReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StageInitializer.class);
    private static final String MAIN_VIEW_FXML = "/fxml/main-view.fxml";
    private static final String STYLESHEET = "/css/style.css";

    private final JavaFxConfig javaFxConfig;

    public StageInitializer(JavaFxConfig javaFxConfig) {
        this.javaFxConfig = javaFxConfig;
    }

    @Override
    public void onApplicationEvent(JavaFxApplication.StageReadyEvent event) {
        try {
            Stage stage = event.getStage();

            FXMLLoader loader = javaFxConfig.loadFxml(MAIN_VIEW_FXML);
            Parent root = loader.getRoot();

            Scene scene = new Scene(root, 1000, 600);

            String stylesheetUrl = getClass().getResource(STYLESHEET).toExternalForm();
            scene.getStylesheets().add(stylesheetUrl);

            stage.setTitle("TCP Proxy Viewer");
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(500);
            stage.show();

            logger.info("Application window initialized successfully");
        } catch (IOException e) {
            logger.error("Failed to load main view", e);
            throw new RuntimeException("Failed to initialize application window", e);
        }
    }
}
