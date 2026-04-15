
package org.example.controller;
import org.example.repository.FAQDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entity.FAQ;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminFAQController implements Initializable {

    @FXML private TextField faqQuestion;
    @FXML private TextArea faqAnswer;
    @FXML private TextField faqSubject;
    @FXML private TextField faqCategory;
    @FXML private TextField searchField;
    @FXML private TextField filterCategoryField;
    @FXML private ComboBox<String> faqSortBox;
    @FXML private Label msgLabel;

    @FXML private TableView<FAQ> faqTable;
    @FXML private TableColumn<FAQ, String> colQuestion;
    @FXML private TableColumn<FAQ, String> colCategory;
    @FXML private TableColumn<FAQ, String> colSubject;

    @FXML private Label statTotal;

    private final FAQDAO dao = new FAQDAO();
    private List<FAQ> faqCache = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        faqSortBox.setItems(FXCollections.observableArrayList("Date desc", "Date asc", "Question A-Z"));
        faqSortBox.setValue("Date desc");

        faqTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> { if (sel != null) remplirForm(sel); }
        );

        charger();
    }

    @FXML public void charger() {
        faqCache = dao.getAll();
        appliquerFiltresEtTri();
    }

    @FXML public void rechercher() {
        appliquerFiltresEtTri();
    }
    @FXML public void filtrerCategorie() { appliquerFiltresEtTri(); }
    @FXML public void trierFAQ() { appliquerFiltresEtTri(); }

    @FXML public void ajouter() {
        String question = faqQuestion.getText().trim();
        String answer = faqAnswer.getText().trim();
        String subject = faqSubject.getText().trim();
        String category = faqCategory.getText().trim();

        if (question.isEmpty()) {
            erreur("La question est obligatoire."); return;
        }
        if (question.length() < 5) {
            erreur("La question doit contenir au moins 5 caracteres."); return;
        }
        if (question.length() > 255) {
            erreur("La question ne doit pas depasser 255 caracteres."); return;
        }
        if (answer.isEmpty()) {
            erreur("La reponse est obligatoire."); return;
        }
        if (answer.length() < 5) {
            erreur("La reponse doit contenir au moins 5 caracteres."); return;
        }
        if (!subject.isEmpty() && subject.length() < 5) {
            erreur("Le sujet optionnel doit contenir au moins 5 caracteres."); return;
        }
        if (!category.isEmpty() && category.length() < 5) {
            erreur("La categorie optionnelle doit contenir au moins 5 caracteres."); return;
        }
        if (dao.existeDejaQuestion(question, -1)) {
            erreur("Cette question existe deja."); return;
        }

        FAQ faq = new FAQ(
                question,
                answer,
                subject.isEmpty() ? null : subject,
                category.isEmpty() ? null : category
        );

        if (dao.ajouter(faq)) {
            succes("FAQ ajoutee avec succes.");
            clearForm();
            charger();
        } else {
            erreur("Erreur lors de l'ajout.");
        }
    }

    @FXML public void modifier() {
        FAQ sel = faqTable.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Selectionne une FAQ a modifier."); return; }

        String question = faqQuestion.getText().trim();
        String answer = faqAnswer.getText().trim();
        String subject = faqSubject.getText().trim();
        String category = faqCategory.getText().trim();

        if (question.isEmpty()) { erreur("La question est obligatoire."); return; }
        if (question.length() < 5) { erreur("La question doit contenir au moins 5 caracteres."); return; }
        if (question.length() > 255) { erreur("Max 255 caracteres."); return; }
        if (answer.isEmpty()) { erreur("La reponse est obligatoire."); return; }
        if (answer.length() < 5) { erreur("La reponse doit contenir au moins 5 caracteres."); return; }
        if (!subject.isEmpty() && subject.length() < 5) {
            erreur("Le sujet optionnel doit contenir au moins 5 caracteres."); return;
        }
        if (!category.isEmpty() && category.length() < 5) {
            erreur("La categorie optionnelle doit contenir au moins 5 caracteres."); return;
        }
        if (dao.existeDejaQuestion(question, sel.getId())) {
            erreur("Cette question existe deja pour une autre FAQ."); return;
        }

        sel.setQuestion(question);
        sel.setAnswer(answer);
        sel.setSubject(subject.isEmpty() ? null : subject);
        sel.setCategory(category.isEmpty() ? null : category);

        if (dao.modifier(sel)) {
            succes("FAQ modifiee.");
            clearForm();
            charger();
        } else {
            erreur("Erreur lors de la modification.");
        }
    }

    @FXML public void supprimer() {
        FAQ sel = faqTable.getSelectionModel().getSelectedItem();
        if (sel == null) { erreur("Selectionne une FAQ a supprimer."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la FAQ : \"" + sel.getQuestion() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                dao.supprimer(sel.getId());
                succes("FAQ supprimee.");
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

    private void appliquerFiltresEtTri() {
        String mot = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String categorie = filterCategoryField.getText() == null ? "" : filterCategoryField.getText().trim().toLowerCase();
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
        statTotal.setText("Total : " + filtered.size() + " FAQ");
    }
}

