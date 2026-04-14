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
import org.example.util.StageManager;

import java.io.IOException;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
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
}
