package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.service.tests.AntiCheatApiServer;
import org.example.service.tests.CertificateApiServer;
import org.example.service.user_managment.CaptchaServer;
import org.example.util.MyDataBase;
import org.example.util.StageManager;
import org.example.webhook.StripeWebhookServer;

public class App extends Application {

    private StripeWebhookServer webhookServer;

    @Override
    public void init() throws Exception {
        MyDataBase.getInstance();
        CertificateApiServer.start();
        AntiCheatApiServer.start();
        CaptchaServer.start();
        String webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET");
        webhookServer = new StripeWebhookServer(webhookSecret);
        webhookServer.start();
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
        CertificateApiServer.stop();
        AntiCheatApiServer.stop();
        CaptchaServer.stop();
        if (webhookServer != null) webhookServer.stop();
        MyDataBase.getInstance().closeConnection();
    }
}