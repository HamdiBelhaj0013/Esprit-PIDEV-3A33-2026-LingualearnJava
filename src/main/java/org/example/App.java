package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.service.tests.AntiCheatApiServer;
import org.example.service.tests.CertificateApiServer;
import org.example.util.MyDataBase;

public class App extends Application {

    @Override
    public void init() throws Exception {
        // 1. Connexion base de données
        MyDataBase.getInstance();
        // 2. API Certificats    → http://localhost:9090/api/certificate/verify/{uuid}
        CertificateApiServer.start();
        // 3. API Anti-Triche   → http://localhost:9091/api/anticheat/logs
        AntiCheatApiServer.start();
    }

    @Override
    public void start(Stage stage) throws Exception {
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
        // Arrêt propre des deux serveurs
        CertificateApiServer.stop();
        AntiCheatApiServer.stop();
        MyDataBase.getInstance().closeConnection();
    }
}