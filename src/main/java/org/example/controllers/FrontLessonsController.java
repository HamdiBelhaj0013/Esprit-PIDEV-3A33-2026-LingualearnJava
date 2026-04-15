package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Lesson;
import org.example.services.FrontProgressService;
import org.example.services.LessonService;

import java.util.*;
import java.util.stream.Collectors;

public class FrontLessonsController {

    @FXML private Label courseTitle;
    @FXML private Label backToCoursesLabel;
    @FXML private ProgressBar lessonProgressBar;
    @FXML private Label progressText;
    @FXML private StackPane contentHolder;

    @FXML private Button btnReadTab;
    @FXML private Button btnVocabTab;
    @FXML private Button btnQuizTab;
    @FXML private Button btnDoneTab;

    @FXML private Button sideReadButton;
    @FXML private Button sideVocabButton;
    @FXML private Button sideQuizButton;
    @FXML private Button sideDoneButton;

    @FXML private Label xpRewardLabel;

    private final LessonService lessonService = new LessonService();
    private final FrontProgressService progressService = new FrontProgressService();

    private List<Lesson> lessons = new ArrayList<>();
    private Lesson currentLesson;
    private int currentLessonIndex = 0;
    private int currentStep = 0; // 0 read, 1 vocab, 2 quiz, 3 done

    private final Random random = new Random();

    @FXML
    public void initialize() {
        courseTitle.setText(FrontNavigationState.getSelectedCourseTitle());
        backToCoursesLabel.setOnMouseClicked(e -> FrontRouter.goTo("/views/frontoffice/front-courses.fxml"));

        btnReadTab.setOnAction(e -> switchStep(0));
        btnVocabTab.setOnAction(e -> switchStep(1));
        btnQuizTab.setOnAction(e -> switchStep(2));
        btnDoneTab.setOnAction(e -> switchStep(3));

        sideReadButton.setOnAction(e -> switchStep(0));
        sideVocabButton.setOnAction(e -> switchStep(1));
        sideQuizButton.setOnAction(e -> switchStep(2));
        sideDoneButton.setOnAction(e -> switchStep(3));

        loadLessons();
    }

    private void loadLessons() {
        try {
            int courseId = FrontNavigationState.getSelectedCourseId();
            lessons = lessonService.getLessonsByCourse(courseId);

            if (lessons == null || lessons.isEmpty()) {
                showEmptyState();
                return;
            }

            currentLessonIndex = 0;
            currentLesson = lessons.get(currentLessonIndex);
            xpRewardLabel.setText(currentLesson.getXpReward() + " XP");
            switchStep(0);
        } catch (Exception e) {
            e.printStackTrace();
            showEmptyState();
        }
    }

    private void showEmptyState() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label title = new Label("No lesson available");
        title.getStyleClass().add("lesson-title");

        Label sub = new Label("This course has no lessons yet.");
        sub.getStyleClass().add("lesson-placeholder");

