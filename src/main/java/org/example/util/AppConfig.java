package org.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration loader.
 * Reads from config.properties on the classpath, with email settings
 * coming from environment variables (never hardcoded).
 */
public class AppConfig {

    // ── config.properties ────────────────────────────────────────────────────
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                props.load(in);
                System.out.println("✅ config.properties chargé.");
            } else {
                System.out.println("⚠️ config.properties introuvable dans les ressources.");
            }
        } catch (IOException e) {
            System.out.println("❌ Erreur chargement config : " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "");
    }

    // ── Email / SMTP (read from environment variables) ────────────────────────
    private static final String MAIL_USERNAME  = System.getenv("MAIL_USERNAME");
    private static final String MAIL_PASSWORD  = System.getenv("MAIL_PASSWORD");
    private static final String MAIL_SMTP_HOST = getEnvOrDefault("MAIL_SMTP_HOST", "smtp.gmail.com");
    private static final String MAIL_SMTP_PORT = getEnvOrDefault("MAIL_SMTP_PORT", "587");

    static {
        if (MAIL_USERNAME == null || MAIL_USERNAME.isBlank()
                || MAIL_PASSWORD == null || MAIL_PASSWORD.isBlank()) {
            System.out.println("[EmailConfig] WARNING: MAIL_USERNAME or MAIL_PASSWORD not set. Email sending will fail.");
        }
    }

    public static String getMailUsername()  { return MAIL_USERNAME  != null ? MAIL_USERNAME  : ""; }
    public static String getMailPassword()  { return MAIL_PASSWORD  != null ? MAIL_PASSWORD  : ""; }
    public static String getMailSmtpHost()  { return MAIL_SMTP_HOST; }
    public static String getMailSmtpPort()  { return MAIL_SMTP_PORT; }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
