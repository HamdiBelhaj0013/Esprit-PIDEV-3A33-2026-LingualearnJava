package org.example.controller.user;

import org.example.entity.User;
import org.example.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

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
        showProfile(null);
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
        // Placeholder — future dashboard view
        contentArea.getChildren().clear();
        Label placeholder = new Label("Dashboard coming soon…");
        placeholder.setStyle("-fx-text-fill: #6c7a99; -fx-font-size: 16px;");
        contentArea.getChildren().add(placeholder);
    }

    @FXML
    private void showProfile(ActionEvent event) {
        setActive(btnProfile, "My Profile");
        loadView("/fxml/user/UserProfileView.fxml");
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
