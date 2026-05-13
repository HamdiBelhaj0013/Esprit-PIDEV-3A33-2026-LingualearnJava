package org.example.controller.tests;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.service.tests.MockTestService;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Shown when the user clicks a test — displays all test details before starting.
 * Navigate to: /fxml/tests/TestPreviewView.fxml
 */
public class TestPreviewController implements Initializable {

    @FXML private Label testTitleLabel;
    @FXML private Label testTypeLabel;
    @FXML private Label testLanguageLabel;
    @FXML private Label testLevelLabel;
    @FXML private Label testDurationLabel;
    @FXML private Label testQuestionsLabel;
    @FXML private Label testScoringLabel;
    @FXML private Label penaltyRulesLabel;

    private MockTestService        service;
    private MockTest               test;
    private User                   user;
    private UserTestListController listController;
    private Stage                  stage;
    private StackPane              contentArea;

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void init(MockTestService service, MockTest test, User user,
                     UserTestListController listController, Stage stage) {
        this.service        = service;
        this.test           = test;
        this.user           = user;
        this.listController = listController;
        this.stage          = stage;

        long nbQuestions = service.countQuestionsByTest(test.getId());

        testTitleLabel.setText(test.getTitle());
        testTypeLabel.setText(test.getTestType());
        testLanguageLabel.setText(
                test.getPlatformLanguage() != null ? test.getPlatformLanguage().getName() : "—");
        testLevelLabel.setText(test.getLevel());
        testDurationLabel.setText(test.getDurationMinutes() + " minutes");
        testQuestionsLabel.setText(nbQuestions + " question(s) — ordre aléatoire à chaque passage");
        testScoringLabel.setText("Score sur 20 pts  (pourcentage × 20 / 100)");
        penaltyRulesLabel.setText(
                "• Soumission en moins de 20% du temps → pénalité  -20%\n" +
                        "• Dépassement léger  (≤ 110%) → pénalité  -10%\n" +
                        "• Dépassement modéré (≤ 125%) → pénalité  -25%\n" +
                        "• Dépassement grave  (≤ 150%) → pénalité  -50%\n" +
                        "• Auto-soumission forcée à 150% du temps alloué");
    }

    @FXML
    private void handleStart(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestDetailView.fxml"));
            Parent root = loader.load();
            UserTestDetailController ctrl = loader.getController();
            ctrl.init(service, test, user, listController);
            if (contentArea != null) {
                ctrl.setContentArea(contentArea);
                contentArea.getChildren().setAll(root);
            } else {
                Scene scene = new Scene(root);
                scene.getStylesheets().add(
                        getClass().getResource("/css/style.css").toExternalForm());
                stage.setScene(scene);
                stage.setTitle("LinguaLearn — " + test.getTitle());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestListView.fxml"));
            Parent root = loader.load();
            UserTestListController ctrl = loader.getController();
            if (listController.getFilterLanguage() != null
                    && listController.getFilterLevels() != null) {
                ctrl.initWithFilter(service, user, listController.getDashboardController(),
                        stage, listController.getFilterLanguage(),
                        listController.getFilterLevels(), listController.getFilterLevelName());
            } else {
                ctrl.init(service, user, listController.getDashboardController(), stage);
            }
            ctrl.setOnBack(listController.getOnBack());
            ctrl.refreshData();
            if (contentArea != null) {
                ctrl.setContentArea(contentArea);
                contentArea.getChildren().setAll(root);
            } else {
                Scene scene = new Scene(root);
                scene.getStylesheets().add(
                        getClass().getResource("/css/style.css").toExternalForm());
                stage.setScene(scene);
                stage.setTitle("LinguaLearn — Tests de Certification");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}