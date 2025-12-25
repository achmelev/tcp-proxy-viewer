package com.tcpviewer.ui.controller;

import com.tcpviewer.config.JavaFxConfig;
import com.tcpviewer.error.ErrorCategory;
import com.tcpviewer.error.ErrorHandlerService;
import com.tcpviewer.model.ConnectionInfo;
import com.tcpviewer.model.DataPacket;
import com.tcpviewer.model.ProxySession;
import com.tcpviewer.proxy.ProxyService;
import com.tcpviewer.ui.error.ErrorDialogService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Controller for the main application window.
 * Manages the connection list, data display, and menu actions.
 */
@Component
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final ProxyService proxyService;
    private final JavaFxConfig javaFxConfig;
    private final ErrorHandlerService errorHandlerService;

    @FXML
    private MenuItem startMenuItem;

    @FXML
    private MenuItem stopMenuItem;

    @FXML
    private ListView<ConnectionInfo> connectionListView;

    @FXML
    private ListView<DataPacket> dataPacketListView;

    @FXML
    private Label statusLabel;

    @FXML
    private Label connectionCountLabel;

    @FXML
    private SplitPane splitPane;

    public MainController(ProxyService proxyService, JavaFxConfig javaFxConfig, ErrorHandlerService errorHandlerService) {
        this.proxyService = proxyService;
        this.javaFxConfig = javaFxConfig;
        this.errorHandlerService = errorHandlerService;
    }

    /**
     * Initializes the controller. Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        this.errorHandlerService.getErrorDialogService().onStart(this);
        logger.info("MainController initialized");

        // Bind connection list to proxy service
        connectionListView.setItems(proxyService.getActiveConnections());

        // Set up connection list cell factory for custom display
        connectionListView.setCellFactory(lv -> new ListCell<ConnectionInfo>() {
            @Override
            protected void updateItem(ConnectionInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (%d bytes)",
                            item.getDisplayName(),
                            item.isActive() ? "ACTIVE" : "CLOSED",
                            item.getTotalBytes()));
                    setStyle(item.isActive() ? "-fx-text-fill: green;" : "-fx-text-fill: gray;");
                }
            }
        });

        // Set up data packet list cell factory
        dataPacketListView.setCellFactory(lv -> new ListCell<DataPacket>() {
            @Override
            protected void updateItem(DataPacket item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String direction = item.getDirection().toString();
                    setText(String.format("[%s] %s:\n%s",
                            item.getTimestamp().toLocalTime(),
                            direction,
                            item.getDisplayText()));
                }
            }
        });

        setupListeners();
    }

    /**
     * Sets up listeners for UI components.
     */
    private void setupListeners() {
        // Connection selection listener
        connectionListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onConnectionSelected(newValue)
        );
    }

    /**
     * Handles connection selection in the list view.
     */
    private void onConnectionSelected(ConnectionInfo connection) {
        if (connection != null) {
            dataPacketListView.setItems(connection.getDataPackets());
            logger.debug("Connection selected: {}", connection.getDisplayName());
        } else {
            dataPacketListView.setItems(null);
        }
    }

    /**
     * Handles File -> Start menu action.
     */
    @FXML
    private void onStartMenuClicked() {
        logger.info("Start menu clicked");
        try {
            // Load start dialog
            FXMLLoader loader = javaFxConfig.loadFxml("/fxml/start-dialog.fxml");
            Parent root = loader.getRoot();
            StartDialogController dialogController = loader.getController();

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Start TCP Proxy");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(statusLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            // Show dialog and wait
            dialogStage.showAndWait();

            // If confirmed, start proxy session
            if (dialogController.isConfirmed()) {
                ProxySession session = new ProxySession(
                        dialogController.getLocalIp(),
                        dialogController.getLocalPort(),
                        dialogController.getTargetHost(),
                        dialogController.getTargetPort(), dialogController.isSSLEnabled(), dialogController.getSSLHost()
                );

                proxyService.startProxySession(session);
                setStatus(String.format("Listening on %s:%d",
                        session.getLocalIp(), session.getLocalPort()));
                setProxyRunning(true);

                logger.info("Proxy session started successfully");
            }
        } catch (IOException e) {
            logger.error("Failed to open start dialog", e);
            errorHandlerService.handleExpectedException(e, ErrorCategory.UI_OPERATION);
        }
    }

    /**
     * Handles File -> Stop menu action.
     */
    @FXML
    private void onStopMenuClicked() {
        logger.info("Stop menu clicked");
        proxyService.stopProxySession();
        setStatus("Idle");
        setProxyRunning(false);
        logger.info("Proxy session stopped");
    }

    /**
     * Handles File -> Exit menu action.
     */
    @FXML
    private void onExitMenuClicked() {
        logger.info("Exit menu clicked");
        doExit(0);
    }

    public void doExit(int returmCode) {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
        System.exit(0);
    }

    /**
     * Handles Help -> About menu action.
     */
    @FXML
    private void onAboutMenuClicked() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("TCP Proxy Viewer");
        alert.setContentText("Version 1.0.0\n\nA TCP proxy application that intercepts and displays network traffic.\n\nBuilt with JavaFX and Spring Boot.");
        alert.showAndWait();
    }

    /**
     * Updates the status label.
     */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Updates the connection count label.
     */
    public void setConnectionCount(int count) {
        connectionCountLabel.setText("Connections: " + count);
    }

    /**
     * Enables/disables menu items based on proxy state.
     */
    public void setProxyRunning(boolean running) {
        startMenuItem.setDisable(running);
        stopMenuItem.setDisable(!running);
    }
}
