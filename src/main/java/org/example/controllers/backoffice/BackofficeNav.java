package org.example.controllers.backoffice;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public final class BackofficeNav {
    private BackofficeNav() {
    }

    public static void navigate(Node source, String fxmlPath, String title) throws IOException {
        Parent root = FXMLLoader.load(BackofficeNav.class.getResource(fxmlPath));
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setScene(new Scene(root, 1100, 700));
        stage.setTitle(title);
    }

    public static void navigateToDashboard(Node source) {
        try {
            navigate(source, "/backoffice/fxml/dashboard.fxml", "LinguaLearn - Dashboard");
        } catch (IOException e) {
            System.err.println("Erreur navigation dashboard: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de navigation");
            alert.setHeaderText("Impossible de charger le dashboard");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}

