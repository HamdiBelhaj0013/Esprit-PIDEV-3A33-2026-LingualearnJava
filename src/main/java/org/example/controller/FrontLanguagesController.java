package org.example.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.pedagogicalcontent.PlatformLanguage;
import org.example.service.CourseService;
import org.example.service.PlatformLanguageService;

import java.io.InputStream;
import java.util.List;

public class FrontLanguagesController {

    @FXML
    private FlowPane languagesContainer;

    private final PlatformLanguageService languageService = new PlatformLanguageService();
    private final CourseService courseService = new CourseService();

    @FXML
    public void initialize() {
        loadLanguages();
    }

    private void loadLanguages() {
        try {
            List<PlatformLanguage> languages = languageService.getEnabledLanguages();
            languagesContainer.getChildren().clear();

            for (PlatformLanguage language : languages) {
                int courseCount = courseService.getCoursesByLanguage(language.getId()).size();
                VBox card = createLanguageCard(language, courseCount);
                languagesContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createLanguageCard(PlatformLanguage language, int courseCount) {
        boolean enrolled = courseCount > 0;

        VBox card = new VBox();
        card.getStyleClass().add("language-card");

        VBox header = new VBox();
        header.getStyleClass().add("language-card-header");
        header.setAlignment(Pos.CENTER);

        Region graphic = buildFlagGraphic(language);
        header.getChildren().add(graphic);

        VBox body = new VBox(12);
        body.getStyleClass().add("language-card-body");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_RIGHT);

        if (enrolled) {
            Label enrolledBadge = new Label("✓ ENROLLED");
            enrolledBadge.getStyleClass().add("enrolled-badge");
            topRow.getChildren().add(enrolledBadge);
        }

        Label name = new Label(language.getName());
        name.getStyleClass().add("language-card-title");

        Label code = new Label(language.getCode() == null ? "-" : language.getCode().toUpperCase());
        code.getStyleClass().add("language-code-chip");

        Label courses = new Label(courseCount + " course" + (courseCount > 1 ? "s" : "") + " available");
        courses.getStyleClass().add("language-card-subtitle");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (enrolled) {
            Button btnView = new Button("📚 View Courses");
            btnView.getStyleClass().add("view-courses-button");
            btnView.setOnAction(e -> openLanguage(language));

            Button btnLeave = new Button("Leave");
            btnLeave.getStyleClass().add("leave-button");

            actions.getChildren().addAll(btnView, btnLeave);
        } else {
            Button btnEnroll = new Button("+ Enroll");
            btnEnroll.getStyleClass().add("enroll-button");
            btnEnroll.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnEnroll, Priority.ALWAYS);
            btnEnroll.setOnAction(e -> openLanguage(language));
            actions.getChildren().add(btnEnroll);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        body.getChildren().addAll(topRow, name, code, courses, spacer, actions);
        body.setPadding(new Insets(16, 18, 18, 18));

        card.getChildren().addAll(header, body);
        return card;
    }

    private Region buildFlagGraphic(PlatformLanguage language) {
        String flagUrl = language.getFlagUrl();

        if (flagUrl != null && !flagUrl.isBlank()) {
            try {
                Image image;

                if (flagUrl.startsWith("http://") || flagUrl.startsWith("https://") || flagUrl.startsWith("file:")) {
                    image = new Image(flagUrl, 52, 52, true, true);
                } else {
                    InputStream is = getClass().getResourceAsStream(flagUrl);
                    if (is != null) {
                        image = new Image(is, 52, 52, true, true);
                    } else {
                        image = new Image("file:" + flagUrl, 52, 52, true, true);
                    }
                }

                if (!image.isError()) {
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(52);
                    imageView.setFitHeight(52);
                    imageView.setPreserveRatio(true);

                    VBox box = new VBox(imageView);
                    box.setAlignment(Pos.CENTER);
                    return box;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Label fallback = new Label(language.getCode() == null ? "🌐" : language.getCode().toUpperCase());
        fallback.getStyleClass().add("language-flag-badge");

        VBox box = new VBox(fallback);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void openLanguage(PlatformLanguage language) {
        FrontNavigationState.setSelectedLanguageId(language.getId());
        FrontNavigationState.setSelectedLanguageName(language.getName());
        FrontRouter.goTo("/fxml/user/front-courses.fxml");
    }
}