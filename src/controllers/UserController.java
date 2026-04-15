package controllers;

import dao.FAQDAO;
import dao.ReclamationDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.FAQ;
import models.Reclamation;
import utils.Session;

import java.net.URL;
import java.util.ResourceBundle;

public class UserController implements Initializable {

    @FXML private TextField subjectField;
    @FXML private TextArea messageField;
    @FXML private ComboBox<String> priorityBox;
    @FXML private Label reclMsg;

    @FXML private TableView<Reclamation> reclTable;
    @FXML private TableColumn<Reclamation, Integer> rColId;
    @FXML private TableColumn<Reclamation, String> rColSubject;
    @FXML private TableColumn<Reclamation, String> rColStatus;
    @FXML private TableColumn<Reclamation, String> rColPriority;
    @FXML private TableColumn<Reclamation, String> rColDate;

    @FXML private TableView<FAQ> faqTable;
    @FXML private TableColumn<FAQ, String> fColQuestion;
    @FXML private TableColumn<FAQ, String> fColAnswer;
    @FXML private TableColumn<FAQ, String> fColCategory;
    @FXML private TextField faqSearch;

    private ReclamationDAO reclDao = new ReclamationDAO();
    private FAQDAO faqDao = new FAQDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        priorityBox.setItems(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH"));

        rColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        rColSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        rColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        rColPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        rColDate.setCellValueFactory(new PropertyValueFactory<>("submittedAt"));

        fColQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        fColAnswer.setCellValueFactory(new PropertyValueFactory<>("answer"));
        fColCategory.setCellValueFactory(new PropertyValueFactory<>("category"));

        chargerReclamations();
        chargerFAQ();
    }

    @FXML public void chargerReclamations() {
        reclTable.setItems(FXCollections.observableArrayList(
            reclDao.getByUserId(Session.getCurrentUserId())
        ));
    }

    @FXML public void chargerFAQ() {
        faqTable.setItems(FXCollections.observableArrayList(faqDao.getAll()));
    }

    @FXML public void rechercherFAQ() {
        String mot = faqSearch.getText().trim();
        if (mot.isEmpty()) { chargerFAQ(); return; }
        faqTable.setItems(FXCollections.observableArrayList(faqDao.rechercher(mot)));
    }

    @FXML public void soumettre() {
        String subject = subjectField.getText().trim();
        String message = messageField.getText().trim();
        String priority = priorityBox.getValue();

        if (subject.isEmpty()) { reclErreur("Le sujet est obligatoire."); return; }
        if (subject.length() > 100) { reclErreur("Sujet max 100 caractères."); return; }
        if (message.isEmpty()) { reclErreur("Le message est obligatoire."); return; }
        if (priority == null) { reclErreur("Choisis une priorité."); return; }

        Reclamation r = new Reclamation(Session.getCurrentUserId(), subject, message, priority);
        if (reclDao.ajouter(r)) {
            reclSucces("Réclamation soumise !");
            subjectField.clear();
            messageField.clear();
            priorityBox.setValue(null);
            chargerReclamations();
        } else {
            reclErreur("Erreur lors de la soumission.");
        }
    }

    @FXML public void modifier() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        if (sel == null) { reclErreur("Sélectionne une réclamation."); return; }
        if (!"PENDING".equals(sel.getStatus())) {
            reclErreur("Impossible : la réclamation est déjà " + sel.getStatus()); return;
        }

        String subject = subjectField.getText().trim();
        String message = messageField.getText().trim();
        String priority = priorityBox.getValue();

        if (subject.isEmpty()) { reclErreur("Le sujet est obligatoire."); return; }
        if (message.isEmpty()) { reclErreur("Le message est obligatoire."); return; }
        if (priority == null) { reclErreur("Choisis une priorité."); return; }

        sel.setSubject(subject);
        sel.setMessageBody(message);
        sel.setPriority(priority);

        if (reclDao.modifier(sel)) {
            reclSucces("Modifiée avec succès !");
            subjectField.clear();
            messageField.clear();
            priorityBox.setValue(null);
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
        priorityBox.setValue(sel.getPriority());
    }

    @FXML public void supprimer() {
        Reclamation sel = reclTable.getSelectionModel().getSelectedItem();
        if (sel == null) { reclErreur("Sélectionne une réclamation."); return; }
        if (!"PENDING".equals(sel.getStatus())) {
            reclErreur("Impossible : statut = " + sel.getStatus()); return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer \"" + sel.getSubject() + "\" ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                reclDao.supprimer(sel.getId());
                reclSucces("Supprimée.");
                chargerReclamations();
            }
        });
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
