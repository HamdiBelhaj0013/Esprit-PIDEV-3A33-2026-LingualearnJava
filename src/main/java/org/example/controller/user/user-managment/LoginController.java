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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.controller.admin.AdminMainController;
import org.example.controller.user.UserMainController;
import org.example.entity.User;
import org.example.service.EmailVerificationService;
import org.example.service.UserService;
import org.example.util.StageManager;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

public class LoginController {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         emailError;
    @FXML private Label         passwordError;
    @FXML private Label         errorLabel;
    @FXML private Button        resendVerificationBtn;

    private String pendingEmail;

    @FXML
    public void initialize() {
        emailField.textProperty().addListener((o, ov, nv) -> clearFieldError(emailError));
        passwordField.textProperty().addListener((o, ov, nv) -> clearFieldError(passwordError));
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        clearAllErrors();

        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isBlank()) {
            showFieldError(emailError, "Email is required.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showFieldError(emailError, "Please enter a valid email address.");
            return;
        }
        if (password.isEmpty()) {
            showFieldError(passwordError, "Password is required.");
            return;
        }

        try {
            // Check if user exists but has not verified their email yet
            Optional<User> existingUser = new UserService().findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().isVerified()) {
                showError("Email not verified. Check your inbox or resend below.");
                resendVerificationBtn.setVisible(true);
                resendVerificationBtn.setManaged(true);
                pendingEmail = email;
                return;
            }

            UserService userService = new UserService();
            var userOpt = userService.authenticate(email, password);

            if (userOpt.isEmpty()) {
                showError("Invalid email or password.");
                return;
            }

            User user    = userOpt.get();
            boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
            navigateToDashboard(user, isAdmin);

        } catch (Exception e) {
            showError("Connection error. Please try again.");
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ForgotPassword.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showError("Could not open password reset page.");
        }
    }

    @FXML
    private void handleGoToRegister(ActionEvent event) {
        try {
            StageManager.switchScene("/fxml/Register.fxml");
        } catch (IOException e) {
            showError("Could not open registration page.");
        }
    }

    @FXML
    private void handleResendVerification(ActionEvent event) {
        if (pendingEmail == null) return;
        resendVerificationBtn.setDisable(true);
        resendVerificationBtn.setText("Sending...");
        String emailToVerify = pendingEmail;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                new EmailVerificationService().generateAndSendCode(emailToVerify);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/VerifyEmail.fxml"));
                Parent root = loader.load();
                VerifyEmailController ctrl = loader.getController();
                ctrl.setEmail(emailToVerify);
                ctrl.setMode("LOGIN");
                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException ex) {
                showError("Could not open verification screen.");
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            resendVerificationBtn.setDisable(false);
            resendVerificationBtn.setText("Resend verification email");
            showError("Could not send email: " + task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    private void navigateToDashboard(User user, boolean isAdmin) throws IOException {
        Stage stage = (Stage) emailField.getScene().getWindow();

        if (isAdmin) {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/admin/AdminMain.fxml"));
            Parent root = loader.load();
            AdminMainController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.setScene(new Scene(root));
            stage.setTitle("LinguaLearn — Admin");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
        } else {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/user/UserMain.fxml"));
            Parent root = loader.load();
            UserMainController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.setScene(new Scene(root));
            stage.setTitle("LinguaLearn — Dashboard");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
        }

        stage.centerOnScreen();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearFieldError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void clearAllErrors() {
        clearFieldError(emailError);
        clearFieldError(passwordError);
        clearFieldError(errorLabel);
    }
}
