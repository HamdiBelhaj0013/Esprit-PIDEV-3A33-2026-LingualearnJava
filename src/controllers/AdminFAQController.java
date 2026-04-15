
package controllers;
import dao.FAQDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.FAQ;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminFAQController implements Initializable {

    @FXML private TextField faqQuestion;
    @FXML private TextArea faqAnswer;
    @FXML private TextField faqSubject;
    @FXML private TextField faqCategory;
    @FXML private TextField searchField;
    @FXML private Label msgLabel;

    @FXML private TableView<FAQ> faqTable;
    @FXML private TableColumn<FAQ, Integer> colId;
    @FXML private TableColumn<FAQ, String> colQuestion;
    @FXML private TableColumn<FAQ, String> colCategory;
    @FXML private TableColumn<FAQ, String> colSubject;

    @FXML private Label statTotal;

    private FAQDAO dao = new FAQDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));

        faqTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> { if (sel != null) remplirForm(sel); }
        );

        charger();
    }

    @FXML public void charger() {
        List<FAQ> liste = dao.getAll();
        faqTable.setItems(FXCollections.observableArrayList(liste));
        statTotal.setText("Total : " + liste.size() + " FAQ");
    }

    @FXML public void rechercher() {
        String mot = searchField.getText().trim();
        if (mot.isEmpty()) { charger(); return; }
        faqTable.setItems(FXCollections.observableArrayList(dao.rechercher(mot)));
    }

    @FXML public void ajouter() {
        String question = faqQuestion.getText().trim();
        String answer = faqAnswer.getText().trim();

        if (question.isEmpty()) {
            erreur("La question est obligatoire."); return;
        }
        if (question.length() > 255) {
            erreur("La question ne doit pas d passer 255 caract res."); return;
        }
        if (answer.isEmpty()) {
            erreur("La r ponse est obligatoire."); return;
        }
        if (dao.existeDejaQuestion(question, -1)) {
            erreur("Cette question existe d j ."); return;
        }

        FAQ faq = new FAQ(
                question,
                answer,
                faqSubject.getText().trim().isEmpty() ? null : faqSubject.getText().trim(),
                faqCategory.getText().trim().isEmpty() ? null : faqCategory.getText().trim()
        );

        if (dao.ajouter(faq)) {
            succes("FAQ ajout e avec succ s !");
            clearForm();
            charger();
        } else {
            erreur("Erreur lors de l'ajout.");
        }
    }

    @FXML public void modifier() {
        FAQ sel = faqTable.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("S lectionne une FAQ   modifier."); return; }

        String question = faqQuestion.getText().trim();
        String answer = faqAnswer.getText().trim();

        if (question.isEmpty()) { erreur("La question est obligatoire."); return; }
        if (question.length() > 255) { erreur("Max 255 caract res."); return; }
        if (answer.isEmpty()) { erreur("La r ponse est obligatoire."); return; }
        if (dao.existeDejaQuestion(question, sel.getId())) {
            erreur("Cette question existe d j  pour une autre FAQ."); return;
        }

        sel.setQuestion(question);
        sel.setAnswer(answer);
        sel.setSubject(faqSubject.getText().trim().isEmpty() ? null : faqSubject.getText().trim());
        sel.setCategory(faqCategory.getText().trim().isEmpty() ? null : faqCategory.getText().trim());

        if (dao.modifier(sel)) {
            succes("FAQ modifi e !");
            clearForm();
            charger();
        } else {
            erreur("Erreur lors de la modification.");
        }
    }

    @FXML public void supprimer() {
        FAQ sel = faqTable.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("S lectionne une FAQ   supprimer."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la FAQ : \"" + sel.getQuestion() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                dao.supprimer(sel.getId());
                succes("FAQ supprim e.");
                clearForm();
                charger();
            }
        });
    }

    @FXML public void clearForm() {
        faqQuestion.clear();
        faqAnswer.clear();
        faqSubject.clear();
        faqCategory.clear();
        faqTable.getSelectionModel().clearSelection();
        msgLabel.setText("");
    }

    private void remplirForm(FAQ f) {
        faqQuestion.setText(f.getQuestion());
        faqAnswer.setText(f.getAnswer() != null ? f.getAnswer() : "");
        faqSubject.setText(f.getSubject() != null ? f.getSubject() : "");
        faqCategory.setText(f.getCategory() != null ? f.getCategory() : "");
    }

    private void succes(String msg) {
        msgLabel.setStyle("-fx-text-fill: #1D9E75; -fx-font-weight: bold;");
        msgLabel.setText(msg);
    }

    private void erreur(String msg) {
        msgLabel.setStyle("-fx-text-fill: #A32D2D; -fx-font-weight: bold;");
        msgLabel.setText(msg);
    }
}
