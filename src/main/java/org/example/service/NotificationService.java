package org.example.service;

import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    public NotificationService() {}

    /** Backward-compatible constructor — EntityManager is no longer used. */
    public NotificationService(Object ignoredEntityManager) {}

    private Connection conn() {
        return MyDataBase.getInstance().getConnection();
    }

    /** Insert a new notification for a user. */
    public void sendNotification(Long userId, String type, String message) {
        String sql = "INSERT INTO notifications (user_id, type, message, is_read, created_at) " +
                     "VALUES (?, ?, ?, 0, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1,      userId);
            ps.setString(2,    type);
            ps.setString(3,    message);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not send notification: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the 5 most recent notifications for a user as plain DTOs.
     * Each NotifRow has: id, type, message, isRead, createdAt.
     */
    public List<NotifRow> getRecentForUser(Long userId) {
        String sql = "SELECT id, type, message, is_read, created_at " +
                     "FROM notifications WHERE user_id = ? " +
                     "ORDER BY created_at DESC LIMIT 5";
        List<NotifRow> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotifRow n = new NotifRow();
                    n.id        = rs.getLong("id");
                    n.type      = rs.getString("type");
                    n.message   = rs.getString("message");
                    n.isRead    = rs.getInt("is_read") == 1;
                    Timestamp ts = rs.getTimestamp("created_at");
                    n.createdAt = ts != null ? ts.toString() : "";
                    result.add(n);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not fetch notifications: " + e.getMessage(), e);
        }
        return result;
    }

    public static class NotifRow {
        public long    id;
        public String  type;
        public String  message;
        public boolean isRead;
        public String  createdAt;
    }
}
