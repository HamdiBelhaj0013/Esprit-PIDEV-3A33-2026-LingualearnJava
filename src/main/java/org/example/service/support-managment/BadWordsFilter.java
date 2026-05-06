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
        // Vérification par mot entier uniquement
        String[] words = lower.split("\\s+|[.,!?;:]");
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-záàâéèêëîïôùûüç]", "");
            if (BAD_WORDS.stream().anyMatch(bw -> cleanWord.equals(bw))) {
                return true;
            }
        }
        return false;
    }

    public static String getFoundBadWord(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        String[] words = lower.split("\\s+|[.,!?;:]");
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-záàâéèêëîïôùûüç]", "");
            for (String bw : BAD_WORDS) {
                if (cleanWord.equals(bw)) return bw;
            }
        }
        return null;
    }
}
