package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.Exercice;
import org.example.entities.Quiz;
import org.example.service.GeminiService;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.*;

/**
 * Controller for the Quiz Result view.
 * Displays the score, per-question results, and AI-powered explanations.
 */
public class QuizResultController {

    @FXML private VBox scoreBanner;
    @FXML private Label scoreTitleLabel;
    @FXML private Label scoreSubLabel;
    @FXML private Label percentLabel;
    @FXML private VBox aiSummaryCard;
    @FXML private Label aiSummaryLabel;
    @FXML private ProgressIndicator summarySpinner;
    @FXML private VBox resultsContainer;

    private Quiz quiz;
    private final GeminiService geminiService = new GeminiService();

    /**
     * Called from QuizPlayController after submission.
     *
     * @param quiz       The quiz that was played
     * @param exercices  The list of exercises
     * @param userAnswers Map of exercice ID -> user's answer string
     */
    public void setResults(Quiz quiz, List<Exercice> exercices, Map<Integer, String> userAnswers) {
        this.quiz = quiz;

        // Calculate score
        int score = 0;
        int total = exercices.size();
        List<Map<String, String>> allResults = new ArrayList<>();

        for (Exercice ex : exercices) {
            String userAnswer = userAnswers.getOrDefault(ex.getId(), "");
            String correctAnswer = ex.getCorrectAnswer() != null ? ex.getCorrectAnswer().trim() : "";
            boolean isCorrect = userAnswer.equalsIgnoreCase(correctAnswer);
            if (isCorrect) score++;

            Map<String, String> result = new LinkedHashMap<>();
            result.put("question", ex.getQuestion());
            result.put("options", ex.getOptions());
            result.put("correctAnswer", correctAnswer);
            result.put("userAnswer", userAnswer);
            result.put("isCorrect", String.valueOf(isCorrect));
            allResults.add(result);
        }

        // Update score banner
        int percent = total > 0 ? (score * 100 / total) : 0;
        scoreTitleLabel.setText(score + " / " + total);
        percentLabel.setText(percent + "%");

        // Color the banner based on performance
        if (percent >= 80) {
            scoreBanner.setStyle("-fx-background-color: linear-gradient(to right, #059669, #10b981); -fx-background-radius: 16; -fx-padding: 35;");
            scoreSubLabel.setText("🎉 Excellent work! You've mastered this quiz!");
        } else if (percent >= 50) {
            scoreBanner.setStyle("-fx-background-color: linear-gradient(to right, #d97706, #f59e0b); -fx-background-radius: 16; -fx-padding: 35;");
            scoreSubLabel.setText("👍 Good effort! Review the explanations below to improve.");
        } else {
            scoreBanner.setStyle("-fx-background-color: linear-gradient(to right, #dc2626, #ef4444); -fx-background-radius: 16; -fx-padding: 35;");
            scoreSubLabel.setText("📚 Keep practicing! The AI explanations below will help you learn.");
        }

        // Build per-question result cards
        buildResultCards(exercices, userAnswers, allResults);

        // Load AI summary in background thread
        loadAISummary(allResults);
    }

    private void buildResultCards(List<Exercice> exercices, Map<Integer, String> userAnswers,
                                   List<Map<String, String>> allResults) {
        resultsContainer.getChildren().clear();

        for (int i = 0; i < exercices.size(); i++) {
            Exercice ex = exercices.get(i);
            Map<String, String> result = allResults.get(i);
            boolean isCorrect = "true".equals(result.get("isCorrect"));
            String userAnswer = result.get("userAnswer");

            // Main card
            VBox card = new VBox();
            card.setSpacing(14);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25; "
                    + "-fx-border-color: " + (isCorrect ? "#bbf7d0" : "#fecaca") + "; "
                    + "-fx-border-radius: 12; -fx-border-width: 1.5;");

            // Header row: Question number + result badge
            HBox header = new HBox();
            header.setSpacing(12);
            header.setAlignment(Pos.CENTER_LEFT);

            Label qNum = new Label("Question " + (i + 1));
            qNum.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label badge = new Label(isCorrect ? "✓ Correct" : "✗ Incorrect");
            badge.setStyle(isCorrect
                    ? "-fx-background-color: rgba(34, 197, 94, 0.1); -fx-text-fill: #16a34a; -fx-padding: 4 14; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;"
                    : "-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #dc2626; -fx-padding: 4 14; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");

            header.getChildren().addAll(qNum, spacer, badge);

            // Question text
            Label questionText = new Label(ex.getQuestion());
            questionText.setWrapText(true);
            questionText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #334155;");

            card.getChildren().addAll(header, questionText);

            // Answers row
            HBox answersRow = new HBox();
            answersRow.setSpacing(16);

            // Your answer box
            VBox yourAnswerBox = new VBox(4);
            HBox.setHgrow(yourAnswerBox, Priority.ALWAYS);
            Label yourTitle = new Label("YOUR ANSWER");
            yourTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-font-size: 10px;");
            Label yourValue = new Label(userAnswer.isEmpty() ? "(no answer)" : userAnswer);
            yourValue.setWrapText(true);
            yourValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                    + (isCorrect ? "#16a34a" : "#dc2626") + ";");
            yourAnswerBox.getChildren().addAll(yourTitle, yourValue);

