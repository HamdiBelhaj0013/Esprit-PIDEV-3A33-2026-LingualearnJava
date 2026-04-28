package org.example.repository.tests;

import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestResult;
import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestResultRepository {

    private Connection getConn() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────

    public List<TestResult> findAll() {
        String sql = """
                SELECT r.*,
                       u.id as u_id, u.first_name, u.last_name, u.email,
                       m.id as m_id, m.title, m.test_type
                FROM test_result r
                LEFT JOIN users u ON r.user_id = u.id
                LEFT JOIN mock_test m ON r.mock_test_id = m.id
                ORDER BY r.date_taken DESC
                """;
        List<TestResult> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll résultats échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── FIND BY USER ──────────────────────────────────────────────────────────

    public List<TestResult> findByUserId(Long userId) {
        String sql = """
                SELECT r.*,
                       u.id as u_id, u.first_name, u.last_name, u.email,
                       m.id as m_id, m.title, m.test_type
                FROM test_result r
                LEFT JOIN users u ON r.user_id = u.id
                LEFT JOIN mock_test m ON r.mock_test_id = m.id
                WHERE r.user_id = ?
                ORDER BY r.date_taken DESC
                """;
        List<TestResult> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUserId échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── FIND BY TEST ──────────────────────────────────────────────────────────

    public List<TestResult> findByMockTestId(Long mockTestId) {
        String sql = """
                SELECT r.*,
                       u.id as u_id, u.first_name, u.last_name, u.email,
                       m.id as m_id, m.title, m.test_type
                FROM test_result r
                LEFT JOIN users u ON r.user_id = u.id
                LEFT JOIN mock_test m ON r.mock_test_id = m.id
                WHERE r.mock_test_id = ?
                ORDER BY r.date_taken DESC
                """;
        List<TestResult> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, mockTestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByMockTestId échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────────

    public Optional<TestResult> findById(Long id) {
        String sql = """
                SELECT r.*,
                       u.id as u_id, u.first_name, u.last_name, u.email,
                       m.id as m_id, m.title, m.test_type
                FROM test_result r
                LEFT JOIN users u ON r.user_id = u.id
                LEFT JOIN mock_test m ON r.mock_test_id = m.id
                WHERE r.id = ?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById résultat échoué : " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    public long countAll() {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT COUNT(*) FROM test_result");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    public double averageScore() {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT AVG(overall_score) FROM test_result");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) { return 0.0; }
    }

    public double averageScoreByMockTestId(Long mockTestId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT AVG(overall_score) FROM test_result WHERE mock_test_id = ?")) {
            ps.setLong(1, mockTestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) { return 0.0; }
    }

    public double averageScoreByUserId(Long userId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT AVG(overall_score) FROM test_result WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) { return 0.0; }
    }

    // ── SAVE ──────────────────────────────────────────────────────────────────

    public void save(TestResult r) {
        if (r.getId() == null) insert(r);
        else update(r);
    }

    private void insert(TestResult r) {
        String sql = """
                INSERT INTO test_result
                    (user_id, mock_test_id, overall_score, ai_predicted_score,
                     ai_weakness_report, ai_correction, ai_note, date_taken, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setLong(1, r.getUser().getId());
            ps.setLong(2, r.getMockTest().getId());
            ps.setFloat(3, r.getOverallScore());
            ps.setFloat(4, r.getAiPredictedScore());
            ps.setString(5, r.getAiWeaknessReport());
            ps.setString(6, r.getAiCorrection());
            ps.setString(7, r.getAiNote());
            ps.setTimestamp(8, Timestamp.valueOf(r.getDateTaken() != null ? r.getDateTaken() : now));
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert résultat échoué : " + e.getMessage(), e);
        }
    }

    private void update(TestResult r) {
        String sql = """
                UPDATE test_result
                SET overall_score=?, ai_predicted_score=?, ai_weakness_report=?,
                    ai_correction=?, ai_note=?, updated_at=?
                WHERE id=?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setFloat(1, r.getOverallScore());
            ps.setFloat(2, r.getAiPredictedScore());
            ps.setString(3, r.getAiWeaknessReport());
            ps.setString(4, r.getAiCorrection());
            ps.setString(5, r.getAiNote());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(7, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update résultat échoué : " + e.getMessage(), e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM test_result WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete résultat échoué : " + e.getMessage(), e);
        }
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────

    private TestResult map(ResultSet rs) throws SQLException {
        TestResult r = new TestResult();
        r.setId(rs.getLong("id"));
        r.setOverallScore(rs.getFloat("overall_score"));
        r.setAiPredictedScore(rs.getFloat("ai_predicted_score"));
        r.setAiWeaknessReport(rs.getString("ai_weakness_report"));
        r.setAiCorrection(rs.getString("ai_correction"));
        r.setAiNote(rs.getString("ai_note"));
        Timestamp dt = rs.getTimestamp("date_taken");
        if (dt != null) r.setDateTaken(dt.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) r.setUpdatedAt(ua.toLocalDateTime());

        // User (minimal)
        long uId = rs.getLong("u_id");
        if (!rs.wasNull()) {
            User u = new User();
            u.setId(uId);
            u.setFirstName(rs.getString("first_name"));
            u.setLastName(rs.getString("last_name"));
            u.setEmail(rs.getString("email"));
            r.setUser(u);
        }

        // MockTest (minimal)
        long mId = rs.getLong("m_id");
        if (!rs.wasNull()) {
            MockTest m = new MockTest();
            m.setId(mId);
            m.setTitle(rs.getString("title"));
            m.setTestType(rs.getString("test_type"));
            r.setMockTest(m);
        }
        return r;
    }

    // ── VERROU PROGRESSION ────────────────────────────────────────────────────

    /**
     * Vérifie si l'user a au moins un résultat >= minScore
     * pour un test de niveau donné (ex: 'A1') dans la langue donnée.
     */
    /**
     * Vérifie si l user a >= minScore dans au moins 1 test
     * du niveau ET de la langue donnés.
     */
    public boolean hasPassedLevel(Long userId, String level, float minScore, Long languageId) {
        String sql = "SELECT COUNT(*) FROM test_result r " +
                "JOIN mock_test m ON r.mock_test_id = m.id " +
                "WHERE r.user_id = ? AND m.level = ? " +
                "AND r.overall_score >= ? AND m.platform_language_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, level);
            ps.setFloat(3, minScore);
            ps.setLong(4, languageId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retourne le meilleur score de l'user dans un groupe de niveaux.
     */
    /**
     * Retourne le meilleur score de l user dans un groupe de niveaux
     * pour une langue donnée.
     */
    public float bestScoreInLevels(Long userId, String[] levels, Long languageId) {
        if (levels == null || levels.length == 0) return 0f;
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < levels.length; i++) {
            inClause.append("'").append(levels[i]).append("'");
            if (i < levels.length - 1) inClause.append(",");
        }
        String sql = "SELECT COALESCE(MAX(r.overall_score), 0) FROM test_result r " +
                "JOIN mock_test m ON r.mock_test_id = m.id " +
                "WHERE r.user_id = ? AND m.level IN (" + inClause + ") " +
                "AND m.platform_language_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, languageId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getFloat(1) : 0f;
            }
        } catch (SQLException e) {
            return 0f;
        }
    }
}