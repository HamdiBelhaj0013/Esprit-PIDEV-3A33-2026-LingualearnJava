package org.example.service.ai;

import org.example.entity.User;
import org.example.repository.UserRepository;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates all AI-powered features for the admin panel:
 *   1. naturalLanguageSearch — pre-filter + AI summary
 *   2. generateUserInsight   — per-user narrative from the AI
 *   3. chat                  — conversational assistant with optional actions
 *   4. analyzePlatformTrends — aggregate stats analysis
 *
 * Ollama calls are BLOCKING — always invoke from a background thread.
 */
public class AdminAiService {

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public static class ChatMessage {
        public final String role;
        public final String content;
        public ChatMessage(String role, String content) {
            this.role    = role;
            this.content = content;
        }
    }

    public static class SearchResult {
        public final List<Map<String, Object>> users;
        public final String summary;
        public final String reasoning;
        public SearchResult(List<Map<String, Object>> users,
                            String summary, String reasoning) {
            this.users     = users;
            this.summary   = summary;
            this.reasoning = reasoning;
        }
    }

    /**
     * Carries the AI's parsed response.
     * {@code action} is always set (defaults to "READ_ONLY").
     * {@code actionParams} is only non-null when the AI suggested a mutation.
     * The controller decides whether to execute after showing a confirmation dialog.
     */
    public static class ChatResult {
        public final String     reply;
        public final String     action;
        public final JSONObject actionParams;
        public ChatResult(String reply, String action, JSONObject actionParams) {
            this.reply        = reply;
            this.action       = action;
            this.actionParams = actionParams;
        }
    }

    public static class QuickStats {
        public final long   totalUsers;
        public final long   premiumCount;
        public final double avgXP;
        public QuickStats(long totalUsers, long premiumCount, double avgXP) {
            this.totalUsers   = totalUsers;
            this.premiumCount = premiumCount;
            this.avgXP        = avgXP;
        }
    }

    // ── collaborators ─────────────────────────────────────────────────────────

    private final OllamaService      ollama;
    private final UserDatasetBuilder datasetBuilder;
    private final JavaPreFilter      preFilter;
    private final AiActionExecutor   actionExecutor;
    private final UserRepository     userRepo;

    public AdminAiService(UserRepository userRepo) {
        this.userRepo       = userRepo;
        this.ollama         = new OllamaService();
        this.datasetBuilder = new UserDatasetBuilder();
        this.preFilter      = new JavaPreFilter();
        this.actionExecutor = new AiActionExecutor(userRepo);
    }

    // ── features ──────────────────────────────────────────────────────────────

    /** Returns all users (delegates to UserRepository). */
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    /** Computes quick platform stats without an Ollama call. */
    public QuickStats getQuickStats() {
        List<Map<String, Object>> dataset =
            datasetBuilder.buildDataset(userRepo.findAll());
        long   total   = dataset.size();
        long   premium = dataset.stream()
            .filter(u -> Boolean.TRUE.equals(u.get("isPremium"))).count();
        double avgXP   = dataset.stream()
            .mapToInt(u -> toInt(u.get("xp"))).average().orElse(0);
        return new QuickStats(total, premium, avgXP);
    }

    /** Natural-language user search: pre-filter → AI summary. */
    public SearchResult naturalLanguageSearch(String query) {
        List<User>               all      = userRepo.findAll();
        List<Map<String, Object>> dataset = datasetBuilder.buildDataset(all);

        JavaPreFilter.FilterResult fr = preFilter.filter(query, dataset);

        String userMsg = "Admin query: \"" + query + "\"\n\n"
            + "Matching users (" + fr.filtered.size() + "):\n"
            + datasetBuilder.toJson(fr.filtered);

        List<Map<String, String>> messages =
            List.of(Map.of("role", "user", "content", userMsg));

        String summary = ollama.chat(messages, SEARCH_SYSTEM_PROMPT);
        return new SearchResult(fr.filtered, summary, fr.reasoning);
    }

