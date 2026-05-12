package org.example.service.forum;

import org.example.entities.forum.Notification;
import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DB-backed persistence for forum notifications.
 * Creates the table on first use if it does not already exist.
 */
public class ServiceNotification {

    private Connection cnx;

    public ServiceNotification() {
        cnx = MyDataBase.getInstance().getConnection();
        createTableIfNotExists();
    }

    // ── DDL ──────────────────────────────────────────────────────────────────

    private void createTableIfNotExists() {
        String sql =
            "CREATE TABLE IF NOT EXISTS notifications (" +
            "  id             INT AUTO_INCREMENT PRIMARY KEY," +
            "  message        VARCHAR(500) NOT NULL," +
            "  type           VARCHAR(20)  NOT NULL," +
            "  publication_id INT          NOT NULL," +
            "  recipient_id   INT          NOT NULL," +
            "  date           DATETIME     NOT NULL," +
            "  lue            TINYINT(1)   DEFAULT 0" +
            ")";
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("[ServiceNotification] DDL failed: " + e.getMessage());
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public void ajouterNotification(String message, String type,
                                    int publicationId, int recipientId) {
        String sql = "INSERT INTO notifications " +
                     "(message, type, publication_id, recipient_id, date, lue) " +
                     "VALUES (?, ?, ?, ?, ?, 0)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setString(2, type);
            ps.setInt(3, publicationId);
            ps.setInt(4, recipientId);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ServiceNotification] insert failed: " + e.getMessage());
        }
    }

    public void marquerLuesPourUser(int userId) {
        String sql = "UPDATE notifications SET lue = 1 WHERE recipient_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ServiceNotification] marquerLues failed: " + e.getMessage());
        }
    }

    public void marquerToutesLues() {
        String sql = "UPDATE notifications SET lue = 1";
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("[ServiceNotification] marquerToutesLues failed: " + e.getMessage());
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All notifications for a given user, newest first. */
    public List<Notification> getForUser(int userId) {
        String sql = "SELECT * FROM notifications WHERE recipient_id = ? ORDER BY date DESC";
        return query(sql, userId);
    }

    /** Unread notifications for a given user, newest first. */
    public List<Notification> getNonLuesPourUser(int userId) {
        String sql = "SELECT * FROM notifications WHERE recipient_id = ? AND lue = 0 ORDER BY date DESC";
        return query(sql, userId);
    }

    /** Count of unread notifications for a given user. */
    public int getNombreNonLuesPourUser(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND lue = 0";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[ServiceNotification] count failed: " + e.getMessage());
        }
        return 0;
    }

    /** All notifications in the table (legacy / admin use). */
    public List<Notification> getAll() {
        String sql = "SELECT * FROM notifications ORDER BY date DESC";
        List<Notification> list = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[ServiceNotification] getAll failed: " + e.getMessage());
        }
        return list;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Notification> query(String sql, int userId) {
        List<Notification> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[ServiceNotification] query failed: " + e.getMessage());
        }
        return list;
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        return new Notification(
            rs.getInt("id"),
            rs.getString("message"),
            rs.getString("type"),
            rs.getInt("publication_id"),
            rs.getInt("recipient_id"),
            rs.getTimestamp("date").toLocalDateTime(),
            rs.getBoolean("lue")
        );
    }
}
