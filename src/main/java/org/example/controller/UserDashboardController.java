package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.controller.tests.LanguageSelectController;
import org.example.entity.User;
import org.example.service.tests.MockTestService;

import java.io.IOException;

public class UserDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label userInfoLabel;
    @FXML private Label planLabel;
    @FXML private Label headerUserLabel;
    @FXML private Label planLabelCard;
    @FXML private VBox  contentArea;

    private User            currentUser;
    private MockTestService mockTestService;
    private Stage           currentStage;

    public void setUser(User user) {
        this.currentUser     = user;
        this.mockTestService = new MockTestService();

        if (welcomeLabel    != null)
            welcomeLabel.setText("Bienvenue, " + user.getFirstName() + " !");
        if (userInfoLabel   != null)
            userInfoLabel.setText(user.getEmail() + "  ·  Plan : " + user.getSubscriptionPlan()
                    + (user.isPremium() ? "  ·  ✨ Premium" : ""));
        if (planLabel       != null)
            planLabel.setText("Plan " + user.getSubscriptionPlan());
        if (headerUserLabel != null)
            headerUserLabel.setText(user.getFirstName() + " " + user.getLastName());
        if (planLabelCard   != null)
            planLabelCard.setText("Plan " + user.getSubscriptionPlan());
    }

    public void setStage(Stage stage) { this.currentStage = stage; }

    public User            getCurrentUser() { return currentUser; }
    public MockTestService getService()     { return mockTestService; }

    // ── Tests → LanguageSelect mode TEST ─────────────────────────────────────

    @FXML
    private void handleOpenTests(ActionEvent event) {
        openLanguageSelect(LanguageSelectController.Mode.TEST,
                "LinguaLearn — Choisir une langue");
    }

    // ── Profil → LanguageSelect mode PROFILE ──────────────────────────────────
    // Même écran de sélection de langue, mais au clic sur une langue,
    // on navigue vers ProfileView filtré sur cette langue.

    @FXML
    private void handleOpenProfile(ActionEvent event) {
        openLanguageSelect(LanguageSelectController.Mode.PROFILE,
                "LinguaLearn — Choisir votre langue");
    }

    private void openLanguageSelect(LanguageSelectController.Mode mode, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/LanguageSelectView.fxml"));
            Parent root = loader.load();
            LanguageSelectController ctrl = loader.getController();

            Stage stage = resolveStage();

            if (mode == LanguageSelectController.Mode.PROFILE) {
                ctrl.initForProfile(mockTestService, currentUser, this, stage);
            } else {
                ctrl.init(mockTestService, currentUser, this, stage);
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMinWidth(1000);
            stage.setMinHeight(680);
            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("Erreur ouverture langue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Retour au dashboard ───────────────────────────────────────────────────

    public void returnToDashboard() {
        try {
            Stage stage = resolveStage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/UserDashboard.fxml"));
            Parent root = loader.load();
            UserDashboardController ctrl = loader.getController();
            ctrl.setUser(currentUser);
            ctrl.setStage(stage);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("LinguaLearn — Dashboard");
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("Erreur retour dashboard : " + e.getMessage());
        }
    }

    // ── Déconnexion ───────────────────────────────────────────────────────────

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Stage stage = resolveStage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("LinguaLearn");
            stage.setMinWidth(400);
            stage.setMinHeight(500);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur logout : " + e.getMessage());
        }
    }

    private Stage resolveStage() {
        if (currentStage != null) return currentStage;
        if (welcomeLabel    != null && welcomeLabel.getScene()    != null)
            return (Stage) welcomeLabel.getScene().getWindow();
        if (headerUserLabel != null && headerUserLabel.getScene() != null)
            return (Stage) headerUserLabel.getScene().getWindow();
        throw new IllegalStateException("Stage introuvable");
    }
}