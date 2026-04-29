package org.example.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.example.entities.pedagogicalcontent.PlatformLanguage;
import org.example.service.PlatformLanguageService;

import java.util.Comparator;
import java.util.List;

public class PlatformLanguageController {

    @FXML private TextField        txtName;
    @FXML private TextField        txtCode;
    @FXML private TextField        txtFlagUrl;
    @FXML private CheckBox         chkEnabled;

    @FXML private TextField        txtSearchLanguage;
    @FXML private ComboBox<String> cbFilterEnabled;
    @FXML private ComboBox<String> cbSortLanguage;

    @FXML private VBox     formPanel;
    @FXML private Label    formPanelTitle;
    @FXML private FlowPane langCardsPane;
    @FXML private VBox     emptyState;
    @FXML private Label    lblLangCount;

    private final PlatformLanguageService service = new PlatformLanguageService();
    private PlatformLanguage selectedLanguage;

    private ObservableList<PlatformLanguage> masterData   = FXCollections.observableArrayList();
    private FilteredList<PlatformLanguage>   filteredData;

    @FXML
    public void initialize() {
        cbFilterEnabled.setItems(FXCollections.observableArrayList("All", "Enabled", "Disabled"));
        cbFilterEnabled.setValue("All");

        cbSortLanguage.setItems(FXCollections.observableArrayList(
                "Default", "Name A-Z", "Name Z-A", "Code A-Z", "Code Z-A"));
        cbSortLanguage.setValue("Default");

        loadLanguages();
        setupSearchFilterSort();
    }

    /* ===== FORM SHOW / HIDE ===== */

    @FXML private void showAddForm() {
        resetFormFields();
        selectedLanguage = null;
        formPanelTitle.setText("Add New Language");
        formPanel.setManaged(true);
        formPanel.setVisible(true);
    }

    @FXML private void hideForm() {
        formPanel.setManaged(false);
        formPanel.setVisible(false);
        resetFormFields();
    }

    /* ===== CRUD ===== */

    @FXML private void addLanguage() {
        try {
            PlatformLanguage language = new PlatformLanguage(
                    txtName.getText().trim(),
                    txtCode.getText().trim(),
                    txtFlagUrl.getText().trim(),
                    chkEnabled.isSelected()
            );
            service.add(language);
            showInfo("Language added successfully.");
            hideForm();
            loadLanguages();
        } catch (IllegalArgumentException e) { showError(e.getMessage()); }
        catch (Exception e) { showError("Error adding language: " + e.getMessage()); }
    }

    @FXML private void updateLanguage() {
        if (selectedLanguage == null) { showWarning("Please select a language to edit."); return; }
        try {
            selectedLanguage.setName(txtName.getText().trim());
            selectedLanguage.setCode(txtCode.getText().trim());
            selectedLanguage.setFlagUrl(txtFlagUrl.getText().trim());
            selectedLanguage.setEnabled(chkEnabled.isSelected());
            service.update(selectedLanguage);
            showInfo("Language updated successfully.");
            hideForm();
            loadLanguages();
        } catch (IllegalArgumentException e) { showError(e.getMessage()); }
        catch (Exception e) { showError("Error updating language: " + e.getMessage()); }
    }

    @FXML private void deleteLanguage() {
        if (selectedLanguage == null) { showWarning("Please select a language to delete."); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText(null);
        alert.setContentText("Delete language \"" + selectedLanguage.getName() + "\"?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.delete(selectedLanguage.getId());
                showInfo("Language deleted.");
                hideForm();
                loadLanguages();
            } catch (Exception e) { showError("Error deleting language: " + e.getMessage()); }
        }
    }

    @FXML private void resetForm() { resetFormFields(); selectedLanguage = null; }

    private void resetFormFields() {
        txtName.clear(); txtCode.clear(); txtFlagUrl.clear(); chkEnabled.setSelected(false);
    }

    /* ===== LOAD + CARD RENDERING ===== */

    private void loadLanguages() {
        try {
            List<PlatformLanguage> languages = service.getAll();
            masterData.setAll(languages);
            if (filteredData == null) filteredData = new FilteredList<>(masterData, b -> true);
            applyFiltersAndSort();
        } catch (Exception e) { showError("Error loading languages: " + e.getMessage()); }
    }

    private void renderCards(List<PlatformLanguage> languages) {
        langCardsPane.getChildren().clear();

        if (lblLangCount != null)
            lblLangCount.setText(languages.size() + " language" + (languages.size() != 1 ? "s" : ""));

        if (languages.isEmpty()) {
            if (emptyState != null) { emptyState.setManaged(true); emptyState.setVisible(true); }
            langCardsPane.setManaged(false); langCardsPane.setVisible(false);
            return;
        }

        if (emptyState != null) { emptyState.setManaged(false); emptyState.setVisible(false); }
        langCardsPane.setManaged(true); langCardsPane.setVisible(true);

        for (PlatformLanguage lang : languages)
            langCardsPane.getChildren().add(buildLangCard(lang));
    }

    private VBox buildLangCard(PlatformLanguage lang) {

        /* ---- FLAG: try to load image, fallback to globe emoji ---- */
        StackPane flagCircle = new StackPane();
        flagCircle.getStyleClass().add("lang-flag-circle");
        flagCircle.setPrefSize(54, 40);
        flagCircle.setMinSize(54, 40);
        flagCircle.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8;");

        String flagUrl = lang.getFlagUrl();
        if (flagUrl != null && !flagUrl.isBlank()) {
            // Show a placeholder globe while loading
            Label placeholder = new Label("🌐");
            placeholder.setStyle("-fx-font-size:20px;");
            flagCircle.getChildren().add(placeholder);

            try {
                // Load image asynchronously (backgroundLoading = true)
                Image img = new Image(flagUrl, 54, 36, true, true, /*backgroundLoading*/ true);

                img.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        // Keep globe placeholder — image failed to load
                    }
                });

