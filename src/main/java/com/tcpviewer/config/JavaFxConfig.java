package com.tcpviewer.config;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.net.URL;

/**
 * Configuration for JavaFX-Spring integration.
 * Provides FXMLLoader factory that supports Spring dependency injection in controllers.
 */
@Configuration
public class JavaFxConfig {

    private final ApplicationContext applicationContext;

    public JavaFxConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Creates FXMLLoader instances with Spring-managed controller factory.
     * This allows FXML controllers to be Spring beans with full dependency injection.
     */
    @Bean
    @Scope("prototype")
    public FXMLLoader fxmlLoader() {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(applicationContext::getBean);
        return loader;
    }

    /**
     * Loads an FXML file with Spring dependency injection support.
     *
     * @param fxmlPath Path to the FXML file relative to resources folder
     * @return FXMLLoader with loaded content
     * @throws IOException if FXML file cannot be loaded
     */
    public FXMLLoader loadFxml(String fxmlPath) throws IOException {
        FXMLLoader loader = fxmlLoader();
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("FXML file not found: " + fxmlPath);
        }
        loader.setLocation(resource);
        loader.load();
        return loader;
    }
}
