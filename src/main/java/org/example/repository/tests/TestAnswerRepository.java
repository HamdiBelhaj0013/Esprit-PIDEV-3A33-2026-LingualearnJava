package org.example.repository.tests;

import org.example.entity.tests.TestAnswer;
import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Repository JDBC pour la table test_answer.
 * Permet de stocker et de relire les réponses individuelles par question,
 * et de calculer les performances réelles par section.
 */
public class TestAnswerRepository {

    private Connection getConn() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── SAVE BATCH ────────────────────────────────────────────────────────────
    /**
     * Sauvegarde une liste de réponses en une seule transaction batch.
     */
    public void saveAll(List<TestAnswer> answers) {
        if (answers == null || answers.isEmpty()) return;
        String sql = """
                INSERT INTO test_answer
                    (test_result_id, question_id, section_category,
                     user_answer, is_correct, points_obtained, points_max, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            for (TestAnswer a : answers) {
                ps.setLong(1, a.getTestResultId());
                ps.setLong(2, a.getQuestionId());
                ps.setString(3, a.getSectionCategory());
                ps.setString(4, a.getUserAnswer());
                ps.setBoolean(5, a.isCorrect());
                ps.setInt(6, a.getPointsObtained());
                ps.setInt(7, a.getPointsMax());
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("saveAll answers échoué : " + e.getMessage(), e);
        }
    }

    // ── FIND BY RESULT ────────────────────────────────────────────────────────
    public List<TestAnswer> findByResultId(Long resultId) {
        String sql = "SELECT * FROM test_answer WHERE test_result_id = ? ORDER BY id ASC";
        List<TestAnswer> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, resultId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByResultId échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── PERFORMANCE PAR SECTION (CORRECTION 1) ────────────────────────────────
    /**
     * Calcule la performance RÉELLE de l'user par section,
     * en se basant sur les réponses individuelles stockées.
     *
     * Pour chaque section → (SUM(points_obtained) / SUM(points_max)) * 100
     *
     * C'est la vraie logique : si l'user a 3/5 pts en Reading et 0/5 en Listening,
     * il aura Reading=60%, Listening=0% — indépendamment du score global du test.
     */
    public Map<String, Double> getScoreParSectionPourUser(Long userId) {
        String sql = """
                SELECT ta.section_category,
                       SUM(ta.points_obtained) AS pts_obtenus,
                       SUM(ta.points_max)      AS pts_max
                FROM test_answer ta
                JOIN test_result tr ON ta.test_result_id = tr.id
                WHERE tr.user_id = ?
                  AND ta.section_category IS NOT NULL
                  AND ta.section_category != ''
                GROUP BY ta.section_category
                ORDER BY ta.section_category ASC
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String section   = rs.getString("section_category");
                    double obtenus   = rs.getDouble("pts_obtenus");
                    double max       = rs.getDouble("pts_max");
                    double score     = (max > 0) ? (obtenus / max * 100.0) : 0.0;
                    result.put(section, score);
                }
            }
        } catch (SQLException e) {
            // Table pas encore créée → retourner vide (dégradé gracieux)
            return new LinkedHashMap<>();
        }
        return result;
    }

    /**
     * Filtré par langue : performance par section pour les tests
     * d'une langue donnée uniquement.
     */
    public Map<String, Double> getScoreParSectionPourUserEtLangue(Long userId, Long langId) {
        String sql = """
                SELECT ta.section_category,
                       SUM(ta.points_obtained) AS pts_obtenus,
                       SUM(ta.points_max)      AS pts_max
                FROM test_answer ta
                JOIN test_result tr ON ta.test_result_id = tr.id
                JOIN mock_test   mt ON tr.mock_test_id   = mt.id
                WHERE tr.user_id = ?
                  AND mt.platform_language_id = ?
                  AND ta.section_category IS NOT NULL
                  AND ta.section_category != ''
                GROUP BY ta.section_category
                ORDER BY ta.section_category ASC
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, langId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String section = rs.getString("section_category");
                    double obtenus = rs.getDouble("pts_obtenus");
                    double max     = rs.getDouble("pts_max");
                    result.put(section, max > 0 ? obtenus / max * 100.0 : 0.0);
                }
            }
        } catch (SQLException e) {
            return new LinkedHashMap<>();
        }
        return result;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public void deleteByResultId(Long resultId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM test_answer WHERE test_result_id = ?")) {
            ps.setLong(1, resultId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteByResultId échoué : " + e.getMessage(), e);
        }
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────
    private TestAnswer map(ResultSet rs) throws SQLException {
        TestAnswer a = new TestAnswer();
        a.setId(rs.getLong("id"));
        a.setTestResultId(rs.getLong("test_result_id"));
        a.setQuestionId(rs.getLong("question_id"));
        a.setSectionCategory(rs.getString("section_category"));
        a.setUserAnswer(rs.getString("user_answer"));
        a.setCorrect(rs.getBoolean("is_correct"));
        a.setPointsObtained(rs.getInt("points_obtained"));
        a.setPointsMax(rs.getInt("points_max"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) a.setCreatedAt(ca.toLocalDateTime());
        return a;
    }
}