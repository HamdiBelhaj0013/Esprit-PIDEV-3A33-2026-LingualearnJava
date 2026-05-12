package org.example.controller.user.forum;

import org.example.entities.forum.Publication;
import org.example.entity.User;
import org.example.service.forum.NotificationManager;
import org.example.service.forum.ServicePublication;
import org.example.service.user_managment.UserService;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PublicationController {

    @FXML private TextField searchField;
    @FXML private VBox publicationsContainer;
    @FXML private Label triIcon;
    @FXML private HBox storiesContainer;
    private ServicePublication servicePublication = new ServicePublication();
    private UserService userService = new UserService();
    private boolean triCroissant = true;
    private int pageActuelle = 1;
    private final int PUBLICATIONS_PAR_PAGE = 5;
    private List<Publication> toutesPublications = new ArrayList<>();

    @FXML
    public void initialize() {
        loadStories();
        loadPublications();
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

            // Try getFullName() first
            String fullName = null;
            try { fullName = author.getFullName(); } catch (Exception ignored) {}

            if (fullName != null && !fullName.isBlank()) return fullName;

            // Fallback: first name + last name
            String first = "";
            String last  = "";
            try { first = author.getFirstName() != null ? author.getFirstName() : ""; } catch (Exception ignored) {}
            try { last  = author.getLastName()  != null ? author.getLastName()  : ""; } catch (Exception ignored) {}
            String combined = (first + " " + last).trim();
            if (!combined.isBlank()) return combined;

            // Fallback: email
            try {
                String email = author.getEmail();
                if (email != null && !email.isBlank()) return email;
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("[WARN] Failed to fetch author id=" + utilisateurId + ": " + e.getMessage());
        }
        return "Unknown Author";
    }

    public void loadPublications() {
        publicationsContainer.getChildren().clear();
        try {
            toutesPublications = servicePublication.getAll().stream()
                    .filter(p -> p.getTypePub() == null || !p.getTypePub().equalsIgnoreCase("story"))
                    .collect(java.util.stream.Collectors.toList());
            if (triCroissant) {
                toutesPublications.sort((a, b) -> a.getDatePub().compareTo(b.getDatePub()));
            } else {
                toutesPublications.sort((a, b) -> b.getDatePub().compareTo(a.getDatePub()));
            }
            pageActuelle = 1;
            afficherPage();
        } catch (Exception e) {
            System.out.println("Error loading: " + e.getMessage());
        }
    }

    @FXML
    private void handleTri() {
        triCroissant = !triCroissant;
        triIcon.setText(triCroissant ? "↑" : "↓");
        if (triCroissant) {
            toutesPublications.sort((a, b) -> a.getDatePub().compareTo(b.getDatePub()));
        } else {
            toutesPublications.sort((a, b) -> b.getDatePub().compareTo(a.getDatePub()));
        }
        pageActuelle = 1;
        afficherPage();
    }

    private VBox createPublicationCard(Publication p) {
        VBox card = new VBox(10);
        card.setMaxWidth(600);
        card.setPrefWidth(600);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e4e6eb;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );
        card.setPadding(new Insets(15));

        // HEADER
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label avatar = new Label("👤");
        avatar.setStyle(
                "-fx-background-color: #e4e6eb;" +
                        "-fx-background-radius: 50;" +
                        "-fx-padding: 8;" +
                        "-fx-font-size: 18px;"
        );

        VBox userInfo = new VBox(2);
        // ✅ FIXED: Fetch actual author name
        String authorName = getAuthorName(p.getUtilisateurId());
        Label nomUser = new Label(authorName);
        nomUser.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1c1e21;");
        Label date = new Label("📅 " + p.getDatePub().toLocalDate().toString());
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676b;");
        userInfo.getChildren().addAll(nomUser, date);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        // Get current user ID for permission checks
        int currentUserId = getCurrentUserId();
        boolean isOwner = currentUserId != -1 && p.getUtilisateurId() == currentUserId;

        System.out.println("[DEBUG][CARD] pub.id=" + p.getId()
                + "  pub.utilisateurId=" + p.getUtilisateurId()
                + "  session.userId=" + currentUserId
                + "  isOwner=" + isOwner);

        // ✅ FIXED: Only show edit/delete buttons if user owns the publication
        if (isOwner) {
            // MODIFY BUTTON
            Label modifier = new Label("✏️");
            modifier.setStyle("-fx-font-size: 16px; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 50;");
            modifier.setOnMouseEntered(e -> modifier.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980b9; -fx-cursor: hand; -fx-padding: 5; -fx-background-color: #eaf4fb; -fx-background-radius: 50;"));
            modifier.setOnMouseExited(e -> modifier.setStyle("-fx-font-size: 16px; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 50;"));
            modifier.setOnMouseClicked(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/forum/fxml/ModifierPublicationView.fxml"));
                    Parent root = loader.load();
                    ModifierPublicationController controller = loader.getController();
                    controller.setPublication(p, this);
                    Stage stage = new Stage();
                    stage.setTitle("Modify the publication");
                    stage.setScene(new Scene(root));
                    stage.initModality(Modality.APPLICATION_MODAL);
                    stage.show();
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            });

            // DELETE BUTTON
            Label supprimer = new Label("🗑️");
            supprimer.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 50;");
            supprimer.setOnMouseEntered(e -> supprimer.setStyle("-fx-font-size: 16px; -fx-text-fill: #c0392b; -fx-cursor: hand; -fx-padding: 5; -fx-background-color: #fdecea; -fx-background-radius: 50;"));
            supprimer.setOnMouseExited(e -> supprimer.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 50;"));
            supprimer.setOnMouseClicked(event -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm");
                confirm.setHeaderText("Delete this publication?");
                confirm.setContentText("This action is irreversible.");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            servicePublication.delete(p.getId());
                            loadPublications();
                        } catch (Exception e) {
                            System.out.println("Error deleting: " + e.getMessage());
                        }
                    }
                });
            });

            header.getChildren().addAll(avatar, userInfo, headerSpacer, modifier, supprimer);
        } else {
            header.getChildren().addAll(avatar, userInfo, headerSpacer);
        }

        // TITLE
        Label titre = new Label(p.getTitrePub());
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        titre.setWrapText(true);

        // CONTENT
        Label contenu = new Label(p.getContenuPub());
        contenu.setStyle("-fx-font-size: 14px; -fx-text-fill: #3e4042;");
        contenu.setWrapText(true);

        card.getChildren().addAll(header, titre, contenu);

        // IMAGE
        if (p.getLienPub() != null && !p.getLienPub().isEmpty()) {
            try {
                File imgFile = new File(p.getLienPub());
                if (imgFile.exists()) {
                    ImageView imageView = new ImageView(new Image(imgFile.toURI().toString()));
                    imageView.setFitWidth(570);
                    imageView.setPreserveRatio(true);
                    card.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.out.println("Image not found: " + e.getMessage());
            }
        }

        // SEPARATOR
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #e4e6eb; -fx-pref-height: 1;");

        // ── REACTIONS ──────────────────────────────────────────────
        final int[] etatVote = {0};

        final String LIKE_DEFAULT    = "-fx-font-size: 14px; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #f0f2f5;";
        final String LIKE_ACTIVE     = "-fx-font-size: 14px; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #3b5bdb; -fx-font-weight: bold;";
        final String LIKE_HOVER      = "-fx-font-size: 14px; -fx-text-fill: #3b5bdb; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #dde4ff;";
        final String DISLIKE_DEFAULT = "-fx-font-size: 14px; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #f0f2f5;";
        final String DISLIKE_ACTIVE  = "-fx-font-size: 14px; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #e74c3c; -fx-font-weight: bold;";
        final String DISLIKE_HOVER   = "-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #fdecea;";

        Label likes = new Label("👍  " + p.getLikes());
        likes.setStyle(LIKE_DEFAULT);

        Label dislikes = new Label("👎  " + p.getDislikes());
        dislikes.setStyle(DISLIKE_DEFAULT);

        int totalVotes = p.getLikes() + p.getDislikes();
        double ratioLikes = totalVotes > 0 ? (double) p.getLikes() / totalVotes : 0.5;

        javafx.scene.layout.StackPane ratioBar = buildRatioBar(ratioLikes, 200, 6);

        likes.setOnMouseClicked(e -> {
            try {
                if (etatVote[0] == 1) {
                    servicePublication.unlikePublication(p.getId());
                    p.setLikes(p.getLikes() - 1);
                    likes.setText("👍  " + p.getLikes());
                    likes.setStyle(LIKE_DEFAULT);
                    etatVote[0] = 0;
                } else {
                    if (etatVote[0] == -1) {
                        servicePublication.undislikePublication(p.getId());
                        p.setDislikes(p.getDislikes() - 1);
                        dislikes.setText("👎  " + p.getDislikes());
                        dislikes.setStyle(DISLIKE_DEFAULT);
                    }
                    servicePublication.like(p.getId());
                    p.setLikes(p.getLikes() + 1);
                    likes.setText("👍  " + p.getLikes());
                    likes.setStyle(LIKE_ACTIVE);
                    etatVote[0] = 1;
                    NotificationManager.getInstance().ajouterNotification(
                            "👍 Someone liked your publication: \"" + p.getTitrePub() + "\"",
                            "like", p.getId()
                    );
                }
                updateRatioBar(ratioBar, p.getLikes(), p.getDislikes());
            } catch (Exception ex) {
                System.out.println("Error like: " + ex.getMessage());
            }
        });

        dislikes.setOnMouseClicked(e -> {
            try {
                if (etatVote[0] == -1) {
                    servicePublication.undislikePublication(p.getId());
                    p.setDislikes(p.getDislikes() - 1);
                    dislikes.setText("👎  " + p.getDislikes());
                    dislikes.setStyle(DISLIKE_DEFAULT);
                    etatVote[0] = 0;
                } else {
                    if (etatVote[0] == 1) {
                        servicePublication.unlikePublication(p.getId());
                        p.setLikes(p.getLikes() - 1);
                        likes.setText("👍  " + p.getLikes());
                        likes.setStyle(LIKE_DEFAULT);
                    }
                    servicePublication.dislike(p.getId());
                    p.setDislikes(p.getDislikes() + 1);
                    dislikes.setText("👎  " + p.getDislikes());
                    dislikes.setStyle(DISLIKE_ACTIVE);
                    etatVote[0] = -1;
                    NotificationManager.getInstance().ajouterNotification(
                            "👎 Someone disliked your publication: \"" + p.getTitrePub() + "\"",
                            "dislike", p.getId()
                    );
                }
                updateRatioBar(ratioBar, p.getLikes(), p.getDislikes());
            } catch (Exception ex) {
                System.out.println("Error dislike: " + ex.getMessage());
            }
        });

        likes.setOnMouseEntered(e -> { if (etatVote[0] != 1)  likes.setStyle(LIKE_HOVER); });
        likes.setOnMouseExited(e  -> { if (etatVote[0] != 1)  likes.setStyle(LIKE_DEFAULT); });
        dislikes.setOnMouseEntered(e -> { if (etatVote[0] != -1) dislikes.setStyle(DISLIKE_HOVER); });
        dislikes.setOnMouseExited(e  -> { if (etatVote[0] != -1) dislikes.setStyle(DISLIKE_DEFAULT); });

        Label toggleCommentaires = new Label("💬 Comment");
        toggleCommentaires.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #f0f2f5;");
        toggleCommentaires.setOnMouseEntered(e -> toggleCommentaires.setStyle("-fx-font-size: 13px; -fx-text-fill: #1c1e21; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #e4e6eb;"));
        toggleCommentaires.setOnMouseExited(e  -> toggleCommentaires.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #f0f2f5;"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label partager = new Label("↗ Share");
        partager.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #f0f2f5;");
        partager.setOnMouseEntered(e -> partager.setStyle("-fx-font-size: 13px; -fx-text-fill: #1c1e21; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #e4e6eb;"));
        partager.setOnMouseExited(e  -> partager.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20; -fx-background-color: #f0f2f5;"));

        HBox reactions = new HBox(10);
        reactions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        reactions.setPadding(new Insets(6, 0, 0, 0));
        reactions.getChildren().addAll(likes, dislikes, toggleCommentaires, spacer, partager);

        CommentaireController commentaireController = new CommentaireController();
        commentaireController.init(p.getId(), new VBox());
        VBox commentairesSection = commentaireController.createCommentairesSection();

        toggleCommentaires.setOnMouseClicked(e -> {
            boolean visible = commentairesSection.isVisible();
            commentairesSection.setVisible(!visible);
            commentairesSection.setManaged(!visible);
            toggleCommentaires.setText(visible ? "💬 Comment" : "💬 Hide");
        });

        card.getChildren().addAll(separator, ratioBar, reactions, commentairesSection);

        return card;
    }

    private javafx.scene.layout.StackPane buildRatioBar(double ratioLikes, double width, double height) {
        javafx.scene.layout.StackPane bar = new javafx.scene.layout.StackPane();
        bar.setMaxWidth(width);
        bar.setPrefWidth(width);
        bar.setPrefHeight(height);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #fde8e8; -fx-background-radius: 6;");

        javafx.scene.layout.Region fill = new javafx.scene.layout.Region();
        fill.setPrefHeight(height);
        fill.setPrefWidth(width * ratioLikes);
        fill.setStyle("-fx-background-color: linear-gradient(to right, #40c057, #2f9e44); -fx-background-radius: 6;");
        fill.setId("ratio-fill");

        javafx.scene.layout.StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);
        bar.getChildren().add(fill);
        return bar;
    }

    private void updateRatioBar(javafx.scene.layout.StackPane bar, int newLikes, int newDislikes) {
        int total = newLikes + newDislikes;
        double ratio = total > 0 ? (double) newLikes / total : 0.5;
        double barWidth = bar.getPrefWidth();
        bar.getChildren().stream()
                .filter(n -> "ratio-fill".equals(n.getId()))
                .map(n -> (javafx.scene.layout.Region) n)
                .findFirst()
                .ifPresent(fill -> fill.setPrefWidth(barWidth * ratio));
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText();
        try {
            java.util.List<Publication> all = keyword.isEmpty()
                    ? servicePublication.getAll()
                    : servicePublication.search(keyword);
            toutesPublications = all.stream()
                    .filter(p -> p.getTypePub() == null || !p.getTypePub().equalsIgnoreCase("story"))
                    .collect(java.util.stream.Collectors.toList());
            if (triCroissant) {
                toutesPublications.sort((a, b) -> a.getDatePub().compareTo(b.getDatePub()));
            } else {
                toutesPublications.sort((a, b) -> b.getDatePub().compareTo(a.getDatePub()));
            }
            pageActuelle = 1;
            afficherPage();
        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
        }
    }

    @FXML
    private void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/user/forum/fxml/AjouterPublicationView.fxml")
            );
            Parent root = loader.load();
            AjouterPublicationController controller = loader.getController();
            controller.setPublicationController(this);
            Stage stage = new Stage();
            stage.setTitle("Add a publication");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void loadStories() {
        storiesContainer.getChildren().clear();

        VBox addStory = new VBox(5);
        addStory.setAlignment(javafx.geometry.Pos.CENTER);
        addStory.setPrefWidth(90);
        addStory.setPrefHeight(130);
        addStory.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #3b5bdb;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-style: dashed;" +
                        "-fx-border-radius: 50;" +
                        "-fx-background-radius: 50;" +
                        "-fx-cursor: hand;"
        );
        addStory.setPrefWidth(80);
        addStory.setPrefHeight(120);
        Label addIcon = new Label("+");
        addIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #3b5bdb;");
        Label addLabel = new Label("Story");
        addLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #3b5bdb;");
        addStory.getChildren().addAll(addIcon, addLabel);
        addStory.setOnMouseClicked(e -> ouvrirAjouterStory());
        storiesContainer.getChildren().add(addStory);

        try {
            List<Publication> stories = servicePublication.getAllStories();
            for (Publication s : stories) {
                storiesContainer.getChildren().add(createStoryCard(s));
            }
        } catch (Exception e) {
            System.out.println("Stories error: " + e.getMessage());
        }
    }

    private VBox createStoryCard(Publication s) {
        VBox card = new VBox(5);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPrefWidth(90);

        card.setCursor(javafx.scene.Cursor.HAND);

        javafx.scene.layout.StackPane cercle = new javafx.scene.layout.StackPane();
        cercle.setPrefWidth(75);
        cercle.setPrefHeight(75);
        cercle.setMinWidth(75);
        cercle.setMinHeight(75);
        cercle.setStyle(
                "-fx-background-radius: 50;" +
                        "-fx-border-radius: 50;" +
                        "-fx-border-color: #3b5bdb;" +
                        "-fx-border-width: 3;" +
                        "-fx-effect: dropshadow(gaussian, rgba(59,91,219,0.4), 8, 0, 0, 2);"
        );

        if (s.getLienPub() != null && !s.getLienPub().isEmpty()) {
            File imgFile = new File(s.getLienPub());
            if (imgFile.exists()) {
                ImageView img = new ImageView(new Image(imgFile.toURI().toString()));
                img.setFitWidth(69);
                img.setFitHeight(69);
                img.setPreserveRatio(false);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(34.5, 34.5, 34.5);
                img.setClip(clip);
                cercle.getChildren().add(img);
            }
        } else {
            Label icon = new Label("🌐");
            icon.setStyle("-fx-font-size: 28px;");
            cercle.setStyle(cercle.getStyle() +
                    "-fx-background-color: linear-gradient(to bottom, #3b5bdb, #9b5de5);"
            );
            cercle.getChildren().add(icon);
        }

        Label titre = new Label(s.getTitrePub());
        titre.setStyle("-fx-font-size: 10px; -fx-text-fill: #333; -fx-font-weight: bold;");
        titre.setWrapText(true);
        titre.setMaxWidth(85);
        titre.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(cercle, titre);
        card.setOnMouseClicked(e -> voirStory(s));

        return card;
    }

    private void voirStory(Publication s) {
        List<Publication> stories = new ArrayList<>();
        try {
            stories.addAll(servicePublication.getAllStories());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        final int[] index = {stories.indexOf(stories.stream()
                .filter(st -> st.getId() == s.getId())
                .findFirst().orElse(s))};

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setPrefWidth(420);

        final List<Publication> finalStories = stories;
        Runnable[] afficherStory = {null};
        afficherStory[0] = () -> {
            root.getChildren().clear();
            Publication current = finalStories.get(index[0]);

            int currentUserId = getCurrentUserId();
            boolean isOwner = currentUserId != -1 && current.getUtilisateurId() == currentUserId;

            HBox header = new HBox(10);
            header.setAlignment(javafx.geometry.Pos.CENTER);
            header.setPadding(new Insets(10));
            header.setStyle("-fx-background-color: #1a1a2e;");

            if (isOwner) {
                Label modifierStory = new Label("✏️");
                modifierStory.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-cursor: hand;");
                modifierStory.setOnMouseClicked(e -> ouvrirModifierStory(current));

                Label supprimerStory = new Label("🗑️");
                supprimerStory.setStyle("-fx-font-size: 18px; -fx-text-fill: red; -fx-cursor: hand;");
                supprimerStory.setOnMouseClicked(e -> {
                    supprimerStory(current);
                    stage.close();
                });

                header.getChildren().addAll(modifierStory, supprimerStory);
            }

            Label fleche_gauche = new Label("◮");
            fleche_gauche.setStyle(
                    "-fx-font-size: 24px;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 5 15;" +
                            "-fx-background-color: rgba(255,255,255,0.1);" +
                            "-fx-background-radius: 50;"
            );
            fleche_gauche.setVisible(index[0] > 0);
            fleche_gauche.setOnMouseClicked(e -> {
                if (index[0] > 0) {
                    index[0]--;
                    afficherStory[0].run();
                }
            });

            Label indicateur = new Label((index[0] + 1) + " / " + finalStories.size());
            indicateur.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa;");

            Region spacerL = new Region();
            Region spacerR = new Region();
            HBox.setHgrow(spacerL, Priority.ALWAYS);
            HBox.setHgrow(spacerR, Priority.ALWAYS);

            Label fleche_droite = new Label("◯");
            fleche_droite.setStyle(
                    "-fx-font-size: 24px;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 5 15;" +
                            "-fx-background-color: rgba(255,255,255,0.1);" +
                            "-fx-background-radius: 50;"
            );
            fleche_droite.setVisible(index[0] < finalStories.size() - 1);
            fleche_droite.setOnMouseClicked(e -> {
                if (index[0] < finalStories.size() - 1) {
                    index[0]++;
                    afficherStory[0].run();
                }
            });

            header.getChildren().addAll(fleche_gauche, spacerL, indicateur, spacerR, fleche_droite);

            VBox content = new VBox(12);
            content.setStyle("-fx-background-color: #1a1a2e;");
            content.setPadding(new Insets(0, 20, 20, 20));

            if (current.getLienPub() != null && !current.getLienPub().isEmpty()) {
                File imgFile = new File(current.getLienPub());
                if (imgFile.exists()) {
                    ImageView img = new ImageView(new Image(imgFile.toURI().toString()));
                    img.setFitWidth(380);
                    img.setFitHeight(280);
                    img.setPreserveRatio(true);
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(380, 280);
                    clip.setArcWidth(20);
                    clip.setArcHeight(20);
                    img.setClip(clip);
                    content.getChildren().add(img);
                }
            }

            Label titre = new Label(current.getTitrePub());
            titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
            titre.setWrapText(true);

            Label contenu = new Label(current.getContenuPub());
            contenu.setStyle("-fx-font-size: 13px; -fx-text-fill: #cccccc;");
            contenu.setWrapText(true);

            Region sep = new Region();
            sep.setStyle("-fx-background-color: #444; -fx-pref-height: 1;");

            final int[] etatVote = {0};

            final String S_LIKE_DEFAULT    = "-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: rgba(255,255,255,0.08);";
            final String S_LIKE_ACTIVE     = "-fx-font-size: 14px; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: #3b5bdb; -fx-font-weight: bold;";
            final String S_LIKE_HOVER      = "-fx-font-size: 14px; -fx-text-fill: #748ffc; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: rgba(59,91,219,0.18);";
            final String S_DISLIKE_DEFAULT = "-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: rgba(255,255,255,0.08);";
            final String S_DISLIKE_ACTIVE  = "-fx-font-size: 14px; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: #e74c3c; -fx-font-weight: bold;";
            final String S_DISLIKE_HOVER   = "-fx-font-size: 14px; -fx-text-fill: #ff6b6b; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: rgba(231,76,60,0.18);";

            HBox reactions = new HBox(10);
            reactions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            reactions.setPadding(new Insets(5, 0, 0, 0));

            Label likes = new Label("👍  " + current.getLikes());
            likes.setStyle(S_LIKE_DEFAULT);

            Label dislikes = new Label("👎  " + current.getDislikes());
            dislikes.setStyle(S_DISLIKE_DEFAULT);

            int stTotal = current.getLikes() + current.getDislikes();
            double stRatio = stTotal > 0 ? (double) current.getLikes() / stTotal : 0.5;
            javafx.scene.layout.StackPane storyRatioBar = buildRatioBar(stRatio, 180, 5);

            likes.setOnMouseClicked(e -> {
                try {
                    if (etatVote[0] == 1) {
                        servicePublication.unlikePublication(current.getId());
                        current.setLikes(current.getLikes() - 1);
                        likes.setText("👍  " + current.getLikes());
                        likes.setStyle(S_LIKE_DEFAULT);
                        etatVote[0] = 0;
                    } else {
                        if (etatVote[0] == -1) {
                            servicePublication.undislikePublication(current.getId());
                            current.setDislikes(current.getDislikes() - 1);
                            dislikes.setText("👎  " + current.getDislikes());
                            dislikes.setStyle(S_DISLIKE_DEFAULT);
                        }
                        servicePublication.like(current.getId());
                        current.setLikes(current.getLikes() + 1);
                        likes.setText("👍  " + current.getLikes());
                        likes.setStyle(S_LIKE_ACTIVE);
                        etatVote[0] = 1;
                        NotificationManager.getInstance().ajouterNotification(
                                "👍 Someone liked your story: \"" + current.getTitrePub() + "\"",
                                "like", current.getId()
                        );
                    }
                    updateRatioBar(storyRatioBar, current.getLikes(), current.getDislikes());
                } catch (Exception ex) {
                    System.out.println("Error like story: " + ex.getMessage());
                }
            });

            dislikes.setOnMouseClicked(e -> {
                try {
                    if (etatVote[0] == -1) {
                        servicePublication.undislikePublication(current.getId());
                        current.setDislikes(current.getDislikes() - 1);
                        dislikes.setText("👎  " + current.getDislikes());
                        dislikes.setStyle(S_DISLIKE_DEFAULT);
                        etatVote[0] = 0;
                    } else {
                        if (etatVote[0] == 1) {
                            servicePublication.unlikePublication(current.getId());
                            current.setLikes(current.getLikes() - 1);
                            likes.setText("👍  " + current.getLikes());
                            likes.setStyle(S_LIKE_DEFAULT);
                        }
                        servicePublication.dislike(current.getId());
                        current.setDislikes(current.getDislikes() + 1);
                        dislikes.setText("👎  " + current.getDislikes());
                        dislikes.setStyle(S_DISLIKE_ACTIVE);
                        etatVote[0] = -1;
                        NotificationManager.getInstance().ajouterNotification(
                                "👎 Someone disliked your story: \"" + current.getTitrePub() + "\"",
                                "dislike", current.getId()
                        );
                    }
                    updateRatioBar(storyRatioBar, current.getLikes(), current.getDislikes());
                } catch (Exception ex) {
                    System.out.println("Error dislike story: " + ex.getMessage());
                }
            });

            likes.setOnMouseEntered(e    -> { if (etatVote[0] != 1)  likes.setStyle(S_LIKE_HOVER); });
            likes.setOnMouseExited(e     -> { if (etatVote[0] != 1)  likes.setStyle(S_LIKE_DEFAULT); });
            dislikes.setOnMouseEntered(e -> { if (etatVote[0] != -1) dislikes.setStyle(S_DISLIKE_HOVER); });
            dislikes.setOnMouseExited(e  -> { if (etatVote[0] != -1) dislikes.setStyle(S_DISLIKE_DEFAULT); });

            Label toggleComment = new Label("💬 Comment");
            toggleComment.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: rgba(255,255,255,0.08);");
            toggleComment.setOnMouseEntered(e -> toggleComment.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: rgba(255,255,255,0.15);"));
            toggleComment.setOnMouseExited(e  -> toggleComment.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 20; -fx-background-color: rgba(255,255,255,0.08);"));
            reactions.getChildren().addAll(likes, dislikes, toggleComment);

            CommentaireController commentaireController = new CommentaireController();
            commentaireController.init(current.getId(), new VBox());
            VBox commentairesSection = commentaireController.createCommentairesSection();
            commentairesSection.setStyle(
                    "-fx-background-color: #2a2a3e;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 10;"
            );

            toggleComment.setOnMouseClicked(e -> {
                boolean visible = commentairesSection.isVisible();
                commentairesSection.setVisible(!visible);
                commentairesSection.setManaged(!visible);
                toggleComment.setText(visible ? "💬 Comment" : "💬 Hide");
            });

            content.getChildren().addAll(titre, contenu, sep, storyRatioBar, reactions, commentairesSection);
            root.getChildren().addAll(header, content);

            stage.sizeToScene();
        };

        afficherStory[0].run();

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #1a1a2e;");

        stage.setScene(new Scene(scrollPane, 420, 600));
        stage.show();
    }

    private void ouvrirAjouterStory() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/user/forum/fxml/AjouterStoryView.fxml")
            );
            Parent root = loader.load();
            AjouterStoryController controller = loader.getController();
            controller.setPublicationController(this);
            Stage stage = new Stage();
            stage.setTitle("Add a story");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void afficherPage() {
        publicationsContainer.getChildren().clear();

        int total = toutesPublications.size();
        int totalPages = (int) Math.ceil((double) total / PUBLICATIONS_PAR_PAGE);

        int debut = (pageActuelle - 1) * PUBLICATIONS_PAR_PAGE;
        int fin = Math.min(debut + PUBLICATIONS_PAR_PAGE, total);

        for (int i = debut; i < fin; i++) {
            publicationsContainer.getChildren().add(createPublicationCard(toutesPublications.get(i)));
        }

        if (totalPages > 1) {
            publicationsContainer.getChildren().add(createPagination(totalPages));
        }
    }

    private HBox createPagination(int totalPages) {
        HBox pagination = new HBox(8);
        pagination.setAlignment(javafx.geometry.Pos.CENTER);
        pagination.setPadding(new Insets(15, 0, 10, 0));

        Label precedent = new Label("◮");
        precedent.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 12;" +
                        "-fx-background-radius: 8;" +
                        "-fx-background-color: " + (pageActuelle > 1 ? "#3b5bdb" : "#e0e0e0") + ";" +
                        "-fx-text-fill: " + (pageActuelle > 1 ? "white" : "#999") + ";"
        );
        if (pageActuelle > 1) {
            precedent.setOnMouseClicked(e -> {
                pageActuelle--;
                afficherPage();
            });
        }

        Label suivant = new Label("◯");
        suivant.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 12;" +
                        "-fx-background-radius: 8;" +
                        "-fx-background-color: " + (pageActuelle < totalPages ? "#3b5bdb" : "#e0e0e0") + ";" +
                        "-fx-text-fill: " + (pageActuelle < totalPages ? "white" : "#999") + ";"
        );
        if (pageActuelle < totalPages) {
            suivant.setOnMouseClicked(e -> {
                pageActuelle++;
                afficherPage();
            });
        }

        pagination.getChildren().add(precedent);
        for (int i = 1; i <= totalPages; i++) {
            final int numPage = i;
            Label pageLbl = new Label(String.valueOf(i));
            if (i == pageActuelle) {
                pageLbl.setStyle(
                        "-fx-font-size: 14px;" +
                                "-fx-padding: 8 12;" +
                                "-fx-background-radius: 8;" +
                                "-fx-background-color: #3b5bdb;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;"
                );
            } else {
                pageLbl.setStyle(
                        "-fx-font-size: 14px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 8 12;" +
                                "-fx-background-radius: 8;" +
                                "-fx-background-color: white;" +
                                "-fx-text-fill: #3b5bdb;" +
                                "-fx-border-color: #3b5bdb;" +
                                "-fx-border-radius: 8;"
                );
                pageLbl.setOnMouseClicked(e -> {
                    pageActuelle = numPage;
                    afficherPage();
                });
            }
            pagination.getChildren().add(pageLbl);
        }
        pagination.getChildren().add(suivant);

        return pagination;
    }

    private void supprimerStory(Publication s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm");
        confirm.setHeaderText("Delete this story?");
        confirm.setContentText("This action is irreversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    servicePublication.delete(s.getId());
                    loadStories();
                } catch (Exception e) {
                    System.out.println("Error deleting story: " + e.getMessage());
                }
            }
        });
    }

    private void ouvrirModifierStory(Publication s) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/user/forum/fxml/ModifierPublicationView.fxml")
            );
            Parent root = loader.load();

            ModifierPublicationController controller = loader.getController();
            controller.setPublication(s, this);

            Stage stage = new Stage();
            stage.setTitle("Modify the story");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @FXML private void handleAccueil() {}
    @FXML private void handleDomaines() {}
    @FXML private void handleQuiz() {}
    @FXML private void handleInscrire() {}
    @FXML private void handleConnecter() {}
    @FXML private void handleRessources() {}
    @FXML private void handleInscrire2() {}
    @FXML private void handleNotifications() {}
}
