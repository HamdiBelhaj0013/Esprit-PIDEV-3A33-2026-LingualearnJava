package org.example.controller.admin.supportmanagement;

import org.example.repository.supportmanagement.ReclamationDAO;
import org.example.repository.supportmanagement.SupportResponseDAO;
import org.example.service.supportManagment.OllamaService;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entity.Reclamation;
import org.example.util.MyDataBase;
import org.example.util.SessionManager;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class AdminController implements Initializable {

    @FXML private TableView<Reclamation>           reclTable;
    @FXML private TableColumn<Reclamation, String> rColSubject;
    @FXML private TableColumn<Reclamation, String> rColStatus;
    @FXML private TableColumn<Reclamation, String> rColPriority;
    @FXML private TableColumn<Reclamation, String> rColDate;
    @FXML private TableColumn<Reclamation, String> rColSLA;
    @FXML private TableColumn<Reclamation, String> rColSentiment; // ← NOUVEAU

    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private TextField        searchReclamation;
    @FXML private ComboBox<String> reclSortBox;

    @FXML private ComboBox<String> statutBox;
    @FXML private TextArea         reponseField;
    @FXML private Label            reponseMsg;
    @FXML private ListView<String> reponsesList;
    @FXML private ListView<String> historiqueList;
    @FXML private Label            slaTimerLabel;

    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statResolved;
    @FXML private Label statUrgent;
    @FXML private Label statLate;
    @FXML private Label statHigh;

    @FXML private Label  labelPage;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    private final ReclamationDAO     reclDao     = new ReclamationDAO();
    private final SupportResponseDAO responseDao = new SupportResponseDAO();

    private List<Reclamation> reclamationCache = new ArrayList<>();
    private List<Reclamation> filteredCache    = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private Timeline slaTimeline;

    // Cache sentiment pour ne pas rappeler Ollama à chaque rendu
    private final Map<Integer, String> sentimentCache = new HashMap<>();

    private static final Map<String, List<String>> WORKFLOW = Map.of(
            "PENDING",     List.of("IN_PROGRESS", "CLOSED"),
            "IN_PROGRESS", List.of("RESOLVED", "CLOSED"),
            "RESOLVED",    List.of("CLOSED"),
            "CLOSED",      List.of()
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        rColSubject.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getSubject()));
        rColStatus.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        rColPriority.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getPriority()));
        rColDate.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getSubmittedAt() != null
                                ? c.getValue().getSubmittedAt().toString()
                                .replace("T", " ").substring(0, 16)
                                : ""));
        if (rColSLA != null)
            rColSLA.setCellValueFactory(c ->
                    new javafx.beans.property.SimpleStringProperty(formatSLA(c.getValue())));

        // ── Colonne Sentiment ─────────────────────────────────────────────
        if (rColSentiment != null) {
            rColSentiment.setCellValueFactory(c -> {
                Reclamation r = c.getValue();
                // Retourne depuis le cache si disponible
                String cached = sentimentCache.get(r.getId());
                if (cached != null)
                    return new javafx.beans.property.SimpleStringProperty(cached);
                // Sinon retourne "..." pendant l'analyse
                return new javafx.beans.property.SimpleStringProperty("...");
            });

            // Couleur selon sentiment
            rColSentiment.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        switch (item) {
                            case "URGENT"  ->
                                    setStyle("-fx-text-fill:#A32D2D;-fx-font-weight:bold;");
                            case "NEGATIF" ->
                                    setStyle("-fx-text-fill:#854F0B;-fx-font-weight:bold;");
                            case "POSITIF" ->
                                    setStyle("-fx-text-fill:#3B6D11;-fx-font-weight:bold;");
                            case "NEUTRE"  ->
                                    setStyle("-fx-text-fill:#555;");
                            default        ->
                                    setStyle("-fx-text-fill:#aaa;");
                        }
                    }
                }
            });
        }

        filtreStatut.setItems(FXCollections.observableArrayList(
                "ALL", "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED"));
        filtreStatut.setValue("ALL");
        filtrePriorite.setItems(FXCollections.observableArrayList(
                "ALL", "URGENT", "HIGH", "MEDIUM"));
        filtrePriorite.setValue("ALL");
        reclSortBox.setItems(FXCollections.observableArrayList(
                "Date desc", "Date asc", "Priorite", "Statut", "SLA"));
        reclSortBox.setValue("Date desc");

        // Double-clic → fenêtre détail
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

        charger();
    }

    // ── Ouvrir fenêtre détail ─────────────────────────────────────────────
    private void ouvrirDetail(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/reclamation_detail.fxml"));
            Parent root = loader.load();
            ReclamationDetailController ctrl = loader.getController();
            ctrl.setReclamation(r, this::charger);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Réclamation — " + r.getSubject());
            stage.setScene(new Scene(root, 700, 750));
            stage.setResizable(true);
            stage.setMinWidth(650);
            stage.setMinHeight(600);
            stage.show();
        } catch (Exception e) {
            System.err.println("Erreur ouverture détail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML public void charger() {
        reclamationCache = reclDao.getAll();
        statTotal.setText(String.valueOf(reclamationCache.size()));
        statPending.setText(String.valueOf(
                reclamationCache.stream()
                        .filter(r -> "PENDING".equals(r.getStatus())).count()));
        statResolved.setText(String.valueOf(
                reclamationCache.stream()
                        .filter(r -> "RESOLVED".equals(r.getStatus())).count()));
        if (statUrgent != null)
            statUrgent.setText(String.valueOf(
                    reclamationCache.stream()
                            .filter(r -> "URGENT".equals(r.getPriority())).count()));
        if (statHigh != null)
            statHigh.setText(String.valueOf(
                    reclamationCache.stream()
                            .filter(r -> "HIGH".equals(r.getPriority())).count()));
        if (statLate != null)
            statLate.setText(String.valueOf(
                    reclamationCache.stream().filter(this::isSLALate).count()));

        filteredCache = new ArrayList<>(reclamationCache);
        currentPage = 1;
        afficherPage();

        if (reponsesList  != null) reponsesList.getItems().clear();
        if (reponseMsg    != null) reponseMsg.setText("");
        if (historiqueList != null) historiqueList.getItems().clear();
        if (slaTimeline   != null) { slaTimeline.stop(); slaTimeline = null; }
        if (slaTimerLabel != null) slaTimerLabel.setText("");

        // ── Analyse sentiment en arrière-plan ─────────────────────────────
        analyserSentimentsTousEnBackground();
    }

    // ── Lance l'analyse Ollama pour chaque réclamation non encore analysée ─
    private void analyserSentimentsTousEnBackground() {
        if (!OllamaService.estDisponible()) {
            System.out.println("Ollama non disponible");
            return;
        }

        // Copie de la liste pour éviter les problèmes de concurrence
        List<Reclamation> aAnalyser = new ArrayList<>();
        for (Reclamation r : reclamationCache) {
            if (!sentimentCache.containsKey(r.getId())) {
                aAnalyser.add(r);
            }
        }

        System.out.println("=== A analyser: " + aAnalyser.size() + " réclamations ===");

        // Un seul thread pour éviter surcharge
        new Thread(() -> {
            for (Reclamation r : aAnalyser) {
                String texte = r.getSubject() + " " + r.getMessageBody();
                String sentiment = OllamaService.analyserSentiment(texte);

                System.out.println("=== ID=" + r.getId() + " sentiment=" + sentiment + " ===");

                sentimentCache.put(r.getId(), sentiment);

                Platform.runLater(() -> reclTable.refresh());
            }
        }).start();
    }

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

        Comparator<Reclamation> comp =
                Comparator.comparing(Reclamation::getSubmittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        switch (tri == null ? "" : tri) {
            case "Date asc" -> {}
            case "Priorite" -> comp = Comparator.comparing(
                    r -> prioriteOrdre(r.getPriority()));
            case "Statut"   -> comp = Comparator.comparing(
                    r -> r.getStatus());
            case "SLA"      -> comp = Comparator.comparing(
                    r -> r.getSlaDeadline() == null
                            ? LocalDateTime.MAX : r.getSlaDeadline());
            default         -> comp = comp.reversed();
        }
        filteredCache.sort(comp);
        afficherPage();
    }

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

    @FXML public void pagePrecedente() {
        if (currentPage > 1) { currentPage--; afficherPage(); }
    }
    @FXML public void pageSuivante() {
        int pages = (int) Math.ceil((double) filteredCache.size() / PAGE_SIZE);
        if (currentPage < pages) { currentPage++; afficherPage(); }
    }

    @FXML public void changerStatut()  {
        showMsg("Double-cliquez sur une réclamation.", "orange"); }
    @FXML public void supprimer()      {
        showMsg("Double-cliquez sur une réclamation.", "orange"); }
    @FXML public void envoyerReponse() {
        showMsg("Double-cliquez sur une réclamation.", "orange"); }

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

    private String formatSLA(Reclamation r) {
        if (r.getSlaDeadline() == null) return "N/A";
        long minutes = ChronoUnit.MINUTES.between(
                LocalDateTime.now(), r.getSlaDeadline());
        if (minutes < 0) return "⚠️ En retard";
        return minutes / 60 + "h " + minutes % 60 + "m";
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

    private void showMsg(String txt, String color) {
        if (reponseMsg == null) return;
        reponseMsg.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        reponseMsg.setText(txt);
    }
}