package org.example.controller.admin;

import jakarta.persistence.EntityManager;
import org.example.App;
import org.example.entity.LearningStats;
import org.example.entity.User;
import org.example.service.NotificationService;
import org.example.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class UserDetailController {

    @FXML private Label  fullNameLabel;
    @FXML private Label  statusBadge;
    @FXML private Label  emailLabel;

    // Profile
    @FXML private Label  rolesLabel;
    @FXML private Label  verifiedLabel;
    @FXML private Label  bannedLabel;
    @FXML private Label  banReasonLabel;
    @FXML private Label  joinedLabel;

    // Subscription
    @FXML private Label  planLabel;
    @FXML private Label  premiumLabel;
    @FXML private Label  expiryLabel;
    @FXML private Label  paymentLabel;
    @FXML private HBox   premiumActions;
    @FXML private Button grantMonthlyBtn;
    @FXML private Button grantYearlyBtn;
    @FXML private Button revokePremiumBtn;

    // Stats
    @FXML private Label  xpLabel;
    @FXML private Label  wordsLabel;
    @FXML private Label  minutesLabel;
    @FXML private Label  lastSessionLabel;

    // Notifications
    @FXML private VBox   notifList;

    // Footer
    @FXML private Button toggleStatusBtn;

    private AdminMainController mainController;
    private User                user;

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Init ───────────────────────────────────────────────────────────────────

    public void setMainController(AdminMainController mc) { this.mainController = mc; }

    public void setUser(User u) {
        this.user = u;
        populate();
    }

    // ── Populate all sections ─────────────────────────────────────────────────

    private void populate() {
        // Header
        fullNameLabel.setText(user.getFullName());
        emailLabel.setText(user.getEmail());
        statusBadge.setText(user.getStatus());
        statusBadge.getStyleClass().setAll("badge-" + user.getStatus());

        // Profile
        rolesLabel.setText(String.join(", ", user.getRoles()));
        verifiedLabel.setText(user.isVerified() ? "Yes" : "No");
        bannedLabel.setText(user.isBanned() ? "Yes" : "No");
        banReasonLabel.setText(user.getBanReason() != null ? user.getBanReason() : "—");
        joinedLabel.setText(user.getCreatedAt() != null ? DT_FMT.format(user.getCreatedAt()) : "—");

        // Subscription
        planLabel.setText(user.getSubscriptionPlan());
        premiumLabel.setText(user.isPremium() ? "★ Active" : "No");
        expiryLabel.setText(user.getSubscriptionExpiry() != null
            ? DT_FMT.format(user.getSubscriptionExpiry()) : "—");
        paymentLabel.setText(user.getLastPaymentStatus() != null
            ? user.getLastPaymentStatus() : "—");

        // Premium action buttons visibility
        boolean premium = user.isPremium();
        grantMonthlyBtn.setVisible(!premium);
        grantMonthlyBtn.setManaged(!premium);
        grantYearlyBtn.setVisible(!premium);
        grantYearlyBtn.setManaged(!premium);
        revokePremiumBtn.setVisible(premium);
        revokePremiumBtn.setManaged(premium);

        // Stats
        LearningStats stats = user.getLearningStats();
        if (stats != null) {
            xpLabel.setText(String.valueOf(stats.getTotalXP()));
            wordsLabel.setText(String.valueOf(stats.getWordsLearned()));
            minutesLabel.setText(String.valueOf(stats.getTotalMinutesStudied()));
            lastSessionLabel.setText(stats.getLastStudySession() != null
                ? DT_FMT.format(stats.getLastStudySession()) : "Never");
        } else {
            xpLabel.setText("0"); wordsLabel.setText("0");
            minutesLabel.setText("0"); lastSessionLabel.setText("Never");
        }

        // Toggle status button label
        toggleStatusBtn.setText("active".equals(user.getStatus()) ? "Suspend" : "Activate");
        toggleStatusBtn.getStyleClass().removeAll("btn-warning", "btn-success");
        toggleStatusBtn.getStyleClass().add(
            "active".equals(user.getStatus()) ? "btn-warning" : "btn-success");

        // Notifications
        loadNotifications();
    }

    private void loadNotifications() {
        notifList.getChildren().clear();
        EntityManager em = App.getEmf().createEntityManager();
        try {
            NotificationService ns = new NotificationService(em);
            List<NotificationService.NotifRow> notifs = ns.getRecentForUser(user.getId());
            if (notifs.isEmpty()) {
                notifList.getChildren().add(new Label("No notifications sent yet.") {{
                    getStyleClass().add("page-subtitle");
                }});
            } else {
                for (NotificationService.NotifRow n : notifs) {
                    VBox row = buildNotifRow(n);
                    notifList.getChildren().add(row);
                }
            }
        } finally {
            em.close();
        }
    }

    private VBox buildNotifRow(NotificationService.NotifRow n) {
        Label typeLbl = new Label(n.type.toUpperCase());
        typeLbl.getStyleClass().add("notif-type");

        Label msgLbl = new Label(n.message);
        msgLbl.getStyleClass().add("notif-message");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(Double.MAX_VALUE);

        Label dateLbl = new Label(n.createdAt + (n.isRead ? " · Read" : " · Unread"));
        dateLbl.getStyleClass().add("notif-date");

        HBox header = new HBox(8, typeLbl, dateLbl);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox row = new VBox(4, header, msgLbl);
        row.getStyleClass().add("notif-row");
        return row;
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    @FXML private void handleEdit(ActionEvent e) {
        mainController.openEditUser(user, () -> reloadAndRefresh());
    }

    @FXML private void handleToggleStatus(ActionEvent e) {
        boolean suspend = "active".equals(user.getStatus());
        String action = suspend ? "suspend" : "activate";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to " + action + " " + user.getFullName() + "?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            withService(svc -> {
                if (suspend) svc.suspendUser(user);
                else         svc.activateUser(user);
            });
            reloadAndRefresh();
        });
    }

    @FXML private void handleGrantMonthly(ActionEvent e) {
        withService(svc ->
            svc.grantPremium(user, "MONTHLY", LocalDateTime.now().plusMonths(1)));
        reloadAndRefresh();
    }

    @FXML private void handleGrantYearly(ActionEvent e) {
        withService(svc ->
            svc.grantPremium(user, "YEARLY", LocalDateTime.now().plusYears(1)));
        reloadAndRefresh();
    }

    @FXML private void handleRevokePremium(ActionEvent e) {
        withService(svc -> svc.revokePremium(user));
        reloadAndRefresh();
    }

    @FXML private void handleEditStats(ActionEvent e) {
        openDialog("/fxml/admin/StatsDialog.fxml", "Edit Learning Stats", 400, 340, ctrl -> {
            if (ctrl instanceof StatsController c) {
                c.setUser(user, App.getEmf(), this::reloadAndRefresh);
            }
        });
    }

    @FXML private void handleSendNotif(ActionEvent e) {
        openDialog("/fxml/admin/NotificationDialog.fxml", "Send Notification", 420, 300, ctrl -> {
            if (ctrl instanceof NotificationController c) {
                c.setUser(user, App.getEmf(), this::reloadAndRefresh);
            }
        });
    }

    @FXML private void handleDelete(ActionEvent e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Permanently delete " + user.getFullName() + "?\nThis cannot be undone.",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            withService(svc -> svc.deleteUser(user));
            mainController.refreshCurrentView();
            closeStage();
        });
    }

    @FXML private void handleClose(ActionEvent e) {
        closeStage();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void reloadAndRefresh() {
        EntityManager em = App.getEmf().createEntityManager();
        try {
            User reloaded = new UserService(em).findById(user.getId()).orElse(user);
            this.user = reloaded;
            populate();
        } finally {
            em.close();
        }
        mainController.refreshCurrentView();
    }

    private void withService(java.util.function.Consumer<UserService> action) {
        EntityManager em = App.getEmf().createEntityManager();
        try {
            action.accept(new UserService(em));
        } catch (Exception ex) {
            showError(ex.getMessage());
        } finally {
            em.close();
        }
    }

    private void openDialog(String fxmlPath, String title, double w, double h,
                            java.util.function.Consumer<Object> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            setup.accept(loader.getController());
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initOwner(fullNameLabel.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(w);
            stage.setMinHeight(h);
            stage.showAndWait();
        } catch (IOException ex) {
            showError("Could not open dialog: " + ex.getMessage());
        }
    }

    private void closeStage() {
        ((Stage) fullNameLabel.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
