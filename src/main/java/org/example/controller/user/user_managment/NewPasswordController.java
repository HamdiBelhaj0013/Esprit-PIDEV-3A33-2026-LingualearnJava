package org.example.controller.user.user_managment;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.service.CaptchaServer;
import org.example.service.HCaptchaService;
import org.example.service.PasswordResetService;

import java.io.IOException;

public class NewPasswordController {

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         passwordError;
    @FXML private Label         confirmError;
    @FXML private Label         errorLabel;
    @FXML private WebView       captchaWebView;
    @FXML private Label         captchaError;

    private String email;
    private String otp;

    private final HCaptchaService captchaService = new HCaptchaService();
    private WebEngine webEngine;

    @FXML
    public void initialize() {
        webEngine = captchaWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Suppress JS console errors in production (optional — remove to see JS logs)
        webEngine.setOnError(event ->
                System.err.println("[WebView Error] " + event.getMessage()));

        try {
            String url = CaptchaServer.start();
            System.out.println("[NewPasswordController] Loading captcha from: " + url);
            webEngine.load(url);
        } catch (Exception e) {
            System.err.println("[NewPasswordController] Failed to start CaptchaServer: " + e.getMessage());
        }
    }

    public void setEmail(String email) { this.email = email; }
    public void setOtp(String otp)     { this.otp   = otp;   }

    /**
     * Reads the hCaptcha token from the WebView by calling the JS getToken() function.
     */
    private String getCaptchaToken() {
        try {
            Object result = webEngine.executeScript("getToken()");
            String token = result != null ? result.toString() : "";
            System.out.println("[NewPasswordController] Token retrieved: "
                    + (token.isEmpty() ? "<empty>" : token.substring(0, Math.min(20, token.length())) + "..."));
            return token;
        } catch (Exception e) {
            System.err.println("[NewPasswordController] Could not get captcha token: " + e.getMessage());
            return "";
        }
    }

    @FXML
    private void handleReset(ActionEvent event) {
        clearAllErrors();

        // ── CAPTCHA verification ──────────────────────────
        String token = getCaptchaToken();
        if (token.isEmpty() || !captchaService.verify(token)) {
            showFieldError(captchaError, "Please complete the security check.");
            try {
                webEngine.executeScript("resetCaptcha()");   // uses the wrapper in captcha.html
            } catch (Exception ignored) {}
            return;
        }
        captchaError.setVisible(false);
        captchaError.setManaged(false);

        // ── Password validation ───────────────────────────
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        if (password.isBlank()) {
            showFieldError(passwordError, "Password is required.");
            return;
        }
        if (password.length() < 6) {
            showFieldError(passwordError, "Minimum 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showFieldError(confirmError, "Passwords do not match.");
            return;
        }

        // ── Reset password ────────────────────────────────
        try {
            new PasswordResetService().resetPassword(email, otp, password, confirm);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Password reset successfully!");
            alert.showAndWait();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Reset failed. Please try again.");
        }
    }

    // ── Helpers ───────────────────────────────────────────

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearAllErrors() {
        for (Label l : new Label[]{passwordError, confirmError, errorLabel, captchaError}) {
            l.setText("");
            l.setVisible(false);
            l.setManaged(false);
        }
    }
}