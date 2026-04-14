package org.example.controllers.frontoffice;


import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.entities.Notification;
import org.example.services.NotificationManager;

import java.util.List;

public class NotificationController {

    @FXML private VBox notificationsContainer;

    @FXML
    public void initialize() {
        loadNotifications();
    }

    public void loadNotifications() {
        notificationsContainer.getChildren().clear();
        List<Notification> notifications = NotificationManager.getInstance().getAll();

        if (notifications.isEmpty()) {
            Label empty = new Label("Aucune notification.");
            empty.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
            notificationsContainer.getChildren().add(empty);
            return;
        }

        for (Notification n : notifications) {
            notificationsContainer.getChildren().add(createNotificationCard(n));
        }
    }

    private HBox createNotificationCard(Notification n) {
        HBox card = new HBox(10);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: " + (n.isLue() ? "white" : "#eef2ff") + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + (n.isLue() ? "#e4e6eb" : "#3b5bdb") + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
        );

        // ICONE
        Label icone = new Label(n.getType().equals("like") ? "👍" : "💬");
        icone.setStyle("-fx-font-size: 20px;");

        // MESSAGE + DATE
        VBox info = new VBox(3);
        Label message = new Label(n.getMessage());
        message.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #1c1e21;" +
                        (n.isLue() ? "" : "-fx-font-weight: bold;")
        );
        message.setWrapText(true);

        Label date = new Label("🕐 " + n.getDate().toLocalTime()
                .toString().substring(0, 5) + " - " +
                n.getDate().toLocalDate().toString());
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: #65676b;");

        info.getChildren().addAll(message, date);

        // POINT ROUGE si non lue
        if (!n.isLue()) {
            Label point = new Label("●");
            point.setStyle("-fx-text-fill: #3b5bdb; -fx-font-size: 10px;");
            card.getChildren().addAll(icone, info, point);
        } else {
            card.getChildren().addAll(icone, info);
        }

        return card;
    }

    @FXML
    private void handleMarquerLues() {
        NotificationManager.getInstance().marquerToutesLues();
        loadNotifications();
    }
}