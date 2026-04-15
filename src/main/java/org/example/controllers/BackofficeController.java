package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class BackofficeController {

    @FXML
    private StackPane contentPane;

    @FXML
    public void initialize() {
        loadPage("/views/platform_language.fxml");
    }

    @FXML
    private void showLanguages() {
        loadPage("/views/platform_language.fxml");
    }

    @FXML
    private void showCourses() {
        loadPage("/views/course.fxml");
    }

    @FXML
    private void showLessons() {
        loadPage("/views/lesson.fxml");
    }

    @FXML
    private void showStats() {
        loadPage("/views/backoffice-stats.fxml");
    }

    private void loadPage(String path) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(path));
            contentPane.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}