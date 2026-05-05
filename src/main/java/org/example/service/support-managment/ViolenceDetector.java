package org.example.service.supportManagment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;

public class ViolenceDetector {

    private static final String URL   = "http://localhost:11434/api/generate";
    private static final String MODEL = "llava";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static boolean contientSang(String imagePath) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) return false;

            // Convertir image en Base64
            byte[] imageBytes = Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Prompt pour llava
            String prompt =
                "Analyze this image carefully. " +
                "Does this image contain blood, violence, gore, or disturbing violent content? " +
                "Reply with ONLY one word: YES or NO. " +
                "Nothing else, just YES or NO.";

            // Construire la requête JSON
            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.addProperty("prompt", prompt);
            body.addProperty("stream", false);

            // Ajouter l'image en base64
            com.google.gson.JsonArray images = new com.google.gson.JsonArray();
            images.add(base64Image);
            body.add("images", images);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body())
                        .getAsJsonObject();
                String result = json.get("response")
                        .getAsString().trim().toUpperCase();

                System.out.println("=== LLAVA RÉPONSE: " + result + " ===");

                return result.contains("YES");
            }

        } catch (Exception e) {
            System.err.println("ViolenceDetector erreur: " + e.getMessage());
        }
        return false;
    }
}
