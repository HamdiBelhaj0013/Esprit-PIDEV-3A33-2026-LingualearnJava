package org.example.controller.user.forum;

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
import org.example.entities.forum.Commentaire;
import org.example.entities.forum.Publication;
import org.example.entity.User;
import org.example.service.forum.ServiceCommentaire;
import org.example.service.forum.ServicePublication;
import org.example.service.user_managment.UserService;
import org.example.util.SessionManager;

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
    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        setupPublicationsTable();
        setupCommentairesTable();
        loadPublications();
        loadCommentaires();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Get current logged-in user ID, or -1 if none
    // ─────────────────────────────────────────────────────────────────────────
    private int getCurrentUserId() {
        try {
            if (SessionManager.getCurrentUser() != null
                    && SessionManager.getCurrentUser().getId() != null) {
                int id = SessionManager.getCurrentUser().getId().intValue();
                System.out.println("[DEBUG] User logged in, ID = " + id);
                return id;
            }
        } catch (Exception e) {
            System.err.println("[WARN] Unable to read session ID: " + e.getMessage());
        }
        System.out.println("[DEBUG] No user in session → currentUserId = -1");
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Get author name by user ID
    // ─────────────────────────────────────────────────────────────────────────
    private String getAuthorName(int utilisateurId) {
        if (utilisateurId <= 0) {
            return "Unknown Author";
        }
        try {
            User author = userService.findById((long) utilisateurId).orElse(null);
            if (author == null) return "Unknown Author";

            String fullName = null;
            try { fullName = author.getFullName(); } catch (Exception ignored) {}

            if (fullName != null && !fullName.isBlank()) return fullName;

            String first = "";
            String last  = "";
            try { first = author.getFirstName() != null ? author.getFirstName() : ""; } catch (Exception ignored) {}
            try { last  = author.getLastName()  != null ? author.getLastName()  : ""; } catch (Exception ignored) {}
            String combined = (first + " " + last).trim();
            if (!combined.isBlank()) return combined;

            try {
                String email = author.getEmail();
                if (email != null && !email.isBlank()) return email;
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("[WARN] Failed to fetch author id=" + utilisateurId + ": " + e.getMessage());
        }
        return "Unknown Author";
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
                    contenu != null && contenu.length() > 50
                            ? contenu.substring(0, 50) + "..." : contenu);
        });

        colPubDate.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDatePub() != null
                                ? data.getValue().getDatePub().toLocalDate().toString()
                                : "N/A"));

        colPubLikes.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getLikes()).asObject());

        colPubDislikes.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getDislikes()).asObject());

        // ── Actions Column ──────────────────────────────────────────────────
        colPubActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }

                Publication p = getTableView().getItems().get(getIndex());
                int currentUserId = getCurrentUserId();

                System.out.println("[DEBUG][PUB] pub.utilisateurId=" + p.getUtilisateurId()
                        + "  session.userId=" + currentUserId);

                // ✅ FIXED: User can only edit/delete OWN publications
                boolean isOwner = currentUserId != -1
                        && p.getUtilisateurId() == currentUserId;

                HBox actions = new HBox(5);
                if (isOwner) {
                    Button btnModifier = new Button("✏️");
                    btnModifier.setStyle(
                            "-fx-background-color: #3498db;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-cursor: hand; -fx-padding: 4 8;");
                    btnModifier.setOnAction(e -> modifierPublication(p));

                    Button btnSupprimer = new Button("🗑️");
                    btnSupprimer.setStyle(
                            "-fx-background-color: #e74c3c;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-cursor: hand; -fx-padding: 4 8;");
                    btnSupprimer.setOnAction(e -> supprimerPublication(p));

                    actions.getChildren().addAll(btnModifier, btnSupprimer);
                }
                setGraphic(actions);
            }
        });
    }

    private void loadPublications() {
        try {
            List<Publication> publications = servicePublication.getAll();
            publicationsTable.getItems().setAll(publications);
            totalPubLabel.setText("Total: " + publications.size() + " publications");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void modifierPublication(Publication p) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/user/forum/fxml/ModifierPublicationView.fxml"));
            Parent root = loader.load();
            ModifierPublicationController controller = loader.getController();
            controller.setPublication(p, null);
            Stage stage = new Stage();
            stage.setTitle("Modify the publication");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadPublications();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void supprimerPublication(Publication p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Delete the publication?");
        confirm.setContentText("This action is irreversible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    servicePublication.delete(p.getId());
                    loadPublications();
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
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
            totalPubLabel.setText("Total: " + results.size() + " publications");
        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
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
                    contenu != null && contenu.length() > 60
                            ? contenu.substring(0, 60) + "..." : contenu);
        });

        colComDate.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDateCom() != null
                                ? data.getValue().getDateCom().toLocalDate().toString()
                                : "N/A"));

        colComPubId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getPublicationId()).asObject());

        // ── Actions Column ──────────────────────────────────────────────────
        colComActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }

                Commentaire c = getTableView().getItems().get(getIndex());
                int currentUserId = getCurrentUserId();

                System.out.println("[DEBUG][COM] com.utilisateurId=" + c.getUtilisateurId()
                        + "  session.userId=" + currentUserId);

                // ✅ FIXED: User can only edit/delete OWN comments
                boolean isOwner = currentUserId != -1
                        && c.getUtilisateurId() == currentUserId;

                HBox actions = new HBox(5);
                if (isOwner) {
                    Button btnModifier = new Button("✏️");
                    btnModifier.setStyle(
                            "-fx-background-color: #3498db;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-cursor: hand; -fx-padding: 4 8;");
                    btnModifier.setOnAction(e -> modifierCommentaire(c));

                    Button btnSupprimer = new Button("🗑️");
                    btnSupprimer.setStyle(
                            "-fx-background-color: #e74c3c;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-cursor: hand; -fx-padding: 4 8;");
                    btnSupprimer.setOnAction(e -> supprimerCommentaire(c));

                    actions.getChildren().addAll(btnModifier, btnSupprimer);
                }
                setGraphic(actions);
            }
        });
    }

    private void loadCommentaires() {
        try {
            List<Commentaire> commentaires = serviceCommentaire.getAll();
            commentairesTable.getItems().setAll(commentaires);
            totalComLabel.setText("Total: " + commentaires.size() + " comments");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void modifierCommentaire(Commentaire c) {
        TextInputDialog dialog = new TextInputDialog(c.getContenuC());
        dialog.setTitle("Modify the comment");
        dialog.setHeaderText(null);
        dialog.setContentText("New content:");
        dialog.showAndWait().ifPresent(newContenu -> {
            if (!newContenu.trim().isEmpty()) {
                try {
                    c.setContenuC(newContenu.trim());
                    serviceCommentaire.update(c);
                    loadCommentaires();
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        });
    }

    private void supprimerCommentaire(Commentaire c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Delete the comment?");
        confirm.setContentText("This action is irreversible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceCommentaire.delete(c.getId());
                    loadCommentaires();
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
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
            totalComLabel.setText("Total: " + results.size() + " comments");
        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshCom() {
        searchComField.clear();
        loadCommentaires();
    }
}
