package org.example.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TextToSpeechService {

    private static final int MAX_LENGTH = 120;

    public List<String> textToSpeechChunks(String text, String language) {
        List<String> files = new ArrayList<>();

        try {
            Files.createDirectories(Paths.get("exports/audio/chunks"));

            if (text == null || text.trim().isEmpty()) {
                System.err.println("Texte vide.");
                return files;
            }

            String cleaned = text
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace("“", "\"")
                    .replace("”", "\"")
                    .replace("’", "'")
                    .replaceAll("[\\r\\n]+", ". ")
                    .trim();

            List<String> chunks = splitText(cleaned, MAX_LENGTH);
            long timestamp = System.currentTimeMillis();

            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                String filename = "exports/audio/chunks/chunk_" + timestamp + "_" + i + ".mp3";

                boolean ok = downloadMp3(chunkText, language, filename);

                if (ok) {
                    File f = new File(filename);
                    System.out.println("Audio saved: " + f.getAbsolutePath() + " | size = " + f.length());
                    files.add(filename);
                } else {
                    System.err.println("Audio failed for chunk: " + chunkText);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return files;
    }

    public String textToSpeech(String text, String language) {
        List<String> files = textToSpeechChunks(text, language);
        return files.isEmpty() ? null : files.get(0);
    }

    public List<String> generateLessonAudioChunks(String lessonTitle, String lessonContent, String language) {
        return textToSpeechChunks(lessonContent, language);
    }

    private boolean downloadMp3(String text, String language, String filename) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String urlStr = "https://translate.google.com/translate_tts?ie=UTF-8"
                    + "&client=tw-ob"
                    + "&tl=" + language
                    + "&q=" + encodedText;

            System.out.println("TTS URL: " + urlStr);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "audio/mpeg");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            System.out.println("Response: " + code);
            System.out.println("Content-Type: " + conn.getContentType());

            if (code != 200) {
                return false;
            }

            byte[] bytes;
            try (InputStream in = conn.getInputStream()) {
                bytes = in.readAllBytes();
            }

            if (bytes.length < 1000) {
                System.err.println("Fichier trop petit: " + bytes.length + " bytes");
                return false;
            }

            try (FileOutputStream out = new FileOutputStream(filename)) {
                out.write(bytes);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<String> splitText(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");

        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > maxLength) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }

            current.append(word).append(" ");
        }

        if (!current.toString().trim().isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }
}