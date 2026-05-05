package org.example.service.forum;

import java.util.Arrays;
import java.util.List;

/**
 * Service de dÃ©tection de mots inappropriÃ©s dans les commentaires.
 */
public class BadWordChecker {

    private static final List<String> BAD_WORDS = Arrays.asList(
        // FranÃ§ais
        "merde", "putain", "connard", "salope", "con", "conne", "idiot",
        "imbecile", "imbÃ©cile", "stupide", "abruti", "enfoirÃ©", "batard",
        "bÃ¢tard", "nique", "fdp", "tg", "ta gueule", "cul", "bite",
        "foutre", "chier", "encule", "enculÃ©", "pute", "pÃ©tasse", "salopard",
        "crÃ©tin", "cretin", "dÃ©bile", "debile", "mongol", "Nazi", "Nazi",
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


