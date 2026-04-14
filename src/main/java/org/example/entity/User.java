package org.example.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {

    private Long   id;
    private String email;
    private String password;
    private String roles = "[\"ROLE_USER\"]";
    private String firstName;
    private String lastName;
    private String subscriptionPlan   = "FREE";
    private LocalDateTime subscriptionExpiry;
    private boolean isPremium         = false;
    private String  lastPaymentStatus;
    private String  status            = "active";
    private LocalDateTime createdAt;
    private boolean isVerified        = false;
    private String  stripeCustomerId;
    private String  stripeSubscriptionId;
    private boolean isBanned          = false;
    private String  banReason;
    private LearningStats learningStats;

    public User() {}

    // ── ID ────────────────────────────────────────────────────────────────────

    public Long getId()            { return id; }
    public void setId(Long id)     { this.id = id; }

    // ── Credentials ───────────────────────────────────────────────────────────

    public String getEmail()          { return email; }
    public void setEmail(String e)    { this.email = e; }

    public String getPassword()       { return password; }
    public void setPassword(String p) { this.password = p; }

    // ── Roles (stored as Symfony JSON array: ["ROLE_USER","ROLE_ADMIN"]) ──────

    /** Set raw JSON string directly (used by repository when loading from DB). */
    public void setRolesJson(String json) { this.roles = json; }

    /** Parse Symfony JSON array: ["ROLE_USER","ROLE_ADMIN"] */
    public List<String> getRoles() {
        if (roles == null || roles.isBlank()) return new ArrayList<>(List.of("ROLE_USER"));
        String t = roles.trim();
        if (t.startsWith("[")) {
            t = t.substring(1, t.length() - 1);
            if (t.isBlank()) return new ArrayList<>(List.of("ROLE_USER"));
            List<String> result = new ArrayList<>();
            for (String part : t.split(",")) {
                result.add(part.trim().replace("\"", ""));
            }
            return result;
        }
        return new ArrayList<>(List.of(t.split(",")));
    }

    /** Write Symfony JSON array format: ["ROLE_USER","ROLE_ADMIN"] */
    public void setRoles(List<String> r) {
        List<String> list = new ArrayList<>(r.stream().distinct().toList());
        if (!list.contains("ROLE_USER")) list.add(0, "ROLE_USER");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        this.roles = sb.append("]").toString();
    }

    public String getRolesJson()            { return roles; }
    public boolean hasRole(String role)     { return getRoles().contains(role); }

    // ── Name ──────────────────────────────────────────────────────────────────

    public String getFirstName()        { return firstName; }
    public void setFirstName(String v)  { this.firstName = v; }

    public String getLastName()         { return lastName; }
    public void setLastName(String v)   { this.lastName = v; }

    public String getFullName()         { return firstName + " " + lastName; }

    // ── Status ────────────────────────────────────────────────────────────────

    public String getStatus()           { return status; }
    public void setStatus(String v)     { this.status = v; }

    // ── Timestamps ────────────────────────────────────────────────────────────

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void          setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    // ── Subscription / premium ────────────────────────────────────────────────

    public String getSubscriptionPlan()  { return subscriptionPlan; }
    public void setSubscriptionPlan(String v) {
        this.subscriptionPlan = v;
        updatePremiumStatus();
    }

    public LocalDateTime getSubscriptionExpiry() { return subscriptionExpiry; }
    public void setSubscriptionExpiry(LocalDateTime v) {
        this.subscriptionExpiry = v;
        updatePremiumStatus();
    }

    public boolean isPremium() { return isPremium; }

    public String getLastPaymentStatus()         { return lastPaymentStatus; }
    public void   setLastPaymentStatus(String v) { this.lastPaymentStatus = v; }

    /**
     * Recalculates isPremium — mirrors Symfony's updatePremiumStatus().
     * Called by setSubscriptionPlan/setSubscriptionExpiry setters and
     * explicitly by the repository after loading all fields from the DB.
     */
    public void updatePremiumStatus() {
        this.isPremium =
                ("MONTHLY".equals(subscriptionPlan) || "YEARLY".equals(subscriptionPlan))
                        && subscriptionExpiry != null
                        && subscriptionExpiry.isAfter(LocalDateTime.now());
    }

    // ── Verification / Stripe ─────────────────────────────────────────────────

    public boolean isVerified()        { return isVerified; }
    public void setVerified(boolean v) { this.isVerified = v; }

    public String getStripeCustomerId()           { return stripeCustomerId; }
    public void   setStripeCustomerId(String v)   { this.stripeCustomerId = v; }
    public String getStripeSubscriptionId()       { return stripeSubscriptionId; }
    public void   setStripeSubscriptionId(String v) { this.stripeSubscriptionId = v; }

    // ── Ban ───────────────────────────────────────────────────────────────────

    public boolean isBanned()          { return isBanned; }
    public void setIsBanned(boolean v) { this.isBanned = v; }
    public String getBanReason()       { return banReason; }
    public void setBanReason(String v) { this.banReason = v; }

    // ── Learning stats (POJO association — no cascade, managed by repository) ─

    public LearningStats getLearningStats()        { return learningStats; }
    public void setLearningStats(LearningStats ls) {
        this.learningStats = ls;
        if (ls != null) ls.setUser(this);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', name='" + getFullName()
                + "', status='" + status + "', plan='" + subscriptionPlan
                + "', premium=" + isPremium + "}";
    }
}
