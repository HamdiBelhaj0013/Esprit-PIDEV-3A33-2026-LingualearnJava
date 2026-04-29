package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/admin/backoffice-shell.fxml")
        );

        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                getClass().getResource("/css/backoffice.css").toExternalForm()
        );

        stage.setTitle("LinguaLearn Backoffice");
        stage.setScene(scene);

        // Responsive: start maximized, respect minimum size
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setMaximized(true);  // opens fullscreen/maximized like a real admin panel

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}