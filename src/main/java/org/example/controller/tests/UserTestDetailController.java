package org.example.controller.tests;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestAnswer;
import org.example.entity.tests.TestQuestion;
import org.example.entity.tests.TestResult;
import org.example.repository.tests.TestAnswerRepository;
import org.example.service.tests.AntiCheatGuard;
import org.example.service.tests.ExamTimeGuardService;
import org.example.service.tests.MockTestService;
import org.example.service.tests.TestResultService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class UserTestDetailController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label      testTitleLabel;
    @FXML private Label      testTypeLabel;
    @FXML private Label      testLevelLabel;
    @FXML private Label      testDurationLabel;
    @FXML private Label      testLanguageLabel;
    @FXML private Label      testQuestionsCountLabel;
    @FXML private Label      totalPointsLabel;
    @FXML private Label      chronoLabel;
    @FXML private VBox       chronoBox;
    @FXML private VBox       questionsContainer;
    @FXML private ScrollPane questionsScroll;
    @FXML private VBox       resultBox;
    @FXML private Label      scoreLabel;
    @FXML private Label      scorePctLabel;
    @FXML private Label      scoreDetailLabel;
    @FXML private Label      penaliteLabel;
    @FXML private Label      antiCheatLabel;   // affiche le compteur d'avertissements
    @FXML private Button     submitBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private MockTestService        mockTestService;
    private TestResultService      resultService;
    private TestAnswerRepository   answerRepo;
    private ExamTimeGuardService   timeGuard;
    private AntiCheatGuard         antiCheat;        // ← NOUVEAU
    private MockTest               currentTest;
    private User                   currentUser;
    private UserTestListController listController;
    private List<TestQuestion>     questions;
    private LocalDateTime          startTime;
    private Timeline               chronoTimeline;
    private long                   secondesTotales;
    private boolean                testInProgress  = false;
    private String                 noteTriche      = null; // rempli si triche détectée

    private final List<ToggleGroup>      toggleGroups = new ArrayList<>();
    private final List<TextInputControl> openAnswers  = new ArrayList<>();

    // ── Initialize ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        if (penaliteLabel  != null) { penaliteLabel.setVisible(false);  penaliteLabel.setManaged(false); }
        if (scorePctLabel  != null) { scorePctLabel.setVisible(false);  scorePctLabel.setManaged(false); }
        if (antiCheatLabel != null) { antiCheatLabel.setVisible(false); antiCheatLabel.setManaged(false); }
    }

    public void init(MockTestService service, MockTest test, User user,
                     UserTestListController listController) {
        this.mockTestService = service;
        this.resultService   = new TestResultService();
        this.answerRepo      = new TestAnswerRepository();
        this.timeGuard       = new ExamTimeGuardService();
        this.currentTest     = test;
        this.currentUser     = user;
        this.listController  = listController;
        this.startTime       = LocalDateTime.now();
        this.secondesTotales = (long) test.getDurationMinutes() * 60;

        testTitleLabel.setText(test.getTitle());
        testTypeLabel.setText(test.getTestType());
        testLevelLabel.setText(test.getLevel());
        testDurationLabel.setText(test.getDurationMinutes() + " min");
        testLanguageLabel.setText(test.getPlatformLanguage() != null
                ? test.getPlatformLanguage().getName() : "—");

        // Mélange aléatoire
        List<TestQuestion> raw = new ArrayList<>(service.findQuestionsByTest(test.getId()));
        Collections.shuffle(raw);
        this.questions = raw;

        testQuestionsCountLabel.setText(questions.size() + " question(s)");
        totalPointsLabel.setText("Sur 20 pts");

        testInProgress = true;
        buildQuestionsUI();
        startChrono();

        // ── Démarrer la surveillance anti-triche ─────────────────────────────
        Platform.runLater(() -> {
            Stage stage = (Stage) testTitleLabel.getScene().getWindow();
            startAntiCheat(stage);
        });
    }

    // ── Anti-Cheat ────────────────────────────────────────────────────────────

    private void startAntiCheat(Stage stage) {
        antiCheat = new AntiCheatGuard(stage, this::onForceSubmit)
                .setAvertissementCallback(this::onAvertissement)
                .setContext(
                        currentUser.getId(),
                        currentTest.getId(),
                        currentUser.getFullName(),
                        currentTest.getTitle()
                );
        antiCheat.start();
    }

    /**
     * Callback : avertissement affiché → mettre à jour le label dans l'UI.
     */
    private void onAvertissement(int nbSorties, int maxSorties) {
        Platform.runLater(() -> {
            if (antiCheatLabel == null) return;
            antiCheatLabel.setVisible(true);
            antiCheatLabel.setManaged(true);
            String color = nbSorties >= maxSorties ? "#d63939" : "#f59f00";
            antiCheatLabel.setText("⚠️ Avertissement " + nbSorties + "/" + maxSorties
                    + " — Ne quittez pas la fenêtre !");
            antiCheatLabel.setStyle(
                    "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-background-color:" + color + ";-fx-background-radius:6;" +
                            "-fx-padding:5 14;");
        });
    }

    /**
     * Callback : soumission forcée par l'anti-triche.
     * Appelé depuis AntiCheatGuard après 3 sorties ou fermeture fenêtre.
     */
    private void onForceSubmit(int nbSorties, String note) {
        Platform.runLater(() -> {
            if (!testInProgress) return;
            noteTriche     = note;
            testInProgress = false;
            stopChrono();
            // Appliquer pénalité -50% forcée (isAntiCheat = true)
            doSubmit(true);
            // Naviguer vers la liste après un court délai
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                Platform.runLater(this::navigateBackToList);
            }).start();
        });
    }

    private void stopAntiCheat() {
        if (antiCheat != null) antiCheat.stop();
    }

    // ── Chronomètre ───────────────────────────────────────────────────────────

    private void startChrono() {
        if (chronoLabel == null) return;
        updateChronoDisplay(secondesTotales);
        chronoTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long ecoulees  = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
            long restantes = secondesTotales - ecoulees;
            Platform.runLater(() -> updateChronoDisplay(restantes));
            if (ExamTimeGuardService.doitSoumettreAuto(ecoulees, currentTest.getDurationMinutes())) {
                chronoTimeline.stop();
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.WARNING,
                            "Temps dépassé ! Soumission automatique avec pénalité -50%.",
                            ButtonType.OK).showAndWait();
                    testInProgress = false;
                    doSubmit(true);
                });
            }
        }));
        chronoTimeline.setCycleCount(Timeline.INDEFINITE);
        chronoTimeline.play();
    }

    private void updateChronoDisplay(long secondesRestantes) {
        if (chronoLabel == null) return;
        String texte, couleur;
        if (secondesRestantes >= 0) {
            texte   = ExamTimeGuardService.formatChrono(secondesRestantes);
            couleur = ExamTimeGuardService.getCouleurChrono(secondesRestantes, secondesTotales);
        } else {
            texte   = "+" + ExamTimeGuardService.formatChrono(-secondesRestantes);
            couleur = "#7b1fa2";
        }
        chronoLabel.setText(texte);
        chronoLabel.setStyle("-fx-font-size:22px;-fx-font-weight:bold;" +
                "-fx-font-family:monospace;-fx-text-fill:" + couleur + ";");
        if (chronoBox != null) {
            String bg = secondesRestantes < 0 ? "rgba(198,40,40,0.20)"
                    : secondesRestantes < secondesTotales * 0.20 ? "rgba(198,40,40,0.12)"
                    : "rgba(0,0,0,0.25)";
            chronoBox.setStyle("-fx-background-color:" + bg + ";" +
                    "-fx-background-radius:10;-fx-padding:6 16;-fx-min-width:120;");
        }
    }

    private void stopChrono() { if (chronoTimeline != null) chronoTimeline.stop(); }

    // ── Construction des questions ────────────────────────────────────────────

    private void buildQuestionsUI() {
        questionsContainer.getChildren().clear();
        toggleGroups.clear();
        openAnswers.clear();
        if (questions.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label msg = new Label("Ce test n'a pas encore de questions.");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#6c7a99;");
            empty.getChildren().add(msg);
            questionsContainer.getChildren().add(empty);
            submitBtn.setDisable(true);
            return;
        }
        int num = 1;
        for (TestQuestion q : questions) questionsContainer.getChildren().add(buildQuestionCard(q, num++));
    }

    private VBox buildQuestionCard(TestQuestion q, int num) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
                "-fx-border-color:#e3e8f0;-fx-border-radius:10;-fx-border-width:1;" +
                "-fx-padding:20;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");
        String section = q.getSectionCategory() != null ? q.getSectionCategory() : "Reading";
        String sectionColor = switch (section) {
            case "Listening" -> "#e65100";
            case "Writing"   -> "#2e7d32";
            case "Speaking"  -> "#c2185b";
            default          -> "#3b5bdb";
        };
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label numLbl = new Label("Q" + num);
        numLbl.setStyle("-fx-background-color:" + sectionColor + ";-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-font-size:12px;" +
                "-fx-background-radius:20;-fx-padding:4 10;");
        Label secLbl = new Label(section);
        secLbl.setStyle("-fx-background-color:#f0f4ff;-fx-text-fill:" + sectionColor + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 10;" +
                "-fx-border-color:" + sectionColor + ";-fx-border-radius:20;-fx-border-width:1;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label ptsLbl = new Label(q.getPoints() + " pt" + (q.getPoints() > 1 ? "s" : ""));
        ptsLbl.setStyle("-fx-text-fill:#6c7a99;-fx-font-size:12px;" +
                "-fx-background-color:#f8fafc;-fx-background-radius:20;-fx-padding:3 10;");
        header.getChildren().addAll(numLbl, secLbl, sp, ptsLbl);

        String qTextStr = (q.getQuestionText() != null && !q.getQuestionText().isBlank())
                ? q.getQuestionText()
                : switch (section) {
            case "Listening" -> "🎧 Exercice d'écoute — répondez à la question ci-dessous.";
            case "Writing"   -> "✍️ Exercice d'expression écrite — rédigez votre réponse.";
            case "Speaking"  -> "🎤 Exercice d'expression orale — décrivez le sujet proposé.";
            default          -> "—";
        };
        Label qLabel = new Label(qTextStr);
        qLabel.setWrapText(true);
        qLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#1a1f36;-fx-font-weight:bold;-fx-padding:4 0;");
        card.getChildren().addAll(header, qLabel);

        if ("Reading".equals(section) || "Listening".equals(section)) buildQCM(card, q);
        else buildOpenAnswer(card, q, sectionColor);
        return card;
    }

    private void buildQCM(VBox card, TestQuestion q) {
        List<String> opts = parseOptions(q.getOptions());
        if (opts.isEmpty()) { toggleGroups.add(null); buildOpenAnswer(card, q, "#3b5bdb"); return; }
        ToggleGroup tg = new ToggleGroup();
        toggleGroups.add(tg);
        openAnswers.add(null);
        VBox optBox = new VBox(8);
        optBox.setPadding(new Insets(8, 0, 0, 16));
        for (String opt : opts) {
            RadioButton rb = new RadioButton(opt);
            rb.setToggleGroup(tg);
            rb.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-cursor:hand;");
            optBox.getChildren().add(rb);
        }
        card.getChildren().add(optBox);
    }

    private void buildOpenAnswer(VBox card, TestQuestion q, String color) {
        toggleGroups.add(null);
        VBox wrapper = new VBox(6);
        Label hint = new Label("Writing".equals(q.getSectionCategory())
                ? "Rédigez votre réponse (quelques phrases minimum) :"
                : "Décrivez votre réponse oralement ou par écrit :");
        hint.setStyle("-fx-font-size:11px;-fx-text-fill:#6c7a99;-fx-font-style:italic;");
        TextArea ta = new TextArea();
        ta.setPromptText("Writing".equals(q.getSectionCategory())
                ? "Votre réponse ici..."
                : "Décrivez votre réponse...");
        ta.setWrapText(true);
        ta.setPrefHeight("Writing".equals(q.getSectionCategory()) ? 120 : 90);
        ta.setStyle("-fx-font-size:13px;-fx-border-color:#d1d5db;-fx-border-radius:6;");

        // ── Bloquer copier / coller / couper ─────────────────────────────────
        bloquerCopierColler(ta);

        openAnswers.add(ta);
        Label noPaste = new Label("🚫 Copier-coller désactivé dans cet exercice");
        noPaste.setStyle("-fx-font-size:10px;-fx-text-fill:#d63939;-fx-font-style:italic;-fx-opacity:0.8;");
        Label auto = new Label("ℹ️ Toute réponse non vide sera automatiquement créditée");
        auto.setStyle("-fx-font-size:10px;-fx-text-fill:" + color + ";-fx-opacity:0.75;");
        wrapper.getChildren().addAll(hint, ta, noPaste, auto);
        card.getChildren().add(wrapper);
    }

    /**
     * Bloque les événements clavier Ctrl+C / Ctrl+V / Ctrl+X / Ctrl+A
     * et le menu contextuel (clic droit) sur n'importe quel TextInputControl.
     */
    private void bloquerCopierColler(TextInputControl field) {
        // Bloquer raccourcis clavier
        field.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() || event.isMetaDown()) {
                switch (event.getCode()) {
                    case C, V, X, A -> {
                        event.consume(); // bloquer Ctrl+C, Ctrl+V, Ctrl+X, Ctrl+A
                    }
                    default -> {}
                }
            }
        });

        // Bloquer le menu contextuel (clic droit → copier/coller)
        field.setContextMenu(new javafx.scene.control.ContextMenu()); // menu vide
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

    // ── Soumission normale ────────────────────────────────────────────────────

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (questions.isEmpty()) return;
        testInProgress = false;
        stopChrono();
        stopAntiCheat();  // ← désactiver la surveillance après soumission normale
        doSubmit(false);
    }

    // ── Calcul + Affichage résultat ───────────────────────────────────────────

    private void doSubmit(boolean isAutoOrCheat) {
        stopChrono();
        testInProgress = false;

        // Calcul score
        int totalPoints = 0, obtainedPoints = 0;
        List<TestAnswer> answers = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            TestQuestion q    = questions.get(i);
            String section    = q.getSectionCategory() != null ? q.getSectionCategory() : "Reading";
            String userAnswer = getUserAnswer(i, q);
            int    ptsMax     = q.getPoints();
            int    ptsObt     = 0;
            boolean correct   = false;
            totalPoints      += ptsMax;

            if ("Reading".equals(section) || "Listening".equals(section)) {
                String ca = q.getCorrectAnswer();
                if (userAnswer != null && ca != null && !userAnswer.trim().isEmpty()) {
                    for (String c : ca.split(";")) {
                        if (userAnswer.trim().equalsIgnoreCase(c.trim())) {
                            ptsObt = ptsMax; correct = true; break;
                        }
                    }
                }
            } else {
                if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                    ptsObt = ptsMax; correct = true;
                }
            }
            obtainedPoints += ptsObt;
            if (q.getId() != null) {
                TestAnswer ta = new TestAnswer();
                ta.setQuestionId(q.getId());
                ta.setSectionCategory(section);
                ta.setUserAnswer(userAnswer != null ? userAnswer : "");
                ta.setCorrect(correct);
                ta.setPointsObtained(ptsObt);
                ta.setPointsMax(ptsMax);
                answers.add(ta);
            }
        }

        float scoreBrut = totalPoints > 0 ? (float) obtainedPoints / totalPoints * 100f : 0f;

        // Si soumission forcée par anti-triche → pénalité -50% directe
        float scoreFinal;
        String noteFinale;

        if (noteTriche != null) {
            // Triche détectée : pénalité -50% sur le score brut
            scoreFinal = scoreBrut * 0.50f;
            noteFinale = noteTriche + "\nScore brut avant pénalité : " + Math.round(scoreBrut) + "%";
        } else {
            // Flux normal : pénalité temporelle via ExamTimeGuardService
            ExamTimeGuardService.RapportTemporel rapport =
                    timeGuard.analyser(startTime, currentTest.getDurationMinutes(), scoreBrut);
            scoreFinal = rapport.scoreFinal;
            noteFinale = rapport.toRapportTexte();
        }

        saveTestResult(scoreFinal, noteFinale, answers);

        // ── Affichage ─────────────────────────────────────────────────────────
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        submitBtn.setDisable(true);

        String color = scoreFinal >= 75 ? "#2fb344" : scoreFinal >= 50 ? "#f59f00" : "#d63939";
        float  sur20  = Math.round(scoreFinal / 100f * 20f * 10f) / 10f;

        scoreLabel.setText(String.format("%.1f / 20", sur20));
        scoreLabel.setStyle("-fx-font-size:34px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");

        if (scorePctLabel != null) {
            scorePctLabel.setText(String.format("= %.0f%%", scoreFinal));
            scorePctLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;" +
                    "-fx-text-fill:" + color + ";-fx-opacity:0.85;");
            scorePctLabel.setVisible(true);
            scorePctLabel.setManaged(true);
        }

        String perf = noteTriche != null
                ? "⛔ Soumission anti-triche — pénalité -50%"
                : getNiveauPerformance(scoreFinal);
        scoreDetailLabel.setText(perf);
        scoreDetailLabel.setStyle("-fx-text-fill:" +
                (noteTriche != null ? "#d63939" : "#a0aec0") + ";-fx-font-size:13px;");

        // Pénalité triche dans le label
        if (penaliteLabel != null && noteTriche != null) {
            penaliteLabel.setText("⛔ Comportement suspect détecté — Pénalité -50% appliquée");
            penaliteLabel.setStyle("-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                    "-fx-background-color:#d63939;-fx-background-radius:6;-fx-padding:6 14;");
            penaliteLabel.setVisible(true);
            penaliteLabel.setManaged(true);
        }

        if (chronoLabel != null) {
            chronoLabel.setText("Terminé ✓");
            chronoLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;" +
                    "-fx-text-fill:#6c7a99;-fx-font-family:monospace;");
        }
        Platform.runLater(() -> questionsScroll.setVvalue(0.0));
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    private void saveTestResult(float score, String note, List<TestAnswer> answers) {
        try {
            if (currentUser == null || currentUser.getId() == null) return;
            if (currentTest == null || currentTest.getId() == null) return;
            TestResult result = new TestResult();
            result.setUser(currentUser);
            result.setMockTest(currentTest);
            result.setOverallScore(score);
            result.setAiPredictedScore(score);
            result.setDateTaken(LocalDateTime.now());
            result.setAiNote(note);
            resultService.create(result);
            if (result.getId() != null && !answers.isEmpty()) {
                answers.forEach(a -> a.setTestResultId(result.getId()));
                answerRepo.saveAll(answers);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR,
                            "Erreur sauvegarde : " + e.getMessage()).showAndWait());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getUserAnswer(int index, TestQuestion q) {
        if (index < toggleGroups.size() && toggleGroups.get(index) != null) {
            Toggle sel = toggleGroups.get(index).getSelectedToggle();
            if (sel instanceof RadioButton rb) return rb.getText();
        }
        if (index < openAnswers.size() && openAnswers.get(index) != null)
            return openAnswers.get(index).getText();
        return null;
    }

    private String getNiveauPerformance(float score) {
        if (score >= 90) return "🏆 Excellent !";
        if (score >= 75) return "🥇 Très bien !";
        if (score >= 60) return "✅ Bien";
        if (score >= 50) return "🔔 Passable";
        return "📚 À retravailler";
    }

    // ── Retour avec confirmation ──────────────────────────────────────────────

    @FXML
    private void handleBack(ActionEvent event) {
        if (testInProgress) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Quitter le test");
            alert.setHeaderText("Voulez-vous vraiment quitter ?");
            alert.setContentText("Vos réponses actuelles seront enregistrées.");
            ButtonType btnSave   = new ButtonType("💾 Quitter et enregistrer",
                    ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel = new ButtonType("↩ Annuler",
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnSave, btnCancel);
            alert.showAndWait().ifPresent(choice -> {
                if (choice == btnSave) {
                    testInProgress = false;
                    stopChrono();
                    stopAntiCheat();
                    doSubmit(false);
                    navigateBackToList();
                }
            });
        } else {
            navigateBackToList();
        }
    }

    private void navigateBackToList() {
        stopChrono();
        stopAntiCheat();
        try {
            Stage stage = (Stage) testTitleLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestListView.fxml"));
            Parent root = loader.load();
            UserTestListController ctrl = loader.getController();
            if (listController.getFilterLanguage() != null
                    && listController.getFilterLevels() != null) {
                ctrl.initWithFilter(mockTestService, currentUser,
                        listController.getDashboardController(), stage,
                        listController.getFilterLanguage(), listController.getFilterLevels(),
                        listController.getFilterLevelName());
            } else {
                ctrl.init(mockTestService, currentUser,
                        listController.getDashboardController(), stage);
            }
            ctrl.refreshData();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("LinguaLearn — Tests de Certification");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur retour : " + e.getMessage()).showAndWait();
        }
    }
}