package org.example.service.tests;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service IA Speaking — Groq / LLaMA 3.3-70b
 *
 * Deux responsabilités :
 *   1. genererQuestions() → 5 questions de conversation adaptées au niveau + langue
 *   2. evaluerConversation() → note/20 + feedback détaillé sur les 5 réponses
 */
public class GroqSpeakingService {

    private static final Logger LOG      = Logger.getLogger(GroqSpeakingService.class.getName());
    private static final String API_KEY  = "........................."; // ← même clé Groq que Writing
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    // ─────────────────────────────────────────────────────────────────────────
    //  RECORD résultat évaluation
    // ─────────────────────────────────────────────────────────────────────────

    public record SpeakingFeedback(
            int    noteSur20,
            float  scorePct,
            String niveauCEFR,       // ex: "B1", "B2"
            String grammaire,
            String vocabulaire,
            String fluidite,         // aisance / débit
            String coherence,
            String prononciation,    // basée sur la transcription
            String suggestions,
            String bilanGlobal
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    //  1. GÉNÉRATION DES QUESTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Génère 5 questions de conversation adaptées au niveau et à la langue.
     * @return  liste de 5 questions (String)
     */
    public List<String> genererQuestions(String niveau, String langue) throws Exception {
        String prompt = """
            Tu es un examinateur de %s spécialisé dans les certifications CECRL niveau %s.
            Génère exactement 5 questions de conversation orale pour évaluer un candidat.
            
            Contraintes :
            - Questions rédigées EN %s
            - Adaptées au niveau %s (vocabulaire et complexité appropriés)
            - Questions progressives : simples → complexes
            - Thèmes variés : présentation, opinion, description, situation, argumentation
            - Format OBLIGATOIRE — réponds UNIQUEMENT avec ce format exact :
            
            Q1: [question 1]
            Q2: [question 2]
            Q3: [question 3]
            Q4: [question 4]
            Q5: [question 5]
            
            Rien d'autre. Pas d'introduction, pas de commentaire.
            """.formatted(langue, niveau, detecterLangue(langue), niveau);

        String raw = callGroq(prompt);
        return parseQuestions(raw);
    }

    private List<String> parseQuestions(String text) {
        List<String> questions = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String marker = "Q" + i + ":";
            int idx = text.indexOf(marker);
            if (idx >= 0) {
                idx += marker.length();
                // Aller jusqu'au prochain Q(i+1) ou fin
                String nextMarker = "Q" + (i + 1) + ":";
                int end = text.indexOf(nextMarker, idx);
                String q = (end > idx ? text.substring(idx, end) : text.substring(idx))
                        .trim().replaceAll("\\n+", " ");
                questions.add(q);
            }
        }
        // Fallback si parsing échoue
        if (questions.isEmpty()) {
            questions.add("Pouvez-vous vous présenter brièvement ?");
            questions.add("Parlez-moi de vos loisirs.");
            questions.add("Décrivez votre journée idéale.");
            questions.add("Que pensez-vous de l'importance des langues étrangères ?");
            questions.add("Quel est votre objectif d'apprentissage principal ?");
        }
        return questions;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. ÉVALUATION DE LA CONVERSATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Évalue la conversation complète (5 Q/R) et retourne un feedback structuré.
     *
     * @param questions  les 5 questions posées
     * @param reponses   les 5 réponses transcrites par AssemblyAI
     * @param niveau     niveau CECRL du test
     * @param langue     langue du test
     */
    public SpeakingFeedback evaluerConversation(List<String> questions,
                                                List<String> reponses,
                                                String niveau,
                                                String langue) throws Exception {
        // Construire la conversation complète
        StringBuilder conv = new StringBuilder();
        for (int i = 0; i < Math.min(questions.size(), reponses.size()); i++) {
            conv.append("QUESTION ").append(i + 1).append(" : ").append(questions.get(i)).append("\n");
            conv.append("RÉPONSE  ").append(i + 1).append(" : ").append(reponses.get(i)).append("\n\n");
        }

        String prompt = """
            Tu es un examinateur expert en %s, certifié CECRL niveau %s.
            
            Voici la conversation orale d'un candidat (transcrite automatiquement) :
            
            %s
            
            Évalue cette performance orale selon les critères CECRL.
            Note que le texte est une transcription automatique — tiens compte des éventuelles
            erreurs de transcription dans ton évaluation de la prononciation.
            
            Réponds UNIQUEMENT avec ce format exact :
            
            NOTE: [chiffre de 0 à 20]
            
            NIVEAU_CEFR: [niveau estimé : A1, A2, B1, B2, C1 ou C2]
            
            GRAMMAIRE: [Analyse des structures grammaticales utilisées. Cite des exemples des réponses. Identifie les erreurs et propose des corrections.]
            
            VOCABULAIRE: [Richesse et pertinence du vocabulaire. Cite des mots/expressions utilisés. Suggère des alternatives plus riches.]
            
            FLUIDITE: [Aisance de la communication. Le candidat répond-il de manière développée ? Hésite-t-il ? Ses réponses sont-elles complètes ?]
            
            COHERENCE: [Logique et organisation des idées dans les réponses. Les réponses sont-elles pertinentes aux questions ?]
            
            PRONONCIATION: [Basée sur la qualité de la transcription. Des mots semblent-ils mal transcrits suggérant une mauvaise prononciation ?]
            
            SUGGESTIONS: [3 conseils concrets pour améliorer l'expression orale au niveau %s.]
            
            BILAN: [En 3 phrases : points forts, axes d'amélioration, encouragement.]
            """.formatted(langue, niveau, conv.toString(), niveau);

        String raw = callGroq(prompt);
        return parseEvaluation(raw);
    }

    private SpeakingFeedback parseEvaluation(String text) {
        int    note         = extractInt(text,     "NOTE:");
        String niveauCEFR   = extractSection(text, "NIVEAU_CEFR:", "GRAMMAIRE:");
        String grammaire    = extractSection(text, "GRAMMAIRE:",   "VOCABULAIRE:");
        String vocabulaire  = extractSection(text, "VOCABULAIRE:", "FLUIDITE:");
        String fluidite     = extractSection(text, "FLUIDITE:",    "COHERENCE:");
        String coherence    = extractSection(text, "COHERENCE:",   "PRONONCIATION:");
        String prononciation= extractSection(text, "PRONONCIATION:","SUGGESTIONS:");
        String suggestions  = extractSection(text, "SUGGESTIONS:", "BILAN:");
        String bilan        = extractSection(text, "BILAN:",        null);

        note = Math.max(0, Math.min(20, note));
        float scorePct = note / 20f * 100f;

        return new SpeakingFeedback(note, scorePct,
                niveauCEFR.isBlank()   ? "B1"                                        : niveauCEFR.trim(),
                grammaire.isBlank()    ? "Analyse indisponible."                     : grammaire,
                vocabulaire.isBlank()  ? "Analyse indisponible."                     : vocabulaire,
                fluidite.isBlank()     ? "Analyse indisponible."                     : fluidite,
                coherence.isBlank()    ? "Analyse indisponible."                     : coherence,
                prononciation.isBlank()? "Analyse indisponible."                     : prononciation,
                suggestions.isBlank()  ? "Continuez à pratiquer l'oral régulièrement.": suggestions,
                bilan.isBlank()        ? "Résultat : " + note + "/20"                : bilan);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HTTP
    // ─────────────────────────────────────────────────────────────────────────

    private String callGroq(String prompt) throws Exception {
        String safe = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        String body = """
                {"model":"%s","messages":[{"role":"user","content":"%s"}],
                 "temperature":0.7,"max_tokens":1500}
                """.formatted(MODEL, safe);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(40))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            LOG.severe("Groq HTTP " + resp.statusCode() + " : " + resp.body());
            throw new Exception("Groq erreur " + resp.statusCode());
        }
        return extractContent(resp.body());
    }

