package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "roles", nullable = false, columnDefinition = "json")
    private String roles = "[\"ROLE_USER\"]";

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "subscription_plan", nullable = false)
    private String subscriptionPlan = "FREE";

    @Column(name = "subscription_expiry")
    private LocalDateTime subscriptionExpiry;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium = false;

    @Column(name = "last_payment_status")
    private String lastPaymentStatus;

    @Column(name = "status", nullable = false)
    private String status = "active";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "is_banned", nullable = false)
    private boolean isBanned = false;

    @Column(name = "ban_reason")
    private String banReason;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private LearningStats learningStats;

    public User() {}

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PostLoad
    public void postLoad() {
        updatePremiumStatus();
    }

    // ── Getters / setters ────────────────────────────────────────────────────

    public Long getId()               { return id; }

    /**
     * Needed by JDBC repositories (TestResultRepository, etc.)
     * to set the id after reading from ResultSet.
     * JPA never calls this — it manages the id field directly.
     */
    public void setId(Long id)        { this.id = id; }

    public String getEmail()          { return email; }
    public void setEmail(String e)    { this.email = e; }

    public String getPassword()       { return password; }
    public void setPassword(String p) { this.password = p; }

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

    public boolean hasRole(String role) { return getRoles().contains(role); }

    public String getFirstName()        { return firstName; }
    public void setFirstName(String v)  { this.firstName = v; }
    public String getLastName()         { return lastName; }
    public void setLastName(String v)   { this.lastName = v; }
    public String getFullName()         { return firstName + " " + lastName; }

    public String getStatus()           { return status; }
    public void setStatus(String v)     { this.status = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    /** Returns the raw JSON string stored in the database (e.g. ["ROLE_USER","ROLE_ADMIN"]). */
    public String getRolesJson() { return roles; }
    /** Sets the roles field directly from the raw JSON string read from the database. */
    public void setRolesJson(String json) { this.roles = json; }

    public String getSubscriptionPlan() { return subscriptionPlan; }
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

    public String getLastPaymentStatus()       { return lastPaymentStatus; }
    public void setLastPaymentStatus(String v) { this.lastPaymentStatus = v; }

    void updatePremiumStatus() {
        this.isPremium =
                ("MONTHLY".equals(subscriptionPlan) || "YEARLY".equals(subscriptionPlan))
                        && subscriptionExpiry != null
                        && subscriptionExpiry.isAfter(LocalDateTime.now());
    }

    public boolean isVerified()        { return isVerified; }
    public void setVerified(boolean v) { this.isVerified = v; }

    public String getStripeCustomerId()           { return stripeCustomerId; }
    public void setStripeCustomerId(String v)     { this.stripeCustomerId = v; }
    public String getStripeSubscriptionId()       { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String v) { this.stripeSubscriptionId = v; }

    public boolean isBanned()          { return isBanned; }
    public void setIsBanned(boolean v) { this.isBanned = v; }
    public String getBanReason()       { return banReason; }
    public void setBanReason(String v) { this.banReason = v; }

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