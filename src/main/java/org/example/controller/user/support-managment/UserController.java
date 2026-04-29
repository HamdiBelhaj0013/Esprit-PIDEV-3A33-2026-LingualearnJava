package org.example.controller.user.supportmanagement;

import org.example.entity.SupportResponse;
import org.example.repository.supportmanagement.FAQDAO;
import org.example.repository.supportmanagement.ReclamationDAO;
import org.example.repository.supportmanagement.SupportResponseDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entity.FAQ;
import org.example.entity.Reclamation;
import org.example.util.SessionManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import org.example.service.supportManagment.BadWordsFilter;
import org.example.service.supportManagment.PriorityDetector;

public class UserController implements Initializable {

    @FXML private TextField subjectField;
    @FXML private TextArea messageField;
    @FXML private ComboBox<String> priorityBox;
    @FXML private Label reclMsg;

    @FXML private TableView<Reclamation> reclTable;
    @FXML private TableColumn<Reclamation, String> rColSubject;
    @FXML private TableColumn<Reclamation, String> rColStatus;
    @FXML private TableColumn<Reclamation, String> rColPriority;
    @FXML private TableColumn<Reclamation, String> rColDate;
    @FXML private ListView<String> userResponsesList;
    @FXML private TextField reclSearch;
    @FXML private ComboBox<String> reclStatusFilter;
    @FXML private ComboBox<String> reclSortBox;

    @FXML private TableView<FAQ> faqTable;
    @FXML private TableColumn<FAQ, String> fColQuestion;
    @FXML private TableColumn<FAQ, String> fColAnswer;
    @FXML private TableColumn<FAQ, String> fColCategory;
    @FXML private TextField faqSearch;
    @FXML private TextField faqCategoryFilter;
    @FXML private ComboBox<String> faqSortBox;

    private final ReclamationDAO reclDao = new ReclamationDAO();
    private final FAQDAO faqDao = new FAQDAO();
    private final SupportResponseDAO responseDao = new SupportResponseDAO();
    private List<Reclamation> reclamationCache = new ArrayList<>();
    private List<FAQ> faqCache = new ArrayList<>();

