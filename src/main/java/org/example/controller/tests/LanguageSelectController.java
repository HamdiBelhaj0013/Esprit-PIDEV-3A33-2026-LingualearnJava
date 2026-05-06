package org.example.controller.tests;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.controller.UserDashboardController;
import org.example.entity.User;
import org.example.entity.tests.PlatformLanguage;
import org.example.service.tests.MockTestService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class LanguageSelectController implements Initializable {

    @FXML private Label    userNameLabel;
    @FXML private FlowPane languageCardsPane;

    private MockTestService         service;
    private User                    currentUser;
    private UserDashboardController dashboardController;
    private Stage                   currentStage;

    /**
     * MODE détermine la destination après sélection de langue :
     *   TEST    → LevelSelectView (flux tests)
     *   PROFILE → ProfileView (profil filtré par langue)
     */
    public enum Mode { TEST, PROFILE }
    private Mode mode = Mode.TEST;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    /** Entrée depuis "Voir les tests disponibles" → mode TEST */
    public void init(MockTestService service, User user,
                     UserDashboardController dashboardController, Stage stage) {
        initWithMode(service, user, dashboardController, stage, Mode.TEST);
    }

    /** Entrée depuis "Voir mon profil" → mode PROFILE */
    public void initForProfile(MockTestService service, User user,
                               UserDashboardController dashboardController, Stage stage) {
        initWithMode(service, user, dashboardController, stage, Mode.PROFILE);
    }

    private void initWithMode(MockTestService service, User user,
                              UserDashboardController dashboardController,
                              Stage stage, Mode mode) {
        this.service             = service;
        this.currentUser         = user;
        this.dashboardController = dashboardController;
        this.currentStage        = stage;
        this.mode                = mode;

        userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
        loadLanguageCards();
    }

    private void loadLanguageCards() {
        languageCardsPane.getChildren().clear();
        List<PlatformLanguage> langs = service.findAllLanguages();

        String[] colors = {"#3b5bdb","#2e7d32","#e65100","#c2185b","#0277bd","#6a1b9a"};
        String[] bgs    = {"#eef2ff","#e8f5e9","#fff3e0","#fce4ec","#e1f5fe","#ede7f6"};

        int i = 0;
        for (PlatformLanguage lang : langs) {
            final PlatformLanguage selectedLang = lang;
            String color = colors[i % colors.length];
            String bg    = bgs[i % bgs.length];

            VBox card = new VBox(12);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(180);
            card.setPrefHeight(160);
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 14;" +
                            "-fx-border-color: #e3e8f0; -fx-border-radius: 14; -fx-border-width: 1;" +
                            "-fx-cursor: hand; -fx-padding: 24;" +
                            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);"
            );

            Label initial = new Label(lang.getName().substring(0, 1).toUpperCase());
            initial.setStyle(
                    "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";" +
                            "-fx-background-color: " + bg + "; -fx-background-radius: 50%;" +
                            "-fx-min-width: 60; -fx-min-height: 60; -fx-alignment: center;"
            );
            initial.setMinSize(60, 60);
            initial.setMaxSize(60, 60);
            initial.setAlignment(Pos.CENTER);

            Label nameLbl = new Label(lang.getName());
            nameLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1f36;");

            long count = service.filterByLanguageId(lang.getId()).size();
            Label countLbl = new Label(count + " test(s)");
            countLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");

            if (mode == Mode.PROFILE) {
                Label modeLbl = new Label("Voir mon profil");
                modeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + ";" +
                        "-fx-background-color: " + bg + "; -fx-background-radius: 20;" +
                        "-fx-padding: 2 8; -fx-font-weight: bold;");
                card.getChildren().addAll(initial, nameLbl, countLbl, modeLbl);
            } else {
                card.getChildren().addAll(initial, nameLbl, countLbl);
            }

            String normalStyle = "-fx-background-color: white; -fx-background-radius: 14;" +
                    "-fx-border-color: #e3e8f0; -fx-border-radius: 14; -fx-border-width: 1;" +
                    "-fx-cursor: hand; -fx-padding: 24;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);";
            String hoverStyle  = "-fx-background-color: " + bg + "; -fx-background-radius: 14;" +
                    "-fx-border-color: " + color + "; -fx-border-radius: 14; -fx-border-width: 2;" +
                    "-fx-cursor: hand; -fx-padding: 24;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),14,0,0,5);";

            card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
            card.setOnMouseExited(e  -> card.setStyle(normalStyle));
            card.setOnMouseClicked(e -> handleLanguageSelected(selectedLang));

            languageCardsPane.getChildren().add(card);
            i++;
        }
    }

    private void handleLanguageSelected(PlatformLanguage lang) {
        if (mode == Mode.PROFILE) {
            navigateToProfile(lang);
        } else {
            navigateToLevel(lang);
        }
    }

    private void navigateToLevel(PlatformLanguage lang) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/LevelSelectView.fxml"));
            Parent root = loader.load();
            LevelSelectController ctrl = loader.getController();
            ctrl.init(service, currentUser, dashboardController, currentStage, lang);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("LinguaLearn — Niveau");
        } catch (IOException e) {
            System.err.println("Erreur navigation niveau : " + e.getMessage());
        }
    }

    private void navigateToProfile(PlatformLanguage lang) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/ProfileView.fxml"));
            Parent root = loader.load();
            ProfileController ctrl = loader.getController();
            // Nouveau init avec langue
            ctrl.init(currentUser, dashboardController, currentStage, service, lang);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("LinguaLearn — Mon Profil · " + lang.getName());
            currentStage.setMinWidth(1100);
            currentStage.setMinHeight(700);
            currentStage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur navigation profil : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        if (dashboardController != null) dashboardController.returnToDashboard();
    }
}