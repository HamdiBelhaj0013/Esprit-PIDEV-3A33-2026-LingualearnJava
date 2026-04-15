package org.example.validators;

import org.example.entities.Lesson;

import java.util.ArrayList;
import java.util.List;

public class LessonValidator {

    public static List<String> validate(Lesson lesson) {
        List<String> errors = new ArrayList<>();

        if (lesson.getTitle() == null || lesson.getTitle().trim().isEmpty()) {
            errors.add("Le titre de la leçon est obligatoire.");
        } else if (lesson.getTitle().trim().length() < 3) {
            errors.add("Le titre de la leçon doit contenir au moins 3 caractères.");
        }

        if (lesson.getContent() == null || lesson.getContent().trim().isEmpty()) {
            errors.add("Le contenu est obligatoire.");
        }

        if (lesson.getVocabularyData() == null || lesson.getVocabularyData().trim().isEmpty()) {
            errors.add("Vocabulary data est obligatoire.");
        } else if (!isValidJson(lesson.getVocabularyData())) {
            errors.add("Vocabulary data doit être en JSON valide. Exemple : [\"word1\",\"word2\"]");
        }

        if (lesson.getGrammarData() == null || lesson.getGrammarData().trim().isEmpty()) {
            errors.add("Grammar data est obligatoire.");
        } else if (!isValidJson(lesson.getGrammarData())) {
            errors.add("Grammar data doit être en JSON valide. Exemple : {\"grammar\":\"present\"}");
        }

        if (lesson.getXpReward() < 0) {
            errors.add("XP reward doit être supérieur ou égal à 0.");
        }

        if (lesson.getCourseId() <= 0) {
            errors.add("Le cours associé est invalide.");
        }

        if (lesson.getVideoName() != null && lesson.getVideoName().length() > 255) {
            errors.add("Le nom de la vidéo ne doit pas dépasser 255 caractères.");
        }

        if (lesson.getThumbName() != null && lesson.getThumbName().length() > 255) {
            errors.add("Le nom de l'image ne doit pas dépasser 255 caractères.");
        }

        if (lesson.getResourceName() != null && lesson.getResourceName().length() > 255) {
            errors.add("Le nom de la ressource ne doit pas dépasser 255 caractères.");
        }

        return errors;
    }

    private static boolean isValidJson(String text) {
        text = text.trim();
        return (text.startsWith("[") && text.endsWith("]"))
                || (text.startsWith("{") && text.endsWith("}"));
    }
}