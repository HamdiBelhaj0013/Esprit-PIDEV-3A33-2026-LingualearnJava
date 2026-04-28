package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.entity.User;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label userInfoLabel;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Welcome, " + user.getFirstName() + "!");
        userInfoLabel.setText(user.getEmail()
                + "  ·  Statut : " + user.getStatus()
                + "  ·  Plan : " + user.getSubscriptionPlan());
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("LinguaLearn");
        stage.setMinWidth(400);
        stage.setMinHeight(500);
        stage.centerOnScreen();
    }

    @FXML
    private void handleOpenUserManagement(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/AdminDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("LinguaLearn — Gestion Utilisateurs");
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir User Management : " + e.getMessage());
        }
    }

    // ── Navigation : International Tests ──────────────────────────────────────

    @FXML
    private void handleOpenInternationalTests(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/MockTestDashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/admin.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("LinguaLearn — Tests de Certification Internationaux");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir International Tests : " + e.getMessage());
            e.printStackTrace();
        }
    }
}