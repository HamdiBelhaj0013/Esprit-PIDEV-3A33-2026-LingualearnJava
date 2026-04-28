package org.example.controller.tests;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestQuestion;
import org.example.service.tests.MockTestService;
import org.example.service.tests.ApiNinjasService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TestQuestionManagerController implements Initializable {

    // ── En-tête ───────────────────────────────────────────────────────────────
    @FXML private Label testTitleLabel;
    @FXML private Label questionCountLabel;
    @FXML private Label totalPointsLabel;

    // ── Panels StackPane (form / list) ────────────────────────────────────────
    @FXML private VBox formPanel;   // le formulaire plein écran
    @FXML private VBox listPanel;   // la liste des questions

    // ── Table (dans listPanel) ────────────────────────────────────────────────
    @FXML private TableView<TestQuestion>            questionsTable;
    @FXML private TableColumn<TestQuestion, String>  colSection;
    @FXML private TableColumn<TestQuestion, String>  colQuestion;
    @FXML private TableColumn<TestQuestion, String>  colAnswer;
    @FXML private TableColumn<TestQuestion, Integer> colPoints;

    // ── Boutons de section ────────────────────────────────────────────────────
    @FXML private ToggleButton btnReading;
    @FXML private ToggleButton btnListening;
    @FXML private ToggleButton btnWriting;
    @FXML private ToggleButton btnSpeaking;

    // ── Zones dynamiques ──────────────────────────────────────────────────────
    @FXML private VBox placeholderBox;
    @FXML private VBox readingForm;
    @FXML private VBox listeningForm;
    @FXML private VBox writingForm;
    @FXML private VBox speakingForm;

    // ── Champs Reading ────────────────────────────────────────────────────────
    @FXML private TextArea         questionTextArea;
    @FXML private VBox             optionsContainer;
    @FXML private VBox             correctAnswersContainer;
    @FXML private Spinner<Integer> pointsSpinner;

    // ── Champs IA ─────────────────────────────────────────────────────────────
    @FXML private Spinner<Integer> pointsSpinnerListening;
    @FXML private Spinner<Integer> pointsSpinnerWriting;
    @FXML private Spinner<Integer> pointsSpinnerSpeaking;

    // ── ComboBox caché (compatibilité) ────────────────────────────────────────
    @FXML private ComboBox<String> sectionCombo;

    // ── Labels erreurs ────────────────────────────────────────────────────────
    @FXML private Label panelTitleLabel;
    @FXML private Label sectionError;
    @FXML private Label questionTextError;
    @FXML private Label optionsError;
    @FXML private Label answerError;
    @FXML private Label globalError;
    @FXML private Label globalSuccess;

    // ── État ──────────────────────────────────────────────────────────────────
    private MockTestService             service;
    private MockTest                    currentTest;
    private TestQuestion                editingQuestion = null;
    private MockTestDashboardController dashboardController;
    private String                      currentSection  = null;

    private final List<TextField> optionFields = new ArrayList<>();
    private final List<CheckBox>  answerChecks = new ArrayList<>();
    private final ToggleGroup     sectionToggleGroup = new ToggleGroup();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupSpinners();
        setupSectionButtons();
        // Pas de listener automatique sur la sélection —
        // la sélection sert uniquement pour Modifier/Supprimer
    }

    // ── Spinners ──────────────────────────────────────────────────────────────

    private void setupSpinners() {
        pointsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        pointsSpinner.setEditable(true);

        pointsSpinnerListening.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        pointsSpinnerListening.setEditable(true);

        pointsSpinnerWriting.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        pointsSpinnerWriting.setEditable(true);

        pointsSpinnerSpeaking.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        pointsSpinnerSpeaking.setEditable(true);
    }

    // ── Section buttons ───────────────────────────────────────────────────────

    private void setupSectionButtons() {
        btnReading.setToggleGroup(sectionToggleGroup);
        btnListening.setToggleGroup(sectionToggleGroup);
        btnWriting.setToggleGroup(sectionToggleGroup);
        btnSpeaking.setToggleGroup(sectionToggleGroup);

        sectionToggleGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle == null) {
                showPlaceholder();
                currentSection = null;
                return;
            }
            if      (newToggle == btnReading)   switchToSection("Reading");
            else if (newToggle == btnListening) switchToSection("Listening");
            else if (newToggle == btnWriting)   switchToSection("Writing");
            else if (newToggle == btnSpeaking)  switchToSection("Speaking");
        });
    }

    private void switchToSection(String section) {
        currentSection = section;
        if (sectionCombo != null) sectionCombo.setValue(section);

        showPlaceholder();   // masque tout
        switch (section) {
            case "Reading"   -> {
                readingForm.setVisible(true);  readingForm.setManaged(true);
                placeholderBox.setVisible(false); placeholderBox.setManaged(false);
                if (optionFields.isEmpty()) { addOptionField(""); addOptionField(""); }
                rebuildAnswerChecks();
            }
            case "Listening" -> {
                listeningForm.setVisible(true); listeningForm.setManaged(true);
                placeholderBox.setVisible(false); placeholderBox.setManaged(false);
            }
            case "Writing"   -> {
                writingForm.setVisible(true);   writingForm.setManaged(true);
                placeholderBox.setVisible(false); placeholderBox.setManaged(false);
            }
            case "Speaking"  -> {
                speakingForm.setVisible(true);  speakingForm.setManaged(true);
                placeholderBox.setVisible(false); placeholderBox.setManaged(false);
            }
        }
        updateSectionButtonStyles(section);
        clearAllErrors();
    }

    private void showPlaceholder() {
        if (placeholderBox   != null) { placeholderBox.setVisible(true);   placeholderBox.setManaged(true); }
        if (readingForm      != null) { readingForm.setVisible(false);     readingForm.setManaged(false); }
        if (listeningForm    != null) { listeningForm.setVisible(false);   listeningForm.setManaged(false); }
        if (writingForm      != null) { writingForm.setVisible(false);     writingForm.setManaged(false); }
        if (speakingForm     != null) { speakingForm.setVisible(false);    speakingForm.setManaged(false); }
    }

    private void updateSectionButtonStyles(String active) {
        resetToggleStyle(btnReading,   "#eef2ff", "#3b5bdb", "#c5d0fc");
        resetToggleStyle(btnListening, "#fff3e0", "#e65100", "#ffcc80");
        resetToggleStyle(btnWriting,   "#e8f5e9", "#2e7d32", "#a5d6a7");
        resetToggleStyle(btnSpeaking,  "#fce4ec", "#c2185b", "#f48fb1");
        switch (active) {
            case "Reading"   -> activateToggle(btnReading,   "#3b5bdb", "white", "#2f4ac0");
            case "Listening" -> activateToggle(btnListening, "#e65100", "white", "#bf360c");
            case "Writing"   -> activateToggle(btnWriting,   "#2e7d32", "white", "#1b5e20");
            case "Speaking"  -> activateToggle(btnSpeaking,  "#c2185b", "white", "#880e4f");
        }
    }

    private void resetToggleStyle(ToggleButton btn, String bg, String fg, String border) {
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-weight:bold;-fx-font-size:13px;-fx-padding:14 0;" +
                "-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-border-color:" + border + ";-fx-border-radius:10;-fx-border-width:2;");
    }

    private void activateToggle(ToggleButton btn, String bg, String fg, String border) {
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-weight:bold;-fx-font-size:13px;-fx-padding:14 0;" +
                "-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-border-color:" + border + ";-fx-border-radius:10;-fx-border-width:2;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.2),6,0,0,2);");
    }

    // ── Init depuis dashboard ─────────────────────────────────────────────────

    public void init(MockTestService service, MockTest test,
                     MockTestDashboardController dashboardController) {
        this.service             = service;
        this.currentTest         = test;
        this.dashboardController = dashboardController;
        testTitleLabel.setText("📋 " + test.getTitle()
                + "  [" + test.getTestType() + " — " + test.getLevel() + "]");
        // Démarrer directement sur la liste des questions existantes
        showListPanel();
        updateStats();
    }

    // ── Panel switching : FORM ↔ LIST ─────────────────────────────────────────

    /** Affiche le formulaire d'ajout de question (plein écran). */
    private void showFormPanel() {
        if (formPanel != null) { formPanel.setVisible(true);  formPanel.setManaged(true); }
        if (listPanel != null) { listPanel.setVisible(false); listPanel.setManaged(false); }
    }

    /** Affiche la liste des questions du test. */
    private void showListPanel() {
        if (formPanel != null) { formPanel.setVisible(false); formPanel.setManaged(false); }
        if (listPanel != null) { listPanel.setVisible(true);  listPanel.setManaged(true); }
        loadQuestions();
    }

    /** Bouton "Voir les questions enregistrées" (dans le formPanel). */
    @FXML
    private void handleShowList(ActionEvent event) {
        showListPanel();
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupTable() {
        colSection.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getSectionCategory()));
        colQuestion.setCellValueFactory(c -> {
            String t = c.getValue().getQuestionText();
            if (t == null || t.isBlank()) {
                t = "[" + c.getValue().getSectionCategory() + " — Contenu IA]";
            }
            return new SimpleStringProperty(t.length() > 65 ? t.substring(0, 65) + "…" : t);
        });
        colAnswer.setCellValueFactory(c -> {
            String ans = c.getValue().getCorrectAnswer();
            if (ans == null || ans.isBlank()) return new SimpleStringProperty("🤖 IA");
            return new SimpleStringProperty(ans.length() > 25 ? ans.substring(0, 25) + "…" : ans);
        });
        colPoints.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getPoints()).asObject());

        // ── RowFactory : couleur par section, respecte la sélection ──────────
        questionsTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(TestQuestion q, boolean empty) {
                super.updateItem(q, empty);
                if (empty || q == null) { setStyle(""); return; }
                // Ne pas écraser le style de sélection JavaFX
                if (isSelected()) return;
                switch (q.getSectionCategory() != null ? q.getSectionCategory() : "") {
                    case "Reading"   -> setStyle("-fx-background-color:#f5f7ff;");
                    case "Listening" -> setStyle("-fx-background-color:#fffaf5;");
                    case "Writing"   -> setStyle("-fx-background-color:#f5faf5;");
                    case "Speaking"  -> setStyle("-fx-background-color:#fff5f8;");
                    default          -> setStyle("");
                }
            }
        });

        // ── Double-clic sur une ligne → modifier la question ──────────────────
        questionsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2
                    && questionsTable.getSelectionModel().getSelectedItem() != null) {
                handleEditSelected(null);
            }
        });
    }

    private void loadQuestions() {
        List<TestQuestion> questions = service.findQuestionsByTest(currentTest.getId());
        questionsTable.setItems(FXCollections.observableArrayList(questions));
        updateStats();
    }

    private void updateStats() {
        long count = service.countQuestionsByTest(currentTest.getId());
        int  pts   = service.sumPointsByTest(currentTest.getId());
        questionCountLabel.setText(count + " question(s)");
        totalPointsLabel.setText("Total : " + pts + " pts");
    }

    // ── Options dynamiques (Reading) ──────────────────────────────────────────

    private void addOptionField(String value) {
        TextField tf = new TextField(value);
        tf.setPromptText("Option " + (optionFields.size() + 1));
        tf.setStyle("-fx-font-size:13px;-fx-pref-height:34px;");
        tf.textProperty().addListener((o, a, n) -> rebuildAnswerChecks());

        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color:#d63939;-fx-text-fill:white;" +
                "-fx-background-radius:4;-fx-cursor:hand;-fx-padding:4 8;-fx-font-size:11px;");
        removeBtn.setOnAction(e -> {
            optionFields.remove(tf);
            optionsContainer.getChildren().removeIf(node ->
                    node instanceof HBox hb && hb.getChildren().contains(tf));
            rebuildAnswerChecks();
        });

        HBox row = new HBox(8, tf, removeBtn);
        HBox.setHgrow(tf, Priority.ALWAYS);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        optionFields.add(tf);
        optionsContainer.getChildren().add(row);
        rebuildAnswerChecks();
    }

    private void rebuildAnswerChecks() {
        correctAnswersContainer.getChildren().clear();
        answerChecks.clear();
        if (!"Reading".equals(currentSection)) return;

        for (TextField tf : optionFields) {
            String optText = tf.getText().trim();
            if (optText.isBlank()) continue;
            CheckBox cb = new CheckBox(optText);
            cb.setStyle("-fx-font-size:13px;");
            answerChecks.add(cb);
            correctAnswersContainer.getChildren().add(cb);
        }
        if (correctAnswersContainer.getChildren().isEmpty()) {
            Label hint = new Label("Les options apparaîtront ici.");
            hint.setStyle("-fx-text-fill:#9ca3af;-fx-font-size:12px;");
            correctAnswersContainer.getChildren().add(hint);
        }
    }

    @FXML
    private void handleAddOption(ActionEvent event) {
        addOptionField("");
        clearError(optionsError);
    }

    // ── Actions CRUD ──────────────────────────────────────────────────────────

    /**
     * "Réinitialiser" ou "← Ajouter une autre question" :
     * bascule vers le formPanel et remet les champs à zéro.
     */
    @FXML
    private void handleNewQuestion(ActionEvent event) {
        showFormPanel();
        resetPanel();
    }

    @FXML
    private void handleEditSelected(ActionEvent event) {
        TestQuestion sel = questionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Veuillez sélectionner une question à modifier."); return; }
        showFormPanel();
        prefillPanel(sel);
    }

    @FXML
    private void handleSaveQuestion(ActionEvent event) {
        clearAllErrors();

        if (currentSection == null) {
            showGlobalError("Veuillez sélectionner une section.");
            return;
        }
        if ("Reading".equals(currentSection) && !validateReadingPanel()) return;

        String qText   = "Reading".equals(currentSection)
                ? questionTextArea.getText().trim()
                : "[" + currentSection + " - Generated by AI]";
        String options = buildOptionsJson();
        String answer  = buildCorrectAnswer();
        int    points  = getActivePoints();

        if ("Reading".equals(currentSection) && (answer == null || answer.isBlank())) {
            showError(answerError, "Veuillez cocher au moins une réponse correcte.");
            return;
        }

        try {
            if (editingQuestion == null) {
                TestQuestion q = new TestQuestion();
                q.setMockTest(currentTest);
                q.setSectionCategory(currentSection);
                q.setQuestionType(currentSection);
                q.setQuestionText(qText);
                q.setOptions(options);
                q.setCorrectAnswer(answer);
                q.setPoints(points);
                q.setActive(true);
                service.createQuestion(q);
                showSuccess("✅  Question ajoutée !");
            } else {
                editingQuestion.setSectionCategory(currentSection);
                editingQuestion.setQuestionType(currentSection);
                editingQuestion.setQuestionText(qText);
                editingQuestion.setOptions(options);
                editingQuestion.setCorrectAnswer(answer);
                editingQuestion.setPoints(points);
                service.updateQuestion(editingQuestion);
                showSuccess("✅  Question mise à jour !");
            }

            // ── Après save : basculer vers la liste des questions ─────────────
            updateStats();
            new Thread(() -> {
                try { Thread.sleep(600); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(this::showListPanel);
            }).start();

        } catch (IllegalArgumentException e) {
            showGlobalError(e.getMessage());
        } catch (Exception e) {
            showGlobalError("Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteQuestion(ActionEvent event) {
        TestQuestion sel = questionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Veuillez sélectionner une question à supprimer."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer cette question ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();

        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                service.deleteQuestion(sel.getId());
                loadQuestions();
                showSuccess("✅  Question supprimée !");
            } catch (Exception e) {
                showGlobalError("Impossible de supprimer : " + e.getMessage());
            }
        }
    }

    // ── API Ninjas : Génération automatique de questions ──────────────────────

    @FXML
    private void handleGenererApi(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("🤖 Générer des questions via API Ninjas");
        dialog.setHeaderText("Génération automatique de questions linguistiques");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color:#f8fafc;");

        // Type de question linguistique
        Label typeLbl = new Label("Type de questions à générer :");
        typeLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(
                "MIXTE — Vocabulaire + Grammaire + Mot du jour",
                "VOCABULAIRE — Synonymes & Antonymes",
                "GRAMMAIRE — Définitions & Parties du discours",
                "MOT_DU_JOUR — Mot du jour avec contexte"
        );
        typeCombo.setValue("MIXTE — Vocabulaire + Grammaire + Mot du jour");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        // Nombre de questions
        Label nbLbl = new Label("Nombre de questions (1 à 10) :");
        nbLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
        Spinner<Integer> nbSpinner = new Spinner<>(1, 10, 5);
        nbSpinner.setEditable(true);
        nbSpinner.setMaxWidth(Double.MAX_VALUE);

        // Points par question
        Label ptsLbl = new Label("Points par question :");
        ptsLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
        Spinner<Integer> ptsSpinner = new Spinner<>(1, 10, 2);
        ptsSpinner.setEditable(true);
        ptsSpinner.setMaxWidth(Double.MAX_VALUE);

        // Description dynamique selon le type choisi
        Label descLbl = new Label();
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-text-fill:#1565c0;-fx-font-size:11px;" +
                "-fx-background-color:#e3f2fd;-fx-background-radius:8;-fx-padding:10 14;");
        updateDescription(typeCombo.getValue(), descLbl);
        typeCombo.valueProperty().addListener((obs, o, n) -> updateDescription(n, descLbl));

        content.getChildren().addAll(typeLbl, typeCombo, nbLbl, nbSpinner,
                ptsLbl, ptsSpinner, descLbl);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(480);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // Extraire le code du type (avant " — ")
        String typeChoisi = typeCombo.getValue().split(" — ")[0];
        int    nbQ        = nbSpinner.getValue();
        int    pts        = ptsSpinner.getValue();

        showSuccess("⏳ Génération en cours via API Ninjas...");

        new Thread(() -> {
            try {
                ApiNinjasService apiService = new ApiNinjasService();
                int count = apiService.genererEtSauvegarder(
                        currentTest, service, typeChoisi, nbQ, pts);

                javafx.application.Platform.runLater(() -> {
                    if (count > 0) {
                        showSuccess("✅ " + count + " question(s) linguistique(s) générée(s) !");
                        showListPanel();
                    } else {
                        showGlobalError("❌ Aucune question générée. Vérifiez la connexion internet.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showGlobalError("Erreur API Ninjas : " + e.getMessage()));
            }
        }).start();
    }

    private void updateDescription(String type, javafx.scene.control.Label lbl) {
        String desc = switch (type.split(" — ")[0]) {
            case "VOCABULAIRE"  -> "📚 Synonymes & Antonymes\nEx: 'Which word is a synonym of happy?'\nEndpoint: /v1/thesaurus";
            case "GRAMMAIRE"    -> "📖 Définitions & Parties du discours\nEx: 'What part of speech is eloquent?'\nEndpoint: /v1/dictionary";
            case "MOT_DU_JOUR"  -> "✨ Mot du jour avec exemple en contexte\nEx: 'Read this sentence, what does X mean?'\nEndpoint: /v1/wordoftheday";
            default             -> "🎯 Mélange équilibré : Vocabulaire + Grammaire + Mot du jour\nUtilise : /v1/thesaurus + /v1/dictionary + /v1/wordoftheday";
        };
        lbl.setText(desc);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (dashboardController != null) dashboardController.returnToDashboard();
    }

    // ── Helpers section ───────────────────────────────────────────────────────

    private int getActivePoints() {
        return switch (currentSection != null ? currentSection : "") {
            case "Reading"   -> pointsSpinner.getValue();
            case "Listening" -> pointsSpinnerListening.getValue();
            case "Writing"   -> pointsSpinnerWriting.getValue();
            case "Speaking"  -> pointsSpinnerSpeaking.getValue();
            default          -> 1;
        };
    }

    private String buildOptionsJson() {
        if (!"Reading".equals(currentSection) || optionFields.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (TextField tf : optionFields) {
            String opt = tf.getText().trim().replace("\"", "\\\"");
            if (opt.isBlank()) continue;
            if (!first) sb.append(",");
            sb.append("\"").append(opt).append("\"");
            first = false;
        }
        return sb.append("]").toString();
    }

    private String buildCorrectAnswer() {
        if ("Reading".equals(currentSection)) {
            List<String> selected = new ArrayList<>();
            for (CheckBox cb : answerChecks) if (cb.isSelected()) selected.add(cb.getText());
            return selected.isEmpty() ? "" : String.join(";", selected);
        }
        return "";
    }

    // ── Panel helpers ─────────────────────────────────────────────────────────

    private void prefillPanel(TestQuestion q) {
        editingQuestion = q;
        panelTitleLabel.setText("✏️  Modifier la question");
        String section = q.getSectionCategory();

        switch (section != null ? section : "") {
            case "Reading"   -> sectionToggleGroup.selectToggle(btnReading);
            case "Listening" -> sectionToggleGroup.selectToggle(btnListening);
            case "Writing"   -> sectionToggleGroup.selectToggle(btnWriting);
            case "Speaking"  -> sectionToggleGroup.selectToggle(btnSpeaking);
        }

        if ("Reading".equals(section)) {
            questionTextArea.setText(q.getQuestionText() != null ? q.getQuestionText() : "");
            pointsSpinner.getValueFactory().setValue(q.getPoints());
            optionFields.clear();
            optionsContainer.getChildren().clear();
            if (q.getOptions() != null && !q.getOptions().isBlank())
                for (String opt : parseOptions(q.getOptions())) addOptionField(opt);
            rebuildAnswerChecks();
            if (q.getCorrectAnswer() != null) {
                String[] answers = q.getCorrectAnswer().split(";");
                for (CheckBox cb : answerChecks)
                    for (String ans : answers)
                        if (cb.getText().equalsIgnoreCase(ans.trim())) cb.setSelected(true);
            }
        } else {
            int pts = q.getPoints();
            switch (section != null ? section : "") {
                case "Listening" -> pointsSpinnerListening.getValueFactory().setValue(pts);
                case "Writing"   -> pointsSpinnerWriting.getValueFactory().setValue(pts);
                case "Speaking"  -> pointsSpinnerSpeaking.getValueFactory().setValue(pts);
            }
        }
        clearAllErrors();
    }

    private void resetPanel() {
        editingQuestion = null;
        currentSection  = null;
        if (panelTitleLabel != null) panelTitleLabel.setText("➕  Ajouter une question");
        sectionToggleGroup.selectToggle(null);
        showPlaceholder();
        resetToggleStyle(btnReading,   "#eef2ff", "#3b5bdb", "#c5d0fc");
        resetToggleStyle(btnListening, "#fff3e0", "#e65100", "#ffcc80");
        resetToggleStyle(btnWriting,   "#e8f5e9", "#2e7d32", "#a5d6a7");
        resetToggleStyle(btnSpeaking,  "#fce4ec", "#c2185b", "#f48fb1");
        if (questionTextArea != null) questionTextArea.clear();
        optionFields.clear();
        if (optionsContainer != null)        optionsContainer.getChildren().clear();
        if (correctAnswersContainer != null) correctAnswersContainer.getChildren().clear();
        answerChecks.clear();
        if (pointsSpinner         != null) pointsSpinner.getValueFactory().setValue(1);
        if (pointsSpinnerListening != null) pointsSpinnerListening.getValueFactory().setValue(1);
        if (pointsSpinnerWriting  != null) pointsSpinnerWriting.getValueFactory().setValue(1);
        if (pointsSpinnerSpeaking != null) pointsSpinnerSpeaking.getValueFactory().setValue(1);
        if (sectionCombo != null) sectionCombo.setValue(null);
        clearAllErrors();
    }

    // ── Validation Reading ────────────────────────────────────────────────────

    private boolean validateReadingPanel() {
        boolean valid = true;
        String qText = questionTextArea.getText();
        if (qText == null || qText.isBlank()) {
            showError(questionTextError, "Le texte de la question est obligatoire."); valid = false;
        } else if (qText.trim().length() < 10) {
            showError(questionTextError, "Le texte doit contenir au moins 10 caractères."); valid = false;
        }
        long filled = optionFields.stream().filter(tf -> !tf.getText().isBlank()).count();
        if (filled < 2) {
            showError(optionsError, "Veuillez saisir au moins 2 options."); valid = false;
        }
        return valid;
    }

    private List<String> parseOptions(String json) {
        List<String> opts = new ArrayList<>();
        if (json == null || json.isBlank()) return opts;
        String inner = json.trim().replaceAll("^\\[|]$", "");
        for (String part : inner.split(",")) {
            String opt = part.trim().replace("\"", "");
            if (!opt.isBlank()) opts.add(opt);
        }
        return opts;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private void showError(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }
    private void clearError(Label lbl) {
        if (lbl == null) return;
        lbl.setText(""); lbl.setVisible(false); lbl.setManaged(false);
    }
    private void clearAllErrors() {
        clearError(sectionError); clearError(questionTextError);
        clearError(optionsError); clearError(answerError);
        clearError(globalError);  clearError(globalSuccess);
    }
    private void showGlobalError(String msg) {
        if (globalError == null) return;
        globalError.setText("❌  " + msg);
        globalError.setVisible(true); globalError.setManaged(true);
    }
    private void showSuccess(String msg) {
        if (globalSuccess == null) return;
        globalSuccess.setText(msg);
        globalSuccess.setVisible(true); globalSuccess.setManaged(true);
    }
    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}