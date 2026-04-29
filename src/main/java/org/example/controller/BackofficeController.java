package org.example.controller;

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
        loadPage("/fxml/admin/platform_language.fxml");
    }

    @FXML
    private void showLanguages() {
        loadPage("/fxml/admin/platform_language.fxml");
    }

    @FXML
    private void showCourses() {
        loadPage("/fxml/admin/course.fxml");
    }

    @FXML
    private void showLessons() {
        loadPage("/fxml/admin/lesson.fxml");
    }

    @FXML
    private void showStats() {
        loadPage("/fxml/admin/backoffice-stats.fxml");
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