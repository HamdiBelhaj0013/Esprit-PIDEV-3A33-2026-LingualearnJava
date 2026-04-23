package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.example.service.PasswordResetService;

import java.io.IOException;

public class NewPasswordController {

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         passwordError;
    @FXML private Label         confirmError;
    @FXML private Label         errorLabel;

    private String email;
    private String otp;

    public void setEmail(String email) { this.email = email; }
    public void setOtp(String otp)     { this.otp   = otp;   }

    @FXML
    private void handleReset(ActionEvent event) {
        clearAllErrors();

        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        if (password.isBlank()) {
            showFieldError(passwordError, "Password is required.");
            return;
        }
        if (password.length() < 6) {
            showFieldError(passwordError, "Minimum 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showFieldError(confirmError, "Passwords do not match.");
            return;
        }

        try {
            new PasswordResetService().resetPassword(email, otp, password, confirm);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Password reset successfully!");
            alert.showAndWait();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Reset failed. Please try again.");
        }
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearAllErrors() {
        for (Label l : new Label[]{passwordError, confirmError, errorLabel}) {
            l.setText("");
            l.setVisible(false);
            l.setManaged(false);
        }
    }
}
