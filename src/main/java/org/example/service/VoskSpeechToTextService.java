package org.example.service;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;

public class VoskSpeechToTextService {

    private static final String MODEL_PATH = "model/vosk-model-small-fr-0.22/vosk-model-small-fr-0.22";

    public String transcribeAudio(File audioFile) {
        try {
            Model model = new Model(MODEL_PATH);

            AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);

            AudioFormat baseFormat = ais.getFormat();

            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    16000,
                    16,
                    1,
                    2,
                    16000,
                    false
            );

            AudioInputStream din = AudioSystem.getAudioInputStream(targetFormat, ais);

            Recognizer recognizer = new Recognizer(model, 16000);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = din.read(buffer)) >= 0) {
                recognizer.acceptWaveForm(buffer, bytesRead);
            }

            String result = recognizer.getFinalResult();

            recognizer.close();
            model.close();

            System.out.println("VOSK RESULT: " + result);

            // extraire texte JSON
            return extractText(result);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String extractText(String json) {
        try {
            com.google.gson.JsonObject obj =
                    new com.google.gson.Gson().fromJson(json, com.google.gson.JsonObject.class);

            if (obj != null && obj.has("text")) {
                return obj.get("text").getAsString().trim();
            }

            return "";

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public double analyzePronunciation(String expected, String recognized) {
        expected = expected.toLowerCase().trim();
        recognized = recognized.toLowerCase().trim();

        if (recognized.isEmpty()) return 0;

        int distance = levenshtein(expected, recognized);
        int maxLen = Math.max(expected.length(), recognized.length());

        return (1.0 - ((double) distance / maxLen)) * 100;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length()+1][b.length()+1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = Math.min(
                        Math.min(dp[i-1][j]+1, dp[i][j-1]+1),
                        dp[i-1][j-1] + (a.charAt(i-1)==b.charAt(j-1) ? 0 : 1)
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    public String getFeedback(double score) {
        if (score >= 90) return "Excellent !";
        if (score >= 70) return "Très bien !";
        if (score >= 50) return "Pas mal";
        return "Essaie encore";
    }
}