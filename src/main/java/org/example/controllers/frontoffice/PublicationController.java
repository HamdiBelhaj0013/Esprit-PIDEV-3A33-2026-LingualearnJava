package org.example.controllers.frontoffice;

import org.example.entities.Publication;
import org.example.services.NotificationManager;
import org.example.services.ServicePublication;
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
    private boolean triCroissant = true;
    private int pageActuelle = 1;
    private final int PUBLICATIONS_PAR_PAGE = 5;
    private List<Publication> toutesPublications = new ArrayList<>();
    @FXML
    public void initialize() {
        loadStories();
        loadPublications();
    }

    public void loadPublications() {
        publicationsContainer.getChildren().clear();
        try {
            toutesPublications = servicePublication.getAll();
            if (triCroissant) {
                toutesPublications.sort((a, b) -> a.getDatePub().compareTo(b.getDatePub()));
            } else {
                toutesPublications.sort((a, b) -> b.getDatePub().compareTo(a.getDatePub()));
            }
            pageActuelle = 1;
            afficherPage();
        } catch (Exception e) {
            System.out.println("Erreur chargement : " + e.getMessage());
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
        // CARTE PRINCIPALE
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
        Label nomUser = new Label("Utilisateur");
        nomUser.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1c1e21;");
        Label date = new Label("📅 " + p.getDatePub().toLocalDate().toString());
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676b;");
        userInfo.getChildren().addAll(nomUser, date);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        // BOUTON MODIFIER
        Label modifier = new Label("✏️");
        modifier.setStyle("-fx-font-size: 16px; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 50;");
        modifier.setOnMouseEntered(e -> modifier.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980b9; -fx-cursor: hand; -fx-padding: 5; -fx-background-color: #eaf4fb; -fx-background-radius: 50;"));
        modifier.setOnMouseExited(e -> modifier.setStyle("-fx-font-size: 16px; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 50;"));
        modifier.setOnMouseClicked(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/frontoffice/fxml/ModifierPublicationView.fxml"));
                Parent root = loader.load();
                ModifierPublicationController controller = loader.getController();
                controller.setPublication(p, this);
                Stage stage = new Stage();
                stage.setTitle("Modifier la publication");
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.show();
            } catch (IOException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        });

        // BOUTON SUPPRIMER
        Label supprimer = new Label("🗑");
        supprimer.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 50;");
        supprimer.setOnMouseEntered(e -> supprimer.setStyle("-fx-font-size: 16px; -fx-text-fill: #c0392b; -fx-cursor: hand; -fx-padding: 5; -fx-background-color: #fdecea; -fx-background-radius: 50;"));
        supprimer.setOnMouseExited(e -> supprimer.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 50;"));
        supprimer.setOnMouseClicked(event -> {
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
                        System.out.println("Erreur suppression : " + e.getMessage());
                    }
                }
            });
        });

        header.getChildren().addAll(avatar, userInfo, headerSpacer, modifier, supprimer);

        // TITRE
        Label titre = new Label(p.getTitrePub());
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        titre.setWrapText(true);

        // CONTENU
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
                System.out.println("Image non trouvée : " + e.getMessage());
            }
        }

        // SEPARATEUR
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #e4e6eb; -fx-pref-height: 1;");

        // REACTIONS
        HBox reactions = new HBox(20);
        reactions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        reactions.setPadding(new Insets(5, 0, 0, 0));

// État du vote : 0 = rien, 1 = liked, -1 = disliked
        final int[] etatVote = {0};

// LIKES
        Label likes = new Label("👍 " + p.getLikes());
        likes.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");

