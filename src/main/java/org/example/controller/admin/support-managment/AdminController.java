package org.example.controller.admin.supportmanagement;

import org.example.repository.supportmanagement.ReclamationDAO;
import org.example.repository.supportmanagement.SupportResponseDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.entity.Reclamation;
import org.example.entity.SupportResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminController implements Initializable {

    @FXML private TableView<Reclamation> reclTable;
    @FXML private TableColumn<Reclamation, String> rColSubject;
    @FXML private TableColumn<Reclamation, String> rColStatus;
    @FXML private TableColumn<Reclamation, String> rColPriority;
    @FXML private TableColumn<Reclamation, String> rColDate;

    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private TextField searchReclamation;
    @FXML private ComboBox<String> reclSortBox;
    @FXML private ComboBox<String> statutBox;
    @FXML private TextArea reponseField;
    @FXML private Label reponseMsg;
    @FXML private ListView<String> reponsesList;

    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statResolved;

    private final ReclamationDAO reclDao = new ReclamationDAO();
    private final SupportResponseDAO responseDao = new SupportResponseDAO();
    private List<Reclamation> reclamationCache = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rColSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        rColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        rColPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        rColDate.setCellValueFactory(new PropertyValueFactory<>("submittedAt"));

        filtreStatut.setItems(FXCollections.observableArrayList(
            "ALL", "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED"));
        filtreStatut.setValue("ALL");
        filtrePriorite.setItems(FXCollections.observableArrayList("ALL", "LOW", "MEDIUM", "HIGH"));
        filtrePriorite.setValue("ALL");
        reclSortBox.setItems(FXCollections.observableArrayList("Date desc", "Date asc", "Priorite", "Statut"));
        reclSortBox.setValue("Date desc");
        statutBox.setItems(FXCollections.observableArrayList(
            "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED"));

        reclTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) chargerReponses(sel.getId()); });

        charger();
    }

    @FXML public void charger() {
        reclamationCache = reclDao.getAll();
        appliquerFiltresEtTri();
        statTotal.setText(String.valueOf(reclamationCache.size()));
        statPending.setText(String.valueOf(
            reclamationCache.stream().filter(r -> "PENDING".equals(r.getStatus())).count()));
        statResolved.setText(String.valueOf(
            reclamationCache.stream().filter(r -> "RESOLVED".equals(r.getStatus())).count()));
        reponsesList.getItems().clear();
        reponseMsg.setText("");
    }

    @FXML public void filtrer() {
        appliquerFiltresEtTri();
    }
    @FXML public void rechercher() { appliquerFiltresEtTri(); }
    @FXML public void trier() { appliquerFiltresEtTri(); }

    @FXML public void changerStatut() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        String s = statutBox.getValue();
        if (sel == null || s == null) {
            showMsg("Selectionne une reclamation et un statut.", "red"); return;
        }
        reclDao.changerStatut(sel.getId(), s);
        showMsg("Statut mis a jour : " + s, "green");
        charger();
    }

    @FXML public void envoyerReponse() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        String msg = reponseField.getText().trim();
        if (sel == null) { showMsg("Selectionne une reclamation.", "red"); return; }
        if (msg.isEmpty()) { showMsg("La reponse ne peut pas etre vide.", "red"); return; }
        if (msg.length() < 5) { showMsg("La reponse doit contenir au moins 5 caracteres.", "red"); return; }

        SupportResponse sr = new SupportResponse(msg, sel.getId(), org.example.util.SessionManager.getCurrentUser().getId().intValue());
        responseDao.ajouter(sr);
        if ("PENDING".equals(sel.getStatus()))
            reclDao.changerStatut(sel.getId(), "IN_PROGRESS");
        reponseField.clear();
        showMsg("Reponse envoyee.", "green");
        chargerReponses(sel.getId());
        charger();
    }

    @FXML public void supprimer() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showMsg("Selectionne une reclamation.", "red"); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer \"" + sel.getSubject() + "\" ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                reclDao.supprimerAdmin(sel.getId());
                showMsg("Supprimee.", "green");
                charger();
            }
        });
    }

    @FXML public void ouvrirGestionFAQ() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin/admin_faq.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Gestion FAQ");
            stage.setScene(new Scene(root, 900, 650));
            stage.show();
        } catch (Exception e) {
            System.out.println("Erreur ouverture FAQ: " + e.getMessage());
        }
    }

    private void chargerReponses(int id) {
        List<String> items = responseDao.getByReclamationId(id).stream()
            .map(r -> "[" + r.getRespondedAt() + "] " + r.getMessage())
            .collect(Collectors.toList());
        if (items.isEmpty()) {
            items = List.of("Aucune reponse enregistree.");
        }
        reponsesList.setItems(FXCollections.observableArrayList(items));
    }

    private void appliquerFiltresEtTri() {
        String mot = searchReclamation.getText() == null ? "" : searchReclamation.getText().trim().toLowerCase();
        String statut = filtreStatut.getValue();
        String priorite = filtrePriorite.getValue();
        String tri = reclSortBox.getValue();

        List<Reclamation> filtered = reclamationCache.stream()
            .filter(r -> mot.isEmpty()
                || (r.getSubject() != null && r.getSubject().toLowerCase().contains(mot))
                || (r.getMessageBody() != null && r.getMessageBody().toLowerCase().contains(mot)))
            .filter(r -> statut == null || "ALL".equals(statut) || statut.equals(r.getStatus()))
            .filter(r -> priorite == null || "ALL".equals(priorite) || priorite.equals(r.getPriority()))
            .collect(Collectors.toList());

        Comparator<Reclamation> comparator = Comparator.comparing(Reclamation::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("Date desc".equals(tri)) comparator = comparator.reversed();
        if ("Priorite".equals(tri)) comparator = Comparator.comparing(r -> String.valueOf(r.getPriority()));
        if ("Statut".equals(tri)) comparator = Comparator.comparing(r -> String.valueOf(r.getStatus()));
        filtered.sort(comparator);

        reclTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showMsg(String txt, String color) {
        reponseMsg.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        reponseMsg.setText(txt);
    }
}

