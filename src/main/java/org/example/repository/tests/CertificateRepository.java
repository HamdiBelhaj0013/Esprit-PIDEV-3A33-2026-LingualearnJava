package org.example.repository.tests;

import org.example.entity.tests.Certificate;
import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CertificateRepository {

    private Connection getConn() {
        return MyDataBase.getInstance().getConnection();
    }

    public Certificate save(Certificate c) {
        if (c.getId() == null) return insert(c);
        update(c);
        return c;
    }

    private Certificate insert(Certificate c) {
        String sql = """
                INSERT INTO certificate
                    (uuid, user_id, user_full_name, user_email,
                     niveau, language_name, score_moyen, pdf_path, issued_at, is_valid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getUuid());
            ps.setLong(2, c.getUserId());
            ps.setString(3, c.getUserFullName() != null ? c.getUserFullName() : "");
            ps.setString(4, c.getUserEmail()    != null ? c.getUserEmail()    : "");
            ps.setString(5, c.getNiveau());
            ps.setString(6, c.getLanguageName());
            ps.setFloat(7, c.getScoreMoyen());
            ps.setString(8, c.getPdfPath());
            ps.setTimestamp(9, Timestamp.valueOf(
                    c.getIssuedAt() != null ? c.getIssuedAt() : LocalDateTime.now()));
            ps.setBoolean(10, c.isValid());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert certificate échoué : " + e.getMessage(), e);
        }
        return c;
    }

    private void update(Certificate c) {
        String sql = "UPDATE certificate SET pdf_path=?, is_valid=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, c.getPdfPath());
            ps.setBoolean(2, c.isValid());
            ps.setLong(3, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update certificate échoué : " + e.getMessage(), e);
        }
    }

    public Optional<Certificate> findByUuid(String uuid) {
        String sql = "SELECT * FROM certificate WHERE uuid = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUuid échoué : " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Certificate> findByUserId(Long userId) {
        List<Certificate> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT * FROM certificate WHERE user_id=? ORDER BY issued_at DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUserId échoué : " + e.getMessage(), e);
        }
        return list;
    }

    public Optional<Certificate> findExisting(Long userId, String niveau, String languageName) {
        String sql = "SELECT * FROM certificate WHERE user_id=? AND niveau=? AND language_name=? ORDER BY issued_at DESC LIMIT 1";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, niveau);
            ps.setString(3, languageName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findExisting échoué : " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    private Certificate map(ResultSet rs) throws SQLException {
        Certificate c = new Certificate();
        c.setId(rs.getLong("id"));
        c.setUuid(rs.getString("uuid"));
        c.setUserId(rs.getLong("user_id"));
        c.setUserFullName(rs.getString("user_full_name"));
        c.setUserEmail(rs.getString("user_email"));
        c.setNiveau(rs.getString("niveau"));
        c.setLanguageName(rs.getString("language_name"));
        c.setScoreMoyen(rs.getFloat("score_moyen"));
        c.setPdfPath(rs.getString("pdf_path"));
        c.setValid(rs.getBoolean("is_valid"));
        Timestamp ia = rs.getTimestamp("issued_at");
        if (ia != null) c.setIssuedAt(ia.toLocalDateTime());
        return c;
    }
}