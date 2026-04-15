package org.example.controllers.backoffice;

import org.example.entities.Publication;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.services.ServicePublication;

import java.io.File;
import java.time.LocalDateTime;

public class PublicationNewController {
    @FXML private TextField titreField;
    @FXML private TextField typeField;
    @FXML private TextField imagePathField;
    @FXML private ImageView imagePreview;
    @FXML private TextArea contenuField;
    @FXML private Label statusLabel;

    private final ServicePublication servicePublication = new ServicePublication();
    private String selectedImagePath = "";

    @FXML
    public void initialize() {
    }

    @FXML
    public void save() {
        if (titreField.getText() == null || titreField.getText().isBlank()) {
            setStatus("Le titre est obligatoire.", true);
            return;
        }
        if (contenuField.getText() == null || contenuField.getText().isBlank()) {
            setStatus("Le contenu est obligatoire.", true);
            return;
        }

        try {
            Publication p = new Publication();
            p.setTitrePub(titreField.getText().trim());
            p.setTypePub(typeField.getText() == null || typeField.getText().isBlank() ? "post" : typeField.getText().trim());
            p.setLienPub(selectedImagePath);
            p.setContenuPub(contenuField.getText());
            p.setDatePub(LocalDateTime.now());
            p.setUtilisateurId(1);
            servicePublication.add(p);
            closeWindow();
        } catch (Exception e) {
            showError("Ajout publication impossible", e.getMessage());
        }
    }

    @FXML
    public void cancel() {
        closeWindow();
    }

    @FXML
    public void handleChoisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(imagePathField.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            imagePathField.setText(file.getName());
            imagePreview.setImage(new Image(file.toURI().toString()));
            imagePreview.setVisible(true);
        }
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void setStatus(String message, boolean error) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #047857;");
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