        box.getChildren().addAll(title, sub);
        contentHolder.getChildren().setAll(box);
    }

    private void switchStep(int step) {
        currentStep = step;
        updateTabs();
        updateSideMenu();
        updateProgress();

        if (currentLesson == null) {
            showEmptyState();
            return;
        }

        xpRewardLabel.setText(currentLesson.getXpReward() + " XP");

        Node content = switch (step) {
            case 0 -> buildReadView();
            case 1 -> buildVocabView();
            case 2 -> buildQuizView();
            case 3 -> buildDoneView();
            default -> buildReadView();
        };

        contentHolder.getChildren().setAll(content);
    }

    private void updateTabs() {
        setTabActive(btnReadTab, currentStep == 0);
        setTabActive(btnVocabTab, currentStep == 1);
        setTabActive(btnQuizTab, currentStep == 2);
        setTabActive(btnDoneTab, currentStep == 3);
    }

    private void updateSideMenu() {
        setSideActive(sideReadButton, currentStep == 0);
        setSideActive(sideVocabButton, currentStep == 1);
        setSideActive(sideQuizButton, currentStep == 2);
        setSideActive(sideDoneButton, currentStep == 3);
    }

    private void setTabActive(Button button, boolean active) {
        button.getStyleClass().remove("player-step-tab-active");
        if (active) {
            button.getStyleClass().add("player-step-tab-active");
            button.setStyle("-fx-background-color:#2563eb; -fx-text-fill:white; -fx-background-radius:8; -fx-background-insets:0; -fx-border-insets:0; -fx-border-color:transparent;");
        } else {
            button.setStyle("-fx-background-color:transparent; -fx-background-radius:8; -fx-background-insets:0; -fx-border-insets:0; -fx-border-color:transparent;");
        }
    }

    private void setSideActive(Button button, boolean active) {
        button.getStyleClass().remove("side-step-active");
        if (active && !button.getStyleClass().contains("side-step-active")) {
            button.getStyleClass().add("side-step-active");
        }
    }

    private void updateProgress() {
        double progress = (currentStep + 1) / 4.0;
        lessonProgressBar.setProgress(progress);
        progressText.setText((currentStep + 1) + " / 4");
    }

    private Node buildReadView() {
        VBox wrapper = new VBox(18);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(760);

        Label lessonIndex = new Label("LESSON " + (currentLessonIndex + 1) + " OF " + lessons.size());
        lessonIndex.getStyleClass().add("player-progress-text");

        VBox card = new VBox(16);
        card.getStyleClass().add("read-card");

        Label title = new Label(currentLesson.getTitle());
        title.getStyleClass().add("lesson-title");

        Label content = new Label(safeText(currentLesson.getContent(), "No reading content available."));
        content.setWrapText(true);
        content.getStyleClass().add("lesson-content");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button next = new Button("Next: Vocabulary →");
        next.getStyleClass().add("lesson-step-button");
        next.setOnAction(e -> switchStep(1));

        actions.getChildren().add(next);
        card.getChildren().addAll(title, content, actions);

        wrapper.getChildren().addAll(lessonIndex, card);
        return wrapper;
    }

    private Node buildVocabView() {
        VBox wrapper = new VBox(18);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(760);

        List<VocabItem> vocabItems = parseVocabulary(currentLesson.getVocabularyData());

        Label tip = new Label("Click any card to reveal the translation.");
        tip.getStyleClass().add("player-muted");

        VBox list = new VBox(14);

        if (vocabItems.isEmpty()) {
            VBox empty = new VBox(10);
            empty.getStyleClass().add("vocab-card");
            Label msg = new Label("No vocabulary data found for this lesson.");
            msg.getStyleClass().add("lesson-placeholder");
            empty.getChildren().add(msg);
            list.getChildren().add(empty);
        } else {
            for (VocabItem item : vocabItems) {
                list.getChildren().add(createVocabCard(item));
            }
        }

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button goQuiz = new Button("Next: Quiz →");
        goQuiz.getStyleClass().add("lesson-step-button");
        goQuiz.setOnAction(e -> switchStep(2));

        footer.getChildren().add(goQuiz);

        wrapper.getChildren().addAll(tip, list, footer);
        return wrapper;
    }

    private Node createVocabCard(VocabItem item) {
        VBox card = new VBox(8);
        card.getStyleClass().add("vocab-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(26));
        card.setMaxWidth(Double.MAX_VALUE);

        Label word = new Label(item.word());
        word.getStyleClass().add("vocab-word");

        Label hint = new Label("TAP TO REVEAL");
        hint.getStyleClass().add("player-progress-text");

        Label translation = new Label(item.translation());
        translation.getStyleClass().add("vocab-translation");
        translation.setVisible(false);
        translation.setManaged(false);

        card.getChildren().addAll(word, hint, translation);

        card.setOnMouseClicked(e -> {
            boolean show = !translation.isVisible();
            translation.setVisible(show);
            translation.setManaged(show);
            hint.setText(show ? "REVEALED" : "TAP TO REVEAL");
        });

        return card;
    }

    private Node buildQuizView() {
        VBox wrapper = new VBox(18);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(760);

        List<VocabItem> vocabItems = parseVocabulary(currentLesson.getVocabularyData());

        if (vocabItems.isEmpty()) {
            VBox empty = new VBox(12);
            empty.getStyleClass().add("quiz-card");
            Label title = new Label("No quiz available");
            title.getStyleClass().add("quiz-question");
            Label sub = new Label("Add vocabulary pairs in the lesson to auto-build a quiz.");
            sub.getStyleClass().add("lesson-placeholder");

            Button goDone = new Button("Go to completion →");
            goDone.getStyleClass().add("lesson-step-button");
            goDone.setOnAction(e -> switchStep(3));

            empty.getChildren().addAll(title, sub, goDone);
            return empty;
        }

        VBox quizList = new VBox(18);
        int maxQuestions = Math.min(3, vocabItems.size());

        for (int i = 0; i < maxQuestions; i++) {
            quizList.getChildren().add(createQuizCard(vocabItems.get(i), vocabItems));
        }

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button done = new Button("Next: Complete →");
        done.getStyleClass().add("lesson-step-button");
        done.setOnAction(e -> switchStep(3));

        footer.getChildren().add(done);
        wrapper.getChildren().addAll(quizList, footer);
        return wrapper;
    }

    private Node createQuizCard(VocabItem questionItem, List<VocabItem> allItems) {
        VBox card = new VBox(12);
        card.getStyleClass().add("quiz-card");

        Label question = new Label("What is the translation of \"" + questionItem.word() + "\"?");
        question.getStyleClass().add("quiz-question");

        List<String> options = buildOptions(questionItem.translation(), allItems);
        VBox choices = new VBox(10);

        Label resultLabel = new Label();
        resultLabel.getStyleClass().add("player-muted");

        for (String option : options) {
            Button choice = new Button(option);
            choice.getStyleClass().add("quiz-option");
            choice.setMaxWidth(Double.MAX_VALUE);

            choice.setOnAction(e -> {
                boolean correct = option.equalsIgnoreCase(questionItem.translation());
                resultLabel.setText(correct ? "Correct answer." : "Wrong answer. Correct: " + questionItem.translation());
                if (correct) {
                    choice.setStyle("-fx-background-color: #e9f9ef; -fx-border-color: #16a34a;");
                } else {
                    choice.setStyle("-fx-background-color: #fff1f2; -fx-border-color: #ef4444;");
                }
            });

            choices.getChildren().add(choice);
        }

        card.getChildren().addAll(question, choices, resultLabel);
        return card;
    }

    private List<String> buildOptions(String correct, List<VocabItem> allItems) {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.add(correct);

        List<String> others = allItems.stream()
                .map(VocabItem::translation)
                .filter(v -> !v.equalsIgnoreCase(correct))
                .distinct()
                .collect(Collectors.toList());

        Collections.shuffle(others);

        for (String other : others) {
            if (options.size() >= 3) {
                break;
            }
            options.add(other);
        }

        List<String> finalOptions = new ArrayList<>(options);
        Collections.shuffle(finalOptions);
        return finalOptions;
    }

    private Node buildDoneView() {
        VBox wrapper = new VBox(18);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(760);

        VBox doneCard = new VBox(12);
        doneCard.getStyleClass().add("done-card");

        Label title = new Label("Lesson Complete!");
        title.getStyleClass().add("done-title");

        Label subtitle = new Label("Great work finishing " + currentLesson.getTitle() + ".");
        subtitle.getStyleClass().add("done-subtitle");

        Label xp = new Label("⭐ +" + currentLesson.getXpReward() + " XP earned");
        xp.getStyleClass().add("xp-badge");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        Button complete = new Button("✅ Complete Lesson (+" + currentLesson.getXpReward() + " XP)");
        complete.getStyleClass().add("success-button");
        complete.setOnAction(e -> completeCurrentLesson());

        Button allLessons = new Button("📚 All Lessons");
        allLessons.getStyleClass().add("secondary-pill-button");
        allLessons.setOnAction(e -> FrontRouter.goTo("/views/frontoffice/front-courses.fxml"));

        Button nextLesson = new Button("Next Lesson →");
        nextLesson.getStyleClass().add("lesson-step-button");
        nextLesson.setOnAction(e -> openNextLesson());

        actions.getChildren().addAll(complete, allLessons, nextLesson);
        doneCard.getChildren().addAll(title, subtitle, xp, actions);

        wrapper.getChildren().add(doneCard);
        return wrapper;
    }

    private void completeCurrentLesson() {
        try {
            int currentUserId = 124; // Remplace par l'id réel du user connecté
            progressService.completeLesson(currentUserId, currentLesson.getId(), currentLesson.getXpReward());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setTitle("Success");
            alert.setContentText("Lesson completed and XP added.");
            alert.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setTitle("Error");
            alert.setContentText("Could not complete lesson.");
            alert.showAndWait();
        }
    }

    private void openNextLesson() {
        if (currentLessonIndex + 1 < lessons.size()) {
            currentLessonIndex++;
            currentLesson = lessons.get(currentLessonIndex);
            xpRewardLabel.setText(currentLesson.getXpReward() + " XP");
            switchStep(0);
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setTitle("Done");
            alert.setContentText("You finished all lessons in this course.");
            alert.showAndWait();
        }
    }

    private List<VocabItem> parseVocabulary(String raw) {
        List<VocabItem> items = new ArrayList<>();

        if (raw == null || raw.isBlank()) {
            return items;
        }

        String normalized = raw.replace("\r", "\n");
        String[] lines = normalized.split("[;\n]+");

        for (String line : lines) {
            String value = line.trim();
            if (value.isBlank()) {
                continue;
            }

            String[] parts;
            if (value.contains("=")) {
                parts = value.split("=", 2);
            } else if (value.contains(":")) {
                parts = value.split(":", 2);
            } else if (value.contains("-")) {
                parts = value.split("-", 2);
            } else if (value.contains("->")) {
                parts = value.split("->", 2);
            } else {
                continue;
            }

            String left = parts[0].trim();
            String right = parts[1].trim();

            if (!left.isBlank() && !right.isBlank()) {
                items.add(new VocabItem(left, right));
            }
        }

        return items;
    }

    private String safeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private record VocabItem(String word, String translation) {
    }
}