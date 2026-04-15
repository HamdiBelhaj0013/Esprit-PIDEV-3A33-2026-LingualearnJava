package org.example.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class FrontRouter {

    private static StackPane contentPane;

    public static void setContentPane(StackPane pane) {
        contentPane = pane;
    }

    public static void goTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(FrontRouter.class.getResource(fxmlPath));
            Node node = loader.load();
            contentPane.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}