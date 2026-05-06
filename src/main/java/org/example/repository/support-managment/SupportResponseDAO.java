package org.example.repository.supportmanagement;

import org.example.entity.SupportResponse;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SupportResponseDAO {

    // ✅ PLUS de champ conn partagé

    public boolean ajouter(SupportResponse sr) {
        String sql = "INSERT INTO support_response (message, responded_at, reclamation_id, author_id) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sr.getMessage());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, sr.getReclamationId());
            // ✅ FK fix : si authorId est null ou 0 → on met NULL en DB
            if (sr.getAuthorId() != null && sr.getAuthorId() != 0)
                ps.setInt(4, sr.getAuthorId());
            else
                ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur ajouter reponse: " + e.getMessage());
            return false;
        }
    }

    public List<SupportResponse> getByReclamationId(int reclamationId) {
        List<SupportResponse> list = new ArrayList<>();
        String sql = "SELECT * FROM support_response WHERE reclamation_id=? ORDER BY responded_at ASC";
        try (Connection conn = org.example.util.MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SupportResponse sr = new SupportResponse();
                sr.setId(rs.getInt("id"));
                sr.setMessage(rs.getString("message"));
                sr.setReclamationId(rs.getInt("reclamation_id"));
                sr.setAuthorId(rs.getObject("author_id") != null ? rs.getInt("author_id") : null);
                Timestamp t = rs.getTimestamp("responded_at");
                if (t != null) sr.setRespondedAt(t.toLocalDateTime());
                list.add(sr);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getByReclamationId: " + e.getMessage());
        }
        return list;
    }
}