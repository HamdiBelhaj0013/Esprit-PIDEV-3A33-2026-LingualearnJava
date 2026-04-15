import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.Session;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Session.setCurrentUser(1, "ROLE_USER");

        Parent userRoot = FXMLLoader.load(getClass().getResource("/views/user_view.fxml"));
        Stage userStage = new Stage();
        userStage.setTitle("LinguaLearn — Mon espace");
        userStage.setScene(new Scene(userRoot, 820, 640));
        userStage.show();

        Parent adminRoot = FXMLLoader.load(getClass().getResource("/views/admin_view.fxml"));
        stage.setTitle("LinguaLearn — Administration");
        stage.setScene(new Scene(adminRoot, 900, 680));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
