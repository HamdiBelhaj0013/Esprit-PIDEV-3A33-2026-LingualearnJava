package org.example.controller.tests;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.example.entity.tests.MockTest;
import org.example.service.tests.MockTestService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MockTestDashboardController implements Initializable {

    @FXML private Label totalTestsLabel;
    @FXML private Label totalQuestionsLabel;
    @FXML private Label totalResultsLabel;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> levelFilterCombo;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<MockTest>            testsTable;
    @FXML private TableColumn<MockTest, String>  colType;
    @FXML private TableColumn<MockTest, String>  colTitle;
    @FXML private TableColumn<MockTest, String>  colLanguage;
    @FXML private TableColumn<MockTest, String>  colLevel;
    @FXML private TableColumn<MockTest, Integer> colDuration;
    @FXML private TableColumn<MockTest, Integer> colNbQuestions;

    private StackPane       contentArea;
    private MockTestService service;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        service = new MockTestService();
        setupTable();
        setupFilters();
        loadData();
        loadStats();
    }

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }
    public MockTestService getService()               { return service; }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupTable() {
        colType.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getTestType()));
        colTitle.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getTitle()));
        colLanguage.setCellValueFactory(c -> {
            var lang = c.getValue().getPlatformLanguage();
            return new SimpleStringProperty(lang != null ? lang.getName() : "—");
        });
        colLevel.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getLevel()));
        colDuration.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getDurationMinutes()).asObject());
        colNbQuestions.setCellValueFactory(c -> new SimpleIntegerProperty(
                (int) service.countQuestionsByTest(c.getValue().getId())).asObject());

        testsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && testsTable.getSelectionModel().getSelectedItem() != null)
                navigateTo("/fxml/tests/TestQuestionManager.fxml", ctrl -> {
                    if (ctrl instanceof TestQuestionManagerController c)
                        c.init(service, testsTable.getSelectionModel().getSelectedItem(), this);
                });
        });
    }

    private void setupFilters() {
        typeFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "TOEFL", "IELTS", "DELF", "DALF", "TCF", "Cambridge"));
        typeFilterCombo.setValue("Tous");
        levelFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "A1", "A2", "B1", "B2", "C1", "C2"));
        levelFilterCombo.setValue("Tous");
        sortCombo.setItems(FXCollections.observableArrayList(
                "Type A→Z", "Titre A→Z", "Durée ↑", "Durée ↓", "Plus récent"));
        sortCombo.setValue("Type A→Z");

        searchField.textProperty().addListener((obs, o, n)     -> applyFiltersAndSort());
        typeFilterCombo.valueProperty().addListener((obs, o, n) -> applyFiltersAndSort());
        levelFilterCombo.valueProperty().addListener((obs, o, n)-> applyFiltersAndSort());
        sortCombo.valueProperty().addListener((obs, o, n)       -> applyFiltersAndSort());
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    public void loadData()  { applyFiltersAndSort(); }

    public void loadStats() {
        totalTestsLabel.setText(String.valueOf(service.countAll()));
        totalQuestionsLabel.setText(String.valueOf(service.countAllQuestions()));
        totalResultsLabel.setText(String.valueOf(service.countAllResults()));
    }

    private void applyFiltersAndSort() {
        String term  = searchField.getText();
        String type  = "Tous".equals(typeFilterCombo.getValue())  ? null : typeFilterCombo.getValue();
        String level = "Tous".equals(levelFilterCombo.getValue()) ? null : levelFilterCombo.getValue();
        String sort  = sortCombo.getValue();

        List<MockTest> result = (term != null && !term.isBlank())
                ? service.search(term)
                : service.filterAdvanced(type, level, null);

        if (sort != null) {
            result = switch (sort) {
                case "Titre A→Z"   -> service.sortByTitleAsc();
                case "Durée ↑"     -> service.sortByDurationAsc();
                case "Durée ↓"     -> service.sortByDurationDesc();
                case "Plus récent" -> service.sortByDateDesc();
                default            -> result;
            };
        }
        testsTable.setItems(FXCollections.observableArrayList(result));
    }

    // ── Actions CRUD ──────────────────────────────────────────────────────────

    @FXML private void handleNewTest(ActionEvent event) { openForm(null); }

    @FXML
    private void handleEdit(ActionEvent event) {
        MockTest sel = testsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Veuillez sélectionner un test à modifier."); return; }
        openForm(sel);
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        MockTest sel = testsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Veuillez sélectionner un test à supprimer."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer le test « " + sel.getTitle() + " » ?");
        confirm.setContentText("Toutes les questions associées seront également supprimées.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.delete(sel.getId());
                loadData(); loadStats();
                showSuccess("Le test « " + sel.getTitle() + " » a été supprimé.");
            } catch (Exception e) { showError("Erreur : " + e.getMessage()); }
        }
    }

    @FXML
    private void handleViewQuestions(ActionEvent event) {
        MockTest sel = testsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Veuillez sélectionner un test."); return; }
        navigateTo("/fxml/tests/TestQuestionManager.fxml", ctrl -> {
            if (ctrl instanceof TestQuestionManagerController c)
                c.init(service, sel, this);
        });
    }

    @FXML
    private void handleViewResults(ActionEvent event) {
        navigateTo("/fxml/tests/TestResultView.fxml", ctrl -> {
            if (ctrl instanceof TestResultViewController c)
                c.init(service, this);
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openForm(MockTest test) {
        navigateTo("/fxml/tests/MockTestForm.fxml", ctrl -> {
            if (ctrl instanceof MockTestFormController c)
                c.init(service, test, this);
        });
    }

    public void returnToDashboard() {
        navigateTo("/fxml/tests/MockTestDashboard.fxml", ctrl -> {
            if (ctrl instanceof MockTestDashboardController c)
                c.setContentArea(contentArea);
        });
    }

    private void navigateTo(String fxmlPath, java.util.function.Consumer<Object> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            setup.accept(loader.getController());
            if (contentArea != null) {
                // Mode intégré dans AdminMain — afficher dans le StackPane
                contentArea.getChildren().setAll(view);
            } else {
                // Mode standalone — remplacer toute la scène
                javafx.scene.Scene currentScene = testsTable.getScene();
                if (currentScene != null) {
                    javafx.stage.Stage stage = (javafx.stage.Stage) currentScene.getWindow();
                    javafx.scene.Scene newScene = new javafx.scene.Scene((javafx.scene.Parent) view);
                    newScene.getStylesheets().addAll(currentScene.getStylesheets());
                    stage.setScene(newScene);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur navigation : " + e.getClass().getSimpleName() + " : " + e.getMessage() + (e.getCause() != null ? " | Cause: " + e.getCause().getMessage() : ""));
        }
    }

    private void showInfo(String msg)    { alert(Alert.AlertType.INFORMATION, "Information", msg); }
    private void showSuccess(String msg) { alert(Alert.AlertType.INFORMATION, "Succès ✅", msg); }
    private void showError(String msg)   { alert(Alert.AlertType.ERROR, "Erreur", msg); }
    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}