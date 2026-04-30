package org.example.controller.user.supportmanagement;

import org.example.entity.FAQ;
import org.example.entity.Reclamation;
import org.example.repository.supportmanagement.FAQDAO;
import org.example.repository.supportmanagement.ReclamationDAO;
import org.example.repository.supportmanagement.SupportResponseDAO;
import org.example.service.supportManagment.BadWordsFilter;
import org.example.service.supportManagment.GeminiService;
import org.example.service.supportManagment.PriorityDetector;
import org.example.service.supportManagment.PusherListenerService;
import org.example.util.Session;
import org.example.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class UserController implements Initializable {

    @FXML private TextField        subjectField;
    @FXML private TextArea         messageField;
    @FXML private Label            reclMsg;
    @FXML private Label            msgCorrection;

    // ── Image ─────────────────────────────────────────────────────────────
    @FXML private Label  labelImageChoisie;
    @FXML private Button btnSupprimerImage;
    private File selectedImageFile = null;

    // Cachés compatibilité
    @FXML private ComboBox<String> priorityBox;
    @FXML private ListView<String> userResponsesList;

    @FXML private TableView<Reclamation>           reclTable;
    @FXML private TableColumn<Reclamation, String> rColSubject;
    @FXML private TableColumn<Reclamation, String> rColStatus;
    @FXML private TableColumn<Reclamation, String> rColPriority;
    @FXML private TableColumn<Reclamation, String> rColDate;

    @FXML private TextField        reclSearch;
    @FXML private ComboBox<String> reclStatusFilter;
    @FXML private ComboBox<String> reclSortBox;

    @FXML private TableView<FAQ>           faqTable;
    @FXML private TableColumn<FAQ, String> fColQuestion;
    @FXML private TableColumn<FAQ, String> fColAnswer;
    @FXML private TableColumn<FAQ, String> fColCategory;
    @FXML private TextField        faqSearch;
    @FXML private TextField        faqCategoryFilter;
    @FXML private ComboBox<String> faqSortBox;

    private final ReclamationDAO     reclDao     = new ReclamationDAO();
    private final FAQDAO             faqDao      = new FAQDAO();
    private final SupportResponseDAO responseDao = new SupportResponseDAO();

    private List<Reclamation> reclamationCache = new ArrayList<>();
    private List<FAQ>         faqCache         = new ArrayList<>();
    private final List<LocalDateTime> submissionTimes = new ArrayList<>();

    // Serveur HTTP microphone
    private static com.sun.net.httpserver.HttpServer micServer = null;
    private static int micPort = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reclStatusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED"));
        reclStatusFilter.setValue("ALL");
        reclSortBox.setItems(FXCollections.observableArrayList(
                "Date desc", "Date asc", "Priorite", "Statut"));
        reclSortBox.setValue("Date desc");
        faqSortBox.setItems(FXCollections.observableArrayList(
                "Date desc", "Date asc", "Question A-Z"));
        faqSortBox.setValue("Date desc");

        rColSubject.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getSubject()));
        rColStatus.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        rColPriority.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getPriority()));
        rColDate.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getSubmittedAt() != null
                                ? c.getValue().getSubmittedAt().toString().replace("T", " ").substring(0, 16) : ""));

        fColQuestion.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getQuestion()));
        fColAnswer.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getAnswer()));
        fColCategory.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getCategory()));

        reclTable.setRowFactory(tv -> {
            TableRow<Reclamation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !row.isEmpty()) {
                    ouvrirDetail(row.getItem());
                }
            });
            return row;
        });

        chargerReclamations();
        chargerFAQ();

        int userId = getUserId();
        if (userId != 0) {
            PusherListenerService.demarrer(userId, message ->
                    Platform.runLater(() -> {
                        chargerReclamations();
                        afficherNotification(message);
                    })
            );
        }
    }

    private void afficherNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🔔 Nouvelle réponse");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    // ── Upload image ──────────────────────────────────────────────────────
    @FXML public void choisirImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = fc.showOpenDialog(subjectField.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            labelImageChoisie.setText("📎 " + file.getName());
            if (btnSupprimerImage != null) {
                btnSupprimerImage.setVisible(true);
                btnSupprimerImage.setManaged(true);
            }
        }
    }

    @FXML public void supprimerImage() {
        selectedImageFile = null;
        if (labelImageChoisie != null)
            labelImageChoisie.setText("Aucune image sélectionnée");
        if (btnSupprimerImage != null) {
            btnSupprimerImage.setVisible(false);
            btnSupprimerImage.setManaged(false);
        }
    }

    // Copie l'image dans le dossier uploads et retourne le chemin
    private String sauvegarderImage(File sourceFile) {
        try {
            Path uploadsDir = Path.of("uploads", "reclamations");
            Files.createDirectories(uploadsDir);
            String ext = sourceFile.getName().substring(sourceFile.getName().lastIndexOf('.'));
            String newName = "recl_" + System.currentTimeMillis() + ext;
            Path dest = uploadsDir.resolve(newName);
            Files.copy(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toString();
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde image: " + e.getMessage());
            return null;
        }
    }

    // ── Correction IA ─────────────────────────────────────────────────────
    @FXML public void corrigerAvantSoumission() {
        String sujet   = subjectField.getText().trim();
        String message = messageField.getText().trim();

        if (sujet.isEmpty() && message.isEmpty()) {
            if (msgCorrection != null) {
                msgCorrection.setStyle("-fx-text-fill: orange;");
                msgCorrection.setText("Rien à corriger.");
            }
            return;
        }
        if (msgCorrection != null) {
            msgCorrection.setStyle("-fx-text-fill: gray;");
            msgCorrection.setText("⏳ Correction en cours...");
        }
        subjectField.setDisable(true);
        messageField.setDisable(true);

        new Thread(() -> {
            String sujetCorrige   = sujet.isEmpty()   ? sujet   : GeminiService.corriger(sujet);
            String messageCorrige = message.isEmpty() ? message : GeminiService.corriger(message);
            Platform.runLater(() -> {
                if (sujetCorrige   != null) subjectField.setText(sujetCorrige);
                if (messageCorrige != null) messageField.setText(messageCorrige);
                subjectField.setDisable(false);
                messageField.setDisable(false);
                if (msgCorrection != null) {
                    msgCorrection.setStyle("-fx-text-fill: green;");
                    msgCorrection.setText("✅ Texte corrigé !");
                }
            });
        }).start();
    }

    // ── Microphone ────────────────────────────────────────────────────────
    @FXML public void ouvrirMicrophone() {
        try {
            if (micServer == null) {
                micServer = com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress(0), 0);
                micPort = micServer.getAddress().getPort();
                final String html = buildSpeechHtml();
                micServer.createContext("/speech.html", exchange -> {
                    byte[] content = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, content.length);
                    exchange.getResponseBody().write(content);
                    exchange.getResponseBody().close();
                });
                micServer.setExecutor(null);
                micServer.start();
            }
            java.awt.Desktop.getDesktop().browse(
                    new java.net.URI("http://localhost:" + micPort + "/speech.html"));
            reclMsg.setStyle("-fx-text-fill: #1a73e8; -fx-font-weight: bold;");
            reclMsg.setText("🎤 Parlez dans Chrome puis copiez le texte dans le champ message !");
        } catch (Exception e) {
            reclMsg.setStyle("-fx-text-fill: red;");
            reclMsg.setText("Erreur microphone: " + e.getMessage());
        }
    }

    private String buildSpeechHtml() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/><title>Microphone</title>"
                + "<style>body{font-family:Arial,sans-serif;text-align:center;padding:40px;background:#f8f9fa;}"
                + "h2{color:#1a73e8;}button{font-size:50px;border:none;background:#e8f0fe;"
                + "border-radius:50%;padding:20px;cursor:pointer;margin:20px;transition:all 0.2s;}"
                + "button:hover{background:#d2e3fc;transform:scale(1.1);}"
                + "#result{margin-top:20px;font-size:20px;color:#1a73e8;font-weight:bold;"
                + "padding:15px;background:#e8f0fe;border-radius:8px;min-height:50px;}"
                + "#status{font-size:16px;color:#555;margin:10px;}"
                + ".copy-btn{font-size:16px!important;border-radius:8px!important;"
                + "padding:10px 20px!important;background:#1a73e8!important;color:white;margin-top:10px;}"
                + "</style></head>"
                + "<body><h2>🎤 Reconnaissance vocale</h2>"
                + "<div id='status'>Cliquez sur le microphone et parlez en français</div>"
                + "<button id='micBtn' onclick='toggleMic()'>🎤</button>"
                + "<div id='result'>Le texte reconnu apparaîtra ici...</div>"
                + "<br/><button class='copy-btn' onclick='copyText()'>📋 Copier le texte</button>"
                + "<script>var r,active=false;"
                + "function toggleMic(){if(active){r.stop();return;}"
                + "var R=window.SpeechRecognition||window.webkitSpeechRecognition;"
                + "if(!R){document.getElementById('status').innerText='❌ Utilisez Chrome ou Edge';return;}"
                + "r=new R();r.lang='fr-FR';r.continuous=true;r.interimResults=true;"
                + "r.onstart=function(){active=true;document.getElementById('micBtn').innerText='⏹️';"
                + "document.getElementById('status').innerText='🔴 Écoute en cours...';};"
                + "r.onresult=function(e){var t='';for(var i=e.resultIndex;i<e.results.length;i++)"
                + "t+=e.results[i][0].transcript;document.getElementById('result').innerText=t;};"
                + "r.onerror=function(e){document.getElementById('status').innerText='Erreur: '+e.error;"
                + "active=false;document.getElementById('micBtn').innerText='🎤';};"
                + "r.onend=function(){active=false;document.getElementById('micBtn').innerText='🎤';"
                + "document.getElementById('status').innerText='✅ Terminé ! Copiez le texte.';};"
                + "r.start();}"
                + "function copyText(){var t=document.getElementById('result').innerText;"
                + "navigator.clipboard.writeText(t).then(()=>{"
                + "document.getElementById('status').innerText='✅ Copié dans le presse-papiers !';});}"
                + "</script></body></html>";
    }

    // ── Soumettre ─────────────────────────────────────────────────────────
    @FXML public void soumettre() {
        String subject = subjectField.getText().trim();
        String message = messageField.getText().trim();

        LocalDateTime maintenant = LocalDateTime.now();
        submissionTimes.removeIf(t -> t.isBefore(maintenant.minusMinutes(5)));
        if (submissionTimes.size() >= 3) {
            LocalDateTime prochaine = submissionTimes.get(0).plusMinutes(5);
            long sec = java.time.temporal.ChronoUnit.SECONDS.between(maintenant, prochaine);
            reclErreur("⚠️ Anti-spam : attendez " + sec / 60 + "m " + sec % 60 + "s"); return;
        }

        if (subject.isEmpty()) { reclErreur("Sujet obligatoire."); return; }
        if (subject.length() < 3) { reclErreur("Sujet trop court (min 3)."); return; }
        if (message.isEmpty()) { reclErreur("Message obligatoire."); return; }
        if (message.length() < 10) { reclErreur("Message trop court (min 10)."); return; }

        if (BadWordsFilter.containsBadWord(subject) || BadWordsFilter.containsBadWord(message)) {
            reclErreur("❌ Contenu inapproprié."); return;
        }

        String priority = PriorityDetector.detect(subject, message);
        Reclamation r = new Reclamation();
        r.setSubject(subject);
        r.setMessageBody(message);
        r.setPriority(priority);
        r.setUserId(getUserId());

        // ✅ Sauvegarder image si choisie
        if (selectedImageFile != null) {
            String imagePath = sauvegarderImage(selectedImageFile);
            r.setImagePath(imagePath);
        }

        LocalDateTime now = LocalDateTime.now();
        r.setSlaDeadline(switch (priority) {
            case "URGENT" -> now.plusHours(2);
            case "HIGH"   -> now.plusHours(8);
            default       -> now.plusDays(1);
        });

        boolean ok = reclDao.ajouter(r);
        if (ok) {
            submissionTimes.add(maintenant);
            subjectField.clear();
            messageField.clear();
            supprimerImage();
            if (msgCorrection != null) msgCorrection.setText("");
            reclSucces("✅ Soumise ! Priorité : " + priority);
            chargerReclamations();
        } else {
            reclErreur("Erreur lors de la soumission.");
        }
    }

    private void ouvrirDetail(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/user/reclamation_user_detail.fxml"));
            Parent root = loader.load();
            ReclamationUserDetailController ctrl = loader.getController();
            ctrl.setReclamation(r, this::chargerReclamations);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Réclamation — " + r.getSubject());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.err.println("Erreur ouverture détail: " + e.getMessage());
        }
    }

    @FXML public void chargerReclamations() {
        int userId = getUserId();
        if (userId == 0) return;
        reclamationCache = reclDao.getByUserId(userId);
        appliquerFiltresReclamations();
        if (userResponsesList != null) userResponsesList.getItems().clear();
    }

    @FXML public void chargerFAQ()           { faqCache = faqDao.getAll(); appliquerFiltresFAQ(); }
    @FXML public void rechercherFAQ()        { appliquerFiltresFAQ(); }
    @FXML public void filtrerReclamations()  { appliquerFiltresReclamations(); }
    @FXML public void trierReclamations()    { appliquerFiltresReclamations(); }
    @FXML public void filtrerFAQ()           { appliquerFiltresFAQ(); }
    @FXML public void trierFAQ()             { appliquerFiltresFAQ(); }
    @FXML public void chargerPourModif()     {}
    @FXML public void modifier()             {}
    @FXML public void supprimer()            {}

    private void appliquerFiltresReclamations() {
        String mot    = reclSearch != null && reclSearch.getText() != null
                ? reclSearch.getText().trim().toLowerCase() : "";
        String statut = reclStatusFilter.getValue();
        String tri    = reclSortBox.getValue();

        List<Reclamation> filtered = reclamationCache.stream()
                .filter(r -> mot.isEmpty() || r.getSubject().toLowerCase().contains(mot))
                .filter(r -> "ALL".equals(statut) || statut.equals(r.getStatus()))
                .collect(Collectors.toList());

        Comparator<Reclamation> comp =
                Comparator.comparing(Reclamation::getSubmittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        switch (tri == null ? "" : tri) {
            case "Date asc"  -> comp = Comparator.comparing(Reclamation::getSubmittedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "Priorite"  -> comp = Comparator.comparing(r -> prioriteOrdre(r.getPriority()));
            case "Statut"    -> comp = Comparator.comparing(Reclamation::getStatus);
        }
        filtered.sort(comp);
        reclTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void appliquerFiltresFAQ() {
        String mot       = faqSearch != null && faqSearch.getText() != null
                ? faqSearch.getText().trim().toLowerCase() : "";
        String categorie = faqCategoryFilter != null && faqCategoryFilter.getText() != null
                ? faqCategoryFilter.getText().trim().toLowerCase() : "";
        String tri       = faqSortBox.getValue();

        List<FAQ> filtered = faqCache.stream()
                .filter(f -> mot.isEmpty()
                        || f.getQuestion().toLowerCase().contains(mot)
                        || f.getAnswer().toLowerCase().contains(mot))
                .filter(f -> categorie.isEmpty()
                        || (f.getCategory() != null && f.getCategory().toLowerCase().contains(categorie)))
                .collect(Collectors.toList());

        Comparator<FAQ> comp =
                Comparator.comparing(FAQ::getSubmittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        switch (tri == null ? "" : tri) {
            case "Date asc"     -> comp = Comparator.comparing(FAQ::getSubmittedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "Question A-Z" -> comp = Comparator.comparing(f -> f.getQuestion().toLowerCase());
        }
        filtered.sort(comp);
        faqTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private int getUserId() {
        try {
            if (SessionManager.getCurrentUser() != null)
                return SessionManager.getCurrentUser().getId().intValue();
        } catch (Exception ignored) {}
        return Session.getCurrentUserId();
    }

    private int prioriteOrdre(String p) {
        return switch (p == null ? "" : p) {
            case "URGENT" -> 0; case "HIGH" -> 1; case "MEDIUM" -> 2; default -> 3;
        };
    }

    private void reclErreur(String msg) {
        reclMsg.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        reclMsg.setText(msg);
    }

    private void reclSucces(String msg) {
        reclMsg.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        reclMsg.setText(msg);
    }
}