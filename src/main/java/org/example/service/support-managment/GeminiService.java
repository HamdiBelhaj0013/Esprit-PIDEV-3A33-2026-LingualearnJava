package org.example.service.supportManagment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService {

    private static final String API_KEY = "AIzaSyC7u03iYXlr0No-8DfJsrhXVn7m8jxCfNs";
    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    private static final HttpClient client = HttpClient.newHttpClient();

    private static String ask(String prompt) {
        try {
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            JsonArray parts = new JsonArray();
            parts.add(part);
            JsonObject content = new JsonObject();
            content.add("parts", parts);
            JsonArray contents = new JsonArray();
            contents.add(content);
            JsonObject body = new JsonObject();
            body.add("contents", contents);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();

            // ✅ Parse robuste avec vérification à chaque étape
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("error")) {
                System.err.println("Gemini API error: " + json.get("error"));
                return null;
            }

            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            JsonObject candidate = candidates.get(0).getAsJsonObject();
            if (!candidate.has("content")) return null;

            JsonArray partsArr = candidate.getAsJsonObject("content").getAsJsonArray("parts");
            if (partsArr == null || partsArr.isEmpty()) return null;

            return partsArr.get(0).getAsJsonObject().get("text").getAsString().trim();

        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur réseau Gemini: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Erreur parsing Gemini: " + e.getMessage());
            return null;
        }
    }

    public static String traduire(String texte, String langueSource, String langueCible) {
        if (texte == null || texte.isBlank()) return texte;
        String prompt = "Traduis ce texte vers " + langueCible +
                ". Réponds UNIQUEMENT avec la traduction, sans explication.\n\nTexte: " + texte;
        return ask(prompt);
    }

    public static String corriger(String texte) {
        if (texte == null || texte.isBlank()) return texte;
        String prompt = "Corrige les fautes d'orthographe et de grammaire dans ce texte." +
                " Réponds UNIQUEMENT avec le texte corrigé, sans explication.\n\nTexte: " + texte;
        return ask(prompt);
    }

    public static String suggererReponse(String sujet, String message) {
        if (sujet == null || message == null) return null;
        String prompt = "Tu es un agent de support. Génère une réponse professionnelle et concise " +
                "pour cette réclamation. Réponds UNIQUEMENT avec la réponse.\n\n" +
                "Sujet: " + sujet + "\nMessage: " + message;
        return ask(prompt);
    }
}