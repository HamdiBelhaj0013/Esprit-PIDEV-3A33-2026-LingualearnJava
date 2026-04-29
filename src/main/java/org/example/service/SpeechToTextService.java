package org.example.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SpeechToTextService {

    private static final String OPENAI_TRANSCRIPTION_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String DEFAULT_MODEL = "gpt-4o-mini-transcribe";

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Transcrit un fichier audio avec l'API OpenAI Speech-to-Text.
     * Formats acceptes generalement: mp3, mp4, mpeg, mpga, m4a, wav, webm.
     *
     * IMPORTANT:
     * Dans IntelliJ, ajoute ta cle dans les variables d'environnement:
     * OPENAI_API_KEY=sk-...
     */
    public String transcribeAudio(File audioFile) {
        return transcribeAudio(audioFile, "fr");
    }

    public String transcribeAudio(File audioFile, String language) {
        try {
            if (audioFile == null || !audioFile.exists() || !audioFile.isFile()) {
                throw new IllegalArgumentException("Audio file not found.");
            }
            System.out.println("OPENAI_API_KEY = " + System.getenv("OPENAI_API_KEY"));

            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("OPENAI_API_KEY is missing. Add it in IntelliJ environment variables.");
            }

            String boundary = "----JavaBoundary" + UUID.randomUUID();
            byte[] body = buildMultipartBody(boundary, audioFile.toPath(), DEFAULT_MODEL, language);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_TRANSCRIPTION_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("STT API error " + response.statusCode() + ": " + response.body());
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json != null && json.has("text")) {
                return json.get("text").getAsString();
            }

            throw new IOException("Invalid STT response: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur STT: " + e.getMessage();
        }
    }

    public String saveTranscript(String lessonId, String transcript) {
        try {
            Files.createDirectories(Paths.get("exports/recordings"));

            String filename = String.format("exports/recordings/lesson_%s_%d.txt",
                    lessonId, System.currentTimeMillis());

            try (FileWriter writer = new FileWriter(filename)) {
                writer.write("Lesson ID: " + lessonId + "\n");
                writer.write("Recorded: " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("================================\n");
                writer.write(transcript == null ? "" : transcript);
            }

            System.out.println("Transcript saved: " + filename);
            return filename;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String transcribeAndSave(String lessonId, File audioFile, String language) {
        String transcript = transcribeAudio(audioFile, language);
        saveTranscript(lessonId, transcript);
        return transcript;
    }

    public double analyzePronunciation(String originalText, String spokenText) {
        if (originalText == null || spokenText == null || originalText.isBlank()) {
            return 0;
        }

        int matches = 0;
        String[] originalWords = originalText.toLowerCase().trim().split("\\s+");
        String[] spokenWords = spokenText.toLowerCase().trim().split("\\s+");

        int minLength = Math.min(originalWords.length, spokenWords.length);
        for (int i = 0; i < minLength; i++) {
            if (originalWords[i].equals(spokenWords[i])) {
                matches++;
            }
        }

        return originalWords.length > 0 ? (double) matches / originalWords.length * 100 : 0;
    }

    private byte[] buildMultipartBody(String boundary, Path audioPath, String model, String language) throws IOException {
        String fileName = audioPath.getFileName().toString();
        String mimeType = detectMimeType(fileName);
        byte[] fileBytes = Files.readAllBytes(audioPath);

        StringBuilder start = new StringBuilder();
        appendFormField(start, boundary, "model", model);
        appendFormField(start, boundary, "language", language == null || language.isBlank() ? "fr" : language);
        appendFormField(start, boundary, "response_format", "json");

        start.append("--").append(boundary).append("\r\n");
        start.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        start.append("Content-Type: ").append(mimeType).append("\r\n\r\n");

        byte[] startBytes = start.toString().getBytes(StandardCharsets.UTF_8);
        byte[] endBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[startBytes.length + fileBytes.length + endBytes.length];
        System.arraycopy(startBytes, 0, body, 0, startBytes.length);
        System.arraycopy(fileBytes, 0, body, startBytes.length, fileBytes.length);
        System.arraycopy(endBytes, 0, body, startBytes.length + fileBytes.length, endBytes.length);

        return body;
    }

    private void appendFormField(StringBuilder sb, String boundary, String name, String value) {
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        sb.append(value).append("\r\n");
    }

    private String detectMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".mp3") || lower.endsWith(".mpeg") || lower.endsWith(".mpga")) return "audio/mpeg";
        if (lower.endsWith(".mp4")) return "audio/mp4";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".webm")) return "audio/webm";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".flac")) return "audio/flac";
        return "application/octet-stream";
    }
    public String getFeedback(double score) {
        if (score >= 90) return "Excellent ! Prononciation presque parfaite.";
        if (score >= 70) return "Très bien ! Continue à t'entraîner.";
        if (score >= 50) return "Pas mal, mais tu dois répéter encore.";
        return "Essaie encore. Écoute le mot puis répète lentement.";
    }
}