    /** Generates a human-readable insight paragraph for a single user. */
    public String generateUserInsight(long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String userJson = datasetBuilder.toJson(
            datasetBuilder.buildDataset(List.of(user)));

        List<Map<String, String>> messages = List.of(Map.of(
            "role",    "user",
            "content", "Generate an admin insight for this user:\n" + userJson
        ));
        return ollama.chat(messages, INSIGHT_SYSTEM_PROMPT);
    }

    /**
     * Conversational chat. Returns a ChatResult whose {@code action} field
     * tells the controller what (if anything) needs to be executed after
     * admin confirmation.
     *
     * @param history    prior turns (excluding the new message)
     * @param newMessage the latest user message
     */
    public ChatResult chat(List<ChatMessage> history, String newMessage) {
        List<User>               all      = userRepo.findAll();
        List<Map<String, Object>> dataset = datasetBuilder.buildDataset(all);
        String systemPrompt = buildChatSystemPrompt(datasetBuilder.toJson(dataset));

        List<Map<String, String>> msgs = new ArrayList<>();
        for (ChatMessage cm : history) {
            msgs.add(Map.of("role", cm.role, "content", cm.content));
        }
        msgs.add(Map.of("role", "user", "content", newMessage));

        String raw = ollama.chat(msgs, systemPrompt);
        return parseChatResponse(raw);
    }

    /**
     * Executes an action that was extracted from a prior chat response.
     * Called by the controller AFTER the admin confirms the dialog.
     */
    public AiActionExecutor.ActionResult executeAction(String action, JSONObject params) {
        try {
            AiActionExecutor.Action a = AiActionExecutor.Action.valueOf(action);
            return actionExecutor.execute(a,
                params != null ? params : new JSONObject());
        } catch (IllegalArgumentException e) {
            return new AiActionExecutor.ActionResult(
                false, "Unknown action: " + action, null);
        }
    }

    /** Aggregate platform-trend analysis (not in the Symfony version). */
    public String analyzePlatformTrends() {
        List<User>               all      = userRepo.findAll();
        List<Map<String, Object>> dataset = datasetBuilder.buildDataset(all);

        long   totalUsers    = dataset.size();
        long   premiumCount  = dataset.stream()
            .filter(u -> Boolean.TRUE.equals(u.get("isPremium"))).count();
        double avgXP         = dataset.stream()
            .mapToInt(u -> toInt(u.get("xp"))).average().orElse(0);
        long   totalMinutes  = dataset.stream()
            .mapToLong(u -> toInt(u.get("minutes"))).sum();

        List<Map<String, Object>> top3 = dataset.stream()
            .sorted(Comparator.comparingInt(
                (Map<String, Object> u) -> toInt(u.get("xp"))).reversed())
            .limit(3)
            .collect(Collectors.toList());

        JSONArray top3Json = new JSONArray();
        for (Map<String, Object> u : top3) {
            top3Json.put(new JSONObject()
                .put("name",    u.getOrDefault("name",    "Unknown"))
                .put("xp",      toInt(u.get("xp")))
                .put("minutes", toInt(u.get("minutes"))));
        }

        String statsJson = new JSONObject()
            .put("totalUsers",         totalUsers)
            .put("premiumCount",       premiumCount)
            .put("freeCount",          totalUsers - premiumCount)
            .put("premiumRate",        totalUsers > 0
                ? Math.round(premiumCount * 100.0 / totalUsers) + "%" : "0%")
            .put("avgXP",              Math.round(avgXP))
            .put("totalMinutesStudied", totalMinutes)
            .put("top3ActiveUsers",    top3Json)
            .toString(2);

        List<Map<String, String>> messages = List.of(Map.of(
            "role",    "user",
            "content", "Analyze these platform statistics:\n" + statsJson
        ));
        return ollama.chat(messages, ANALYTICS_SYSTEM_PROMPT);
    }

