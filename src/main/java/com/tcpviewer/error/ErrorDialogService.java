package com.tcpviewer.error;

import com.tcpviewer.javafx.wrapper.PlatformWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Service responsible for displaying error dialogs to users.
 * Shows user-friendly error messages with expandable technical details.
 * All dialogs are shown on the JavaFX Application Thread for thread safety.
 */
@Service
public class ErrorDialogService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlatformWrapper platformWrapper;

    public ErrorDialogService(PlatformWrapper platformWrapper) {
        if (platformWrapper == null) {
            throw new NullPointerException("platformWrapper cannot be null");
        }
        this.platformWrapper = platformWrapper;
    }

    /**
     * Shows an error dialog based on the error context.
     * Dispatches to fatal or recoverable dialog based on severity.
     *
     * @param errorContext the error context containing all error information
     */
    public void showErrorDialog(ErrorContext errorContext) {
        if (errorContext.getSeverity() == ErrorSeverity.FATAL) {
            showFatalErrorDialog(errorContext);
        } else {
            showRecoverableErrorDialog(errorContext);
        }
    }

    /**
     * Shows a fatal error dialog (red alert style).
     * User is informed that the application will close.
     *
     * @param errorContext the error context
     */
    public void showFatalErrorDialog(ErrorContext errorContext) {
        platformWrapper.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fatal Error");
            alert.setHeaderText(errorContext.getCategory().getDisplayName() + " Error");
            alert.setContentText(errorContext.getUserMessage() + "\n\nThe application will now close.");

            // Add expandable technical details
            addExpandableDetails(alert, errorContext);

            // Customize button
            alert.getButtonTypes().setAll(ButtonType.OK);

            alert.showAndWait();
        });
    }

    /**
     * Shows a recoverable error dialog (warning style).
     * User can acknowledge and continue using the application.
     *
     * @param errorContext the error context
     */
    public void showRecoverableErrorDialog(ErrorContext errorContext) {
        platformWrapper.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Error");
            alert.setHeaderText(errorContext.getCategory().getDisplayName() + " Error");
            alert.setContentText(errorContext.getUserMessage());

            // Add expandable technical details
            addExpandableDetails(alert, errorContext);

            // Customize button
            alert.getButtonTypes().setAll(ButtonType.OK);

            alert.showAndWait();
        });
    }

    /**
     * Adds expandable technical details section to the alert dialog.
     *
     * @param alert the alert dialog
     * @param errorContext the error context with technical details
     */
    private void addExpandableDetails(Alert alert, ErrorContext errorContext) {
        // Create expandable content
        Label label = new Label("Technical Details:");

        // Create text area with technical details
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        // Format technical details with timestamp
        StringBuilder details = new StringBuilder();
        details.append("Timestamp: ")
               .append(errorContext.getTimestamp().format(TIMESTAMP_FORMATTER))
               .append("\n");
        details.append("Severity: ").append(errorContext.getSeverity()).append("\n");
        details.append("Category: ").append(errorContext.getCategory().getDisplayName()).append("\n\n");
        details.append(errorContext.getTechnicalDetails());

        // Add additional context if present
        if (!errorContext.getAdditionalContext().isEmpty()) {
            details.append("\n\nAdditional Context:\n");
            errorContext.getAdditionalContext().forEach((key, value) ->
                details.append(key).append(": ").append(value).append("\n"));
        }

        textArea.setText(details.toString());

        // Create expandable content container
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable content
        alert.getDialogPane().setExpandableContent(expContent);

        // Set preferred size for the dialog
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(400);
    }
}
