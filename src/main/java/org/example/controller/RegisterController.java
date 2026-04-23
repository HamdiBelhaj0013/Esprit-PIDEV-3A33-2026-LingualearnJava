package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.service.UserService;
import org.example.util.StageManager;

import java.util.regex.Pattern;

public class RegisterController {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label generalError;

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        firstNameField.textProperty().addListener((o, ov, nv)        -> clearError(firstNameError));
        lastNameField.textProperty().addListener((o, ov, nv)         -> clearError(lastNameError));
        emailField.textProperty().addListener((o, ov, nv)            -> clearError(emailError));
        passwordField.textProperty().addListener((o, ov, nv)         -> clearError(passwordError));
        confirmPasswordField.textProperty().addListener((o, ov, nv)  -> clearError(confirmPasswordError));
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    @FXML
    private void handleRegister(ActionEvent event) {
        clearErrors();

        String firstName       = firstNameField.getText().trim();
        String lastName        = lastNameField.getText().trim();
        String email           = emailField.getText().trim();
        String password        = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (firstName.isBlank()) {
            showError(firstNameError, "First name is required.");
            return;
        }
        if (firstName.length() < 2) {
            showError(firstNameError, "At least 2 characters.");
            return;
        }
        if (firstName.chars().anyMatch(Character::isDigit)) {
            showError(firstNameError, "First name must not contain numbers.");
            return;
        }

        if (lastName.isBlank()) {
            showError(lastNameError, "Last name is required.");
            return;
        }
        if (lastName.length() < 2) {
            showError(lastNameError, "At least 2 characters.");
            return;
        }
        if (lastName.chars().anyMatch(Character::isDigit)) {
            showError(lastNameError, "Last name must not contain numbers.");
            return;
        }

        if (email.isBlank()) {
            showError(emailError, "Email is required.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(emailError, "Please enter a valid email address.");
            return;
        }

        if (password.isEmpty()) {
            showError(passwordError, "Password is required.");
            return;
        }
        if (password.length() < 8) {
            showError(passwordError, "At least 8 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordError, "Passwords do not match.");
            return;
        }

        try {
            new UserService().registerUser(firstName, lastName, email, password);
            // Registration succeeded — navigate back to login
            StageManager.switchScene("/fxml/login.fxml");

        } catch (IllegalArgumentException e) {
            // Duplicate email or other validation errors from the service
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("email")) {
                showError(emailError, msg);
            } else {
                showError(generalError, msg);
            }
        } catch (Exception e) {
            showError(generalError, "Registration failed. Please try again.");
        }
    }

    @FXML
    private void handleGoToLogin(ActionEvent event) {
        try {
            StageManager.switchScene("/fxml/login.fxml");
        } catch (Exception e) {
            showError(generalError, "Navigation error. Please restart the app.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void clearErrors() {
        clearError(firstNameError);
        clearError(lastNameError);
        clearError(emailError);
        clearError(passwordError);
        clearError(confirmPasswordError);
        clearError(generalError);
    }
}
