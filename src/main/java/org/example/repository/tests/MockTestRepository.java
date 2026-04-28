package org.example.repository.tests;

import org.example.entity.tests.MockTest;
import org.example.entity.tests.PlatformLanguage;
import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MockTestRepository {

    private Connection getConn() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── SELECT ALL ────────────────────────────────────────────────────────────

    public List<MockTest> findAll() {
        String sql = """
                SELECT m.*, p.id as lang_id, p.name as lang_name,
                       p.code as lang_code, p.flag_url, p.is_enabled
                FROM mock_test m
                LEFT JOIN platform_language p ON m.platform_language_id = p.id
                ORDER BY m.test_type ASC, m.level ASC
                """;
        List<MockTest> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── SELECT BY ID ──────────────────────────────────────────────────────────

    public Optional<MockTest> findById(Long id) {
        String sql = """
                SELECT m.*, p.id as lang_id, p.name as lang_name,
                       p.code as lang_code, p.flag_url, p.is_enabled
                FROM mock_test m
                LEFT JOIN platform_language p ON m.platform_language_id = p.id
                WHERE m.id = ?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById échoué : " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    public List<MockTest> search(String term) {
        String like = "%" + term.toLowerCase() + "%";
        String sql = """
                SELECT m.*, p.id as lang_id, p.name as lang_name,
                       p.code as lang_code, p.flag_url, p.is_enabled
                FROM mock_test m
                LEFT JOIN platform_language p ON m.platform_language_id = p.id
                WHERE LOWER(m.title) LIKE ? OR LOWER(m.test_type) LIKE ?
                ORDER BY m.test_type ASC
                """;
        List<MockTest> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("search échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── FILTER ADVANCED ───────────────────────────────────────────────────────

    public List<MockTest> filterAdvanced(String testType, String level, Long languageId) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.*, p.id as lang_id, p.name as lang_name,
                       p.code as lang_code, p.flag_url, p.is_enabled
                FROM mock_test m
                LEFT JOIN platform_language p ON m.platform_language_id = p.id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (testType   != null) { sql.append(" AND m.test_type = ?");             params.add(testType); }
        if (level      != null) { sql.append(" AND m.level = ?");                 params.add(level); }
        if (languageId != null) { sql.append(" AND m.platform_language_id = ?"); params.add(languageId); }

        List<MockTest> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("filterAdvanced échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── SORT ──────────────────────────────────────────────────────────────────

    public List<MockTest> sortByTitleAsc()    { return queryAll("ORDER BY m.title ASC"); }
    public List<MockTest> sortByDurationAsc() { return queryAll("ORDER BY m.duration_minutes ASC"); }
    public List<MockTest> sortByDurationDesc(){ return queryAll("ORDER BY m.duration_minutes DESC"); }
    public List<MockTest> sortByDateDesc()    { return queryAll("ORDER BY m.created_at DESC"); }

    private List<MockTest> queryAll(String orderBy) {
        String sql = """
                SELECT m.*, p.id as lang_id, p.name as lang_name,
                       p.code as lang_code, p.flag_url, p.is_enabled
                FROM mock_test m
                LEFT JOIN platform_language p ON m.platform_language_id = p.id
                """ + orderBy;
        List<MockTest> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("queryAll échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── LANGUAGES ─────────────────────────────────────────────────────────────

    public List<PlatformLanguage> findAllLanguages() {
        String sql = "SELECT * FROM platform_language WHERE is_enabled = 1 ORDER BY name ASC";
        List<PlatformLanguage> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlatformLanguage lang = new PlatformLanguage();
                lang.setId(rs.getLong("id"));
                lang.setName(rs.getString("name"));
                lang.setCode(rs.getString("code"));
                lang.setFlagUrl(rs.getString("flag_url"));
                lang.setEnabled(rs.getBoolean("is_enabled"));
                list.add(lang);
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAllLanguages échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    public long countAll() {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT COUNT(*) FROM mock_test");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    public double averageDuration() {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT AVG(duration_minutes) FROM mock_test");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) { return 0.0; }
    }

    // ── FIX #1 : countByTestType — GROUP BY test_type ─────────────────────────
    /**
     * Retourne le nombre de tests par type.
     * Exemple : { "TOEFL" -> 3, "IELTS" -> 2, "DELF" -> 5 }
     */
    public Map<String, Long> countByTestType() {
        String sql = "SELECT test_type, COUNT(*) as cnt FROM mock_test GROUP BY test_type ORDER BY test_type ASC";
        Map<String, Long> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("test_type"), rs.getLong("cnt"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("countByTestType échoué : " + e.getMessage(), e);
        }
        return result;
    }

    // ── FIX #1 : countByLevel — GROUP BY level ────────────────────────────────
    /**
     * Retourne le nombre de tests par niveau.
     * Exemple : { "A1" -> 4, "A2" -> 2, "B1" -> 3 }
     */
    public Map<String, Long> countByLevel() {
        String sql = "SELECT level, COUNT(*) as cnt FROM mock_test GROUP BY level ORDER BY level ASC";
        Map<String, Long> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("level"), rs.getLong("cnt"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("countByLevel échoué : " + e.getMessage(), e);
        }
        return result;
    }

    // ── SAVE ─────────────────────────────────────────────────────────────────

    public void save(MockTest m) {
        if (m.getId() == null) insert(m);
        else                   update(m);
    }

    private void insert(MockTest m) {
        String sql = """
                INSERT INTO mock_test
                  (title, test_type, level, duration_minutes, platform_language_id, is_active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getTestType());
            ps.setString(3, m.getLevel());
            ps.setInt(4, m.getDurationMinutes());
            if (m.getPlatformLanguage() != null) ps.setLong(5, m.getPlatformLanguage().getId());
            else ps.setNull(5, Types.BIGINT);
            ps.setBoolean(6, m.isActive());
            ps.setTimestamp(7, Timestamp.valueOf(
                    m.getCreatedAt() != null ? m.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert MockTest échoué : " + e.getMessage(), e);
        }
    }

    private void update(MockTest m) {
        String sql = """
                UPDATE mock_test
                SET title = ?, test_type = ?, level = ?, duration_minutes = ?,
                    platform_language_id = ?, is_active = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getTestType());
            ps.setString(3, m.getLevel());
            ps.setInt(4, m.getDurationMinutes());
            if (m.getPlatformLanguage() != null) ps.setLong(5, m.getPlatformLanguage().getId());
            else ps.setNull(5, Types.BIGINT);
            ps.setBoolean(6, m.isActive());
            ps.setLong(7, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update MockTest échoué : " + e.getMessage(), e);
        }
    }

    public void delete(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM mock_test WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete MockTest échoué : " + e.getMessage(), e);
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    private MockTest map(ResultSet rs) throws SQLException {
        MockTest m = new MockTest();
        m.setId(rs.getLong("id"));
        m.setTitle(rs.getString("title"));
        m.setTestType(rs.getString("test_type"));
        m.setLevel(rs.getString("level"));
        m.setDurationMinutes(rs.getInt("duration_minutes"));
        m.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) m.setCreatedAt(createdAt.toLocalDateTime());

        // Langue associée
        long langId = rs.getLong("lang_id");
        if (!rs.wasNull()) {
            PlatformLanguage lang = new PlatformLanguage();
            lang.setId(langId);
            lang.setName(rs.getString("lang_name"));
            lang.setCode(rs.getString("lang_code"));
            lang.setFlagUrl(rs.getString("flag_url"));
            lang.setEnabled(rs.getBoolean("is_enabled"));
            m.setPlatformLanguage(lang);
        }
        return m;
    }
}