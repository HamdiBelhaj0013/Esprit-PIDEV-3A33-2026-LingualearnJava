package org.example.controller.admin.supportmanagement;

import org.example.repository.supportmanagement.ReclamationDAO;
import org.example.repository.supportmanagement.SupportResponseDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entity.Reclamation;
import org.example.entity.SupportResponse;
import org.example.util.MyDataBase;
import org.example.util.SessionManager;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class AdminController implements Initializable {

    // ── Table ──────────────────────────────────────────────────────────────
    @FXML private TableView<Reclamation>          reclTable;
    @FXML private TableColumn<Reclamation,String> rColSubject;
    @FXML private TableColumn<Reclamation,String> rColStatus;
    @FXML private TableColumn<Reclamation,String> rColPriority;
    @FXML private TableColumn<Reclamation,String> rColDate;
    @FXML private TableColumn<Reclamation, LocalDateTime> rColSLA;      // ← nouveau

    // ── Filtres ────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private TextField        searchReclamation;
    @FXML private ComboBox<String> reclSortBox;

    // ── Actions ────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> statutBox;
    @FXML private TextArea         reponseField;
    @FXML private Label            reponseMsg;
    @FXML private ListView<String> reponsesList;

    // ── Stats ──────────────────────────────────────────────────────────────
    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statResolved;
    @FXML private Label statUrgent;   // ← nouveau
    @FXML private Label statLate;     // ← nouveau
    @FXML private Label statHigh;     // ← nouveau

    // ── Pagination ─────────────────────────────────────────────────────────
    @FXML private Label labelPage;    // ← nouveau  ex: "Page 1 / 5"
    @FXML private Button btnPrev;     // ← nouveau
    @FXML private Button btnNext;     // ← nouveau

    // ── Historique ─────────────────────────────────────────────────────────
    @FXML private ListView<String> historiqueList; // ← nouveau

    // ── SLA Timer label ────────────────────────────────────────────────────
    @FXML private Label slaTimerLabel; // ← nouveau  ex: "⏰ 1h 32m restantes"

    // ── DAOs ───────────────────────────────────────────────────────────────
    private final ReclamationDAO    reclDao    = new ReclamationDAO();
    private final SupportResponseDAO responseDao = new SupportResponseDAO();

    // ── State ──────────────────────────────────────────────────────────────
    private List<Reclamation> reclamationCache = new ArrayList<>();
    private List<Reclamation> filteredCache    = new ArrayList<>();

    // Pagination
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    // Anti-spam : map userId → dernière soumission
    private final Map<Integer, LocalDateTime> lastResponseTime = new HashMap<>();

    // SLA timer
    private Timeline slaTimeline;

    // ── Workflow : transitions autorisées ──────────────────────────────────
    private static final Map<String, List<String>> WORKFLOW = Map.of(
        "PENDING",     List.of("IN_PROGRESS", "CLOSED"),
        "IN_PROGRESS", List.of("RESOLVED", "CLOSED"),
        "RESOLVED",    List.of("CLOSED"),
        "CLOSED",      List.of()
    );

    // ══════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rColSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        rColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        rColPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        rColDate.setCellValueFactory(new PropertyValueFactory<>("submittedAt"));

        // Colonne SLA restant
        if (rColSLA != null) {
            rColSLA.setCellFactory(col -> new TableCell<Reclamation, LocalDateTime>() {
                @Override protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null); setStyle("");
                        return;
                    }
                    Reclamation r = (Reclamation) getTableRow().getItem();
                    setText(formatSLA(r));
                    setStyle(isSLALate(r) ? "-fx-text-fill: red; -fx-font-weight:bold;"
                                          : "-fx-text-fill: green;");
                }
            });
            rColSLA.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                    cellData.getValue().getSlaDeadline()));
        }

        filtreStatut.setItems(FXCollections.observableArrayList(
            "ALL","PENDING","IN_PROGRESS","RESOLVED","CLOSED"));
        filtreStatut.setValue("ALL");

        filtrePriorite.setItems(FXCollections.observableArrayList(
            "ALL","URGENT","HIGH","MEDIUM"));
        filtrePriorite.setValue("ALL");

        reclSortBox.setItems(FXCollections.observableArrayList(
            "Date desc","Date asc","Priorite","Statut","SLA"));
        reclSortBox.setValue("Date desc");

        // Listener sélection → charger réponses + historique + timer SLA
        reclTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                if (sel != null) {
                    chargerReponses(sel.getId());
                    chargerHistorique(sel.getId());
                    demarrerSLATimer(sel);
                    mettreAJourWorkflow(sel);
                }
            });

        charger();
    }

    // ── Charger toutes les réclamations ───────────────────────────────────
    @FXML public void charger() {
        reclamationCache = reclDao.getAll();
        appliquerFiltresEtTri();
        mettreAJourStats();
        reponsesList.getItems().clear();
        reponseMsg.setText("");
        if (historiqueList != null) historiqueList.getItems().clear();
        arreterSLATimer();
    }

    // ── Stats avancées ────────────────────────────────────────────────────
    private void mettreAJourStats() {
        statTotal.setText(String.valueOf(reclamationCache.size()));
        statPending.setText(String.valueOf(
            reclamationCache.stream().filter(r -> "PENDING".equals(r.getStatus())).count()));
        statResolved.setText(String.valueOf(
            reclamationCache.stream().filter(r -> "RESOLVED".equals(r.getStatus())).count()));

        if (statUrgent != null)
            statUrgent.setText(String.valueOf(
                reclamationCache.stream().filter(r -> "URGENT".equals(r.getPriority())).count()));

        if (statHigh != null)
            statHigh.setText(String.valueOf(
                reclamationCache.stream().filter(r -> "HIGH".equals(r.getPriority())).count()));

        if (statLate != null)
            statLate.setText(String.valueOf(
                reclamationCache.stream().filter(this::isSLALate).count()));
    }

    // ── Filtres + Tri + Pagination ────────────────────────────────────────
    @FXML public void filtrer()    { currentPage = 1; appliquerFiltresEtTri(); }
    @FXML public void rechercher() { currentPage = 1; appliquerFiltresEtTri(); }
    @FXML public void trier()      { appliquerFiltresEtTri(); }

    private void appliquerFiltresEtTri() {
        String mot      = searchReclamation.getText() == null ? ""
                        : searchReclamation.getText().trim().toLowerCase();
        String statut   = filtreStatut.getValue();
        String priorite = filtrePriorite.getValue();
        String tri      = reclSortBox.getValue();

        filteredCache = reclamationCache.stream()
            .filter(r -> mot.isEmpty()
                || r.getSubject().toLowerCase().contains(mot)
                || r.getMessageBody().toLowerCase().contains(mot))
            .filter(r -> "ALL".equals(statut)   || statut.equals(r.getStatus()))
            .filter(r -> "ALL".equals(priorite) || priorite.equals(r.getPriority()))
            .collect(Collectors.toList());

        // Tri
        Comparator<Reclamation> comp =
            Comparator.comparing(Reclamation::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        switch (tri == null ? "" : tri) {
            case "Date asc"  -> {}
            case "Priorite"  -> comp = Comparator.comparing(r -> prioriteOrdre(r.getPriority()));
            case "Statut"    -> comp = Comparator.comparing(r -> r.getStatus());
            case "SLA"       -> comp = Comparator.comparing(r ->
                                    r.getSlaDeadline() == null ? LocalDateTime.MAX : r.getSlaDeadline());
            default          -> comp = comp.reversed(); // Date desc
        }
        filteredCache.sort(comp);

        afficherPage();
    }

    // ── Pagination ────────────────────────────────────────────────────────
    private void afficherPage() {
        int total = filteredCache.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage = Math.min(currentPage, pages);

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        reclTable.setItems(FXCollections.observableArrayList(
            filteredCache.subList(from, to)));

        if (labelPage != null)
            labelPage.setText("Page " + currentPage + " / " + pages);
        if (btnPrev != null) btnPrev.setDisable(currentPage <= 1);
        if (btnNext != null) btnNext.setDisable(currentPage >= pages);
    }

    @FXML public void pagePrecedente() { if (currentPage > 1) { currentPage--; afficherPage(); } }
    @FXML public void pageSuivante()   {
        int pages = (int) Math.ceil((double) filteredCache.size() / PAGE_SIZE);
        if (currentPage < pages) { currentPage++; afficherPage(); }
    }

    // ── Workflow : mise à jour du ComboBox selon statut actuel ────────────
    private void mettreAJourWorkflow(Reclamation r) {
        if (statutBox == null) return;
        List<String> allowed = WORKFLOW.getOrDefault(r.getStatus(), List.of());
        statutBox.setItems(FXCollections.observableArrayList(allowed));
        if (!allowed.isEmpty()) statutBox.setValue(allowed.get(0));
    }

    @FXML public void changerStatut() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        String nouveauStatut = statutBox.getValue();
        if (sel == null || nouveauStatut == null) {
            showMsg("Selectionne une reclamation et un statut.", "red"); return;
        }

        // Vérification workflow
        List<String> allowed = WORKFLOW.getOrDefault(sel.getStatus(), List.of());
        if (!allowed.contains(nouveauStatut)) {
            showMsg("Transition " + sel.getStatus() + " → " + nouveauStatut + " non autorisée !", "red");
            return;
        }

        reclDao.changerStatut(sel.getId(), nouveauStatut);
        logHistorique(sel.getId(), "STATUS_CHANGED",
            "Statut changé de '" + sel.getStatus() + "' → '" + nouveauStatut + "'",
            sel.getStatus(), nouveauStatut);
        showMsg("Statut mis à jour : " + nouveauStatut, "green");
        charger();
    }

    // ── Anti-spam + Envoyer réponse ───────────────────────────────────────
    @FXML public void envoyerReponse() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        String msg = reponseField.getText().trim();

        if (sel == null) { showMsg("Selectionne une reclamation.", "red"); return; }
        if (msg.isEmpty()) { showMsg("La reponse ne peut pas etre vide.", "red"); return; }
        if (msg.length() < 5) { showMsg("Min 5 caracteres.", "red"); return; }

        // ── Anti-spam : max 1 réponse toutes les 10 secondes ──
        int adminId = SessionManager.getCurrentUser().getId().intValue();
        LocalDateTime lastTime = lastResponseTime.get(adminId);
        if (lastTime != null && ChronoUnit.SECONDS.between(lastTime, LocalDateTime.now()) < 10) {
            showMsg("⚠️ Anti-spam : attendez quelques secondes avant de répondre.", "red");
            return;
        }
        lastResponseTime.put(adminId, LocalDateTime.now());

        // Envoyer
        SupportResponse sr = new SupportResponse(msg, sel.getId(), adminId);
        responseDao.ajouter(sr);

        // Workflow auto : PENDING → IN_PROGRESS
        if ("PENDING".equals(sel.getStatus())) {
            reclDao.changerStatut(sel.getId(), "IN_PROGRESS");
            logHistorique(sel.getId(), "RESPONSE_ADDED",
                "Réponse ajoutée, statut → IN_PROGRESS", "PENDING", "IN_PROGRESS");
        } else {
            logHistorique(sel.getId(), "RESPONSE_ADDED", "Réponse ajoutée", null, null);
        }

        reponseField.clear();
        showMsg("Réponse envoyée.", "green");
        chargerReponses(sel.getId());
        chargerHistorique(sel.getId());
        charger();
    }

    // ── Supprimer ─────────────────────────────────────────────────────────
    @FXML public void supprimer() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showMsg("Selectionne une reclamation.", "red"); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer \"" + sel.getSubject() + "\" ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                reclDao.supprimerAdmin(sel.getId());
                showMsg("Supprimée.", "green");
                charger();
            }
        });
    }

    // ── SLA Timer ─────────────────────────────────────────────────────────
    private void demarrerSLATimer(Reclamation r) {
        arreterSLATimer();
        if (slaTimerLabel == null || r.getSlaDeadline() == null) return;

        slaTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), r.getSlaDeadline());
            long hours   = minutes / 60;
            long mins    = Math.abs(minutes % 60);

            if (minutes < 0) {
                slaTimerLabel.setText("🔴 SLA dépassé de " + Math.abs(hours) + "h " + mins + "m");
                slaTimerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else if (minutes < 60) {
                slaTimerLabel.setText("🟡 " + mins + "m restantes !");
                slaTimerLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            } else {
                slaTimerLabel.setText("🟢 " + hours + "h " + mins + "m restantes");
                slaTimerLabel.setStyle("-fx-text-fill: green;");
            }
        }));
        slaTimeline.setCycleCount(Timeline.INDEFINITE);
        slaTimeline.play();
    }

    private void arreterSLATimer() {
        if (slaTimeline != null) { slaTimeline.stop(); slaTimeline = null; }
        if (slaTimerLabel != null) { slaTimerLabel.setText(""); }
    }

    // ── Historique (audit logs) ───────────────────────────────────────────
    private void chargerHistorique(int reclamationId) {
        if (historiqueList == null) return;
        String sql = "SELECT action, description, created_at, old_value, new_value " +
                     "FROM support_audit_logs WHERE reclamation_id = ? ORDER BY created_at DESC";
        List<String> items = new ArrayList<>();
        try (Connection conn = MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String line = "[" + rs.getTimestamp("created_at") + "] "
                    + rs.getString("action") + " — "
                    + rs.getString("description");
                items.add(line);
            }
        } catch (SQLException e) {
            System.err.println("Erreur historique: " + e.getMessage());
        }
        historiqueList.setItems(FXCollections.observableArrayList(
            items.isEmpty() ? List.of("Aucun historique.") : items));
    }

    private void logHistorique(int reclamationId, String action,
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
            ps.setInt(6, SessionManager.getCurrentUser().getId().intValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur log historique: " + e.getMessage());
        }
    }

    // ── Helpers SLA ───────────────────────────────────────────────────────
    private String formatSLA(Reclamation r) {
        if (r.getSlaDeadline() == null) return "N/A";
        long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), r.getSlaDeadline());
        if (minutes < 0) return "⚠️ En retard";
        long h = minutes / 60, m = minutes % 60;
        return h + "h " + m + "m";
    }

    private boolean isSLALate(Reclamation r) {
        return r.getSlaDeadline() != null
            && r.getSlaDeadline().isBefore(LocalDateTime.now())
            && !"RESOLVED".equals(r.getStatus())
            && !"CLOSED".equals(r.getStatus());
    }

    private int prioriteOrdre(String p) {
        return switch (p == null ? "" : p) {
            case "URGENT" -> 0;
            case "HIGH"   -> 1;
            case "MEDIUM" -> 2;
            default       -> 3;
        };
    }

    // ── Charger réponses ──────────────────────────────────────────────────
    private void chargerReponses(int id) {
        List<String> items = responseDao.getByReclamationId(id).stream()
            .map(r -> "[" + r.getRespondedAt() + "] " + r.getMessage())
            .collect(Collectors.toList());
        reponsesList.setItems(FXCollections.observableArrayList(
            items.isEmpty() ? List.of("Aucune réponse.") : items));
    }

    // ── FAQ ───────────────────────────────────────────────────────────────
    @FXML public void ouvrirGestionFAQ() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/admin/admin_faq.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Gestion FAQ");
            stage.setScene(new Scene(root, 900, 650));
            stage.show();
        } catch (Exception e) {
            System.out.println("Erreur FAQ: " + e.getMessage());
        }
    }

    private void showMsg(String txt, String color) {
        reponseMsg.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        reponseMsg.setText(txt);
    }
}