                img.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                    if (newProgress.doubleValue() >= 1.0 && !img.isError()) {
                        Platform.runLater(() -> {
                            ImageView iv = new ImageView(img);
                            iv.setFitWidth(54);
                            iv.setFitHeight(36);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(true);
                            flagCircle.getChildren().setAll(iv);
                        });
                    }
                });

                // If already cached / loaded synchronously
                if (!img.isError() && img.getProgress() >= 1.0) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(54);
                    iv.setFitHeight(36);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    flagCircle.getChildren().setAll(iv);
                }

            } catch (Exception ignored) {
                // flagCircle already has the globe placeholder
            }
        } else {
            Label globe = new Label("🌐");
            globe.setStyle("-fx-font-size:20px;");
            flagCircle.getChildren().add(globe);
        }

        /* ---- NAME ---- */
        Label nameLabel = new Label(lang.getName());
        nameLabel.getStyleClass().add("lang-name");

        /* ---- CODE BADGE ---- */
        Label codeBadge = new Label(lang.getCode() != null ? lang.getCode().toUpperCase() : "??");
        codeBadge.getStyleClass().add("lang-code-badge");

        /* ---- STATUS BADGE ---- */
        Label statusBadge = new Label(lang.isEnabled() ? "✓ Enabled" : "✗ Disabled");
        statusBadge.getStyleClass().add(lang.isEnabled() ? "lang-status-enabled" : "lang-status-disabled");

        HBox badges = new HBox(8, codeBadge, statusBadge);
        badges.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(4, nameLabel, badges);
        textBox.setAlignment(Pos.CENTER_LEFT);

        /* ---- EDIT / DELETE BUTTONS ---- */
        Button editBtn = new Button("✏ Edit");
        editBtn.getStyleClass().add("btn-icon-edit");
        editBtn.setOnAction(e -> { e.consume(); openEditForm(lang); });

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("btn-icon-delete");
        deleteBtn.setOnAction(e -> { e.consume(); selectedLanguage = lang; deleteLanguage(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionsBox = new HBox(6, spacer, editBtn, deleteBtn);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        HBox topRow = new HBox(12, flagCircle, textBox);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, topRow, actionsBox);
        card.getStyleClass().add("lang-card");
        card.setPrefWidth(280);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setOnMouseClicked(e -> openEditForm(lang));

        return card;
    }

    private void openEditForm(PlatformLanguage lang) {
        selectedLanguage = lang;
        txtName.setText(lang.getName());
        txtCode.setText(lang.getCode());
        txtFlagUrl.setText(lang.getFlagUrl() != null ? lang.getFlagUrl() : "");
        chkEnabled.setSelected(lang.isEnabled());
        formPanelTitle.setText("Edit Language — " + lang.getName());
        formPanel.setManaged(true);
        formPanel.setVisible(true);
    }

    /* ===== SEARCH / FILTER / SORT ===== */

    private void setupSearchFilterSort() {
        filteredData = new FilteredList<>(masterData, b -> true);
        txtSearchLanguage.textProperty().addListener((obs, o, n) -> applyFiltersAndSort());
        cbFilterEnabled.valueProperty().addListener((obs, o, n)  -> applyFiltersAndSort());
        cbSortLanguage.valueProperty().addListener((obs, o, n)   -> applyFiltersAndSort());
    }

    private void applyFiltersAndSort() {
        String search       = txtSearchLanguage.getText() == null ? "" : txtSearchLanguage.getText().trim().toLowerCase();
        String statusFilter = cbFilterEnabled.getValue();
        String sortValue    = cbSortLanguage.getValue();

        filteredData.setPredicate(lang -> {
            boolean matchSearch = search.isEmpty()
                    || lang.getName().toLowerCase().contains(search)
                    || lang.getCode().toLowerCase().contains(search);
            boolean matchStatus = true;
            if ("Enabled".equals(statusFilter))  matchStatus = lang.isEnabled();
            if ("Disabled".equals(statusFilter)) matchStatus = !lang.isEnabled();
            return matchSearch && matchStatus;
        });

        SortedList<PlatformLanguage> sorted = new SortedList<>(filteredData);
        if ("Name A-Z".equals(sortValue))
            sorted.setComparator(Comparator.comparing(PlatformLanguage::getName, String.CASE_INSENSITIVE_ORDER));
        else if ("Name Z-A".equals(sortValue))
            sorted.setComparator(Comparator.comparing(PlatformLanguage::getName, String.CASE_INSENSITIVE_ORDER).reversed());
        else if ("Code A-Z".equals(sortValue))
            sorted.setComparator(Comparator.comparing(PlatformLanguage::getCode, String.CASE_INSENSITIVE_ORDER));
        else if ("Code Z-A".equals(sortValue))
            sorted.setComparator(Comparator.comparing(PlatformLanguage::getCode, String.CASE_INSENSITIVE_ORDER).reversed());
        else
            sorted.setComparator(null);

        renderCards(sorted);
    }

    /* ===== ALERTS ===== */
    private void showInfo(String msg)    { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private void showWarning(String msg) { new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait(); }
    private void showError(String msg)   { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}