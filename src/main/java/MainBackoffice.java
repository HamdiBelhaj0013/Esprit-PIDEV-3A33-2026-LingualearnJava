import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainBackoffice extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/backoffice/fxml/dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/backoffice/css/style.css")).toExternalForm()
        );
        stage.setTitle("LinguaLearn Backoffice");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

