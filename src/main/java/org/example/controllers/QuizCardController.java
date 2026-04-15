package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.entities.Quiz;
import org.example.services.LessonService;

import java.util.function.Consumer;

public class QuizCardController {

    @FXML private Label labelId;
    @FXML private Label labelTitle;
    @FXML private Label labelLesson;
    @FXML private Label labelExercices;
    @FXML private Label labelPassingScore;
    @FXML private Label labelDifficulty;
    @FXML private Label labelStatus;

    @FXML private javafx.scene.control.Button btnEdit;
    @FXML private javafx.scene.control.Button btnDelete;

    private Quiz quiz;
    private Consumer<Quiz> onEdit;
    private Consumer<Quiz> onDelete;
    private Consumer<Quiz> onView;

    private final LessonService lessonService = new LessonService();

    public void setData(Quiz quiz, Consumer<Quiz> onView, Consumer<Quiz> onEdit, Consumer<Quiz> onDelete) {
        this.quiz = quiz;
        this.onView = onView;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        labelId.setText("#" + quiz.getId());
        labelTitle.setText(quiz.getTitle());

        // Lesson
        if (quiz.getLessonId() != null) {
            lessonService.getAllLessons().stream()
                    .filter(l -> l.getId() == quiz.getLessonId())
                    .findFirst()
                    .ifPresent(l -> labelLesson.setText(l.getTitle()));
        } else {
            labelLesson.setText("No lesson");
            labelLesson.getStyleClass().remove("badge-lesson");
            labelLesson.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px;");
        }

        // Stats
        labelExercices.setText(String.valueOf(quiz.getQuestionCount()));
        labelPassingScore.setText(quiz.getPassingScore() + "%");

        // Star difficulty
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < quiz.getDifficulty(); i++) stars.append("⭐");
        for (int i = quiz.getDifficulty(); i < 5; i++) stars.append("☆");
        labelDifficulty.setText(stars.toString());

        // Status badge
        if (quiz.isEnabled()) {
            labelStatus.setText("● Active");
            labelStatus.getStyleClass().setAll("badge-active");
        } else {
            labelStatus.setText("● Inactive");
            labelStatus.getStyleClass().setAll("badge-inactive");
        }

        if (!org.example.util.UserSession.isAdmin) {
            if (btnEdit != null) { btnEdit.setVisible(false); btnEdit.setManaged(false); }
            if (btnDelete != null) { btnDelete.setVisible(false); btnDelete.setManaged(false); }
        }
    }

    @FXML
    private void handleView() {
        if (onView != null) onView.accept(quiz);
    }

    @FXML
    private void handleEdit() {
        if (onEdit != null) onEdit.accept(quiz);
    }

    @FXML
    private void handleDelete() {
        if (onDelete != null) onDelete.accept(quiz);
    }
}
