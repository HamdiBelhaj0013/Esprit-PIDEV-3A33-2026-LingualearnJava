package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Label errorLabel;
    @FXML private Button btnLogin;

    @FXML
    public void initialize() {
        // Clear error label when user types
        tfEmail.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        pfPassword.textProperty().addListener((obs, oldVal, newVal) -> hideError());
    }

    @FXML
    private void handleLogin() {
        String email = tfEmail.getText().trim();
        String password = pfPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir votre email et mot de passe.");
            return;
        }

        // Dummy Auth Logic
        if ("admin@lingua.com".equals(email) && "admin123".equals(password)) {
            // Login as Admin
            org.example.util.UserSession.isAdmin = true;
            navigateTo("/QuizView.fxml", "Admin Dashboard - LinguaLearn");
        } else if ("client@lingua.com".equals(email) && "client123".equals(password)) {
            // Login as Client
            org.example.util.UserSession.isAdmin = false;
            navigateTo("/QuizView.fxml", "Client Portal - LinguaLearn");
        } else {
            // Failed
            showError("Identifiants incorrects. Veuillez réessayer.");
        }
    }

    @FXML
    private void fillAdminCreds() {
        tfEmail.setText("admin@lingua.com");
        pfPassword.setText("admin123");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        tfEmail.setStyle("-fx-border-color: #ef4444; -fx-background-color: #fef2f2;");
        pfPassword.setStyle("-fx-border-color: #ef4444; -fx-background-color: #fef2f2;");
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        tfEmail.setStyle(null); // Return to CSS styling
        pfPassword.setStyle(null);
    }

    private void navigateTo(String fxmlPatch, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPatch));
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            
            // On a login swap, it's safer to just set the root of the existing scene
            // so we don't snap out of maximized mode
            Scene scene = stage.getScene();
            scene.setRoot(root);
            stage.setTitle(title);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de la page: " + fxmlPatch);
        }
    }
}
