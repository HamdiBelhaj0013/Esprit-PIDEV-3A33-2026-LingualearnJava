package org.example.service.tests;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * AntiCheatGuard — Surveillance anti-triche + API REST
 *
 * Fonctionnalités :
 *   1. Détecte les sorties de fenêtre (Stage.focusedProperty)
 *   2. Bloque Ctrl+C et Ctrl+V dans toute la scène du test
 *   3. Envoie chaque événement à AntiCheatApiServer (port 9091)
 *   4. 1 avertissement → 2ème sortie = soumission forcée -50%
 *   5. Fermeture fenêtre (ALT+F4) = soumission forcée immédiate
 */
public class AntiCheatGuard {

    private static final Logger LOG           = Logger.getLogger(AntiCheatGuard.class.getName());
    private static final int    MAX_SORTIES   = 1;
    private static final String API_EVENT_URL = "http://localhost:9091/api/anticheat/event";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public interface ForceSubmitCallback {
        void forceSubmit(int nbSorties, String raisonNote);
    }

    public interface AvertissementCallback {
        void onAvertissement(int nbSorties, int maxSorties);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Stage               stage;
    private final ForceSubmitCallback onForceSubmit;
    private AvertissementCallback     onAvertissement;

    // Contexte du test pour les logs API
    private long   userId       = 0;
    private long   testId       = 0;
    private String userFullName = "";
    private String testTitle    = "";

    private int     nbSorties        = 0;
    private boolean actif            = false;
    private boolean alerteEnCours    = false;
    private boolean soumissionForcee = false;

    private javafx.beans.value.ChangeListener<Boolean> focusListener;
    private javafx.event.EventHandler<KeyEvent>         keyFilter;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ── Constructeur ──────────────────────────────────────────────────────────

    public AntiCheatGuard(Stage stage, ForceSubmitCallback onForceSubmit) {
        this.stage         = stage;
        this.onForceSubmit = onForceSubmit;
    }

    public AntiCheatGuard setAvertissementCallback(AvertissementCallback cb) {
        this.onAvertissement = cb;
        return this;
    }

    /** Définir le contexte pour les logs API. */
    public AntiCheatGuard setContext(long userId, long testId,
                                     String userFullName, String testTitle) {
        this.userId       = userId;
        this.testId       = testId;
        this.userFullName = userFullName;
        this.testTitle    = testTitle;
        return this;
    }

    // ── Démarrage ─────────────────────────────────────────────────────────────

    public void start() {
        if (actif) return;
        actif = true;

        // ── 1. Surveillance focus ─────────────────────────────────────────────
        focusListener = (obs, wasFocused, isFocused) -> {
            if (!actif || soumissionForcee || alerteEnCours) return;
            if (!isFocused) Platform.runLater(this::handleFocusLost);
        };
        stage.focusedProperty().addListener(focusListener);

        // ── 2. Bloquer Ctrl+C / Ctrl+V / Ctrl+X ──────────────────────────────
        KeyCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
        KeyCombination ctrlV = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
        KeyCombination ctrlX = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN);

        keyFilter = event -> {
            if (!actif) return;
            if (ctrlC.match(event)) {
                event.consume();
                logApiEvent("COPY_ATTEMPT", false, 0, 0f, 0f,
                        "Ctrl+C bloqué à " + LocalDateTime.now().format(FMT));
                showBlockedAlert("copie (Ctrl+C)");
            } else if (ctrlV.match(event)) {
                event.consume();
                logApiEvent("PASTE_ATTEMPT", false, 0, 0f, 0f,
                        "Ctrl+V bloqué à " + LocalDateTime.now().format(FMT));
                showBlockedAlert("collage (Ctrl+V)");
            } else if (ctrlX.match(event)) {
                event.consume();
                logApiEvent("COPY_ATTEMPT", false, 0, 0f, 0f,
                        "Ctrl+X bloqué à " + LocalDateTime.now().format(FMT));
                showBlockedAlert("couper (Ctrl+X)");
            }
        };

        if (stage.getScene() != null)
            stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);

        // ── 3. Bloquer fermeture fenêtre ──────────────────────────────────────
        stage.setOnCloseRequest(event -> {
            if (actif && !soumissionForcee) {
                event.consume();
                Platform.runLater(this::handleFermetureForce);
            }
        });

