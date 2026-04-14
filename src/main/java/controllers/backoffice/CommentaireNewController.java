package controllers.backoffice;

import entities.Commentaire;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.ServiceCommentaire;

import java.time.LocalDateTime;

public class CommentaireNewController {
    @FXML private TextField publicationIdField;
    @FXML private TextArea contenuField;
    @FXML private Label statusLabel;

    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

    @FXML
    public void save() {
        if (contenuField.getText() == null || contenuField.getText().isBlank()) {
            setStatus("Le contenu est obligatoire.", true);
            return;
        }
        int pubId;
        try {
            pubId = Integer.parseInt(publicationIdField.getText());
        } catch (Exception e) {
            setStatus("Publication ID invalide.", true);
            return;
        }

        try {
            Commentaire c = new Commentaire();
            c.setContenuC(contenuField.getText());
            c.setDateCom(LocalDateTime.now());
            c.setPublicationId(pubId);
            serviceCommentaire.add(c);
            closeWindow();
        } catch (Exception e) {
            showError("Ajout commentaire impossible", e.getMessage());
        }
    }

    @FXML
    public void cancel() {
        closeWindow();
    }

    private void setStatus(String message, boolean error) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #047857;");
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) publicationIdField.getScene().getWindow();
        stage.close();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

