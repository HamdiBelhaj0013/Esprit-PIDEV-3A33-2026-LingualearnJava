package org.example.controller;

import org.example.App;
import jakarta.persistence.EntityManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entity.User;
import org.example.service.UserService;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        EntityManager em = App.getEmf().createEntityManager();
        try {
            UserService userService = new UserService(em);
            var userOpt = userService.authenticate(email, password);

            if (userOpt.isEmpty()) {
                showError("Invalid email or password.");
                return;
            }

            User user = userOpt.get();
            boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");

            navigateToDashboard(user, isAdmin);

        } catch (Exception e) {
            showError("Connection error. Please try again.");
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private void navigateToDashboard(User user, boolean isAdmin) throws IOException {
        String fxmlPath = isAdmin ? "/fxml/AdminDashboard.fxml" : "/fxml/UserDashboard.fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        if (isAdmin) {
            AdminDashboardController ctrl = loader.getController();
            ctrl.setUser(user);
        } else {
            UserDashboardController ctrl = loader.getController();
            ctrl.setUser(user);
        }

        Stage stage = (Stage) emailField.getScene().getWindow();
        Scene scene  = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("LinguaLearn — " + (isAdmin ? "Admin Panel" : "Dashboard"));
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.centerOnScreen();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
