package org.example.service.user_managment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HCaptchaService {

    private static final String SECRET = "xxxxxxxxxxxxxxxxxxxxx";
    private static final String VERIFY_URL = "https://api.hcaptcha.com/siteverify";

    public boolean verify(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            String body = "secret=" + URLEncoder.encode(SECRET, StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            URL url = new URL(VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) response.append(line);
            }

            return response.toString().contains("\"success\":true");

        } catch (Exception e) {
            return false;
        }
    }
}
