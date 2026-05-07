package org.example.service.tests;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Service IA — Writing Test (Groq / LLaMA 3.3-70b)
 *
 * Deux responsabilités :
 *   1. genererSujet()      → Groq génère un sujet de rédaction adapté au niveau + langue
 *   2. corrigerRedaction() → Groq corrige la copie et retourne note/20 + feedback détaillé
 */
public class GroqWritingService {

    private static final Logger LOG      = Logger.getLogger(GroqWritingService.class.getName());
    private static final String API_KEY  = "......................"; // ← remplace par ta clé complète
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    // ─────────────────────────────────────────────────────────────────────────
    //  RECORD résultat correction
    // ─────────────────────────────────────────────────────────────────────────

    public record WritingFeedback(
            int    noteSur20,
            float  scorePct,
            String grammaire,
            String coherence,
            String vocabulaire,
            String suggestions,
            String correctionGlobale
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    //  1. GÉNÉRATION DU SUJET
    // ─────────────────────────────────────────────────────────────────────────

    public String genererSujet(String niveau, String langue, int motsCibles) throws Exception {
        String prompt = buildSujetPrompt(niveau, langue, motsCibles);
        return callGroq(prompt).trim();
    }

    private String buildSujetPrompt(String niveau, String langue, int mots) {
        String langueCode = detecterLangue(langue);
        return """
            Tu es un professeur de %s expert en certification linguistique CECRL.
            Génère UN sujet de rédaction pour un apprenant de niveau %s.
            
            Contraintes strictes :
            - Le sujet doit être rédigé EN %s (la langue cible)
            - Adapté au niveau %s : vocabulaire et complexité appropriés
            - L'apprenant devra écrire environ %d mots
            - Le sujet doit être stimulant et concret (vie quotidienne, société, culture)
            - Format de réponse OBLIGATOIRE :
              TITRE: [titre court du sujet]
              CONSIGNE: [instruction claire de 2-3 phrases]
              CONSEIL: [un conseil de style ou de structure pour ce niveau]
            
            Ne donne que le sujet, sans commentaire supplémentaire.
            """.formatted(langue, niveau, langueCode, niveau, mots);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. CORRECTION DE LA RÉDACTION
    // ─────────────────────────────────────────────────────────────────────────

    public WritingFeedback corrigerRedaction(String sujet, String redaction,
                                             String niveau, String langue) throws Exception {
        if (redaction == null || redaction.isBlank()) {
            return new WritingFeedback(0, 0f,
                    "Aucun texte soumis.",
                    "Aucun texte soumis.",
                    "Aucun texte soumis.",
                    "Veuillez rédiger un texte avant de soumettre.",
                    "Note : 0/20 — Aucune rédaction soumise.");
        }
        String prompt = buildCorrectionPrompt(sujet, redaction, niveau, langue);
        return parseCorrection(callGroq(prompt));
    }

    private String buildCorrectionPrompt(String sujet, String redaction,
                                         String niveau, String langue) {
        return """
            Tu es un correcteur expert en %s, spécialisé dans l'évaluation CECRL niveau %s.
            
            SUJET DONNÉ À L'APPRENANT :
            %s
            
            RÉDACTION DE L'APPRENANT :
            %s
            
            Évalue cette rédaction selon les critères CECRL pour le niveau %s.
            Réponds UNIQUEMENT avec ce format exact (respecte les balises) :
            
            NOTE: [chiffre de 0 à 20]
            
            GRAMMAIRE: [Analyse précise des erreurs grammaticales. Cite les phrases fautives entre guillemets et donne la correction. Ex: "Il sont allé" doit être "Ils sont allés". Si aucune erreur, dis-le.]
            
            COHERENCE: [Analyse de la structure, de l'organisation des idées, de la logique du texte. Cite des exemples précis du texte de l'apprenant.]
            
            VOCABULAIRE: [Analyse du vocabulaire utilisé. Signale les mots trop simples pour le niveau et propose des alternatives. Cite des exemples.]
            
            SUGGESTIONS: [3 conseils concrets pour améliorer ce type de rédaction au niveau %s.]
            
            CORRECTION: [En 2-3 phrases, résumé global des points forts et axes d'amélioration.]
            """.formatted(langue, niveau, sujet, redaction, niveau, niveau);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PARSING
    // ─────────────────────────────────────────────────────────────────────────

    private WritingFeedback parseCorrection(String text) {
        int    note        = extractInt(text, "NOTE:");
        String grammaire   = extractSection(text, "GRAMMAIRE:",   "COHERENCE:");
        String coherence   = extractSection(text, "COHERENCE:",   "VOCABULAIRE:");
        String vocabulaire = extractSection(text, "VOCABULAIRE:", "SUGGESTIONS:");
        String suggestions = extractSection(text, "SUGGESTIONS:", "CORRECTION:");
        String correction  = extractSection(text, "CORRECTION:",  null);

        note = Math.max(0, Math.min(20, note));
        float scorePct = note / 20f * 100f;

        return new WritingFeedback(note, scorePct,
                grammaire.isBlank()   ? "Aucune erreur grammaticale majeure détectée." : grammaire,
                coherence.isBlank()   ? "Structure correcte pour ce niveau."           : coherence,
                vocabulaire.isBlank() ? "Vocabulaire adapté au niveau."                : vocabulaire,
                suggestions.isBlank() ? "Continuez à pratiquer régulièrement."         : suggestions,
                correction.isBlank()  ? "Résultat : " + note + "/20"                   : correction);
    }

    private int extractInt(String text, String key) {
        int idx = text.indexOf(key);
        if (idx < 0) return 10;
        String sub = text.substring(idx + key.length()).trim();
        StringBuilder sb = new StringBuilder();
        for (char c : sub.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
            else if (!sb.isEmpty()) break;
        }
        try { return Integer.parseInt(sb.toString()); }
        catch (NumberFormatException e) { return 10; }
    }

    private String extractSection(String text, String startKey, String endKey) {
        int start = text.indexOf(startKey);
        if (start < 0) return "";
        start += startKey.length();
        int end = endKey != null ? text.indexOf(endKey, start) : -1;
        return ((end > start) ? text.substring(start, end) : text.substring(start)).trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HTTP — Appel Groq (format OpenAI-compatible)
    // ─────────────────────────────────────────────────────────────────────────

    private String callGroq(String prompt) throws Exception {
        String safePrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.7,
                  "max_tokens": 1024
                }
                """.formatted(MODEL, safePrompt);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 401) {
            LOG.severe("Groq 401 — clé API invalide : " + resp.body());
            throw new Exception("Clé API Groq invalide. Vérifie dans GroqWritingService.java");
        }
        if (resp.statusCode() == 429) {
            LOG.warning("Groq 429 — rate limit : " + resp.body());
            throw new Exception("Rate limit Groq (429) — réessaie dans quelques secondes.");
        }
        if (resp.statusCode() != 200) {
            LOG.warning("Groq HTTP " + resp.statusCode() + " : " + resp.body());
            throw new Exception("Groq API erreur " + resp.statusCode());
        }

        return extractTextFromGroq(resp.body());
    }

    private String extractTextFromGroq(String json) {
        String marker = "\"content\":";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            LOG.warning("Réponse Groq inattendue : " + json);
            return json;
        }
        idx += marker.length();
        while (idx < json.length() && json.charAt(idx) != '"') idx++;
        idx++;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (c == '"') break;
            if (c == '\\' && idx + 1 < json.length()) {
                char next = json.charAt(idx + 1);
                switch (next) {
                    case 'n'  -> { sb.append('\n'); idx += 2; continue; }
                    case 't'  -> { sb.append('\t'); idx += 2; continue; }
                    case '"'  -> { sb.append('"');  idx += 2; continue; }
                    case '\\' -> { sb.append('\\'); idx += 2; continue; }
                    default   -> { sb.append(next); idx += 2; continue; }
                }
            }
            sb.append(c);
            idx++;
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    public static int motsCiblesParNiveau(String niveau) {
        if (niveau == null) return 150;
        return switch (niveau.toUpperCase()) {
            case "A1", "A2" -> 100;
            case "B1", "B2" -> 200;
            case "C1", "C2" -> 300;
            default         -> 150;
        };
    }

    private String detecterLangue(String langue) {
        if (langue == null) return "FRANÇAIS";
        String l = langue.toLowerCase();
        if (l.contains("fran") || l.contains("french"))     return "FRANÇAIS";
        if (l.contains("english") || l.contains("anglais"))  return "ENGLISH";
        if (l.contains("espagnol") || l.contains("spanish")) return "ESPAÑOL";
        if (l.contains("allemand") || l.contains("german"))  return "DEUTSCH";
        if (l.contains("arabe")    || l.contains("arabic"))  return "ARABE";
        if (l.contains("italien")  || l.contains("italian")) return "ITALIANO";
        return langue.toUpperCase();
    }
}
