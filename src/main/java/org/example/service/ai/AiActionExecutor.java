package org.example.service.ai;

import org.example.entity.LearningStats;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes user-management actions suggested by the AI.
 *
 * Safety rule: users with ROLE_ADMIN in their roles are NEVER mutated.
 */
public class AiActionExecutor {

    // ── result / action types ─────────────────────────────────────────────────

    public static class ActionResult {
        public final boolean success;
        public final String  message;
        public final Object  data;

        public ActionResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data    = data;
        }
    }

    public enum Action {
        READ_ONLY,
        CREATE_USER,
        DELETE_USERS,
        SUSPEND_USERS,
        ACTIVATE_USERS,
        CHANGE_PLAN,
        CHANGE_ROLE,
        RESET_PASSWORD,
        EXPORT_USER_IDS
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private final UserRepository userRepo;

    public AiActionExecutor(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // ── dispatch ──────────────────────────────────────────────────────────────

    public ActionResult execute(Action action, JSONObject params) {
        try {
            return switch (action) {
                case READ_ONLY      -> new ActionResult(true,  "Read-only — no changes made.", null);
                case SUSPEND_USERS  -> suspendUsers(params);
                case ACTIVATE_USERS -> activateUsers(params);
                case DELETE_USERS   -> deleteUsers(params);
                case CHANGE_PLAN    -> changePlan(params);
                case CHANGE_ROLE    -> changeRole(params);
                case RESET_PASSWORD -> resetPassword(params);
                case EXPORT_USER_IDS-> exportUserIds(params);
                case CREATE_USER    -> createUser(params);
            };
        } catch (Exception e) {
            return new ActionResult(false, "Action failed: " + e.getMessage(), null);
        }
    }

    // ── action implementations ────────────────────────────────────────────────

    private ActionResult createUser(JSONObject params) {
        System.out.println("[AiActionExecutor] CREATE_USER params received: " + params.toString(2));

        // ── name ──────────────────────────────────────────────────────────────
        String rawName  = params.optString("name", "").trim();
        String firstName;
        String lastName;
        if (rawName.isEmpty()) {
            System.out.println("[AiActionExecutor] CREATE_USER: 'name' param is missing or empty");
            return new ActionResult(false,
                "CREATE_USER failed: 'name' is required.", null);
        }
        String[] nameParts = rawName.split("\\s+", 2);
        firstName = nameParts[0];
        lastName  = (nameParts.length > 1) ? nameParts[1] : "";
        System.out.println("[AiActionExecutor] CREATE_USER: firstName='" + firstName
            + "', lastName='" + lastName + "'");

        // ── email ─────────────────────────────────────────────────────────────
        String email = params.optString("email", "").trim();
        if (email.isEmpty()) {
            System.out.println("[AiActionExecutor] CREATE_USER: 'email' param is missing or empty");
            return new ActionResult(false,
                "CREATE_USER failed: 'email' is required.", null);
        }
        if (userRepo.findByEmail(email).isPresent()) {
            System.out.println("[AiActionExecutor] CREATE_USER: email already in use — " + email);
            return new ActionResult(false,
                "CREATE_USER failed: email '" + email + "' is already in use.", null);
        }

        // ── password ──────────────────────────────────────────────────────────
        String rawPassword = params.optString("password", "").trim();
        if (rawPassword.length() < 6) {
            rawPassword = generateTempPassword();
            System.out.println("[AiActionExecutor] CREATE_USER: password too short or absent, "
                + "generated temp password: " + rawPassword);
        } else {
            System.out.println("[AiActionExecutor] CREATE_USER: using provided password (length="
                + rawPassword.length() + ")");
        }
        String hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        // ── build user ────────────────────────────────────────────────────────
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(hashed);
        user.setStatus("active");
        user.setVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setRoles(new ArrayList<>(List.of("ROLE_USER")));
        System.out.println("[AiActionExecutor] CREATE_USER: user object ready — " + email);

        // ── build stats ───────────────────────────────────────────────────────
        LearningStats ls = new LearningStats();
        ls.setUser(user);

        // ── persist ───────────────────────────────────────────────────────────
        userRepo.saveWithStats(user, ls);
        System.out.println("[AiActionExecutor] CREATE_USER: saved successfully — " + email);

        return new ActionResult(true,
            "User '" + rawName + "' (" + email + ") created."
            + (rawPassword.startsWith("Temp@")
                ? " Temp password: " + rawPassword : ""),
            email);
    }

    private ActionResult suspendUsers(JSONObject params) {
        List<Long> ids = extractIds(params);
        int count = 0;
        for (Long id : ids) {
            User u = findSafe(id);
            if (u == null) continue;
            u.setStatus("suspended");
            userRepo.save(u);
            count++;
        }
        return new ActionResult(true, "Suspended " + count + " user(s).", ids);
    }

    private ActionResult activateUsers(JSONObject params) {
        List<Long> ids = extractIds(params);
        int count = 0;
        for (Long id : ids) {
            User u = findSafe(id);
            if (u == null) continue;
            u.setStatus("active");
            userRepo.save(u);
            count++;
        }
        return new ActionResult(true, "Activated " + count + " user(s).", ids);
    }

    private ActionResult deleteUsers(JSONObject params) {
        List<Long> ids = extractIds(params);
        int count = 0;
        for (Long id : ids) {
            User u = findSafe(id);
            if (u == null) continue;
            userRepo.delete(u);
            count++;
        }
        return new ActionResult(true, "Deleted " + count + " user(s).", ids);
    }

    private ActionResult changePlan(JSONObject params) {
        List<Long> ids  = extractIds(params);
        String    plan  = params.optString("plan", "FREE").toUpperCase();
        int count = 0;
        for (Long id : ids) {
            User u = findSafe(id);
            if (u == null) continue;
            u.setSubscriptionPlan(plan);
            if ("FREE".equals(plan)) {
                u.setSubscriptionExpiry(null);
            } else {
                int months = "MONTHLY".equals(plan) ? 1 : 12;
                u.setSubscriptionExpiry(LocalDateTime.now().plusMonths(months));
            }
            userRepo.save(u);
            count++;
        }
        return new ActionResult(true,
            "Changed plan to " + plan + " for " + count + " user(s).", ids);
    }

    private ActionResult changeRole(JSONObject params) {
        List<Long> ids  = extractIds(params);
        String    role  = params.optString("role", "ROLE_USER");
        int count = 0;
        for (Long id : ids) {
            User u = findSafe(id);
            if (u == null) continue;
            List<String> roles = new ArrayList<>();
            roles.add(role);
            u.setRoles(roles);
            userRepo.save(u);
            count++;
        }
        return new ActionResult(true,
            "Changed role to " + role + " for " + count + " user(s).", ids);
    }

    private ActionResult resetPassword(JSONObject params) {
        List<Long> ids     = extractIds(params);
        String     newPwd  = params.optString("password", generateTempPassword());
        String     hashed  = BCrypt.hashpw(newPwd, BCrypt.gensalt());
        int count = 0;
        for (Long id : ids) {
            User u = findSafe(id);
            if (u == null) continue;
            u.setPassword(hashed);
            userRepo.save(u);
            count++;
        }
        return new ActionResult(true,
            "Reset password for " + count + " user(s). Temp password: " + newPwd, ids);
    }

    private ActionResult exportUserIds(JSONObject params) {
        List<Long> ids = extractIds(params);
        return new ActionResult(true, "Exported " + ids.size() + " user ID(s).", ids);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Returns null (and skips) if the user doesn't exist or is an admin. */
    private User findSafe(Long id) {
        return userRepo.findById(id)
            .filter(u -> !u.getRoles().contains("ROLE_ADMIN"))
            .orElse(null);
    }

    private List<Long> extractIds(JSONObject params) {
        List<Long> ids = new ArrayList<>();
        JSONArray arr = params.optJSONArray("user_ids");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) ids.add(arr.getLong(i));
        } else if (params.has("user_id")) {
            ids.add(params.getLong("user_id"));
        }
        return ids;
    }

    private String generateTempPassword() {
        int rand = 100_000 + (int) (Math.random() * 900_000);
        return "Temp@" + rand;
    }
}
