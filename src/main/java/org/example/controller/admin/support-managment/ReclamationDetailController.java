package org.example.controller.admin.supportmanagement;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.entity.Reclamation;
import org.example.entity.SupportResponse;
import org.example.repository.supportmanagement.ReclamationDAO;
import org.example.repository.supportmanagement.SupportResponseDAO;
import org.example.service.supportManagment.EmailService;
import org.example.service.supportManagment.PusherService;
import org.example.util.MyDataBase;
import org.example.util.Session;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ReclamationDetailController implements Initializable {

    @FXML private Label labelSujet;
    @FXML private Label labelStatut;
    @FXML private Label labelPriorite;
    @FXML private Label labelDate;
    @FXML private Label labelSLA;
    @FXML private Label labelMessage;

    // ── Image de la réclamation ───────────────────────────────────────────
    @FXML private ImageView imageReclamation;
    @FXML private Label     labelImageInfo;

    @FXML private ComboBox<String> statutBox;
    @FXML private Button           btnChangerStatut;
    @FXML private Button           btnSupprimer;
    @FXML private Label            msgStatut;

    @FXML private TextArea         reponseField;
    @FXML private Button           btnEnvoyer;
    @FXML private Label            msgReponse;
    @FXML private ListView<String> reponsesList;
    @FXML private ListView<String> auditList;

    private final ReclamationDAO     reclDao     = new ReclamationDAO();
    private final SupportResponseDAO responseDao = new SupportResponseDAO();

    private Reclamation reclamation;
    private Runnable    onClose;
    private String      userEmail = null;

    private static final Map<String, List<String>> WORKFLOW = Map.of(
            "PENDING",     List.of("IN_PROGRESS", "CLOSED"),
            "IN_PROGRESS", List.of("RESOLVED", "CLOSED"),
            "RESOLVED",    List.of("CLOSED"),
            "CLOSED",      List.of()
    );

    private LocalDateTime lastResponseTime = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setReclamation(Reclamation r, Runnable onCloseCallback) {
        this.reclamation = r;
        this.onClose     = onCloseCallback;
        chargerEmailUser();
        rafraichir();
        verifierEtRepondreAutomatiquement(); // ← réponse auto si SLA dépassé
    }

    private void rafraichir() {
        labelSujet.setText(reclamation.getSubject());
        labelStatut.setText(reclamation.getStatus());
        labelPriorite.setText(reclamation.getPriority());
        labelDate.setText(reclamation.getSubmittedAt() != null
                ? reclamation.getSubmittedAt().toString().replace("T", " ").substring(0, 16) : "");
        labelMessage.setText(reclamation.getMessageBody());

        if (reclamation.getSlaDeadline() != null) {
            long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), reclamation.getSlaDeadline());
            if (minutes < 0)
                labelSLA.setText("🔴 SLA dépassé de " + Math.abs(minutes / 60) + "h " + Math.abs(minutes % 60) + "m");
            else
                labelSLA.setText("🟢 " + minutes / 60 + "h " + minutes % 60 + "m restantes");
        } else {
            labelSLA.setText("N/A");
        }

        // ✅ Afficher image si présente
        afficherImage();

        List<String> allowed = WORKFLOW.getOrDefault(reclamation.getStatus(), List.of());
        statutBox.setItems(FXCollections.observableArrayList(allowed));
        if (!allowed.isEmpty()) {
            statutBox.setValue(allowed.get(0));
            statutBox.setDisable(false);
            btnChangerStatut.setDisable(false);
        } else {
            statutBox.setValue(null);
            statutBox.setDisable(true);
            btnChangerStatut.setDisable(true);
        }

        chargerReponses();
        chargerAudit();
    }

    // ── Afficher image ────────────────────────────────────────────────────
    private void afficherImage() {
        if (imageReclamation == null) return;
        String imagePath = reclamation.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                java.io.File file = new java.io.File(imagePath);
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString(),
                            300, 200, true, true);
                    imageReclamation.setImage(img);
                    imageReclamation.setVisible(true);
                    imageReclamation.setManaged(true);
                    if (labelImageInfo != null) labelImageInfo.setText("📎 Image jointe");
                } else {
                    imageReclamation.setVisible(false);
                    imageReclamation.setManaged(false);
                    if (labelImageInfo != null) labelImageInfo.setText("⚠️ Image introuvable");
                }
            } catch (Exception e) {
                imageReclamation.setVisible(false);
                imageReclamation.setManaged(false);
            }
        } else {
            imageReclamation.setVisible(false);
            imageReclamation.setManaged(false);
            if (labelImageInfo != null) labelImageInfo.setText("Aucune image jointe.");
        }
    }

    // ── Charger email user ────────────────────────────────────────────────
    private void chargerEmailUser() {
        String sql = "SELECT email FROM users WHERE id = ?";
        try (Connection conn = MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reclamation.getUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) userEmail = rs.getString("email");
        } catch (SQLException e) {
            System.err.println("Erreur email user: " + e.getMessage());
        }
    }

    @FXML public void changerStatut() {
        String nouveauStatut = statutBox.getValue();
        if (nouveauStatut == null) { showMsg(msgStatut, "Choisis un statut.", "red"); return; }

        List<String> allowed = WORKFLOW.getOrDefault(reclamation.getStatus(), List.of());
        if (!allowed.contains(nouveauStatut)) {
            showMsg(msgStatut, "Transition non autorisée !", "red"); return;
        }

        boolean ok = reclDao.changerStatut(reclamation.getId(), nouveauStatut);
        if (!ok) { showMsg(msgStatut, "Erreur base de données.", "red"); return; }

        logAudit("STATUS_CHANGED",
                "Statut : '" + reclamation.getStatus() + "' → '" + nouveauStatut + "'",
                reclamation.getStatus(), nouveauStatut);

        reclamation.setStatus(nouveauStatut);
        showMsg(msgStatut, "✅ Statut mis à jour : " + nouveauStatut, "green");
        rafraichir();
        if (onClose != null) onClose.run();
    }

    @FXML public void envoyerReponse() {
        String msg = reponseField.getText().trim();
        if (msg.isEmpty()) { showMsg(msgReponse, "Réponse vide.", "red"); return; }
        if (msg.length() < 5) { showMsg(msgReponse, "Min 5 caractères.", "red"); return; }

        if (lastResponseTime != null
                && ChronoUnit.SECONDS.between(lastResponseTime, LocalDateTime.now()) < 10) {
            showMsg(msgReponse, "⚠️ Attendez quelques secondes.", "red"); return;
        }
        lastResponseTime = LocalDateTime.now();

        int adminId = Session.getCurrentUserId();
        SupportResponse sr = new SupportResponse(msg, reclamation.getId(), adminId);
        responseDao.ajouter(sr);

        if ("PENDING".equals(reclamation.getStatus())) {
            reclDao.changerStatut(reclamation.getId(), "IN_PROGRESS");
            logAudit("RESPONSE_ADDED", "Réponse ajoutée, statut → IN_PROGRESS",
                    "PENDING", "IN_PROGRESS");
            reclamation.setStatus("IN_PROGRESS");
        } else {
            logAudit("RESPONSE_ADDED", "Réponse ajoutée", null, null);
        }

        // ✅ Pusher notification
        if (reclamation.getUserId() != 0) {
            PusherService.notifierUser(reclamation.getUserId(), reclamation.getSubject());
        }

        // ✅ Email notification
        if (userEmail != null && !userEmail.isEmpty()) {
            EmailService.envoyerNotificationReponse(userEmail, reclamation.getSubject(), msg);
            System.out.println("📧 Email envoyé à " + userEmail);
        }

        reponseField.clear();
        showMsg(msgReponse, "✅ Réponse envoyée + email notifié.", "green");
        rafraichir();
        if (onClose != null) onClose.run();
    }

    @FXML public void supprimerReclamation() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + reclamation.getSubject() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Confirmer la suppression");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                reclDao.supprimerAdmin(reclamation.getId());
                if (onClose != null) onClose.run();
                fermer();
            }
        });
    }

    @FXML public void fermer() {
        ((Stage) labelSujet.getScene().getWindow()).close();
    }

    private void chargerReponses() {
        List<String> items = responseDao.getByReclamationId(reclamation.getId()).stream()
                .map(r -> "[" + r.getRespondedAt() + "] " + r.getMessage())
                .collect(Collectors.toList());
        reponsesList.setItems(FXCollections.observableArrayList(
                items.isEmpty() ? List.of("Aucune réponse.") : items));
    }

    private void chargerAudit() {
        String sql = "SELECT action, description, created_at FROM support_audit_logs " +
                "WHERE reclamation_id = ? ORDER BY created_at DESC";
        List<String> items = new ArrayList<>();
        try (Connection conn = MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reclamation.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add("[" + rs.getTimestamp("created_at") + "] "
                        + rs.getString("action") + " — "
                        + rs.getString("description"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur audit: " + e.getMessage());
        }
        auditList.setItems(FXCollections.observableArrayList(
                items.isEmpty() ? List.of("Aucun historique.") : items));
    }

    private void logAudit(String action, String description, String oldVal, String newVal) {
        int adminId = Session.getCurrentUserId();
        String sql = "INSERT INTO support_audit_logs " +
                "(action, description, created_at, old_value, new_value, reclamation_id, performed_by_id) " +
                "VALUES (?, ?, NOW(), ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, description);
            ps.setString(3, oldVal);
            ps.setString(4, newVal);
            ps.setInt(5, reclamation.getId());
            if (adminId != 0) ps.setInt(6, adminId);
            else              ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur log audit: " + e.getMessage());
        }
    }

    // Surcharge avec reclamationId explicite (pour réponse système sans reclamation.getId())
    private void logAudit(int reclamationId, String action,
                          String description, String oldVal, String newVal) {
        String sql = "INSERT INTO support_audit_logs " +
                "(action, description, created_at, old_value, new_value, reclamation_id, performed_by_id) " +
                "VALUES (?, ?, NOW(), ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, description);
            ps.setString(3, oldVal);
            ps.setString(4, newVal);
            ps.setInt(5, reclamationId);
            ps.setNull(6, Types.INTEGER); // système = null
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Erreur logAudit: " + e.getMessage());
        }
    }

    private void showMsg(Label label, String txt, String color) {
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        label.setText(txt);
    }

    // ── Text-to-Speech Windows natif ─────────────────────────────────────
    @FXML public void lireReclamation() {
        try {
            String texte = "Sujet : " + reclamation.getSubject()
                    + ". Priorité : " + reclamation.getPriority()
                    + ". Message : " + reclamation.getMessageBody();

            // Nettoyer le texte pour PowerShell
            String textePropre = texte
                    .replace("'", " ")
                    .replace("\"", " ")
                    .replace("\n", " ")
                    .replace("\r", " ");

            String script =
                    "Add-Type -AssemblyName System.Speech; " +
                            "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$synth.Rate = 0; " +
                            "$synth.Volume = 100; " +
                            "$synth.Speak('" + textePropre + "');";

            new ProcessBuilder("powershell", "-Command", script)
                    .start();

        } catch (Exception e) {
            System.err.println("TTS erreur: " + e.getMessage());
        }
    }

    // ── Réponse automatique si SLA dépassée ──────────────────────────────
    private void verifierEtRepondreAutomatiquement() {
        System.out.println("=== VERIFICATION SLA ===");
        if (reclamation == null) { System.out.println("reclamation null"); return; }
        System.out.println("Status: " + reclamation.getStatus());
        System.out.println("SLA: " + reclamation.getSlaDeadline());

        if (!"PENDING".equals(reclamation.getStatus())
                && !"IN_PROGRESS".equals(reclamation.getStatus())) {
            System.out.println("Status pas PENDING/IN_PROGRESS"); return;
        }
        if (reclamation.getSlaDeadline() == null) {
            System.out.println("SLA null"); return;
        }
        if (!reclamation.getSlaDeadline().isBefore(LocalDateTime.now())) {
            System.out.println("SLA pas encore dépassé"); return;
        }
        System.out.println("=== SLA DÉPASSÉ: Lancement Ollama ===");

        // Vérifier si une réponse automatique a déjà été envoyée
        boolean dejaRepondu = responseDao.getByReclamationId(reclamation.getId())
                .stream()
                .anyMatch(r -> r.getMessage().contains("[RÉPONSE AUTOMATIQUE]"));
        if (dejaRepondu) {
            System.out.println("Réponse automatique déjà envoyée."); return;
        }

        // Générer réponse avec Ollama en arrière-plan
        new Thread(() -> {
            System.out.println("=== THREAD OLLAMA DÉMARRÉ ===");
            String prompt =
                    "Tu es un agent de support. La réclamation suivante a dépassé son délai SLA. " +
                    "Génère une réponse d'excuse professionnelle en français, courte (2-3 phrases). " +
                    "Mentionne que le problème est pris en charge en priorité. " +
                    "Sujet de la réclamation : '" + reclamation.getSubject() + "'. " +
                    "Message : '" + reclamation.getMessageBody() + "'. " +
                    "Réponds UNIQUEMENT avec le message, sans explication.";

            String reponseIA = callOllama(prompt);
            System.out.println("=== RÉPONSE OLLAMA : " + reponseIA + " ===");

            if (reponseIA != null && !reponseIA.isBlank()) {
                String messageFinal = "[RÉPONSE AUTOMATIQUE] " + reponseIA;

                // Sauvegarder la réponse (authorId = null → sera stocké NULL en DB)
                SupportResponse sr = new SupportResponse(
                        messageFinal, reclamation.getId(), null);
                boolean ok = responseDao.ajouter(sr);
                System.out.println("=== SAUVEGARDE OK : " + ok + " ===");

                // Logger dans audit
                logAudit(reclamation.getId(),
                        "AUTO_RESPONSE",
                        "Réponse automatique envoyée car SLA dépassé",
                        null, null);

                javafx.application.Platform.runLater(() -> {
                    chargerReponses();
                    if (msgReponse != null) {
                        msgReponse.setStyle("-fx-text-fill: #1a73e8;");
                        msgReponse.setText("🤖 Réponse automatique envoyée (SLA dépassé)");
                    }
                });
            } else {
                System.out.println("=== RÉPONSE NULL OU VIDE ===");
            }
        }).start();
    }

    private String callOllama(String prompt) {
        try {
            com.google.gson.JsonObject body = new com.google.gson.JsonObject();
            body.addProperty("model", "phi3");
            body.addProperty("prompt", prompt);
            body.addProperty("stream", false);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(60))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.JsonObject json = com.google.gson.JsonParser
                        .parseString(response.body()).getAsJsonObject();
                return json.get("response").getAsString().trim();
            }
        } catch (Exception e) {
            System.err.println("Ollama erreur: " + e.getMessage());
        }
        return null;
    }
}
