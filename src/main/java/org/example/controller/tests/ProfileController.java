package org.example.controller.tests;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.controller.UserDashboardController;
import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestResult;
import org.example.service.tests.MockTestService;
import org.example.service.tests.TestPerformanceAnalyzer;
import org.example.service.tests.CertificateService;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ProfileController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label    userNameLabel;
    @FXML private VBox     avatarBox;
    @FXML private Label    avatarLabel;
    @FXML private Label    fullNameLabel;
    @FXML private Label    emailLabel;
    @FXML private Label    planBadge;
    @FXML private Label    niveauBadge;

    // Stats cards
    @FXML private Label    scoreMoyenLabel;
    @FXML private Label    testsPassesLabel;
    @FXML private Label    meilleurScoreLabel;
    @FXML private Label    niveauAtteintLabel;

    // Graphique
    @FXML private LineChart<Number, Number> progressionChart;

    // Conteneurs dynamiques
    @FXML private VBox parTypeContainer;
    @FXML private VBox forcesContainer;
    @FXML private VBox recommendationsContainer;
    @FXML private HBox certificatsContainer;

    // ── State ─────────────────────────────────────────────────────────────────
    private User                               currentUser;
    private UserDashboardController            dashboardController;
    private Stage                              currentStage;
    private MockTestService                    mockTestService;
    private TestPerformanceAnalyzer            analyzer;
    private org.example.entity.tests.PlatformLanguage selectedLanguage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    /**
     * Entrée depuis LanguageSelect (mode PROFILE) — avec langue sélectionnée.
     * Toutes les stats sont filtrées sur les tests de cette langue.
     */
    public void init(User user, UserDashboardController dashboardController,
                     Stage stage, MockTestService mockTestService,
                     org.example.entity.tests.PlatformLanguage language) {
        this.currentUser         = user;
        this.dashboardController = dashboardController;
        this.currentStage        = stage;
        this.mockTestService     = mockTestService;
        this.selectedLanguage    = language;
        this.analyzer            = new TestPerformanceAnalyzer(user.getId());
        loadProfile();
    }

    /**
     * Entrée sans langue (accès direct) — stats globales.
     */
    public void init(User user, UserDashboardController dashboardController,
                     Stage stage, MockTestService mockTestService) {
        init(user, dashboardController, stage, mockTestService, null);
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void loadProfile() {
        loadIdentite();
        TestPerformanceAnalyzer.Resume resume = (selectedLanguage != null)
                ? analyzer.calculerResumePourLangue(selectedLanguage.getId())
                : analyzer.calculerResume();
        loadStats(resume);
        loadGraphique(resume);
        loadFaiblesses(resume);   // ← seulement les faiblesses
        loadParType(resume);
        loadRecommandations();
        loadCertificats(resume);
    }

    // ── Section 1 : Identité ─────────────────────────────────────────────────

    private void loadIdentite() {
        String nom = currentUser.getFullName();
        if (userNameLabel != null) userNameLabel.setText(nom);
        if (fullNameLabel != null) fullNameLabel.setText(nom);
        if (emailLabel    != null) emailLabel.setText(currentUser.getEmail());
        if (planBadge     != null) planBadge.setText("Plan " + currentUser.getSubscriptionPlan());

        if (selectedLanguage != null && niveauBadge != null)
            niveauBadge.setText("Profil · " + selectedLanguage.getName());

        String initiale = nom.isEmpty() ? "?" : String.valueOf(nom.charAt(0)).toUpperCase();
        if (avatarLabel != null) avatarLabel.setText(initiale);
        String[] couleurs = {"#3b5bdb","#2e7d32","#e65100","#c2185b","#0277bd","#6a1b9a"};
        String couleur = couleurs[Math.abs(nom.hashCode()) % couleurs.length];
        if (avatarBox != null) avatarBox.setStyle(
                "-fx-background-color:" + couleur + ";-fx-background-radius:50%;" +
                        "-fx-min-width:80;-fx-min-height:80;-fx-max-width:80;-fx-max-height:80;" +
                        "-fx-alignment:center;");
    }

    // ── Section 2 : Stats ─────────────────────────────────────────────────────

    private void loadStats(TestPerformanceAnalyzer.Resume r) {
        if (r.testsPassés == 0) {
            if (scoreMoyenLabel   != null) scoreMoyenLabel.setText("—");
            if (meilleurScoreLabel != null) meilleurScoreLabel.setText("—");
            if (niveauAtteintLabel != null) niveauAtteintLabel.setText("Débutant");
        } else {
            // ── FIX score /20 ─────────────────────────────────────────────────
            float moy20  = Math.round(r.scoreMoyen  / 100f * 20f * 10f) / 10f;
            float best20 = Math.round(r.meilleurScore / 100f * 20f * 10f) / 10f;

            // Format court pour les labels de stats (évite la troncature)
            if (scoreMoyenLabel    != null)
                scoreMoyenLabel.setText(String.format("%.1f/20  (%.0f%%)", moy20, r.scoreMoyen));
            if (meilleurScoreLabel != null)
                meilleurScoreLabel.setText(String.format("%.1f/20  (%.0f%%)", best20, r.meilleurScore));
            if (niveauAtteintLabel != null)
                niveauAtteintLabel.setText(r.niveauActuel);
        }
        if (testsPassesLabel != null) testsPassesLabel.setText(String.valueOf(r.testsPassés));
        if (niveauBadge      != null && selectedLanguage == null)
            niveauBadge.setText("Niveau " + r.niveauActuel);
    }

    // ── Graphique ─────────────────────────────────────────────────────────────

    private void loadGraphique(TestPerformanceAnalyzer.Resume r) {
        if (progressionChart == null) return;
        progressionChart.setLegendVisible(false);
        progressionChart.setAnimated(false);
        progressionChart.setCreateSymbols(true);
        progressionChart.setStyle("-fx-background-color:transparent;");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        if (r.progression.isEmpty()) {
            series.getData().add(new XYChart.Data<>(0, 0));
        } else {
            for (double[] pt : r.progression)
                series.getData().add(new XYChart.Data<>(pt[0], pt[1]));
        }
        progressionChart.getData().add(series);
        progressionChart.setStyle(
                "-fx-background-color:transparent;-fx-plot-background-color:#f8fafc;");
    }

    // ── Section Faiblesses uniquement ────────────────────────────────────────
    //
    // Logique corrigée :
    // 1. On calcule la moyenne des scores de l'utilisateur par section
    //    (Reading, Listening, Writing, Speaking) à partir de ses résultats.
    //    La query SQL de TestPerformanceAnalyzer.getPerformanceParSection()
    //    joint test_result → mock_test → test_question GROUP BY section_category
    //    et prend AVG(overall_score). Ce n'est pas parfait mais c'est la meilleure
    //    approximation possible sans avoir le score par question sauvegardé.
    //
    // 2. On n'affiche QUE les sections dont la moyenne < 50% → faiblesses.
    //    Si aucune faiblesse → message positif "Aucune faiblesse détectée !".
    //
    private void loadFaiblesses(TestPerformanceAnalyzer.Resume r) {
        if (forcesContainer == null) return;
        forcesContainer.getChildren().clear();

        if (r.parSection.isEmpty()) {
            Label vide = new Label("Passez des tests pour détecter vos faiblesses.");
            vide.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;-fx-font-style:italic;");
            forcesContainer.getChildren().add(vide);
            return;
        }

        // Filtrer : ne garder QUE les sections < 50% (faiblesses)
        List<Map.Entry<String, Double>> faiblesses = r.parSection.entrySet().stream()
                .filter(e -> e.getValue() < 50.0)
                .sorted(Map.Entry.comparingByValue()) // du plus faible au plus fort
                .toList();

        if (faiblesses.isEmpty()) {
            // Aucune faiblesse → afficher message positif
            HBox ok = new HBox(10);
            ok.setAlignment(Pos.CENTER_LEFT);
            ok.setStyle("-fx-background-color:#d3f9d8;-fx-background-radius:10;" +
                    "-fx-padding:14 18;-fx-border-color:#a5d6a7;-fx-border-radius:10;" +
                    "-fx-border-width:1;");
            Label icon = new Label("🏆");
            icon.setStyle("-fx-font-size:20px;");
            Label msg = new Label("Excellente performance ! Aucune faiblesse détectée.");
            msg.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#2e7d32;");
            ok.getChildren().addAll(icon, msg);
            forcesContainer.getChildren().add(ok);
            return;
        }

        // Titre
        Label titre = new Label("⚠️  Sections à améliorer (moyenne < 50%)");
        titre.setStyle("-fx-font-size:12px;-fx-text-fill:#6c7a99;-fx-font-style:italic;" +
                "-fx-padding:0 0 6 0;");
        forcesContainer.getChildren().add(titre);

        for (Map.Entry<String, Double> entry : faiblesses) {
            String section = entry.getKey();
            double score   = entry.getValue();
            float  sur20   = Math.round(score / 100f * 20f * 10f) / 10f;

            // Couleur selon sévérité
            String color, bg;
            if (score < 25) {
                color = "#b91c1c"; bg = "#fee2e2";
            } else if (score < 35) {
                color = "#d63939"; bg = "#ffe3e3";
            } else {
                color = "#e65100"; bg = "#fff3e0";
            }

            String icon = switch (section) {
                case "Reading"   -> "📖";
                case "Listening" -> "🎧";
                case "Writing"   -> "✍️";
                case "Speaking"  -> "🎤";
                default          -> "📌";
            };

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:10;" +
                    "-fx-border-color:" + color + ";-fx-border-radius:10;" +
                    "-fx-border-width:1;-fx-padding:12 16;");

            // Icone + badge section
            Label iconLbl = new Label(icon);
            iconLbl.setStyle("-fx-font-size:18px;");

            Label badge = new Label(section);
            badge.setStyle("-fx-background-color:white;-fx-text-fill:" + color + ";" +
                    "-fx-font-size:12px;-fx-font-weight:bold;" +
                    "-fx-background-radius:20;-fx-padding:3 12;" +
                    "-fx-border-color:" + color + ";-fx-border-radius:20;-fx-border-width:1;" +
                    "-fx-min-width:80;-fx-alignment:CENTER;");

            // Spacer
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            // Score en % ET /20
            VBox scoreBox = new VBox(2);
            scoreBox.setAlignment(Pos.CENTER_RIGHT);
            Label pctLbl = new Label(String.format("%.0f%%", score));
            pctLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
            Label sur20Lbl = new Label(String.format("(%.1f / 20)", sur20));
            sur20Lbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + color + ";-fx-opacity:0.8;");
            scoreBox.getChildren().addAll(pctLbl, sur20Lbl);

            // Label statut
            Label statut = new Label("À améliorer");
            statut.setStyle("-fx-font-size:11px;-fx-text-fill:" + color + ";" +
                    "-fx-font-style:italic;-fx-min-width:80;-fx-alignment:CENTER_RIGHT;");

            row.getChildren().addAll(iconLbl, badge, sp, scoreBox, statut);
            forcesContainer.getChildren().add(row);
        }
    }

    // ── Performance par type ──────────────────────────────────────────────────

    private void loadParType(TestPerformanceAnalyzer.Resume r) {
        if (parTypeContainer == null) return;
        parTypeContainer.getChildren().clear();

        if (r.parType.isEmpty()) {
            Label vide = new Label("Aucun test passé encore.");
            vide.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;");
            parTypeContainer.getChildren().add(vide);
            return;
        }

        for (Map.Entry<String, Double> entry : r.parType.entrySet()) {
            String type  = entry.getKey();
            double score = entry.getValue();

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);

            Label typeLbl = new Label(type);
            typeLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a1f36;" +
                    "-fx-min-width:90;");

            StackPane barre = new StackPane();
            barre.setPrefHeight(8);
            HBox.setHgrow(barre, Priority.ALWAYS);
            Region bg   = new Region();
            bg.setStyle("-fx-background-color:#e3e8f0;-fx-background-radius:4;");
            bg.setPrefHeight(8);
            Region fill = new Region();
            fill.setPrefWidth(Math.min(score * 1.5, 200));
            fill.setPrefHeight(8);
            String col = score >= 75 ? "#2fb344" : score >= 50 ? "#f59f00" : "#d63939";
            fill.setStyle("-fx-background-color:" + col + ";-fx-background-radius:4;");
            fill.setMaxWidth(Math.min(score * 1.5, 200));
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            barre.getChildren().addAll(bg, fill);

            Label pct = new Label(String.format("%.0f%%", score));
            pct.setStyle("-fx-font-size:12px;-fx-text-fill:#6c7a99;-fx-min-width:40;");

            row.getChildren().addAll(typeLbl, barre, pct);
            parTypeContainer.getChildren().add(row);
        }
    }

    // ── Recommandations ───────────────────────────────────────────────────────

    private void loadRecommandations() {
        if (recommendationsContainer == null) return;
        recommendationsContainer.getChildren().clear();

        List<TestResult> results = analyzer.getTousLesResultats();

        // ── CAS A : aucun résultat → tests A1 de la langue ou tous A1 ────────
        if (results.isEmpty()) {
            List<MockTest> tests = selectedLanguage != null
                    ? mockTestService.filterByLanguageId(selectedLanguage.getId()).stream()
                    .filter(t -> "A1".equals(t.getLevel())).toList()
                    : mockTestService.filterByLevel("A1");
            if (tests.isEmpty())
                tests = selectedLanguage != null
                        ? mockTestService.filterByLanguageId(selectedLanguage.getId())
                        : mockTestService.findAll();
            if (tests.isEmpty()) {
                Label vide = new Label("Aucun test disponible pour le moment.");
                vide.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;");
                recommendationsContainer.getChildren().add(vide);
                return;
            }
            Label hint = new Label("🎯 Commence par évaluer ton niveau :");
            hint.setStyle("-fx-font-size:13px;-fx-text-fill:#6c7a99;-fx-font-style:italic;-fx-padding:0 0 6 0;");
            recommendationsContainer.getChildren().add(hint);
            tests.stream().limit(3).forEach(t -> recommendationsContainer.getChildren()
                    .add(buildRecommandationCard(t, "Test de départ recommandé pour les débutants")));
            return;
        }

        // Faiblesses = sections < 50% (scores RÉELS depuis test_answer)
        Map<String, Double> parSection = (selectedLanguage != null)
                ? analyzer.getPerformanceParSectionEtLangue(selectedLanguage.getId())
                : analyzer.getPerformanceParSection();
        Set<String> sectionsFaibles = new HashSet<>();
        parSection.forEach((section, score) -> { if (score < 50.0) sectionsFaibles.add(section); });

        // ── CAS B : aucune faiblesse → tests du niveau supérieur ─────────────
        if (sectionsFaibles.isEmpty()) {
            String niveauActuel = analyzer.getNiveauActuel();
            String niveauCible  = getProchainNiveau(niveauActuel);
            List<MockTest> testsNiveau = selectedLanguage != null
                    ? mockTestService.filterByLanguageId(selectedLanguage.getId()).stream()
                    .filter(t -> niveauCible.equals(t.getLevel())).toList()
                    : mockTestService.filterByLevel(niveauCible);
            if (testsNiveau.isEmpty())
                testsNiveau = selectedLanguage != null
                        ? mockTestService.filterByLanguageId(selectedLanguage.getId())
                        : mockTestService.findAll();
            Label hint = new Label("🚀 Excellente performance ! Prochaine étape — niveau " + niveauCible + " :");
            hint.setStyle("-fx-font-size:13px;-fx-text-fill:#2e7d32;-fx-font-weight:bold;-fx-padding:0 0 6 0;");
            recommendationsContainer.getChildren().add(hint);
            testsNiveau.stream().limit(3).forEach(t -> recommendationsContainer.getChildren()
                    .add(buildRecommandationCard(t, "Test recommandé pour progresser au niveau " + niveauCible)));
            return;
        }

        // ── CAS C : faiblesses → tests ciblés triés par pertinence ───────────
        List<MockTest> candidats = selectedLanguage != null
                ? mockTestService.filterByLanguageId(selectedLanguage.getId())
                : mockTestService.findAll();

        Map<MockTest, Integer> pertinence = new LinkedHashMap<>();
        for (MockTest test : candidats) {
            long nb = mockTestService.findQuestionsByTest(test.getId()).stream()
                    .filter(q -> sectionsFaibles.contains(q.getSectionCategory())).count();
            pertinence.put(test, (int) nb);
        }

        String[] ordreNiveaux = {"A1","A2","B1","B2","C1","C2"};
        Map<String, Integer> niveauOrdre = new LinkedHashMap<>();
        for (int i = 0; i < ordreNiveaux.length; i++) niveauOrdre.put(ordreNiveaux[i], i);

        List<Map.Entry<MockTest, Integer>> triee = new ArrayList<>(pertinence.entrySet());
        triee.sort((a, b) -> {
            int diff = b.getValue() - a.getValue();
            if (diff != 0) return diff;
            return Integer.compare(
                    niveauOrdre.getOrDefault(a.getKey().getLevel(), 99),
                    niveauOrdre.getOrDefault(b.getKey().getLevel(), 99));
        });

        Label hint = new Label("📚 Tests ciblés sur tes sections à améliorer :");
        hint.setStyle("-fx-font-size:13px;-fx-text-fill:#6c7a99;-fx-font-style:italic;-fx-padding:0 0 6 0;");
        recommendationsContainer.getChildren().add(hint);

        triee.stream().limit(3).forEach(e -> {
            String raison = e.getValue() > 0
                    ? "Ce test contient " + e.getValue() + " question(s) dans tes sections faibles"
                    : "Test recommandé pour progresser";
            recommendationsContainer.getChildren().add(buildRecommandationCard(e.getKey(), raison));
        });
    }

    private HBox buildRecommandationCard(MockTest test, String raison) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
                "-fx-border-color:#e3e8f0;-fx-border-radius:10;-fx-border-width:1;" +
                "-fx-padding:16 20;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titre = new Label(test.getTitle());
        titre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1a1f36;");
        HBox badges = new HBox(8);
        for (String txt : new String[]{test.getTestType(), test.getLevel()}) {
            Label b = new Label(txt);
            b.setStyle("-fx-background-color:#eef2ff;-fx-text-fill:#3b5bdb;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-background-radius:20;-fx-padding:2 8;");
            badges.getChildren().add(b);
        }
        Label raisonLbl = new Label(raison);
        raisonLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#6c7a99;");
        raisonLbl.setWrapText(true);
        info.getChildren().addAll(titre, badges, raisonLbl);

        Button btn = new Button("Commencer");
        btn.setStyle("-fx-background-color:#3b5bdb;-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:6;" +
                "-fx-cursor:hand;-fx-padding:8 16;");
        btn.setOnAction(e -> naviguerVersTest(test));

        card.getChildren().addAll(info, btn);
        return card;
    }

    private void naviguerVersTest(MockTest test) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/TestPreviewView.fxml"));
            Parent root = loader.load();
            TestPreviewController ctrl = loader.getController();

            // Créer un UserTestListController factice pour le retour
            FXMLLoader listLoader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/UserTestListView.fxml"));
            listLoader.load();
            UserTestListController listCtrl = listLoader.getController();

            String level = test.getLevel();
            String[] levels;
            String levelName;
            if (level != null && (level.equals("A1") || level.equals("A2"))) {
                levels = new String[]{"A1","A2"}; levelName = "Beginner";
            } else if (level != null && (level.equals("B1") || level.equals("B2"))) {
                levels = new String[]{"B1","B2"}; levelName = "Intermediate";
            } else {
                levels = new String[]{"C1","C2"}; levelName = "Advanced";
            }

            org.example.entity.tests.PlatformLanguage lang =
                    test.getPlatformLanguage() != null ? test.getPlatformLanguage() : selectedLanguage;

            if (lang != null) {
                listCtrl.initWithFilter(mockTestService, currentUser, dashboardController,
                        currentStage, lang, levels, levelName);
            } else {
                listCtrl.init(mockTestService, currentUser, dashboardController, currentStage);
            }

            ctrl.init(mockTestService, test, currentUser, listCtrl, currentStage);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("LinguaLearn — Aperçu : " + test.getTitle());
        } catch (IOException e) {
            System.err.println("Erreur navigation test : " + e.getMessage());
        }
    }

    // ── Certificats ───────────────────────────────────────────────────────────

    private void loadCertificats(TestPerformanceAnalyzer.Resume r) {
        if (certificatsContainer == null) return;
        certificatsContainer.getChildren().clear();

        boolean hasBeginner     = checkCertifBeginner(r);
        boolean hasIntermediate = checkCertifIntermediate(r);
        boolean hasAdvanced     = checkCertifAdvanced(r);

        certificatsContainer.getChildren().addAll(
                buildCertifCard("BEGINNER",     "A1 + A2",                hasBeginner,     "#2e7d32","#e8f5e9"),
                buildCertifCard("INTERMEDIATE", "BEGINNER + B1 + B2",     hasIntermediate, "#e65100","#fff3e0"),
                buildCertifCard("ADVANCED",     "INTERMEDIATE + C1 + C2", hasAdvanced,     "#4527a0","#ede7f6")
        );
    }

    private boolean checkCertifBeginner(TestPerformanceAnalyzer.Resume r) {
        return r.parNiveau.getOrDefault("A1",0.0) >= 50
                && r.parNiveau.getOrDefault("A2",0.0) >= 50;
    }
    private boolean checkCertifIntermediate(TestPerformanceAnalyzer.Resume r) {
        return checkCertifBeginner(r)
                && r.parNiveau.getOrDefault("B1",0.0) >= 50
                && r.parNiveau.getOrDefault("B2",0.0) >= 50;
    }
    private boolean checkCertifAdvanced(TestPerformanceAnalyzer.Resume r) {
        return checkCertifIntermediate(r)
                && r.parNiveau.getOrDefault("C1",0.0) >= 50
                && r.parNiveau.getOrDefault("C2",0.0) >= 50;
    }

    private VBox buildCertifCard(String nom, String niveaux,
                                 boolean debloque, String color, String bg) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color:" + (debloque ? bg : "#f5f5f5") + ";" +
                "-fx-background-radius:14;" +
                "-fx-border-color:" + (debloque ? color : "#e0e0e0") + ";" +
                "-fx-border-radius:14;-fx-border-width:" + (debloque ? "2" : "1") + ";" +
                "-fx-padding:24;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);" +
                (debloque ? "" : "-fx-opacity:0.75;"));

        Label icone = new Label(debloque ? "🏅 CERTIF" : "🔒 VERROUILLÉ");
        icone.setStyle("-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (debloque ? color : "#9ca3af") + ";" +
                "-fx-background-color:" + (debloque ? "white" : "#f0f0f0") + ";" +
                "-fx-background-radius:20;-fx-padding:4 12;");

        Label nomLbl = new Label(nom);
        nomLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (debloque ? color : "#9ca3af") + ";");

        Label niveauxLbl = new Label(niveaux);
        niveauxLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6c7a99;");
        niveauxLbl.setWrapText(true);
        niveauxLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label statut = new Label(debloque ? "✅ Conditions remplies !" : "❌ Conditions non remplies");
        statut.setStyle("-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (debloque ? color : "#9ca3af") + ";");

        card.getChildren().addAll(icone, nomLbl, niveauxLbl, statut);

        if (debloque) {
            Button btn = new Button("📄 Générer le certificat");
            btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                    "-fx-font-size:12px;-fx-font-weight:bold;" +
                    "-fx-background-radius:6;-fx-cursor:hand;-fx-padding:8 14;");
            btn.setOnAction(e -> genererCertificat(nom, btn));
            card.getChildren().add(btn);
        }
        return card;
    }

    /** Retourne le niveau supérieur au niveau actuel de l'utilisateur. */
    private String getProchainNiveau(String niveau) {
        return switch (niveau != null ? niveau : "Débutant") {
            case "A1"       -> "A2";
            case "A2"       -> "B1";
            case "B1"       -> "B2";
            case "B2"       -> "C1";
            case "C1","C2"  -> "C2";
            default         -> "A1";
        };
    }

    // ── Génération du certificat PDF ──────────────────────────────────────────

    private void genererCertificat(String niveau, Button btn) {
        btn.setDisable(true);
        btn.setText("⏳ Génération...");

        String langName = selectedLanguage != null
                ? selectedLanguage.getName() : "Toutes langues";

        // Score moyen du niveau
        TestPerformanceAnalyzer.Resume r = (selectedLanguage != null)
                ? analyzer.calculerResumePourLangue(selectedLanguage.getId())
                : analyzer.calculerResume();
        double scoreMoyen = switch (niveau) {
            case "BEGINNER"     -> (r.parNiveau.getOrDefault("A1", 0.0)
                    + r.parNiveau.getOrDefault("A2", 0.0)) / 2.0;
            case "INTERMEDIATE" -> (r.parNiveau.getOrDefault("B1", 0.0)
                    + r.parNiveau.getOrDefault("B2", 0.0)) / 2.0;
            case "ADVANCED"     -> (r.parNiveau.getOrDefault("C1", 0.0)
                    + r.parNiveau.getOrDefault("C2", 0.0)) / 2.0;
            default             -> r.scoreMoyen;
        };

        // Dossier de sortie — bureau de l'utilisateur
        String outputDir = System.getProperty("user.home")
                + java.io.File.separator + "Desktop"
                + java.io.File.separator + "LinguaLearn_Certificats";

        new Thread(() -> {
            try {
                CertificateService certService = new CertificateService();
                org.example.entity.tests.Certificate cert = certService.generer(
                        currentUser, niveau, langName, (float) scoreMoyen, outputDir);

                javafx.application.Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("✅ PDF généré !");
                    btn.setStyle(btn.getStyle().replace("-fx-background-color:#2e7d32;",
                                    "-fx-background-color:#1b5e20;")
                            .replace("-fx-background-color:#e65100;", "-fx-background-color:#bf360c;")
                            .replace("-fx-background-color:#4527a0;", "-fx-background-color:#311b92;"));

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Certificat généré !");
                    alert.setHeaderText("✅ Votre certificat PDF a été créé.");
                    alert.setContentText(
                            "Fichier enregistré :\n" + cert.getPdfPath()
                                    + "\n\nUUID de vérification :\n" + cert.getUuid()
                                    + "\n\nURL de vérification :\n" + cert.getVerifyUrl());
                    alert.showAndWait();
                });

            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("❌ Erreur — Réessayer");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur génération PDF");
                    alert.setHeaderText("Impossible de générer le certificat.");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (dashboardController != null) dashboardController.returnToDashboard();
    }
}