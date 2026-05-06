package org.example.controller.tests;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.service.tests.GroqWritingService;
import org.example.service.tests.MockTestService;

import java.io.IOException;

public class WritingResultController {

    @FXML private Label testTitleLabel;
    @FXML private Label scoreLabel;
    @FXML private Label scorePctLabel;
    @FXML private Label perfLabel;
    @FXML private Label motsLabel;
    @FXML private Label objectifLabel;
    @FXML private Label niveauLabel;
    @FXML private Label grammaireLabel;
    @FXML private Label coherenceLabel;
    @FXML private Label vocabulaireLabel;
    @FXML private Label suggestionsLabel;
    @FXML private Label bilanLabel;

    // Contexte navigation
    private MockTestService        mockTestService;
    private User                   currentUser;
    private UserTestListController listController;
    private MockTest               currentTest;

    // ─────────────────────────────────────────────────────────────────────────

    public void init(MockTestService mockTestService,
                     MockTest currentTest,
                     User currentUser,
                     UserTestListController listController,
                     GroqWritingService.WritingFeedback feedback,
                     int motsEcrits) {

        this.mockTestService  = mockTestService;
        this.currentTest      = currentTest;
        this.currentUser      = currentUser;
        this.listController   = listController;

        String niveau     = currentTest.getLevel() != null ? currentTest.getLevel() : "B1";
        int    motsCibles = GroqWritingService.motsCiblesParNiveau(niveau);

        // ── Score card ────────────────────────────────────────────────────────
        testTitleLabel.setText(currentTest.getTitle() != null
                ? currentTest.getTitle() : "Test Writing");

        int   note     = feedback.noteSur20();
        float scorePct = feedback.scorePct();
        String color   = scorePct >= 75 ? "#2fb344" : scorePct >= 50 ? "#f59f00" : "#e53e3e";

        scoreLabel.setText(note + " / 20");
        scoreLabel.setStyle("-fx-font-size:72px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");

        scorePctLabel.setText(String.format("%.0f%%", scorePct));

        String perf;
        String perfColor;
        if (scorePct >= 90) { perf = "🏆 Excellent !";      perfColor = "#2fb344"; }
        else if (scorePct >= 75) { perf = "🥇 Très bien !"; perfColor = "#2fb344"; }
        else if (scorePct >= 60) { perf = "✅ Bien";         perfColor = "#f59f00"; }
        else if (scorePct >= 50) { perf = "🔔 Passable";    perfColor = "#f59f00"; }
        else                     { perf = "📚 À retravailler"; perfColor = "#e53e3e"; }
        perfLabel.setText(perf);

        // ── Statistiques mots ─────────────────────────────────────────────────
        motsLabel.setText(String.valueOf(motsEcrits));
        motsLabel.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:"
                + (motsEcrits >= motsCibles ? "#2fb344" : "#f59f00") + ";");
        objectifLabel.setText(String.valueOf(motsCibles));
        niveauLabel.setText(niveau);

        // ── Feedback détaillé ────────────────────────────────────────────────
        grammaireLabel.setText(feedback.grammaire());
        coherenceLabel.setText(feedback.coherence());
        vocabulaireLabel.setText(feedback.vocabulaire());
        suggestionsLabel.setText(feedback.suggestions());
        bilanLabel.setText(feedback.correctionGlobale());
    }

    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) scoreLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestListView.fxml"));
            Parent root = loader.load();
            UserTestListController ctrl = loader.getController();
            if (listController.getFilterLanguage() != null
                    && listController.getFilterLevels() != null) {
                ctrl.initWithFilter(mockTestService, currentUser,
                        listController.getDashboardController(), stage,
                        listController.getFilterLanguage(),
                        listController.getFilterLevels(),
                        listController.getFilterLevelName());
            } else {
                ctrl.init(mockTestService, currentUser,
                        listController.getDashboardController(), stage);
            }
            ctrl.refreshData();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("LinguaLearn — Tests");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur navigation : " + e.getMessage()).showAndWait();
        }
    }
}