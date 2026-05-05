package org.example.controller.tests;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestAnswer;
import org.example.entity.tests.TestResult;
import org.example.repository.tests.TestAnswerRepository;
import org.example.service.tests.*;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class SpeakingTestController implements Initializable {

    private static final Logger LOG = Logger.getLogger(SpeakingTestController.class.getName());
    private static final int TOTAL_QUESTIONS = 5;

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label      testTitleLabel;
    @FXML private Label      niveauLabel;
    @FXML private Label      langueLabel;
    @FXML private Label      progressLabel;
    @FXML private Label      questionLabel;
    @FXML private Label      statutLabel;
    @FXML private Label      transcriptionLabel;
    @FXML private Button     btnEnregistrer;
    @FXML private Button     btnSuivant;
    @FXML private Button     btnEcouter;
    @FXML private VBox       historiqueBox;
    @FXML private ProgressBar progressBar;
    // Cercles visualiseur
    @FXML private Circle     micCircle;
    @FXML private Circle     wave1;
    @FXML private Circle     wave2;
    @FXML private Circle     wave3;
    @FXML private Circle     recIndicator;
    @FXML private Label      micIcon;

    // ── State ─────────────────────────────────────────────────────────────────
    private MockTestService        mockTestService;
    private TestResultService      resultService;
    private TestAnswerRepository   answerRepo;
    private MockTest               currentTest;
    private User                   currentUser;
    private UserTestListController listController;
    private GroqSpeakingService    groqService;
    private AssemblyAIService      assemblyService;

    private List<String>  questions      = new ArrayList<>();
    private List<String>  reponses       = new ArrayList<>();
    private int           questionIdx    = 0;
    private boolean       enregistrement = false;
    private boolean       transcriptionOk = false;

    // Audio
    private TargetDataLine microLine;
    private Path           tempWavFile;

    // Animations
    private Timeline       pulseAnim;
    private Timeline       waveAnim;
    private Timeline       recBlinkAnim;

    // ── Initialize ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        groqService     = new GroqSpeakingService();
        assemblyService = new AssemblyAIService();
        btnSuivant.setDisable(true);
        btnEcouter.setDisable(true);
        setupMicAnimations();
    }

    public void init(MockTestService mockTestService, MockTest test, User user,
                     UserTestListController listController) {
        this.mockTestService = mockTestService;
        this.currentTest     = test;
        this.currentUser     = user;
        this.listController  = listController;
        this.resultService   = new TestResultService();
        this.answerRepo      = new TestAnswerRepository();

        testTitleLabel.setText(test.getTitle() != null ? test.getTitle() : "Speaking Test");
        niveauLabel.setText(test.getLevel() != null ? test.getLevel() : "B1");
        langueLabel.setText(test.getPlatformLanguage() != null
                ? test.getPlatformLanguage().getName() : "Francais");

        chargerQuestions();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANIMATIONS MICRO
    // ─────────────────────────────────────────────────────────────────────────

    private void setupMicAnimations() {
        // Pulse idle — légère pulsation du cercle central
        pulseAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(micCircle.scaleXProperty(), 1.0),
                        new KeyValue(micCircle.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(1400),
                        new KeyValue(micCircle.scaleXProperty(), 1.06),
                        new KeyValue(micCircle.scaleYProperty(), 1.06)),
                new KeyFrame(Duration.millis(2800),
                        new KeyValue(micCircle.scaleXProperty(), 1.0),
                        new KeyValue(micCircle.scaleYProperty(), 1.0))
        );
        pulseAnim.setCycleCount(Timeline.INDEFINITE);
        pulseAnim.play();

        // Ondes — expansions pendant enregistrement
        waveAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(wave1.opacityProperty(), 0.3),
                        new KeyValue(wave2.opacityProperty(), 0.15),
                        new KeyValue(wave3.opacityProperty(), 0.08),
                        new KeyValue(wave1.scaleXProperty(), 1.0),
                        new KeyValue(wave2.scaleXProperty(), 1.0),
                        new KeyValue(wave3.scaleXProperty(), 1.0)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(wave1.opacityProperty(), 0.0),
                        new KeyValue(wave2.opacityProperty(), 0.3),
                        new KeyValue(wave3.opacityProperty(), 0.15),
                        new KeyValue(wave1.scaleXProperty(), 1.5),
                        new KeyValue(wave2.scaleXProperty(), 1.0),
                        new KeyValue(wave3.scaleXProperty(), 1.0)),
                new KeyFrame(Duration.millis(1800),
                        new KeyValue(wave2.opacityProperty(), 0.0),
                        new KeyValue(wave3.opacityProperty(), 0.3),
                        new KeyValue(wave2.scaleXProperty(), 1.5),
                        new KeyValue(wave3.scaleXProperty(), 1.0)),
                new KeyFrame(Duration.millis(2700),
                        new KeyValue(wave3.opacityProperty(), 0.0),
                        new KeyValue(wave3.scaleXProperty(), 1.5))
        );
        waveAnim.setCycleCount(Timeline.INDEFINITE);

        // Clignotement indicateur REC
        recBlinkAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(recIndicator.fillProperty(), Color.web("#ef4444"))),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(recIndicator.fillProperty(), Color.TRANSPARENT)),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(recIndicator.fillProperty(), Color.web("#ef4444")))
        );
        recBlinkAnim.setCycleCount(Timeline.INDEFINITE);
    }

    private void setModeRecording(boolean recording) {
        if (recording) {
            // Rouge + ondes
            micCircle.setFill(Color.web("#ef4444"));
            micCircle.setStyle("-fx-effect:dropshadow(gaussian,rgba(239,68,68,0.7),24,0,0,0);");
            micIcon.setText("⏹");
            btnEnregistrer.setText("Cliquer pour arreter");
            btnEnregistrer.setStyle("-fx-background-color:transparent;" +
                    "-fx-text-fill:rgba(239,68,68,0.8);-fx-font-size:12px;" +
                    "-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:0;");
            pulseAnim.stop();
            waveAnim.play();
            recBlinkAnim.play();
            setStatut("Enregistrement en cours...", "#ef4444");
        } else {
            // Indigo + idle
            micCircle.setFill(Color.web("#6366f1"));
            micCircle.setStyle("-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.7),24,0,0,0);");
            micIcon.setText("🎙");
            btnEnregistrer.setText("Appuyer pour parler");
            btnEnregistrer.setStyle("-fx-background-color:transparent;" +
                    "-fx-text-fill:rgba(255,255,255,0.45);-fx-font-size:12px;" +
                    "-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:0;");
            waveAnim.stop();
            recBlinkAnim.stop();
            recIndicator.setFill(Color.TRANSPARENT);
            // Reset ondes
            wave1.setOpacity(0.3); wave1.setScaleX(1); wave1.setScaleY(1);
            wave2.setOpacity(0.15); wave2.setScaleX(1); wave2.setScaleY(1);
            wave3.setOpacity(0.08); wave3.setScaleX(1); wave3.setScaleY(1);
            pulseAnim.play();
        }
    }

    private void setModeTranscribing() {
        micCircle.setFill(Color.web("#f59f00"));
        micCircle.setStyle("-fx-effect:dropshadow(gaussian,rgba(245,159,0,0.6),20,0,0,0);");
        micIcon.setText("⏳");
        btnEnregistrer.setText("Transcription...");
        btnEnregistrer.setStyle("-fx-background-color:transparent;" +
                "-fx-text-fill:rgba(245,159,0,0.8);-fx-font-size:12px;" +
                "-fx-cursor:default;-fx-border-color:transparent;-fx-padding:0;");
        setStatut("AssemblyAI transcrit votre reponse...", "#f59f00");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHARGEMENT QUESTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerQuestions() {
        setStatut("Generation des questions par l'IA...", "#6366f1");
        questionLabel.setText("Preparation de votre examen...");
        btnEnregistrer.setDisable(true);

        String niveau = currentTest.getLevel() != null ? currentTest.getLevel() : "B1";
        String langue = currentTest.getPlatformLanguage() != null
                ? currentTest.getPlatformLanguage().getName() : "Francais";

        new Thread(() -> {
            try {
                List<String> qs = groqService.genererQuestions(niveau, langue);
                Platform.runLater(() -> {
                    questions.addAll(qs);
                    afficherQuestion(0);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    questions.addAll(List.of(
                            "Pouvez-vous vous presenter brievement ?",
                            "Parlez-moi de vos loisirs preferes.",
                            "Decrivez votre journee ideale.",
                            "Que pensez-vous de l'apprentissage des langues ?",
                            "Quels sont vos projets pour l'avenir ?"
                    ));
                    afficherQuestion(0);
                    setStatut("Questions par defaut (IA indisponible)", "#f59f00");
                });
            }
        }).start();
    }

    private void afficherQuestion(int idx) {
        questionIdx = idx;
        String q = questions.get(idx);

        questionLabel.setText(q);
        progressLabel.setText(String.format("%02d / %02d", idx + 1, TOTAL_QUESTIONS));
        progressBar.setProgress((double) idx / TOTAL_QUESTIONS);
        transcriptionLabel.setText("");
        transcriptionOk = false;
        btnSuivant.setDisable(true);
        btnEnregistrer.setDisable(false);
        btnEcouter.setDisable(false);

        boolean derniere = (idx == TOTAL_QUESTIONS - 1);
        btnSuivant.setText(derniere ? "Terminer l'examen et voir le bilan" : "Question suivante");

        setModeRecording(false);
        setStatut("Appuyez sur le micro pour repondre", "#94a3b8");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TTS — LECTURE DE LA QUESTION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleEcouter() {
        if (questions.isEmpty() || questionIdx >= questions.size()) return;
        String texte = questions.get(questionIdx);
        // Detecter la langue du test pour choisir la bonne voix TTS
        String langue = currentTest.getPlatformLanguage() != null
                ? currentTest.getPlatformLanguage().getName().toLowerCase() : "francais";

        btnEcouter.setDisable(true);
        btnEcouter.setText("En cours...");

        new Thread(() -> {
            try {
                // Choisir la voix selon la langue
                String voixFiltre;
                if (langue.contains("fran") || langue.contains("french")) {
                    voixFiltre = "French";
                } else if (langue.contains("english") || langue.contains("anglais")) {
                    voixFiltre = "English";
                } else if (langue.contains("espagnol") || langue.contains("spanish")) {
                    voixFiltre = "Spanish";
                } else if (langue.contains("allemand") || langue.contains("german")) {
                    voixFiltre = "German";
                } else {
                    voixFiltre = "French";
                }

                String safeTexte = texte.replace("'", " ").replace("\"", " ");

                // PowerShell : chercher voix de la bonne langue, sinon premiere voix disponible
                String script = String.format(
                        "Add-Type -AssemblyName System.Speech; " +
                                "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                "$voix = $s.GetInstalledVoices() | " +
                                "  Where-Object { $_.VoiceInfo.Culture.Name -like '*%s*' } | " +
                                "  Select-Object -First 1; " +
                                "if ($voix) { $s.SelectVoice($voix.VoiceInfo.Name); }; " +
                                "$s.Rate = -2; " +
                                "$s.Volume = 100; " +
                                "$s.Speak('%s');",
                        voixFiltre, safeTexte);

                ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", script);
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                proc.waitFor();
            } catch (Exception e) {
                LOG.warning("TTS erreur : " + e.getMessage());
            } finally {
                Platform.runLater(() -> {
                    btnEcouter.setDisable(false);
                    btnEcouter.setText("Ecouter la question");
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ENREGISTREMENT AUDIO
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleEnregistrer() {
        if (btnEnregistrer.isDisabled()) return;
        if (!enregistrement) {
            demarrerEnregistrement();
        } else {
            arreterEnregistrement();
        }
    }

    private void demarrerEnregistrement() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                setStatut("Microphone non disponible", "#ef4444");
                return;
            }

            microLine = (TargetDataLine) AudioSystem.getLine(info);
            microLine.open(format);
            microLine.start();

            enregistrement = true;
            setModeRecording(true);
            btnEcouter.setDisable(true);

            tempWavFile = Files.createTempFile("speaking_q" + questionIdx + "_", ".wav");

            new Thread(() -> enregistrerAudio(format)).start();

        } catch (Exception e) {
            LOG.severe("Erreur micro : " + e.getMessage());
            setStatut("Impossible d'acceder au microphone", "#ef4444");
        }
    }

    private void enregistrerAudio(AudioFormat format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            while (enregistrement) {
                int read = microLine.read(buffer, 0, buffer.length);
                if (read > 0) baos.write(buffer, 0, read);
            }
            byte[] audioData = baos.toByteArray();
            if (audioData.length > 0) {
                AudioInputStream wavStream = new AudioInputStream(
                        new ByteArrayInputStream(audioData), format,
                        audioData.length / format.getFrameSize());
                AudioSystem.write(wavStream, AudioFileFormat.Type.WAVE, tempWavFile.toFile());
                LOG.info("Audio: " + audioData.length + " bytes -> " + tempWavFile);
            }
        } catch (Exception e) {
            LOG.severe("Erreur enregistrement : " + e.getMessage());
        }
    }

    private void arreterEnregistrement() {
        enregistrement = false;
        if (microLine != null) {
            microLine.stop();
            microLine.close();
        }
        btnEnregistrer.setDisable(true);
        setModeTranscribing();

        String langue = currentTest.getPlatformLanguage() != null
                ? currentTest.getPlatformLanguage().getName() : "Francais";

        new Thread(() -> {
            try {
                // Verifier que le fichier existe et a du contenu
                if (tempWavFile == null || !Files.exists(tempWavFile)
                        || Files.size(tempWavFile) < 1000) {
                    throw new Exception("Fichier audio trop court ou vide");
                }

                String texte = assemblyService.transcrire(tempWavFile, langue);
                Files.deleteIfExists(tempWavFile);

                Platform.runLater(() -> {
                    transcriptionLabel.setText(texte);
                    reponses.add(texte);
                    ajouterAuHistorique(questionIdx, texte);
                    transcriptionOk = true;
                    btnSuivant.setDisable(false);
                    btnEnregistrer.setDisable(false);
                    btnEcouter.setDisable(false);
                    setModeRecording(false);
                    setStatut("Transcription reussie !", "#10b981");
                });
            } catch (Exception e) {
                LOG.severe("Erreur transcription : " + e.getMessage());
                Platform.runLater(() -> {
                    String reponseFallback = "[Transcription indisponible - Q" + (questionIdx+1) + "]";
                    reponses.add(reponseFallback);
                    transcriptionLabel.setText("Transcription indisponible. Vous pouvez continuer.");
                    ajouterAuHistorique(questionIdx, reponseFallback);
                    transcriptionOk = false;
                    // RESET COMPLET du bouton — bug corrige
                    btnEnregistrer.setDisable(false);
                    btnEcouter.setDisable(false);
                    btnSuivant.setDisable(false);
                    setModeRecording(false);
                    setStatut("Erreur transcription - vous pouvez continuer", "#f59f00");
                });
            }
        }).start();
    }

    private void ajouterAuHistorique(int idx, String reponse) {
        VBox item = new VBox(8);
        item.setStyle("-fx-background-color:rgba(255,255,255,0.04);" +
                "-fx-background-radius:12;" +
                "-fx-border-color:rgba(255,255,255,0.07);" +
                "-fx-border-radius:12;-fx-border-width:1;-fx-padding:14 16;");

        Label labelQ = new Label("Q" + (idx+1) + "  " + questions.get(idx));
        labelQ.setWrapText(true);
        labelQ.setStyle("-fx-text-fill:rgba(255,255,255,0.6);-fx-font-size:11px;" +
                "-fx-font-weight:bold;");

        Label labelSep = new Label();
        labelSep.setStyle("-fx-background-color:rgba(99,102,241,0.3);" +
                "-fx-min-height:1;-fx-max-height:1;");

        Label labelR = new Label(reponse);
        labelR.setWrapText(true);
        labelR.setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-font-size:12px;" +
                "-fx-font-style:italic;-fx-padding:4 0 0 0;");

        item.getChildren().addAll(labelQ, labelSep, labelR);
        historiqueBox.getChildren().add(item);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleSuivant() {
        // Si pas de reponse enregistree pour cette question, ajouter placeholder
        if (reponses.size() <= questionIdx) {
            reponses.add("[Pas de reponse - Q" + (questionIdx+1) + "]");
        }

        int nextIdx = questionIdx + 1;
        if (nextIdx < TOTAL_QUESTIONS && nextIdx < questions.size()) {
            afficherQuestion(nextIdx);
        } else {
            evaluerEtTerminer();
        }
    }

    private void evaluerEtTerminer() {
        btnSuivant.setDisable(true);
        btnEnregistrer.setDisable(true);
        btnEcouter.setDisable(true);
        questionLabel.setText("Evaluation de votre performance par l'IA...");
        setStatut("Groq analyse votre conversation...", "#6366f1");
        progressBar.setProgress(1.0);
        progressLabel.setText("Evaluation");
        pulseAnim.stop();
        waveAnim.stop();

        String niveau = currentTest.getLevel() != null ? currentTest.getLevel() : "B1";
        String langue = currentTest.getPlatformLanguage() != null
                ? currentTest.getPlatformLanguage().getName() : "Francais";

        new Thread(() -> {
            try {
                // Completer les reponses manquantes
                List<String> rep = new ArrayList<>(reponses);
                while (rep.size() < questions.size())
                    rep.add("[Pas de reponse]");

                GroqSpeakingService.SpeakingFeedback fb =
                        groqService.evaluerConversation(questions, rep, niveau, langue);

                sauvegarderResultat(fb, rep);
                Platform.runLater(() -> naviguerVersResultat(fb));

            } catch (Exception e) {
                LOG.severe("Erreur evaluation : " + e.getMessage());
                GroqSpeakingService.SpeakingFeedback fallback =
                        new GroqSpeakingService.SpeakingFeedback(
                                10, 50f, niveau,
                                "Evaluation IA indisponible.",
                                "Evaluation IA indisponible.",
                                "Evaluation IA indisponible.",
                                "Evaluation IA indisponible.",
                                "Evaluation IA indisponible.",
                                "Continuez a pratiquer l'oral.",
                                "Note automatique : 10/20."
                        );
                sauvegarderResultat(fallback, reponses);
                Platform.runLater(() -> naviguerVersResultat(fallback));
            }
        }).start();
    }

    private void sauvegarderResultat(GroqSpeakingService.SpeakingFeedback fb,
                                     List<String> rep) {
        try {
            TestResult result = new TestResult();
            result.setUser(currentUser);
            result.setMockTest(currentTest);
            result.setOverallScore(fb.scorePct());
            result.setAiPredictedScore(fb.scorePct());
            result.setDateTaken(LocalDateTime.now());
            result.setAiNote("Speaking IA - Note : " + fb.noteSur20()
                    + "/20 - CEFR : " + fb.niveauCEFR());
            result.setAiCorrection(fb.bilanGlobal());
            resultService.create(result);

            if (result.getId() != null) {
                List<TestAnswer> answers = new ArrayList<>();
                for (int i = 0; i < questions.size(); i++) {
                    TestAnswer ta = new TestAnswer();
                    ta.setTestResultId(result.getId());
                    ta.setSectionCategory("Speaking");
                    ta.setUserAnswer(i < rep.size() ? rep.get(i) : "");
                    ta.setCorrect(fb.noteSur20() >= 10);
                    ta.setPointsObtained(Math.round(fb.noteSur20() / 20f));
                    ta.setPointsMax(1);
                    answers.add(ta);
                }
                answerRepo.saveAll(answers);
            }
        } catch (Exception e) {
            LOG.severe("Erreur sauvegarde : " + e.getMessage());
        }
    }

    private void naviguerVersResultat(GroqSpeakingService.SpeakingFeedback fb) {
        try {
            Stage stage = (Stage) testTitleLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/SpeakingResultView.fxml"));
            Parent root = loader.load();
            SpeakingResultController ctrl = loader.getController();
            ctrl.init(mockTestService, currentTest, currentUser, listController, fb);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("LinguaLearn - Resultat Speaking");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur navigation : " + e.getMessage()).showAndWait();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void setStatut(String msg, String color) {
        if (statutLabel != null) {
            statutLabel.setText(msg);
            statutLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;");
        }
    }

    @FXML
    private void handleBack() {
        if (enregistrement) {
            enregistrement = false;
            if (microLine != null) { microLine.stop(); microLine.close(); }
        }
        pulseAnim.stop();
        waveAnim.stop();
        recBlinkAnim.stop();
        try {
            Stage stage = (Stage) testTitleLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestListView.fxml"));
            Parent root = loader.load();
            UserTestListController ctrl = loader.getController();
            ctrl.init(mockTestService, currentUser,
                    listController.getDashboardController(), stage);
            ctrl.refreshData();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }
}