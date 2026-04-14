package org.example;

import org.example.util.MyDataBase;
import org.example.util.StageManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void init() {
        // Eagerly open the singleton JDBC connection so any config error surfaces at startup.
        MyDataBase.getInstance();
    }

    @Override
    public void start(Stage stage) throws Exception {
        StageManager.setPrimaryStage(stage);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("LinguaLearn");
        stage.setMinWidth(400);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Connection lifecycle is managed by MyDataBase singleton.
    }
}
