package org.example.controller.admin;

import org.example.entity.User;
import org.example.service.NotificationService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class NotificationController {

    @FXML private Label       userNameLabel;
    @FXML private ChoiceBox<String> typeChoice;
    @FXML private TextArea    messageArea;
    @FXML private Label       errorLabel;

    private User     user;
    private Runnable onSent;

    @FXML
    private void initialize() {
        typeChoice.setItems(FXCollections.observableArrayList(
            "info", "warning", "success", "system"));
        typeChoice.setValue("info");
    }

    public void setUser(User u, Runnable onSent) {
        this.user   = u;
        this.onSent = onSent;
        userNameLabel.setText("To: " + u.getFullName());
    }

    @FXML
    private void handleSend(ActionEvent event) {
        String type    = typeChoice.getValue();
        String message = messageArea.getText().trim();

        if (message.isBlank()) {
            showError("Message cannot be empty.");
            return;
        }

        try {
            new NotificationService().sendNotification(user.getId(), type, message);
            closeStage();
            if (onSent != null) onSent.run();
        } catch (Exception ex) {
            showError("Failed to send: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeStage();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void closeStage() {
        ((Stage) messageArea.getScene().getWindow()).close();
    }
}
