package dao;

import models.Reclamation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReclamationDAO {

    private Connection conn = DatabaseConnection.getConnection();

    private Reclamation map(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(rs.getInt("id"));
        r.setSubject(rs.getString("subject"));
        r.setMessageBody(rs.getString("message_body"));
        r.setStatus(rs.getString("status"));
        r.setUserId(rs.getInt("user_id"));
        r.setPriority(rs.getString("priority"));
        Timestamp t = rs.getTimestamp("submitted_at");
        if (t != null) r.setSubmittedAt(t.toLocalDateTime());
        return r;
    }

    public boolean ajouter(Reclamation r) {
        String sql = "INSERT INTO reclamation (subject, message_body, status, submitted_at, user_id, priority) VALUES (?, ?, 'PENDING', ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getSubject());
            ps.setString(2, r.getMessageBody());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, r.getUserId());
            ps.setString(5, r.getPriority());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur ajouter reclamation: " + e.getMessage());
            return false;
        }
    }

    public List<Reclamation> getByUserId(int userId) {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE user_id=? ORDER BY submitted_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getByUserId: " + e.getMessage());
        }
        return list;
    }

    public List<Reclamation> getAll() {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reclamation ORDER BY submitted_at DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getAll: " + e.getMessage());
        }
        return list;
    }

    public boolean modifier(Reclamation r) {
        String sql = "UPDATE reclamation SET subject=?, message_body=?, priority=? WHERE id=? AND status='PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getSubject());
            ps.setString(2, r.getMessageBody());
            ps.setString(3, r.getPriority());
            ps.setInt(4, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur modifier: " + e.getMessage());
            return false;
        }
    }

    public boolean changerStatut(int id, String statut) {
        String sql = "UPDATE reclamation SET status=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur changerStatut: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM reclamation WHERE id=? AND status='PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur supprimer: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimerAdmin(int id) {
        String sql = "DELETE FROM reclamation WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur supprimerAdmin: " + e.getMessage());
            return false;
        }
    }
}
