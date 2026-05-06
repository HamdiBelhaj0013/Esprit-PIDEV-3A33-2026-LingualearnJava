package org.example.controller.user.user_managment;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.service.HCaptchaService;
import org.example.service.user_managment.CaptchaServer;
import org.example.service.user_managment.PasswordResetService;

import java.io.File;

public class NewPasswordController {

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         passwordError;
    @FXML private Label         confirmError;
    @FXML private Label         errorLabel;
    @FXML private WebView       captchaWebView;
    @FXML private Label         captchaError;
    @FXML private Button        resetButton;

    private String email;
    private String otp;

    private final HCaptchaService captchaService = new HCaptchaService();
    private WebEngine webEngine;
    private boolean captchaPageLoaded = false;
    private int reloadAttempts = 0;
    private static final int MAX_RELOAD_ATTEMPTS = 3;

    @FXML
    public void initialize() {
        webEngine = captchaWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        File userDataDir = new File(
                System.getProperty("java.io.tmpdir"),
                "captcha-webview-" + System.nanoTime()
        );
        captchaWebView.getEngine().setUserDataDirectory(userDataDir);

        webEngine.setOnError(event ->
                System.err.println("[WebView Error] " + event.getMessage()));

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    String hasGetToken = (String) webEngine.executeScript(
                            "typeof getToken !== 'undefined' ? 'yes' : 'no'"
                    );
                    if ("no".equals(hasGetToken)) {
                        if (reloadAttempts < MAX_RELOAD_ATTEMPTS) {
                            reloadAttempts++;
                            System.err.println("[NewPasswordController] getToken not defined — reload attempt "
                                    + reloadAttempts + "/" + MAX_RELOAD_ATTEMPTS);
                            String bustUrl = CaptchaServer.getUrl() + "?v=" + System.currentTimeMillis();
                            webEngine.load(bustUrl);
                        } else {
                            // Stale server is running — give up and show error
                            System.err.println("[NewPasswordController] getToken still missing after "
                                    + MAX_RELOAD_ATTEMPTS + " attempts. Stale CaptchaServer detected.");
                            captchaPageLoaded = false;
                            Platform.runLater(() ->
                                    showFieldError(captchaError,
                                            "Security check failed to load. Please restart the application.")
                            );
                        }
                        return;
                    }
                    reloadAttempts = 0;
                    captchaPageLoaded = true;
                    System.out.println("[NewPasswordController] captcha.html loaded and JS verified.");
                } catch (Exception e) {
                    System.err.println("[NewPasswordController] JS check failed: " + e.getMessage());
                    captchaPageLoaded = false;
                }
            } else if (newState == Worker.State.FAILED) {
                captchaPageLoaded = false;
                System.err.println("[NewPasswordController] Failed to load captcha.html: "
                        + webEngine.getLoadWorker().getException());
            }
        });

        try {
            CaptchaServer.start();
            String bustUrl = CaptchaServer.getUrl() + "?v=" + System.currentTimeMillis();
            System.out.println("[NewPasswordController] Loading captcha from: " + bustUrl);
            webEngine.load(bustUrl);
        } catch (Exception e) {
            System.err.println("[NewPasswordController] Failed to start CaptchaServer: "
                    + e.getMessage());
        }
    }

    public void setEmail(String email) { this.email = email; }
    public void setOtp(String otp)     { this.otp   = otp;   }

    private String getCaptchaToken() {
        if (!captchaPageLoaded) {
            System.err.println("[NewPasswordController] Page not loaded yet — cannot get token.");
            return "";
        }
        try {
            Object result = webEngine.executeScript("getToken()");
            String token = result != null ? result.toString() : "";
            System.out.println("[NewPasswordController] Token retrieved: "
                    + (token.isEmpty() ? "<empty>"
                    : token.substring(0, Math.min(20, token.length())) + "..."));
            return token;
        } catch (Exception e) {
            System.err.println("[NewPasswordController] Could not get captcha token: "
                    + e.getMessage());
            return "";
        }
    }

    @FXML
    private void handleReset(ActionEvent event) {
        clearAllErrors();

        if (!captchaPageLoaded) {
            showFieldError(captchaError,
                    "Security check not ready. Please wait a moment and try again.");
            return;
        }

        String token = getCaptchaToken();

        if (token.isBlank()) {
            showFieldError(captchaError, "Please complete the security check.");
            return;
        }

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

        resetButton.setDisable(true);
        resetButton.setText("Verifying...");

        String finalToken = token;
        Thread verifyThread = new Thread(() -> {
            boolean valid = captchaService.verify(finalToken);

            Platform.runLater(() -> {
                resetButton.setDisable(false);
                resetButton.setText("Reset Password");

                if (!valid) {
                    showFieldError(captchaError,
                            "Security check failed. Please try again.");
                    try {
                        webEngine.executeScript("resetCaptcha()");
                    } catch (Exception ignored) {}
                    return;
                }

                doPasswordReset(password);
            });
        }, "captcha-verify-thread");

        verifyThread.setDaemon(true);
        verifyThread.start();
    }

    private void doPasswordReset(String password) {
        try {
            new PasswordResetService().resetPassword(email, otp, password, password);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Password reset successfully!");
            alert.showAndWait();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Reset failed. Please try again.");
        }
    }

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