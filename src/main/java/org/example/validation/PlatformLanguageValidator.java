package org.example.validation;

import org.example.entities.pedagogicalcontent.PlatformLanguage;

import java.util.ArrayList;
import java.util.List;

public class PlatformLanguageValidator {

    public static List<String> validate(PlatformLanguage language) {
        List<String> errors = new ArrayList<>();

        if (language.getName() == null || language.getName().trim().isEmpty()) {
            errors.add("Le nom de la langue est obligatoire.");
        } else if (language.getName().trim().length() < 2) {
            errors.add("Le nom de la langue doit contenir au moins 2 caractères.");
        }

        if (language.getCode() == null || language.getCode().trim().isEmpty()) {
            errors.add("Le code de la langue est obligatoire.");
        } else if (language.getCode().trim().length() < 2 || language.getCode().trim().length() > 10) {
            errors.add("Le code doit contenir entre 2 et 10 caractères.");
        }

        if (language.getFlagUrl() != null && language.getFlagUrl().length() > 255) {
            errors.add("L'URL du drapeau ne doit pas dépasser 255 caractères.");
        }

        return errors;
    }
}