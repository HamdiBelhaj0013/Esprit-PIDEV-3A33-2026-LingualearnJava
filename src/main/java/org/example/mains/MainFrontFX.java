package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

public class MainFrontFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/frontoffice/front-shell.fxml")
        );

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        double width = Math.min(1220, bounds.getWidth() - 80);
        double height = Math.min(760, bounds.getHeight() - 80);

        Scene scene = new Scene(loader.load(), width, height);
        scene.getStylesheets().add(
                getClass().getResource("/css/frontoffice.css").toExternalForm()
        );

        stage.setTitle("LinguaLearn Front Office");
        stage.setScene(scene);

        stage.setMinWidth(1000);
        stage.setMinHeight(680);

        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}