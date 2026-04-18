package org.example.services;

import org.example.util.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service d'appel à l'API GROQ (LLaMA 3) — utilisé pour le chatbot.
 */
public class GroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Envoie un message au chatbot et retourne la réponse.
     */
    public String chat(String systemPrompt, String userMessage) throws Exception {
        String apiKey = AppConfig.get("groq.api.key").trim();
        if (apiKey.isEmpty() || apiKey.equals("VOTRE_CLE_GROQ_ICI")) {
            throw new Exception("Clé GROQ non configurée. Éditez src/main/resources/config.properties");
        }

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("stream", false);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userMessage));
        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Erreur API GROQ " + response.statusCode() + ":\n" + response.body());
        }

        return new JSONObject(response.body())
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
}