// DISLIKES
        Label dislikes = new Label("👎 " + p.getDislikes());
        dislikes.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");

        likes.setOnMouseClicked(e -> {
            try {
                if (etatVote[0] == 1) {
                    // Déjà liké → annuler le like
                    servicePublication.unlikePublication(p.getId());
                    p.setLikes(p.getLikes() - 1);
                    likes.setText("👍 " + p.getLikes());
                    likes.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");
                    etatVote[0] = 0;
                } else {
                    // Liker
                    if (etatVote[0] == -1) {
                        // Annuler le dislike d'abord
                        servicePublication.undislikePublication(p.getId());
                        p.setDislikes(p.getDislikes() - 1);
                        dislikes.setText("👎 " + p.getDislikes());
                        dislikes.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");
                    }
                    servicePublication.like(p.getId());
                    // Après servicePublication.like(p.getId());
                    NotificationManager.getInstance().ajouterNotification(
                            "👍 Quelqu'un a aimé votre publication : \"" + p.getTitrePub() + "\"",
                            "like",
                            p.getId()
                    );
                    p.setLikes(p.getLikes() + 1);
                    likes.setText("👍 " + p.getLikes());
                    likes.setStyle("-fx-font-size: 13px; -fx-text-fill: #3b5bdb; -fx-cursor: hand; -fx-font-weight: bold;");
                    etatVote[0] = 1;
                }
            } catch (Exception ex) {
                System.out.println("Erreur like : " + ex.getMessage());
            }
        });

        dislikes.setOnMouseClicked(e -> {
            try {
                if (etatVote[0] == -1) {
                    // Déjà disliké → annuler le dislike
                    servicePublication.undislikePublication(p.getId());
                    p.setDislikes(p.getDislikes() - 1);
                    dislikes.setText("👎 " + p.getDislikes());
                    dislikes.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");
                    etatVote[0] = 0;
                } else {
                    // Disliker
                    if (etatVote[0] == 1) {
                        // Annuler le like d'abord
                        servicePublication.unlikePublication(p.getId());
                        p.setLikes(p.getLikes() - 1);
                        likes.setText("👍 " + p.getLikes());
                        likes.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");
                    }
                    servicePublication.dislike(p.getId());
                    p.setDislikes(p.getDislikes() + 1);
                    dislikes.setText("👎 " + p.getDislikes());
                    dislikes.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-font-weight: bold;");
                    etatVote[0] = -1;
                }
            } catch (Exception ex) {
                System.out.println("Erreur dislike : " + ex.getMessage());
            }
        });

        likes.setOnMouseEntered(e -> {
            if (etatVote[0] != 1)
                likes.setStyle("-fx-font-size: 13px; -fx-text-fill: #3b5bdb; -fx-cursor: hand;");
        });
        likes.setOnMouseExited(e -> {
            if (etatVote[0] != 1)
                likes.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");
        });
        dislikes.setOnMouseEntered(e -> {
            if (etatVote[0] != -1)
                dislikes.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c; -fx-cursor: hand;");
        });
        dislikes.setOnMouseExited(e -> {
            if (etatVote[0] != -1)
                dislikes.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");
        });
        // TOGGLE COMMENTAIRES
        Label toggleCommentaires = new Label("💬 Commenter");
        toggleCommentaires.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label partager = new Label("↗ Partager");
        partager.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-cursor: hand;");

        reactions.getChildren().addAll(likes, dislikes, toggleCommentaires, spacer, partager);

        // SECTION COMMENTAIRES
        CommentaireController commentaireController = new CommentaireController();
        commentaireController.init(p.getId(), new VBox());
        VBox commentairesSection = commentaireController.createCommentairesSection();

        toggleCommentaires.setOnMouseClicked(e -> {
            boolean visible = commentairesSection.isVisible();
            commentairesSection.setVisible(!visible);
            commentairesSection.setManaged(!visible);
            toggleCommentaires.setText(visible ? "💬 Commenter" : "💬 Masquer");
        });

        card.getChildren().addAll(separator, reactions, commentairesSection);

        return card;
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText();
        try {
            toutesPublications = keyword.isEmpty()
                    ? servicePublication.getAll()
                    : servicePublication.search(keyword);
            if (triCroissant) {
                toutesPublications.sort((a, b) -> a.getDatePub().compareTo(b.getDatePub()));
            } else {
                toutesPublications.sort((a, b) -> b.getDatePub().compareTo(a.getDatePub()));
            }
            pageActuelle = 1;
            afficherPage();
        } catch (Exception e) {
            System.out.println("Erreur recherche : " + e.getMessage());
        }
    }
    @FXML
    private void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/frontoffice/fxml/AjouterPublicationView.fxml")
            );
            Parent root = loader.load();
            AjouterPublicationController controller = loader.getController();
            controller.setPublicationController(this);
            Stage stage = new Stage();
            stage.setTitle("Ajouter une publication");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    public void loadStories() {
        storiesContainer.getChildren().clear();

        // Bouton ajouter story
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

        // Charger les stories
        try {
            List<Publication> stories = servicePublication.getAllStories();
            for (Publication s : stories) {
                storiesContainer.getChildren().add(createStoryCard(s));
            }
        } catch (Exception e) {
            System.out.println("Erreur stories : " + e.getMessage());
        }
    }

    private VBox createStoryCard(Publication s) {
        VBox card = new VBox(5);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPrefWidth(90);


        // HEADER (icônes)
        HBox header = new HBox(5);
        //header.setAlignment(javafx.geometry.Pos.TOP_RIGHT);



        card.setCursor(javafx.scene.Cursor.HAND);

        // CERCLE
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
            Label icon = new Label("📖");
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
        // Récupérer toutes les stories
        List<Publication> stories = new ArrayList<>();
        try {
            stories.addAll(servicePublication.getAllStories());
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }

        final int[] index = {stories.indexOf(stories.stream()
                .filter(st -> st.getId() == s.getId())
                .findFirst().orElse(s))};

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        // Container principal
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setPrefWidth(420);

        // Fonction pour afficher une story
        final List<Publication> finalStories = stories;
        Runnable[] afficherStory = {null};
        afficherStory[0] = () -> {
            root.getChildren().clear();
            Publication current = finalStories.get(index[0]);

            // HEADER avec flèches
            HBox header = new HBox(10);

            // ICONES
            Label modifier = new Label("✏️");
            modifier.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-cursor: hand;");
            modifier.setOnMouseClicked(e -> ouvrirModifierStory(current));

            Label supprimer = new Label("🗑");
            supprimer.setStyle("-fx-font-size: 18px; -fx-text-fill: red; -fx-cursor: hand;");
            supprimer.setOnMouseClicked(e -> {
                supprimerStory(current);
                stage.close();
            });


            header.setAlignment(javafx.geometry.Pos.CENTER);
            header.setPadding(new Insets(10));
            header.setStyle("-fx-background-color: #1a1a2e;");

            // FLECHE GAUCHE
            Label fleche_gauche = new Label("❮");
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

            // INDICATEUR (1/3)
            Label indicateur = new Label((index[0] + 1) + " / " + finalStories.size());
            indicateur.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa;");

            Region spacerL = new Region();
            Region spacerR = new Region();
            HBox.setHgrow(spacerL, Priority.ALWAYS);
            HBox.setHgrow(spacerR, Priority.ALWAYS);

            // FLECHE DROITE
            Label fleche_droite = new Label("❯");
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

            header.getChildren().addAll(fleche_gauche, modifier, supprimer, spacerL, indicateur, spacerR, fleche_droite);

            // CONTENU STORY
            VBox content = new VBox(12);
            content.setStyle("-fx-background-color: #1a1a2e;");
            content.setPadding(new Insets(0, 20, 20, 20));

            // IMAGE
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

            // TITRE
            Label titre = new Label(current.getTitrePub());
            titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
            titre.setWrapText(true);

            // CONTENU
            Label contenu = new Label(current.getContenuPub());
            contenu.setStyle("-fx-font-size: 13px; -fx-text-fill: #cccccc;");
            contenu.setWrapText(true);

            // SEPARATEUR
            Region sep = new Region();
            sep.setStyle("-fx-background-color: #444; -fx-pref-height: 1;");

            // REACTIONS
            final int[] etatVote = {0};

            HBox reactions = new HBox(20);
            reactions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            reactions.setPadding(new Insets(5, 0, 0, 0));

            Label likes = new Label("👍 " + current.getLikes());
            likes.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand;");

            Label dislikes = new Label("👎 " + current.getDislikes());
            dislikes.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand;");

            likes.setOnMouseClicked(e -> {
                try {
                    if (etatVote[0] == 1) {
                        servicePublication.unlikePublication(current.getId());
                        current.setLikes(current.getLikes() - 1);
                        likes.setText("👍 " + current.getLikes());
                        likes.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand;");
                        etatVote[0] = 0;
                    } else {
                        if (etatVote[0] == -1) {
                            servicePublication.undislikePublication(current.getId());
                            current.setDislikes(current.getDislikes() - 1);
                            dislikes.setText("👎 " + current.getDislikes());
                            dislikes.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand;");
                        }
                        servicePublication.like(current.getId());
                        current.setLikes(current.getLikes() + 1);
                        likes.setText("👍 " + current.getLikes());
                        likes.setStyle("-fx-font-size: 14px; -fx-text-fill: #3b5bdb; -fx-cursor: hand; -fx-font-weight: bold;");
                        etatVote[0] = 1;
                    }
                } catch (Exception ex) {
                    System.out.println("Erreur like : " + ex.getMessage());
                }
            });

            dislikes.setOnMouseClicked(e -> {
                try {
                    if (etatVote[0] == -1) {
                        servicePublication.undislikePublication(current.getId());
                        current.setDislikes(current.getDislikes() - 1);
                        dislikes.setText("👎 " + current.getDislikes());
                        dislikes.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand;");
                        etatVote[0] = 0;
                    } else {
                        if (etatVote[0] == 1) {
                            servicePublication.unlikePublication(current.getId());
                            current.setLikes(current.getLikes() - 1);
                            likes.setText("👍 " + current.getLikes());
                            likes.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand;");
                        }
                        servicePublication.dislike(current.getId());
                        current.setDislikes(current.getDislikes() + 1);
                        dislikes.setText("👎 " + current.getDislikes());
                        dislikes.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-font-weight: bold;");
                        etatVote[0] = -1;
                    }
                } catch (Exception ex) {
                    System.out.println("Erreur dislike : " + ex.getMessage());
                }
            });

            Label toggleComment = new Label("💬 Commenter");
            toggleComment.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaa; -fx-cursor: hand;");
            reactions.getChildren().addAll(likes, dislikes, toggleComment);

            // COMMENTAIRES
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
                toggleComment.setText(visible ? "💬 Commenter" : "💬 Masquer");
            });

            content.getChildren().addAll(titre, contenu, sep, reactions, commentairesSection);
            root.getChildren().addAll(header, content);

            stage.sizeToScene();
        };

        // Afficher la première story
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
                    getClass().getResource("/frontoffice/fxml/AjouterStoryView.fxml")
            );
            Parent root = loader.load();
            AjouterStoryController controller = loader.getController();
            controller.setPublicationController(this);
            Stage stage = new Stage();
            stage.setTitle("Ajouter une story");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    private void afficherPage() {
        publicationsContainer.getChildren().clear();

        int total = toutesPublications.size();
        int totalPages = (int) Math.ceil((double) total / PUBLICATIONS_PAR_PAGE);

        int debut = (pageActuelle - 1) * PUBLICATIONS_PAR_PAGE;
        int fin = Math.min(debut + PUBLICATIONS_PAR_PAGE, total);

        // Afficher les publications de la page
        for (int i = debut; i < fin; i++) {
            publicationsContainer.getChildren().add(createPublicationCard(toutesPublications.get(i)));
        }

        // PAGINATION
        if (totalPages > 1) {
            publicationsContainer.getChildren().add(createPagination(totalPages));
        }
    }

    private HBox createPagination(int totalPages) {
        HBox pagination = new HBox(8);
        pagination.setAlignment(javafx.geometry.Pos.CENTER);
        pagination.setPadding(new Insets(15, 0, 10, 0));

        // BOUTON PRECEDENT
        Label precedent = new Label("❮");
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

        // NUMEROS DE PAGES
        for (int i = 1; i <= totalPages; i++) {
            final int numPage = i;
            Label pageLbl = new Label(String.valueOf(i));
            if (i == pageActuelle) {
                pageLbl.setStyle(
                        "-fx-font-size: 14px;" +
                                "-fx-cursor: hand;" +
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

        // BOUTON SUIVANT
        Label suivant = new Label("❯");
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

        pagination.getChildren().addAll(precedent, suivant);

        // Réorganiser : précédent, pages, suivant
        pagination.getChildren().clear();
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
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la story ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    servicePublication.delete(s.getId());
                    loadStories(); // refresh
                } catch (Exception e) {
                    System.out.println("Erreur suppression story : " + e.getMessage());
                }
            }
        });
    }

    private void ouvrirModifierStory(Publication s) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/frontoffice/fxml/ModifierPublicationView.fxml")
            );
            Parent root = loader.load();

            ModifierPublicationController controller = loader.getController();
            controller.setPublication(s, this);

            Stage stage = new Stage();
            stage.setTitle("Modifier la story");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            System.out.println("Erreur : " + e.getMessage());
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