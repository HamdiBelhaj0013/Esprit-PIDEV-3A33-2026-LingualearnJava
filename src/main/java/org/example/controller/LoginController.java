package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.controller.admin.AdminMainController;
import org.example.controller.user.UserMainController;
import org.example.entity.User;
import org.example.service.UserService;
import org.example.util.Session;
import org.example.util.StageManager;

import java.io.IOException;
import java.util.regex.Pattern;

public class LoginController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         emailError;
    @FXML private Label         passwordError;
    @FXML private Label         errorLabel;

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
            UserService userService = new UserService();
            var userOpt = userService.authenticate(email, password);

            if (userOpt.isEmpty()) {
                showError("Invalid email or password.");
                return;
            }

            User user    = userOpt.get();
            boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");

            // ✅ CORRIGÉ : set la session avec le vrai ID de l'utilisateur connecté
            String role = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";
            Session.setCurrentUser(user.getId().intValue(), role);

            navigateToDashboard(user, isAdmin);

        } catch (Exception e) {
            showError("Connection error. Please try again.");
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