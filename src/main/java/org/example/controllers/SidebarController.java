package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import java.io.IOException;

public class SidebarController {

    @FXML private Button btnQuizzes;
    @FXML private Button btnExercises;
    @FXML private Button btnStats;
    @FXML private Button btnDashboard;

    @FXML
    public void initialize() {
        if (!org.example.util.UserSession.isAdmin) {
            if (btnDashboard != null) { btnDashboard.setVisible(false); btnDashboard.setManaged(false); }
            if (btnStats != null) { btnStats.setVisible(false); btnStats.setManaged(false); }
        }
    }

    public void setActive(String page) {
        if ("quizzes".equals(page) && btnQuizzes != null) btnQuizzes.getStyleClass().add("nav-button-active");
        if ("exercises".equals(page) && btnExercises != null) btnExercises.getStyleClass().add("nav-button-active");
        if ("stats".equals(page) && btnStats != null) btnStats.getStyleClass().add("nav-button-active");
    }

    @FXML
    private void goToQuizzes() {
        switchScene("/QuizView.fxml", "Quiz Management");
    }

    @FXML
    private void goToExercises() {
        switchScene("/ExerciceView.fxml", "Exercise Management");
    }

    @FXML
    private void goToStats() {
        switchScene("/StatsView.fxml", "Statistics");
    }

    @FXML
    private void goToDashboard() {
        // Implement dashboard if needed
    }

    private void switchScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) btnQuizzes.getScene().getWindow();
            
            // Re-use the existing scene's stylesheets and just swap the root
            Scene scene = stage.getScene();
            scene.setRoot(root);
            
            stage.setTitle(title);
            // Ensure maximization is preserved
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        switchScene("/LoginView.fxml", "Login - LinguaLearn");
    }
}
