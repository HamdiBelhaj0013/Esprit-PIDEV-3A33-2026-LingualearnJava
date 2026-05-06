package org.example.service.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP wrapper around the local Ollama /api/chat endpoint.
 * All calls are blocking — callers must run this on a background thread.
 */
public class OllamaService {

    public static final String OLLAMA_URL   = "http://127.0.0.1:11434/api/chat";
    public static final String MODEL        = "llama3";
    private static final int   CONNECT_MS   = 10_000;
    private static final int   READ_MS      = 120_000;

    /**
     * Sends a chat request to Ollama.
     *
     * @param messages     conversation turns as [{role, content}, ...]
     * @param systemPrompt injected as the first system message
     * @return the model's reply text
     * @throws RuntimeException if Ollama is unreachable or returns an error
     */
    public String chat(List<Map<String, String>> messages, String systemPrompt) {
        try {
            JSONObject body = buildBody(messages, systemPrompt);
            HttpURLConnection conn = openConnection();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                String err = readStream(conn.getErrorStream());
                throw new RuntimeException(
                    "Ollama returned HTTP " + status + ": " + err);
            }

            String raw = readStream(conn.getInputStream());
            JSONObject response = new JSONObject(raw);
            return response.getJSONObject("message").getString("content");

        } catch (IOException e) {
            throw new RuntimeException(
                "AI service unavailable. Make sure Ollama is running.", e);
        }
    }

    /** Returns true if the Ollama daemon is reachable. */
    public boolean isAvailable() {
        try {
            HttpURLConnection conn = (HttpURLConnection)
                new URL("http://127.0.0.1:11434").openConnection();
            conn.setConnectTimeout(3_000);
            conn.setReadTimeout(3_000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JSONObject buildBody(List<Map<String, String>> messages, String systemPrompt) {
        JSONArray msgs = new JSONArray();
        msgs.put(new JSONObject()
            .put("role",    "system")
            .put("content", systemPrompt));
        for (Map<String, String> m : messages) {
            msgs.put(new JSONObject()
                .put("role",    m.get("role"))
                .put("content", m.get("content")));
        }
        return new JSONObject()
            .put("model",   MODEL)
            .put("stream",  false)
            .put("options", new JSONObject().put("temperature", 0.1))
            .put("messages", msgs);
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(OLLAMA_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(CONNECT_MS);
        conn.setReadTimeout(READ_MS);
        conn.setDoOutput(true);
        return conn;
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
