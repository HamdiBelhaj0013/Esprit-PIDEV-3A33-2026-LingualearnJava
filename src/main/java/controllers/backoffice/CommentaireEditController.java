package controllers.backoffice;

import entities.Commentaire;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.ServiceCommentaire;

public class CommentaireEditController {
    @FXML private TextField idField;
    @FXML private TextField publicationIdField;
    @FXML private TextArea contenuField;
    @FXML private Label statusLabel;

    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private Commentaire commentaire;

    public void setCommentaire(Commentaire commentaire) {
        this.commentaire = commentaire;
        idField.setText(String.valueOf(commentaire.getId()));
        publicationIdField.setText(String.valueOf(commentaire.getPublicationId()));
        contenuField.setText(commentaire.getContenuC());
    }

    @FXML
    public void save() {
        if (commentaire == null) {
            setStatus("Commentaire non charge.", true);
            return;
        }
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
            c.setId(commentaire.getId());
            c.setContenuC(contenuField.getText());
            c.setPublicationId(pubId);
            serviceCommentaire.update(c);
            closeWindow();
        } catch (Exception e) {
            showError("Modification commentaire impossible", e.getMessage());
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
        Stage stage = (Stage) idField.getScene().getWindow();
        stage.close();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

