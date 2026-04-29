package org.example.service.supportManagment;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

public class PusherListenerService {

    private static final String KEY     = "5f6021b614f01111799d";
    private static final String CLUSTER = "eu";
    private static Pusher pusher;

    public static void demarrer(int userId, NotificationCallback callback) {
        if (pusher != null) arreter();

        PusherOptions options = new PusherOptions();
        options.setCluster(CLUSTER);
        options.setUseTLS(true);

        pusher = new Pusher(KEY, options);

        String channelName = "user-" + userId;

        // ── Connexion avec listener — s'abonne APRÈS connexion établie ──
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                System.out.println("Pusher état : "
                        + change.getPreviousState()
                        + " → " + change.getCurrentState());

                // S'abonner seulement quand CONNECTED
                if (change.getCurrentState() == ConnectionState.CONNECTED) {
                    Channel channel = pusher.subscribe(channelName);

                    channel.bind("nouvelle-reponse", event -> {
                        try {
                            String dataStr = event.getData();
                            System.out.println("Pusher reçu : " + dataStr);

                            JsonObject data = JsonParser.parseString(dataStr)
                                    .getAsJsonObject();
                            String message = data.has("message")
                                    ? data.get("message").getAsString()
                                    : "Nouvelle réponse reçue";

                            Platform.runLater(() -> callback.onNotification(message));

                        } catch (Exception e) {
                            System.err.println("Pusher parse erreur : " + e.getMessage());
                            Platform.runLater(() ->
                                    callback.onNotification("Nouvelle notification reçue"));
                        }
                    });

                    System.out.println("Pusher abonné sur : " + channelName);
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                System.out.println("Pusher erreur : " + message + " code=" + code);
            }

        }, ConnectionState.ALL);
    }

    public static void arreter() {
        if (pusher != null) {
            try {
                pusher.disconnect();
            } catch (Exception e) {
                System.out.println("Pusher arrêt : " + e.getMessage());
            }
            pusher = null;
        }
    }

    public interface NotificationCallback {
        void onNotification(String message);
    }
}