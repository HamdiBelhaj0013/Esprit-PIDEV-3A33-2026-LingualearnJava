package org.example.controllers.frontoffice;


import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.entities.Notification;
import org.example.services.NotificationManager;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnNotifications;
    @FXML private VBox notificationPopupContainer;

    @FXML
    public void initialize() {
        NotificationManager.getInstance().setOnNewNotification(() -> {
            updateBadge();
            // Afficher la dernière notification en popup
            Notification derniere = NotificationManager.getInstance().getAll().get(0);
            showNotificationPopup(derniere);
        });
        loadView("/frontoffice/fxml/PublicationView.fxml");
    }

    private void updateBadge() {
        int count = NotificationManager.getInstance().getNombreNonLues();
        btnNotifications.setText("🔔 Notifications (" + count + ")");
        if (count > 0) {
            btnNotifications.setStyle(
                    "-fx-background-color: #e74c3c;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 6;" +
                            "-fx-font-size: 13px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 8 15;"
            );
        } else {
            btnNotifications.setStyle(
                    "-fx-background-color: #3b5bdb;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 6;" +
                            "-fx-font-size: 13px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 8 15;"
            );
        }
    }

    private void showNotificationPopup(Notification n) {
        // Créer le popup
        HBox popup = new HBox(10);
        popup.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        popup.setPadding(new Insets(12, 20, 12, 20));
        popup.setMaxWidth(400);
        popup.setStyle(
                "-fx-background-color: #1c1e21;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);"
        );

        Label icone = new Label(n.getType().equals("like") ? "👍" : "💬");
        icone.setStyle("-fx-font-size: 18px;");

        Label message = new Label(n.getMessage());
        message.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: white;" +
                        "-fx-wrap-text: true;"
        );
        message.setMaxWidth(340);
        message.setWrapText(true);

        popup.getChildren().addAll(icone, message);

        // Ajouter au container
        notificationPopupContainer.getChildren().add(0, popup);

        // Opacité initiale à 0
        popup.setOpacity(0);

        // Animation apparition
        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(popup.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(300), new KeyValue(popup.opacityProperty(), 1))
        );

        // Animation disparition après 3 secondes
        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.millis(2700), new KeyValue(popup.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(3000), new KeyValue(popup.opacityProperty(), 0))
        );

        fadeOut.setOnFinished(e -> {
            notificationPopupContainer.getChildren().remove(popup);
        });

        fadeIn.play();
        fadeOut.play();
    }

    @FXML
    void showNotifications() {
        NotificationManager.getInstance().marquerToutesLues();
        updateBadge();
        loadView("/frontoffice/fxml/NotificationView.fxml");
    }

    @FXML
    void showBackoffice() {
        loadView("/backoffice/fxml/dashboard.fxml");
    }

    @FXML void showAccueil() { loadView("/frontoffice/fxml/PublicationView.fxml"); }
    @FXML void showDomaines() {}
    @FXML void showQuiz() {}
    @FXML void showInscrire() {}
    @FXML void showConnecter() {}
    @FXML void showRessources() { loadView("/frontoffice/fxml/PublicationView.fxml"); }

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

