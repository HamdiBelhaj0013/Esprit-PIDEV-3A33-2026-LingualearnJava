package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.entities.Exercice;
import org.example.service.QuizService;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

public class ExerciceCardController {

    @FXML private Label labelId;
    @FXML private Label labelType;
    @FXML private Label labelQuestion;
    @FXML private Label labelAnswer;
    @FXML private Label labelQuiz;
    @FXML private HBox diffContainer;
    @FXML private Label labelStatus;
    @FXML private Label labelRating;
    
    @FXML private javafx.scene.control.Button btnEdit;
    @FXML private javafx.scene.control.Button btnDelete;

    private Exercice exercice;
    private Consumer<Exercice> onView;
    private Consumer<Exercice> onEdit;
    private Consumer<Exercice> onDelete;

    private final QuizService quizService = new QuizService();
    private final org.example.service.RatingService ratingService = new org.example.service.RatingService();

    public void setData(Exercice ex, Consumer<Exercice> onView, Consumer<Exercice> onEdit, Consumer<Exercice> onDelete) {
        this.exercice = ex;
        this.onView = onView;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        labelId.setText("#" + ex.getId());
        labelQuestion.setText(ex.getQuestion());
        labelAnswer.setText(ex.getCorrectAnswer());

        // Type badge
        String typeLabel = ex.getType().toUpperCase().replace("_", " ");
        labelType.setText(typeLabel);

        // Difficulty stars
        diffContainer.getChildren().clear();
        for (int i = 0; i < 5; i++) {
            FontIcon star = new FontIcon("fas-star");
            star.setIconSize(12);
            if (i < ex.getDifficulty()) {
                star.setIconColor(javafx.scene.paint.Color.web("#f59e0b"));
            } else {
                star.setIconColor(javafx.scene.paint.Color.web("#cbd5e1"));
            }
            diffContainer.getChildren().add(star);
        }

        // Status
        if (ex.isEnabled()) {
            labelStatus.setText("● Active");
            labelStatus.getStyleClass().setAll("badge-active");
        } else {
            labelStatus.setText("● Inactive");
            labelStatus.getStyleClass().setAll("badge-inactive");
        }

        // Average Rating
        double avg = ratingService.getAverageRating(ex.getId());
        labelRating.setText(String.format("%.1f", avg));

        // Quiz name
        quizService.getAllQuizzes().stream()
                .filter(q -> q.getId() == ex.getQuizId())
                .findFirst()
                .ifPresent(q -> labelQuiz.setText(q.getTitle()));

        if (!org.example.util.UserSession.isAdmin) {
            if (btnEdit != null) { btnEdit.setVisible(false); btnEdit.setManaged(false); }
            if (btnDelete != null) { btnDelete.setVisible(false); btnDelete.setManaged(false); }
        }
    }

    @FXML
    private void handleView() {
        if (onView != null) onView.accept(exercice);
    }

    @FXML
    private void handleEdit() {
        if (onEdit != null) onEdit.accept(exercice);
    }

    @FXML
    private void handleDelete() {
        if (onDelete != null) onDelete.accept(exercice);
    }
}
