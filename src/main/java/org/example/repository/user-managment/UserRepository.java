package org.example.repository;

import org.example.entity.LearningStats;
import org.example.entity.User;
import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    // ── Base SELECT (users LEFT JOIN learning_stats) ─────────────────────────

    private static final String SELECT_BASE =
        "SELECT u.id, u.email, u.password, u.roles, u.first_name, u.last_name, " +
        "u.subscription_plan, u.subscription_expiry, u.is_premium, u.last_payment_status, " +
        "u.status, u.created_at, u.is_verified, u.stripe_customer_id, u.stripe_subscription_id, " +
        "u.is_banned, u.ban_reason, " +
        "ls.id AS ls_id, ls.total_xp, ls.words_learned, ls.total_minutes_studied, ls.last_study_session " +
        "FROM users u LEFT JOIN learning_stats ls ON ls.user_id = u.id";

    // ── Connection ────────────────────────────────────────────────────────────

    private Connection conn() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRolesJson(rs.getString("roles"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setStatus(rs.getString("status"));
        u.setLastPaymentStatus(rs.getString("last_payment_status"));

        // Set expiry BEFORE plan so that the final updatePremiumStatus() call inside
        // setSubscriptionPlan() sees the correct expiry value.
        Timestamp expiry = rs.getTimestamp("subscription_expiry");
        u.setSubscriptionExpiry(expiry != null ? expiry.toLocalDateTime() : null);
        String plan = rs.getString("subscription_plan");
        u.setSubscriptionPlan(plan != null ? plan : "FREE");  // triggers updatePremiumStatus()

        Timestamp createdAt = rs.getTimestamp("created_at");
        u.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        u.setVerified(rs.getBoolean("is_verified"));
        u.setStripeCustomerId(rs.getString("stripe_customer_id"));
        u.setStripeSubscriptionId(rs.getString("stripe_subscription_id"));
        u.setIsBanned(rs.getBoolean("is_banned"));
        u.setBanReason(rs.getString("ban_reason"));

        // Populate LearningStats if the LEFT JOIN produced a row
        long lsId = rs.getLong("ls_id");
        if (!rs.wasNull()) {
            LearningStats ls = new LearningStats();
            ls.setId(lsId);
            ls.setTotalXP(rs.getInt("total_xp"));
            ls.setWordsLearned(rs.getInt("words_learned"));
            ls.setTotalMinutesStudied(rs.getInt("total_minutes_studied"));
            Timestamp lastSession = rs.getTimestamp("last_study_session");
            ls.setLastStudySession(lastSession != null ? lastSession.toLocalDateTime() : null);
            u.setLearningStats(ls);  // also sets ls.user = u via the setter
        }

        return u;
    }

    private void setTs(PreparedStatement ps, int i, LocalDateTime dt) throws SQLException {
        ps.setTimestamp(i, dt != null ? Timestamp.valueOf(dt) : null);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Optional<User> findById(Long id) {
        String sql = SELECT_BASE + " WHERE u.id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = SELECT_BASE + " WHERE u.email = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByEmail failed", e);
        }
    }

    public List<User> findAll() {
        String sql = SELECT_BASE + " ORDER BY u.created_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(mapUser(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
    }

    public List<User> search(String term) {
        String like = "%" + term.toLowerCase() + "%";
        String sql = SELECT_BASE +
            " WHERE LOWER(u.email) LIKE ? OR LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) list.add(mapUser(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("search failed", e);
        }
    }

    public List<User> findByStatus(String status) {
        String sql = SELECT_BASE + " WHERE u.status = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) list.add(mapUser(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByStatus failed", e);
        }
    }

    public List<User> findExpiredSubscriptions() {
        String sql = SELECT_BASE + " WHERE u.is_premium = 1 AND u.subscription_expiry < NOW()";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(mapUser(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("findExpiredSubscriptions failed", e);
        }
    }

    public List<User> findExpiringSubscriptions(LocalDateTime before) {
        String sql = SELECT_BASE + " WHERE u.is_premium = 1 AND u.subscription_expiry < ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            setTs(ps, 1, before);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) list.add(mapUser(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findExpiringSubscriptions failed", e);
        }
    }

    // ── Counts ────────────────────────────────────────────────────────────────

    public long countAll() {
        return countQuery("SELECT COUNT(*) FROM users", null, null);
    }

    public long countByStatus(String status) {
        return countQuery("SELECT COUNT(*) FROM users WHERE status = ?", ps -> ps.setString(1, status), null);
    }

    public long countPremium() {
        return countQuery("SELECT COUNT(*) FROM users WHERE is_premium = 1", null, null);
    }

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private long countQuery(String sql, Binder binder, Object unused) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (binder != null) binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Count query failed: " + sql, e);
        }
    }

    // ── Register (duplicate-safe insert) ─────────────────────────────────────

    /**
     * Inserts a brand-new user account.
     * Throws {@link IllegalArgumentException} if the email is already taken.
     */
    public void register(User user) {
        if (findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        save(user);
    }

    // ── Save (INSERT / UPDATE) ────────────────────────────────────────────────

    public void save(User user) {
        try {
            if (user.getId() == null) insert(user);
            else update(user);
        } catch (SQLException e) {
            throw new RuntimeException("save failed", e);
        }
    }

    /** Atomically inserts a new user together with their initial LearningStats. */
    public void saveWithStats(User user, LearningStats stats) {
        Connection c = conn();
        try {
            c.setAutoCommit(false);
            insert(user);           // sets user.id from generated key
            stats.setUser(user);
            insertLearningStats(stats);
            c.commit();
        } catch (SQLException e) {
            try { c.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("saveWithStats failed", e);
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public void saveLearningStats(LearningStats stats) {
        try {
            if (stats.getId() == null) insertLearningStats(stats);
            else updateLearningStats(stats);
        } catch (SQLException e) {
            throw new RuntimeException("saveLearningStats failed", e);
        }
    }

    private void insert(User u) throws SQLException {
        if (u.getCreatedAt() == null) u.setCreatedAt(LocalDateTime.now());
        String sql =
            "INSERT INTO users (email, password, roles, first_name, last_name, subscription_plan, " +
            "subscription_expiry, is_premium, last_payment_status, status, created_at, is_verified, " +
            "stripe_customer_id, stripe_subscription_id, is_banned, ban_reason) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUser(ps, u);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getLong(1));
            }
        }
    }

    private void update(User u) throws SQLException {
        String sql =
            "UPDATE users SET email=?, password=?, roles=?, first_name=?, last_name=?, " +
            "subscription_plan=?, subscription_expiry=?, is_premium=?, last_payment_status=?, " +
            "status=?, is_verified=?, stripe_customer_id=?, stripe_subscription_id=?, " +
            "is_banned=?, ban_reason=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            // same 15 fields as INSERT (minus created_at which never changes)
            ps.setString(1,  u.getEmail());
            ps.setString(2,  u.getPassword());
            ps.setString(3,  u.getRolesJson());
            ps.setString(4,  u.getFirstName());
            ps.setString(5,  u.getLastName());
            ps.setString(6,  u.getSubscriptionPlan());
            setTs(ps, 7,     u.getSubscriptionExpiry());
            ps.setBoolean(8, u.isPremium());
            ps.setString(9,  u.getLastPaymentStatus());
            ps.setString(10, u.getStatus());
            ps.setBoolean(11, u.isVerified());
            ps.setString(12, u.getStripeCustomerId());
            ps.setString(13, u.getStripeSubscriptionId());
            ps.setBoolean(14, u.isBanned());
            ps.setString(15, u.getBanReason());
            ps.setLong(16,   u.getId());
            ps.executeUpdate();
        }
    }

    /** Binds 16 INSERT columns (including created_at). */
    private void bindUser(PreparedStatement ps, User u) throws SQLException {
        ps.setString(1,  u.getEmail());
        ps.setString(2,  u.getPassword());
        ps.setString(3,  u.getRolesJson());
        ps.setString(4,  u.getFirstName());
        ps.setString(5,  u.getLastName());
        ps.setString(6,  u.getSubscriptionPlan());
        setTs(ps, 7,     u.getSubscriptionExpiry());
        ps.setBoolean(8, u.isPremium());
        ps.setString(9,  u.getLastPaymentStatus());
        ps.setString(10, u.getStatus());
        setTs(ps, 11,    u.getCreatedAt());
        ps.setBoolean(12, u.isVerified());
        ps.setString(13, u.getStripeCustomerId());
        ps.setString(14, u.getStripeSubscriptionId());
        ps.setBoolean(15, u.isBanned());
        ps.setString(16, u.getBanReason());
    }

    private void insertLearningStats(LearningStats ls) throws SQLException {
        String sql =
            "INSERT INTO learning_stats (user_id, total_xp, words_learned, total_minutes_studied, last_study_session) " +
            "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, ls.getUser().getId());
            ps.setInt(2,  ls.getTotalXP());
            ps.setInt(3,  ls.getWordsLearned());
            ps.setInt(4,  ls.getTotalMinutesStudied());
            setTs(ps, 5,  ls.getLastStudySession());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ls.setId(keys.getLong(1));
            }
        }
    }

    private void updateLearningStats(LearningStats ls) throws SQLException {
        String sql =
            "UPDATE learning_stats SET total_xp=?, words_learned=?, total_minutes_studied=?, last_study_session=? " +
            "WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, ls.getTotalXP());
            ps.setInt(2, ls.getWordsLearned());
            ps.setInt(3, ls.getTotalMinutesStudied());
            setTs(ps, 4, ls.getLastStudySession());
            ps.setLong(5, ls.getId());
            ps.executeUpdate();
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(User user) {
        Connection c = conn();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM learning_stats WHERE user_id = ?")) {
                ps.setLong(1, user.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM users WHERE id = ?")) {
                ps.setLong(1, user.getId());
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            try { c.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("delete failed", e);
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    // ── Advanced search with pagination ───────────────────────────────────────

    /** Legacy overload — no role/sort filter. */
    public List<User> findAdvanced(String search, String status, String plan, int page, int pageSize) {
        return findAdvanced(search, status, null, plan, null, page, pageSize);
    }

    public List<User> findAdvanced(String search, String status, String role,
                                   String plan, String sort, int page, int pageSize) {
        StringBuilder sql = new StringBuilder(SELECT_BASE + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        appendFilters(sql, params, search, status, role, plan);
        sql.append(orderByClause(sort));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) list.add(mapUser(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAdvanced failed", e);
        }
    }

    public long countAdvanced(String search, String status, String role, String plan) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users u WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, role, plan);

        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("countAdvanced failed", e);
        }
    }

    private void appendFilters(StringBuilder sql, List<Object> params,
                                String search, String status, String role, String plan) {
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(u.email) LIKE ? OR LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ?)");
            String like = "%" + search.toLowerCase() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND u.status = ?");
            params.add(status);
        }
        if (role != null && !role.isBlank()) {
            sql.append(" AND u.roles LIKE ?");
            params.add("%\"" + role + "\"%");
        }
        if (plan != null && !plan.isBlank()) {
            sql.append(" AND u.subscription_plan = ?");
            params.add(plan);
        }
    }

    private String orderByClause(String sort) {
        return switch (sort != null ? sort : "") {
            case "name"    -> " ORDER BY u.first_name ASC, u.last_name ASC";
            case "email"   -> " ORDER BY u.email ASC";
            case "status"  -> " ORDER BY u.status ASC";
            case "premium" -> " ORDER BY u.is_premium DESC, u.subscription_plan ASC";
            default        -> " ORDER BY u.created_at DESC";
        };
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object v = params.get(i);
            if (v instanceof String s) ps.setString(i + 1, s);
            else if (v instanceof Integer n) ps.setInt(i + 1, n);
            else if (v instanceof Long n) ps.setLong(i + 1, n);
            else ps.setObject(i + 1, v);
        }
    }
}
