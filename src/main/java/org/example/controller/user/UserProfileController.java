package org.example.controller.user;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.entity.User;
import org.example.service.UserService;
import org.example.util.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class UserProfileController {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    // ── Card 1 — display mode ─────────────────────────────────────────────────
    @FXML private VBox   displayModeBox;
    @FXML private Label  avatarLabel;
    @FXML private Label  displayName;
    @FXML private Label  displayEmail;
    @FXML private Label  displayMemberSince;
    @FXML private Label  statusBadge;

    // ── Card 1 — edit mode ────────────────────────────────────────────────────
    @FXML private VBox      editModeBox;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private Label     firstNameError;
    @FXML private Label     lastNameError;
    @FXML private Label     emailError;
    @FXML private Button    saveButton;

    // ── Card 2 — change password ──────────────────────────────────────────────
    @FXML private PasswordField currentPwField;
    @FXML private TextField     currentPwVisible;
    @FXML private PasswordField newPwField;
    @FXML private TextField     newPwVisible;
    @FXML private PasswordField confirmPwField;
    @FXML private TextField     confirmPwVisible;

    @FXML private Region seg1;
    @FXML private Region seg2;
    @FXML private Region seg3;
    @FXML private Region seg4;
    @FXML private Label  strengthLabel;
    @FXML private Label  matchLabel;
    @FXML private Button updatePwButton;

    // ── Global banner ─────────────────────────────────────────────────────────
    @FXML private Label bannerLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("MMM d, yyyy");

    private static final String[] AVATAR_COLORS = {
        "#3b5bdb", "#2f9e44", "#e03131", "#f59f00", "#7c3aed", "#0891b2"
    };

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        populateDisplayMode();
        wireChangeDetection();
        wirePasswordListeners();
    }

    // ── Display mode ─────────────────────────────────────────────────────────

    private void populateDisplayMode() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        String initials = UserMainController.buildInitials(u);
        String color    = UserMainController.avatarColor(u.getFullName());

        avatarLabel.setText(initials);
        avatarLabel.setStyle("-fx-background-color: " + color + ";");

        displayName.setText(u.getFullName());
        displayEmail.setText(u.getEmail());

        if (u.getCreatedAt() != null)
            displayMemberSince.setText("Member since " + u.getCreatedAt().format(DATE_FMT));
        else
            displayMemberSince.setText("");

        applyStatusBadge(u.getStatus());
    }

    private void applyStatusBadge(String status) {
        statusBadge.getStyleClass().removeAll("badge-active", "badge-suspended");
        if ("active".equalsIgnoreCase(status)) {
            statusBadge.setText("Active");
            statusBadge.getStyleClass().add("badge-active");
        } else {
            statusBadge.setText("Suspended");
            statusBadge.getStyleClass().add("badge-suspended");
        }
    }

    // ── Edit profile — enter / exit ───────────────────────────────────────────

    @FXML
    private void startEdit(ActionEvent event) {
        User u = SessionManager.getCurrentUser();
        firstNameField.setText(u.getFirstName());
        lastNameField.setText(u.getLastName());
        emailField.setText(u.getEmail());

        clearFieldErrors();
        saveButton.setDisable(true);

        displayModeBox.setVisible(true);
        displayModeBox.setManaged(true);
        editModeBox.setVisible(true);
        editModeBox.setManaged(true);
    }

    @FXML
    private void cancelEdit(ActionEvent event) {
        editModeBox.setVisible(false);
        editModeBox.setManaged(false);
        clearFieldErrors();
    }

    // ── Change detection (enable Save only when something changed) ────────────

    private void wireChangeDetection() {
        Runnable check = () -> {
            User u = SessionManager.getCurrentUser();
            boolean changed =
                !firstNameField.getText().trim().equals(u.getFirstName()) ||
                !lastNameField.getText().trim().equals(u.getLastName())   ||
                !emailField.getText().trim().equals(u.getEmail());
            saveButton.setDisable(!changed);
        };

        firstNameField.textProperty().addListener((o, ov, nv) -> { clearError(firstNameError); check.run(); });
        lastNameField.textProperty().addListener((o, ov, nv)  -> { clearError(lastNameError);  check.run(); });
        emailField.textProperty().addListener((o, ov, nv)     -> { clearError(emailError);     check.run(); });
    }

    // ── Save profile ──────────────────────────────────────────────────────────

    @FXML
    private void saveProfile(ActionEvent event) {
        clearFieldErrors();

        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String email     = emailField.getText().trim();

        boolean valid = true;

        if (firstName.length() < 2) {
            showFieldError(firstNameError, "First name must be at least 2 characters");
            valid = false;
        }
        if (lastName.length() < 2) {
            showFieldError(lastNameError, "Last name must be at least 2 characters");
            valid = false;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showFieldError(emailError, "Please enter a valid email address");
            valid = false;
        }
        if (!valid) return;

        User currentUser = SessionManager.getCurrentUser();
        UserService svc  = new UserService();

        // Email uniqueness check — ensure no other account uses this email
        var existing = svc.findByEmail(email);
        if (existing.isPresent() && !existing.get().getId().equals(currentUser.getId())) {
            showFieldError(emailError, "This email is already in use");
            return;
        }

        try {
            // Set email before calling updateName — save() persists all User fields at once
            currentUser.setEmail(email);
            svc.updateName(currentUser, firstName, lastName);

            // Reload from DB so LearningStats and all fields are current in the session
            svc.findById(currentUser.getId()).ifPresent(fresh -> {
                SessionManager.setCurrentUser(fresh);
                UserMainController.refreshUserInfo();
            });

        } catch (Exception ex) {
            showBanner("Error saving profile: " + ex.getMessage(), false);
            return;
        }

        // Refresh display
        populateDisplayMode();
        cancelEdit(null);
        showBanner("Profile updated successfully!", true);
    }

    // ── Password section ──────────────────────────────────────────────────────

    private void wirePasswordListeners() {
        // Enable Update button only when all 3 fields are non-empty
        Runnable checkPwFields = () -> {
            boolean allFilled =
                !getPwValue(currentPwField, currentPwVisible).isEmpty() &&
                !getPwValue(newPwField,     newPwVisible).isEmpty()     &&
                !getPwValue(confirmPwField, confirmPwVisible).isEmpty();
            updatePwButton.setDisable(!allFilled);
        };

        currentPwField.textProperty().addListener((o, ov, nv)  -> checkPwFields.run());
        currentPwVisible.textProperty().addListener((o, ov, nv) -> checkPwFields.run());

        newPwField.textProperty().addListener((o, ov, nv) -> {
            updateStrengthBar(getPwValue(newPwField, newPwVisible));
            updateMatchLabel();
            checkPwFields.run();
        });
        newPwVisible.textProperty().addListener((o, ov, nv) -> {
            updateStrengthBar(getPwValue(newPwField, newPwVisible));
            updateMatchLabel();
            checkPwFields.run();
        });

        confirmPwField.textProperty().addListener((o, ov, nv)  -> { updateMatchLabel(); checkPwFields.run(); });
        confirmPwVisible.textProperty().addListener((o, ov, nv) -> { updateMatchLabel(); checkPwFields.run(); });
    }

    @FXML
    private void updatePassword(ActionEvent event) {
        String currentPw = getPwValue(currentPwField, currentPwVisible);
        String newPw     = getPwValue(newPwField,     newPwVisible);
        String confirmPw = getPwValue(confirmPwField, confirmPwVisible);

        // 1. All fields non-empty (button guard already enforces this, but re-check)
        if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
            showBanner("Please fill in all password fields.", false);
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        UserService svc  = new UserService();

        // 2. Verify current password
        if (!svc.verifyPassword(currentPw, currentUser.getPassword())) {
            showBanner("Current password is incorrect.", false);
            return;
        }

        // 3. Minimum length
        if (newPw.length() < 8) {
            showBanner("New password must be at least 8 characters.", false);
            return;
        }

        // 4. Can't reuse current password
        if (svc.verifyPassword(newPw, currentUser.getPassword())) {
            showBanner("New password must be different from the current one.", false);
            return;
        }

        // 5. Passwords match
        if (!newPw.equals(confirmPw)) {
            showBanner("Passwords do not match.", false);
            return;
        }

        // 6. Hash and persist
        try {
            svc.adminResetPassword(currentUser, newPw);  // hashes + saves via JDBC
            SessionManager.setCurrentUser(currentUser);
        } catch (Exception ex) {
            showBanner("Error updating password: " + ex.getMessage(), false);
            return;
        }

        showBanner("Password updated successfully!", true);

        // Clear fields after 1 second
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            currentPwField.clear();
            currentPwVisible.clear();
            newPwField.clear();
            newPwVisible.clear();
            confirmPwField.clear();
            confirmPwVisible.clear();
            strengthLabel.setText("");
            matchLabel.setText("");
            resetStrengthBar();
        });
        pause.play();
    }

    // ── Eye toggles ───────────────────────────────────────────────────────────

    @FXML
    private void toggleCurrentPw(ActionEvent event) {
        togglePwVisibility(currentPwField, currentPwVisible);
    }

    @FXML
    private void toggleNewPw(ActionEvent event) {
        togglePwVisibility(newPwField, newPwVisible);
    }

    @FXML
    private void toggleConfirmPw(ActionEvent event) {
        togglePwVisibility(confirmPwField, confirmPwVisible);
    }

    private void togglePwVisibility(PasswordField pwField, TextField visibleField) {
        if (pwField.isVisible()) {
            // Switch to text field
            visibleField.setText(pwField.getText());
            pwField.setVisible(false);
            pwField.setManaged(false);
            visibleField.setVisible(true);
            visibleField.setManaged(true);
            visibleField.requestFocus();
            visibleField.positionCaret(visibleField.getText().length());
        } else {
            // Switch back to password field
            pwField.setText(visibleField.getText());
            visibleField.setVisible(false);
            visibleField.setManaged(false);
            pwField.setVisible(true);
            pwField.setManaged(true);
            pwField.requestFocus();
            pwField.positionCaret(pwField.getText().length());
        }
    }

    /** Returns the current text regardless of which field (pw or plain) is active. */
    private String getPwValue(PasswordField pwField, TextField visibleField) {
        return pwField.isVisible() ? pwField.getText() : visibleField.getText();
    }

    // ── Strength bar ─────────────────────────────────────────────────────────

    private void updateStrengthBar(String pw) {
        int level = computeStrength(pw);
        resetStrengthBar();
        switch (level) {
            case 1 -> {
                applyStrengthClass(seg1, "weak");
                strengthLabel.setText("Too short");
                strengthLabel.setStyle("-fx-text-fill: #d63939;");
            }
            case 2 -> {
                applyStrengthClass(seg1, "fair");
                applyStrengthClass(seg2, "fair");
                strengthLabel.setText("Weak");
                strengthLabel.setStyle("-fx-text-fill: #f59f00;");
            }
            case 3 -> {
                applyStrengthClass(seg1, "good");
                applyStrengthClass(seg2, "good");
                applyStrengthClass(seg3, "good");
                strengthLabel.setText("Fair");
                strengthLabel.setStyle("-fx-text-fill: #4dabf7;");
            }
            case 4 -> {
                applyStrengthClass(seg1, "strong");
                applyStrengthClass(seg2, "strong");
                applyStrengthClass(seg3, "strong");
                applyStrengthClass(seg4, "strong");
                strengthLabel.setText("Strong");
                strengthLabel.setStyle("-fx-text-fill: #2fb344;");
            }
            default -> strengthLabel.setText("");
        }
    }

    private int computeStrength(String pw) {
        if (pw == null || pw.isEmpty()) return 0;
        if (pw.length() < 6) return 1;
        boolean hasUpper  = pw.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit  = pw.chars().anyMatch(Character::isDigit);
        if (pw.length() >= 8 && hasUpper && hasDigit) return 4;
        if (hasUpper || hasDigit) return 3;
        return 2;
    }

    private void resetStrengthBar() {
        for (Region seg : new Region[]{seg1, seg2, seg3, seg4}) {
            seg.getStyleClass().removeAll("weak", "fair", "good", "strong");
        }
    }

    private void applyStrengthClass(Region seg, String cls) {
        seg.getStyleClass().removeAll("weak", "fair", "good", "strong");
        seg.getStyleClass().add(cls);
    }

    // ── Match label ───────────────────────────────────────────────────────────

    private void updateMatchLabel() {
        String newPw     = getPwValue(newPwField,    newPwVisible);
        String confirmPw = getPwValue(confirmPwField, confirmPwVisible);

        if (confirmPw.isEmpty()) {
            matchLabel.setText("");
            return;
        }
        if (newPw.equals(confirmPw)) {
            matchLabel.setText("\u2713 Passwords match");
            matchLabel.getStyleClass().remove("field-error");
            matchLabel.getStyleClass().add("field-success");
        } else {
            matchLabel.setText("\u2717 Passwords do not match");
            matchLabel.getStyleClass().remove("field-success");
            matchLabel.getStyleClass().add("field-error");
        }
    }

    // ── Banner ────────────────────────────────────────────────────────────────

    private void showBanner(String message, boolean success) {
        bannerLabel.setText(message);
        bannerLabel.getStyleClass().removeAll("success-banner", "error-banner");
        bannerLabel.getStyleClass().add(success ? "success-banner" : "error-banner");
        bannerLabel.setVisible(true);
        bannerLabel.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            bannerLabel.setVisible(false);
            bannerLabel.setManaged(false);
        });
        pause.play();
    }

    // ── Field error helpers ───────────────────────────────────────────────────

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError(Label errorLabel) {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void clearFieldErrors() {
        clearError(firstNameError);
        clearError(lastNameError);
        clearError(emailError);
    }
}
