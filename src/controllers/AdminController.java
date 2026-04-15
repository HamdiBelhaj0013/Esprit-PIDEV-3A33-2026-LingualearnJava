package controllers;

import dao.ReclamationDAO;
import dao.SupportResponseDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.Reclamation;
import models.SupportResponse;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminController implements Initializable {

    @FXML private TableView<Reclamation> reclTable;
    @FXML private TableColumn<Reclamation, Integer> rColId;
    @FXML private TableColumn<Reclamation, String> rColSubject;
    @FXML private TableColumn<Reclamation, String> rColStatus;
    @FXML private TableColumn<Reclamation, String> rColPriority;
    @FXML private TableColumn<Reclamation, Integer> rColUser;
    @FXML private TableColumn<Reclamation, String> rColDate;

    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> statutBox;
    @FXML private TextArea reponseField;
    @FXML private Label reponseMsg;
    @FXML private ListView<String> reponsesList;

    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statResolved;

    private ReclamationDAO reclDao = new ReclamationDAO();
    private SupportResponseDAO responseDao = new SupportResponseDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        rColSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        rColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        rColPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        rColUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        rColDate.setCellValueFactory(new PropertyValueFactory<>("submittedAt"));

        filtreStatut.setItems(FXCollections.observableArrayList(
            "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED"));
        statutBox.setItems(FXCollections.observableArrayList(
            "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED"));

        reclTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) chargerReponses(sel.getId()); });

        charger();
    }

    @FXML public void charger() {
        List<Reclamation> liste = reclDao.getAll();
        reclTable.setItems(FXCollections.observableArrayList(liste));
        statTotal.setText(String.valueOf(liste.size()));
        statPending.setText(String.valueOf(
            liste.stream().filter(r -> "PENDING".equals(r.getStatus())).count()));
        statResolved.setText(String.valueOf(
            liste.stream().filter(r -> "RESOLVED".equals(r.getStatus())).count()));
        reponsesList.getItems().clear();
        reponseMsg.setText("");
    }

    @FXML public void filtrer() {
        String s = filtreStatut.getValue();
        if (s == null) { charger(); return; }
        reclTable.setItems(FXCollections.observableArrayList(
            reclDao.getAll().stream()
                .filter(r -> s.equals(r.getStatus()))
                .collect(Collectors.toList())
        ));
    }

    @FXML public void changerStatut() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        String s = statutBox.getValue();
        if (sel == null || s == null) {
            showMsg("Sélectionne une réclamation et un statut.", "red"); return;
        }
        reclDao.changerStatut(sel.getId(), s);
        showMsg("Statut mis à jour : " + s, "green");
        charger();
    }

    @FXML public void envoyerReponse() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        String msg = reponseField.getText().trim();
        if (sel == null) { showMsg("Sélectionne une réclamation.", "red"); return; }
        if (msg.isEmpty()) { showMsg("La réponse ne peut pas être vide.", "red"); return; }

        SupportResponse sr = new SupportResponse(msg, sel.getId(), 2);
        responseDao.ajouter(sr);
        if ("PENDING".equals(sel.getStatus()))
            reclDao.changerStatut(sel.getId(), "IN_PROGRESS");
        reponseField.clear();
        showMsg("Réponse envoyée !", "green");
        chargerReponses(sel.getId());
        charger();
    }

    @FXML public void supprimer() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showMsg("Sélectionne une réclamation.", "red"); return; }
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

    @FXML public void ouvrirGestionFAQ() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/admin_faq.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Gestion FAQ");
            stage.setScene(new Scene(root, 900, 650));
            stage.show();
        } catch (Exception e) {
            System.out.println("Erreur ouverture FAQ: " + e.getMessage());
        }
    }

    private void chargerReponses(int id) {
        reponsesList.setItems(FXCollections.observableArrayList(
            responseDao.getByReclamationId(id).stream()
                .map(r -> "[" + r.getRespondedAt() + "] " + r.getMessage())
                .collect(Collectors.toList())
        ));
    }

    private void showMsg(String txt, String color) {
        reponseMsg.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        reponseMsg.setText(txt);
    }
}