            // Correct answer box (shown only if wrong)
            if (!isCorrect) {
                VBox correctBox = new VBox(4);
                HBox.setHgrow(correctBox, Priority.ALWAYS);
                Label correctTitle = new Label("CORRECT ANSWER");
                correctTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-font-size: 10px;");
                Label correctValue = new Label(ex.getCorrectAnswer());
                correctValue.setWrapText(true);
                correctValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
                correctBox.getChildren().addAll(correctTitle, correctValue);
                answersRow.getChildren().addAll(yourAnswerBox, correctBox);
            } else {
                answersRow.getChildren().add(yourAnswerBox);
            }
            card.getChildren().add(answersRow);

            // AI Explanation area (with spinner placeholder)
            VBox aiBox = new VBox(8);
            aiBox.setStyle("-fx-background-color: rgba(139, 92, 246, 0.04); -fx-background-radius: 8; -fx-padding: 16;");

            HBox aiHeader = new HBox(8);
            aiHeader.setAlignment(Pos.CENTER_LEFT);
            FontIcon robotIcon = new FontIcon("fas-robot");
            robotIcon.setIconSize(14);
            robotIcon.setIconColor(javafx.scene.paint.Color.web("#8b5cf6"));
            Label aiTitle = new Label("AI Explanation");
            aiTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b5cf6; -fx-font-size: 12px;");
            aiHeader.getChildren().addAll(robotIcon, aiTitle);

            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(18, 18);
            spinner.setMaxSize(18, 18);

            Label aiText = new Label("Generating explanation...");
            aiText.setWrapText(true);
            aiText.setMinHeight(Region.USE_PREF_SIZE);
            aiText.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-line-spacing: 3;");

            aiBox.getChildren().addAll(aiHeader, spinner, aiText);
            card.getChildren().add(aiBox);

            resultsContainer.getChildren().add(card);

            // Load AI explanation in background
            final int questionIndex = i;
            final String question = ex.getQuestion();
            final String options = ex.getOptions();
            final String correctAnswer = ex.getCorrectAnswer();
            final boolean correct = isCorrect;

            Thread aiThread = new Thread(() -> {
                String explanation = geminiService.getAnswerExplanation(
                        question, options, correctAnswer, userAnswer, correct);
                Platform.runLater(() -> {
                    aiText.setText(explanation);
                    aiBox.getChildren().remove(spinner);
                });
            });
            aiThread.setDaemon(true);
            aiThread.start();
        }
    }

    private void loadAISummary(List<Map<String, String>> allResults) {
        // Ensure the summary label never truncates
        aiSummaryLabel.setMinHeight(Region.USE_PREF_SIZE);

        Thread summaryThread = new Thread(() -> {
            String summary = geminiService.getQuizSummary(allResults);
            Platform.runLater(() -> {
                aiSummaryLabel.setText(summary);
                summarySpinner.setVisible(false);
            });
        });
        summaryThread.setDaemon(true);
        summaryThread.start();
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QuizDetailView.fxml"));
            Parent root = loader.load();
            QuizDetailController controller = loader.getController();
            controller.setQuiz(quiz);

            Stage stage = (Stage) scoreTitleLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
