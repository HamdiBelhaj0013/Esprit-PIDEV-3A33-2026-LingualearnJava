package org.example.services;

import org.example.util.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service d'appel à l'API Google Gemini pour l'amélioration de publications.
 */
public class GeminiService {

    private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Améliore le titre et le contenu d'une publication via Gemini.
     * Retourne un tableau [newTitre, newContenu].
     */
    public String[] ameliorerPublication(String titre, String contenu) throws Exception {
        String apiKey = AppConfig.get("gemini.api.key").trim();
        if (apiKey.isEmpty() || apiKey.equals("VOTRE_CLE_GEMINI_ICI")) {
            throw new Exception("Clé Gemini non configurée. Éditez src/main/resources/config.properties");
        }

        String prompt = String.format(
            "Améliore ce titre et ce contenu pour qu'ils soient clairs, engageants et professionnels. " +
            "Répond UNIQUEMENT en JSON valide avec les clés 'titre' et 'contenu'. " +
            "Aucun texte avant ou après le JSON.%n%n" +
            "Titre : %s%nContenu : %s",
            titre, contenu
        );

        // Build request body
        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject contentObj = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject body = new JSONObject().put("contents", new JSONArray().put(contentObj));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Gemini API Error " + response.statusCode() + ": " + response.body());
        }

        String rawText = new JSONObject(response.body())
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // Clean markdown if present
        rawText = rawText.replaceAll("(?s)```json\\s*", "")
                         .replaceAll("(?s)```\\s*", "")
                         .trim();

        JSONObject result = new JSONObject(rawText);
        return new String[]{
            result.optString("titre", titre),
            result.optString("contenu", contenu)
        };
    }
}
