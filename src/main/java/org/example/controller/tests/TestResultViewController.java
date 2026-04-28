package org.example.controller.tests;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestResult;
import org.example.service.tests.MockTestService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class TestResultViewController implements Initializable {

    @FXML private TableView<TestResult>           resultsTable;
    @FXML private TableColumn<TestResult, String> colUser;
    @FXML private TableColumn<TestResult, String> colTest;
    @FXML private TableColumn<TestResult, String> colScore;
    @FXML private TableColumn<TestResult, String> colAiScore;
    @FXML private TableColumn<TestResult, String> colDate;

    @FXML private ComboBox<MockTest> testFilterCombo;
    @FXML private TextField          userSearchField;
    @FXML private Label              totalLabel;
    @FXML private Label              avgScoreLabel;

    private MockTestService             service;
    private MockTestDashboardController dashboardController;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        userSearchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    public void init(MockTestService service, MockTestDashboardController dashboardController) {
        this.service             = service;
        this.dashboardController = dashboardController;
        loadTestFilter();
        loadAll();
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupTable() {
        colUser.setCellValueFactory(c -> {
            var u = c.getValue().getUser();
            return new SimpleStringProperty(
                    u != null ? u.getFullName() + " (" + u.getEmail() + ")" : "—");
        });

        colTest.setCellValueFactory(c -> {
            var t = c.getValue().getMockTest();
            return new SimpleStringProperty(
                    t != null ? t.getTitle() + " [" + t.getTestType() + "]" : "—");
        });

        // ── FIX #2 : Score affiché "X.X / 20  =  X%" ────────────────────────
        colScore.setCellValueFactory(c -> {
            float pct    = c.getValue().getOverallScore();
            float sur20  = Math.round(pct / 100f * 20f * 10f) / 10f;
            return new SimpleStringProperty(
                    String.format("%.1f / 20  =  %.0f%%", sur20, pct));
        });
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setAlignment(Pos.CENTER);
                try {
                    // Extraire le % pour colorer
                    String pctStr = item.substring(item.indexOf('=') + 1).replace("%", "").trim();
                    float  pct    = Float.parseFloat(pctStr);
                    if      (pct >= 75) setStyle("-fx-text-fill:#2fb344;-fx-font-weight:bold;");
                    else if (pct >= 50) setStyle("-fx-text-fill:#f59f00;-fx-font-weight:bold;");
                    else                setStyle("-fx-text-fill:#d63939;-fx-font-weight:bold;");
                } catch (Exception e) { setStyle(""); }
            }
        });

        colAiScore.setCellValueFactory(c -> {
            float pct   = c.getValue().getAiPredictedScore();
            float sur20 = Math.round(pct / 100f * 20f * 10f) / 10f;
            return new SimpleStringProperty(String.format("%.1f / 20", sur20));
        });

        colDate.setCellValueFactory(c -> {
            var d = c.getValue().getDateTaken();
            return new SimpleStringProperty(d != null ? d.format(FMT) : "—");
        });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadTestFilter() {
        List<MockTest> tests = service.findAll();
        testFilterCombo.setItems(FXCollections.observableArrayList(tests));
        testFilterCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(MockTest t) {
                return t != null ? t.getTitle() + " — " + t.getTestType() : "";
            }
            @Override public MockTest fromString(String s) { return null; }
        });
        testFilterCombo.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void loadAll() {
        List<TestResult> results = service.findAllResults();
        resultsTable.setItems(FXCollections.observableArrayList(results));
        updateStats(results);
    }

    private void applyFilters() {
        MockTest sel    = testFilterCombo.getValue();
        String   search = userSearchField.getText();
        List<TestResult> results = (sel != null)
                ? service.findResultsByTest(sel.getId())
                : service.findAllResults();
        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            results = results.stream()
                    .filter(r -> r.getUser() != null
                            && (r.getUser().getFullName().toLowerCase().contains(lower)
                            || r.getUser().getEmail().toLowerCase().contains(lower)))
                    .toList();
        }
        resultsTable.setItems(FXCollections.observableArrayList(results));
        updateStats(results);
    }

    private void updateStats(List<TestResult> results) {
        totalLabel.setText(results.size() + " résultat(s)");
        double avg    = results.stream().mapToDouble(TestResult::getOverallScore).average().orElse(0.0);
        double avgS20 = Math.round(avg / 100.0 * 20.0 * 10.0) / 10.0;
        avgScoreLabel.setText(String.format("Score moyen : %.1f / 20  (%.0f%%)", avgS20, avg));
    }

    // ── FIX #3 : handleViewReport — dialog rapport IA complet ────────────────

    @FXML
    private void handleViewReport(ActionEvent event) {
        TestResult sel = resultsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo("Veuillez sélectionner un résultat pour afficher le rapport IA.");
            return;
        }

        // ── Construire le contenu du rapport ─────────────────────────────────
        boolean hasContent = false;

        // Calculer les scores
        float pct   = sel.getOverallScore();
        float sur20 = Math.round(pct / 100f * 20f * 10f) / 10f;

        // ── Dialog principal ──────────────────────────────────────────────────
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Rapport IA — " + (sel.getMockTest() != null ? sel.getMockTest().getTitle() : "Test"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(640);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        // ── En-tête résumé ────────────────────────────────────────────────────
        VBox header = new VBox(6);
        header.setStyle("-fx-background-color:linear-gradient(to right,#1a1f36,#252d5c);" +
                "-fx-background-radius:12;-fx-padding:18 24;");

        Label userLbl = new Label(sel.getUser() != null ? sel.getUser().getFullName() : "Utilisateur");
        userLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");

        Label testLbl = new Label(sel.getMockTest() != null
                ? sel.getMockTest().getTitle() + "  [" + sel.getMockTest().getTestType() + "]"
                : "—");
        testLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.65);");

        Label dateLbl = new Label(sel.getDateTaken() != null
                ? "Passé le " + sel.getDateTaken().format(FMT) : "");
        dateLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.55);");

        // Score badge
        String scoreColor = pct >= 75 ? "#2fb344" : pct >= 50 ? "#f59f00" : "#d63939";
        Label scoreBadge = new Label(String.format("%.1f / 20  =  %.0f%%", sur20, pct));
        scoreBadge.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:" + scoreColor + ";");

        header.getChildren().addAll(userLbl, testLbl, dateLbl, scoreBadge);
        root.getChildren().add(header);

        // ── Rapport aiNote ────────────────────────────────────────────────────
        if (sel.getAiNote() != null && !sel.getAiNote().isBlank()) {
            hasContent = true;
            root.getChildren().add(buildSection(
                    "📝  Rapport temporel & commentaire général",
                    sel.getAiNote(),
                    "#1a1f36", "#f0f4ff", "#c5d0fc"));
        }

        // ── aiWeaknessReport ──────────────────────────────────────────────────
        if (sel.getAiWeaknessReport() != null && !sel.getAiWeaknessReport().isBlank()) {
            hasContent = true;
            root.getChildren().add(buildSection(
                    "⚠️  Points à améliorer",
                    formatJson(sel.getAiWeaknessReport()),
                    "#7c3d12", "#fff8e1", "#ffe082"));
        }

        // ── aiCorrection ──────────────────────────────────────────────────────
        if (sel.getAiCorrection() != null && !sel.getAiCorrection().isBlank()) {
            hasContent = true;
            root.getChildren().add(buildSection(
                    "✏️  Correction détaillée",
                    sel.getAiCorrection(),
                    "#1b5e20", "#f1f8f1", "#a5d6a7"));
        }

        // ── Aucun rapport disponible ──────────────────────────────────────────
        if (!hasContent) {
            Label empty = new Label("Aucun rapport IA n'est disponible pour ce résultat.");
            empty.setStyle("-fx-font-size:13px;-fx-text-fill:#6c7a99;-fx-font-style:italic;" +
                    "-fx-padding:20 0;");
            empty.setWrapText(true);
            root.getChildren().add(empty);
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(480);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-width:0;");

        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    /** Construit une section colorée avec titre + contenu texte. */
    private VBox buildSection(String titre, String contenu, String titleColor, String bgColor, String borderColor) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color:" + bgColor + ";-fx-background-radius:10;" +
                "-fx-border-color:" + borderColor + ";-fx-border-radius:10;" +
                "-fx-border-width:1;-fx-padding:16 20;");

        Label titlLbl = new Label(titre);
        titlLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + titleColor + ";");

        TextArea area = new TextArea(contenu);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(Math.max(80, Math.min(200, contenu.split("\n").length * 22)));
        area.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;" +
                "-fx-background-color:transparent;-fx-border-color:transparent;");

        box.getChildren().addAll(titlLbl, area);
        return box;
    }

    // ── Autres actions ────────────────────────────────────────────────────────

    @FXML
    private void handleResetFilter(ActionEvent event) {
        testFilterCombo.setValue(null);
        userSearchField.clear();
        loadAll();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (dashboardController != null) dashboardController.returnToDashboard();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatJson(String json) {
        if (json == null) return "";
        return json.replace("{", "").replace("}", "")
                .replace("[", "").replace("]", "")
                .replace("\"", "").replace(",", "\n• ").replace(":", " : ");
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}