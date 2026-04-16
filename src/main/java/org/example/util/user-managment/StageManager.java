package org.example.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Centralises all scene-switching so controllers never need to
 * know about the Stage directly.
 */
public class StageManager {

    private static Stage primaryStage;

    private StageManager() {}

    /** Called once from App.start(). */
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Replaces the current scene with the FXML at {@code fxmlPath}
     * (classpath-relative, e.g. {@code "/fxml/login.fxml"}).
     */
    public static void switchScene(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(StageManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.centerOnScreen();
    }

    /**
     * Switches scene and lets the caller initialise the controller
     * before the scene is shown.
     *
     * <pre>
     * StageManager.switchScene("/fxml/user/UserMain.fxml",
     *     (UserMainController c) -&gt; c.setUser(user));
     * </pre>
     */
    public static <C> C switchScene(String fxmlPath, Consumer<C> init) throws IOException {
        FXMLLoader loader = new FXMLLoader(StageManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        C controller = loader.getController();
        init.accept(controller);
        primaryStage.setScene(new Scene(root));
        primaryStage.centerOnScreen();
        return controller;
    }
}
