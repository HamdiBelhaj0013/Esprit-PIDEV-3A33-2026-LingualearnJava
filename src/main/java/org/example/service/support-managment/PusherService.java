package org.example.service.supportManagment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PusherService {

    private static final String APP_ID  = "2119719";
    private static final String KEY     = "5f6021b614f01111799d";
    private static final String SECRET  = "2d3ff2faeaadd0925e70";
    private static final String CLUSTER = "eu";

    private static final HttpClient client = HttpClient.newHttpClient();

    public static void notifierUser(int userId, String sujetReclamation) {
        new Thread(() -> {
            try {
                String channel = "user-" + userId;
                String event   = "nouvelle-reponse";

                // ── 1. Data JSON (ce que reçoit le client Pusher) ──────────
                JsonObject dataObj = new JsonObject();
                dataObj.addProperty("message",
                        "L'admin a répondu à votre réclamation : " + sujetReclamation);
                String dataStr = dataObj.toString();

                // ── 2. Payload envoyé à l'API Pusher ──────────────────────
                JsonObject payload = new JsonObject();
                payload.addProperty("name", event);
                payload.addProperty("data", dataStr);
                JsonArray channels = new JsonArray();
                channels.add(channel);
                payload.add("channels", channels);
                String payloadStr = payload.toString();

                // ── 3. Paramètres auth (TRIÉS alphabétiquement) ───────────
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                String bodyMd5   = md5(payloadStr);  // MD5 du payload, pas de dataStr

                // ── 4. String to sign — ordre alphabétique obligatoire ─────
                String stringToSign =
                        "POST" + "\n" +
                                "/apps/" + APP_ID + "/events" + "\n" +
                                "auth_key="        + KEY       +
                                "&auth_timestamp=" + timestamp +
                                "&auth_version=1.0" +
                                "&body_md5="       + bodyMd5;

                String signature = hmacSha256(SECRET, stringToSign);

                // ── 5. URL avec query params ───────────────────────────────
                String url = "https://api-" + CLUSTER + ".pusher.com/apps/" + APP_ID + "/events"
                        + "?auth_key="        + KEY
                        + "&auth_timestamp="  + timestamp
                        + "&auth_version=1.0"
                        + "&body_md5="        + bodyMd5
                        + "&auth_signature="  + signature;

                // ── 6. Envoi HTTP POST ─────────────────────────────────────
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payloadStr))
                        .build();

                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("Pusher status : " + response.statusCode());
                System.out.println("Pusher body   : " + response.body());

                if (response.statusCode() == 200) {
                    System.out.println("Pusher OK — notif envoyée à user-" + userId);
                } else {
                    System.out.println("Pusher ERREUR — vérifie les credentials");
                }

            } catch (Exception e) {
                System.err.println("Erreur PusherService: " + e.getMessage());
            }
        }).start();
    }

    // ── MD5 sur le payload complet ─────────────────────────────────────────
    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    // ── HMAC-SHA256 ────────────────────────────────────────────────────────
    private static String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}