package org.example.controller.user.supportmanagement;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entity.Reclamation;
import org.example.repository.supportmanagement.ReclamationDAO;
import org.example.repository.supportmanagement.SupportResponseDAO;
import org.example.service.supportManagment.GeminiService;
import org.example.service.supportManagment.PriorityDetector;
import org.example.util.MyDataBase;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReclamationUserDetailController implements Initializable {

    @FXML private Label labelSujet;
    @FXML private Label labelStatut;
    @FXML private Label labelPriorite;
    @FXML private Label labelDate;
    @FXML private Label labelMessage;

    @FXML private TextField        editSubject;
    @FXML private TextArea         editMessage;
    @FXML private Button           btnModifier;
    @FXML private Button           btnSupprimer;
    @FXML private Label            msgAction;

    @FXML private ListView<String> reponsesList;
    @FXML private ComboBox<String> langueBox;
    @FXML private Label            msgTraduction;
    @FXML private TextArea         traductionArea;

    @FXML private VBox   evalCard;
    @FXML private Button star1, star2, star3, star4, star5;
    @FXML private Label  labelNote;
    @FXML private Label  msgEval;

    private final ReclamationDAO     reclDao     = new ReclamationDAO();
    private final SupportResponseDAO responseDao = new SupportResponseDAO();

    private Reclamation reclamation;
    private Runnable    onClose;
    private int         selectedNote = 0;
    private List<String> reponsesOriginales;

    private static final String STAR_ON  = "-fx-font-size:28px; -fx-background-color:transparent; -fx-cursor:hand; -fx-text-fill:#f5a623; -fx-padding:0;";
    private static final String STAR_OFF = "-fx-font-size:28px; -fx-background-color:transparent; -fx-cursor:hand; -fx-text-fill:#ccc; -fx-padding:0;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        langueBox.setItems(FXCollections.observableArrayList(
                "Français", "English", "العربية", "Español", "Deutsch", "Italiano"));
        langueBox.setValue("Français");
    }

    public void setReclamation(Reclamation r, Runnable onCloseCallback) {
        this.reclamation = r;
        this.onClose     = onCloseCallback;
        rafraichir();
    }

    private void rafraichir() {
        labelSujet.setText(reclamation.getSubject());
        labelStatut.setText(reclamation.getStatus());
        labelPriorite.setText(reclamation.getPriority());
        labelDate.setText(reclamation.getSubmittedAt() != null
                ? reclamation.getSubmittedAt().toString().replace("T", " ").substring(0, 16) : "");
        labelMessage.setText(reclamation.getMessageBody());

        boolean isPending = "PENDING".equals(reclamation.getStatus());
        editSubject.setText(reclamation.getSubject());
        editMessage.setText(reclamation.getMessageBody());
        editSubject.setDisable(!isPending);
        editMessage.setDisable(!isPending);
        btnModifier.setDisable(!isPending);
        btnSupprimer.setDisable(!isPending);
        msgAction.setText(isPending ? "" : "Modification/Suppression uniquement si statut = PENDING");
        msgAction.setStyle("-fx-text-fill: gray;");

        chargerReponses();
        configurerEvaluation();
        traductionArea.setVisible(false);
        traductionArea.setManaged(false);
        msgTraduction.setText("");
    }

    // ── Traduction ────────────────────────────────────────────────────────
    @FXML public void traduireReponse() {
        if (reponsesOriginales == null || reponsesOriginales.isEmpty()) {
            msgTraduction.setStyle("-fx-text-fill: orange;");
            msgTraduction.setText("Aucune réponse à traduire."); return;
        }
        String langue = langueBox.getValue() == null ? "Français" : langueBox.getValue();
        msgTraduction.setStyle("-fx-text-fill: gray;");
        msgTraduction.setText("⏳ Traduction en cours...");
        traductionArea.setVisible(false);
        traductionArea.setManaged(false);

        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            for (String reponse : reponsesOriginales) {
                String msg = reponse.contains("] ") ? reponse.substring(reponse.indexOf("] ") + 2) : reponse;
                String traduit = GeminiService.traduire(msg, "auto", langue);
                sb.append(traduit != null ? traduit : "Erreur traduction").append("\n\n");
            }
            final String resultat = sb.toString().trim();
            Platform.runLater(() -> {
                traductionArea.setText(resultat);
                traductionArea.setVisible(true);
                traductionArea.setManaged(true);
                msgTraduction.setStyle("-fx-text-fill: green;");
                msgTraduction.setText("✅ Traduit en " + langue);
            });
        }).start();
    }

    @FXML public void afficherOriginal() {
        traductionArea.setVisible(false);
        traductionArea.setManaged(false);
        msgTraduction.setText("");
    }

    // ── Évaluation ────────────────────────────────────────────────────────
    private void configurerEvaluation() {
        boolean hasReponses = !responseDao.getByReclamationId(reclamation.getId()).isEmpty();
        boolean isResolved  = "RESOLVED".equals(reclamation.getStatus())
                || "CLOSED".equals(reclamation.getStatus());
        evalCard.setVisible(hasReponses && isResolved);
        evalCard.setManaged(hasReponses && isResolved);
        if (!evalCard.isVisible()) return;

        Integer noteExistante = chargerNoteExistante();
        if (noteExistante != null) {
            selectedNote = noteExistante;
            afficherEtoiles(noteExistante);
            labelNote.setText(noteExistante + "/5");
            msgEval.setStyle("-fx-text-fill: gray;");
            msgEval.setText("Vous avez déjà évalué cette réclamation.");
            desactiverEtoiles();
        } else {
            afficherEtoiles(0);
            labelNote.setText("");
            msgEval.setText("");
            activerEtoiles();
        }
    }

    @FXML public void noter1() { noterEtoiles(1); }
    @FXML public void noter2() { noterEtoiles(2); }
    @FXML public void noter3() { noterEtoiles(3); }
    @FXML public void noter4() { noterEtoiles(4); }
    @FXML public void noter5() { noterEtoiles(5); }

    private void noterEtoiles(int note) {
        afficherEtoiles(note);
        labelNote.setText(note + "/5");
        sauvegarderNote(note);
    }

    private void afficherEtoiles(int note) {
        Button[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < 5; i++)
            stars[i].setStyle(i < note ? STAR_ON : STAR_OFF);
    }

    private void desactiverEtoiles() {
        star1.setDisable(true); star2.setDisable(true); star3.setDisable(true);
        star4.setDisable(true); star5.setDisable(true);
    }

    private void activerEtoiles() {
        star1.setDisable(false); star2.setDisable(false); star3.setDisable(false);
        star4.setDisable(false); star5.setDisable(false);
    }

    private void sauvegarderNote(int note) {
        String sql = "UPDATE reclamation SET satisfaction_score=?, satisfaction_rated_at=? WHERE id=?";
        try (Connection conn = MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, note);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, reclamation.getId());
            ps.executeUpdate();
            msgEval.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            msgEval.setText("✅ Merci pour votre évaluation !");
            desactiverEtoiles();
            if (onClose != null) onClose.run();
        } catch (SQLException e) {
            msgEval.setStyle("-fx-text-fill: red;");
            msgEval.setText("Erreur: " + e.getMessage());
        }
    }

    private Integer chargerNoteExistante() {
        String sql = "SELECT satisfaction_score FROM reclamation WHERE id=? AND satisfaction_score IS NOT NULL";
        try (Connection conn = MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reclamation.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("satisfaction_score");
        } catch (SQLException e) {
            System.err.println("Erreur note: " + e.getMessage());
        }
        return null;
    }

    // ── Modifier ──────────────────────────────────────────────────────────
    @FXML public void modifier() {
        String sujet   = editSubject.getText().trim();
        String message = editMessage.getText().trim();

        if (sujet.isEmpty() || message.isEmpty()) {
            showMsg("Remplis tous les champs.", "red"); return;
        }

        // ✅ Priorité recalculée automatiquement
        String priorite = PriorityDetector.detect(sujet, message);
        reclamation.setSubject(sujet);
        reclamation.setMessageBody(message);
        reclamation.setPriority(priorite);

        boolean ok = reclDao.modifier(reclamation);
        if (ok) {
            showMsg("✅ Modifié. Priorité : " + priorite, "green");
            labelPriorite.setText(priorite);
            if (onClose != null) onClose.run();
        } else {
            showMsg("Erreur lors de la modification.", "red");
        }
    }

    @FXML public void supprimer() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + reclamation.getSubject() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = reclDao.supprimer(reclamation.getId());
                if (ok) { if (onClose != null) onClose.run(); fermer(); }
                else showMsg("Erreur suppression.", "red");
            }
        });
    }

    @FXML public void fermer() {
        ((Stage) labelSujet.getScene().getWindow()).close();
    }

    private void chargerReponses() {
        reponsesOriginales = responseDao.getByReclamationId(reclamation.getId()).stream()
                .map(r -> "[" + r.getRespondedAt() + "] " + r.getMessage())
                .collect(Collectors.toList());
        reponsesList.setItems(FXCollections.observableArrayList(
                reponsesOriginales.isEmpty()
                        ? List.of("Aucune réponse de l'administration.")
                        : reponsesOriginales));
    }

    private void showMsg(String txt, String color) {
        msgAction.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        msgAction.setText(txt);
    }
}