package org.example.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.entity.User;
import org.example.service.UserService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class UserFormController {

    @FXML private Label       dialogTitle;
    @FXML private Label       dialogSubtitle;

    @FXML private TextField   firstNameField;
    @FXML private Label       firstNameError;
    @FXML private TextField   lastNameField;
    @FXML private Label       lastNameError;
    @FXML private TextField   emailField;
    @FXML private Label       emailError;
    @FXML private Label       passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private Label       passwordError;
    @FXML private PasswordField confirmField;
    @FXML private Label       confirmError;

    @FXML private VBox             statusRow;
    @FXML private ChoiceBox<String> statusChoice;
    @FXML private Label            statusError;

    @FXML private CheckBox    roleUser;
    @FXML private CheckBox    roleAdmin;
    @FXML private CheckBox    roleTeacher;

    @FXML private Label       globalError;
    @FXML private Button      saveBtn;

    private enum Mode { CREATE, EDIT }
    private Mode               mode;
    private User               editTarget;
    private EntityManagerFactory emf;
    private Runnable           onSaved;

    // ── Init ───────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        firstNameField.textProperty().addListener((o, ov, nv) -> clearFieldError(firstNameError));
        lastNameField.textProperty().addListener((o, ov, nv)  -> clearFieldError(lastNameError));
        emailField.textProperty().addListener((o, ov, nv)     -> clearFieldError(emailError));
        passwordField.textProperty().addListener((o, ov, nv)  -> clearFieldError(passwordError));
        confirmField.textProperty().addListener((o, ov, nv)   -> clearFieldError(confirmError));
    }

    public void initForCreate(EntityManagerFactory emf, Runnable onSaved) {
        this.mode    = Mode.CREATE;
        this.emf     = emf;
        this.onSaved = onSaved;

        dialogTitle.setText("Create User");
        dialogSubtitle.setText("All fields marked * are required");
        passwordLabel.setText("Password *");
        statusRow.setVisible(false);
        statusRow.setManaged(false);
        roleUser.setSelected(true);
    }

    public void initForEdit(User user, EntityManagerFactory emf, Runnable onSaved) {
        this.mode       = Mode.EDIT;
        this.editTarget = user;
        this.emf        = emf;
        this.onSaved    = onSaved;

        dialogTitle.setText("Edit User");
        dialogSubtitle.setText("Leave password blank to keep it unchanged");
        passwordLabel.setText("New Password (optional)");

        // Populate fields
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());

        // Status
        statusRow.setVisible(true);
        statusRow.setManaged(true);
        statusChoice.setItems(FXCollections.observableArrayList("active", "suspended", "deleted"));
        statusChoice.setValue(user.getStatus());

        // Roles
        roleUser.setSelected(true);
        roleAdmin.setSelected(user.hasRole("ROLE_ADMIN"));
        roleTeacher.setSelected(user.hasRole("ROLE_TEACHER"));
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    @FXML
    private void handleSave(ActionEvent event) {
        clearErrors();
        if (!validate()) return;

        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String email     = emailField.getText().trim();
        String password  = passwordField.getText();
        String confirm   = confirmField.getText();

        EntityManager em = emf.createEntityManager();
        try {
            UserService svc = new UserService(em);

            if (mode == Mode.CREATE) {
                List<String> roles = buildRoles();
                svc.createUser(email, password, confirm, firstName, lastName, roles);
            } else {
                // Update core fields
                svc.adminUpdateUser(editTarget, firstName, lastName,
                    password.isBlank() ? null : password,
                    password.isBlank() ? null : confirm);

                // Update email separately if changed
                if (!email.equalsIgnoreCase(editTarget.getEmail())) {
                    editTarget.setEmail(email.toLowerCase());
                }

                // Update status
                if (statusChoice.getValue() != null) {
                    editTarget.setStatus(statusChoice.getValue());
                }

                // Update roles
                svc.changeRoles(editTarget, buildRoles());
            }

            closeStage();
            if (onSaved != null) onSaved.run();

        } catch (Exception ex) {
            showGlobalError(ex.getMessage());
        } finally {
            em.close();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeStage();
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    private boolean validate() {
        boolean ok = true;

        if (firstNameField.getText().trim().isBlank()) {
            showError(firstNameError, "First name is required");
            ok = false;
        } else if (firstNameField.getText().trim().length() < 2) {
            showError(firstNameError, "Minimum 2 characters");
            ok = false;
        }

        if (lastNameField.getText().trim().isBlank()) {
            showError(lastNameError, "Last name is required");
            ok = false;
        } else if (lastNameField.getText().trim().length() < 2) {
            showError(lastNameError, "Minimum 2 characters");
            ok = false;
        }

        String email = emailField.getText().trim();
        if (email.isBlank()) {
            showError(emailError, "Email is required");
            ok = false;
        } else if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "Invalid email format");
            ok = false;
        }

        String pwd = passwordField.getText();
        if (mode == Mode.CREATE && pwd.isBlank()) {
            showError(passwordError, "Password is required");
            ok = false;
        } else if (!pwd.isBlank() && pwd.length() < 8) {
            showError(passwordError, "Minimum 8 characters");
            ok = false;
        } else if (!pwd.isBlank() && !pwd.equals(confirmField.getText())) {
            showError(confirmError, "Passwords do not match");
            ok = false;
        }

        if (mode == Mode.EDIT) {
            String status = statusChoice.getValue();
            if (status == null || status.isBlank()) {
                showError(statusError, "Status is required");
                ok = false;
            } else if (!status.equals("active") && !status.equals("suspended") && !status.equals("deleted")) {
                showError(statusError, "Status must be active, suspended, or deleted");
                ok = false;
            }
        }

        return ok;
    }

    private List<String> buildRoles() {
        List<String> roles = new ArrayList<>();
        roles.add("ROLE_USER");
        if (roleAdmin.isSelected())   roles.add("ROLE_ADMIN");
        if (roleTeacher.isSelected()) roles.add("ROLE_TEACHER");
        return roles;
    }

    // ── Error helpers ─────────────────────────────────────────────────────────

    private void clearErrors() {
        for (Label l : List.of(firstNameError, lastNameError, emailError,
                               passwordError, confirmError, statusError, globalError)) {
            l.setVisible(false);
            l.setManaged(false);
        }
    }

    private void clearFieldError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showGlobalError(String msg) {
        globalError.setText(msg);
        globalError.setVisible(true);
        globalError.setManaged(true);
    }

    private void closeStage() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }
}
