package org.example.controllers.frontoffice;


import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.Publication;
import org.example.services.ServicePublication;

import java.io.File;

public class ModifierPublicationController {

    @FXML private TextField titreField;
    @FXML private TextArea contenuArea;
    @FXML private TextField imagePathField;
    @FXML private ImageView imagePreview;

    private ServicePublication servicePublication = new ServicePublication();
    private PublicationController publicationController;
    private Publication publication;
    private String selectedImagePath = "";

    // Reçoit la publication à modifier et pré-remplit le formulaire
    public void setPublication(Publication p, PublicationController controller) {
        this.publication = p;
        this.publicationController = controller;

        // Pré-remplir les champs
        titreField.setText(p.getTitrePub());
        contenuArea.setText(p.getContenuPub());

        // Afficher l'image actuelle si elle existe
        if (p.getLienPub() != null && !p.getLienPub().isEmpty()) {
            selectedImagePath = p.getLienPub();
            File imgFile = new File(p.getLienPub());
            if (imgFile.exists()) {
                imagePathField.setText(imgFile.getName());
                imagePreview.setImage(new Image(imgFile.toURI().toString()));
                imagePreview.setVisible(true);
            }
        }
    }

    @FXML
    private void handleChoisirImage() {
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

    @FXML
    private void handleModifier() {
        String titre = titreField.getText().trim();
        String contenu = contenuArea.getText().trim();

        if (titre.isEmpty() || contenu.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Veuillez remplir le titre et le contenu.");
            return;
        }

        try {
            publication.setTitrePub(titre);
            publication.setContenuPub(contenu);

            if (!selectedImagePath.isEmpty()) {
                publication.setLienPub(selectedImagePath);
            }

            servicePublication.update(publication);

            if (publicationController != null) {
                publicationController.loadPublications();
                publicationController.loadStories(); // 🔥 FIX
            }

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Modification réussie !");
            fermerFenetre();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur DB", e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setContentText(message);
        alert.showAndWait();
    }
}