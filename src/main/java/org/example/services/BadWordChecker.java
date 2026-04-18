package org.example.services;

import java.util.Arrays;
import java.util.List;

/**
 * Service de détection de mots inappropriés dans les commentaires.
 */
public class BadWordChecker {

    private static final List<String> BAD_WORDS = Arrays.asList(
        // Français
        "merde", "putain", "connard", "salope", "con", "conne", "idiot",
        "imbecile", "imbécile", "stupide", "abruti", "enfoiré", "batard",
        "bâtard", "nique", "fdp", "tg", "ta gueule", "cul", "bite",
        "foutre", "chier", "encule", "enculé", "pute", "pétasse", "salopard",
        "crétin", "cretin", "débile", "debile", "mongol", "Nazi", "Nazi",
        "raciste", "haine", "tuer", "mort", "suicide",
        // Anglais
        "fuck", "shit", "bitch", "asshole", "bastard", "damn", "crap",
        "dick", "pussy", "slut", "whore", "nigger", "retard", "idiot",
        "hate", "kill", "die"
    );

    public boolean containsBadWords(String text) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        for (String word : BAD_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String getFoundBadWord(String text) {
        if (text == null || text.isEmpty()) return null;
        String lower = text.toLowerCase();
        for (String word : BAD_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                return word;
            }
        }
        return null;
    }
}

