package com.tcpviewer.ui;

import com.tcpviewer.TCPViewerApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX Application wrapper that integrates with Spring Boot.
 * This class creates the Spring application context and publishes a StageReadyEvent
 * when the JavaFX stage is ready.
 */
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        this.context = new SpringApplicationBuilder(TCPViewerApplication.class)
                .run(getParameters().getRaw().toArray(new String[0]));
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
