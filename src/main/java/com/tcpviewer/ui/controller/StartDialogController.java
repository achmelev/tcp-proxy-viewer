package com.tcpviewer.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Controller for the Start Proxy dialog.
 * Handles user input for proxy configuration.
 */
@Component
public class StartDialogController {

    private static final Logger logger = LoggerFactory.getLogger(StartDialogController.class);

    @FXML
    private TextField localIpField;

    @FXML
    private TextField localPortField;

    @FXML
    private TextField targetHostField;

    @FXML
    private TextField targetPortField;

    @FXML
    private CheckBox sslEnabled;

    @FXML
    private  Label sslHostLabel;

    @FXML
    private  TextField sslHost;

    @FXML
    private Label validationLabel;


    private boolean confirmed = false;

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        validationLabel.managedProperty().bind(validationLabel.visibleProperty());
        sslHost.managedProperty().bind(sslHost.visibleProperty());
        sslHostLabel.managedProperty().bind(sslHostLabel.visibleProperty());
        logger.info("StartDialogController initialized");
    }

    @FXML
    private void onSSLCheckBoxClicked() {
        sslHostLabel.setVisible(sslEnabled.isSelected());
        sslHost.setVisible(sslEnabled.isSelected());
        resizeDialog();
    }

    /**
     * Handles OK button click.
     */
    @FXML
    private void onOkClicked() {
        if (validateInput()) {
            confirmed = true;
            closeDialog();
            logger.info("Start dialog confirmed");
        }
    }

    /**
     * Handles Cancel button click.
     */
    @FXML
    private void onCancelClicked() {
        confirmed = false;
        closeDialog();
        logger.info("Start dialog cancelled");
    }

    /**
     * Validates user input.
     */
    private boolean validateInput() {
        validationLabel.setVisible(false);

        String localIp = localIpField.getText().trim();
        String localPort = localPortField.getText().trim();
        String targetHost = targetHostField.getText().trim();
        String targetPort = targetPortField.getText().trim();
        String sslHostValue = sslHost.getText().trim();

        if (localIp.isEmpty()) {
            showValidationError("Local IP cannot be empty");
            return false;
        }

        if (localPort.isEmpty()) {
            showValidationError("Local Port cannot be empty");
            return false;
        }

        try {
            int port = Integer.parseInt(localPort);
            if (port < 1 || port > 65535) {
                showValidationError("Local Port must be between 1 and 65535");
                return false;
            }
        } catch (NumberFormatException e) {
            showValidationError("Local Port must be a valid number");
            return false;
        }

        if (targetHost.isEmpty()) {
            showValidationError("Target Host cannot be empty");
            return false;
        }

        if (targetPort.isEmpty()) {
            showValidationError("Target Port cannot be empty");
            return false;
        }

        if  (sslEnabled.isSelected()) {
            if (sslHostValue.isEmpty()) {
                showValidationError("SSL host name cannot be empty");
                return false;
            }
        }

        try {
            int port = Integer.parseInt(targetPort);
            if (port < 1 || port > 65535) {
                showValidationError("Target Port must be between 1 and 65535");
                return false;
            }
        } catch (NumberFormatException e) {
            showValidationError("Target Port must be a valid number");
            return false;
        }

        return true;
    }

    /**
     * Shows validation error message.
     */
    private void showValidationError(String message) {
        validationLabel.setText(message);
        validationLabel.setVisible(true);
        resizeDialog();
    }

    /**
     * Closes the dialog.
     */
    private void closeDialog() {
        Stage stage = (Stage) localIpField.getScene().getWindow();
        stage.close();
    }

    private void resizeDialog() {
        Platform.runLater(() -> {
            Stage stage = (Stage) localIpField.getScene().getWindow();
            stage.sizeToScene();
        });
    }

    /**
     * Returns whether the dialog was confirmed.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns the local IP value.
     */
    public String getLocalIp() {
        return localIpField.getText().trim();
    }

    /**
     * Returns the local port value.
     */
    public int getLocalPort() {
        return Integer.parseInt(localPortField.getText().trim());
    }

    /**
     * Returns the target host value.
     */
    public String getTargetHost() {
        return targetHostField.getText().trim();
    }

    /**
     * Returns the target port value.
     */
    public int getTargetPort() {
        return Integer.parseInt(targetPortField.getText().trim());
    }

    /**
     * Returns true, if SSL ist to used
     */
    public boolean isSSLEnabled() {
        return sslEnabled.isSelected();
    }

    /**
     * Returns true, if SSL ist to used
     */
    public String getSSLHost() {
        if (!isSSLEnabled()) {
            return null;
        } else {
            return sslHost.getText().trim();
        }
    }


}
