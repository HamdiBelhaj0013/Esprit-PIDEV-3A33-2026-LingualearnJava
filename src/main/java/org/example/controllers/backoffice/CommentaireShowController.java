package org.example.controllers.backoffice;

import org.example.entities.Commentaire;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CommentaireShowController {
    @FXML private Label idLabel;
    @FXML private Label publicationIdLabel;
    @FXML private Label dateLabel;
    @FXML private Label contenuLabel;

    public void setCommentaire(Commentaire c) {
        idLabel.setText(String.valueOf(c.getId()));
        publicationIdLabel.setText(String.valueOf(c.getPublicationId()));
        dateLabel.setText(c.getDateCom() != null ? c.getDateCom().toString() : "-");
        contenuLabel.setText(c.getContenuC() == null || c.getContenuC().isBlank() ? "-" : c.getContenuC());
    }

    @FXML
    public void close() {
        Stage stage = (Stage) idLabel.getScene().getWindow();
        stage.close();
    }
}

