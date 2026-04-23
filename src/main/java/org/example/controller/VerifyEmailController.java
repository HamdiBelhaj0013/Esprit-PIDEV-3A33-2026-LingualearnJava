package org.example.controller;

import javafx.animation.PauseTransition;
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
import javafx.util.Duration;
import org.example.service.EmailVerificationService;

import java.io.IOException;

public class VerifyEmailController {

    @FXML private Label     subtitleLabel;
    @FXML private TextField codeField;
    @FXML private Label     codeError;
    @FXML private Label     successLabel;
    @FXML private Button    resendBtn;

    private String email;
    private String mode; // "REGISTRATION" or "LOGIN"

    // ── Called by the previous controller before the scene is shown ───────────

    public void setEmail(String email) {
        this.email = email;
        subtitleLabel.setText("We sent a 6-digit code to " + email);
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    // ── Verify the entered code ───────────────────────────────────────────────

    @FXML
    private void handleVerify(ActionEvent event) {
        String entered = codeField.getText().trim();

        if (entered.isBlank() || !entered.matches("\\d{6}")) {
            showError("Please enter the 6-digit code sent to your email.");
            return;
        }

        clearMessages();

        EmailVerificationService svc = new EmailVerificationService();

        if (svc.verifyCode(email, entered)) {
            svc.markVerified(email);
            showSuccess("Email verified successfully!");

            PauseTransition pause = new PauseTransition(Duration.millis(1500));
            pause.setOnFinished(e -> navigateToLogin());
            pause.play();
        } else {
            showError("Invalid or expired code. Try again.");
        }
    }

    // ── Resend a fresh code ───────────────────────────────────────────────────

    @FXML
    private void handleResend(ActionEvent event) {
        resendBtn.setDisable(true);
        resendBtn.setText("Sending...");
        clearMessages();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                new EmailVerificationService().generateAndSendCode(email);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            resendBtn.setDisable(false);
            resendBtn.setText("Resend code");
            showSuccess("New code sent to " + email);
            codeField.clear();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            resendBtn.setDisable(false);
            resendBtn.setText("Resend code");
            showError("Could not send: " + task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    // ── Back to login ─────────────────────────────────────────────────────────

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        navigateToLogin();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showError("Navigation error: " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        codeError.setVisible(false);
        codeError.setManaged(false);
    }

    private void showError(String message) {
        codeError.setText(message);
        codeError.setVisible(true);
        codeError.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void clearMessages() {
        codeError.setVisible(false);
        codeError.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}
