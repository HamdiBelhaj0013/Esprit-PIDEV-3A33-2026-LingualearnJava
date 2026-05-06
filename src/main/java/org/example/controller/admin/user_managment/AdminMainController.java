package org.example.controller.admin.user_managment;

import org.example.entity.User;
import org.example.controller.admin.AdminAiController;
import org.example.controller.admin.user_managment.DashboardViewController;
import org.example.controller.admin.user_managment.UserListController;
import org.example.controller.admin.user_managment.UserDetailController;
import org.example.controller.admin.user_managment.UserFormController;
import org.example.controller.tests.MockTestDashboardController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class AdminMainController {

    @FXML private Label     topbarTitle;
    @FXML private Label     adminNameLabel;
    @FXML private StackPane contentArea;
    @FXML private Button    btnDashboard;
    @FXML private Button    btnUsers;
    @FXML private Button    btnSupport;
    @FXML private Button    btnTests;
    @FXML private Button    btnAiAssistant;
    @FXML private Button    btnForumStats;
    @FXML private Button    btnForumPubs;
    @FXML private Button    btnForumComments;

    private User   loggedInUser;
    private Button activeButton;

    public void setUser(User user) {
        this.loggedInUser = user;
        adminNameLabel.setText(user.getFirstName() + " " + user.getLastName());
        showDashboard(null);
    }

    @FXML
    private void showDashboard(ActionEvent event) {
        setActive(btnDashboard, "Dashboard");
        loadView("/fxml/admin/DashboardView.fxml", ctrl -> {
            if (ctrl instanceof DashboardViewController c) {
                c.load();
            }
        });
    }

    @FXML
    private void showUsers(ActionEvent event) {
        setActive(btnUsers, "User Management");
        loadView("/fxml/admin/UserListView.fxml", ctrl -> {
            if (ctrl instanceof UserListController c) {
                c.setMainController(this);
                c.load();
            }
        });
    }

    @FXML
    private void showSupport(ActionEvent event) {
        setActive(btnSupport, "Support");
        loadView("/fxml/admin/admin_view.fxml", ctrl -> {});
    }

    @FXML
    private void showTests(ActionEvent event) {
        setActive(btnTests, "International Tests");
        loadView("/fxml/tests/MockTestDashboard.fxml", ctrl -> {
            if (ctrl instanceof MockTestDashboardController c) {
                c.setContentArea(contentArea);
            }
        });
    }

    @FXML
    private void showAiAssistant(ActionEvent event) {
        setActive(btnAiAssistant, "AI Assistant");
        loadView("/fxml/admin/AdminAiView.fxml", ctrl -> {
            // AdminAiController self-initializes in @FXML initialize()
        });
    }

    @FXML
    private void showForumStats(ActionEvent event) {
        setActive(btnForumStats, "Forum - Statistics");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/fxml/admin/forum/fxml/dashboard.fxml"));
            Node view = loader.load();
            if (view instanceof HBox hbox && !hbox.getChildren().isEmpty()) {
                Node forumSidebar = hbox.getChildren().get(0);
                forumSidebar.setVisible(false);
                forumSidebar.setManaged(false);
            }
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Failed to load Forum Statistics: " + e.getMessage());
        }
    }

    @FXML
    private void showForumPublications(ActionEvent event) {
        setActive(btnForumPubs, "Forum - Publications");
        loadView("/fxml/admin/forum/fxml/publication_manager.fxml", ctrl -> {});
    }

    @FXML
    private void showForumComments(ActionEvent event) {
        setActive(btnForumComments, "Forum - Comments");
        loadView("/fxml/admin/forum/fxml/commentaire_manager.fxml", ctrl -> {});
    }

    public void openUserDetail(User user) {
        openStage("/fxml/admin/UserDetailDialog.fxml", "User: " + user.getFullName(),
                700, 620, ctrl -> {
                    if (ctrl instanceof UserDetailController c) {
                        c.setMainController(this);
                        c.setUser(user);
                    }
                });
    }

    public void openCreateUser(Runnable onSaved) {
        openStage("/fxml/admin/UserFormDialog.fxml", "Create User",
                500, 560, ctrl -> {
                    if (ctrl instanceof UserFormController c) {
                        c.initForCreate(onSaved);
                    }
                });
    }

    public void openEditUser(User user, Runnable onSaved) {
        openStage("/fxml/admin/UserFormDialog.fxml", "Edit User - " + user.getFullName(),
                500, 560, ctrl -> {
                    if (ctrl instanceof UserFormController c) {
                        c.initForEdit(user, onSaved);
                    }
                });
    }

    public void refreshCurrentView() {
        if (activeButton == btnUsers) showUsers(null);
        else showDashboard(null);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("LinguaLearn");
            stage.setMinWidth(400);
            stage.setMinHeight(500);
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Logout failed: " + e.getMessage());
        }
    }

    private void setActive(Button btn, String title) {
        if (activeButton != null)
            activeButton.getStyleClass().remove("sidebar-item-active");
        btn.getStyleClass().add("sidebar-item-active");
        activeButton = btn;
        topbarTitle.setText(title);
    }

    private void loadView(String fxmlPath, Consumer<Object> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            setup.accept(loader.getController());
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Failed to load view: " + e.getMessage());
        }
    }

    private void openStage(String fxmlPath, String title,
                           double minW, double minH, Consumer<Object> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            setup.accept(loader.getController());
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initOwner(contentArea.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setMinWidth(minW);
            stage.setMinHeight(minH);
            stage.showAndWait();
        } catch (IOException e) {
            showError("Failed to open window: " + e.getMessage());
        }
    }

    public void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}