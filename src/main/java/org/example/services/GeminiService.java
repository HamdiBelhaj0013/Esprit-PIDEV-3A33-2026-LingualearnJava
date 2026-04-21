package org.example.services;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service d'amélioration de publications via GROQ (LLaMA 3).
 * Cle API dediee a l'amelioration, hardcodee ici.
 */
public class GeminiService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String AMELIORATION_API_KEY = "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";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Améliore le titre et le contenu d'une publication via GROQ.
     * Retourne [nouveauTitre, nouveauContenu].
     */
    public String[] ameliorerPublication(String titre, String contenu) throws Exception {
        String systemPrompt = "Tu es un expert en rédaction. Améliore le titre et le contenu pour qu'ils soient clairs, engageants et professionnels. "
                + "Réponds UNIQUEMENT en JSON valide avec les clés 'titre' et 'contenu'. Aucun texte avant ou après le JSON.";

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("stream", false);
        body.put("temperature", 0.3);
        body.put("messages", new org.json.JSONArray()
                .put(new JSONObject().put("role", "system").put("content", systemPrompt))
                .put(new JSONObject().put("role", "user").put("content", "Titre : " + titre + "\nContenu : " + contenu))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + AMELIORATION_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Erreur GROQ amelioration (" + response.statusCode() + "): " + response.body());
        }

        String rawText = new JSONObject(response.body())
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        // Nettoyer le markdown si présent
        rawText = rawText.replaceAll("(?s)```json\\s*", "")
                         .replaceAll("(?s)```\\s*", "")
                         .trim();

        System.out.println("🔍 Réponse IA brute : " + rawText);

        // Parser le JSON avec org.json (déjà dans pom.xml)
        try {
            JSONObject json = new JSONObject(rawText);
            String newTitre = json.optString("titre", titre);
            String newContenu = json.optString("contenu", contenu);

            System.out.println("✅ Amélioration appliquée");
            System.out.println("  Titre : " + newTitre);
            System.out.println("  Contenu : " + newContenu);

            return new String[]{newTitre, newContenu};

        } catch (Exception e) {
            System.out.println("⚠️ Erreur parsing JSON: " + e.getMessage());
            System.out.println("  Texte non valide: " + rawText);
            throw new Exception("Impossible de parser la reponse IA. Verifiez le format JSON.");
        }
    }
}
