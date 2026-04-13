package org.example.validation;

import org.example.entity.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.regex.Pattern;


public class ValidationService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    private static final Set<String> VALID_PLANS = Set.of("FREE", "MONTHLY", "YEARLY");

    private static final Set<String> VALID_ROLES =
        Set.of("ROLE_USER", "ROLE_ADMIN", "ROLE_TEACHER");

    // JPA Bean Validation — used by validateOrThrow()
    private final Validator validator;

    public ValidationService() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(fieldName + " must not be blank.");
    }

    /**
     * Throws if value is shorter than min characters.
     * Mirrors Symfony's @Length(min=...) constraint.
     */
    public static void requireMinLength(String value, String fieldName, int min) {
        requireNonBlank(value, fieldName);
        if (value.trim().length() < min)
            throw new IllegalArgumentException(
                fieldName + " must be at least " + min + " characters long.");
    }

    /**
     * Throws if email format is invalid.
     * Mirrors Symfony's @Email constraint.
     */
    public static void requireValidEmail(String email) {
        requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new IllegalArgumentException("'" + email.trim() + "' is not a valid email address.");
    }

    /**
     * Throws if the two password strings differ.
     * Mirrors Symfony's RegistrationController password-match check.
     */
    public static void requirePasswordsMatch(String password, String confirm) {
        if (!password.equals(confirm))
            throw new IllegalArgumentException("Passwords do not match.");
    }

    /**
     * Throws if the plan is not FREE, MONTHLY, or YEARLY.
     * Mirrors Symfony's @Choice constraint on subscriptionPlan.
     */
    public static void requireValidPlan(String plan) {
        if (plan == null || !VALID_PLANS.contains(plan.toUpperCase()))
            throw new IllegalArgumentException(
                "Invalid subscription plan '" + plan + "'. Allowed: FREE, MONTHLY, YEARLY.");
    }

    /**
     * Throws if the role is not in the allowed set.
     * Mirrors Symfony's role-name guard in UserService.
     */
    public static void requireValidRole(String role) {
        if (role == null || !VALID_ROLES.contains(role.toUpperCase()))
            throw new IllegalArgumentException(
                "Invalid role '" + role + "'. Allowed: " + VALID_ROLES);
    }

    // ── Bean Validation (entity-level) ────────────────────────────────────────

    /**
     * Runs Jakarta Bean Validation (@NotBlank, @Size, @Pattern, etc.) on any
     * entity and throws with a combined error message if violations are found.
     * Mirrors Symfony's automatic form-level entity validation.
     */
    public <T> void validateOrThrow(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (violations.isEmpty()) return;

        StringBuilder sb = new StringBuilder("Validation failed:");
        for (ConstraintViolation<T> v : violations) {
            sb.append("\n  • ").append(v.getPropertyPath()).append(": ").append(v.getMessage());
        }
        throw new IllegalArgumentException(sb.toString());
    }
}
