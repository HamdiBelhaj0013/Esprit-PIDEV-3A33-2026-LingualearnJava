package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.service.UserService;
import org.example.util.StageManager;

public class RegisterController {

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

    // ── Handlers ─────────────────────────────────────────────────────────────

    @FXML
    private void handleRegister(ActionEvent event) {
        clearErrors();

        String firstName      = firstNameField.getText().trim();
        String lastName       = lastNameField.getText().trim();
        String email          = emailField.getText().trim();
        String password       = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        boolean valid = true;

        if (firstName.isBlank()) {
            showError(firstNameError, "First name is required.");
            valid = false;
        } else if (firstName.length() < 2) {
            showError(firstNameError, "At least 2 characters.");
            valid = false;
        }

        if (lastName.isBlank()) {
            showError(lastNameError, "Last name is required.");
            valid = false;
        } else if (lastName.length() < 2) {
            showError(lastNameError, "At least 2 characters.");
            valid = false;
        }

        if (email.isBlank()) {
            showError(emailError, "Email is required.");
            valid = false;
        }

        if (password.isEmpty()) {
            showError(passwordError, "Password is required.");
            valid = false;
        } else if (password.length() < 6) {
            showError(passwordError, "At least 6 characters.");
            valid = false;
        }

        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordError, "Passwords do not match.");
            valid = false;
        }

        if (!valid) return;

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
