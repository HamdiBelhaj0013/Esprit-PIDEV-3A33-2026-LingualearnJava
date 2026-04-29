package org.example.service;

import org.example.util.MyDataBase;

import java.sql.*;
import java.util.*;

public class BackofficeStatsService {

    private final Connection cnx;

    public BackofficeStatsService() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    // ─── BASIC KPIs ───────────────────────────────────────────────────────────

    public int getTotalUsers() {
        String sql = "SELECT COUNT(DISTINCT user_id) FROM learning_stats";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTotalCourses() {
        String sql = "SELECT COUNT(*) FROM course";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTotalLessons() {
        String sql = "SELECT COUNT(*) FROM lesson";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTotalPlatformXp() {
        String sql = "SELECT IFNULL(SUM(total_xp), 0) FROM learning_stats";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ─── NEW KPIs ─────────────────────────────────────────────────────────────

    /** Total quiz attempts on the platform */
    public int getTotalQuizAttempts() {
        String sql = "SELECT COUNT(*) FROM quiz_attempt";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /** Average score across all quiz attempts (0–100 rounded) */
    public int getAverageQuizScore() {
        String sql = "SELECT IFNULL(AVG(score), 0) FROM quiz_attempt";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return (int) Math.round(rs.getDouble(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /** Count of premium vs free users: {"Premium": n, "Free": n} */
    public Map<String, Integer> getPremiumVsFreeCount() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT is_premium, COUNT(*) as count FROM users GROUP BY is_premium";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getInt("is_premium") == 1 ? "Premium 👑" : "Free 🎓";
                result.put(key, rs.getInt("count"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        if (result.isEmpty()) { result.put("Free 🎓", 0); result.put("Premium 👑", 0); }
        return result;
    }

    /** Top 5 students ranked by XP */
    public List<StudentStat> getTopStudentsByXp() {
        List<StudentStat> list = new ArrayList<>();
        String sql = """
            SELECT u.first_name, u.last_name, ls.total_xp
            FROM learning_stats ls
            JOIN users u ON ls.user_id = u.id
            ORDER BY ls.total_xp DESC
            LIMIT 5
            """;
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                list.add(new StudentStat(name, rs.getInt("total_xp")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ─── CHARTS ───────────────────────────────────────────────────────────────

    public Map<String, Integer> getCoursesPerLanguage() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = """
            SELECT pl.name, COUNT(c.id) as count
            FROM platform_language pl
            LEFT JOIN course c ON pl.id = c.platform_language_id
            GROUP BY pl.name
            ORDER BY count DESC
            """;
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("name"), rs.getInt("count"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    public Map<String, Integer> getLessonsPerCourse() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = """
            SELECT c.title, COUNT(l.id) as count
            FROM course c
            LEFT JOIN lesson l ON c.id = l.course_id
            GROUP BY c.title
            ORDER BY count DESC
            LIMIT 10
            """;
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("title"), rs.getInt("count"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    public Map<String, Integer> getActivityTrend() {
        Map<String, Integer> trend = new LinkedHashMap<>();
        String sql = """
            SELECT DATE(created_at) as day, COUNT(*) as count
            FROM quiz_attempt
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY day
            ORDER BY day ASC
            """;
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                trend.put(rs.getString("day"), rs.getInt("count"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return trend;
    }

    // ─── RECORDS ──────────────────────────────────────────────────────────────

    public record StudentStat(String name, int xp) {}
    public record LanguageDetail(String name, int courseCount, int lessonCount, int totalXp) {}

    public List<LanguageDetail> getLanguageBreakdown() {
        List<LanguageDetail> list = new ArrayList<>();
        String sql = """
            SELECT pl.name,
                   COUNT(DISTINCT c.id)  as course_count,
                   COUNT(DISTINCT l.id)  as lesson_count,
                   IFNULL(SUM(l.xp_reward), 0) as total_xp
            FROM platform_language pl
            LEFT JOIN course c ON pl.id = c.platform_language_id
            LEFT JOIN lesson l ON c.id  = l.course_id
            GROUP BY pl.id, pl.name
            """;
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new LanguageDetail(rs.getString("name"),
                        rs.getInt("course_count"),
                        rs.getInt("lesson_count"),
                        rs.getInt("total_xp")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
