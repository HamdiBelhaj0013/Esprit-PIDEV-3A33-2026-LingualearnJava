package org.example.service.ai;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Applies fast regex-based pre-filtering to narrow the user dataset before
 * handing it to the AI, reducing token usage and improving precision.
 *
 * All active filters are combined with AND logic.
 */
public class JavaPreFilter {

    // ── result carrier ────────────────────────────────────────────────────────

    public static class FilterResult {
        /** The subset of users that matched all active filters. */
        public final List<Map<String, Object>> filtered;
        /**
         * True when at least one filter was applied AND the result is non-empty,
         * indicating the pre-filter alone produced a meaningful answer.
         */
        public final boolean resolved;
        /** Human-readable description of which filters were applied. */
        public final String  reasoning;

        public FilterResult(List<Map<String, Object>> filtered,
                            boolean resolved,
                            String reasoning) {
            this.filtered  = filtered;
            this.resolved  = resolved;
            this.reasoning = reasoning;
        }
    }

    // ── compiled patterns ─────────────────────────────────────────────────────

    private static final Pattern P_SUSPENDED =
        Pattern.compile("\\bsuspend(ed|ion)?\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_DELETED =
        Pattern.compile("\\bdelet(ed|ion)?\\b", Pattern.CASE_INSENSITIVE);

    // "active" NOT immediately followed by suspend/delete/ban words
    private static final Pattern P_ACTIVE =
        Pattern.compile("\\bactive\\b(?!\\s*(suspend|delet|ban))", Pattern.CASE_INSENSITIVE);

    // "premium" NOT preceded by "not"
    private static final Pattern P_PREMIUM =
        Pattern.compile("(?<!not\\s)\\bpremium\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_FREE =
        Pattern.compile("\\bfree\\s+(user|plan|account)\\b|\\bplan[:\\s]+free\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern P_MONTHLY =
        Pattern.compile("\\bmonthly\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_YEARLY =
        Pattern.compile("\\byearly\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_FAILED_PAYMENT =
        Pattern.compile("\\b(failed\\s+payment|payment\\s+fail(ed|ure)?|payment\\s+issue)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern P_NEW_USER =
        Pattern.compile("\\b(new\\s+user|recently\\s+joined|joined\\s+recently)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern P_EXPIRING =
        Pattern.compile("\\bexpiring\\s+soon\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_TOP_N =
        Pattern.compile("\\btop\\s+(\\d+)\\s*(learner|user|xp|highest)?\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern P_HIGHEST_XP =
        Pattern.compile("\\bhighest\\s+xp\\b", Pattern.CASE_INSENSITIVE);

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Applies all matching filters from {@code query} against {@code dataset}.
     *
     * @param query   raw admin natural-language input
     * @param dataset sanitized user list from {@link UserDatasetBuilder}
     * @return FilterResult with the narrowed list and reasoning
     */
    public FilterResult filter(String query, List<Map<String, Object>> dataset) {
        List<Map<String, Object>> result = new ArrayList<>(dataset);
        List<String> reasons = new ArrayList<>();

        // ── status ────────────────────────────────────────────────────────────
        if (P_SUSPENDED.matcher(query).find()) {
            result = byStatus(result, "suspended");
            reasons.add("status=suspended");
        } else if (P_DELETED.matcher(query).find()) {
            result = byStatus(result, "deleted");
            reasons.add("status=deleted");
        } else if (P_ACTIVE.matcher(query).find()) {
            result = byStatus(result, "active");
            reasons.add("status=active");
        }

        // ── premium / plan ────────────────────────────────────────────────────
        boolean wantsPremium = P_PREMIUM.matcher(query).find()
            && !query.toLowerCase().contains("not premium");
        boolean wantsFree    = P_FREE.matcher(query).find();

        if (wantsPremium && !wantsFree) {
            result = result.stream()
                .filter(u -> Boolean.TRUE.equals(u.get("isPremium")))
                .collect(Collectors.toList());
            reasons.add("isPremium=true");
        } else if (wantsFree && !wantsPremium) {
            result = result.stream()
                .filter(u -> !Boolean.TRUE.equals(u.get("isPremium")))
                .collect(Collectors.toList());
            reasons.add("isPremium=false");
        }

        if (P_MONTHLY.matcher(query).find()) {
            result = byPlan(result, "monthly");
            reasons.add("plan=monthly");
        } else if (P_YEARLY.matcher(query).find()) {
            result = byPlan(result, "yearly");
            reasons.add("plan=yearly");
        }

        // ── payment status ────────────────────────────────────────────────────
        if (P_FAILED_PAYMENT.matcher(query).find()) {
            result = result.stream()
                .filter(u -> "failed".equals(u.get("paymentStatus")))
                .collect(Collectors.toList());
            reasons.add("paymentStatus=failed");
        }

        // ── new users (joined within last 6 months) ───────────────────────────
        if (P_NEW_USER.matcher(query).find()) {
            LocalDate cutoff = LocalDate.now().minusMonths(6);
            result = result.stream()
                .filter(u -> parseDate(u.get("joined")) != null
                          && parseDate(u.get("joined")).isAfter(cutoff))
                .collect(Collectors.toList());
            reasons.add("joined within last 6 months");
        }

        // ── expiring soon (subscription expiry within 30 days) ────────────────
        if (P_EXPIRING.matcher(query).find()) {
            LocalDate today  = LocalDate.now();
            LocalDate in30   = today.plusDays(30);
            result = result.stream()
                .filter(u -> {
                    LocalDate exp = parseDate(u.get("expiry"));
                    return exp != null && !exp.isBefore(today) && exp.isBefore(in30);
                })
                .collect(Collectors.toList());
            reasons.add("expiry within next 30 days");
        }

        // ── top N by XP ───────────────────────────────────────────────────────
        int topN = 0;
        Matcher mTopN = P_TOP_N.matcher(query);
        if (mTopN.find()) {
            topN = Integer.parseInt(mTopN.group(1));
        } else if (P_HIGHEST_XP.matcher(query).find()) {
            topN = 5;
        }
        if (topN > 0) {
            int limit = topN;
            result = result.stream()
                .sorted(Comparator.comparingInt(
                    (Map<String, Object> u) -> toInt(u.get("xp"))).reversed())
                .limit(limit)
                .collect(Collectors.toList());
            reasons.add("top " + limit + " by XP");
        }

        boolean anyFilter = !reasons.isEmpty();
        String reasoning = anyFilter
            ? "Pre-filter applied: " + String.join(", ", reasons)
                + " → " + result.size() + " user(s) matched"
            : "No specific filter pattern detected; full dataset passed to AI";

        return new FilterResult(result, anyFilter && !result.isEmpty(), reasoning);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<Map<String, Object>> byStatus(List<Map<String, Object>> src, String status) {
        return src.stream()
            .filter(u -> status.equals(u.get("status")))
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> byPlan(List<Map<String, Object>> src, String plan) {
        return src.stream()
            .filter(u -> plan.equals(u.get("plan")))
            .collect(Collectors.toList());
    }

    private LocalDate parseDate(Object value) {
        if (!(value instanceof String s) || s.isEmpty()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return 0;
    }
}
