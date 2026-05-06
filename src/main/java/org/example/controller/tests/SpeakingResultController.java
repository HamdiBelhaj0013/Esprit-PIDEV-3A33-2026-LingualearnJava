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
import org.example.service.tests.GroqSpeakingService;
import org.example.service.tests.MockTestService;

import java.io.IOException;

public class SpeakingResultController {

    @FXML private Label testTitleLabel;
    @FXML private Label scoreLabel;
    @FXML private Label scorePctLabel;
    @FXML private Label perfLabel;
    @FXML private Label niveauCEFRLabel;
    @FXML private Label niveauTestLabel;
    @FXML private Label grammaireLabel;
    @FXML private Label vocabulaireLabel;
    @FXML private Label fluiditeLabel;
    @FXML private Label coherenceLabel;
    @FXML private Label prononciationLabel;
    @FXML private Label suggestionsLabel;
    @FXML private Label bilanLabel;

    private MockTestService        mockTestService;
    private User                   currentUser;
    private UserTestListController listController;
    private MockTest               currentTest;

    public void init(MockTestService mockTestService,
                     MockTest currentTest,
                     User currentUser,
                     UserTestListController listController,
                     GroqSpeakingService.SpeakingFeedback feedback) {

        this.mockTestService = mockTestService;
        this.currentTest     = currentTest;
        this.currentUser     = currentUser;
        this.listController  = listController;

        testTitleLabel.setText(
                currentTest.getTitle() != null ? currentTest.getTitle() : "Speaking Test");

        int note      = feedback.noteSur20();
        float scorePct = feedback.scorePct();
        String color  = scorePct >= 75 ? "#2fb344" : scorePct >= 50 ? "#f59f00" : "#e53e3e";

        scoreLabel.setText(note + " / 20");
        scoreLabel.setStyle(
                "-fx-font-size:72px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");

        scorePctLabel.setText(String.format("%.0f%%", scorePct));
        niveauCEFRLabel.setText("Niveau estimé : " + feedback.niveauCEFR());
        niveauTestLabel.setText(
                currentTest.getLevel() != null ? currentTest.getLevel() : "B1");

        String perf;
        if (scorePct >= 90)      perf = "Excellent !";
        else if (scorePct >= 75) perf = "Tres bien !";
        else if (scorePct >= 60) perf = "Bien";
        else if (scorePct >= 50) perf = "Passable";
        else                     perf = "A retravailler";
        perfLabel.setText(perf);

        grammaireLabel.setText(feedback.grammaire());
        vocabulaireLabel.setText(feedback.vocabulaire());
        fluiditeLabel.setText(feedback.fluidite());
        coherenceLabel.setText(feedback.coherence());
        prononciationLabel.setText(feedback.prononciation());
        suggestionsLabel.setText(feedback.suggestions());
        bilanLabel.setText(feedback.bilanGlobal());
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) scoreLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestListView.fxml"));
            Parent root = loader.load();
            UserTestListController ctrl = loader.getController();
            if (listController.getFilterLanguage() != null) {
                ctrl.initWithFilter(
                        mockTestService, currentUser,
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
            stage.setTitle("LinguaLearn - Tests");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur navigation : " + e.getMessage()).showAndWait();
        }
    }
}