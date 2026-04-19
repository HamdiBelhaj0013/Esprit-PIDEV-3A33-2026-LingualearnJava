package org.example.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'appel à l'API GROQ (LLaMA 3) — utilisé pour le chatbot.
 */
public class GroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Clé de secours — fonctionne même sans variable d'environnement
    private static final String FALLBACK_API_KEY =
            "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";

    // Plusieurs modèles testés en cascade si l'un est indisponible
    private static final String[] MODELS = {
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "llama3-8b-8192",
            "llama3-70b-8192"
    };

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final List<String[]> history = new ArrayList<>();

    /**
     * Envoie un message et retourne la réponse (avec historique de conversation).
     */
    public String chat(String systemPrompt, String userMessage) {
        String apiKey = resolveApiKey();

        history.add(new String[]{"user", userMessage});
        String messagesJson = buildMessagesJson(systemPrompt);

        for (String model : MODELS) {
            String body = "{\"model\":\"" + model + "\","
                    + "\"messages\":[" + messagesJson + "],"
                    + "\"temperature\":0.7}";
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String reply = extractContent(response.body());
                    history.add(new String[]{"assistant", reply});
                    return reply;
                }

                System.out.println("⚠️ GROQ [" + model + "] code " + response.statusCode() + " : " + response.body());

                // 400/404 → modèle indisponible, tenter le suivant
                // 401 → clé invalide, inutile de retenter
                if (response.statusCode() == 401) {
                    return "Clé API GROQ invalide (401). Mettez à jour FALLBACK_API_KEY dans GroqService.";
                }
                if (response.statusCode() == 400 || response.statusCode() == 404) continue;

                return "Service indisponible temporairement (code " + response.statusCode() + ").";

            } catch (Exception ignored) {
                // Tenter le modèle suivant
            }
        }

        return "Désolé, impossible de joindre le service IA. Vérifiez votre connexion.";
    }

    public void clearHistory() {
        history.clear();
    }

    // ── Résolution de la clé API ──────────────────────────────────────────

    private String resolveApiKey() {
        String env = System.getenv("GROQ_API_KEY");
        if (env != null && !env.isBlank()) return env;

        String envGrok = System.getenv("GROK_API_KEY");
        if (envGrok != null && !envGrok.isBlank()) return envGrok;

        String prop = System.getProperty("GROQ_API_KEY");
        if (prop != null && !prop.isBlank()) return prop;

        return FALLBACK_API_KEY;
    }

    // ── Construction du JSON messages (avec historique) ───────────────────

    private String buildMessagesJson(String systemPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"role\":\"system\",\"content\":\"")
          .append(escapeJson(systemPrompt)).append("\"}");

        int start = Math.max(0, history.size() - 10); // garder les 10 derniers
        for (int i = start; i < history.size(); i++) {
            String[] msg = history.get(i);
            sb.append(",{\"role\":\"").append(msg[0])
              .append("\",\"content\":\"").append(escapeJson(msg[1])).append("\"}");
        }
        return sb.toString();
    }

    // ── Extraction du contenu de la réponse JSON ──────────────────────────

    private String extractContent(String json) {
        try {
            int messageIndex = json.indexOf("\"message\"");
            int contentIdx = json.indexOf("\"content\":\"",
                    messageIndex >= 0 ? messageIndex : 0);
            if (contentIdx < 0) return "Aucune réponse générée.";
            int start = contentIdx + 11;
            int end = json.indexOf("\"", start);
            while (end > 0 && json.charAt(end - 1) == '\\') {
                end = json.indexOf("\"", end + 1);
            }
            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .trim();
        } catch (Exception e) {
            return "Réponse reçue, mais format inattendu.";
        }
    }

    // ── Échappement JSON ──────────────────────────────────────────────────

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