    // ── response parsing ──────────────────────────────────────────────────────

    /**
     * Extracts the JSON block the AI is instructed to always include.
     * Falls back to READ_ONLY if parsing fails.
     */
    private ChatResult parseChatResponse(String raw) {
        String extracted = extractJsonBlock(raw);
        if (extracted == null) {
            return new ChatResult(raw.trim(), "READ_ONLY", null);
        }
        try {
            JSONObject json   = new JSONObject(extracted);
            String     action = json.optString("action", "READ_ONLY");
            String     reply  = json.optString("reply", "");
            if (reply.isBlank()) {
                // The model may have put readable text outside the JSON block
                int jsonStart = raw.indexOf('{');
                reply = (jsonStart > 0) ? raw.substring(0, jsonStart).trim() : raw.trim();
                if (reply.isBlank()) reply = extracted;
            }
            JSONObject params = json.optJSONObject("params");
            return new ChatResult(reply, action, params);
        } catch (Exception e) {
            return new ChatResult(raw.trim(), "READ_ONLY", null);
        }
    }

    /** Finds the outermost {...} block in {@code text}. */
    private String extractJsonBlock(String text) {
        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) return null;
        return text.substring(start, end + 1);
    }

    private int toInt(Object v) {
        return (v instanceof Number n) ? n.intValue() : 0;
    }

    // ── system prompts ────────────────────────────────────────────────────────

    private static final String SEARCH_SYSTEM_PROMPT =
        "You are an AI assistant for the LinguaLearn admin panel. "
        + "Given an admin's natural-language query and a filtered JSON list of matching users, "
        + "write a concise (max 3 sentences) plain-English summary of the results. "
        + "Highlight patterns, counts, or anomalies. Never expose passwords or Stripe IDs.";

    private static final String INSIGHT_SYSTEM_PROMPT =
        "You are an AI assistant for the LinguaLearn admin panel. "
        + "Generate a single insightful paragraph about the given user's learning journey, "
        + "subscription status, and engagement. Suggest one admin action if appropriate. "
        + "Be factual and data-driven. Never mention passwords, IDs, or Stripe fields.";

    private static final String ANALYTICS_SYSTEM_PROMPT =
        "You are an analytics AI for LinguaLearn, a language-learning SaaS platform. "
        + "Analyze the provided aggregate statistics and produce a professional multi-paragraph report covering: "
        + "(1) overall user base health, (2) premium conversion and revenue indicators, "
        + "(3) learning engagement trends, (4) top-performer highlights, "
        + "(5) actionable recommendations for the admin team. "
        + "Be specific and data-driven.";

    private String buildChatSystemPrompt(String datasetJson) {
        return "You are an AI admin assistant for LinguaLearn, a language-learning platform.\n"
            + "You MUST ALWAYS reply with exactly this JSON structure — no extra text outside it:\n"
            + "{\n"
            + "  \"action\": \"ACTION_TYPE\",\n"
            + "  \"params\": {},\n"
            + "  \"reply\": \"Your human-readable response here\"\n"
            + "}\n\n"
            + "Available ACTION_TYPE values:\n"
            + "  READ_ONLY        — for queries, analysis, explanations (no DB change)\n"
            + "  CREATE_USER      — params: {\"name\": \"First Last\", \"email\": \"user@example.com\","
            + " \"password\": \"optional_6char_min\"}\n"
            + "  SUSPEND_USERS    — params: {\"user_ids\": [id, ...]}\n"
            + "  ACTIVATE_USERS   — params: {\"user_ids\": [id, ...]}\n"
            + "  DELETE_USERS     — params: {\"user_ids\": [id, ...]} — IRREVERSIBLE\n"
            + "  CHANGE_PLAN      — params: {\"user_ids\": [...], \"plan\": \"FREE|MONTHLY|YEARLY\"}\n"
            + "  CHANGE_ROLE      — params: {\"user_ids\": [...], \"role\": \"ROLE_USER|ROLE_ADMIN\"}\n"
            + "  RESET_PASSWORD   — params: {\"user_ids\": [...], \"password\": \"newpass\"}\n"
            + "  EXPORT_USER_IDS  — params: {\"user_ids\": [...]}\n\n"
            + "CREATE_USER rules:\n"
            + "- 'name' is required — use the full name the admin provides (e.g. 'John Smith').\n"
            + "- 'email' is required — must look like a valid email address.\n"
            + "- 'password' is optional — if provided it must be at least 6 characters; "
            + "if absent the system generates a temporary password automatically.\n\n"
            + "CRITICAL RULES:\n"
            + "- NEVER mutate users whose roles contain ROLE_ADMIN.\n"
            + "- NEVER include passwords or Stripe IDs in \"reply\".\n"
            + "- Use READ_ONLY for all informational requests.\n\n"
            + "Current user database (sanitized):\n"
            + datasetJson;
    }

    // ── AI-FEATURE: notification-writer ───────────────────────────────────────

    /**
     * Generates a ready-to-send notification message for a specific user.
     * <p>
     * The {@code intent} parameter lets the admin briefly describe the purpose
     * (e.g. "account suspended", "welcome back offer"). The model drafts a
     * professional, 1–3 sentence message whose tone matches the notification type.
     * <p>
     * Blocking — must be called from a background thread.
     *
     * @param userId           the target user's ID
     * @param notificationType one of: info, warning, success, system
     * @param intent           brief description of what the notification is about
     * @return a ready-to-send notification message string
     */
    public String generateNotificationMessage(long userId,
                                              String notificationType,
                                              String intent) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String userJson = datasetBuilder.toJson(
            datasetBuilder.buildDataset(List.of(user)));

        List<Map<String, String>> messages = List.of(Map.of(
            "role",    "user",
            "content", "Write a \"" + notificationType + "\" notification for this user.\n"
                     + "Intent: " + intent + "\n\n"
                     + "User context:\n" + userJson
        ));
        return ollama.chat(messages, NOTIFICATION_WRITER_PROMPT);
    }

    // ── AI-FEATURE: stats-summary ─────────────────────────────────────────────

    /**
     * Generates a concise plain-English summary of a user's learning stats.
     * <p>
     * Describes their progress (XP, words learned, minutes studied) in a
     * motivational, data-driven paragraph. Safe to display in the admin panel.
     * <p>
     * Blocking — must be called from a background thread.
     *
     * @param userId the target user's ID
     * @return a summary paragraph (2–4 sentences)
     */
    public String generateStatsSummary(long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String userJson = datasetBuilder.toJson(
            datasetBuilder.buildDataset(List.of(user)));

        List<Map<String, String>> messages = List.of(Map.of(
            "role",    "user",
            "content", "Summarize the learning progress for this user:\n" + userJson
        ));
        return ollama.chat(messages, STATS_SUMMARY_PROMPT);
    }

    private static final String NOTIFICATION_WRITER_PROMPT =
        "You are an admin notification writer for LinguaLearn, a language-learning platform. "
        + "Given a user's profile, a notification type, and an intent, write a concise, "
        + "professional notification message (1–3 sentences). "
        + "Match the tone to the type: warm for 'success', firm for 'warning', "
        + "neutral for 'info', authoritative for 'system'. "
        + "Address the user by first name. Never mention passwords or internal IDs. "
        + "Reply with only the notification text — no quotes, no preamble.";

    private static final String STATS_SUMMARY_PROMPT =
        "You are a learning analytics assistant for LinguaLearn. "
        + "Given a user's learning stats (XP, words learned, minutes studied), "
        + "write a concise (2–4 sentences) plain-English summary of their progress. "
        + "Be encouraging and data-driven. Never mention passwords or Stripe fields. "
        + "Reply with only the summary text — no quotes, no preamble.";
}
