package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.example.entities.Quiz;
import org.example.services.LessonService;

import java.io.IOException;

public class QuizDetailController {

    @FXML private Label titleLabel;
    @FXML private Label lessonLabel;
    @FXML private Label descLabel;
    @FXML private Label diffLabel;
    @FXML private Label scoreLabel;
    @FXML private Label countLabel;
    @FXML private Label statusLabel;

    private Quiz quiz;
    private final LessonService lessonService = new LessonService();

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        
        titleLabel.setText(quiz.getTitle());
        descLabel.setText(quiz.getDescription());
        scoreLabel.setText(quiz.getPassingScore() + "%");
        countLabel.setText(quiz.getQuestionCount() + " Questions");

        // Status
        if (quiz.isEnabled()) {
            statusLabel.setText("Active");
            statusLabel.setStyle("-fx-background-color: rgba(34, 197, 94, 0.1); -fx-text-fill: #16a34a; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            statusLabel.setText("Inactive");
            statusLabel.setStyle("-fx-background-color: rgba(245, 158, 11, 0.1); -fx-text-fill: #b45309; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: bold;");
        }

        // Lesson
        if (quiz.getLessonId() != null) {
            lessonService.getAllLessons().stream()
                .filter(l -> l.getId() == quiz.getLessonId())
                .findFirst()
                .ifPresentOrElse(
                    l -> lessonLabel.setText("Linked to: " + l.getTitle()),
                    () -> lessonLabel.setText("Standalone Quiz")
                );
        } else {
            lessonLabel.setText("Standalone Quiz");
        }

        // Difficulty
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < quiz.getDifficulty(); i++) stars.append("⭐");
        for (int i = quiz.getDifficulty(); i < 5; i++) stars.append("☆");
        diffLabel.setText(stars.toString());
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/QuizView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startQuiz() {
        if (quiz == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/QuizPlayView.fxml"));
            Parent root = loader.load();
            QuizPlayController controller = loader.getController();
            controller.setQuiz(quiz);
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
