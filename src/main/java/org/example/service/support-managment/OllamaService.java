package org.example.service.supportManagment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaService {

    private static final String URL   = "http://localhost:11434/api/generate";
    private static final String MODEL = "phi3";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static String analyserSentiment(String texte) {
        try {
            if (!estDisponible()) {
                System.out.println("Ollama non démarré — sentiment ignoré");
                return "NEUTRE";
            }

            String prompt =
                    "Classify this message sentiment. " +
                            "Reply with EXACTLY one word: NEGATIF, POSITIF, NEUTRE, or URGENT. " +
                            "URGENT if contains: urgent, help, immediately, problem, broken. " +
                            "NEGATIF if negative/angry. POSITIF if happy/satisfied. NEUTRE otherwise. " +
                            "Message: \"" + texte + "\"\n" +
                            "One word answer:";

            JsonObject body = new JsonObject();
            body.addProperty("model",  MODEL);
            body.addProperty("prompt", prompt);
            body.addProperty("stream", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body())
                        .getAsJsonObject();
                String result = json.get("response")
                        .getAsString().trim().toUpperCase();

                // ── DEBUG — voir exactement ce que répond phi3 ──
                System.out.println("=== OLLAMA BRUTE === [" + result + "]");

                // ── Normalisation ──────────────────────────────
                if (result.contains("URGENT"))                                return "URGENT";
                if (result.contains("NEGATIF") || result.contains("NEGATIVE")) return "NEGATIF";
                if (result.contains("POSITIF") || result.contains("POSITIVE")) return "POSITIF";
                if (result.contains("NEUTRE")  || result.contains("NEUTRAL"))  return "NEUTRE";

                // Cherche mot par mot si pas trouvé directement
                for (String mot : result.split("\\s+")) {
                    String m = mot.replaceAll("[^A-Z]", "");
                    if (m.equals("URGENT"))                           return "URGENT";
                    if (m.equals("NEGATIF") || m.equals("NEGATIVE")) return "NEGATIF";
                    if (m.equals("POSITIF") || m.equals("POSITIVE")) return "POSITIF";
                    if (m.equals("NEUTRE")  || m.equals("NEUTRAL"))  return "NEUTRE";
                }

                System.out.println("=== AUCUN MOT RECONNU — retourne NEUTRE ===");
                return "NEUTRE";

            } else {
                System.out.println("Ollama HTTP erreur : " + response.statusCode());
                return "NEUTRE";
            }

        } catch (Exception e) {
            System.out.println("Ollama erreur : " + e.getMessage());
            return "NEUTRE";
        }
    }

    public static boolean estDisponible() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}