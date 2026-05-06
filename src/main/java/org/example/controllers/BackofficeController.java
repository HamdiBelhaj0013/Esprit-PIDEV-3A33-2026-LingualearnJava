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
        loadPage("/fxml/modules/platform_language.fxml");
    }

    @FXML
    private void showLanguages() {
        loadPage("/fxml/modules/platform_language.fxml");
    }

    @FXML
    private void showCourses() {
        loadPage("/fxml/modules/course.fxml");
    }

    @FXML
    private void showLessons() {
        loadPage("/fxml/modules/lesson.fxml");
    }

    @FXML
    private void showStats() {
        loadPage("/fxml/modules/backoffice-stats.fxml");
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
