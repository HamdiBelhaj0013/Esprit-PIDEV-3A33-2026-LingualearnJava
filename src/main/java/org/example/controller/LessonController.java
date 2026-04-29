package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.example.entities.pedagogicalcontent.Course;
import org.example.entities.pedagogicalcontent.Lesson;
import org.example.service.CourseService;
import org.example.service.LessonService;
import org.example.validation.LessonValidator;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class LessonController {

    @FXML private TextField         txtTitle;
    @FXML private TextArea          txtContent;
    @FXML private TextArea          txtVocabularyData;
    @FXML private TextArea          txtGrammarData;
    @FXML private TextField         txtXpReward;
    @FXML private ComboBox<Course>  cbCourse;
    @FXML private TextField         txtVideoName;
    @FXML private TextField         txtThumbName;
    @FXML private TextField         txtResourceName;
    @FXML private DatePicker        dpUpdatedAt;

    @FXML private TextField         txtSearchLesson;
    @FXML private ComboBox<String>  cbFilterCourse;
    @FXML private ComboBox<String>  cbSortLesson;

    @FXML private TableView<Lesson>               tableLessons;
    @FXML private TableColumn<Lesson, Integer>    colId;
    @FXML private TableColumn<Lesson, String>     colTitle;
    @FXML private TableColumn<Lesson, Integer>    colXpReward;
    @FXML private TableColumn<Lesson, Integer>    colCourseId;
    @FXML private TableColumn<Lesson, Timestamp>  colUpdatedAt;

    // Form panel
    @FXML private VBox  formPanel;
    @FXML private Label formPanelTitle;

    private final LessonService  lessonService  = new LessonService();
    private final CourseService  courseService  = new CourseService();
    private Lesson selectedLesson;

    private ObservableList<Lesson>  masterData   = FXCollections.observableArrayList();
    private FilteredList<Lesson>    filteredData;
    private ObservableList<Course>  courseList   = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colXpReward.setCellValueFactory(new PropertyValueFactory<>("xpReward"));
        colCourseId.setCellValueFactory(new PropertyValueFactory<>("courseId"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        cbSortLesson.setItems(FXCollections.observableArrayList(
                "Default", "Title A-Z", "Title Z-A", "XP Asc", "XP Desc"));
        cbSortLesson.setValue("Default");

        loadCourses();
        loadLessons();
        setupSearchFilterSort();

        tableLessons.getSelectionModel().selectedItemProperty().addListener((obs, o, newVal) -> {
            if (newVal != null) {
                selectedLesson = newVal;
                populateForm(newVal);
                if (formPanel != null) {
                    formPanelTitle.setText("Edit Lesson — " + newVal.getTitle());
                    formPanel.setManaged(true);
                    formPanel.setVisible(true);
                }
            }
        });
    }

    /* ===== FORM SHOW / HIDE ===== */

    @FXML
    private void showAddLessonForm() {
        resetFormFields();
        selectedLesson = null;
        if (formPanelTitle != null) formPanelTitle.setText("Add New Lesson");
        if (formPanel != null) { formPanel.setManaged(true); formPanel.setVisible(true); }
    }

    @FXML
    private void hideForm() {
        if (formPanel != null) { formPanel.setManaged(false); formPanel.setVisible(false); }
        resetFormFields();
    }

    /* ===== CRUD ===== */

    @FXML
    private void addLesson() {
        try {
            Lesson lesson = buildLessonFromForm();
            List<String> errors = LessonValidator.validate(lesson);
            if (!errors.isEmpty()) { showError(String.join("\n", errors)); return; }
            lessonService.add(lesson);
            showInfo("Lesson added successfully.");
            hideForm();
            loadLessons();
        } catch (NumberFormatException e) { showError("XP Reward must be a valid number."); }
        catch (Exception e) { showError("Error adding lesson: " + e.getMessage()); }
    }

    @FXML
    private void updateLesson() {
        if (selectedLesson == null) { showWarning("Select a lesson to edit."); return; }
        try {
            Lesson lesson = buildLessonFromForm();
            lesson.setId(selectedLesson.getId());
            List<String> errors = LessonValidator.validate(lesson);
            if (!errors.isEmpty()) { showError(String.join("\n", errors)); return; }
            lessonService.update(lesson);
            showInfo("Lesson updated.");
            hideForm();
            loadLessons();
        } catch (NumberFormatException e) { showError("XP Reward must be a valid number."); }
        catch (Exception e) { showError("Error updating lesson: " + e.getMessage()); }
    }

    @FXML
    private void deleteLesson() {
        if (selectedLesson == null) { showWarning("Select a lesson to delete."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete this lesson?", ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                lessonService.delete(selectedLesson.getId());
                showInfo("Lesson deleted.");
                hideForm();
                loadLessons();
            } catch (Exception e) { showError("Error deleting lesson: " + e.getMessage()); }
        }
    }

    @FXML
    private void resetForm() {
        resetFormFields();
        selectedLesson = null;
        tableLessons.getSelectionModel().clearSelection();
    }

    /* ===== HELPERS ===== */

    private void populateForm(Lesson l) {
        txtTitle.setText(l.getTitle());
        txtContent.setText(l.getContent());
        txtVocabularyData.setText(l.getVocabularyData());
        txtGrammarData.setText(l.getGrammarData());
        txtXpReward.setText(String.valueOf(l.getXpReward()));
        txtVideoName.setText(l.getVideoName() == null ? "" : l.getVideoName());
        txtThumbName.setText(l.getThumbName() == null ? "" : l.getThumbName());
        txtResourceName.setText(l.getResourceName() == null ? "" : l.getResourceName());
        dpUpdatedAt.setValue(l.getUpdatedAt() != null
                ? l.getUpdatedAt().toLocalDateTime().toLocalDate() : null);
        for (Course c : cbCourse.getItems()) {
            if (c.getId() == l.getCourseId()) { cbCourse.setValue(c); break; }
        }
    }

    private void resetFormFields() {
        txtTitle.clear(); txtContent.clear(); txtVocabularyData.clear();
        txtGrammarData.clear(); txtXpReward.clear(); cbCourse.setValue(null);
        txtVideoName.clear(); txtThumbName.clear(); txtResourceName.clear();
        dpUpdatedAt.setValue(null);
    }

    private Lesson buildLessonFromForm() {
        int xp = txtXpReward.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtXpReward.getText().trim());
        Course c = cbCourse.getValue();
        Timestamp updatedAt = null;
        LocalDate date = dpUpdatedAt.getValue();
        if (date != null)
            updatedAt = Timestamp.valueOf(LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 0, 0));
        return new Lesson(txtTitle.getText().trim(), txtContent.getText().trim(),
                txtVocabularyData.getText().trim(), txtGrammarData.getText().trim(),
                xp, c == null ? 0 : c.getId(),
                txtVideoName.getText().trim(), txtThumbName.getText().trim(),
                txtResourceName.getText().trim(), updatedAt);
    }

    private void loadLessons() {
        try {
            masterData.setAll(lessonService.getAll());
            if (filteredData == null) filteredData = new FilteredList<>(masterData, b -> true);
            applyFiltersAndSort();
        } catch (Exception e) { showError("Error loading lessons: " + e.getMessage()); }
    }

    private void loadCourses() {
        try {
            List<Course> courses = courseService.getAll();
            courseList.setAll(courses);
            cbCourse.setItems(courseList);
            ObservableList<String> filterItems = FXCollections.observableArrayList("All");
            courses.forEach(cc -> filterItems.add(cc.getTitle()));
            cbFilterCourse.setItems(filterItems);
            cbFilterCourse.setValue("All");
        } catch (Exception e) { showError("Error loading courses: " + e.getMessage()); }
    }

    private void setupSearchFilterSort() {
        filteredData = new FilteredList<>(masterData, b -> true);
        txtSearchLesson.textProperty().addListener((obs, o, n) -> applyFiltersAndSort());
        cbFilterCourse.valueProperty().addListener((obs, o, n) -> applyFiltersAndSort());
        cbSortLesson.valueProperty().addListener((obs, o, n) -> applyFiltersAndSort());
    }

    private void applyFiltersAndSort() {
        String search = txtSearchLesson.getText() == null ? "" : txtSearchLesson.getText().trim().toLowerCase();
        String courseFilter = cbFilterCourse.getValue();
        String sortVal = cbSortLesson.getValue();

        filteredData.setPredicate(lesson -> {
            boolean matchSearch = search.isEmpty() || lesson.getTitle().toLowerCase().contains(search);
            boolean matchCourse = true;
            if (courseFilter != null && !"All".equals(courseFilter)) {
                matchCourse = courseList.stream().anyMatch(cc ->
                        cc.getId() == lesson.getCourseId() && cc.getTitle().equals(courseFilter));
            }
            return matchSearch && matchCourse;
        });

        SortedList<Lesson> sorted = new SortedList<>(filteredData);
        if ("Title A-Z".equals(sortVal))
            sorted.setComparator(Comparator.comparing(Lesson::getTitle, String.CASE_INSENSITIVE_ORDER));
        else if ("Title Z-A".equals(sortVal))
            sorted.setComparator(Comparator.comparing(Lesson::getTitle, String.CASE_INSENSITIVE_ORDER).reversed());
        else if ("XP Asc".equals(sortVal))
            sorted.setComparator(Comparator.comparingInt(Lesson::getXpReward));
        else if ("XP Desc".equals(sortVal))
            sorted.setComparator(Comparator.comparingInt(Lesson::getXpReward).reversed());
        else
            sorted.setComparator(null);

        tableLessons.setItems(sorted);
    }

    private void showInfo(String m)    { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void showWarning(String m) { Alert a = new Alert(Alert.AlertType.WARNING);     a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void showError(String m)   { Alert a = new Alert(Alert.AlertType.ERROR);       a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
}