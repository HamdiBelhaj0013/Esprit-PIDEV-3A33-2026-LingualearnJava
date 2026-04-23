package org.example.repository;

import org.example.entity.FAQ;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FAQDAO {

    private Connection conn = DatabaseConnection.getConnection();

    private FAQ map(ResultSet rs) throws SQLException {
        FAQ f = new FAQ();
        f.setId(rs.getInt("id"));
        f.setQuestion(rs.getString("question"));
        f.setAnswer(rs.getString("answer"));
        f.setSubject(rs.getString("subject"));
        f.setCategory(rs.getString("category"));
        Timestamp t = rs.getTimestamp("submitted_at");
        if (t != null) f.setSubmittedAt(t.toLocalDateTime());
        return f;
    }

    public boolean ajouter(FAQ faq) {
        if (existeDejaQuestion(faq.getQuestion(), -1)) return false;

        String sql = "INSERT INTO faq (question, answer, subject, category, submitted_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, faq.getQuestion());
            ps.setString(2, faq.getAnswer());
            ps.setString(3, faq.getSubject());
            ps.setString(4, faq.getCategory());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur ajouter FAQ: " + e.getMessage());
            return false;
        }
    }

    public List<FAQ> getAll() {
        List<FAQ> list = new ArrayList<>();
        String sql = "SELECT * FROM faq ORDER BY submitted_at DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getAll FAQ: " + e.getMessage());
        }
        return list;
    }

    public List<FAQ> rechercher(String motCle) {
        List<FAQ> list = new ArrayList<>();
        String sql = "SELECT * FROM faq WHERE question LIKE ? OR category LIKE ? OR subject LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + motCle + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur recherche FAQ: " + e.getMessage());
        }
        return list;
    }

    public boolean modifier(FAQ faq) {
        if (existeDejaQuestion(faq.getQuestion(), faq.getId())) return false;

        String sql = "UPDATE faq SET question=?, answer=?, subject=?, category=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, faq.getQuestion());
            ps.setString(2, faq.getAnswer());
            ps.setString(3, faq.getSubject());
            ps.setString(4, faq.getCategory());
            ps.setInt(5, faq.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur modifier FAQ: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM faq WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur supprimer FAQ: " + e.getMessage());
            return false;
        }
    }

    public boolean existeDejaQuestion(String question, int excludeId) {
        String sql = "SELECT COUNT(*) FROM faq WHERE question = ? AND id != ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, question.trim());
            ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Erreur unicite: " + e.getMessage());
        }
        return false;
    }

    public int countByCategory(String category) {
        String sql = "SELECT COUNT(*) FROM faq WHERE category = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Erreur count: " + e.getMessage());
        }
        return 0;
    }
}
