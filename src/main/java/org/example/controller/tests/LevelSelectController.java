package org.example.controller.tests;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.controller.UserDashboardController;
import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.PlatformLanguage;
import org.example.service.tests.MockTestService;
import org.example.service.tests.TestResultService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class LevelSelectController implements Initializable {

    @FXML private Label userNameLabel;
    @FXML private Label languageTitleLabel;
    @FXML private Label languageSubtitleLabel;

    @FXML private VBox  beginnerCard;
    @FXML private Label beginnerCountLabel;
    @FXML private Label beginnerBestLabel;
    @FXML private Label beginnerLockLabel;

    @FXML private VBox  intermediateCard;
    @FXML private Label intermediateCountLabel;
    @FXML private Label intermediateBestLabel;
    @FXML private Label intermediateLockLabel;

    @FXML private VBox  advancedCard;
    @FXML private Label advancedCountLabel;
    @FXML private Label advancedBestLabel;
    @FXML private Label advancedLockLabel;

    private MockTestService         service;
    private TestResultService       resultService;
    private User                    currentUser;
    private UserDashboardController dashboardController;
    private Stage                   currentStage;
    private PlatformLanguage        selectedLanguage;
    private Runnable                onBack;
    private Scene                   originalScene; // ← NOUVEAU
    private String                  originalTitle; // ← NOUVEAU

    private boolean intermediateUnlocked = false;
    private boolean advancedUnlocked     = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setOnBack(Runnable onBack) { this.onBack = onBack; }

    // ← NOUVEAU
    public void setOriginalScene(Scene scene, String title) {
        this.originalScene = scene;
        this.originalTitle = title;
    }

    public void init(MockTestService service, User user,
                     UserDashboardController dashboardController,
                     Stage stage, PlatformLanguage language) {
        this.service             = service;
        this.resultService       = new TestResultService();
        this.currentUser         = user;
        this.dashboardController = dashboardController;
        this.currentStage        = stage;
        this.selectedLanguage    = language;

        userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
        languageTitleLabel.setText("Tests en " + language.getName());
        languageSubtitleLabel.setText("Choisissez votre niveau — " + language.getName());

        checkUnlocks();
        updateCounts();
        updateCardsUI();
    }

    private void checkUnlocks() {
        Long uid  = currentUser.getId();
        Long lang = selectedLanguage.getId();
        intermediateUnlocked = resultService.hasPassedLevel(uid, "A1", 50f, lang);
        advancedUnlocked = resultService.hasPassedLevel(uid, "B1", 50f, lang)
                || resultService.hasPassedLevel(uid, "B2", 50f, lang);
    }

    private void updateCounts() {
        List<MockTest> all = service.filterByLanguageId(selectedLanguage.getId());

        long beginner     = all.stream().filter(t -> isA(t.getLevel())).count();
        long intermediate = all.stream().filter(t -> isB(t.getLevel())).count();
        long advanced     = all.stream().filter(t -> isC(t.getLevel())).count();

        beginnerCountLabel.setText(beginner + " test(s) disponible(s)");
        intermediateCountLabel.setText(intermediate + " test(s) disponible(s)");
        advancedCountLabel.setText(advanced + " test(s) disponible(s)");

        Long lang = selectedLanguage.getId();
        float bestBeg   = resultService.bestScoreInLevels(currentUser.getId(), new String[]{"A1","A2"}, lang);
        float bestInter = resultService.bestScoreInLevels(currentUser.getId(), new String[]{"B1","B2"}, lang);
        float bestAdv   = resultService.bestScoreInLevels(currentUser.getId(), new String[]{"C1","C2"}, lang);

        beginnerBestLabel.setText(bestBeg > 0 ? "Meilleur score : " + Math.round(bestBeg) + "%" : "Pas encore tente");
        intermediateBestLabel.setText(bestInter > 0 ? "Meilleur score : " + Math.round(bestInter) + "%" : "Pas encore tente");
        advancedBestLabel.setText(bestAdv > 0 ? "Meilleur score : " + Math.round(bestAdv) + "%" : "Pas encore tente");
    }

    private void updateCardsUI() {
        styleCard(beginnerCard, true, "#2e7d32", "#e8f5e9", "#a5d6a7");
        beginnerLockLabel.setVisible(false);
        beginnerLockLabel.setManaged(false);

        if (intermediateUnlocked) {
            styleCard(intermediateCard, true, "#e65100", "#fff3e0", "#ffcc80");
            intermediateLockLabel.setVisible(false);
            intermediateLockLabel.setManaged(false);
        } else {
            styleCardLocked(intermediateCard);
            intermediateLockLabel.setText("Verrou : Obtenez >= 50% dans un test A1 pour debloquer");
            intermediateLockLabel.setVisible(true);
            intermediateLockLabel.setManaged(true);
        }

        if (advancedUnlocked) {
            styleCard(advancedCard, true, "#4527a0", "#ede7f6", "#ce93d8");
            advancedLockLabel.setVisible(false);
            advancedLockLabel.setManaged(false);
        } else {
            styleCardLocked(advancedCard);
            advancedLockLabel.setText("Verrou : Obtenez >= 50% dans un test B1 ou B2 pour debloquer");
            advancedLockLabel.setVisible(true);
            advancedLockLabel.setManaged(true);
        }
    }

    private void styleCard(VBox card, boolean active, String color, String bg, String border) {
        card.setStyle(
                "-fx-background-color: " + bg + "; -fx-background-radius: 14;" +
                        "-fx-border-color: " + border + "; -fx-border-radius: 14; -fx-border-width: 2;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);"
        );
    }

    private void styleCardLocked(VBox card) {
        card.setStyle(
                "-fx-background-color: #f5f5f5; -fx-background-radius: 14;" +
                        "-fx-border-color: #e0e0e0; -fx-border-radius: 14; -fx-border-width: 1.5;" +
                        "-fx-cursor: default; -fx-opacity: 0.75;"
        );
    }

    @FXML
    private void handleBeginner(MouseEvent event) {
        navigateToTests(new String[]{"A1", "A2"}, "Beginner");
    }

    @FXML
    private void handleIntermediate(MouseEvent event) {
        if (!intermediateUnlocked) {
            showLockAlert("Intermediate",
                    "Obtenez au moins 50% dans un test de niveau A1 pour debloquer ce niveau.");
            return;
        }
        navigateToTests(new String[]{"B1", "B2"}, "Intermediate");
    }

    @FXML
    private void handleAdvanced(MouseEvent event) {
        if (!advancedUnlocked) {
            showLockAlert("Advanced",
                    "Obtenez au moins 50% dans un test de niveau B1 ou B2 pour debloquer ce niveau.");
            return;
        }
        navigateToTests(new String[]{"C1", "C2"}, "Advanced");
    }

    private void showLockAlert(String level, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Niveau verrouille");
        alert.setHeaderText("Niveau " + level + " — Non disponible");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void navigateToTests(String[] levels, String levelName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestListView.fxml"));
            Parent root = loader.load();
            UserTestListController ctrl = loader.getController();
            ctrl.initWithFilter(service, currentUser, dashboardController,
                    currentStage, selectedLanguage, levels, levelName);
            ctrl.setOnBack(onBack);
            ctrl.setOriginalScene(originalScene, originalTitle); // ← NOUVEAU
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("LinguaLearn — Tests " + levelName);
        } catch (IOException e) {
            System.err.println("Erreur navigation tests : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/LanguageSelectView.fxml"));
            Parent root = loader.load();
            LanguageSelectController ctrl = loader.getController();
            if (onBack != null) {
                ctrl.initEmbedded(service, currentUser, onBack, currentStage);
            } else {
                ctrl.init(service, currentUser, dashboardController, currentStage);
            }
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("LinguaLearn — Langue");
        } catch (IOException e) {
            System.err.println("Erreur retour langue : " + e.getMessage());
        }
    }

    private boolean isA(String level) { return "A1".equals(level) || "A2".equals(level); }
    private boolean isB(String level) { return "B1".equals(level) || "B2".equals(level); }
    private boolean isC(String level) { return "C1".equals(level) || "C2".equals(level); }
}