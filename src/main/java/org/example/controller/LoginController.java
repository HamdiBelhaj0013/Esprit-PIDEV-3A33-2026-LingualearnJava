package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.controller.admin.AdminMainController;
import org.example.entity.User;
import org.example.util.MyDataBase;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir votre email et mot de passe.");
            return;
        }

        try {
            User user = findUserByEmail(email);

            if (user == null) {
                showError("Email ou mot de passe incorrect.");
                return;
            }

            if (!"active".equals(user.getStatus())) {
                showError("Votre compte est suspendu ou inactif.");
                return;
            }

            // Vérification BCrypt (support hash $2y$ Symfony)
            String hash = user.getPassword();
            if (hash != null && hash.startsWith("$2y$"))
                hash = "$2a$" + hash.substring(4);

            if (!BCrypt.checkpw(password, hash)) {
                showError("Email ou mot de passe incorrect.");
                return;
            }

            boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
            navigateToDashboard(user, isAdmin);

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private User findUserByEmail(String email) throws Exception {
        Connection conn = MyDataBase.getInstance().getConnection();
        String sql = """
                SELECT id, email, password, roles, first_name, last_name,
                       status, subscription_plan, subscription_expiry,
                       is_premium, is_verified, is_banned, ban_reason,
                       created_at, last_payment_status,
                       stripe_customer_id, stripe_subscription_id
                FROM users
                WHERE LOWER(email) = LOWER(?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                User u = new User();
                u.setId(rs.getLong("id"));
                u.setEmail(rs.getString("email"));
                u.setPassword(rs.getString("password"));
                u.setFirstName(rs.getString("first_name"));
                u.setLastName(rs.getString("last_name"));
                u.setStatus(rs.getString("status"));
                u.setSubscriptionPlan(rs.getString("subscription_plan"));
                u.setVerified(rs.getBoolean("is_verified"));
                u.setIsBanned(rs.getBoolean("is_banned"));
                u.setBanReason(rs.getString("ban_reason"));
                u.setLastPaymentStatus(rs.getString("last_payment_status"));
                u.setStripeCustomerId(rs.getString("stripe_customer_id"));
                u.setStripeSubscriptionId(rs.getString("stripe_subscription_id"));

                Timestamp expiry = rs.getTimestamp("subscription_expiry");
                if (expiry != null)
                    u.setSubscriptionExpiry(expiry.toLocalDateTime());

                // Parse roles JSON ["ROLE_USER","ROLE_ADMIN"]
                String rolesJson = rs.getString("roles");
                if (rolesJson != null && !rolesJson.isBlank()) {
                    String trimmed = rolesJson.trim().replaceAll("^\\[|]$", "");
                    List<String> roles = new ArrayList<>();
                    for (String part : trimmed.split(",")) {
                        String r = part.trim().replace("\"", "");
                        if (!r.isBlank()) roles.add(r);
                    }
                    u.setRoles(roles);
                }
                return u;
            }
        }
    }

    private void navigateToDashboard(User user, boolean isAdmin) throws IOException {
        Stage stage = (Stage) emailField.getScene().getWindow();

        if (isAdmin) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/AdminMain.fxml"));
            Parent root = loader.load();
            AdminMainController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.setScene(new Scene(root));
            stage.setTitle("LinguaLearn — Admin");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
        } else {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/UserDashboard.fxml"));
            Parent root = loader.load();
            UserDashboardController ctrl = loader.getController();
            ctrl.setUser(user);
            ctrl.setStage(stage);
            stage.setScene(new Scene(root));
            stage.setTitle("LinguaLearn — Dashboard");
            stage.setMinWidth(800);
            stage.setMinHeight(600);
        }
        stage.centerOnScreen();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}