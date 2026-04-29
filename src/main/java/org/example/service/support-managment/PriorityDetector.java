package org.example.service.supportManagment;

import java.util.List;

public class PriorityDetector {

    private static final List<String> URGENT_WORDS = List.of(
        "urgent", "emergency", "immediately", "asap", "critical",
        "now", "help", "danger", "broken", "blocked",
        "urgence", "immédiatement", "critique", "bloqué", "impossible"
    );

    private static final List<String> HIGH_WORDS = List.of(
        "important", "serious", "problem", "issue", "error",
        "failed", "wrong", "not working", "bug",
        "problème", "erreur", "grave", "sérieux", "ne fonctionne pas"
    );

    // Retourne "URGENT", "HIGH", ou "MEDIUM" automatiquement
    public static String detect(String subject, String messageBody) {
        String text = (subject + " " + messageBody).toLowerCase();

        if (URGENT_WORDS.stream().anyMatch(text::contains)) return "URGENT";
        if (HIGH_WORDS.stream().anyMatch(text::contains))   return "HIGH";
        return "MEDIUM";
    }
}
