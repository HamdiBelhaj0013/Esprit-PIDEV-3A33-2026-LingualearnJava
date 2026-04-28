package org.example.repository.tests;

import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestQuestion;
import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestQuestionRepository {

    private Connection getConn() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── FIND BY TEST ──────────────────────────────────────────────────────────

    public List<TestQuestion> findByMockTestId(Long mockTestId) {
        String sql = "SELECT * FROM test_question WHERE mock_test_id = ? AND is_active = 1 ORDER BY id ASC";
        List<TestQuestion> list = new ArrayList<>();
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

    public Optional<TestQuestion> findById(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT * FROM test_question WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById échoué : " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────

    public List<TestQuestion> findAll() {
        List<TestQuestion> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT * FROM test_question ORDER BY mock_test_id ASC, id ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    public List<TestQuestion> search(Long mockTestId, String keyword) {
        String like = "%" + keyword.toLowerCase() + "%";
        String sql = """
                SELECT * FROM test_question
                WHERE mock_test_id = ? AND (LOWER(question_text) LIKE ? OR LOWER(section_category) LIKE ?)
                ORDER BY id ASC
                """;
        List<TestQuestion> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, mockTestId);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("search échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── FILTER BY SECTION ─────────────────────────────────────────────────────

    public List<TestQuestion> findBySection(Long mockTestId, String section) {
        String sql = "SELECT * FROM test_question WHERE mock_test_id = ? AND section_category = ? ORDER BY id ASC";
        List<TestQuestion> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, mockTestId);
            ps.setString(2, section);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findBySection échoué : " + e.getMessage(), e);
        }
        return list;
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    public long countAll() {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT COUNT(*) FROM test_question");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    public long countByMockTestId(Long mockTestId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(*) FROM test_question WHERE mock_test_id = ?")) {
            ps.setLong(1, mockTestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) { return 0; }
    }

    public int sumPointsByMockTestId(Long mockTestId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COALESCE(SUM(points), 0) FROM test_question WHERE mock_test_id = ?")) {
            ps.setLong(1, mockTestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) { return 0; }
    }

    // ── SAVE ─────────────────────────────────────────────────────────────────

    public void save(TestQuestion q) {
        if (q.getId() == null) insert(q);
        else update(q);
    }

    private void insert(TestQuestion q) {
        String sql = """
                INSERT INTO test_question
                    (mock_test_id, section_category, question_type, question_text,
                     reading_passage, audio_text, writing_subject, options,
                     correct_answer, points, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setLong(1, q.getMockTest().getId());
            ps.setString(2, q.getSectionCategory());
            ps.setString(3, q.getQuestionType());
            ps.setString(4, q.getQuestionText());
            ps.setString(5, q.getReadingPassage());
            ps.setString(6, q.getAudioText());
            ps.setString(7, q.getWritingSubject());
            ps.setString(8, q.getOptions());
            ps.setString(9, q.getCorrectAnswer());
            ps.setInt(10, q.getPoints());
            ps.setBoolean(11, true);
            ps.setTimestamp(12, Timestamp.valueOf(now));
            ps.setTimestamp(13, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) q.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert question échoué : " + e.getMessage(), e);
        }
    }

    private void update(TestQuestion q) {
        String sql = """
                UPDATE test_question
                SET section_category=?, question_type=?, question_text=?,
                    reading_passage=?, audio_text=?, writing_subject=?,
                    options=?, correct_answer=?, points=?, updated_at=?
                WHERE id=?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, q.getSectionCategory());
            ps.setString(2, q.getQuestionType());
            ps.setString(3, q.getQuestionText());
            ps.setString(4, q.getReadingPassage());
            ps.setString(5, q.getAudioText());
            ps.setString(6, q.getWritingSubject());
            ps.setString(7, q.getOptions());
            ps.setString(8, q.getCorrectAnswer());
            ps.setInt(9, q.getPoints());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(11, q.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update question échoué : " + e.getMessage(), e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM test_question WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete question échoué : " + e.getMessage(), e);
        }
    }

    public void deleteAllByMockTestId(Long mockTestId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM test_question WHERE mock_test_id = ?")) {
            ps.setLong(1, mockTestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteAllByMockTestId échoué : " + e.getMessage(), e);
        }
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────

    private TestQuestion map(ResultSet rs) throws SQLException {
        TestQuestion q = new TestQuestion();
        q.setId(rs.getLong("id"));
        q.setSectionCategory(rs.getString("section_category"));
        q.setQuestionType(rs.getString("question_type"));
        q.setQuestionText(rs.getString("question_text"));
        q.setReadingPassage(rs.getString("reading_passage"));
        q.setAudioText(rs.getString("audio_text"));
        q.setWritingSubject(rs.getString("writing_subject"));
        q.setOptions(rs.getString("options"));
        q.setCorrectAnswer(rs.getString("correct_answer"));
        q.setPoints(rs.getInt("points"));
        q.setActive(rs.getBoolean("is_active"));
        q.setEmbedding(rs.getString("embedding"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) q.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) q.setUpdatedAt(ua.toLocalDateTime());

        // Lien mockTest (juste l'id, pas de JOIN ici)
        long testId = rs.getLong("mock_test_id");
        if (!rs.wasNull()) {
            MockTest mt = new MockTest();
            mt.setId(testId);
            q.setMockTest(mt);
        }
        return q;
    }
}