package org.example.controller.user.forum;

import org.example.entities.forum.Commentaire;
import org.example.entities.forum.Publication;
import org.example.service.forum.BadWordChecker;
import org.example.service.forum.ForumEmailService;
import org.example.service.forum.NotificationManager;
import org.example.service.forum.ServiceCommentaire;
import org.example.service.forum.ServicePublication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.List;

public class CommentaireController {

    private ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private ServicePublication servicePublication = new ServicePublication();
    private BadWordChecker badWordChecker = new BadWordChecker();
    private int publicationId;
    private String publicationTitre = "Publication inconnue";
    private VBox listeCommentaires = new VBox(6);

    public void init(int publicationId, VBox container) {
        this.publicationId = publicationId;
        this.listeCommentaires = container;
        // Try to get publication title for email context
        try {
            Publication pub = servicePublication.getById(publicationId);
            if (pub != null) publicationTitre = pub.getTitrePub();
        } catch (Exception ignored) {}
        loadCommentaires();
    }

    public void loadCommentaires() {
        listeCommentaires.getChildren().clear();
        try {
            List<Commentaire> commentaires = serviceCommentaire.getByPublicationId(publicationId);
            for (Commentaire c : commentaires) {
                listeCommentaires.getChildren().add(createCommentaireCard(c));
            }
        } catch (Exception e) {
            System.out.println("Erreur chargement commentaires : " + e.getMessage());
        }
    }

    public VBox createCommentairesSection() {
        VBox section = new VBox(8);
        section.setVisible(false);
        section.setManaged(false);
        section.setPadding(new Insets(10, 0, 0, 0));

        // Liste des commentaires
        listeCommentaires = new VBox(6);
        loadCommentaires();

        // Champ ajout commentaire
        HBox addBox = new HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);

        TextField commentField = new TextField();
        commentField.setPromptText("Ã‰crire un commentaire...");
        commentField.setStyle(
                "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-color: #e4e6eb;" +
                        "-fx-padding: 8 15;"
        );
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button envoyerBtn = new Button("Envoyer");
        envoyerBtn.setStyle(
                "-fx-background-color: #3b5bdb;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 15;"
        );

        envoyerBtn.setOnAction(e -> {
            String contenu = commentField.getText().trim();
            if (!contenu.isEmpty()) {
                // â”€â”€ Bad word check â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                if (badWordChecker.containsBadWords(contenu)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Commentaire inappropriÃ©");
                    alert.setHeaderText("âš ï¸ Votre commentaire a Ã©tÃ© bloquÃ©");
                    alert.setContentText("Votre commentaire contient des mots inappropriÃ©s et n'a pas Ã©tÃ© publiÃ©. Merci de respecter les rÃ¨gles de la communautÃ©.");
                    alert.showAndWait();
                    // Envoyer email Ã  l'admin
                    ForumEmailService.sendBadWordWarning(contenu, publicationTitre);
                    commentField.clear();
                    return;
                }
                // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                try {
                    // âœ… Sauvegarde dans la DB
                    Commentaire c = new Commentaire();
                    c.setContenuC(contenu);
                    c.setDateCom(LocalDateTime.now());
                    c.setPublicationId(publicationId);
                    serviceCommentaire.add(c);
                    NotificationManager.getInstance().ajouterNotification(
                            "ðŸ’¬ Nouveau commentaire sur : \"" + publicationTitre + "\"",
                            "commentaire",
                            publicationId
                    );

                    // RafraÃ®chir la liste
                    loadCommentaires();
                    commentField.clear();
                    System.out.println("âœ… Commentaire enregistrÃ© !");
                } catch (Exception ex) {
                    System.out.println("âŒ Erreur : " + ex.getMessage());
                }
            }
        });

        // Envoyer avec la touche Enter
        commentField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                envoyerBtn.fire();
            }
        });

        addBox.getChildren().addAll(commentField, envoyerBtn);
        section.getChildren().addAll(listeCommentaires, addBox);

        return section;
    }

    private HBox createCommentaireCard(Commentaire c) {
        HBox commentCard = new HBox(10);
        commentCard.setAlignment(Pos.CENTER_LEFT);
        commentCard.setStyle(
                "-fx-background-color: #f0f2f5;" +
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 8 12;"
        );

        Label avatar = new Label("ðŸ‘¤");
        avatar.setStyle("-fx-font-size: 14px;");

        VBox contentBox = new VBox(2);
        Label contenuLabel = new Label(c.getContenuC());
        contenuLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1c1e21;");
        contenuLabel.setWrapText(true);

        String dateText = c.getDateCom() != null
                ? c.getDateCom().toLocalDate().toString() : "";
        Label dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #65676b;");

        contentBox.getChildren().addAll(contenuLabel, dateLabel);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // BOUTON MODIFIER
        Label modifier = new Label("âœï¸");
        modifier.setStyle("-fx-font-size: 12px; -fx-text-fill: #3498db; -fx-cursor: hand;");
        modifier.setOnMouseClicked(e -> {
            TextInputDialog dialog = new TextInputDialog(c.getContenuC());
            dialog.setTitle("Modifier le commentaire");
            dialog.setHeaderText(null);
            dialog.setContentText("Nouveau contenu :");
            dialog.showAndWait().ifPresent(newContenu -> {
                if (!newContenu.trim().isEmpty()) {
                    try {
                        c.setContenuC(newContenu.trim());
                        serviceCommentaire.update(c);
                        contenuLabel.setText(newContenu.trim());
                        System.out.println("âœ… Commentaire modifiÃ© !");
                    } catch (Exception ex) {
                        System.out.println("âŒ Erreur modification : " + ex.getMessage());
                    }
                }
            });
        });

        // BOUTON SUPPRIMER
        Label supprimer = new Label("ðŸ—‘");
        supprimer.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-cursor: hand;");
        supprimer.setOnMouseClicked(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le commentaire ?");
            confirm.setContentText("Cette action est irrÃ©versible.");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        serviceCommentaire.delete(c.getId());
                        loadCommentaires();
                        System.out.println("âœ… Commentaire supprimÃ© !");
                    } catch (Exception ex) {
                        System.out.println("âŒ Erreur suppression : " + ex.getMessage());
                    }
                }
            });
        });

        commentCard.getChildren().addAll(avatar, contentBox, modifier, supprimer);
        return commentCard;
    }
}



