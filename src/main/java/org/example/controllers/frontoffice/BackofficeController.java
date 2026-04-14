package org.example.controllers.frontoffice;


import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.Commentaire;
import org.example.entities.Publication;
import org.example.services.ServiceCommentaire;
import org.example.services.ServicePublication;

import java.io.IOException;
import java.util.List;

public class BackofficeController {

    // PUBLICATIONS
    @FXML private TableView<Publication> publicationsTable;
    @FXML private TableColumn<Publication, Integer> colPubId;
    @FXML private TableColumn<Publication, String> colPubTitre;
    @FXML private TableColumn<Publication, String> colPubContenu;
    @FXML private TableColumn<Publication, String> colPubDate;
    @FXML private TableColumn<Publication, Integer> colPubLikes;
    @FXML private TableColumn<Publication, Integer> colPubDislikes;
    @FXML private TableColumn<Publication, String> colPubActions;
    @FXML private TextField searchPubField;
    @FXML private Label totalPubLabel;

    // COMMENTAIRES
    @FXML private TableView<Commentaire> commentairesTable;
    @FXML private TableColumn<Commentaire, Integer> colComId;
    @FXML private TableColumn<Commentaire, String> colComContenu;
    @FXML private TableColumn<Commentaire, String> colComDate;
    @FXML private TableColumn<Commentaire, Integer> colComPubId;
    @FXML private TableColumn<Commentaire, String> colComActions;
    @FXML private TextField searchComField;
    @FXML private Label totalComLabel;

    private ServicePublication servicePublication = new ServicePublication();
    private ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

    @FXML
    public void initialize() {
        setupPublicationsTable();
        setupCommentairesTable();
        loadPublications();
        loadCommentaires();
    }

    // ===== PUBLICATIONS =====

    private void setupPublicationsTable() {
        colPubId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        colPubTitre.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTitrePub()));

        colPubContenu.setCellValueFactory(data -> {
            String contenu = data.getValue().getContenuPub();
            return new SimpleStringProperty(
                    contenu.length() > 50 ? contenu.substring(0, 50) + "..." : contenu
            );
        });

        colPubDate.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDatePub().toLocalDate().toString()
                ));

        colPubLikes.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getLikes()).asObject());

        colPubDislikes.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getDislikes()).asObject());

        // COLONNE ACTIONS
        colPubActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Publication p = getTableView().getItems().get(getIndex());

                    Button btnModifier = new Button("✏️");
                    btnModifier.setStyle(
                            "-fx-background-color: #3498db;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-cursor: hand; -fx-padding: 4 8;"
                    );
                    btnModifier.setOnAction(e -> modifierPublication(p));

                    Button btnSupprimer = new Button("🗑");
                    btnSupprimer.setStyle(
                            "-fx-background-color: #e74c3c;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-cursor: hand; -fx-padding: 4 8;"
                    );
                    btnSupprimer.setOnAction(e -> supprimerPublication(p));

                    HBox actions = new HBox(5, btnModifier, btnSupprimer);
                    actions.setPadding(new Insets(2));
                    setGraphic(actions);
                }
            }
        });
    }

    private void loadPublications() {
        try {
            List<Publication> publications = servicePublication.getAll();
            publicationsTable.getItems().setAll(publications);
            totalPubLabel.setText("Total : " + publications.size() + " publications");
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    private void modifierPublication(Publication p) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ModifierPublicationView.fxml")
            );
            Parent root = loader.load();
            ModifierPublicationController controller = loader.getController();
            controller.setPublication(p, null);
            Stage stage = new Stage();
            stage.setTitle("Modifier la publication");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadPublications(); // Rafraîchir après modification
        } catch (IOException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    private void supprimerPublication(Publication p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la publication ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    servicePublication.delete(p.getId());
                    loadPublications();
                } catch (Exception e) {
                    System.out.println("Erreur : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleSearchPub() {
        String keyword = searchPubField.getText().trim();
        try {
            List<Publication> results = keyword.isEmpty()
                    ? servicePublication.getAll()
                    : servicePublication.search(keyword);
            publicationsTable.getItems().setAll(results);
            totalPubLabel.setText("Total : " + results.size() + " publications");
        } catch (Exception e) {
            System.out.println("Erreur recherche : " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshPub() {
        searchPubField.clear();
        loadPublications();
    }

    // ===== COMMENTAIRES =====

    private void setupCommentairesTable() {
        colComId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        colComContenu.setCellValueFactory(data -> {
            String contenu = data.getValue().getContenuC();
            return new SimpleStringProperty(
                    contenu.length() > 60 ? contenu.substring(0, 60) + "..." : contenu
            );
        });

        colComDate.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDateCom() != null
                                ? data.getValue().getDateCom().toLocalDate().toString()
                                : "N/A"
                ));

        colComPubId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getPublicationId()).asObject());

        // COLONNE ACTIONS
        colComActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Commentaire c = getTableView().getItems().get(getIndex());

                    Button btnModifier = new Button("✏️");
                    btnModifier.setStyle(
                            "-fx-background-color: #3498db;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-cursor: hand; -fx-padding: 4 8;"
                    );
                    btnModifier.setOnAction(e -> modifierCommentaire(c));

                    Button btnSupprimer = new Button("🗑");
                    btnSupprimer.setStyle(
                            "-fx-background-color: #e74c3c;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-cursor: hand; -fx-padding: 4 8;"
                    );
                    btnSupprimer.setOnAction(e -> supprimerCommentaire(c));

                    HBox actions = new HBox(5, btnModifier, btnSupprimer);
                    actions.setPadding(new Insets(2));
                    setGraphic(actions);
                }
            }
        });
    }

    private void loadCommentaires() {
        try {
            List<Commentaire> commentaires = serviceCommentaire.getAll();
            commentairesTable.getItems().setAll(commentaires);
            totalComLabel.setText("Total : " + commentaires.size() + " commentaires");
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    private void modifierCommentaire(Commentaire c) {
        TextInputDialog dialog = new TextInputDialog(c.getContenuC());
        dialog.setTitle("Modifier le commentaire");
        dialog.setHeaderText(null);
        dialog.setContentText("Nouveau contenu :");
        dialog.showAndWait().ifPresent(newContenu -> {
            if (!newContenu.trim().isEmpty()) {
                try {
                    c.setContenuC(newContenu.trim());
                    serviceCommentaire.update(c);
                    loadCommentaires();
                } catch (Exception e) {
                    System.out.println("Erreur : " + e.getMessage());
                }
            }
        });
    }

    private void supprimerCommentaire(Commentaire c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le commentaire ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceCommentaire.delete(c.getId());
                    loadCommentaires();
                } catch (Exception e) {
                    System.out.println("Erreur : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleSearchCom() {
        String keyword = searchComField.getText().trim();
        try {
            List<Commentaire> results = keyword.isEmpty()
                    ? serviceCommentaire.getAll()
                    : serviceCommentaire.search(keyword);
            commentairesTable.getItems().setAll(results);
            totalComLabel.setText("Total : " + results.size() + " commentaires");
        } catch (Exception e) {
            System.out.println("Erreur recherche : " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshCom() {
        searchComField.clear();
        loadCommentaires();
    }
}