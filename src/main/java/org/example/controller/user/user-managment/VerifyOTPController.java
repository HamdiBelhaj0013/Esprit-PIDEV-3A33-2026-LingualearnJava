package org.example.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.service.PasswordResetService;

import java.io.IOException;

public class VerifyOTPController {

    @FXML private TextField otpField;
    @FXML private Label     errorLabel;
    @FXML private Label     subtitleLabel;
    @FXML private Label     successLabel;

    private String email;

    public void setEmail(String email) {
        this.email = email;
        subtitleLabel.setText("We sent a 6-digit code to " + email);
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        clearMessages();

        String otp = otpField.getText().trim();
        if (otp.isBlank()) {
            showError("Please enter the 6-digit code.");
            return;
        }
        if (!otp.matches("\\d{6}")) {
            showError("Code must be exactly 6 digits.");
            return;
        }

        boolean valid = new PasswordResetService().verifyOTP(email, otp);
        if (!valid) {
            showError("Invalid or expired code. Please try again.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/NewPassword.fxml"));
            Parent root = loader.load();
            NewPasswordController ctrl = loader.getController();
            ctrl.setEmail(email);
            ctrl.setOtp(otp);
            Stage stage = (Stage) otpField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showError("Navigation error: " + e.getMessage());
        }
    }

    @FXML
    private void handleResend(ActionEvent event) {
        clearMessages();
        otpField.clear();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                new PasswordResetService().generateAndSendOTP(email);
                return null;
            }
        };

        task.setOnSucceeded(e ->
            Platform.runLater(() -> {
                successLabel.setText("Code resent successfully.");
                successLabel.setVisible(true);
                successLabel.setManaged(true);
            })
        );

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> showError(ex != null ? ex.getMessage() : "Failed to resend code."));
        });

        new Thread(task).start();
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) otpField.getScene().getWindow();
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
