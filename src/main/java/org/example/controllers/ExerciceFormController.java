package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.Exercice;
import org.example.entities.Quiz;
import org.example.services.ExerciceService;
import org.example.services.QuizService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExerciceFormController {

    @FXML private Label labelFormTitle;
    @FXML private ComboBox<Quiz> comboQuiz;
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea fieldQuestion;
    @FXML private ComboBox<String> comboDifficulty;
    @FXML private CheckBox  checkEnabled;
    @FXML private CheckBox  checkAiGenerated;
    @FXML private VBox      vboxOptions;
    @FXML private VBox      vboxSkills;
    @FXML private Button    btnSubmit;

    @FXML private Label errorQuiz;
    @FXML private Label errorType;
    @FXML private Label errorQuestion;
    @FXML private Label errorDifficulty;
    @FXML private Label errorOptions;

    private ToggleGroup correctOptionGroup = new ToggleGroup();
    private final ExerciceService exService = new ExerciceService();
    private final QuizService quizService = new QuizService();
    private Exercice editingEx = null;

    @FXML
    public void initialize() {
        comboQuiz.setItems(FXCollections.observableArrayList(quizService.getAllQuizzes()));
        comboType.setItems(FXCollections.observableArrayList("multiple_choice", "true_false", "fill_blank", "matching"));
        comboDifficulty.setItems(FXCollections.observableArrayList("1", "2", "3", "4", "5"));

        // Clear validation errors dynamically
        comboQuiz.valueProperty().addListener((obs, o, n) -> clearError(comboQuiz, errorQuiz));
        comboType.valueProperty().addListener((obs, o, n) -> clearError(comboType, errorType));
        comboDifficulty.valueProperty().addListener((obs, o, n) -> clearError(comboDifficulty, errorDifficulty));
        fieldQuestion.textProperty().addListener((obs, o, n) -> clearError(fieldQuestion, errorQuestion));
    }

    private void clearError(javafx.scene.control.Control field, Label label) {
        if (field != null) {
            field.getStyleClass().remove("text-field-error");
        }
        if (label != null) {
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    public void setExercice(Exercice ex) {
        this.editingEx = ex;
        if (ex != null) {
            labelFormTitle.setText("Edit Exercise #" + ex.getId());
            fieldQuestion.setText(ex.getQuestion());
            comboDifficulty.setValue(String.valueOf(ex.getDifficulty()));
            checkEnabled.setSelected(ex.isEnabled());
            checkAiGenerated.setSelected(ex.isAiGenerated());
            comboType.setValue(ex.getType());

            vboxOptions.getChildren().clear();
            correctOptionGroup = new ToggleGroup();
            parseJsonArray(ex.getOptions()).forEach(opt -> addOptionRow(opt, opt.equals(ex.getCorrectAnswer())));

            vboxSkills.getChildren().clear();
            parseJsonArray(ex.getSkillCodes()).forEach(this::addSkillRow);

            comboQuiz.getItems().stream()
                .filter(q -> q.getId() == ex.getQuizId())
                .findFirst().ifPresent(comboQuiz::setValue);
            
            btnSubmit.setText("Update Exercise");
        }
    }

    @FXML
    private void handleSubmit() {
        boolean valid = true;

        errorQuiz.setVisible(false); errorQuiz.setManaged(false);
        errorType.setVisible(false); errorType.setManaged(false);
        errorQuestion.setVisible(false); errorQuestion.setManaged(false);
        errorDifficulty.setVisible(false); errorDifficulty.setManaged(false);
        errorOptions.setVisible(false); errorOptions.setManaged(false);

        fieldQuestion.getStyleClass().remove("text-field-error");
        comboQuiz.getStyleClass().remove("text-field-error");
        comboType.getStyleClass().remove("text-field-error");
        comboDifficulty.getStyleClass().remove("text-field-error");

        if (comboQuiz.getValue() == null) {
            errorQuiz.setVisible(true); errorQuiz.setManaged(true);
            comboQuiz.getStyleClass().add("text-field-error");
            valid = false;
        }

        if (comboType.getValue() == null) {
            errorType.setVisible(true); errorType.setManaged(true);
            comboType.getStyleClass().add("text-field-error");
            valid = false;
        }

        if (fieldQuestion.getText() == null || fieldQuestion.getText().trim().isEmpty()) {
            errorQuestion.setVisible(true); errorQuestion.setManaged(true);
            fieldQuestion.getStyleClass().add("text-field-error");
            valid = false;
        }

        if (comboDifficulty.getValue() == null) {
            errorDifficulty.setVisible(true); errorDifficulty.setManaged(true);
            comboDifficulty.getStyleClass().add("text-field-error");
            valid = false;
        }

        String correctAnswer = getSelectedCorrectAnswer();
        if (correctAnswer == null || correctAnswer.trim().isEmpty() || vboxOptions.getChildren().isEmpty()) {
            errorOptions.setVisible(true); errorOptions.setManaged(true);
            valid = false;
        }

        if (!valid) return;

        Exercice ex = (editingEx == null) ? new Exercice() : editingEx;
        ex.setQuestion(fieldQuestion.getText());
        ex.setQuizId(comboQuiz.getValue().getId());
        ex.setType(comboType.getValue());
        ex.setDifficulty(parseInt(comboDifficulty.getValue()));
        ex.setEnabled(checkEnabled.isSelected());
        ex.setAiGenerated(checkAiGenerated.isSelected());
        ex.setOptions(getOptionsAsJson());
        ex.setCorrectAnswer(correctAnswer);
        ex.setSkillCodes(getSkillsAsJson());

        if (editingEx == null) exService.addExercice(ex);
        else exService.updateExercice(ex);

        goBack();
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ExerciceView.fxml"));
            Stage stage = (Stage) btnSubmit.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void addOptionLine() { addOptionRow("", false); }
    @FXML private void addSkillLine() { addSkillRow(""); }

    private void addOptionRow(String text, boolean isCorrect) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        RadioButton rb = new RadioButton();
        rb.setToggleGroup(correctOptionGroup);
        rb.setSelected(isCorrect);
        TextField tf = new TextField(text);
        tf.getStyleClass().add("text-field-modern");
        HBox.setHgrow(tf, Priority.ALWAYS);
        Button btnDel = new Button("✕");
        btnDel.setStyle("-fx-background-color: #FFF5F5; -fx-text-fill: #DC3545; -fx-padding: 8 12; -fx-background-radius: 8;");
        btnDel.setOnAction(e -> vboxOptions.getChildren().remove(row));
        row.getChildren().addAll(rb, tf, btnDel);
        vboxOptions.getChildren().add(row);
    }

    private void addSkillRow(String value) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField tf = new TextField(value);
        tf.getStyleClass().add("text-field-modern");
        HBox.setHgrow(tf, Priority.ALWAYS);
        Button btnDel = new Button("✕");
        btnDel.setStyle("-fx-background-color: #FFF5F5; -fx-text-fill: #DC3545; -fx-padding: 8 12; -fx-background-radius: 8;");
        btnDel.setOnAction(e -> vboxSkills.getChildren().remove(row));
        row.getChildren().addAll(tf, btnDel);
        vboxSkills.getChildren().add(row);
    }

    private String getSelectedCorrectAnswer() {
        for (javafx.scene.Node node : vboxOptions.getChildren()) {
            HBox row = (HBox) node;
            RadioButton rb = (RadioButton) row.getChildren().get(0);
            if (rb.isSelected()) return ((TextField) row.getChildren().get(1)).getText().trim();
        }
        return null;
    }

    private String getOptionsAsJson() {
        List<String> opts = vboxOptions.getChildren().stream()
            .map(node -> ((TextField) ((HBox) node).getChildren().get(1)).getText().trim())
            .filter(s -> !s.isEmpty()).collect(Collectors.toList());
        return listToJson(opts);
    }

    private String getSkillsAsJson() {
        List<String> skills = vboxSkills.getChildren().stream()
            .map(node -> ((TextField) ((HBox) node).getChildren().get(0)).getText().trim())
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
        String[] items = content.split("\",\"");
        for (String s : items) list.add(s.replace("\"", ""));
        return list;
    }

    private int parseInt(String val) { try { return Integer.parseInt(val.trim()); } catch (Exception e) { return 0; } }
}
