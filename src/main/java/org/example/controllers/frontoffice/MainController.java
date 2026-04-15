package org.example.controllers.frontoffice;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.entities.Notification;
import org.example.services.NotificationManager;

import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnNotifications;
    @FXML private VBox notificationPopupContainer;
    @FXML private VBox notificationDropdown;
    @FXML private VBox notificationDropdownList;

    private boolean dropdownOpen = false;

    @FXML
    public void initialize() {
        NotificationManager.getInstance().setOnNewNotification(() -> {
            updateBadge();
            Notification derniere = NotificationManager.getInstance().getAll().get(0);
            showNotificationPopup(derniere);
            // Si le panel est ouvert, rafraîchir la liste
            if (dropdownOpen) refreshDropdownList();
        });
        loadView("/frontoffice/fxml/PublicationView.fxml");
    }

    // ── Badge ──────────────────────────────────────────────────────────────
    private void updateBadge() {
        int count = NotificationManager.getInstance().getNombreNonLues();
        btnNotifications.setText("🔔 Notifications" + (count > 0 ? " (" + count + ")" : ""));
        if (count > 0) {
            btnNotifications.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8 15;"
            );
        } else {
            btnNotifications.setStyle(
                "-fx-background-color: #3b5bdb; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8 15;"
            );
        }
    }

    // ── Toggle dropdown Facebook-style ─────────────────────────────────────
    @FXML
    void toggleNotificationPanel() {
        if (dropdownOpen) {
            closeDropdown();
        } else {
            openDropdown();
        }
    }

    private void openDropdown() {
        refreshDropdownList();
        notificationDropdown.setVisible(true);
        notificationDropdown.setManaged(true);
        notificationDropdown.setOpacity(0);
        notificationDropdown.setScaleY(0.92);

        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(notificationDropdown.opacityProperty(), 0),
                new KeyValue(notificationDropdown.scaleYProperty(), 0.92)
            ),
            new KeyFrame(Duration.millis(180),
                new KeyValue(notificationDropdown.opacityProperty(), 1),
                new KeyValue(notificationDropdown.scaleYProperty(), 1.0)
            )
        );
        anim.play();
        dropdownOpen = true;

        // Fermer si on clique ailleurs
        javafx.application.Platform.runLater(() -> {
            if (contentArea.getScene() != null) {
                contentArea.getScene().setOnMousePressed(e -> {
                    if (!notificationDropdown.isHover() && !btnNotifications.isHover()) {
                        closeDropdown();
                        contentArea.getScene().setOnMousePressed(null);
                    }
                });
            }
        });
    }

    private void closeDropdown() {
        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(notificationDropdown.opacityProperty(), 1),
                new KeyValue(notificationDropdown.scaleYProperty(), 1.0)
            ),
            new KeyFrame(Duration.millis(150),
                new KeyValue(notificationDropdown.opacityProperty(), 0),
                new KeyValue(notificationDropdown.scaleYProperty(), 0.92)
            )
        );
        anim.setOnFinished(e -> {
            notificationDropdown.setVisible(false);
            notificationDropdown.setManaged(false);
        });
        anim.play();
        dropdownOpen = false;
    }

    private void refreshDropdownList() {
        notificationDropdownList.getChildren().clear();
        List<Notification> notifications = NotificationManager.getInstance().getAll();

        if (notifications.isEmpty()) {
            Label empty = new Label("Aucune notification pour l'instant.");
            empty.setStyle("-fx-text-fill: #65676b; -fx-font-size: 13px; -fx-padding: 16;");
            notificationDropdownList.getChildren().add(empty);
            return;
        }

        for (Notification n : notifications) {
            notificationDropdownList.getChildren().add(buildDropdownItem(n));
        }
        // Marquer comme lues et mettre à jour le badge
        NotificationManager.getInstance().marquerToutesLues();
        updateBadge();
    }

    private HBox buildDropdownItem(Notification n) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle(
            "-fx-background-color: " + (n.isLue() ? "transparent" : "#eef2ff") + ";" +
            "-fx-background-radius: 10; -fx-cursor: hand;"
        );

        // Hover effect
        item.setOnMouseEntered(e -> item.setStyle(
            "-fx-background-color: #f0f2f5; -fx-background-radius: 10; -fx-cursor: hand;"
        ));
        item.setOnMouseExited(e -> item.setStyle(
            "-fx-background-color: " + (n.isLue() ? "transparent" : "#eef2ff") + ";" +
            "-fx-background-radius: 10; -fx-cursor: hand;"
        ));

        // Icône dans un cercle
        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(42, 42);
        iconCircle.setMaxSize(42, 42);
        String iconBg = n.getType().equals("like")    ? "#dde4ff" :
                        n.getType().equals("dislike")  ? "#fde8e8" : "#e8f5e9";
        iconCircle.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 50;");

        Label icone = new Label(
            n.getType().equals("like")    ? "👍" :
            n.getType().equals("dislike") ? "👎" : "💬"
        );
        icone.setStyle("-fx-font-size: 18px;");
        iconCircle.getChildren().add(icone);

        // Texte
        VBox textBox = new VBox(3);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        Label message = new Label(n.getMessage());
        message.setStyle(
            "-fx-font-size: 13px; -fx-text-fill: #1c1e21; -fx-wrap-text: true;" +
            (n.isLue() ? "" : "-fx-font-weight: bold;")
        );
        message.setWrapText(true);
        message.setMaxWidth(240);

        Label dateLabel = new Label(
            n.getDate().toLocalDate().toString() + " à " +
            n.getDate().toLocalTime().toString().substring(0, 5)
        );
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #3b5bdb;");

        textBox.getChildren().addAll(message, dateLabel);

        // Point bleu si non lue
        VBox dotBox = new VBox();
        dotBox.setAlignment(Pos.CENTER);
        if (!n.isLue()) {
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #3b5bdb; -fx-font-size: 9px;");
            dotBox.getChildren().add(dot);
        }

        item.getChildren().addAll(iconCircle, textBox, dotBox);
        return item;
    }

    // ── Toast popup (coin haut-droit) ──────────────────────────────────────
    private void showNotificationPopup(Notification n) {
        HBox popup = new HBox(10);
        popup.setAlignment(Pos.CENTER_LEFT);
        popup.setPadding(new Insets(12, 20, 12, 20));
        popup.setMaxWidth(360);
        popup.setStyle(
            "-fx-background-color: #1c1e21; -fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);"
        );

        Label icone = new Label(
            n.getType().equals("like")    ? "👍" :
            n.getType().equals("dislike") ? "👎" : "💬"
        );
        icone.setStyle("-fx-font-size: 18px;");

        Label message = new Label(n.getMessage());
        message.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-wrap-text: true;");
        message.setMaxWidth(300);
        message.setWrapText(true);

        popup.getChildren().addAll(icone, message);
        notificationPopupContainer.getChildren().add(0, popup);
        popup.setOpacity(0);

        Timeline fadeIn = new Timeline(
            new KeyFrame(Duration.ZERO,       new KeyValue(popup.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(300), new KeyValue(popup.opacityProperty(), 1))
        );
        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.millis(2700), new KeyValue(popup.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(3000), new KeyValue(popup.opacityProperty(), 0))
        );
        fadeOut.setOnFinished(e -> notificationPopupContainer.getChildren().remove(popup));
        fadeIn.play();
        fadeOut.play();
    }

    // ── Marquer toutes lues depuis le header du dropdown ──────────────────
    @FXML
    void handleMarquerLues() {
        NotificationManager.getInstance().marquerToutesLues();
        updateBadge();
        refreshDropdownList();
    }

    // ── Navigation ─────────────────────────────────────────────────────────
    @FXML void showAccueil()    { closeDropdown(); loadView("/frontoffice/fxml/PublicationView.fxml"); }
    @FXML void showRessources() { closeDropdown(); loadView("/frontoffice/fxml/PublicationView.fxml"); }
    @FXML void showInscrire()   { closeDropdown(); }
    @FXML void showConnecter()  { closeDropdown(); }
    @FXML void showDomaines()   {}
    @FXML void showQuiz()       {}

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.out.println("Erreur chargement vue : " + e.getMessage());
        }
    }
}

