package org.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Chargeur centralisé de la configuration (config.properties).
 */
public class AppConfig {

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
}

