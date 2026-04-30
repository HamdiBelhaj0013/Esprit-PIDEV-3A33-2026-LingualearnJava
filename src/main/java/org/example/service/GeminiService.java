package org.example.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for calling Google Gemini AI API.
 * Provides AI-powered explanations for quiz answers.
 */
public class GeminiService {

    private static final String API_KEY = "AIzaSyDSLIw8DjXPsCF2rHzUps1WR_cDMBvuEZ8";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private final HttpClient httpClient;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Generates AI feedback for a single quiz question.
     *
     * @param question      The question text
     * @param options       The available options (can be null)
     * @param correctAnswer The correct answer
     * @param userAnswer    The user's submitted answer
     * @param isCorrect     Whether the user got it right
     * @return AI-generated explanation string
     */
    public String getAnswerExplanation(String question, String options, String correctAnswer,
                                        String userAnswer, boolean isCorrect) {
        try {
            String prompt = buildExplanationPrompt(question, options, correctAnswer, userAnswer, isCorrect);
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠ Unable to generate AI explanation. Please try again later.";
        }
    }

    /**
     * Generates a full quiz summary with AI feedback for all questions.
     *
     * @param results List of maps, each containing: question, options, correctAnswer, userAnswer, isCorrect
     * @return AI-generated summary string
     */
    public String getQuizSummary(List<Map<String, String>> results) {
        try {
            String prompt = buildQuizSummaryPrompt(results);
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠ Unable to generate AI summary. Please try again later.";
        }
    }

    private String buildExplanationPrompt(String question, String options, String correctAnswer,
                                           String userAnswer, boolean isCorrect) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert language learning tutor. A student just answered a quiz question. ");
        sb.append("Give a brief, encouraging explanation (2-3 sentences max) about why the correct answer is correct. ");
        sb.append("If the student got it wrong, gently explain their mistake. ");
        sb.append("Use simple language. Do NOT use markdown formatting.\n\n");
        sb.append("Question: ").append(question).append("\n");
        if (options != null && !options.isEmpty() && !options.equals("[]")) {
            sb.append("Options: ").append(options).append("\n");
        }
        sb.append("Correct Answer: ").append(correctAnswer).append("\n");
        sb.append("Student's Answer: ").append(userAnswer.isEmpty() ? "(no answer given)" : userAnswer).append("\n");
        sb.append("Result: ").append(isCorrect ? "CORRECT ✓" : "INCORRECT ✗").append("\n");
        return sb.toString();
    }

    private String buildQuizSummaryPrompt(List<Map<String, String>> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert language learning tutor. A student just completed a quiz. ");
        sb.append("Provide a brief overall assessment (3-4 sentences) with encouragement and one specific tip for improvement. ");
        sb.append("Do NOT use markdown formatting.\n\n");

        int correct = 0;
        for (int i = 0; i < results.size(); i++) {
            Map<String, String> r = results.get(i);
            sb.append("Q").append(i + 1).append(": ").append(r.get("question")).append("\n");
            sb.append("  Student answered: ").append(r.get("userAnswer")).append("\n");
            sb.append("  Correct answer: ").append(r.get("correctAnswer")).append("\n");
            sb.append("  Result: ").append(r.get("isCorrect")).append("\n\n");
            if ("true".equals(r.get("isCorrect"))) correct++;
        }
        sb.append("Score: ").append(correct).append("/").append(results.size()).append("\n");
        return sb.toString();
    }

    private String callGeminiAPI(String prompt) throws Exception {
        // Escape special characters for JSON
        String escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Gemini API error: " + response.statusCode() + " - " + response.body());
            return "⚠ AI service temporarily unavailable (Error " + response.statusCode() + ")";
        }

        return extractTextFromResponse(response.body());
    }

    /**
     * Extracts the text content from Gemini API JSON response.
     * Manual parsing to avoid extra dependencies.
     */
    private String extractTextFromResponse(String json) {
        try {
            // Find the "text" field value in the response JSON
            // Response structure: {"candidates":[{"content":{"parts":[{"text":"..."}],...}}]}
            String marker = "\"text\"";
            int textIndex = json.indexOf(marker);
            if (textIndex == -1) {
                return "⚠ Could not parse AI response.";
            }

            // Find the start of the text value (after "text": ")
            int colonIndex = json.indexOf(":", textIndex);
            int quoteStart = json.indexOf("\"", colonIndex + 1);
            if (quoteStart == -1) return "⚠ Could not parse AI response.";

            // Find the end of the text value, handling escaped quotes
            StringBuilder result = new StringBuilder();
            int i = quoteStart + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == '"') {
                        result.append('"');
                        i += 2;
                    } else if (next == 'n') {
                        result.append('\n');
                        i += 2;
                    } else if (next == '\\') {
                        result.append('\\');
                        i += 2;
                    } else if (next == 't') {
                        result.append('\t');
                        i += 2;
                    } else {
                        result.append(c);
                        i++;
                    }
                } else if (c == '"') {
                    break;
                } else {
                    result.append(c);
                    i++;
                }
            }

            return result.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠ Could not parse AI response.";
        }
    }
}
