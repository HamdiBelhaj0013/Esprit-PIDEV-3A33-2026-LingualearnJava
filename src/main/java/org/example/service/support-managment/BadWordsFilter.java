package org.example.service.supportManagment;

import java.util.List;

public class BadWordsFilter {

    private static final List<String> BAD_WORDS = List.of(
        "idiot", "stupid", "fool", "moron", "dumb",
        "hate", "kill", "damn", "crap", "shut up",
        "connard", "con", "merde", "salaud", "idiot",
        "nul", "imbécile", "crétin", "abruti"
    );

    // Retourne true si le texte contient un bad word
    public static boolean containsBadWord(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return BAD_WORDS.stream().anyMatch(lower::contains);
    }

    // Retourne le mot interdit trouvé
    public static String getFoundBadWord(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        return BAD_WORDS.stream()
            .filter(lower::contains)
            .findFirst()
            .orElse(null);
    }
}
