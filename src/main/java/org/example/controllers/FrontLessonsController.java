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

import java.sql.SQLException;
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
        backToCoursesLabel.setOnMouseClicked(e -> FrontRouter.goTo("/fxml/modules/frontoffice/front-courses.fxml"));

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

    // ── HTTP server for speech recognition ───────────────────────────────────
    private static com.sun.net.httpserver.HttpServer micServer = null;
    private static int micPort = 0;

    private Node buildReadView() {
        VBox wrapper = new VBox(24);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(760);
        wrapper.setPadding(new javafx.geometry.Insets(0, 0, 60, 0));

        Label lessonIndex = new Label("LESSON " + (currentLessonIndex + 1) + " OF " + lessons.size());
        lessonIndex.getStyleClass().add("player-progress-text");

        VBox card = new VBox(20);
        card.getStyleClass().add("read-card");

        Label title = new Label(currentLesson.getTitle());
        title.getStyleClass().add("lesson-title");

        // The user explicitly requested to NOT see the lesson text here.
        // We only keep the title and the practice modules.

        // ── Top Buttons (PDF, Start, Stop) ───────────────────────────────────
        Button btnPdf = new Button("📄 Download PDF");
        btnPdf.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");
        btnPdf.setOnAction(e -> exportPdf());

        Button btnStart = new Button("▶ Start");
        btnStart.setStyle("-fx-background-color:#16a34a; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");
        btnStart.setOnAction(e -> speakText(currentLesson.getContent()));

        Button btnStop = new Button("⏹ Stop");
        btnStop.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");
        btnStop.setOnAction(e -> stopSpeaking());

        HBox topButtons = new HBox(12, btnPdf, btnStart, btnStop);
        topButtons.setAlignment(Pos.CENTER_LEFT);

        // ── Pronunciation Practice ───────────────────────────────────────────
        VBox practiceBox = new VBox(12);
        practiceBox.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0; -fx-border-width:1; -fx-border-radius:12; -fx-background-radius:12; -fx-padding:20;");

        Label practiceTitle = new Label("🎤 Pronunciation Practice");
        practiceTitle.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");

        TextField wordInput = new TextField();
        wordInput.setPromptText("Écris un mot à pratiquer, exemple : Bonjour");
        wordInput.setStyle("-fx-font-size:14px; -fx-padding:10; -fx-background-radius:6; -fx-border-color:#cbd5e1; -fx-border-width:1; -fx-border-radius:6;");

        Button btnEcouter = new Button("🔊 Écouter");
        btnEcouter.setStyle("-fx-background-color:#16a34a; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 20; -fx-cursor:hand;");
        btnEcouter.setOnAction(e -> {
            if (!wordInput.getText().trim().isEmpty()) {
                speakText(wordInput.getText().trim());
            }
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
            if (targetWord.isEmpty()) {
                lblMotReconnu.setText("Veuillez écrire un mot d'abord.");
                return;
            }
            lblMotReconnu.setText("Mot reconnu : Écoute en cours...");
            lblScore.setText("Score : -");
            lblFeedback.setText("Feedback : -");
            openSpeechBrowser(targetWord, lblMotReconnu, lblScore, lblFeedback);
        });

        practiceBox.getChildren().addAll(practiceTitle, wordInput, practiceButtons, lblMotReconnu, lblScore, lblFeedback);

        // ── Next Button ──────────────────────────────────────────────────────
        Button next = new Button("Next: Vocabulary →");
        next.getStyleClass().add("lesson-step-button");
        next.setOnAction(e -> switchStep(1));
        HBox bottomBar = new HBox(next);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new javafx.geometry.Insets(10, 0, 20, 0));

        card.getChildren().addAll(title, topButtons, practiceBox, bottomBar);
        wrapper.getChildren().addAll(lessonIndex, card);
        return wrapper;
    }

    private Process currentTtsProcess = null;

    private void speakText(String text) {
        stopSpeaking();
        new Thread(() -> {
            try {
                String psCommand = "Add-Type -AssemblyName System.Speech; " +
                        "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$synth.Speak('" + text.replace("'", "''") + "');";
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", psCommand);
                currentTtsProcess = pb.start();
                currentTtsProcess.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void stopSpeaking() {
        if (currentTtsProcess != null && currentTtsProcess.isAlive()) {
            currentTtsProcess.destroyForcibly();
            try {
                new ProcessBuilder("taskkill", "/F", "/IM", "powershell.exe").start();
            } catch (Exception ignored) {}
        }
    }

    private volatile boolean forceStop = false;

    private void openSpeechBrowser(String targetWord, Label lblMotReconnu, Label lblScore, Label lblFeedback) {
        try {
            if (micServer == null) {
                micServer = com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress(0), 0);
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

            // Browser POSTs recognised text here
            micServer.createContext("/result", exchange -> {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String spoken = new String(exchange.getRequestBody().readAllBytes(),
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
                        
                        // Reset global labels back to default style for next times
                        lblMotReconnu.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#334155;");
                        lblScore.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#334155;");
                    });
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.getResponseBody().close();
                }
            });

            // Polling endpoint for the browser to know if user clicked "Arrêter"
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
            + "</style></head>"
            + "<body>"
            + "<h2>🎤 Pratique de prononciation</h2>"
            + "<p class='sub'>Parlez maintenant — l'écoute démarre automatiquement</p>"
            + "<button id='micBtn' onclick='toggle()'>🎤</button>"
            + "<div id='status'>En attente du microphone…</div>"
            + "<div id='transcript'>Le texte reconnu apparaîtra ici…</div>"
            + "<div id='sendStatus'></div>"
            + "<script>"
            + "var rec,active=false,full='';"
            + "function start(){"
            + "  var R=window.SpeechRecognition||window.webkitSpeechRecognition;"
            + "  if(!R){document.getElementById('status').innerText='❌ Utilisez Chrome ou Edge';return;}"
            + "  rec=new R();"
            + "  rec.lang='fr-FR';"
            + "  rec.continuous=true;"
            + "  rec.interimResults=true;"
            + "  rec.onstart=function(){"
            + "    active=true;full='';"
            + "    document.getElementById('micBtn').className='active';"
            + "    document.getElementById('micBtn').innerText='⏹';"
            + "    document.getElementById('status').innerText='🔴 Écoute en cours…';"
            + "  };"
            + "  rec.onresult=function(e){"
            + "    var interim='',final_='';"
            + "    for(var i=0;i<e.results.length;i++){"
            + "      if(e.results[i].isFinal)final_+=e.results[i][0].transcript;"
            + "      else interim+=e.results[i][0].transcript;"
            + "    }"
            + "    full=final_;"
            + "    document.getElementById('transcript').innerText=(final_+' '+interim).trim();"
            + "  };"
            + "  rec.onerror=function(e){"
            + "    document.getElementById('status').innerText='Erreur: '+e.error;"
            + "    active=false;"
            + "    document.getElementById('micBtn').className='';"
            + "    document.getElementById('micBtn').innerText='🎤';"
            + "  };"
            + "  rec.onend=function(){"
            + "    active=false;"
            + "    document.getElementById('micBtn').className='';"
            + "    document.getElementById('micBtn').innerText='🎤';"
            + "    document.getElementById('status').innerText='✅ Envoi du résultat…';"
            + "    var text=document.getElementById('transcript').innerText.trim();"
            + "    if(text&&text!=='Le texte reconnu apparaîtra ici…'){"
            + "      fetch('http://localhost:" + port + "/result',{"
            + "        method:'POST',"
            + "        headers:{'Content-Type':'text/plain;charset=UTF-8'},"
            + "        body:text"
            + "      }).then(function(){"
            + "        document.getElementById('sendStatus').innerText='✅ Résultat envoyé à LinguaLearn !';"
            + "        document.getElementById('status').innerText='Terminé. Vous pouvez fermer cet onglet.';"
            + "      }).catch(function(){"
            + "        document.getElementById('sendStatus').innerText='⚠️ Envoi échoué — vérifiez la connexion.';"
            + "      });"
            + "    } else {"
            + "      document.getElementById('status').innerText='Aucun texte capturé. Réessayez.';"
            + "    }"
            + "  };"
            + "  rec.start();"
            + "}"
            + "function toggle(){if(active&&rec)rec.stop();else start();}"
            + "setInterval(function(){"
            + "  if(active) {"
            + "    fetch('http://localhost:" + port + "/poll')"
            + "      .then(function(r){return r.text();})"
            + "      .then(function(t){ if(t==='STOP'){ active=false; rec.stop(); } });"
            + "  }"
            + "}, 1000);"
            + "window.onload=function(){setTimeout(start,600);};"
            + "</script></body></html>";
    }


    /** Word-overlap similarity score 0–100 */
    private int computeSimilarity(String reference, String spoken) {
        if (reference == null || spoken == null || spoken.isBlank()) return 0;
        String[] refWords = reference.toLowerCase().replaceAll("[^a-zàâçéèêëîïôûùüÿœæ ]", " ").split("\\s+");
        String[] spokenWords = spoken.toLowerCase().replaceAll("[^a-zàâçéèêëîïôûùüÿœæ ]", " ").split("\\s+");
        if (refWords.length == 0) return 0;
        Set<String> refSet = new HashSet<>(Arrays.asList(refWords));
        long matches = Arrays.stream(spokenWords).filter(refSet::contains).count();
        return (int) Math.min(100, (matches * 100) / refWords.length);
    }

    private void exportPdf() {
        if (currentLesson == null) return;
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Lesson PDF");
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        fc.setInitialFileName(currentLesson.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        java.io.File file = fc.showSaveDialog(null);
        if (file == null) return;

        new Thread(() -> {
            try {
                com.itextpdf.kernel.pdf.PdfWriter writer =
                        new com.itextpdf.kernel.pdf.PdfWriter(file.getAbsolutePath());
                com.itextpdf.kernel.pdf.PdfDocument pdf =
                        new com.itextpdf.kernel.pdf.PdfDocument(writer);
                com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);

                com.itextpdf.kernel.colors.Color blue =
                        new com.itextpdf.kernel.colors.DeviceRgb(37, 99, 235);
                com.itextpdf.kernel.colors.Color gray =
                        new com.itextpdf.kernel.colors.DeviceRgb(100, 116, 139);

                doc.add(new com.itextpdf.layout.element.Paragraph(currentLesson.getTitle())
                        .setFontSize(22).setBold().setFontColor(blue).setMarginBottom(8));

                doc.add(new com.itextpdf.layout.element.Paragraph("Course: "
                        + FrontNavigationState.getSelectedCourseTitle())
                        .setFontSize(11).setFontColor(gray).setMarginBottom(4));

                doc.add(new com.itextpdf.layout.element.Paragraph("XP Reward: "
                        + currentLesson.getXpReward() + " XP")
                        .setFontSize(11).setFontColor(gray).setMarginBottom(16));

                doc.add(new com.itextpdf.layout.element.LineSeparator(
                        new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginBottom(16));

                doc.add(new com.itextpdf.layout.element.Paragraph("📖 Lesson Content")
                        .setFontSize(14).setBold().setMarginBottom(8));

                String text = safeText(currentLesson.getContent(), "No content available.");
                doc.add(new com.itextpdf.layout.element.Paragraph(text)
                        .setFontSize(12).setMarginBottom(16));

                if (currentLesson.getVocabularyData() != null
                        && !currentLesson.getVocabularyData().isBlank()) {
                    doc.add(new com.itextpdf.layout.element.Paragraph("🔤 Vocabulary")
                            .setFontSize(14).setBold().setMarginBottom(8));
                    doc.add(new com.itextpdf.layout.element.Paragraph(currentLesson.getVocabularyData())
                            .setFontSize(11).setFontColor(gray));
                }

                doc.close();
                javafx.application.Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setHeaderText(null);
                    a.setTitle("PDF Saved");
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
        allLessons.setOnAction(e -> FrontRouter.goTo("/fxml/modules/frontoffice/front-courses.fxml"));

        Button nextLesson = new Button("Next Lesson →");
        nextLesson.getStyleClass().add("lesson-step-button");
        nextLesson.setOnAction(e -> openNextLesson());

        actions.getChildren().addAll(complete, allLessons, nextLesson);
        
        // Recommendation Area
        VBox recommendationArea = new VBox(15);
        recommendationArea.setAlignment(Pos.CENTER);
        recommendationArea.setPadding(new Insets(20, 0, 0, 0));
        recommendationArea.setVisible(false);
        recommendationArea.setManaged(false);

        Label recHeader = new Label("🌟 Recommended For You");
        recHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        Lesson recommended = getRecommendedLesson();
        if (recommended != null) {
            VBox recCard = new VBox(10);
            recCard.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: #cbd5e1; -fx-border-radius: 12;");
            recCard.setMaxWidth(400);
            recCard.setAlignment(Pos.CENTER);

            Label recTitle = new Label(recommended.getTitle());
            recTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #334155;");
            
            Button startRec = new Button("Start This Lesson");
            startRec.getStyleClass().add("success-button");
            startRec.setOnAction(e -> {
                currentLesson = recommended;
                // Ideally we'd fetch all lessons of the new course if it's different, 
                // but for simplicity we'll just show this one.
                switchStep(0);
            });

            recCard.getChildren().addAll(recTitle, startRec);
            recommendationArea.getChildren().addAll(recHeader, recCard);
        }

        doneCard.getChildren().addAll(title, subtitle, xp, actions, recommendationArea);
        
        // Save ref to show it later
        complete.setOnAction(e -> {
            completeCurrentLesson();
            recommendationArea.setVisible(true);
            recommendationArea.setManaged(true);
            complete.setDisable(true);
            complete.setText("✅ Lesson Completed!");
        });

        wrapper.getChildren().add(doneCard);
        return wrapper;
    }

    private Lesson getRecommendedLesson() {
        // 1. Try next lesson in same course
        if (currentLessonIndex + 1 < lessons.size()) {
            return lessons.get(currentLessonIndex + 1);
        }
        
        // 2. Try a random lesson from another course
        try {
            List<Lesson> all = lessonService.getAll();
            List<Lesson> others = all.stream()
                .filter(l -> l.getCourseId() != currentLesson.getCourseId())
                .collect(Collectors.toList());
            
            if (!others.isEmpty()) {
                return others.get(random.nextInt(others.size()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
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
