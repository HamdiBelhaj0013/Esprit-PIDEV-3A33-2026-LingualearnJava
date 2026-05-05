package org.example;

import org.example.util.MyDataBase;
import org.example.util.StageManager;
import org.example.webhook.StripeWebhookServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private StripeWebhookServer webhookServer;

    // Static flag so other parts of the app can check if payments are available
    public static boolean paymentsEnabled = false;

    @Override
    public void init() throws Exception {
        // ── Database ──────────────────────────────────────────────────────────
        MyDataBase.getInstance();

        // ── Stripe webhook HTTP server (optional) ─────────────────────────────
        String webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET");
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            webhookServer = new StripeWebhookServer(webhookSecret);
            webhookServer.start();
            paymentsEnabled = true;
            System.out.println("[Stripe] Webhook server started. Payments enabled.");
        } else {
            System.out.println("[Stripe] STRIPE_WEBHOOK_SECRET not set — payments disabled.");
        }
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
        if (webhookServer != null) {
            webhookServer.stop();
        }
        MyDataBase.getInstance().closeConnection();
    }
}