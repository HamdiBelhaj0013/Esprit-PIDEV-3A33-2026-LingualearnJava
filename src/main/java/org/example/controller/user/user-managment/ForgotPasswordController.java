package org.example.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.service.PasswordResetService;

import java.io.IOException;
import java.util.regex.Pattern;

public class ForgotPasswordController {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    @FXML private TextField emailField;
    @FXML private Label     errorLabel;
    @FXML private Label     successLabel;
    @FXML private Button    sendButton;

    @FXML
    private void handleSendCode(ActionEvent event) {
        clearMessages();

        String email = emailField.getText().trim();
        if (email.isBlank()) {
            showError("Email is required.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return;
        }

        sendButton.setDisable(true);
        sendButton.setText("Sending...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                new PasswordResetService().generateAndSendOTP(email);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            sendButton.setDisable(false);
            sendButton.setText("Send Reset Code");
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/VerifyOTP.fxml"));
                Parent root = loader.load();
                VerifyOTPController ctrl = loader.getController();
                ctrl.setEmail(email);
                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException ex) {
                showError("Navigation error: " + ex.getMessage());
            }
        });

        task.setOnFailed(e -> {
            sendButton.setDisable(false);
            sendButton.setText("Send Reset Code");
            Throwable ex = task.getException();
            Platform.runLater(() -> showError(ex != null ? ex.getMessage() : "Failed to send code."));
        });

        new Thread(task).start();
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showError("Could not open login page.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearMessages() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setText("");
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}
