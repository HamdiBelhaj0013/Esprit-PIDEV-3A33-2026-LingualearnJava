package controllers.backoffice;

import entities.Publication;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class PublicationShowController {
    @FXML private Label idLabel;
    @FXML private Label titreLabel;
    @FXML private Label typeLabel;
    @FXML private Label likesLabel;
    @FXML private Label dislikesLabel;
    @FXML private Label userIdLabel;
    @FXML private Label dateLabel;
    @FXML private Label lienLabel;
    @FXML private Label contenuLabel;

    public void setPublication(Publication p) {
        idLabel.setText(String.valueOf(p.getId()));
        titreLabel.setText(defaultText(p.getTitrePub()));
        typeLabel.setText(defaultText(p.getTypePub()));
        likesLabel.setText(String.valueOf(p.getLikes()));
        dislikesLabel.setText(String.valueOf(p.getDislikes()));
        userIdLabel.setText(String.valueOf(p.getUtilisateurId()));
        dateLabel.setText(p.getDatePub() != null ? p.getDatePub().toString() : "-");
        lienLabel.setText(defaultText(p.getLienPub()));
        contenuLabel.setText(defaultText(p.getContenuPub()));
    }

    @FXML
    public void close() {
        Stage stage = (Stage) idLabel.getScene().getWindow();
        stage.close();
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