        LOG.info("AntiCheatGuard activé.");
    }

    public void stop() {
        actif = false;
        if (focusListener != null) {
            stage.focusedProperty().removeListener(focusListener);
            focusListener = null;
        }
        if (keyFilter != null && stage.getScene() != null) {
            stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            keyFilter = null;
        }
        stage.setOnCloseRequest(null);
        LOG.info("AntiCheatGuard désactivé.");
    }

    // ── Gestionnaires ─────────────────────────────────────────────────────────

    private void handleFocusLost() {
        if (!actif || soumissionForcee || alerteEnCours) return;
        alerteEnCours = true;
        nbSorties++;

        logApiEvent("FOCUS_LOST", nbSorties > MAX_SORTIES, 0, 0f, 0f,
                "Sortie fenêtre n°" + nbSorties + " à " + LocalDateTime.now().format(FMT));

        if (nbSorties > MAX_SORTIES) {
            forcerSoumission("SORTIE_DE_FENETRE");
        } else {
            afficherAvertissement();
        }
    }

    private void handleFermetureForce() {
        if (!actif || soumissionForcee) return;
        logApiEvent("WINDOW_CLOSE", true, 50, 0f, 0f,
                "Tentative fermeture à " + LocalDateTime.now().format(FMT));
        nbSorties = MAX_SORTIES + 1;
        forcerSoumission("FERMETURE_FENETRE");
    }

    private void afficherAvertissement() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("⚠️ Avertissement Anti-Triche");
        alert.setHeaderText("Vous avez quitté la fenêtre du test !");
        alert.setContentText(
                "⚠️  Sortie détectée à " + LocalDateTime.now().format(FMT) + "\n\n" +
                        "🚨  DERNIER AVERTISSEMENT !\n" +
                        "La prochaine sortie entraînera la soumission automatique\n" +
                        "de votre test avec une pénalité de -50%%.\n\n" +
                        "⛔  Ctrl+C et Ctrl+V sont également bloqués.\n\n" +
                        "Restez sur cette fenêtre pendant toute la durée du test.");

        alert.getButtonTypes().setAll(new ButtonType("Je comprends — Retour au test"));
        if (onAvertissement != null) onAvertissement.onAvertissement(nbSorties, MAX_SORTIES);

        alert.showAndWait();
        alerteEnCours = false;

        Platform.runLater(() -> { stage.requestFocus(); stage.toFront(); });
    }

    private void showBlockedAlert(String action) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("⛔ Action bloquée");
        alert.setHeaderText(null);
        alert.setContentText(
                "⛔ La " + action + " est désactivée pendant le test.\n" +
                        "Rédigez votre réponse manuellement.");
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.show();
    }

    private void forcerSoumission(String raison) {
        if (soumissionForcee) return;
        soumissionForcee = true;
        actif = false;

        String heure = LocalDateTime.now().format(FMT);
        String raisonLabel = switch (raison) {
            case "FERMETURE_FENETRE" -> "Tentative de fermeture de la fenêtre";
            default -> "Sorties répétées (" + nbSorties + " fois)";
        };

        logApiEvent("FORCE_SUBMIT", true, 50, 0f, 0f,
                raisonLabel + " — soumission forcée à " + heure);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("❌ Test soumis automatiquement");
        alert.setHeaderText("Comportement suspect détecté");
        alert.setContentText(
                "🚨  " + raisonLabel + "\n\n" +
                        "Votre test a été soumis automatiquement à " + heure + ".\n" +
                        "Une pénalité de -50%% a été appliquée.\n\n" +
                        "Ce résultat a été enregistré et signalé.");
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();

        // Nettoyer les listeners
        if (focusListener != null) {
            stage.focusedProperty().removeListener(focusListener);
            focusListener = null;
        }
        if (keyFilter != null && stage.getScene() != null) {
            stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            keyFilter = null;
        }
        stage.setOnCloseRequest(null);

        String note = buildNote(raisonLabel, heure);
        if (onForceSubmit != null) onForceSubmit.forceSubmit(nbSorties, note);
    }

    // ── Note triche ───────────────────────────────────────────────────────────

    private String buildNote(String raison, String heure) {
        return "⚠️ RAPPORT ANTI-TRICHE\n"
                + "───────────────────────────────\n"
                + "Raison       : " + raison + "\n"
                + "Sorties      : " + nbSorties + "\n"
                + "Heure        : " + heure + "\n"
                + "Pénalité     : -50%\n"
                + "Soumis par   : Système (anti-triche)\n"
                + "API log      : http://localhost:9091/api/anticheat/logs\n"
                + "───────────────────────────────";
    }

    // ── Envoi à l'API (asynchrone) ────────────────────────────────────────────

    public void logApiEvent(String eventType, boolean soumisAuto, int penalitePct,
                            float scoreAvant, float scoreApres, String detail) {
        new Thread(() -> {
            try {
                String body = "{"
                        + "\"userId\":"        + userId + ","
                        + "\"testId\":"        + testId + ","
                        + "\"userFullName\":\"" + userFullName.replace("\"", "") + "\","
                        + "\"testTitle\":\""   + testTitle.replace("\"", "") + "\","
                        + "\"eventType\":\""   + eventType + "\","
                        + "\"nbSorties\":"     + nbSorties + ","
                        + "\"soumisAuto\":"    + soumisAuto + ","
                        + "\"penalitePct\":"   + penalitePct + ","
                        + "\"scoreAvant\":"    + scoreAvant + ","
                        + "\"scoreApres\":"    + scoreApres + ","
                        + "\"detail\":\""      + detail.replace("\"", "'") + "\""
                        + "}";

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_EVENT_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                LOG.warning("AntiCheat log API échoué : " + e.getMessage());
            }
        }).start();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int     getNbSorties()       { return nbSorties; }
    public boolean isSoumissionForcee() { return soumissionForcee; }
    public boolean isActif()            { return actif; }
}