    private String extractContent(String json) {
        String marker = "\"content\":";
        int idx = json.indexOf(marker);
        if (idx < 0) return json;
        idx += marker.length();
        while (idx < json.length() && json.charAt(idx) != '"') idx++;
        idx++;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (c == '"') break;
            if (c == '\\' && idx + 1 < json.length()) {
                char n = json.charAt(idx + 1);
                switch (n) {
                    case 'n' -> { sb.append('\n'); idx += 2; continue; }
                    case 't' -> { sb.append('\t'); idx += 2; continue; }
                    case '"' -> { sb.append('"');  idx += 2; continue; }
                    case '\\'-> { sb.append('\\'); idx += 2; continue; }
                    default  -> { sb.append(n);    idx += 2; continue; }
                }
            }
            sb.append(c); idx++;
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private int extractInt(String text, String key) {
        int idx = text.indexOf(key);
        if (idx < 0) return 10;
        String sub = text.substring(idx + key.length()).trim();
        StringBuilder sb = new StringBuilder();
        for (char c : sub.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
            else if (!sb.isEmpty()) break;
        }
        try { return Integer.parseInt(sb.toString()); } catch (Exception e) { return 10; }
    }

    private String extractSection(String text, String startKey, String endKey) {
        int start = text.indexOf(startKey);
        if (start < 0) return "";
        start += startKey.length();
        int end = endKey != null ? text.indexOf(endKey, start) : -1;
        return ((end > start) ? text.substring(start, end) : text.substring(start)).trim();
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
