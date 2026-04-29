package org.example.repository.supportmanagement;

import org.example.entity.Reclamation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReclamationDAO {

    // ✅ PLUS de champ "conn" partagé — chaque méthode ouvre sa propre connexion

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
        Timestamp sla = rs.getTimestamp("sla_deadline");
        if (sla != null) r.setSlaDeadline(sla.toLocalDateTime());
        return r;
    }

    public boolean ajouter(Reclamation r) {
        String sql = "INSERT INTO reclamation " +
                "(subject, message_body, status, submitted_at, user_id, priority, sla_deadline) " +
                "VALUES (?, ?, 'PENDING', ?, ?, ?, ?)";
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getSubject());
            ps.setString(2, r.getMessageBody());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, r.getUserId());
            ps.setString(5, r.getPriority());
            if (r.getSlaDeadline() != null)
                ps.setTimestamp(6, Timestamp.valueOf(r.getSlaDeadline()));
            else
                ps.setNull(6, Types.TIMESTAMP);
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
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getAll: " + e.getMessage());
        }
        return list;
    }

    public boolean modifier(Reclamation r) {
        String sql = "UPDATE reclamation SET subject=?, message_body=?, priority=?, sla_deadline=? " +
                "WHERE id=? AND status='PENDING'";
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getSubject());
            ps.setString(2, r.getMessageBody());
            ps.setString(3, r.getPriority());
            if (r.getSlaDeadline() != null)
                ps.setTimestamp(4, Timestamp.valueOf(r.getSlaDeadline()));
            else
                ps.setNull(4, Types.TIMESTAMP);
            ps.setInt(5, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur modifier: " + e.getMessage());
            return false;
        }
    }

    public boolean changerStatut(int id, String statut) {
        String sql = "UPDATE reclamation SET status=? WHERE id=?";
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur supprimer: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimerAdmin(int id) {
        String sql = "DELETE FROM reclamation WHERE id=?";
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur supprimerAdmin: " + e.getMessage());
            return false;
        }
    }
}