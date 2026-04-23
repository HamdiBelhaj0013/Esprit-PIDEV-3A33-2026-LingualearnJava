package org.example.controller.admin;

import org.example.entity.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
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

    private User   loggedInUser;
    private Button activeButton;

    // ── Entry point from LoginController ─────────────────────────────────────

    public void setUser(User user) {
        this.loggedInUser = user;
        adminNameLabel.setText(user.getFirstName() + " " + user.getLastName());
        showDashboard(null);
    }

    // ── Sidebar navigation ────────────────────────────────────────────────────

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

    // ── Called by child controllers to open dialogs ───────────────────────────

    /** Open the user detail popup window for the given user. */
    public void openUserDetail(User user) {
        openStage("/fxml/admin/UserDetailDialog.fxml", "User: " + user.getFullName(),
            700, 620, ctrl -> {
                if (ctrl instanceof UserDetailController c) {
                    c.setMainController(this);
                    c.setUser(user);
                }
            });
    }

    /** Open create-user form dialog with a callback invoked after successful save. */
    public void openCreateUser(Runnable onSaved) {
        openStage("/fxml/admin/UserFormDialog.fxml", "Create User",
            500, 560, ctrl -> {
                if (ctrl instanceof UserFormController c) {
                    c.initForCreate(onSaved);
                }
            });
    }

    /** Open edit-user form dialog for a given user. */
    public void openEditUser(User user, Runnable onSaved) {
        openStage("/fxml/admin/UserFormDialog.fxml", "Edit User — " + user.getFullName(),
            500, 560, ctrl -> {
                if (ctrl instanceof UserFormController c) {
                    c.initForEdit(user, onSaved);
                }
            });
    }

    /** Refresh whatever view is currently active (used after mutations). */
    public void refreshCurrentView() {
        if (activeButton == btnUsers) showUsers(null);
        else showDashboard(null);
    }

    // ── Logout ───────────────────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────────────────

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
