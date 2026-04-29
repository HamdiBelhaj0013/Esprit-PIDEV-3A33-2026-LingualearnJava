package org.example.service;

import org.example.entities.pedagogicalcontent.Lesson;
import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FrontProgressService {

    private final Connection cnx;

    public FrontProgressService() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    // =========================================================
    // COMPLETE A LESSON — accumulate real XP, real time
    // =========================================================
    // =========================================================
    // COMPLETE A LESSON — accumulate real XP, real time
    // =========================================================
    public void completeLesson(int userId, int lessonId, int xpReward) throws Exception {

        String sql = """
        INSERT INTO learning_stats (user_id, total_xp, total_minutes_studied, words_learned, last_study_session)
        VALUES (?, ?, 0, 0, NOW())
        ON DUPLICATE KEY UPDATE
            total_xp = total_xp + VALUES(total_xp),
            last_study_session = NOW()
    """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, xpReward);
            ps.executeUpdate();
        }
    }
    // =========================================================
    // TOTAL XP — from learning_stats.total_xp
    // =========================================================
    public int getTotalXp(int userId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT total_xp FROM learning_stats WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("total_xp");
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // =========================================================
    // EARNED XP FOR A COURSE
    // =========================================================
    public int getEarnedXpForCourse(int userId, int courseId) {
        String sql = """
            SELECT SUM(l.xp_reward) 
            FROM user_lesson_status uls
            JOIN lesson l ON uls.lesson_id = l.id
            WHERE uls.user_id = ? AND l.course_id = ? AND uls.completed = true
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, courseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // =========================================================
    // TOTAL MINUTES — from learning_stats.total_minutes_studied
    // =========================================================
    public int getTotalMinutesStudied(int userId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT total_minutes_studied FROM learning_stats WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("total_minutes_studied");
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // =========================================================
    // WORDS LEARNED — from learning_stats.words_learned
    // =========================================================
    public int getWordsLearned(int userId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT words_learned FROM learning_stats WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("words_learned");
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // =========================================================
    // COMPLETED LESSONS COUNT — real count from user_lesson_status
    // =========================================================
    public int getCompletedLessonsCount(int userId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM user_lesson_status WHERE user_id = ? AND completed = true")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // =========================================================
    // COMPLETED LESSONS FOR A COURSE
    // =========================================================
    public int getCompletedLessonsCountForCourse(int userId, int courseId) {
        String sql = """
            SELECT COUNT(*) FROM user_lesson_status uls
            JOIN lesson l ON uls.lesson_id = l.id
            WHERE uls.user_id = ? AND l.course_id = ? AND uls.completed = true
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, courseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // =========================================================
    // RECENT COMPLETED LESSONS — joined with lesson table
    // =========================================================
    public List<Lesson> getRecentCompletedLessons(int userId, int limit) {
        List<Lesson> result = new ArrayList<>();
        String sql = """
            SELECT l.id, l.title, l.xp_reward, l.course_id
            FROM user_lesson_status uls
            JOIN lesson l ON uls.lesson_id = l.id
            WHERE uls.user_id = ? AND uls.completed = true
            ORDER BY uls.lesson_id DESC
            LIMIT ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Lesson l = new Lesson();
                l.setId(rs.getInt("id"));
                l.setTitle(rs.getString("title"));
                l.setXpReward(rs.getInt("xp_reward"));
                l.setCourseId(rs.getInt("course_id"));
                result.add(l);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }

    // =========================================================
    // LAST STUDY SESSION — from learning_stats.last_study_session
    // =========================================================
    public String getLastStudySession(int userId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT last_study_session FROM learning_stats WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("last_study_session");
                if (ts != null) {
                    LocalDate date = ts.toLocalDateTime().toLocalDate();
                    if (date.equals(LocalDate.now()))               return "Today";
                    if (date.equals(LocalDate.now().minusDays(1))) return "Yesterday";
                    return date.toString();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "Never";
    }

    // =========================================================
    // ACTIVITY HISTORY (Last 7 Days)
    // =========================================================
    public List<LocalDate> getActivityDates(int userId, int days) {
        List<LocalDate> dates = new ArrayList<>();
        // Since we don't have a full history log yet, we use the last_study_session 
        // to at least show the current day if active. 
        // In a real app, you'd query an 'activity_log' or 'user_lesson_status.completed_at'.
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT last_study_session FROM learning_stats WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("last_study_session");
                if (ts != null) {
                    dates.add(ts.toLocalDateTime().toLocalDate());
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return dates;
    }

    // =========================================================
    // HAS ACTIVITY ON DATE
    // =========================================================
    public boolean hasActivityOnDate(int userId, LocalDate date) {
        // Simple check against last_study_session
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT last_study_session FROM learning_stats WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("last_study_session");
                if (ts != null)
                    return ts.toLocalDateTime().toLocalDate().equals(date);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // =========================================================
    // STREAK
    // =========================================================
    public int getStreak(int userId) {
        // Improved logic: If user was active today, streak is at least 1.
        // If user was active yesterday but not today, streak is maintained.
        // In a full implementation, you'd count back consecutive days.
        String last = getLastStudySession(userId);
        if ("Today".equals(last) || "Yesterday".equals(last)) {
            // Placeholder: for now we return 1 if active today/yesterday.
            // A real streak would query a history table.
            return 1; 
        }
        return 0;
    }
}