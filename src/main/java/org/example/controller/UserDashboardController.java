package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.entity.User;

import java.io.IOException;

public class UserDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label userInfoLabel;

    public void setUser(User user) {
        welcomeLabel.setText("Welcome, " + user.getFirstName() + "!");
        userInfoLabel.setText(user.getEmail()
                + "  ·  Plan: " + user.getSubscriptionPlan()
                + (user.isPremium() ? "  ·  Premium" : ""));
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("LinguaLearn");
        stage.setMinWidth(400);
        stage.setMinHeight(500);
        stage.centerOnScreen();
    }
}
