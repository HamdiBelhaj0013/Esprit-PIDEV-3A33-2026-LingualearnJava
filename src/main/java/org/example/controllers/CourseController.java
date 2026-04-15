package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.example.entities.Course;
import org.example.entities.PlatformLanguage;
import org.example.services.CourseService;
import org.example.services.PlatformLanguageService;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class CourseController {

    // --- Form fields ---
    @FXML private TextField                  txtTitle;
    @FXML private ComboBox<String>           cbLevel;
    @FXML private ComboBox<String>           cbStatus;
    @FXML private DatePicker                 dpPublishedAt;
    @FXML private TextField                  txtAuthorId;
    @FXML private ComboBox<PlatformLanguage> cbLanguage;

    // --- Search / filter fields ---
    @FXML private TextField        txtSearchCourse;
    @FXML private ComboBox<String> cbFilterLanguage;
    @FXML private ComboBox<String> cbFilterLevel;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private ComboBox<String> cbSortCourse;

    // --- Table ---
    @FXML private TableView<Course>              tableCourses;
    @FXML private TableColumn<Course, Integer>   colId;
    @FXML private TableColumn<Course, String>    colTitle;
    @FXML private TableColumn<Course, String>    colLevel;
    @FXML private TableColumn<Course, String>    colStatus;
    @FXML private TableColumn<Course, Timestamp> colPublishedAt;
    @FXML private TableColumn<Course, Integer>   colAuthorId;
    @FXML private TableColumn<Course, Integer>   colLanguageId;

    // --- Form panel ---
    @FXML private VBox  formPanel;
    @FXML private Label formPanelTitle;

    private final CourseService           courseService   = new CourseService();
    private final PlatformLanguageService languageService = new PlatformLanguageService();

    private Course selectedCourse;
    private ObservableList<Course>           masterData   = FXCollections.observableArrayList();
    private FilteredList<Course>             filteredData;
    private ObservableList<PlatformLanguage> languageList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPublishedAt.setCellValueFactory(new PropertyValueFactory<>("publishedAt"));
        colAuthorId.setCellValueFactory(new PropertyValueFactory<>("authorId"));
        colLanguageId.setCellValueFactory(new PropertyValueFactory<>("platformLanguageId"));

        // Form combos
        cbLevel.setItems(FXCollections.observableArrayList("Beginner", "Intermediate", "Advanced"));
        cbStatus.setItems(FXCollections.observableArrayList("Draft", "Published", "Archived"));

        // Search combos
        cbFilterLevel.setItems(FXCollections.observableArrayList("All", "Beginner", "Intermediate", "Advanced"));
        cbFilterLevel.setValue("All");
        cbFilterStatus.setItems(FXCollections.observableArrayList("All", "Draft", "Published", "Archived"));
        cbFilterStatus.setValue("All");
        cbSortCourse.setItems(FXCollections.observableArrayList(
                "Default", "Title A-Z", "Title Z-A", "Date Newest", "Date Oldest"));
        cbSortCourse.setValue("Default");

        loadLanguages();
        loadCourses();
        setupSearchFilterSort();

        tableCourses.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedCourse = newVal;
                populateForm(newVal);
                if (formPanelTitle != null) formPanelTitle.setText("Edit Course — " + newVal.getTitle());
                if (formPanel != null) { formPanel.setManaged(true); formPanel.setVisible(true); }
            }
        });
    }

    // ---- FORM SHOW/HIDE ----

    @FXML private void showAddCourseForm() {
        resetFormFields();
        selectedCourse = null;
        if (formPanelTitle != null) formPanelTitle.setText("Add New Course");
        if (formPanel != null) { formPanel.setManaged(true); formPanel.setVisible(true); }
    }

    @FXML private void hideForm() {
        if (formPanel != null) { formPanel.setManaged(false); formPanel.setVisible(false); }
        resetFormFields();
        tableCourses.getSelectionModel().clearSelection();
    }

    // ---- CRUD ----

    @FXML private void addCourse() {
        try {
            courseService.add(buildCourseFromForm());
            showInfo("Course added successfully.");
            hideForm(); loadCourses();
        } catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    @FXML private void updateCourse() {
        if (selectedCourse == null) { showWarning("Select a course to edit."); return; }
        try {
            Course c = buildCourseFromForm();
            c.setId(selectedCourse.getId());
            courseService.update(c);
            showInfo("Course updated.");
            hideForm(); loadCourses();
        } catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    @FXML private void deleteCourse() {
        if (selectedCourse == null) { showWarning("Select a course to delete."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selectedCourse.getTitle() + "\"?", ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try { courseService.delete(selectedCourse.getId()); showInfo("Course deleted."); hideForm(); loadCourses(); }
            catch (Exception e) { showError("Error: " + e.getMessage()); }
        }
    }

    @FXML private void resetForm() {
        resetFormFields(); selectedCourse = null; tableCourses.getSelectionModel().clearSelection();
    }

    @FXML private void applySearch() { applyFiltersAndSort(); }

    // ---- SEARCH / FILTER / SORT ----

    private void setupSearchFilterSort() {
        filteredData = new FilteredList<>(masterData, b -> true);
        txtSearchCourse.textProperty().addListener((obs, o, n)  -> applyFiltersAndSort());
        cbFilterLanguage.valueProperty().addListener((obs, o, n) -> applyFiltersAndSort());
        cbFilterLevel.valueProperty().addListener((obs, o, n)    -> applyFiltersAndSort());
        cbFilterStatus.valueProperty().addListener((obs, o, n)   -> applyFiltersAndSort());
        cbSortCourse.valueProperty().addListener((obs, o, n)     -> applyFiltersAndSort());
    }

    private void applyFiltersAndSort() {
        String search       = txtSearchCourse.getText() == null ? "" : txtSearchCourse.getText().trim().toLowerCase();
        String langFilter   = cbFilterLanguage.getValue();
        String levelFilter  = cbFilterLevel.getValue();
        String statusFilter = cbFilterStatus.getValue();
        String sortVal      = cbSortCourse.getValue();

        filteredData.setPredicate(course -> {
            boolean matchTitle = search.isEmpty() || course.getTitle().toLowerCase().contains(search);

            boolean matchLang = langFilter == null || "All".equals(langFilter);
            if (!matchLang) {
                matchLang = languageList.stream().anyMatch(l ->
                        l.getId() == course.getPlatformLanguageId() && l.getName().equals(langFilter));
            }

            boolean matchLevel  = levelFilter  == null || "All".equals(levelFilter)
                    || levelFilter.equalsIgnoreCase(course.getLevel());
            boolean matchStatus = statusFilter == null || "All".equals(statusFilter)
                    || statusFilter.equalsIgnoreCase(course.getStatus());

            return matchTitle && matchLang && matchLevel && matchStatus;
        });

        SortedList<Course> sorted = new SortedList<>(filteredData);
        switch (sortVal == null ? "Default" : sortVal) {
            case "Title A-Z"   -> sorted.setComparator(Comparator.comparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER));
            case "Title Z-A"   -> sorted.setComparator(Comparator.comparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER).reversed());
            case "Date Newest" -> sorted.setComparator(Comparator.comparing(Course::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())));
            case "Date Oldest" -> sorted.setComparator(Comparator.comparing(Course::getPublishedAt, Comparator.nullsLast(Comparator.naturalOrder())));
            default            -> sorted.setComparator(null);
        }
        tableCourses.setItems(sorted);
    }

    // ---- HELPERS ----

    private void loadCourses() {
        try {
            masterData.setAll(courseService.getAll());
            if (filteredData == null) filteredData = new FilteredList<>(masterData, b -> true);
            applyFiltersAndSort();
        } catch (Exception e) { showError("Error loading courses: " + e.getMessage()); }
    }

    private void loadLanguages() {
        try {
            List<PlatformLanguage> langs = languageService.getAll();
            languageList.setAll(langs);
            cbLanguage.setItems(languageList);

            ObservableList<String> filterItems = FXCollections.observableArrayList("All");
            langs.forEach(l -> filterItems.add(l.getName()));
            cbFilterLanguage.setItems(filterItems);
            cbFilterLanguage.setValue("All");
        } catch (Exception e) { showError("Error loading languages: " + e.getMessage()); }
    }

    private void populateForm(Course c) {
        txtTitle.setText(c.getTitle());
        cbLevel.setValue(c.getLevel());
        cbStatus.setValue(c.getStatus());
        dpPublishedAt.setValue(c.getPublishedAt() != null
                ? c.getPublishedAt().toLocalDateTime().toLocalDate() : null);
        txtAuthorId.setText(c.getAuthorId() == null ? "" : String.valueOf(c.getAuthorId()));
        for (PlatformLanguage l : cbLanguage.getItems()) {
            if (l.getId() == c.getPlatformLanguageId()) { cbLanguage.setValue(l); break; }
        }
    }

    private void resetFormFields() {
        txtTitle.clear(); cbLevel.setValue(null); cbStatus.setValue(null);
        dpPublishedAt.setValue(null); txtAuthorId.clear(); cbLanguage.setValue(null);
    }

    private Course buildCourseFromForm() {
        PlatformLanguage lang = cbLanguage.getValue();
        Integer authorId = txtAuthorId.getText().trim().isEmpty() ? null
                : Integer.parseInt(txtAuthorId.getText().trim());
        Timestamp publishedAt = null;
        LocalDate date = dpPublishedAt.getValue();
        if (date != null)
            publishedAt = Timestamp.valueOf(LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 0, 0));
        return new Course(txtTitle.getText().trim(), cbLevel.getValue(), cbStatus.getValue(),
                publishedAt, authorId, lang == null ? 0 : lang.getId());
    }

    private void showInfo(String m)    { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void showWarning(String m) { Alert a = new Alert(Alert.AlertType.WARNING);     a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void showError(String m)   { Alert a = new Alert(Alert.AlertType.ERROR);       a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
}