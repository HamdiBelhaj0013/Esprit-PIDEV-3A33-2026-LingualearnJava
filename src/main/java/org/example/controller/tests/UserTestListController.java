package org.example.controller.tests;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.controller.UserDashboardController;
import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.PlatformLanguage;
import org.example.entity.tests.TestResult;
import org.example.service.tests.MockTestService;
import org.example.service.tests.TestResultService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserTestListController implements Initializable {

    @FXML private Label            userNameLabel;
    @FXML private Label            totalTestsLabel;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> levelFilterCombo;

    @FXML private TableView<MockTest>            testsTable;
    @FXML private TableColumn<MockTest, String>  colType;
    @FXML private TableColumn<MockTest, String>  colTitle;
    @FXML private TableColumn<MockTest, String>  colLanguage;
    @FXML private TableColumn<MockTest, String>  colLevel;
    @FXML private TableColumn<MockTest, Integer> colDuration;
    @FXML private TableColumn<MockTest, Integer> colNbQuestions;
    @FXML private TableColumn<MockTest, String>  colLastScore;
    @FXML private TableColumn<MockTest, String>  colAverageScore;

    private MockTestService         service;
    private TestResultService       resultService;
    private User                    currentUser;
    private UserDashboardController dashboardController;
    private Stage                   currentStage;
    private List<TestResult>        cachedResults;
    private Runnable                onBack;
    private Scene                   originalScene; // ← NOUVEAU
    private String                  originalTitle; // ← NOUVEAU

    private PlatformLanguage filterLanguage  = null;
    private String[]         filterLevels    = null;
    private String           filterLevelName = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupFilters();
    }

    public void setOnBack(Runnable onBack) { this.onBack = onBack; }

    // ← NOUVEAU
    public void setOriginalScene(Scene scene, String title) {
        this.originalScene = scene;
        this.originalTitle = title;
    }

    public void init(MockTestService service, User user,
                     UserDashboardController dashboardController, Stage stage) {
        this.service             = service;
        this.currentUser         = user;
        this.dashboardController = dashboardController;
        this.currentStage        = stage;
        this.resultService       = new TestResultService();

        loadResultsCache();
        userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
        loadData();
    }

    public void initWithFilter(MockTestService service, User user,
                               UserDashboardController dashboardController,
                               Stage stage, PlatformLanguage language,
                               String[] levels, String levelName) {
        this.filterLanguage  = language;
        this.filterLevels    = levels;
        this.filterLevelName = levelName;
        init(service, user, dashboardController, stage);
    }

    public UserDashboardController getDashboardController() { return dashboardController; }
    public PlatformLanguage        getFilterLanguage()      { return filterLanguage; }
    public String[]                getFilterLevels()        { return filterLevels; }
    public String                  getFilterLevelName()     { return filterLevelName; }
    public Runnable                getOnBack()              { return onBack; }

    private void loadResultsCache() {
        try { cachedResults = resultService.findByUserId(currentUser.getId()); }
        catch (Exception e) { cachedResults = List.of(); }
    }

    public void refreshData() {
        try { cachedResults = resultService.findByUserId(currentUser.getId()); }
        catch (Exception e) { cachedResults = List.of(); }
        loadData();
        testsTable.refresh();
    }

    private void setupTable() {
        colType.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getTestType()));
        colTitle.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getTitle()));
        colLanguage.setCellValueFactory(c -> {
            var lang = c.getValue().getPlatformLanguage();
            return new SimpleStringProperty(lang != null ? lang.getName() : "—");
        });
        colLevel.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getLevel()));
        colDuration.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getDurationMinutes()).asObject());
        colNbQuestions.setCellValueFactory(c -> new SimpleIntegerProperty(
                (int) service.countQuestionsByTest(c.getValue().getId())).asObject());

        colLastScore.setCellValueFactory(c ->
                new SimpleObjectProperty<>(formatScore(getLastScoreFromCache(c.getValue().getId()))));
        colLastScore.setCellFactory(col -> buildScoreCell());

        colAverageScore.setCellValueFactory(c ->
                new SimpleObjectProperty<>(formatScore(getAverageScoreFromCache(c.getValue().getId()))));
        colAverageScore.setCellFactory(col -> buildScoreCell());

        testsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && testsTable.getSelectionModel().getSelectedItem() != null)
                openTestPreview(testsTable.getSelectionModel().getSelectedItem());
        });
    }

    private TableCell<MockTest, String> buildScoreCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setAlignment(Pos.CENTER);
                if ("—".equals(item)) {
                    setStyle("-fx-text-fill: #9ca3af;");
                } else {
                    try {
                        int s = Integer.parseInt(item.replace("%", ""));
                        if      (s >= 75) setStyle("-fx-text-fill: #2fb344; -fx-font-weight: bold;");
                        else if (s >= 50) setStyle("-fx-text-fill: #f59f00; -fx-font-weight: bold;");
                        else              setStyle("-fx-text-fill: #d63939; -fx-font-weight: bold;");
                    } catch (NumberFormatException ex) {
                        setStyle("-fx-text-fill: #9ca3af;");
                    }
                }
            }
        };
    }

    private Float getLastScoreFromCache(Long testId) {
        if (cachedResults == null) return null;
        return cachedResults.stream()
                .filter(r -> r.getMockTest() != null && r.getMockTest().getId().equals(testId))
                .findFirst().map(TestResult::getOverallScore).orElse(null);
    }

    private Float getAverageScoreFromCache(Long testId) {
        if (cachedResults == null) return null;
        List<Float> scores = cachedResults.stream()
                .filter(r -> r.getMockTest() != null && r.getMockTest().getId().equals(testId))
                .map(TestResult::getOverallScore).toList();
        if (scores.isEmpty()) return null;
        return (float) Math.round(
                scores.stream().mapToDouble(Float::doubleValue).average().orElse(0));
    }

    private String formatScore(Float score) {
        return score == null ? "—" : Math.round(score) + "%";
    }

    private void setupFilters() {
        typeFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "TOEFL", "IELTS", "DELF", "DALF", "TCF", "Cambridge"));
        typeFilterCombo.setValue("Tous");
        levelFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "A1", "A2", "B1", "B2", "C1", "C2"));
        levelFilterCombo.setValue("Tous");
        searchField.textProperty().addListener((obs, o, n)      -> applyFilters());
        typeFilterCombo.valueProperty().addListener((obs, o, n)  -> applyFilters());
        levelFilterCombo.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void loadData() {
        List<MockTest> tests;
        if (filterLanguage != null && filterLevels != null) {
            final String[] levels = filterLevels;
            tests = service.filterByLanguageId(filterLanguage.getId()).stream()
                    .filter(t -> {
                        for (String lvl : levels) if (lvl.equals(t.getLevel())) return true;
                        return false;
                    }).toList();
        } else {
            tests = service.findAll();
        }
        testsTable.setItems(FXCollections.observableArrayList(tests));
        totalTestsLabel.setText(tests.size() + " test(s) disponible(s)");
    }

    private void applyFilters() {
        String term  = searchField.getText();
        String type  = "Tous".equals(typeFilterCombo.getValue())  ? null : typeFilterCombo.getValue();
        String level = "Tous".equals(levelFilterCombo.getValue()) ? null : levelFilterCombo.getValue();
        List<MockTest> result;

        if (filterLanguage != null && filterLevels != null) {
            final String[] levels = filterLevels;
            result = service.filterByLanguageId(filterLanguage.getId()).stream()
                    .filter(t -> { for (String lvl : levels) if (lvl.equals(t.getLevel())) return true; return false; })
                    .filter(t -> type == null || type.equals(t.getTestType()))
                    .filter(t -> term == null || term.isBlank()
                            || t.getTitle().toLowerCase().contains(term.toLowerCase()))
                    .toList();
        } else {
            result = (term != null && !term.isBlank())
                    ? service.search(term)
                    : service.filterAdvanced(type, level, null);
        }
        testsTable.setItems(FXCollections.observableArrayList(result));
        totalTestsLabel.setText(result.size() + " test(s) trouvé(s)");
    }

    @FXML
    private void handleViewTest(ActionEvent event) {
        MockTest sel = testsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Veuillez sélectionner un test."); return; }
        openTestPreview(sel);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (filterLanguage != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/tests/LevelSelectView.fxml"));
                Parent root = loader.load();
                LevelSelectController ctrl = loader.getController();
                ctrl.init(service, currentUser, dashboardController, currentStage, filterLanguage);
                ctrl.setOnBack(onBack);
                ctrl.setOriginalScene(originalScene, originalTitle); // ← NOUVEAU
                Scene scene = new Scene(root);
                scene.getStylesheets().add(
                        getClass().getResource("/css/style.css").toExternalForm());
                currentStage.setScene(scene);
                currentStage.setTitle("LinguaLearn — Niveau");
            } catch (IOException e) {
                System.err.println("Erreur retour niveau : " + e.getMessage());
            }
        } else {
            if (onBack != null) {
                // ← NOUVEAU : restaure la scène originale avant d'appeler onBack
                if (originalScene != null) {
                    currentStage.setScene(originalScene);
                    currentStage.setTitle(originalTitle != null ? originalTitle : "LinguaLearn");
                }
                onBack.run();
            } else if (dashboardController != null) {
                dashboardController.returnToDashboard();
            }
        }
    }

    private void openTestPreview(MockTest test) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/TestPreviewView.fxml"));
            Parent root = loader.load();
            TestPreviewController ctrl = loader.getController();
            ctrl.init(service, test, currentUser, this, currentStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("LinguaLearn — Aperçu : " + test.getTitle());
        } catch (IOException e) {
            showError("Impossible d'ouvrir l'aperçu : " + e.getMessage());
        }
    }

    public void returnToList() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestListView.fxml"));
            Parent root = loader.load();
            UserTestListController ctrl = loader.getController();
            ctrl.init(service, currentUser, dashboardController, currentStage);
            ctrl.setOnBack(onBack);
            ctrl.setOriginalScene(originalScene, originalTitle); // ← NOUVEAU
            ctrl.refreshData();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("LinguaLearn — Tests de Certification");
        } catch (IOException e) {
            showError("Erreur retour : " + e.getMessage());
        }
    }

    private void showInfo(String msg)  { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR,       msg, ButtonType.OK).showAndWait(); }
}