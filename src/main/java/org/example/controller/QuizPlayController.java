package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.entities.Exercice;
import org.example.entities.Quiz;
import org.example.service.ExerciceService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizPlayController {

    @FXML private Label quizTitleLabel;
    @FXML private VBox exercicesContainer;

    private Quiz quiz;
    private final ExerciceService exerciceService = new ExerciceService();
    private List<Exercice> exercices;
    
    // To store user answers: Exercice ID -> TextField
    private final Map<Integer, TextField> answerFields = new HashMap<>();

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        quizTitleLabel.setText(quiz.getTitle());
        
        loadExercices();
    }

    private void loadExercices() {
        exercicesContainer.getChildren().clear();
        answerFields.clear();

        exercices = exerciceService.getExercicesByQuizId(quiz.getId());

        if (exercices == null || exercices.isEmpty()) {
            Label noDataLabel = new Label("No exercises found for this quiz.");
            noDataLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 20;");
            exercicesContainer.getChildren().add(noDataLabel);
            return;
        }

        int questionNumber = 1;
        for (Exercice ex : exercices) {
            VBox card = new VBox();
            card.setSpacing(15);
            card.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

            // Header : Question number and difficulty
            HBox header = new HBox();
            header.setSpacing(10);
            Label qNumLabel = new Label("Question " + questionNumber);
            qNumLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-font-size: 14px;");
            
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < ex.getDifficulty(); i++) stars.append("⭐");
            Label diffLabel = new Label(stars.toString());
            diffLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px;");
            
            header.getChildren().addAll(qNumLabel, diffLabel);

            // Question Text
            Label questionLabel = new Label(ex.getQuestion());
            questionLabel.setWrapText(true);
            questionLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            card.getChildren().addAll(header, questionLabel);

            // Options (if any)
            String options = ex.getOptions();
            if (options != null && !options.trim().isEmpty() && !options.equals("[]")) {
                // Trying a basic parse of ["opt1", "opt2"]
                String cleaned = options.trim();
                if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                    cleaned = cleaned.substring(1, cleaned.length() - 1);
                }
                String[] parts = cleaned.split("\",\"");
                
                VBox optionsBox = new VBox();
                optionsBox.setSpacing(8);
                optionsBox.setStyle("-fx-padding: 10 0;");
                
                for (String part : parts) {
                    String optText = part.replace("\"", "").trim();
                    if (!optText.isEmpty()) {
                        Label optLabel = new Label("• " + optText);
                        optLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
                        optionsBox.getChildren().add(optLabel);
                    }
                }
                if (!optionsBox.getChildren().isEmpty()) {
                    card.getChildren().add(optionsBox);
                }
            }

            // Answer Input
            VBox answerBox = new VBox();
            answerBox.setSpacing(5);
            Label answerTitle = new Label("Your Answer:");
            answerTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-weight: bold;");
            
            TextField answerInput = new TextField();
            answerInput.setPromptText("Type your answer here...");
            answerInput.setStyle("-fx-padding: 12; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-font-size: 14px;");
            
            answerBox.getChildren().addAll(answerTitle, answerInput);
            card.getChildren().add(answerBox);

            // Store ref
            answerFields.put(ex.getId(), answerInput);

            exercicesContainer.getChildren().add(card);
            questionNumber++;
        }
    }

    @FXML
    private void submitAnswers() {
        if (exercices == null || exercices.isEmpty()) {
            return;
        }

        // Collect user answers
        java.util.Map<Integer, String> userAnswers = new java.util.HashMap<>();
        for (Exercice ex : exercices) {
            TextField input = answerFields.get(ex.getId());
            if (input != null) {
                userAnswers.put(ex.getId(), input.getText().trim());
            } else {
                userAnswers.put(ex.getId(), "");
            }
        }

        // Navigate to results page with AI feedback
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/QuizResultView.fxml"));
            Parent root = loader.load();
            QuizResultController controller = loader.getController();
            controller.setResults(quiz, exercices, userAnswers);

            Stage stage = (Stage) quizTitleLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback: show simple alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not load results page");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QuizDetailView.fxml"));
            Parent root = loader.load();
            QuizDetailController controller = loader.getController();
            controller.setQuiz(quiz); // pass back the quiz to avoid null
            
            Stage stage = (Stage) quizTitleLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
