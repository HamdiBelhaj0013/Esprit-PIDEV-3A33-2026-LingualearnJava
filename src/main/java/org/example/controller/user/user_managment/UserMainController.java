package org.example.controller.user.user_managment;

import org.example.controller.tests.LanguageSelectController;
import org.example.controller.user.user_managment.UserDashboardController;
import org.example.entity.User;
import org.example.service.tests.MockTestService;
import org.example.service.user_managment.NotificationService;
import org.example.service.user_managment.UserService;
import org.example.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
    @FXML private Button    btnNotifications;
    @FXML private Button    btnBilling;
    @FXML private Button    btnQuizzes;
    @FXML private Button    btnExercices;

    private Button activeButton;
    private static UserMainController instance;

    private static final String[] AVATAR_COLORS = {
            "#3b5bdb", "#2f9e44", "#e03131", "#f59f00", "#7c3aed", "#0891b2"
    };

    @FXML
    public void initialize() { instance = this; }

    public void setUser(User user) {
        SessionManager.setCurrentUser(user);
        refreshUserInfo();
        showDashboard(null);
    }

    public static void refreshUserInfo() {
        if (instance == null) return;
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        instance.sidebarNameLabel.setText(user.getFullName());
        instance.sidebarEmailLabel.setText(user.getEmail());
        String initials = buildInitials(user);
        instance.sidebarAvatarLabel.setText(initials);
        instance.sidebarAvatarLabel.setStyle(
                "-fx-background-color: " + avatarColor(user.getFullName()) + ";");
    }

    @FXML
    public void showDashboard(ActionEvent event) {
        setActive(btnDashboard, "Dashboard");
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
    private void showLearningStats(ActionEvent event) {
        if (event.getSource() instanceof Button) setActive((Button) event.getSource(), "My Learning Stats");
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/modules/frontoffice/front-stats.fxml"));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            Label err = new Label("Failed to load statistics: " + e.getMessage());
            contentArea.getChildren().setAll(err);
        }
    }

    @FXML
    void showProfile(ActionEvent event) {
        setActive(btnProfile, "My Profile");
        loadView("/fxml/user/UserProfileView.fxml");
    }

    @FXML private void showUserLanguages(ActionEvent e) {
        if (e != null && e.getSource() instanceof Button) setActive((Button) e.getSource(), "Languages");
        org.example.controllers.FrontRouter.setContentPane(contentArea);
        loadView("/fxml/modules/frontoffice/front-languages.fxml");
    }

    @FXML private void showUserCourses(ActionEvent e) {
        if (e != null && e.getSource() instanceof Button) setActive((Button) e.getSource(), "My Courses");
        org.example.controllers.FrontRouter.setContentPane(contentArea);
        loadView("/fxml/modules/frontoffice/front-courses.fxml");
    }

    @FXML private void showUserLessons(ActionEvent e) {
        if (e != null && e.getSource() instanceof Button) setActive((Button) e.getSource(), "My Lessons");
        org.example.controllers.FrontRouter.setContentPane(contentArea);
        loadView("/fxml/modules/frontoffice/front-lessons.fxml");
    }

    @FXML private void showPractice(ActionEvent e) {
        setActive((Button) e.getSource(), "Practice");
        showComingSoonContent();
    }

    @FXML private void showQuizzes(ActionEvent e) {
        setActive((Button) e.getSource(), "Quizzes");
        loadView("/fxml/QuizView.fxml");
    }

    @FXML private void showExercices(ActionEvent e) {
        setActive((Button) e.getSource(), "Exercices");
        loadView("/fxml/admin/ExerciceView.fxml");
    }

    @FXML private void showForum(ActionEvent e) {
        setActive((Button) e.getSource(), "Forum");
        loadView("/fxml/user/forum/fxml/MainView.fxml");
    }

    @FXML private void showFAQ(ActionEvent e) {
        setActive((Button) e.getSource(), "FAQ");
        loadView("/fxml/user/user_view.fxml");
    }

    @FXML private void showReclamations(ActionEvent e) {
        setActive((Button) e.getSource(), "Mes réclamations");
        loadView("/fxml/user/user_view.fxml");
    }

    @FXML private void showMockTests(ActionEvent e) {
        setActive((Button) e.getSource(), "Mock Tests");
        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene dashboardScene = contentArea.getScene();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tests/LanguageSelectView.fxml"));
            Parent root = loader.load();
            LanguageSelectController ctrl = loader.getController();
            ctrl.initEmbedded(
                    new MockTestService(),
                    SessionManager.getCurrentUser(),
                    () -> {
                        stage.setScene(dashboardScene);
                        stage.setTitle("LinguaLearn — Dashboard");
                        showDashboard(null);
                    },
                    stage
            );
            Scene testScene = new Scene(root);
            testScene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(testScene);
            stage.setTitle("LinguaLearn — Langue");
        } catch (IOException ex) {
            Label err = new Label("Failed to load tests: " + ex.getMessage());
            err.setStyle("-fx-text-fill: #d63939;");
            contentArea.getChildren().setAll(err);
        }
    }

    @FXML
    private void showNotifications(ActionEvent event) {
        setActive(btnNotifications, "Notifications");
        User user = SessionManager.getCurrentUser();
        VBox page = new VBox(16);
        page.setPadding(new javafx.geometry.Insets(24));
        Label title = new Label("Notifications");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a1f36;");
        page.getChildren().add(title);
        try {
            List<NotificationService.NotifRow> notifs =
                    new NotificationService().getRecentForUser(user.getId());
            if (notifs.isEmpty()) {
                Label empty = new Label("No notifications yet.");
                empty.setStyle("-fx-text-fill: #6c7a99; -fx-font-size: 14px;");
                page.getChildren().add(empty);
            } else {
                for (NotificationService.NotifRow n : notifs) {
                    VBox row = new VBox(4);
                    row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; " +
                            "-fx-border-color: #e3e8f0; -fx-border-radius: 8; " +
                            "-fx-border-width: 1; -fx-padding: 10 14;");
                    Label typeLbl = new Label(n.type.toUpperCase());
                    typeLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; " +
                            "-fx-text-fill: #3b5bdb; -fx-background-color: #eef2ff; " +
                            "-fx-background-radius: 4; -fx-padding: 2 6;");
                    Label msgLbl = new Label(n.message);
                    msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
                    msgLbl.setWrapText(true);
                    Label dateLbl = new Label(n.createdAt + (n.isRead ? " · Read" : " · Unread"));
                    dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c7a99;");
                    row.getChildren().addAll(typeLbl, msgLbl, dateLbl);
                    page.getChildren().add(row);
                }
            }
        } catch (Exception e) {
            Label err = new Label("Could not load notifications: " + e.getMessage());
            err.setStyle("-fx-text-fill: #d63939;");
            page.getChildren().add(err);
        }
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; " +
                "-fx-border-width: 0;");
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        contentArea.getChildren().setAll(scroll);
    }

    public void navigateToNotifications() { showNotifications(null); }

    @FXML
    private void showBilling(ActionEvent event) {
        setActive(btnBilling, "Plans & Billing");
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/user/Plans.fxml"));
            Node view = loader.load();
            PlansController ctrl = loader.getController();
            ctrl.loadData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            Label err = new Label("Failed to load billing page: " + e.getMessage());
            err.setStyle("-fx-text-fill: #d63939;");
            contentArea.getChildren().setAll(err);
        }
    }

    public void navigateToBilling() { showBilling(null); }

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
            // nothing to do
        }
    }

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