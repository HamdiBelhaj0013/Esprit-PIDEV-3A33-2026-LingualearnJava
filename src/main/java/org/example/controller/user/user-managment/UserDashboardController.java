package org.example.controller.user;

import org.example.entity.LearningStats;
import org.example.entity.User;
import org.example.service.NotificationService;
import org.example.service.UserService;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserDashboardController {

    @FXML private ScrollPane rootPane;
    @FXML private Label      welcomeLabel;
    @FXML private Label      dateLabel;
    @FXML private Label      topAvatarLabel;
    @FXML private Label      wordsLearnedValue;
    @FXML private Label      totalXpValue;
    @FXML private Label      studyTimeValue;
    @FXML private VBox       notificationsBox;
    @FXML private VBox       subscriptionCard;
    @FXML private VBox       accountInfoBox;

    private UserMainController parentController;

    @FXML
    public void initialize() {
        rootPane.getStylesheets().add(
            getClass().getResource("/css/admin.css").toExternalForm()
        );
    }

    public void setParentController(UserMainController parent) {
        this.parentController = parent;
    }

    public void refresh() {
        loadData();
    }

    public void loadData() {
        User cached = SessionManager.getCurrentUser();
        if (cached == null) return;

        // Reload from DB so stats and profile reflect any admin edits made since login
        User user = new UserService().findById(cached.getId()).orElse(cached);
        SessionManager.setCurrentUser(user);

        LearningStats stats = user.getLearningStats();

        // ── Header ──────────────────────────────────────────────────────────
        welcomeLabel.setText("Welcome back, " + user.getFirstName() + " \uD83D\uDC4B");
        dateLabel.setText(LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        topAvatarLabel.setText(UserMainController.buildInitials(user));
        topAvatarLabel.setStyle(
            "-fx-background-color: " + UserMainController.avatarColor(user.getFullName()) + ";" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;" +
            "-fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36;" +
            "-fx-max-width: 36; -fx-max-height: 36; -fx-alignment: center;"
        );

        // ── Stat cards ───────────────────────────────────────────────────────
        int words   = stats != null ? stats.getWordsLearned()       : 0;
        int xp      = stats != null ? stats.getTotalXP()            : 0;
        int minutes = stats != null ? stats.getTotalMinutesStudied() : 0;

        wordsLearnedValue.setText(String.valueOf(words));
        totalXpValue.setText(String.valueOf(xp));
        studyTimeValue.setText(String.valueOf(minutes));

        loadNotifications(user);
        buildSubscriptionCard(user);
        buildAccountInfo(user, stats);
    }

    // ── Notifications ────────────────────────────────────────────────────────

    private void loadNotifications(User user) {
        notificationsBox.getChildren().clear();
        try {
            List<NotificationService.NotifRow> notifs =
                new NotificationService().getRecentForUser(user.getId());

            if (notifs.isEmpty()) {
                Label bell = new Label("\uD83D\uDD14");
                bell.setStyle("-fx-font-size: 28px;");
                Label msg = new Label("You're all caught up!");
                msg.setStyle("-fx-text-fill: #6c7a99; -fx-font-size: 13px;");
                VBox empty = new VBox(6, bell, msg);
                empty.setAlignment(Pos.CENTER);
                empty.setStyle("-fx-padding: 20;");
                notificationsBox.getChildren().add(empty);
            } else {
                for (NotificationService.NotifRow n : notifs) {
                    HBox row = new HBox(10);
                    row.getStyleClass().add("notif-row");
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label type = new Label(n.type);
                    type.getStyleClass().add("notif-type");

                    Label message = new Label(n.message);
                    message.getStyleClass().add("notif-message");
                    message.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(message, Priority.ALWAYS);

                    String dateStr = n.createdAt != null && n.createdAt.length() >= 10
                        ? n.createdAt.substring(0, 10) : (n.createdAt != null ? n.createdAt : "");
                    Label date = new Label(dateStr);
                    date.getStyleClass().add("notif-date");

                    row.getChildren().addAll(type, message, date);
                    notificationsBox.getChildren().add(row);
                }
            }
        } catch (Exception e) {
            Label err = new Label("Could not load notifications.");
            err.setStyle("-fx-text-fill: #d63939; -fx-font-size: 12px;");
            notificationsBox.getChildren().add(err);
        }
    }

    // ── Subscription card ────────────────────────────────────────────────────

    private void buildSubscriptionCard(User user) {
        subscriptionCard.getChildren().clear();

        Label title = new Label("My Subscription");
        title.getStyleClass().add("form-section-title");

        String plan = user.getSubscriptionPlan();
        if (plan == null) plan = "FREE";

        String planDisplayName;
        List<String> features;

        switch (plan) {
            case "MONTHLY" -> {
                planDisplayName = "Monthly Plan";
                features = List.of(
                    "Access to free courses",
                    "Basic vocabulary practice",
                    "All premium courses",
                    "Mock test access"
                );
            }
            case "YEARLY" -> {
                planDisplayName = "Yearly Plan";
                features = List.of(
                    "Access to free courses",
                    "Basic vocabulary practice",
                    "All premium courses",
                    "Mock test access",
                    "AI conversation practice"
                );
            }
            default -> {
                planDisplayName = "Free Plan";
                features = List.of(
                    "Access to free courses",
                    "Basic vocabulary practice"
                );
            }
        }

        Label planName = new Label(planDisplayName);
        planName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3b5bdb;");

        subscriptionCard.getChildren().addAll(title, planName);

        if (!"FREE".equals(plan) && user.getSubscriptionExpiry() != null) {
            String expiry = user.getSubscriptionExpiry()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
            Label expiryLabel = new Label("Renews " + expiry);
            expiryLabel.setStyle("-fx-text-fill: #6c7a99; -fx-font-size: 12px;");
            subscriptionCard.getChildren().add(expiryLabel);
        }

        Label featuresTitle = new Label("Included features");
        featuresTitle.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        VBox.setMargin(featuresTitle, new Insets(8, 0, 4, 0));
        subscriptionCard.getChildren().add(featuresTitle);

        for (String feature : features) {
            Label f = new Label("\u2713  " + feature);
            f.setStyle("-fx-font-size: 12px; -fx-text-fill: #2fb344;");
            subscriptionCard.getChildren().add(f);
        }

        Button manage = new Button("Manage Subscription");
        manage.getStyleClass().addAll("btn-outline", "btn-sm");
        VBox.setMargin(manage, new Insets(12, 0, 0, 0));
        manage.setOnAction(e -> {
            if (parentController != null) parentController.navigateToBilling();
        });
        subscriptionCard.getChildren().add(manage);
    }

    // ── Account info card ────────────────────────────────────────────────────

    private void buildAccountInfo(User user, LearningStats stats) {
        accountInfoBox.getChildren().clear();

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy");

        addInfoRow("Full Name",   user.getFullName());
        addInfoRow("Email",       user.getEmail());
        addInfoRow("Member Since",
            user.getCreatedAt() != null ? user.getCreatedAt().format(dateFmt) : "\u2014");

        // Status with color
        Label statusKey = infoKeyLabel("Status");
        Label statusVal = new Label(
            user.getStatus() != null ? capitalize(user.getStatus()) : "\u2014");
        statusVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " +
            ("active".equals(user.getStatus()) ? "#2fb344" : "#f59f00") + ";");
        addCustomRow(statusKey, statusVal);

        String lastStudy = "\u2014";
        if (stats != null && stats.getLastStudySession() != null)
            lastStudy = stats.getLastStudySession().format(dateFmt);
        addInfoRow("Last Study", lastStudy);

        int words = stats != null ? stats.getWordsLearned() : 0;
        int xp    = stats != null ? stats.getTotalXP()      : 0;
        addProgressRow("Words Learned", words, 500,  "#2fb344");
        addProgressRow("Total XP",      xp,    1000, "#3b5bdb");
    }

    private void addInfoRow(String key, String value) {
        Label k = infoKeyLabel(key);
        Label v = new Label(value != null ? value : "\u2014");
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1f36;");
        addCustomRow(k, v);
    }

    private void addCustomRow(Label key, Label value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(key, spacer, value);
        accountInfoBox.getChildren().add(row);
    }

    private Label infoKeyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #6c7a99; -fx-font-size: 12px; -fx-min-width: 90;");
        return lbl;
    }

    private void addProgressRow(String label, int value, int max, String accentColor) {
        VBox wrapper = new VBox(4);

        HBox header = new HBox(4);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #6c7a99; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(String.valueOf(value));
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a1f36;");
        header.getChildren().addAll(lbl, spacer, val);

        double progress = (max > 0 && value > 0) ? Math.min(1.0, (double) value / max) : 0.0;
        ProgressBar bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);
        bar.setStyle("-fx-accent: " + accentColor + ";");

        wrapper.getChildren().addAll(header, bar);
        accountInfoBox.getChildren().add(wrapper);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Quick action handlers ────────────────────────────────────────────────

    @FXML private void onViewAllNotifications() {
        if (parentController != null) parentController.navigateToNotifications();
    }

    @FXML private void onBrowseCourses() {
        if (parentController != null) parentController.showComingSoonFor("My Courses");
    }

    @FXML private void onPracticeNow() {
        if (parentController != null) parentController.showComingSoonFor("Practice");
    }

    @FXML private void onMockTest() {
        if (parentController != null) parentController.showComingSoonFor("Mock Tests");
    }

    @FXML private void onEditProfile() {
        if (parentController != null) parentController.showProfile(null);
    }
}
