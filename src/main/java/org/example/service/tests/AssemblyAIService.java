package org.example.service.tests;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Service de transcription audio — AssemblyAI
 * Flux : upload fichier WAV → lancer transcription → polling → retourner texte
 */
public class AssemblyAIService {

    private static final Logger LOG      = Logger.getLogger(AssemblyAIService.class.getName());
    private static final String API_KEY  = "cd390ceb92be43a7b4ef71242472d3e5";
    private static final String BASE_URL = "https://api.assemblyai.com/v2";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    // ─────────────────────────────────────────────────────────────────────────
    //  API PRINCIPALE : transcrire un fichier WAV
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Transcrit un fichier audio WAV en texte.
     * @param wavFile  chemin vers le fichier WAV enregistré
     * @param langue   langue du test (ex: "Français", "English")
     * @return         texte transcrit
     */
    public String transcrire(Path wavFile, String langue) throws Exception {
        // Étape 1 : uploader le fichier
        String uploadUrl = uploadFichier(wavFile);
        LOG.info("Audio uploadé : " + uploadUrl);

        // Étape 2 : lancer la transcription
        String transcriptId = lancerTranscription(uploadUrl, langue);
        LOG.info("Transcription lancée, id : " + transcriptId);

        // Étape 3 : polling jusqu'à completion (max 60s)
        return attendreResultat(transcriptId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String uploadFichier(Path wavFile) throws Exception {
        byte[] data = Files.readAllBytes(wavFile);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/upload"))
                .header("Authorization", API_KEY)
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp, "upload");
        return extraireValeur(resp.body(), "upload_url");
    }

    private String lancerTranscription(String uploadUrl, String langue) throws Exception {
        String langCode = detecterCodeLangue(langue);

        String body = """
                {
                  "audio_url": "%s",
                  "language_code": "%s",
                  "speech_models": ["universal-2"],
                  "punctuate": true,
                  "format_text": true
                }
                """.formatted(uploadUrl, langCode);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/transcript"))
                .header("Authorization", API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp, "transcription");
        return extraireValeur(resp.body(), "id");
    }

    private String attendreResultat(String transcriptId) throws Exception {
        String url = BASE_URL + "/transcript/" + transcriptId;
        int tentatives = 0;

        while (tentatives < 30) {
            Thread.sleep(2000);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", API_KEY)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp, "polling");

            String status = extraireValeur(resp.body(), "status");
            LOG.info("AssemblyAI status : " + status);

            switch (status) {
                case "completed" -> {
                    String text = extraireValeur(resp.body(), "text");
                    return text.isBlank() ? "[Aucune parole detectee]" : text;
                }
                case "error" -> {
                    String error = extraireValeur(resp.body(), "error");
                    throw new Exception("AssemblyAI erreur : " + error);
                }
            }
            tentatives++;
        }
        throw new Exception("Timeout AssemblyAI.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void checkStatus(HttpResponse<String> resp, String step) throws Exception {
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            LOG.severe("AssemblyAI " + step + " HTTP " + resp.statusCode()
                    + " : " + resp.body());
            throw new Exception("AssemblyAI erreur " + step
                    + " (HTTP " + resp.statusCode() + ")");
        }
    }

    /** Extraction JSON simple sans librairie. */
    private String extraireValeur(String json, String key) {
        String marker = "\"" + key + "\":";
        int idx = json.indexOf(marker);
        if (idx < 0) return "";
        idx += marker.length();
        while (idx < json.length() && (json.charAt(idx) == ' ')) idx++;

        if (json.charAt(idx) == '"') {
            // valeur string
            idx++;
            StringBuilder sb = new StringBuilder();
            while (idx < json.length() && json.charAt(idx) != '"') {
                char c = json.charAt(idx);
                if (c == '\\' && idx + 1 < json.length()) {
                    idx++;
                    switch (json.charAt(idx)) {
                        case 'n' -> sb.append('\n');
                        case '"' -> sb.append('"');
                        default  -> sb.append(json.charAt(idx));
                    }
                } else {
                    sb.append(c);
                }
                idx++;
            }
            return sb.toString();
        } else {
            // valeur non-string (null, number, bool)
            StringBuilder sb = new StringBuilder();
            while (idx < json.length()
                    && json.charAt(idx) != ',' && json.charAt(idx) != '}') {
                sb.append(json.charAt(idx++));
            }
            return sb.toString().trim().replace("\"", "");
        }
    }

    private String detecterCodeLangue(String langue) {
        if (langue == null) return "fr";
        String l = langue.toLowerCase();
        if (l.contains("fran") || l.contains("french"))    return "fr";
        if (l.contains("english") || l.contains("anglais")) return "en";
        if (l.contains("espagnol") || l.contains("spanish")) return "es";
        if (l.contains("allemand") || l.contains("german"))  return "de";
        if (l.contains("arabe")    || l.contains("arabic"))  return "ar";
        if (l.contains("italien")  || l.contains("italian")) return "it";
        return "fr";
    }
}