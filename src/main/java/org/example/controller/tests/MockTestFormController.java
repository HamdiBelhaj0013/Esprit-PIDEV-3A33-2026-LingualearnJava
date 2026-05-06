package org.example.controller.tests;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.PlatformLanguage;
import org.example.service.tests.MockTestService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MockTestFormController implements Initializable {

    // ── Champs ────────────────────────────────────────────────────────────────
    @FXML private Label            formTitleLabel;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField        titleField;
    @FXML private ComboBox<PlatformLanguage> languageCombo;
    @FXML private ComboBox<String> levelCombo;
    @FXML private Spinner<Integer> durationSpinner;

    // ── Labels erreurs inline ─────────────────────────────────────────────────
    @FXML private Label typeError;
    @FXML private Label titleError;
    @FXML private Label languageError;
    @FXML private Label levelError;
    @FXML private Label durationError;
    @FXML private Label globalError;
    @FXML private Label globalSuccess;

    private MockTestService             service;
    private MockTest                    editMode = null;
    private MockTestDashboardController dashboardController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.setItems(FXCollections.observableArrayList(
                "TOEFL", "IELTS", "DELF", "DALF", "TCF", "Cambridge"));
        levelCombo.setItems(FXCollections.observableArrayList(
                "A1", "A2", "B1", "B2", "C1", "C2"));
        durationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 90, 30, 1));
        durationSpinner.setEditable(true);

        // Validation temps réel
        titleField.textProperty().addListener((o, a, n)     -> clearError(titleError));
        typeCombo.valueProperty().addListener((o, a, n)     -> clearError(typeError));
        levelCombo.valueProperty().addListener((o, a, n)    -> clearError(levelError));
        languageCombo.valueProperty().addListener((o, a, n) -> clearError(languageError));
    }

    /**
     * Appelé depuis MockTestDashboardController.
     * @param service           le service MockTest
     * @param test              null = création, non-null = édition
     * @param dashboardController référence pour retourner au dashboard
     */
    public void init(MockTestService service, MockTest test,
                     MockTestDashboardController dashboardController) {
        this.service              = service;
        this.editMode             = test;
        this.dashboardController  = dashboardController;

        loadLanguages();

        if (test == null) {
            formTitleLabel.setText("➕ Nouveau Test de Certification");
        } else {
            formTitleLabel.setText("✏️ Modifier le Test");
            prefillForm(test);
        }
    }

    private void loadLanguages() {
        List<PlatformLanguage> langs = service.findAllLanguages();
        languageCombo.setItems(FXCollections.observableArrayList(langs));
        languageCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(PlatformLanguage l)   { return l != null ? l.getName() : ""; }
            @Override public PlatformLanguage fromString(String s) { return null; }
        });
    }

    private void prefillForm(MockTest t) {
        typeCombo.setValue(t.getTestType());
        titleField.setText(t.getTitle());
        levelCombo.setValue(t.getLevel());
        durationSpinner.getValueFactory().setValue(t.getDurationMinutes());
        if (t.getPlatformLanguage() != null) {
            languageCombo.getItems().stream()
                    .filter(l -> l.getId().equals(t.getPlatformLanguage().getId()))
                    .findFirst().ifPresent(languageCombo::setValue);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    private void handleSave(ActionEvent event) {
        clearAllErrors();
        if (!validateForm()) return;

        try {
            if (editMode == null) {
                MockTest newTest = new MockTest();
                newTest.setTestType(typeCombo.getValue());
                newTest.setTitle(titleField.getText().trim());
                newTest.setLevel(levelCombo.getValue());
                newTest.setDurationMinutes(durationSpinner.getValue());
                newTest.setPlatformLanguage(languageCombo.getValue());
                service.create(newTest);
                showSuccess("✅ Test créé avec succès !");
            } else {
                editMode.setTestType(typeCombo.getValue());
                editMode.setTitle(titleField.getText().trim());
                editMode.setLevel(levelCombo.getValue());
                editMode.setDurationMinutes(durationSpinner.getValue());
                editMode.setPlatformLanguage(languageCombo.getValue());
                service.update(editMode);
                showSuccess("✅ Test mis à jour avec succès !");
            }

            // Retour automatique au dashboard après 800ms
            new Thread(() -> {
                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> {
                    if (dashboardController != null)
                        dashboardController.returnToDashboard();
                });
            }).start();

        } catch (IllegalArgumentException e) {
            showGlobalError(e.getMessage());
        } catch (Exception e) {
            showGlobalError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        if (dashboardController != null)
            dashboardController.returnToDashboard();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateForm() {
        boolean valid = true;
        if (typeCombo.getValue() == null) {
            showError(typeError, "Le type est obligatoire."); valid = false;
        }
        String title = titleField.getText();
        if (title == null || title.isBlank()) {
            showError(titleError, "Le titre est obligatoire."); valid = false;
        } else if (title.trim().length() < 5) {
            showError(titleError, "Minimum 5 caractères."); valid = false;
        } else if (title.trim().length() > 100) {
            showError(titleError, "Maximum 100 caractères."); valid = false;
        }
        if (languageCombo.getValue() == null) {
            showError(languageError, "La langue est obligatoire."); valid = false;
        }
        if (levelCombo.getValue() == null) {
            showError(levelError, "Le niveau est obligatoire."); valid = false;
        }
        try {
            int dur = durationSpinner.getValue();
            if (dur < 2)  { showError(durationError, "Minimum 2 minutes."); valid = false; }
            if (dur > 90) { showError(durationError, "Maximum 90 minutes."); valid = false; }
        } catch (Exception e) {
            showError(durationError, "Durée invalide."); valid = false;
        }
        return valid;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }
    private void clearError(Label lbl) {
        lbl.setText(""); lbl.setVisible(false); lbl.setManaged(false);
    }
    private void clearAllErrors() {
        clearError(typeError); clearError(titleError); clearError(languageError);
        clearError(levelError); clearError(durationError);
        clearError(globalError); clearError(globalSuccess);
    }
    private void showGlobalError(String msg) {
        globalError.setText("❌ " + msg); globalError.setVisible(true); globalError.setManaged(true);
    }
    private void showSuccess(String msg) {
        globalSuccess.setText(msg); globalSuccess.setVisible(true); globalSuccess.setManaged(true);
    }
}