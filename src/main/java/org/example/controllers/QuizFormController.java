package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.Lesson;
import org.example.entities.Quiz;
import org.example.services.LessonService;
import org.example.services.QuizService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuizFormController {

    @FXML private Label    labelFormTitle;
    @FXML private TextField fieldTitle;
    @FXML private TextArea  fieldDescription;
    @FXML private TextField fieldPassingScore;
    @FXML private TextField fieldQuestionCount;
    @FXML private ComboBox<String>  comboDifficulty;
    @FXML private ComboBox<Lesson>  comboLesson;
    @FXML private VBox      vboxSkills;
    @FXML private CheckBox  checkEnabled;
    @FXML private Button    btnSubmit;

    // Validation error labels
    @FXML private Label errorTitle;
    @FXML private Label errorDifficulty;
    @FXML private Label errorPassingScore;
    @FXML private Label errorQuestionCount;

    private final QuizService    quizService    = new QuizService();
    private final LessonService  lessonService  = new LessonService();
    private Quiz editingQuiz = null;

    @FXML
    public void initialize() {
        comboLesson.setItems(FXCollections.observableArrayList(lessonService.getAllLessons()));

        comboDifficulty.setItems(FXCollections.observableArrayList(
            "⭐  Level 1 — Beginner",
            "⭐⭐  Level 2 — Elementary",
            "⭐⭐⭐  Level 3 — Intermediate",
            "⭐⭐⭐⭐  Level 4 — Advanced",
            "⭐⭐⭐⭐⭐  Level 5 — Expert"
        ));

        // Real-time validation — clear error on typing
        fieldTitle.textProperty().addListener((obs, o, n) -> clearError(fieldTitle, errorTitle));
        fieldPassingScore.textProperty().addListener((obs, o, n) -> clearError(fieldPassingScore, errorPassingScore));
        fieldQuestionCount.textProperty().addListener((obs, o, n) -> clearError(fieldQuestionCount, errorQuestionCount));
        comboDifficulty.valueProperty().addListener((obs, o, n) -> { if (n != null) clearError(null, errorDifficulty); });
    }

    public void setQuiz(Quiz quiz) {
        this.editingQuiz = quiz;
        if (quiz != null) {
            labelFormTitle.setText("✏️  Edit Quiz: " + quiz.getTitle());
            fieldTitle.setText(quiz.getTitle());
            fieldDescription.setText(quiz.getDescription());
            fieldPassingScore.setText(String.valueOf(quiz.getPassingScore()));
            fieldQuestionCount.setText(String.valueOf(quiz.getQuestionCount()));
            checkEnabled.setSelected(quiz.isEnabled());

            // Set difficulty combo (index = difficulty - 1)
            int diff = Math.max(1, Math.min(5, quiz.getDifficulty()));
            comboDifficulty.getSelectionModel().select(diff - 1);

            vboxSkills.getChildren().clear();
            parseJsonArray(quiz.getSkillCodes()).forEach(this::addSkillRow);

            if (quiz.getLessonId() != null) {
                comboLesson.getItems().stream()
                        .filter(l -> l.getId() == quiz.getLessonId())
                        .findFirst().ifPresent(comboLesson::setValue);
            }
            btnSubmit.setText("💾  Update Quiz");
        }
    }

    // ─── VALIDATION ───────────────────────────────────────────
    private boolean validate() {
        boolean valid = true;

        // Title
        if (fieldTitle.getText() == null || fieldTitle.getText().trim().isEmpty()) {
            showError(fieldTitle, errorTitle);
            valid = false;
        }

        // Difficulty
        if (comboDifficulty.getValue() == null) {
            showError(null, errorDifficulty);
            valid = false;
        }

        // Passing score
        String ps = fieldPassingScore.getText();
        if (ps == null || ps.trim().isEmpty() || !isValidInt(ps, 0, 100)) {
            showError(fieldPassingScore, errorPassingScore);
            valid = false;
        }

        // Question count
        String qc = fieldQuestionCount.getText();
        if (qc == null || qc.trim().isEmpty() || !isValidInt(qc, 1, 9999)) {
            showError(fieldQuestionCount, errorQuestionCount);
            valid = false;
        }

        return valid;
    }

    private void showError(TextField field, Label label) {
        if (field != null) {
            field.getStyleClass().removeAll("text-field-modern");
            field.getStyleClass().setAll("text-field-modern", "text-field-error");
        }
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearError(TextField field, Label label) {
        if (field != null) {
            field.getStyleClass().removeAll("text-field-error");
            if (!field.getStyleClass().contains("text-field-modern"))
                field.getStyleClass().add("text-field-modern");
        }
        label.setVisible(false);
        label.setManaged(false);
    }

    private boolean isValidInt(String val, int min, int max) {
        try {
            int v = Integer.parseInt(val.trim());
            return v >= min && v <= max;
        } catch (NumberFormatException e) { return false; }
    }

    // ─── SUBMIT ───────────────────────────────────────────────
    @FXML
    private void handleSubmit() {
        if (!validate()) return;

        Quiz quiz = (editingQuiz == null) ? new Quiz() : editingQuiz;
        quiz.setTitle(fieldTitle.getText().trim());
        quiz.setDescription(fieldDescription.getText());
        quiz.setPassingScore(parseInt(fieldPassingScore.getText()));
        quiz.setQuestionCount(parseInt(fieldQuestionCount.getText()));
        quiz.setEnabled(checkEnabled.isSelected());
        quiz.setDifficulty(comboDifficulty.getSelectionModel().getSelectedIndex() + 1);
        quiz.setLessonId(comboLesson.getValue() != null ? comboLesson.getValue().getId() : null);
        quiz.setSkillCodes(getSkillsAsJson());

        if (editingQuiz == null) quizService.addQuiz(quiz);
        else quizService.updateQuiz(quiz);

        goBack();
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/QuizView.fxml"));
            Stage stage = (Stage) btnSubmit.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ─── SKILLS ───────────────────────────────────────────────
    @FXML
    private void addSkillLine() { addSkillRow(""); }

    private void addSkillRow(String value) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #f8f9fb; -fx-background-radius: 10; -fx-padding: 8 12;");

        Label icon = new Label("🏷");
        icon.setStyle("-fx-font-size: 14px;");

        TextField tf = new TextField(value);
        tf.setPromptText("e.g. grammar, vocabulary, listening...");
        tf.getStyleClass().setAll("text-field-modern");
        tf.setStyle("-fx-background-color: white;");
        HBox.setHgrow(tf, Priority.ALWAYS);

        Button btnDel = new Button("✕");
        btnDel.getStyleClass().setAll("btn-icon-danger");
        btnDel.setStyle("-fx-font-size: 13px; -fx-padding: 6 10;");
        btnDel.setOnAction(e -> vboxSkills.getChildren().remove(row));

        row.getChildren().addAll(icon, tf, btnDel);
        vboxSkills.getChildren().add(row);
    }

    private String getSkillsAsJson() {
        List<String> skills = vboxSkills.getChildren().stream()
                .filter(n -> n instanceof HBox)
                .map(n -> ((TextField) ((HBox) n).getChildren().get(1)).getText().trim())
                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
        return listToJson(skills);
    }

    private String listToJson(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<String> parseJsonArray(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.length() < 2) return list;
        String content = json.substring(1, json.length() - 1);
        if (content.isEmpty()) return list;
        for (String s : content.split("\",\"")) list.add(s.replace("\"", ""));
        return list;
    }

    private int parseInt(String val) {
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return 0; }
    }
}