    // Anti-spam : historique des soumissions par user
    private final List<LocalDateTime> submissionTimes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        priorityBox.setItems(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH"));
        reclStatusFilter.setItems(FXCollections.observableArrayList("ALL", "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED"));
        reclStatusFilter.setValue("ALL");
        reclSortBox.setItems(FXCollections.observableArrayList("Date desc", "Date asc", "Priorite", "Statut"));
        reclSortBox.setValue("Date desc");
        faqSortBox.setItems(FXCollections.observableArrayList("Date desc", "Date asc", "Question A-Z"));
        faqSortBox.setValue("Date desc");

        rColSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        rColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        rColPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        rColDate.setCellValueFactory(new PropertyValueFactory<>("submittedAt"));

        reclTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                if (sel != null) {
                    chargerReponsesUtilisateur(sel.getId());
                } else {
                    userResponsesList.getItems().clear();
                }
            }
        );

        fColQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        fColAnswer.setCellValueFactory(new PropertyValueFactory<>("answer"));
        fColCategory.setCellValueFactory(new PropertyValueFactory<>("category"));

        chargerReclamations();
        chargerFAQ();
    }

    @FXML public void chargerReclamations() {
        reclamationCache = reclDao.getByUserId(SessionManager.getCurrentUser().getId().intValue());
        appliquerFiltresReclamations();
        userResponsesList.getItems().clear();
    }

    @FXML public void chargerFAQ() {
        faqCache = faqDao.getAll();
        appliquerFiltresFAQ();
    }

    @FXML public void rechercherFAQ() {
        appliquerFiltresFAQ();
    }

    @FXML public void filtrerReclamations() { appliquerFiltresReclamations(); }
    @FXML public void trierReclamations() { appliquerFiltresReclamations(); }
    @FXML public void filtrerFAQ() { appliquerFiltresFAQ(); }
    @FXML public void trierFAQ() { appliquerFiltresFAQ(); }

    @FXML public void soumettre() {
        String subject = subjectField.getText().trim();
        String message = messageField.getText().trim();

        // ── Anti-spam : max 3 réclamations par 5 minutes ──────────────────
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.minusMinutes(5);

        // Supprimer les anciennes entrées hors fenêtre
        submissionTimes.removeIf(t -> t.isBefore(limite));

        if (submissionTimes.size() >= 3) {
            // Calculer le temps restant avant de pouvoir soumettre
            LocalDateTime prochaine = submissionTimes.get(0).plusMinutes(5);
            long secondesRestantes = java.time.temporal.ChronoUnit.SECONDS
                .between(maintenant, prochaine);
            long min = secondesRestantes / 60;
            long sec = secondesRestantes % 60;
            reclErreur("⚠️ Anti-spam : max 3 réclamations / 5 min. "
                + "Réessayez dans " + min + "m " + sec + "s.");
            return;
        }

        if (subject.isEmpty()) { reclErreur("Le sujet est obligatoire."); return; }
        if (subject.length() < 5) { reclErreur("Le sujet doit contenir au moins 5 caracteres."); return; }
        if (subject.length() > 100) { reclErreur("Sujet max 100 caracteres."); return; }
        if (message.isEmpty()) { reclErreur("Le message est obligatoire."); return; }
        if (message.length() < 5) { reclErreur("Le message doit contenir au moins 5 caracteres."); return; }

        // ⚠️ Vérification bad words AVANT soumission
        String texte = subject + " " + message;
        if (BadWordsFilter.containsBadWord(texte)) {
            // Bannir le user
            banUserLocally();
            reclErreur("⚠️ Message refusé : contenu inapproprié. Votre compte a été suspendu.");
            return;
        }

        // Priorité détectée automatiquement
        String priority = PriorityDetector.detect(subject, message);

        Reclamation r = new Reclamation(
            SessionManager.getCurrentUser().getId().intValue(),
            subject, message, priority
        );

        // SLA selon priorité
        switch (priority) {
            case "URGENT" -> r.setSlaDeadline(java.time.LocalDateTime.now().plusHours(2));
            case "HIGH"   -> r.setSlaDeadline(java.time.LocalDateTime.now().plusHours(12));
            default       -> r.setSlaDeadline(java.time.LocalDateTime.now().plusHours(24));
        }

        if (reclDao.ajouter(r)) {
            // ✅ Enregistrer le timestamp de cette soumission
            submissionTimes.add(maintenant);
            reclSucces("Reclamation soumise. Priorité détectée : " + priority);
            subjectField.clear();
            messageField.clear();
            chargerReclamations();
        } else {
            reclErreur("Erreur lors de la soumission.");
        }
    }

    private void banUserLocally() {
        String sql = "UPDATE users SET is_banned=1, banned_at=NOW(), " +
                     "banned_until=DATE_ADD(NOW(), INTERVAL 7 DAY), " +
                     "ban_reason='Bad word in reclamation' WHERE id=?";
        try (var conn = org.example.util.MyDataBase.getInstance().getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, SessionManager.getCurrentUser().getId().intValue());
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Ban failed: " + e.getMessage());
        }
    }

    @FXML public void modifier() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        if (sel == null) { reclErreur("Selectionne une reclamation."); return; }
        if (!"PENDING".equals(sel.getStatus())) {
            reclErreur("Impossible : la reclamation est deja " + sel.getStatus()); return;
        }

        String subject = subjectField.getText().trim();
        String message = messageField.getText().trim();

        if (subject.isEmpty()) { reclErreur("Le sujet est obligatoire."); return; }
        if (subject.length() < 5) { reclErreur("Le sujet doit contenir au moins 5 caracteres."); return; }
        if (message.isEmpty()) { reclErreur("Le message est obligatoire."); return; }
        if (message.length() < 5) { reclErreur("Le message doit contenir au moins 5 caracteres."); return; }

        // Bad words check
        if (BadWordsFilter.containsBadWord(subject + " " + message)) {
            banUserLocally();
            reclErreur("⚠️ Contenu inapproprié détecté. Compte suspendu.");
            return;
        }

        // Priorité recalculée automatiquement
        String priority = PriorityDetector.detect(subject, message);
        sel.setSubject(subject);
        sel.setMessageBody(message);
        sel.setPriority(priority);

        if (reclDao.modifier(sel)) {
            reclSucces("Reclamation modifiée. Priorité : " + priority);
            subjectField.clear();
            messageField.clear();
            chargerReclamations();
        } else {
            reclErreur("Modification impossible.");
        }
    }

    @FXML public void chargerPourModif() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        subjectField.setText(sel.getSubject());
        messageField.setText(sel.getMessageBody());
    }

    @FXML public void supprimer() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        if (sel == null) { reclErreur("Selectionne une reclamation."); return; }
        if (!"PENDING".equals(sel.getStatus())) {
            reclErreur("Impossible : statut = " + sel.getStatus()); return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer \"" + sel.getSubject() + "\" ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                reclDao.supprimer(sel.getId());
                reclSucces("Reclamation supprimee.");
                chargerReclamations();
            }
        });
    }

    private void chargerReponsesUtilisateur(int reclamationId) {
        List<SupportResponse> reponses = responseDao.getByReclamationId(reclamationId);
        if (reponses.isEmpty()) {
            userResponsesList.setItems(FXCollections.observableArrayList(
                "Aucune reponse admin pour le moment."
            ));
            return;
        }
        userResponsesList.setItems(FXCollections.observableArrayList(
            reponses.stream()
                .map(r -> "[" + r.getRespondedAt() + "] " + r.getMessage())
                .collect(Collectors.toList())
        ));
    }

    private void appliquerFiltresReclamations() {
        String mot = reclSearch.getText() == null ? "" : reclSearch.getText().trim().toLowerCase();
        String statut = reclStatusFilter.getValue();
        String tri = reclSortBox.getValue();

        List<Reclamation> filtered = reclamationCache.stream()
            .filter(r -> mot.isEmpty()
                || (r.getSubject() != null && r.getSubject().toLowerCase().contains(mot))
                || (r.getMessageBody() != null && r.getMessageBody().toLowerCase().contains(mot)))
            .filter(r -> statut == null || "ALL".equals(statut) || statut.equals(r.getStatus()))
            .collect(Collectors.toList());

        Comparator<Reclamation> comparator = Comparator.comparing(Reclamation::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("Date desc".equals(tri)) comparator = comparator.reversed();
        if ("Priorite".equals(tri)) comparator = Comparator.comparing(r -> String.valueOf(r.getPriority()));
        if ("Statut".equals(tri)) comparator = Comparator.comparing(r -> String.valueOf(r.getStatus()));
        filtered.sort(comparator);

        reclTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void appliquerFiltresFAQ() {
        String mot = faqSearch.getText() == null ? "" : faqSearch.getText().trim().toLowerCase();
        String categorie = faqCategoryFilter.getText() == null ? "" : faqCategoryFilter.getText().trim().toLowerCase();
        String tri = faqSortBox.getValue();

        List<FAQ> filtered = faqCache.stream()
            .filter(f -> mot.isEmpty()
                || (f.getQuestion() != null && f.getQuestion().toLowerCase().contains(mot))
                || (f.getAnswer() != null && f.getAnswer().toLowerCase().contains(mot))
                || (f.getSubject() != null && f.getSubject().toLowerCase().contains(mot)))
            .filter(f -> categorie.isEmpty()
                || (f.getCategory() != null && f.getCategory().toLowerCase().contains(categorie)))
            .collect(Collectors.toList());

        Comparator<FAQ> comparator = Comparator.comparing(FAQ::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("Date desc".equals(tri)) comparator = comparator.reversed();
        if ("Question A-Z".equals(tri)) comparator = Comparator.comparing(f -> String.valueOf(f.getQuestion()));
        filtered.sort(comparator);

        faqTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void reclSucces(String m) {
        reclMsg.setStyle("-fx-text-fill: #1D9E75; -fx-font-weight: bold;");
        reclMsg.setText(m);
    }

    private void reclErreur(String m) {
        reclMsg.setStyle("-fx-text-fill: #A32D2D; -fx-font-weight: bold;");
        reclMsg.setText(m);
    }
}
