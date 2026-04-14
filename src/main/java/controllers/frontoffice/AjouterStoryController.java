package controllers.frontoffice;

import entities.Publication;
import services.ServicePublication;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.time.LocalDateTime;

public class AjouterStoryController {

    @FXML private TextField titreField;
    @FXML private TextArea contenuArea;
    @FXML private TextField imagePathField;
    @FXML private ImageView imagePreview;
    @FXML private Label titreError;
    @FXML private Label contenuError;

    private ServicePublication servicePublication = new ServicePublication();
    private String selectedImagePath = "";
    private PublicationController publicationController;

    public void setPublicationController(PublicationController controller) {
        this.publicationController = controller;
    }

    @FXML
    public void initialize() {
        titreField.textProperty().addListener((obs, old, val) -> validerTitre());
        contenuArea.textProperty().addListener((obs, old, val) -> validerContenu());
    }

    private boolean validerTitre() {
        String titre = titreField.getText().trim();
        if (titre.isEmpty()) {
            titreError.setText("⚠ Le titre est obligatoire.");
            titreField.getStyleClass().removeAll("field-valid");
            titreField.getStyleClass().add("field-error");
            return false;
        } else if (titre.length() < 3) {
            titreError.setText("⚠ Le titre doit contenir au moins 3 caractères.");
            titreField.getStyleClass().removeAll("field-valid");
            titreField.getStyleClass().add("field-error");
            return false;
        } else {
            titreError.setText("");
            titreField.getStyleClass().removeAll("field-error");
            titreField.getStyleClass().add("field-valid");
            return true;
        }
    }

    private boolean validerContenu() {
        String contenu = contenuArea.getText().trim();
        if (contenu.isEmpty()) {
            contenuError.setText("⚠ Le contenu est obligatoire.");
            contenuArea.getStyleClass().removeAll("field-valid");
            contenuArea.getStyleClass().add("field-error");
            return false;
        } else {
            contenuError.setText("");
            contenuArea.getStyleClass().removeAll("field-error");
            contenuArea.getStyleClass().add("field-valid");
            return true;
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
    private void handlePublier() {
        boolean titreOk = validerTitre();
        boolean contenuOk = validerContenu();
        if (!titreOk || !contenuOk) return;

        try {
            Publication p = new Publication();
            p.setTitrePub(titreField.getText().trim());
            p.setTypePub("story"); // ← type story
            p.setLienPub(selectedImagePath);
            p.setContenuPub(contenuArea.getText().trim());
            p.setDatePub(LocalDateTime.now());
            p.setLikes(0);
            p.setDislikes(0);
            p.setUtilisateurId(1);

            servicePublication.add(p);

            if (publicationController != null) {
                publicationController.loadStories();
            }

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Story ajoutée !");
            fermerFenetre();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() { fermerFenetre(); }

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