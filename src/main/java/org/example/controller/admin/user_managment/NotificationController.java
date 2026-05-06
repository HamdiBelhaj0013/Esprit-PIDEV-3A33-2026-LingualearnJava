package org.example.controller.admin.user_managment;

import org.example.entity.User;
import org.example.service.user_managment.NotificationService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.example.repository.UserRepository;
import org.example.service.ai.AdminAiService;

public class NotificationController {

    @FXML private Label       userNameLabel;
    @FXML private ChoiceBox<String> typeChoice;
    @FXML private TextArea    messageArea;
    @FXML private Label       errorLabel;

    private User     user;
    private Runnable onSent;

    // AI-FEATURE: notification-writer
    @FXML private TextField intentField;
    @FXML private Button    generateMessageBtn;
    @FXML private Label     aiStatusLabel;

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

    // AI-FEATURE: notification-writer ─────────────────────────────────────────

    @FXML
    private void handleGenerateMessage(ActionEvent event) {
        String intent = intentField.getText().trim();
        if (intent.isBlank()) {
            showAiStatus("Describe the intent first.", true);
            return;
        }
        String type = typeChoice.getValue() != null ? typeChoice.getValue() : "info";
        generateMessageBtn.setDisable(true);
        showAiStatus("Generating…", false);

        Thread t = new Thread(() -> {
            try {
                AdminAiService svc = new AdminAiService(new UserRepository());
                String result = svc.generateNotificationMessage(user.getId(), type, intent);
                Platform.runLater(() -> {
                    messageArea.setText(result);
                    showAiStatus("", false);
                    generateMessageBtn.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showAiStatus("AI service unavailable. Make sure Ollama is running.", true);
                    generateMessageBtn.setDisable(false);
                });
            }
        }, "notification-writer-thread");
        t.setDaemon(true);
        t.start();
    }

    private void showAiStatus(String msg, boolean isError) {
        if (msg.isBlank()) {
            aiStatusLabel.setVisible(false);
            aiStatusLabel.setManaged(false);
        } else {
            aiStatusLabel.setText(msg);
            aiStatusLabel.setStyle(isError
                ? "-fx-font-size:11px; -fx-text-fill:#ef4444;"
                : "-fx-font-size:11px; -fx-text-fill:#6b7280;");
            aiStatusLabel.setVisible(true);
            aiStatusLabel.setManaged(true);
        }
    }
}
