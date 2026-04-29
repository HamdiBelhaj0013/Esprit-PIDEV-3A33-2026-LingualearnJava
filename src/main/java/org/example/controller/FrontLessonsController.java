package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.pedagogicalcontent.Lesson;
import org.example.service.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;
import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import javax.sound.sampled.*;
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
    private final PdfExportService pdfService = new PdfExportService();
    private final TextToSpeechService ttsService = new TextToSpeechService();
    private final VoskSpeechToTextService sttService = new VoskSpeechToTextService();

    private MediaPlayer mediaPlayer;
    private List<Lesson> lessons = new ArrayList<>();
    private Lesson currentLesson;
    private int currentLessonIndex = 0;
    private int currentStep = 0;

    private final Random random = new Random();

    // ═══ Audio Player ═══

    private Player jlPlayer;
    private Thread playerThread;
    private boolean isPlaying = false;
    @FXML
    public void initialize() {
        courseTitle.setText(FrontNavigationState.getSelectedCourseTitle());
        backToCoursesLabel.setOnMouseClicked(e -> FrontRouter.goTo("/fxml/user/front-courses.fxml"));

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

    // ═══════════════════════════════════════════════════════════════════
    // ═══ READ VIEW ═══
    // ═══════════════════════════════════════════════════════════════════
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

        HBox apiButtons = new HBox(12);
        apiButtons.setAlignment(Pos.CENTER_LEFT);
        apiButtons.getStyleClass().add("api-buttons-container");

        Button btnPdf = new Button("📥  Download PDF");
        btnPdf.getStyleClass().addAll("btn-api", "btn-api-pdf");
        btnPdf.setOnAction(e -> downloadLessonPdf());

        Button btnStart = new Button("▶ Start");
        btnStart.getStyleClass().addAll("btn-api", "btn-api-tts");
        btnStart.setOnAction(e -> playLessonAudio());

        Button btnStop = new Button("⏹ Stop");
        btnStop.getStyleClass().addAll("btn-api", "btn-api-pdf");
        btnStop.setOnAction(e -> stopAudio());



        apiButtons.getChildren().addAll(btnPdf, btnStart, btnStop);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button next = new Button("Next: Vocabulary →");
        next.getStyleClass().add("lesson-step-button");
        next.setOnAction(e -> switchStep(1));

        actions.getChildren().add(next);
        card.getChildren().addAll(title, content, apiButtons, buildPronunciationPracticeBox(), actions);
        wrapper.getChildren().addAll(lessonIndex, card);
        return wrapper;
    }
    private Node buildPronunciationPracticeBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:12; -fx-border-color:#e2e8f0; -fx-border-radius:12;");

        Label title = new Label("🎤 Pronunciation Practice");
        title.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        TextField expectedWordField = new TextField();
        expectedWordField.setPromptText("Écris un mot à pratiquer, exemple : Bonjour");

        Label recognizedLabel = new Label("Mot reconnu : -");
        recognizedLabel.setStyle("-fx-text-fill:#111827; -fx-font-size:13px; -fx-font-weight:bold;");

        Label scoreLabel = new Label("Score : -");
        scoreLabel.setStyle("-fx-text-fill:#111827; -fx-font-size:13px; -fx-font-weight:bold;");

        Label feedbackLabel = new Label("Feedback : -");
        feedbackLabel.setStyle("-fx-text-fill:#111827; -fx-font-size:13px; -fx-font-weight:bold;");

        Button listenButton = new Button("🔊 Écouter");
        listenButton.getStyleClass().addAll("btn-api", "btn-api-tts");

        listenButton.setOnAction(e -> {
            String word = expectedWordField.getText();

            if (word == null || word.isBlank()) {
                showErrorDialog("Erreur", "Écris d'abord un mot.");
                return;
            }

            new Thread(() -> {
                try {
                    List<String> chunks = ttsService.textToSpeechChunks(word.trim(), "fr");
                    if (!chunks.isEmpty()) {
                        playAudioFiles(chunks);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() ->
                            showErrorDialog("TTS Error", ex.getMessage())
                    );
                }
            }).start();
        });

        Button speakButton = new Button("🎤 Parler 3s");
        speakButton.getStyleClass().addAll("btn-api", "btn-api-ai");

        speakButton.setOnAction(e -> {
            String expected = expectedWordField.getText();

            if (expected == null || expected.isBlank()) {
                showErrorDialog("Erreur", "Écris d'abord le mot attendu.");
                return;
            }

            recognizedLabel.setText("Mot reconnu : enregistrement...");
            scoreLabel.setText("Score : ...");
            feedbackLabel.setText("Feedback : parle maintenant.");

            new Thread(() -> {
                try {
                    File audioFile = recordAudioForSeconds(3);
                    System.out.println("Audio file: " + audioFile.getAbsolutePath());
                    System.out.println("File exists: " + audioFile.exists());
                    System.out.println("File size: " + audioFile.length());
                    if (audioFile == null || !audioFile.exists()) {
                        Platform.runLater(() ->
                                showErrorDialog("Erreur", "Aucun fichier audio enregistré.")
                        );
                        return;
                    }

                    String recognizedRaw = sttService.transcribeAudio(audioFile);

                    System.out.println("RECOGNIZED UI = " + recognizedRaw);

                    final String recognizedText =
                            (recognizedRaw == null || recognizedRaw.isBlank())
                                    ? "(rien détecté)"
                                    : recognizedRaw;

                    final double score = sttService.analyzePronunciation(expected, recognizedText);
                    final String feedback = sttService.getFeedback(score);

                    Platform.runLater(() -> {
                        recognizedLabel.setText("Mot reconnu : " + recognizedText);
                        scoreLabel.setText("Score : " + Math.round(score) + "%");
                        feedbackLabel.setText("Feedback : " + feedback);
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() ->
                            showErrorDialog("STT Error", ex.getMessage())
                    );
                }
            }).start();
        });

        HBox buttons = new HBox(10, listenButton, speakButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(
                title,
                expectedWordField,
                buttons,
                recognizedLabel,
                scoreLabel,
                feedbackLabel
        );

        return box;
    }
    // ═══════════════════════════════════════════════════════════════════
    // ═══ VOCAB VIEW ═══
    // ═══════════════════════════════════════════════════════════════════
    private Node buildVocabView() {
        VBox wrapper = new VBox(18);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(760);

        List<VocabItem> vocabItems = parseVocabulary(currentLesson.getVocabularyData());

        Label tip = new Label("Click any card to reveal the translation. Click 🔊 to hear pronunciation.");
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
        HBox cardBox = new HBox(12);
        cardBox.getStyleClass().add("vocab-card-with-speaker");
        cardBox.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(8);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label word = new Label(item.word());
        word.getStyleClass().add("vocab-word");

        Label hint = new Label("TAP TO REVEAL");
        hint.getStyleClass().add("player-progress-text");

        Label translation = new Label(item.translation());
        translation.getStyleClass().add("vocab-translation");
        translation.setVisible(false);
        translation.setManaged(false);

        textBox.getChildren().addAll(word, hint, translation);

        Button btnSpeak = new Button("🔊");
        btnSpeak.getStyleClass().add("btn-speaker");
        btnSpeak.setOnAction(e -> playWordAudio(item.word()));

        cardBox.getChildren().addAll(textBox, btnSpeak);

        cardBox.setOnMouseClicked(e -> {
            boolean show = !translation.isVisible();
            translation.setVisible(show);
            translation.setManaged(show);
            hint.setText(show ? "REVEALED" : "TAP TO REVEAL");
        });

        return cardBox;
    }
    private File recordAudioForSeconds(int seconds) {
        try {
            File dir = new File("exports/recordings");
            if (!dir.exists()) dir.mkdirs();

            File audioFile = new File(dir, "recording_" + System.currentTimeMillis() + ".wav");

            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                Platform.runLater(() ->
                        showErrorDialog("Micro Error", "Microphone non supporté sur ce PC.")
                );
                return null;
            }

            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            AudioInputStream audioStream = new AudioInputStream(line);

            Thread stopper = new Thread(() -> {
                try {
                    Thread.sleep(seconds * 1000L);
                    line.stop();
                    line.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            stopper.start();

            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);

            return audioFile;

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() ->
                    showErrorDialog("Recording Error", e.getMessage())
            );
            return null;
        }
    }
    // ═══════════════════════════════════════════════════════════════════
    // ═══ QUIZ VIEW ═══
    // ═══════════════════════════════════════════════════════════════════
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
                resultLabel.setText(correct
                        ? "✅ Correct answer."
                        : "❌ Wrong answer. Correct: " + questionItem.translation());
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
            if (options.size() >= 3) break;
            options.add(other);
        }

        List<String> finalOptions = new ArrayList<>(options);
        Collections.shuffle(finalOptions);
        return finalOptions;
    }
    private Node buildRecommendedLessonCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("quiz-card");

        Label title = new Label("⭐ Recommended Next Lesson");
        title.getStyleClass().add("quiz-question");

        Lesson recommended = null;
        String reason = "";

        if (currentLessonIndex + 1 < lessons.size()) {
            recommended = lessons.get(currentLessonIndex + 1);
            reason = "This is the next lesson in your course.";
        } else {
            recommended = lessons.stream()
                    .filter(l -> l.getId() != currentLesson.getId())
                    .max(Comparator.comparingInt(Lesson::getXpReward))
                    .orElse(null);
            reason = "This lesson gives the highest XP reward.";
        }

        if (recommended == null) {
            Label empty = new Label("No recommendation available.");
            empty.getStyleClass().add("player-muted");
            card.getChildren().addAll(title, empty);
            return card;
        }

        Lesson finalRecommended = recommended;

        Label lessonName = new Label(finalRecommended.getTitle());
        lessonName.getStyleClass().add("lesson-title");

        Label xp = new Label("+" + finalRecommended.getXpReward() + " XP");
        xp.getStyleClass().add("xp-badge");

        Label reasonLabel = new Label(reason);
        reasonLabel.getStyleClass().add("player-muted");

        Button open = new Button("Open Recommended Lesson →");
        open.getStyleClass().add("lesson-step-button");
        open.setOnAction(e -> {
            currentLesson = finalRecommended;
            currentLessonIndex = lessons.indexOf(finalRecommended);
            xpRewardLabel.setText(currentLesson.getXpReward() + " XP");
            switchStep(0);
        });

        card.getChildren().addAll(title, lessonName, xp, reasonLabel, open);
        return card;
    }
    // ═══════════════════════════════════════════════════════════════════
    // ═══ DONE VIEW ═══
    // ═══════════════════════════════════════════════════════════════════
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
        allLessons.setOnAction(e -> FrontRouter.goTo("/fxml/user/front-courses.fxml"));

        Button nextLesson = new Button("Next Lesson →");
        nextLesson.getStyleClass().add("lesson-step-button");
        nextLesson.setOnAction(e -> openNextLesson());

        actions.getChildren().addAll(complete, allLessons, nextLesson);
        doneCard.getChildren().addAll(title, subtitle, xp, actions);
        wrapper.getChildren().addAll(doneCard, buildRecommendedLessonCard());
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════════
    // ═══ API METHODS ═══
    // ═══════════════════════════════════════════════════════════════════

    private void downloadLessonPdf() {
        new Thread(() -> {
            try {
                String pdfPath = pdfService.exportLessonToPdf(currentLesson);

                Platform.runLater(() -> {
                    if (pdfPath != null) {
                        showSuccessDialog("✅ PDF Downloaded", "File saved: " + pdfPath);
                    } else {
                        showErrorDialog("❌ PDF Error", "Failed to generate PDF");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showErrorDialog("❌ Error", e.getMessage())
                );
            }
        }).start();
    }
    private void playLessonAudio() {
        new Thread(() -> {
            try {
                List<String> chunks = ttsService.generateLessonAudioChunks(
                        currentLesson.getTitle(),
                        currentLesson.getContent(),
                        "fr"
                );

                if (!chunks.isEmpty()) {
                    playAudioFiles(chunks);
                } else {
                    Platform.runLater(() ->
                            showErrorDialog("❌ Audio Error", "Failed to generate audio"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showErrorDialog("❌ Error", e.getMessage()));
            }
        }).start();
    }
    private void playWordAudio(String word) {
        new Thread(() -> {
            try {
                List<String> chunks = ttsService.textToSpeechChunks(word, "fr");
                if (!chunks.isEmpty()) {
                    playAudioFiles(chunks);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    // ═══ Audio playback ═══

    private void playAudioFile(String filePath) {
        playAudioFiles(List.of(filePath));
    }

    // ═══ AUDIO PLAYBACK CORRIGÉ ═══

    private void playAudioFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            Platform.runLater(() ->
                    showErrorDialog("Audio Error", "Aucun fichier audio trouvé.")
            );
            return;
        }

        stopAudio();

        playerThread = new Thread(() -> {
            isPlaying = true;

            for (String filePath : filePaths) {
                if (!isPlaying) break;

                try {
                    File file = new File(filePath);

                    if (!file.exists()) {
                        System.err.println("Fichier introuvable: " + file.getAbsolutePath());
                        continue;
                    }

                    try (
                            FileInputStream fis = new FileInputStream(file);
                            BufferedInputStream bis = new BufferedInputStream(fis)
                    ) {
                        jlPlayer = new Player(bis);

// lecture non bloquante
                        while (isPlaying && !Thread.currentThread().isInterrupted()) {
                            if (!jlPlayer.play(1)) break; // joue frame par frame
                        }
                    }

                } catch (Exception e) {
                    if (isPlaying) e.printStackTrace();
                }
            }

            isPlaying = false;
        });

        playerThread.start();
    }
    private void stopAudio() {
        isPlaying = false;

        try {
            if (jlPlayer != null) {
                jlPlayer.close();
                jlPlayer = null;
            }

            if (playerThread != null && playerThread.isAlive()) {
                playerThread.interrupt();
                playerThread = null;
            }

            System.out.println("Audio stopped.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void playNext(List<String> files, int index) {
        if (index >= files.size()) {
            System.out.println("✅ Audio terminé");
            return;
        }

        try {
            File file = new File(files.get(index));

            if (!file.exists()) {
                System.err.println("❌ Fichier introuvable: " + file.getAbsolutePath());
                playNext(files, index + 1);
                return;
            }

            System.out.println("🎧 Lecture: " + file.getAbsolutePath());

            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(1.0);

            mediaPlayer.setOnReady(() -> {
                System.out.println("✅ Media prêt");
                mediaPlayer.play();
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                System.out.println("✅ Chunk terminé: " + file.getName());
                mediaPlayer.dispose();
                playNext(files, index + 1);
            });

            mediaPlayer.setOnError(() -> {
                System.err.println("❌ Erreur MediaPlayer: " + mediaPlayer.getError());
                mediaPlayer.dispose();
                playNext(files, index + 1);
            });

        } catch (Exception e) {
            e.printStackTrace();
            playNext(files, index + 1);
        }
    }

    // ═══ Lesson actions ═══

    private void completeCurrentLesson() {
        try {
            int currentUserId = 124;
            progressService.completeLesson(currentUserId, currentLesson.getId(), currentLesson.getXpReward());
            showSuccessDialog("✅ Success",
                    "Lesson completed and " + currentLesson.getXpReward() + " XP added!");
        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorDialog("❌ Error", "Could not complete lesson.");
        }
    }

    private void openNextLesson() {
        if (currentLessonIndex + 1 < lessons.size()) {
            currentLessonIndex++;
            currentLesson = lessons.get(currentLessonIndex);
            xpRewardLabel.setText(currentLesson.getXpReward() + " XP");
            switchStep(0);
        } else {
            showSuccessDialog("🎉 Done", "You finished all lessons in this course!");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ═══ DIALOGS ═══
    // ═══════════════════════════════════════════════════════════════════

    private Dialog<?> currentDialog;

    private void showLoadingDialog(String message) {
        if (Platform.isFxApplicationThread()) {
            createLoadingDialog(message);
        } else {
            Platform.runLater(() -> createLoadingDialog(message));
        }
    }

    private void createLoadingDialog(String message) {
        if (currentDialog != null) {
            currentDialog.close();
            currentDialog = null;
        }

        Dialog<?> dialog = new Dialog<>();
        dialog.setTitle("Loading");
        dialog.setHeaderText(null);

        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        Label label = new Label(message);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setProgress(-1);

        content.getChildren().addAll(label, progressBar);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().clear();

        currentDialog = dialog;
        dialog.show();
    }
    private void dismissLoadingDialog() {
        if (Platform.isFxApplicationThread()) {
            closeLoadingDialog();
        } else {
            Platform.runLater(this::closeLoadingDialog);
        }
    }

    private void closeLoadingDialog() {
        if (currentDialog != null) {
            currentDialog.close();
            currentDialog = null;
        }
    }

    private void showSuccessDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().getStyleClass().add("dialog-success");
            alert.showAndWait();
        });
    }

    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().getStyleClass().add("dialog-error");
            alert.showAndWait();
        });
    }


    // ═══════════════════════════════════════════════════════════════════
    // ═══ HELPERS ═══
    // ═══════════════════════════════════════════════════════════════════

    private List<VocabItem> parseVocabulary(String raw) {
        List<VocabItem> items = new ArrayList<>();
        if (raw == null || raw.isBlank()) return items;

        String normalized = raw.replace("\r", "\n");
        String[] lines = normalized.split("[;\n]+");

        for (String line : lines) {
            String value = line.trim();
            if (value.isBlank()) continue;

            String[] parts;
            if (value.contains("->")) {
                parts = value.split("->", 2);
            } else if (value.contains("=")) {
                parts = value.split("=", 2);
            } else if (value.contains(":")) {
                parts = value.split(":", 2);
            } else if (value.contains("-")) {
                parts = value.split("-", 2);
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

    private record VocabItem(String word, String translation) {}
}