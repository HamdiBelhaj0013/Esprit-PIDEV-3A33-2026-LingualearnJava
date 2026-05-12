package org.example.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.example.entities.Lesson;
import org.example.services.FrontProgressService;
import org.example.services.LessonService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FrontLessonsController {

    @FXML private Label courseTitle;
    @FXML private Label backToCoursesLabel;
    @FXML private ProgressBar lessonProgressBar;
    @FXML private Label progressText;
    @FXML private StackPane contentHolder;

    @FXML private Button btnReadTab;
    @FXML private Button btnDoneTab;

    @FXML private Button sideReadButton;
    @FXML private Button sideDoneButton;

    @FXML private Label xpRewardLabel;

    private final LessonService lessonService          = new LessonService();
    private final FrontProgressService progressService = new FrontProgressService();

    private List<Lesson> lessons           = new ArrayList<>();
    private Lesson       currentLesson;
    private int          currentLessonIndex = 0;
    private int          currentStep        = 0; // 0 = read, 1 = done

    // ── Reading timer ──
    private boolean  lessonRead      = false;
    private int      readSecondsLeft = 0;
    private Timeline readTimer       = null;

    private Process currentTtsProcess = null;

    private static com.sun.net.httpserver.HttpServer micServer = null;
    private static int micPort = 0;
    private volatile boolean forceStop = false;

    // ─────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        courseTitle.setText(FrontNavigationState.getSelectedCourseTitle());
        backToCoursesLabel.setOnMouseClicked(e ->
                FrontRouter.goTo("/fxml/modules/frontoffice/front-courses.fxml")
        );

        btnReadTab.setOnAction(e -> switchStep(0));
        btnDoneTab.setOnAction(e -> {
            if (!lessonRead) showReadFirstWarning();
            else switchStep(1);
        });

        sideReadButton.setOnAction(e -> switchStep(0));
        sideDoneButton.setOnAction(e -> {
            if (!lessonRead) showReadFirstWarning();
            else switchStep(1);
        });

        loadLessons();
    }

    // ─────────────────────────────────────────────────────────────
    // LOAD
    // ─────────────────────────────────────────────────────────────

    private void loadLessons() {
        try {
            int courseId = FrontNavigationState.getSelectedCourseId();
            lessons = lessonService.getLessonsByCourse(courseId);

            if (lessons == null || lessons.isEmpty()) {
                showEmptyState();
                return;
            }

            currentLessonIndex = 0;
            currentLesson      = lessons.get(currentLessonIndex);
            lessonRead         = false;
            xpRewardLabel.setText(currentLesson.getXpReward() + " XP");

            updateDoneTabState();
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

    // ─────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────

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
        Node content = (step == 0) ? buildReadView() : buildDoneView();
        contentHolder.getChildren().setAll(content);
    }

    private void updateTabs() {
        setTabActive(btnReadTab, currentStep == 0);
        setTabActive(btnDoneTab, currentStep == 1);
    }

    private void updateSideMenu() {
        setSideActive(sideReadButton, currentStep == 0);
        setSideActive(sideDoneButton, currentStep == 1);
    }

    private void updateProgress() {
        double progress = (currentStep + 1) / 2.0;
        lessonProgressBar.setProgress(progress);
        progressText.setText((currentStep + 1) + " / 2");
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

    // ─────────────────────────────────────────────────────────────
    // DONE TAB STATE
    // ─────────────────────────────────────────────────────────────

    private void updateDoneTabState() {
        btnDoneTab.setDisable(!lessonRead);
        sideDoneButton.setDisable(!lessonRead);

        if (!lessonRead) {
            btnDoneTab.setTooltip(new Tooltip("📖 Read the lesson first!"));
            sideDoneButton.setTooltip(new Tooltip("📖 Read the lesson first!"));
        } else {
            btnDoneTab.setTooltip(null);
            sideDoneButton.setTooltip(null);
        }
    }

    private void showReadFirstWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setTitle("Read First!");
        alert.setContentText("📖 You must finish reading the lesson before completing it.\nWait for the timer to finish.");
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    // READING TIMER
    // ─────────────────────────────────────────────────────────────

    /**
     * Temps minimum de lecture calculé selon le nombre de mots :
     * ~200 mots/minute → minimum 30s, maximum 120s.
     */
    private int computeReadSeconds(String content) {
        if (content == null || content.isBlank()) return 30;
        int words   = content.trim().split("\\s+").length;
        int seconds = (int) Math.round(words / 200.0 * 60);
        return Math.max(30, Math.min(seconds, 120));
    }

    private void startReadTimer(ProgressBar timerBar, Label timerLabel, Button quickDoneBtn) {
        stopReadTimer();

        readSecondsLeft  = computeReadSeconds(currentLesson != null ? currentLesson.getContent() : "");
        int totalSeconds = readSecondsLeft;

        timerBar.setProgress(0);
        timerBar.setStyle("-fx-accent:#f59e0b;");
        updateTimerLabel(timerLabel, readSecondsLeft);

        readTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            readSecondsLeft--;
            double progress = 1.0 - (readSecondsLeft / (double) totalSeconds);
            timerBar.setProgress(progress);
            updateTimerLabel(timerLabel, readSecondsLeft);

            if (readSecondsLeft <= 0) {
                readTimer.stop();
                lessonRead = true;

                timerBar.setProgress(1.0);
                timerBar.setStyle("-fx-accent:#16a34a;");

                timerLabel.setText("✅ Reading complete! You can now go to Done.");
                timerLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#16a34a;");

                quickDoneBtn.setDisable(false);
                updateDoneTabState();
            }
        }));

        readTimer.setCycleCount(totalSeconds);
        readTimer.play();
    }

    private void stopReadTimer() {
        if (readTimer != null) {
            readTimer.stop();
            readTimer = null;
        }
    }

    private void updateTimerLabel(Label label, int secondsLeft) {
        int mins = secondsLeft / 60;
        int secs = secondsLeft % 60;
        String time = String.format("%d:%02d", mins, secs);

        if (secondsLeft > 10) {
            label.setText("⏳ Read time remaining: " + time);
            label.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#92400e;");
        } else {
            label.setText("🔥 Almost done! " + time);
            label.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#dc2626;");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // READ VIEW
    // ─────────────────────────────────────────────────────────────

    private Node buildReadView() {
        VBox wrapper = new VBox(24);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(760);
        wrapper.setPadding(new Insets(0, 0, 60, 0));

        Label lessonIndex = new Label("LESSON " + (currentLessonIndex + 1) + " OF " + lessons.size());
        lessonIndex.getStyleClass().add("player-progress-text");

        VBox card = new VBox(20);
        card.getStyleClass().add("read-card");

        // ── Titre de la leçon ──
        Label title = new Label(currentLesson.getTitle());
        title.getStyleClass().add("lesson-title");

        // ── Contenu de la leçon (affiché sous le titre) ──
        Label contentLabel = new Label(safeText(currentLesson.getContent(), "No content available."));
        contentLabel.setWrapText(true);
        contentLabel.setStyle(
                "-fx-font-size:14px;" +
                        "-fx-text-fill:#334155;" +
                        "-fx-line-spacing:4;"
        );

        VBox contentBox = new VBox(contentLabel);
        contentBox.setStyle(
                "-fx-background-color:#f8fafc;" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:#e2e8f0;" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:10;" +
                        "-fx-padding:16;"
        );

        // ── Timer box ──
        VBox timerBox = new VBox(8);
        timerBox.setStyle(
                "-fx-background-color:#fffbeb;" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:#fde68a;" +
                        "-fx-border-radius:10;" +
                        "-fx-padding:14 16;"
        );

        int totalSecs = computeReadSeconds(currentLesson.getContent());
        int mins      = totalSecs / 60;
        int secs      = totalSecs % 60;

        Label timerHint = new Label(
                "📖 Minimum reading time: " + String.format("%d:%02d", mins, secs) +
                        " min  —  Done unlocks when the timer reaches 100%."
        );
        timerHint.setStyle("-fx-font-size:11px; -fx-text-fill:#92400e;");
        timerHint.setWrapText(true);

        ProgressBar timerBar = new ProgressBar(0);
        timerBar.setMaxWidth(Double.MAX_VALUE);
        timerBar.setPrefHeight(12);
        timerBar.setStyle("-fx-accent:#f59e0b;");

        Label timerLabel = new Label();
        timerLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#92400e;");

        timerBox.getChildren().addAll(timerHint, timerBar, timerLabel);

        // ── Boutons ──
        Button btnPdf = new Button("📄 Download PDF");
        btnPdf.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");
        btnPdf.setOnAction(e -> exportPdf());

        Button btnStart = new Button("▶ Start Audio");
        btnStart.setStyle("-fx-background-color:#16a34a; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");

        Button btnStop = new Button("⏹ Stop");
        btnStop.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");
        btnStop.setOnAction(e -> stopSpeaking());

        Button quickDoneBtn = new Button("✅ Done Reading →");
        quickDoneBtn.setStyle(
                "-fx-background-color:#2563eb; -fx-text-fill:white; -fx-font-weight:bold;" +
                        "-fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;"
        );
        quickDoneBtn.setDisable(!lessonRead);
        quickDoneBtn.setOnAction(e -> switchStep(1));

        btnStart.setOnAction(e -> speakText(currentLesson.getContent()));

        HBox topButtons = new HBox(12, btnPdf, btnStart, btnStop, quickDoneBtn);
        topButtons.setAlignment(Pos.CENTER_LEFT);

        // ── Pronunciation practice ──
        VBox practiceBox = new VBox(12);
        practiceBox.setStyle(
                "-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0;" +
                        "-fx-border-width:1; -fx-border-radius:12; -fx-background-radius:12; -fx-padding:20;"
        );

        Label practiceTitle = new Label("🎤 Pronunciation Practice");
        practiceTitle.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");

        TextField wordInput = new TextField();
        wordInput.setPromptText("Écris un mot à pratiquer, exemple : Bonjour");
        wordInput.setStyle("-fx-font-size:14px; -fx-padding:10; -fx-background-radius:6; -fx-border-color:#cbd5e1; -fx-border-width:1; -fx-border-radius:6;");

        Button btnEcouter = new Button("🔊 Écouter");
        btnEcouter.setStyle("-fx-background-color:#16a34a; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 20; -fx-cursor:hand;");
        btnEcouter.setOnAction(e -> {
            if (!wordInput.getText().trim().isEmpty()) speakText(wordInput.getText().trim());
        });

        Button btnParler = new Button("🎤 Parler 3s");
        btnParler.setStyle("-fx-background-color:#8b5cf6; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 20; -fx-cursor:hand;");

        HBox practiceButtons = new HBox(12, btnEcouter, btnParler);
        practiceButtons.setAlignment(Pos.CENTER_LEFT);

        Label lblMotReconnu = new Label("Mot reconnu : -");
        lblMotReconnu.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        Label lblScore = new Label("Score : -");
        lblScore.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        Label lblFeedback = new Label("Feedback : -");
        lblFeedback.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        btnParler.setOnAction(e -> {
            String targetWord = wordInput.getText().trim();
            if (targetWord.isEmpty()) { lblMotReconnu.setText("Veuillez écrire un mot d'abord."); return; }
            lblMotReconnu.setText("Mot reconnu : Écoute en cours...");
            lblScore.setText("Score : -");
            lblFeedback.setText("Feedback : -");
            openSpeechBrowser(targetWord, lblMotReconnu, lblScore, lblFeedback);
        });

        practiceBox.getChildren().addAll(
                practiceTitle, wordInput, practiceButtons,
                lblMotReconnu, lblScore, lblFeedback
        );

        // ── Assemblage : titre → contenu → timer → boutons → pratique ──
        card.getChildren().addAll(title, contentBox, timerBox, topButtons, practiceBox);
        wrapper.getChildren().addAll(lessonIndex, card);

        // ── Démarrer le timer automatiquement ──
        if (!lessonRead) {
            startReadTimer(timerBar, timerLabel, quickDoneBtn);
        } else {
            timerBar.setProgress(1.0);
            timerBar.setStyle("-fx-accent:#16a34a;");
            timerLabel.setText("✅ Reading complete! You can now go to Done.");
            timerLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#16a34a;");
        }

        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────
    // DONE VIEW
    // ─────────────────────────────────────────────────────────────

    private Node buildDoneView() {
        stopReadTimer();

        VBox wrapper = new VBox(18);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(760);

        VBox doneCard = new VBox(12);
        doneCard.getStyleClass().add("done-card");

        Label title    = new Label("Lesson Complete!");
        title.getStyleClass().add("done-title");

        Label subtitle = new Label("Great work finishing " + currentLesson.getTitle() + ".");
        subtitle.getStyleClass().add("done-subtitle");

        Label xp = new Label("⭐ +" + currentLesson.getXpReward() + " XP earned");
        xp.getStyleClass().add("xp-badge");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        Button complete = new Button("✅ Lesson Completed!");
        complete.getStyleClass().add("success-button");

        Button allLessons = new Button("📚 All Lessons");
        allLessons.getStyleClass().add("secondary-pill-button");
        allLessons.setOnAction(e ->
                FrontRouter.goTo("/fxml/modules/frontoffice/front-courses.fxml")
        );

        Button nextLesson = new Button("Next Lesson →");
        nextLesson.getStyleClass().add("lesson-step-button");
        nextLesson.setOnAction(e -> openNextLesson());

        actions.getChildren().addAll(complete, allLessons, nextLesson);

        VBox recommendationArea = new VBox(12);
        recommendationArea.setAlignment(Pos.CENTER_LEFT);
        recommendationArea.setPadding(new Insets(20, 0, 0, 0));
        recommendationArea.setVisible(false);
        recommendationArea.setManaged(false);

        complete.setOnAction(e -> {
            completeCurrentLesson();
            complete.setDisable(true);
            complete.setText("✅ Lesson Completed!");
            loadRecommendations(recommendationArea);
            recommendationArea.setVisible(true);
            recommendationArea.setManaged(true);
        });

        doneCard.getChildren().addAll(title, subtitle, xp, actions, recommendationArea);
        wrapper.getChildren().add(doneCard);

        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────
    // RECOMMENDATIONS
    // ─────────────────────────────────────────────────────────────

    private void loadRecommendations(VBox recommendationArea) {
        recommendationArea.getChildren().clear();

        Label recTitle = new Label("🎯 Recommended Next Lessons");
        recTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");
        recommendationArea.getChildren().add(recTitle);

        List<Lesson> recommended = new ArrayList<>();
        for (int i = currentLessonIndex + 1; i < lessons.size(); i++) {
            recommended.add(lessons.get(i));
            if (recommended.size() == 3) break;
        }

        if (recommended.isEmpty()) {
            Label done = new Label("🎉 You've completed all lessons in this course!");
            done.setStyle("-fx-font-size:13px; -fx-text-fill:#16a34a; -fx-font-weight:bold;");
            recommendationArea.getChildren().add(done);
            return;
        }

        for (Lesson lesson : recommended) {
            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle(
                    "-fx-background-color:#f1f5f9; -fx-background-radius:10;" +
                            "-fx-border-color:#e2e8f0; -fx-border-radius:10; -fx-padding:12 16;"
            );
            card.setCursor(javafx.scene.Cursor.HAND);

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label name = new Label("📘 " + lesson.getTitle());
            name.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");

            Label xpLabel = new Label("⭐ " + lesson.getXpReward() + " XP");
            xpLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

            info.getChildren().addAll(name, xpLabel);

            Button goBtn = new Button("Start →");
            goBtn.setStyle(
                    "-fx-background-color:#2563eb; -fx-text-fill:white; -fx-font-weight:bold;" +
                            "-fx-background-radius:8; -fx-padding:8 14; -fx-cursor:hand;"
            );

            int lessonIdx = lessons.indexOf(lesson);
            goBtn.setOnAction(ev -> {
                currentLessonIndex = lessonIdx;
                currentLesson      = lessons.get(currentLessonIndex);
                lessonRead         = false;
                xpRewardLabel.setText(currentLesson.getXpReward() + " XP");
                updateDoneTabState();
                switchStep(0);
            });

            card.getChildren().addAll(info, goBtn);
            recommendationArea.getChildren().add(card);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // COMPLETE & NEXT
    // ─────────────────────────────────────────────────────────────

    private void completeCurrentLesson() {
        try {
            int currentUserId = 124;
            progressService.completeLesson(
                    currentUserId,
                    currentLesson.getId(),
                    currentLesson.getXpReward()
            );

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
            lessonRead    = false;
            xpRewardLabel.setText(currentLesson.getXpReward() + " XP");
            updateDoneTabState();
            switchStep(0);
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setTitle("Done");
            alert.setContentText("You finished all lessons in this course.");
            alert.showAndWait();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // TTS
    // ─────────────────────────────────────────────────────────────

    private void speakText(String text) {
        stopSpeaking();
        new Thread(() -> {
            try {
                String safe = safeText(text, "").replace("'", "''");
                String psCommand =
                        "Add-Type -AssemblyName System.Speech; " +
                                "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                "$synth.Speak('" + safe + "');";
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", psCommand);
                currentTtsProcess = pb.start();
                currentTtsProcess.waitFor();
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void stopSpeaking() {
        if (currentTtsProcess != null && currentTtsProcess.isAlive()) {
            currentTtsProcess.destroyForcibly();
            try { new ProcessBuilder("taskkill", "/F", "/IM", "powershell.exe").start(); }
            catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SPEECH BROWSER
    // ─────────────────────────────────────────────────────────────

    private void openSpeechBrowser(String targetWord, Label lblMotReconnu, Label lblScore, Label lblFeedback) {
        try {
            if (micServer == null) {
                micServer = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
                micPort = micServer.getAddress().getPort();
            } else {
                try { micServer.removeContext("/speech.html"); } catch (Exception ignored) {}
                try { micServer.removeContext("/result");      } catch (Exception ignored) {}
                try { micServer.removeContext("/poll");        } catch (Exception ignored) {}
            }

            forceStop = false;
            String html = buildSpeechHtml();

            micServer.createContext("/speech.html", exchange -> {
                byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            });

            micServer.createContext("/result", exchange -> {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String spoken = new String(
                            exchange.getRequestBody().readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8).trim();

                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().close();

                    int score = computeSimilarity(targetWord, spoken);

                    javafx.application.Platform.runLater(() -> {
                        lblMotReconnu.setText("Mot reconnu : " + spoken);
                        lblScore.setText("Score : " + score + "%");

                        if (score >= 70) {
                            lblFeedback.setText("Feedback : Excellent !");
                            lblFeedback.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#16a34a;");
                        } else if (score >= 40) {
                            lblFeedback.setText("Feedback : Presque, réessayez.");
                            lblFeedback.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#f59e0b;");
                        } else {
                            lblFeedback.setText("Feedback : Incorrect.");
                            lblFeedback.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#dc2626;");
                        }

                        lblMotReconnu.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#334155;");
                        lblScore.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#334155;");
                    });
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.getResponseBody().close();
                }
            });

            micServer.createContext("/poll", exchange -> {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                byte[] b = (forceStop ? "STOP" : "RUN").getBytes();
                exchange.sendResponseHeaders(200, b.length);
                exchange.getResponseBody().write(b);
                exchange.getResponseBody().close();
            });

            micServer.setExecutor(null);
            try { micServer.start(); } catch (Exception ignored) {}

            java.awt.Desktop.getDesktop().browse(
                    new java.net.URI("http://localhost:" + micPort + "/speech.html"));

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private String buildSpeechHtml() {
        String port = String.valueOf(micPort);
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/><title>LinguaLearn – Pratique vocale</title>"
                + "<style>"
                + "body{margin:0;font-family:'Segoe UI',Arial,sans-serif;background:#eff6ff;"
                + "display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;padding:24px;box-sizing:border-box;}"
                + "h2{color:#2563eb;margin-bottom:6px;font-size:22px;}"
                + "p.sub{color:#64748b;font-size:13px;margin-bottom:28px;}"
                + "#micBtn{width:100px;height:100px;border-radius:50%;border:none;font-size:44px;"
                + "background:#dbeafe;cursor:pointer;transition:all .2s;box-shadow:0 4px 14px rgba(37,99,235,.2);}"
                + "#micBtn.active{background:#fee2e2;animation:pulse 1s infinite;}"
                + "@keyframes pulse{0%,100%{transform:scale(1);}50%{transform:scale(1.08);}}"
                + "#status{margin:18px 0 12px;font-size:14px;font-weight:600;color:#64748b;}"
                + "#transcript{width:90%;max-width:460px;min-height:64px;padding:14px 16px;"
                + "background:white;border-radius:12px;border:2px solid #bfdbfe;"
                + "font-size:16px;color:#1e40af;line-height:1.5;word-break:break-word;}"
                + "#sendStatus{margin-top:14px;font-size:13px;color:#16a34a;font-weight:bold;min-height:20px;}"
                + "</style></head><body>"
                + "<h2>🎤 Pratique de prononciation</h2>"
                + "<p class='sub'>Parlez maintenant — l'écoute démarre automatiquement</p>"
                + "<button id='micBtn' onclick='toggle()'>🎤</button>"
                + "<div id='status'>En attente du microphone…</div>"
                + "<div id='transcript'>Le texte reconnu apparaîtra ici…</div>"
                + "<div id='sendStatus'></div>"
                + "<script>"
                + "var rec,active=false,full='';"
                + "function start(){var R=window.SpeechRecognition||window.webkitSpeechRecognition;"
                + "if(!R){document.getElementById('status').innerText='❌ Utilisez Chrome ou Edge';return;}"
                + "rec=new R();rec.lang='fr-FR';rec.continuous=true;rec.interimResults=true;"
                + "rec.onstart=function(){active=true;full='';document.getElementById('micBtn').className='active';document.getElementById('micBtn').innerText='⏹';document.getElementById('status').innerText='🔴 Écoute en cours…';};"
                + "rec.onresult=function(e){var interim='',final_='';for(var i=0;i<e.results.length;i++){if(e.results[i].isFinal)final_+=e.results[i][0].transcript;else interim+=e.results[i][0].transcript;}full=final_;document.getElementById('transcript').innerText=(final_+' '+interim).trim();};"
                + "rec.onerror=function(e){document.getElementById('status').innerText='Erreur: '+e.error;active=false;document.getElementById('micBtn').className='';document.getElementById('micBtn').innerText='🎤';};"
                + "rec.onend=function(){active=false;document.getElementById('micBtn').className='';document.getElementById('micBtn').innerText='🎤';document.getElementById('status').innerText='✅ Envoi du résultat…';"
                + "var text=document.getElementById('transcript').innerText.trim();"
                + "if(text&&text!=='Le texte reconnu apparaîtra ici…'){"
                + "fetch('http://localhost:" + port + "/result',{method:'POST',headers:{'Content-Type':'text/plain;charset=UTF-8'},body:text})"
                + ".then(function(){document.getElementById('sendStatus').innerText='✅ Résultat envoyé à LinguaLearn !';document.getElementById('status').innerText='Terminé. Vous pouvez fermer cet onglet.';})"
                + ".catch(function(){document.getElementById('sendStatus').innerText='⚠️ Envoi échoué — vérifiez la connexion.';});"
                + "}else{document.getElementById('status').innerText='Aucun texte capturé. Réessayez.';}};"
                + "rec.start();}"
                + "function toggle(){if(active&&rec)rec.stop();else start();}"
                + "setInterval(function(){if(active){fetch('http://localhost:" + port + "/poll').then(function(r){return r.text();}).then(function(t){if(t==='STOP'){active=false;rec.stop();}});}},1000);"
                + "window.onload=function(){setTimeout(start,600);};"
                + "</script></body></html>";
    }

    // ─────────────────────────────────────────────────────────────
    // UTILS
    // ─────────────────────────────────────────────────────────────

    private int computeSimilarity(String reference, String spoken) {
        if (reference == null || spoken == null || spoken.isBlank()) return 0;

        String[] refWords = reference.toLowerCase()
                .replaceAll("[^a-zàâçéèêëîïôûùüÿœæ ]", " ").split("\\s+");
        String[] spokenWords = spoken.toLowerCase()
                .replaceAll("[^a-zàâçéèêëîïôûùüÿœæ ]", " ").split("\\s+");

        if (refWords.length == 0) return 0;

        Set<String> refSet = new HashSet<>(Arrays.asList(refWords));
        long matches = Arrays.stream(spokenWords).filter(refSet::contains).count();
        return (int) Math.min(100, (matches * 100) / refWords.length);
    }

    private void exportPdf() {
        if (currentLesson == null) return;

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Lesson PDF");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        fc.setInitialFileName(currentLesson.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");

        java.io.File file = fc.showSaveDialog(null);
        if (file == null) return;

        new Thread(() -> {
            try {
                com.itextpdf.kernel.pdf.PdfWriter   writer = new com.itextpdf.kernel.pdf.PdfWriter(file.getAbsolutePath());
                com.itextpdf.kernel.pdf.PdfDocument pdf    = new com.itextpdf.kernel.pdf.PdfDocument(writer);
                com.itextpdf.layout.Document        doc    = new com.itextpdf.layout.Document(pdf);

                com.itextpdf.kernel.colors.Color blue = new com.itextpdf.kernel.colors.DeviceRgb(37,  99, 235);
                com.itextpdf.kernel.colors.Color gray = new com.itextpdf.kernel.colors.DeviceRgb(100, 116, 139);

                doc.add(new com.itextpdf.layout.element.Paragraph(currentLesson.getTitle())
                        .setFontSize(22).setBold().setFontColor(blue).setMarginBottom(8));
                doc.add(new com.itextpdf.layout.element.Paragraph("Course: " + FrontNavigationState.getSelectedCourseTitle())
                        .setFontSize(11).setFontColor(gray).setMarginBottom(4));
                doc.add(new com.itextpdf.layout.element.Paragraph("XP Reward: " + currentLesson.getXpReward() + " XP")
                        .setFontSize(11).setFontColor(gray).setMarginBottom(16));
                doc.add(new com.itextpdf.layout.element.LineSeparator(
                        new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginBottom(16));
                doc.add(new com.itextpdf.layout.element.Paragraph("📖 Lesson Content")
                        .setFontSize(14).setBold().setMarginBottom(8));
                doc.add(new com.itextpdf.layout.element.Paragraph(
                        safeText(currentLesson.getContent(), "No content available."))
                        .setFontSize(12).setMarginBottom(16));
                doc.close();

                javafx.application.Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setHeaderText(null); a.setTitle("PDF Saved");
                    a.setContentText("✅ Lesson saved to:\n" + file.getAbsolutePath());
                    a.showAndWait();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setHeaderText(null);
                    a.setContentText("PDF error: " + ex.getMessage());
                    a.showAndWait();
                });
            }
        }).start();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}