package org.example.service.supportManagment;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

public class PusherListenerService {

    private static final String KEY     = "5f6021b614f01111799d";
    private static final String CLUSTER = "eu";

    private static Pusher  pusher;
    private static int     currentUserId = -1;  // ✅ évite double subscribe

    public static void demarrer(int userId, NotificationCallback callback) {
        // ✅ Si déjà connecté pour ce user — ne pas re-subscribe
        if (pusher != null && currentUserId == userId) return;

        arreter();
        currentUserId = userId;

        PusherOptions options = new PusherOptions().setCluster(CLUSTER);
        pusher = new Pusher(KEY, options);
        pusher.connect();

        String channelName = "user-" + userId;
        Channel channel = pusher.subscribe(channelName);

        channel.bind("nouvelle-reponse", event -> {
            try {
                String dataStr = event.getData();
                JsonObject data = JsonParser.parseString(dataStr).getAsJsonObject();
                String message = data.has("message")
                        ? data.get("message").getAsString()
                        : "Nouvelle réponse reçue";
                Platform.runLater(() -> callback.onNotification(message));
            } catch (Exception e) {
                System.err.println("Erreur parsing Pusher: " + e.getMessage());
            }
        });

        System.out.println("Pusher: écoute sur channel " + channelName);
    }

    public static void arreter() {
        if (pusher != null) {
            try { pusher.disconnect(); } catch (Exception ignored) {}
            pusher = null;
            currentUserId = -1;
        }
    }

    public interface NotificationCallback {
        void onNotification(String message);
    }
}