package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.Course;
import org.example.entities.PlatformLanguage;
import org.example.services.CourseService;
import org.example.services.LessonService;
import org.example.services.PlatformLanguageService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FrontCoursesController {

    @FXML private Label languageTitle;
    @FXML private Label languageBannerName;
    @FXML private Label languageCodeBadge;
    @FXML private Label courseCountLabel;
    @FXML private Label backToLanguagesLabel;
    @FXML private FlowPane coursesContainer;

    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterBeginner;
    @FXML private Button btnFilterIntermediate;
    @FXML private Button btnFilterAdvanced;

    private final CourseService courseService = new CourseService();
    private final PlatformLanguageService languageService = new PlatformLanguageService();
    private final LessonService lessonService = new LessonService();

    private List<Course> allCourses = new ArrayList<>();

    @FXML
    public void initialize() {
        backToLanguagesLabel.setOnMouseClicked(e -> FrontRouter.goTo("/views/frontoffice/front-languages.fxml"));

        btnFilterAll.setOnAction(e -> applyFilter("all"));
        btnFilterBeginner.setOnAction(e -> applyFilter("beginner"));
        btnFilterIntermediate.setOnAction(e -> applyFilter("intermediate"));
        btnFilterAdvanced.setOnAction(e -> applyFilter("advanced"));

        loadCourses();
    }

    private void loadCourses() {
        try {
            int languageId = FrontNavigationState.getSelectedLanguageId();
            String languageName = FrontNavigationState.getSelectedLanguageName();

            allCourses = courseService.getCoursesByLanguage(languageId);
            PlatformLanguage language = languageService.getById(languageId);

            languageTitle.setText(languageName);
            languageBannerName.setText(languageName);
            languageCodeBadge.setText(language != null && language.getCode() != null ? language.getCode().toUpperCase() : "-");
            courseCountLabel.setText(allCourses.size() + " course" + (allCourses.size() > 1 ? "s" : "") + " available");

            renderCourses(allCourses);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFilter(String level) {
        btnFilterAll.getStyleClass().remove("active-filter");
        btnFilterBeginner.getStyleClass().remove("active-filter");
        btnFilterIntermediate.getStyleClass().remove("active-filter");
        btnFilterAdvanced.getStyleClass().remove("active-filter");

        switch (level) {
            case "beginner" -> btnFilterBeginner.getStyleClass().add("active-filter");
            case "intermediate" -> btnFilterIntermediate.getStyleClass().add("active-filter");
            case "advanced" -> btnFilterAdvanced.getStyleClass().add("active-filter");
            default -> btnFilterAll.getStyleClass().add("active-filter");
        }

        List<Course> filtered;
        if ("all".equals(level)) {
            filtered = allCourses;
        } else {
            filtered = allCourses.stream()
                    .filter(c -> normalizeLevel(c.getLevel()).equals(level))
                    .collect(Collectors.toList());
        }

        renderCourses(filtered);
    }

    private String normalizeLevel(String level) {
        if (level == null) {
            return "";
        }

        String v = level.trim().toLowerCase();

        if (v.contains("début") || v.contains("debut") || v.contains("begin")) {
            return "beginner";
        }
        if (v.contains("inter")) {
            return "intermediate";
        }
        if (v.contains("adv")) {
            return "advanced";
        }

        return v;
    }

    private String displayLevel(String level) {
        return switch (normalizeLevel(level)) {
            case "beginner" -> "DÉBUTANT";
            case "intermediate" -> "INTERMEDIATE";
            case "advanced" -> "ADVANCED";
            default -> level == null ? "-" : level.toUpperCase();
        };
    }

    private void renderCourses(List<Course> courses) {
        coursesContainer.getChildren().clear();

        for (Course course : courses) {
            coursesContainer.getChildren().add(createCourseCard(course));
        }

        courseCountLabel.setText(courses.size() + " course" + (courses.size() > 1 ? "s" : "") + " available");
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox();
        card.getStyleClass().add("course-card");

        VBox top = new VBox(10);
        top.getStyleClass().add("course-card-top");

        Label level = new Label(displayLevel(course.getLevel()));
        level.getStyleClass().add("level-badge");

        Label title = new Label(course.getTitle());
        title.getStyleClass().add("course-card-title");

        int lessonCount = 0;
        try {
            lessonCount = lessonService.getLessonsByCourse(course.getId()).size();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Label lessonsCount = new Label("📖 " + lessonCount + " lesson" + (lessonCount > 1 ? "s" : ""));
        lessonsCount.getStyleClass().add("course-card-subtitle");

        top.getChildren().addAll(level, title, lessonsCount);

        HBox bottom = new HBox();
        bottom.getStyleClass().add("course-card-bottom");
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setSpacing(10);

        String published = course.getPublishedAt() == null
                ? "-"
                : course.getPublishedAt().toLocalDateTime().toLocalDate().toString().replace("-", " ");

        Label publishedAt = new Label(published);
        publishedAt.getStyleClass().add("course-card-subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnStart = new Button("Start →");
        btnStart.getStyleClass().add("lesson-step-button");
        btnStart.setOnAction(e -> {
            FrontNavigationState.setSelectedCourseId(course.getId());
            FrontNavigationState.setSelectedCourseTitle(course.getTitle());
            FrontRouter.goTo("/views/frontoffice/front-lessons.fxml");
        });

        bottom.getChildren().addAll(publishedAt, spacer, btnStart);

        card.getChildren().addAll(top, bottom);
        return card;
    }
}