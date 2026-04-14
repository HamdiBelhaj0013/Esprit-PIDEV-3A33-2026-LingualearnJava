package org.example.controller.user;

import org.example.entity.User;
import org.example.service.NotificationService;
import org.example.service.UserService;
import org.example.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class UserMainController {

    @FXML private Label     topbarTitle;
    @FXML private Label     sidebarNameLabel;
    @FXML private Label     sidebarEmailLabel;
    @FXML private Label     sidebarAvatarLabel;
    @FXML private StackPane contentArea;
    @FXML private Button    btnDashboard;
    @FXML private Button    btnProfile;

    private Button activeButton;

    /** Singleton reference so refreshUserInfo() can reach the live instance. */
    private static UserMainController instance;

    private static final String[] AVATAR_COLORS = {
        "#3b5bdb", "#2f9e44", "#e03131", "#f59f00", "#7c3aed", "#0891b2"
    };

    @FXML
    public void initialize() {
        instance = this;
    }

    // ── Entry point from LoginController ─────────────────────────────────────

    public void setUser(User user) {
        SessionManager.setCurrentUser(user);
        refreshUserInfo();
        showDashboard(null);
    }

    // ── Static refresh — called after profile save ────────────────────────────

    public static void refreshUserInfo() {
        if (instance == null) return;
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        instance.sidebarNameLabel.setText(user.getFullName());
        instance.sidebarEmailLabel.setText(user.getEmail());

        String initials = buildInitials(user);
        instance.sidebarAvatarLabel.setText(initials);
        instance.sidebarAvatarLabel.setStyle(
            "-fx-background-color: " + avatarColor(user.getFullName()) + ";"
        );
    }

    // ── Sidebar navigation ────────────────────────────────────────────────────

    @FXML
    private void showDashboard(ActionEvent event) {
        setActive(btnDashboard, "Dashboard");

        // Reload user from DB so the sidebar reflects any admin changes (name, email, status)
        User cached = SessionManager.getCurrentUser();
        if (cached != null) {
            new UserService().findById(cached.getId()).ifPresent(fresh -> {
                SessionManager.setCurrentUser(fresh);
                refreshUserInfo();
            });
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/user/UserDashboard.fxml"));
            Node view = loader.load();
            UserDashboardController controller = loader.getController();
            controller.setParentController(this);
            controller.loadData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            Label err = new Label("Failed to load dashboard: " + e.getMessage());
            err.setStyle("-fx-text-fill: #d63939;");
            contentArea.getChildren().setAll(err);
        }
    }

    @FXML
    void showProfile(ActionEvent event) {
        setActive(btnProfile, "My Profile");
        loadView("/fxml/user/UserProfileView.fxml");
    }

    @FXML private void showCourses(ActionEvent e) {
        setActive((Button) e.getSource(), "My Courses");
        showComingSoonContent();
    }

    @FXML private void showPractice(ActionEvent e) {
        setActive((Button) e.getSource(), "Practice");
        showComingSoonContent();
    }

    @FXML private void showForum(ActionEvent e) {
        setActive((Button) e.getSource(), "Forum");
        showComingSoonContent();
    }

    @FXML private void showFAQ(ActionEvent e) {
        setActive((Button) e.getSource(), "FAQ");
        showComingSoonContent();
    }

    @FXML private void showReclamations(ActionEvent e) {
        setActive((Button) e.getSource(), "Mes réclamations");
        showComingSoonContent();
    }

    @FXML private void showMockTests(ActionEvent e) {
        setActive((Button) e.getSource(), "Mock Tests");
        showComingSoonContent();
    }

    @FXML private void showNotifications(ActionEvent e) {
        setActive((Button) e.getSource(), "Notifications");

        User user = SessionManager.getCurrentUser();
        if (user == null) { showComingSoonContent(); return; }

        VBox page = new VBox(12);
        page.setPadding(new Insets(24));

        Label title = new Label("Notifications");
        title.getStyleClass().add("form-section-title");
        page.getChildren().add(title);

        try {
            List<NotificationService.NotifRow> notifs =
                new NotificationService().getRecentForUser(user.getId());

            if (notifs.isEmpty()) {
                Label empty = new Label("No notifications yet");
                empty.getStyleClass().add("page-subtitle");
                empty.setMaxWidth(Double.MAX_VALUE);
                empty.setAlignment(Pos.CENTER);
                VBox.setMargin(empty, new Insets(32, 0, 0, 0));
                page.getChildren().add(empty);
            } else {
                for (NotificationService.NotifRow n : notifs) {
                    HBox row = new HBox(12);
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
                    page.getChildren().add(row);
                }
            }
        } catch (Exception ex) {
            Label err = new Label("Could not load notifications: " + ex.getMessage());
            err.setStyle("-fx-text-fill: #d63939; -fx-font-size: 12px;");
            page.getChildren().add(err);
        }

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        contentArea.getChildren().setAll(scroll);
    }

    @FXML private void showBilling(ActionEvent e) {
        setActive((Button) e.getSource(), "Plans & Billing");
        showComingSoonContent();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            SessionManager.clearSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("LinguaLearn");
            stage.setMinWidth(400);
            stage.setMinHeight(500);
            stage.centerOnScreen();
        } catch (IOException e) {
            // nothing to do — login screen unavailable
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Called from UserDashboardController quick actions (same package). */
    void showComingSoonFor(String title) {
        topbarTitle.setText(title);
        showComingSoonContent();
    }

    private void showComingSoonContent() {
        Label label = new Label("Coming soon");
        label.getStyleClass().add("page-subtitle");
        label.setAlignment(Pos.CENTER);
        contentArea.getChildren().setAll(label);
    }

    private void setActive(Button btn, String title) {
        if (activeButton != null)
            activeButton.getStyleClass().remove("sidebar-item-active");
        btn.getStyleClass().add("sidebar-item-active");
        activeButton = btn;
        topbarTitle.setText(title);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            Label err = new Label("Failed to load view: " + e.getMessage());
            err.setStyle("-fx-text-fill: #d63939;");
            contentArea.getChildren().setAll(err);
        }
    }

    static String buildInitials(User user) {
        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isBlank())
            initials += Character.toUpperCase(user.getFirstName().charAt(0));
        if (user.getLastName() != null && !user.getLastName().isBlank())
            initials += Character.toUpperCase(user.getLastName().charAt(0));
        return initials.isEmpty() ? "?" : initials;
    }

    static String avatarColor(String name) {
        if (name == null || name.isBlank()) return AVATAR_COLORS[0];
        int idx = Math.abs(name.hashCode()) % AVATAR_COLORS.length;
        return AVATAR_COLORS[idx];
    }
}
