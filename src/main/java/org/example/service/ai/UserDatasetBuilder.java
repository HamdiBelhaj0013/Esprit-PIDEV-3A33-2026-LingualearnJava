package org.example.service.ai;

import org.example.entity.LearningStats;
import org.example.entity.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Converts User entities into a sanitized, AI-friendly dataset.
 * Strips passwords, Stripe IDs, and internal tokens before any AI call.
 */
public class UserDatasetBuilder {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Builds a sanitized list of user maps safe to send to the AI.
     * Fields included: id, name, email, status, isPremium, plan, expiry,
     *                  joined, paymentStatus, roles, xp, words, minutes.
     */
    public List<Map<String, Object>> buildDataset(List<User> users) {
        List<Map<String, Object>> dataset = new ArrayList<>(users.size());
        for (User u : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",            u.getId());
            row.put("name",          u.getFullName());
            row.put("email",         u.getEmail());
            row.put("status",        normalize(u.getStatus(), "active"));
            row.put("isPremium",     u.isPremium());
            row.put("plan",          normalize(u.getSubscriptionPlan(), "free"));
            row.put("expiry",        u.getSubscriptionExpiry() != null
                                         ? u.getSubscriptionExpiry().format(DATE_FMT)
                                         : null);
            row.put("joined",        u.getCreatedAt() != null
                                         ? u.getCreatedAt().format(DATE_FMT)
                                         : null);
            row.put("paymentStatus", u.getLastPaymentStatus() != null
                                         ? u.getLastPaymentStatus().toLowerCase()
                                         : null);
            row.put("roles",         u.getRoles());

            LearningStats ls = u.getLearningStats();
            row.put("xp",      ls != null ? ls.getTotalXP()             : 0);
            row.put("words",   ls != null ? ls.getWordsLearned()        : 0);
            row.put("minutes", ls != null ? ls.getTotalMinutesStudied() : 0);

            dataset.add(row);
        }
        return dataset;
    }

    /** Serializes the dataset to a pretty-printed JSON string. */
    public String toJson(List<Map<String, Object>> dataset) {
        JSONArray arr = new JSONArray();
        for (Map<String, Object> row : dataset) {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object v = entry.getValue();
                if (v == null) {
                    obj.put(entry.getKey(), JSONObject.NULL);
                } else if (v instanceof List<?> list) {
                    obj.put(entry.getKey(), new JSONArray(list));
                } else {
                    obj.put(entry.getKey(), v);
                }
            }
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private String normalize(String value, String fallback) {
        return (value != null && !value.isBlank())
            ? value.toLowerCase()
            : fallback;
    }
}
