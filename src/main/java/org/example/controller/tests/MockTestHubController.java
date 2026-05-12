package org.example.controller.tests;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entity.User;
import org.example.entity.tests.TestResult;
import org.example.service.tests.MockTestService;
import org.example.service.tests.TestResultService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class MockTestHubController {

    @FXML private Label userGreetingLabel;
    @FXML private Label totalTestsLabel;
    @FXML private Label bestScoreLabel;
    @FXML private Label certificatesLabel;
    @FXML private VBox  recentActivityBox;

    private MockTestService   service;
    private TestResultService resultService;
    private User              currentUser;
    private Stage             currentStage;
    private StackPane         contentArea;
    private Runnable          onBack;

    public void init(MockTestService service, User user, Stage stage,
                     StackPane contentArea, Runnable onBack) {
        this.service       = service;
        this.resultService = new TestResultService();
        this.currentUser   = user;
        this.currentStage  = stage;
        this.contentArea   = contentArea;
        this.onBack        = onBack;

        userGreetingLabel.setText("Bonjour, " + user.getFirstName() + " 👋");
        loadStats();
        loadRecentActivity();
    }

    private void loadStats() {
        try {
            List<TestResult> results = resultService.findByUserId(currentUser.getId());
            totalTestsLabel.setText(String.valueOf(results.size()));
            if (results.isEmpty()) {
                bestScoreLabel.setText("—");
                certificatesLabel.setText("0");
            } else {
                float best = (float) results.stream()
                        .mapToDouble(TestResult::getOverallScore).max().orElse(0);
                bestScoreLabel.setText(Math.round(best) + "%");
                long passed = results.stream()
                        .filter(r -> r.getOverallScore() >= 60f).count();
                certificatesLabel.setText(String.valueOf(passed));
            }
        } catch (Exception e) {
            totalTestsLabel.setText("—");
            bestScoreLabel.setText("—");
            certificatesLabel.setText("—");
        }
    }

    private void loadRecentActivity() {
        recentActivityBox.getChildren().clear();
        try {
            List<TestResult> results = resultService.findByUserId(currentUser.getId());
            if (results.isEmpty()) {
                showEmptyState();
                return;
            }

            boolean hasGoodScore = results.stream()
                    .anyMatch(r -> r.getOverallScore() >= 60f);
            if (hasGoodScore) {
                HBox banner = new HBox(10);
                banner.setAlignment(Pos.CENTER_LEFT);
                banner.setStyle(
                        "-fx-background-color: #d3f9d8; -fx-background-radius: 10;" +
                        "-fx-border-color: #a5d6a7; -fx-border-radius: 10; -fx-border-width: 1;" +
                        "-fx-padding: 14 18;");
                Label icon = new Label("🏆");
                icon.setStyle("-fx-font-size: 20px;");
                Label msg = new Label("Félicitations ! Vous avez obtenu de bons résultats sur certains tests.");
                msg.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
                banner.getChildren().addAll(icon, msg);
                recentActivityBox.getChildren().add(banner);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            results.stream()
                    .filter(r -> r.getDateTaken() != null)
                    .sorted(Comparator.comparing(TestResult::getDateTaken).reversed())
                    .limit(5)
                    .forEach(r -> recentActivityBox.getChildren().add(buildActivityRow(r, fmt)));

        } catch (Exception e) {
            Label err = new Label("Impossible de charger l'activité récente.");
            err.setStyle("-fx-font-size: 12px; -fx-text-fill: #d63939;");
            recentActivityBox.getChildren().add(err);
        }
    }

    private void showEmptyState() {
        VBox empty = new VBox(12);
        empty.setAlignment(Pos.CENTER);
        empty.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12;" +
                "-fx-border-color: #e3e8f0; -fx-border-radius: 12; -fx-border-width: 1;" +
                "-fx-padding: 36;");
        Label icon = new Label("📚");
        icon.setStyle("-fx-font-size: 36px;");
        Label msg = new Label("Vous n'avez encore passé aucun test.");
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c7a99;");
        Button start = new Button("Commencer maintenant →");
        start.setStyle(
                "-fx-background-color: #3b5bdb; -fx-text-fill: white;" +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8;" +
                "-fx-cursor: hand; -fx-padding: 10 20;");
        start.setOnAction(e -> handleTakeTest());
        empty.getChildren().addAll(icon, msg, start);
        recentActivityBox.getChildren().add(empty);
    }

    private HBox buildActivityRow(TestResult r, DateTimeFormatter fmt) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;" +
                "-fx-border-color: #e3e8f0; -fx-border-radius: 10; -fx-border-width: 1;" +
                "-fx-padding: 14 18;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);");

        String title = (r.getMockTest() != null && r.getMockTest().getTitle() != null)
                ? r.getMockTest().getTitle() : "Test inconnu";
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1f36;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        float score = r.getOverallScore();
        String color = score >= 75 ? "#2fb344" : score >= 50 ? "#f59f00" : "#d63939";
        String bg    = score >= 75 ? "#d3f9d8" : score >= 50 ? "#fff3cd" : "#fee2e2";
        Label scoreLbl = new Label(Math.round(score) + "%");
        scoreLbl.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";" +
                "-fx-background-color: " + bg + "; -fx-background-radius: 20;" +
                "-fx-padding: 3 12;");

        String dateStr = r.getDateTaken() != null ? r.getDateTaken().format(fmt) : "—";
        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");

        row.getChildren().addAll(titleLbl, scoreLbl, dateLbl);
        return row;
    }

    @FXML
    private void handleTakeTest() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/LanguageSelectView.fxml"));
            Node view = loader.load();
            LanguageSelectController ctrl = loader.getController();
            ctrl.initEmbedded(service, currentUser, onBack, currentStage);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Erreur navigation take test : " + e.getMessage());
        }
    }

    @FXML
    private void handleViewProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/LanguageSelectView.fxml"));
            Node view = loader.load();
            LanguageSelectController ctrl = loader.getController();
            ctrl.initEmbeddedProfile(service, currentUser, onBack, currentStage);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Erreur navigation profil : " + e.getMessage());
        }
    }
}
