package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.Lesson;
import org.example.entities.Quiz;
import org.example.service.LessonService;
import org.example.service.QuizService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class QuizController {

    @FXML private FlowPane quizGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<Lesson> filterLesson;
    @FXML private ComboBox<String> comboSort;
    @FXML private Label labelTotalCount;
    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statInactive;
    @FXML private Button btnAddNew;
    @FXML private Label headerTitle;
    @FXML private Label headerSubtitle;

    private final QuizService quizService = new QuizService();
    private final LessonService lessonService = new LessonService();
    private final ObservableList<Quiz> quizList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!org.example.util.UserSession.isAdmin) {
            if (btnAddNew != null) {
                btnAddNew.setVisible(false);
                btnAddNew.setManaged(false);
            }
            if (headerTitle != null) headerTitle.setText("Quiz Catalog");
            if (headerSubtitle != null) headerSubtitle.setText("Browse and practice available language quizzes");
        }
        
        loadData();
        setupFilters();

        searchField.textProperty().addListener((obs, oldV, newV) -> filterQuizzes());
        filterStatus.valueProperty().addListener((obs, oldV, newV) -> filterQuizzes());
        filterLesson.valueProperty().addListener((obs, oldV, newV) -> filterQuizzes());
        if (comboSort != null) {
            comboSort.valueProperty().addListener((obs, oldV, newV) -> filterQuizzes());
        }
    }

    private void loadData() {
        quizList.setAll(quizService.getAllQuizzes());
        filterLesson.setItems(FXCollections.observableArrayList(lessonService.getAllLessons()));
        updateStats();
        renderGrid(quizList);
    }

    private void updateStats() {
        long total = quizList.size();
        long active = quizList.stream().filter(Quiz::isEnabled).count();
        long inactive = total - active;

        statTotal.setText(String.valueOf(total));
        statActive.setText(String.valueOf(active));
        statInactive.setText(String.valueOf(inactive));
    }

    private void setupFilters() {
        filterStatus.setItems(FXCollections.observableArrayList("All Status", "Active", "Inactive"));
        filterStatus.setValue("All Status");
        
        if (comboSort != null) {
            comboSort.setItems(FXCollections.observableArrayList(
                "Title (A-Z)", "Title (Z-A)", "Difficulty (Low to High)", "Difficulty (High to Low)"
            ));
            comboSort.setValue("Title (A-Z)");
        }
    }

    private void renderGrid(List<Quiz> quizzes) {
        quizGrid.getChildren().clear();
        for (Quiz q : quizzes) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QuizCard.fxml"));
                Parent card = loader.load();
                QuizCardController cardCtrl = loader.getController();
                
                if (org.example.util.UserSession.isAdmin) {
                    cardCtrl.setData(q, this::viewQuiz, this::goToEditForm, this::confirmDelete);
                } else {
                    cardCtrl.setData(q, this::viewQuiz, null, null);
                }
                
                quizGrid.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        labelTotalCount.setText("Showing " + quizzes.size() + " of " + quizList.size() + " quizzes");
    }

    private void filterQuizzes() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String status = filterStatus.getValue();
        Lesson lesson = filterLesson.getValue();

        List<Quiz> filtered = quizList.stream()
            .filter(q -> q.getTitle().toLowerCase().contains(search)
                      || (q.getDescription() != null && q.getDescription().toLowerCase().contains(search)))
            .filter(q -> {
                if (status == null || "All Status".equals(status)) return true;
                return "Active".equals(status) ? q.isEnabled() : !q.isEnabled();
            })
            .filter(q -> lesson == null || (q.getLessonId() != null && q.getLessonId() == lesson.getId()))
            .collect(Collectors.toList());

        // Apply Sorting
        if (comboSort != null && comboSort.getValue() != null) {
            switch (comboSort.getValue()) {
                case "Title (A-Z)":
                    filtered.sort((q1, q2) -> q1.getTitle().compareToIgnoreCase(q2.getTitle()));
                    break;
                case "Title (Z-A)":
                    filtered.sort((q1, q2) -> q2.getTitle().compareToIgnoreCase(q1.getTitle()));
                    break;
                case "Difficulty (Low to High)":
                    filtered.sort((q1, q2) -> Integer.compare(q1.getDifficulty(), q2.getDifficulty()));
                    break;
                case "Difficulty (High to Low)":
                    filtered.sort((q1, q2) -> Integer.compare(q2.getDifficulty(), q1.getDifficulty()));
                    break;
            }
        }

        renderGrid(filtered);
    }

    @FXML
    private void goToCreateForm() { navigateToForm(null); }
    private void goToEditForm(Quiz quiz) { navigateToForm(quiz); }
    
    private void viewQuiz(Quiz quiz) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QuizDetailView.fxml"));
            Parent root = loader.load();
            QuizDetailController ctrl = loader.getController();
            ctrl.setQuiz(quiz);
            Stage stage = (Stage) quizGrid.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void navigateToForm(Quiz quiz) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QuizForm.fxml"));
            Parent root = loader.load();
            QuizFormController ctrl = loader.getController();
            ctrl.setQuiz(quiz);
            Stage stage = (Stage) quizGrid.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void confirmDelete(Quiz quiz) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to delete the quiz \"" + quiz.getTitle() + "\"?\nThis action cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Quiz");
        alert.setHeaderText("⚠️  Confirm Deletion");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                quizService.deleteQuiz(quiz.getId());
                loadData();
            }
        });
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        filterStatus.setValue("All Status");
        filterLesson.setValue(null);
    }
}
