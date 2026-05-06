package org.example.service.forum;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Service d'appel Ã  l'API GROQ (LLaMA 3) â€” utilisÃ© pour le chatbot.
 */
public class GroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // ClÃ© de secours â€” fonctionne mÃªme sans variable d'environnement
    private static final String FALLBACK_API_KEY =
            "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";

    // Plusieurs modÃ¨les testÃ©s en cascade si l'un est indisponible
    private static final String[] MODELS = {
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "llama3-8b-8192",
            "llama3-70b-8192"
    };

    private final HttpClient client = createTrustAllClient();

    private static HttpClient createTrustAllClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String t) {}
                    public void checkServerTrusted(X509Certificate[] certs, String t) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            return HttpClient.newBuilder()
                    .sslContext(sc)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
        } catch (Exception e) {
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
        }
    }

    private final List<String[]> history = new ArrayList<>();

    /**
     * Envoie un message et retourne la rÃ©ponse (avec historique de conversation).
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

                System.out.println("âš ï¸ GROQ [" + model + "] code " + response.statusCode() + " : " + response.body());

                // 400/404 â†’ modÃ¨le indisponible, tenter le suivant
                // 401 â†’ clÃ© invalide, inutile de retenter
                if (response.statusCode() == 401) {
                    return "ClÃ© API GROQ invalide (401). Mettez Ã  jour FALLBACK_API_KEY dans GroqService.";
                }
                if (response.statusCode() == 400 || response.statusCode() == 404) continue;

                return "Service indisponible temporairement (code " + response.statusCode() + ").";

            } catch (Exception ignored) {
                // Tenter le modÃ¨le suivant
            }
        }

        return "DÃ©solÃ©, impossible de joindre le service IA. VÃ©rifiez votre connexion.";
    }

    public void clearHistory() {
        history.clear();
    }

    // â”€â”€ RÃ©solution de la clÃ© API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String resolveApiKey() {
        String env = System.getenv("GROQ_API_KEY");
        if (env != null && !env.isBlank()) return env;

        String envGrok = System.getenv("GROK_API_KEY");
        if (envGrok != null && !envGrok.isBlank()) return envGrok;

        String prop = System.getProperty("GROQ_API_KEY");
        if (prop != null && !prop.isBlank()) return prop;

        return FALLBACK_API_KEY;
    }

    // â”€â”€ Construction du JSON messages (avec historique) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ Extraction du contenu de la rÃ©ponse JSON â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String extractContent(String json) {
        try {
            int messageIndex = json.indexOf("\"message\"");
            int contentIdx = json.indexOf("\"content\":\"",
                    messageIndex >= 0 ? messageIndex : 0);
            if (contentIdx < 0) return "Aucune rÃ©ponse gÃ©nÃ©rÃ©e.";
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
            return "RÃ©ponse reÃ§ue, mais format inattendu.";
        }
    }

    // â”€â”€ Ã‰chappement JSON â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

