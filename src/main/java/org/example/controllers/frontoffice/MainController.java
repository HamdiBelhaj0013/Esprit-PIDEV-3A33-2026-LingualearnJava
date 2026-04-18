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
import javafx.scene.layout.HBox;
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
    @FXML private StackPane chatbotContainer;

    private boolean dropdownOpen = false;
    private Timeline closeAnim = null;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> sceneClickHandler;

    @FXML
    public void initialize() {
        // Nouvelle notification → toast + rafraîchissement si panel ouvert
        NotificationManager.getInstance().setOnNewNotification(() -> {
            updateBadge();
            List<Notification> all = NotificationManager.getInstance().getAll();
            if (!all.isEmpty()) showNotificationPopup(all.get(0));
            if (dropdownOpen) populateList();
        });

        // Mise à jour badge uniquement (après marquerToutesLues)
        NotificationManager.getInstance().setOnBadgeUpdate(this::updateBadge);

        // Init sceneClickHandler as a stable field reference (fixes remove bug)
        sceneClickHandler = this::onSceneClick;

        // Init chatbot widget
        ChatbotController chatbot = new ChatbotController();
        chatbotContainer.getChildren().add(chatbot.buildWidget());

        loadView("/frontoffice/fxml/PublicationView.fxml");
    }

    // ── Badge ──────────────────────────────────────────────────────────────
    private void updateBadge() {
        int count = NotificationManager.getInstance().getNombreNonLues();
        btnNotifications.setText("🔔 Notifications" + (count > 0 ? " (" + count + ")" : ""));
        btnNotifications.setStyle(count > 0
            ? "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8 15;"
            : "-fx-background-color: #3b5bdb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8 15;"
        );
    }

    // ── Toggle dropdown ────────────────────────────────────────────────────
    @FXML
    void toggleNotificationPanel() {
        if (dropdownOpen) {
            closeDropdown();
        } else {
            openDropdown();
        }
    }

    private void openDropdown() {
        // Annuler une éventuelle animation de fermeture en cours
        if (closeAnim != null) { closeAnim.stop(); closeAnim = null; }

        populateList();
        notificationDropdown.setVisible(true);
        notificationDropdown.setManaged(true);
        notificationDropdown.setOpacity(0);
        notificationDropdown.setScaleY(0.93);
        notificationDropdown.setTranslateY(-6);

        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(notificationDropdown.opacityProperty(), 0),
                new KeyValue(notificationDropdown.scaleYProperty(), 0.93),
                new KeyValue(notificationDropdown.translateYProperty(), -6)
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(notificationDropdown.opacityProperty(), 1),
                new KeyValue(notificationDropdown.scaleYProperty(), 1.0),
                new KeyValue(notificationDropdown.translateYProperty(), 0)
            )
        );
        anim.play();
        dropdownOpen = true;

        // Fermer si clic en dehors — enregistré une seule fois
        javafx.application.Platform.runLater(() -> {
            if (contentArea.getScene() != null) {
                contentArea.getScene().addEventFilter(
                    javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                    sceneClickHandler
                );
            }
        });
    }

    private void onSceneClick(javafx.scene.input.MouseEvent e) {
        // Ignorer si le clic est sur le dropdown ou le bouton
        if (notificationDropdown.isHover() || btnNotifications.isHover()) return;
        closeDropdown();
        if (contentArea.getScene() != null) {
            contentArea.getScene().removeEventFilter(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                sceneClickHandler
            );
        }
    }

    private void closeDropdown() {
        if (!dropdownOpen) return;
        dropdownOpen = false;

        // Retirer le filtre de clic
        if (contentArea.getScene() != null) {
            contentArea.getScene().removeEventFilter(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                sceneClickHandler
            );
        }

        closeAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(notificationDropdown.opacityProperty(), 1),
                new KeyValue(notificationDropdown.scaleYProperty(), 1.0),
                new KeyValue(notificationDropdown.translateYProperty(), 0)
            ),
            new KeyFrame(Duration.millis(160),
                new KeyValue(notificationDropdown.opacityProperty(), 0),
                new KeyValue(notificationDropdown.scaleYProperty(), 0.93),
                new KeyValue(notificationDropdown.translateYProperty(), -6)
            )
        );
        closeAnim.setOnFinished(ev -> {
            notificationDropdown.setVisible(false);
            notificationDropdown.setManaged(false);
            closeAnim = null;
        });
        closeAnim.play();
    }

    // ── Peupler la liste du dropdown ───────────────────────────────────────
    private void populateList() {
        notificationDropdownList.getChildren().clear();
        List<Notification> notifications = NotificationManager.getInstance().getAll();

        if (notifications.isEmpty()) {
            Label empty = new Label("Aucune notification pour l'instant.");
            empty.setStyle("-fx-text-fill: #65676b; -fx-font-size: 13px; -fx-padding: 20 16;");
            notificationDropdownList.getChildren().add(empty);
        } else {
            for (Notification n : notifications) {
                notificationDropdownList.getChildren().add(buildDropdownItem(n));
            }
        }

        // Marquer comme lues (déclenche seulement onBadgeUpdate, pas de toast)
        NotificationManager.getInstance().marquerToutesLues();
    }

    private HBox buildDropdownItem(Notification n) {
        boolean wasUnread = !n.isLue();

        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        String bgDefault = wasUnread ? "#eef2ff" : "transparent";
        item.setStyle("-fx-background-color: " + bgDefault + "; -fx-background-radius: 10; -fx-cursor: hand;");

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 10; -fx-cursor: hand;"));
        item.setOnMouseExited(e  -> item.setStyle("-fx-background-color: " + bgDefault + "; -fx-background-radius: 10; -fx-cursor: hand;"));

        // Icône colorée dans un cercle
        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(42, 42);
        iconCircle.setMaxSize(42, 42);
        String iconBg = "like".equals(n.getType())    ? "#dde4ff"
                      : "dislike".equals(n.getType()) ? "#fde8e8"
                      : "#e8f5e9";
        iconCircle.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 50;");
        Label icone = new Label("like".equals(n.getType()) ? "👍" : "dislike".equals(n.getType()) ? "👎" : "💬");
        icone.setStyle("-fx-font-size: 18px;");
        iconCircle.getChildren().add(icone);

        // Texte
        VBox textBox = new VBox(3);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        Label message = new Label(n.getMessage());
        message.setStyle("-fx-font-size: 13px; -fx-text-fill: #1c1e21; -fx-wrap-text: true;"
                + (wasUnread ? " -fx-font-weight: bold;" : ""));
        message.setWrapText(true);
        message.setMaxWidth(240);

        Label dateLabel = new Label(
            n.getDate().toLocalDate() + " à " + n.getDate().toLocalTime().toString().substring(0, 5)
        );
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #3b5bdb;");
        textBox.getChildren().addAll(message, dateLabel);

        // Point bleu si non lue
        if (wasUnread) {
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #3b5bdb; -fx-font-size: 9px;");
            item.getChildren().addAll(iconCircle, textBox, dot);
        } else {
            item.getChildren().addAll(iconCircle, textBox);
        }
        return item;
    }

    // ── Bouton "Tout marquer lu" dans le header ────────────────────────────
    @FXML
    void handleMarquerLues() {
        NotificationManager.getInstance().marquerToutesLues();
        populateList();
    }

    // ── Toast (coin haut-droit, 3 sec) ────────────────────────────────────
    private void showNotificationPopup(Notification n) {
        HBox popup = new HBox(10);
        popup.setAlignment(Pos.CENTER_LEFT);
        popup.setPadding(new Insets(12, 18, 12, 18));
        popup.setMaxWidth(360);
        popup.setStyle(
            "-fx-background-color: #1c1e21; -fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);"
        );

        Label icone = new Label("like".equals(n.getType()) ? "👍" : "dislike".equals(n.getType()) ? "👎" : "💬");
        icone.setStyle("-fx-font-size: 18px;");

        Label msg = new Label(n.getMessage());
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-wrap-text: true;");
        msg.setMaxWidth(300);
        msg.setWrapText(true);

        popup.getChildren().addAll(icone, msg);
        notificationPopupContainer.getChildren().add(0, popup);
        popup.setOpacity(0);

        Timeline fadeIn = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(popup.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(300),  new KeyValue(popup.opacityProperty(), 1))
        );
        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.millis(2700), new KeyValue(popup.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(3000), new KeyValue(popup.opacityProperty(), 0))
        );
        fadeOut.setOnFinished(e -> notificationPopupContainer.getChildren().remove(popup));
        fadeIn.play();
        fadeOut.play();
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
