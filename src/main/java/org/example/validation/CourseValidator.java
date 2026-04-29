package org.example.validation;

import org.example.entities.pedagogicalcontent.Course;

import java.util.ArrayList;
import java.util.List;

public class CourseValidator {

    public static List<String> validate(Course course) {
        List<String> errors = new ArrayList<>();

        if (course.getTitle() == null || course.getTitle().trim().isEmpty()) {
            errors.add("Le titre du cours est obligatoire.");
        } else if (course.getTitle().trim().length() < 3) {
            errors.add("Le titre du cours doit contenir au moins 3 caractères.");
        }

        if (course.getLevel() == null || course.getLevel().trim().isEmpty()) {
            errors.add("Le niveau est obligatoire.");
        } else if (course.getLevel().length() > 50) {
            errors.add("Le niveau ne doit pas dépasser 50 caractères.");
        }

        if (course.getStatus() == null || course.getStatus().trim().isEmpty()) {
            errors.add("Le statut est obligatoire.");
        } else if (course.getStatus().length() > 50) {
            errors.add("Le statut ne doit pas dépasser 50 caractères.");
        }

        if (course.getPublishedAt() == null) {
            errors.add("La date de publication est obligatoire.");
        }

        if (course.getPlatformLanguageId() <= 0) {
            errors.add("La langue associée est invalide.");
        }

        if (course.getAuthorId() != null && course.getAuthorId() <= 0) {
            errors.add("L'auteur est invalide.");
        }

        return errors;
    }
}