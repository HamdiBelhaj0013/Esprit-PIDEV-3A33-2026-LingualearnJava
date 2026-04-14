package services;

import entities.Question;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceQuestion {
    private final Connection cnx;

    public ServiceQuestion() {
        this.cnx = DBConnection.getInstance().getCnx();
    }

    public void ensureSchema() throws SQLException {
        if (cnx == null) {
            throw new SQLException("Database connection unavailable");
        }
        String sql = "CREATE TABLE IF NOT EXISTS questions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "enonce TEXT NOT NULL,"
                + "domaine VARCHAR(120) NOT NULL,"
                + "niveau VARCHAR(80) NOT NULL,"
                + "actif TINYINT(1) NOT NULL DEFAULT 1"
                + ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        }
    }

    public List<Question> getAll() throws SQLException {
        ensureSchema();
        List<Question> result = new ArrayList<>();
        String sql = "SELECT id, enonce, domaine, niveau, actif FROM questions ORDER BY id DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public Optional<Question> getById(int id) throws SQLException {
        ensureSchema();
        String sql = "SELECT id, enonce, domaine, niveau, actif FROM questions WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int add(Question q) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO questions (enonce, domaine, niveau, actif) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getEnonce());
            ps.setString(2, q.getDomaine());
            ps.setString(3, q.getNiveau());
            ps.setBoolean(4, q.isActif());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(Question q) throws SQLException {
        ensureSchema();
        String sql = "UPDATE questions SET enonce = ?, domaine = ?, niveau = ?, actif = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, q.getEnonce());
            ps.setString(2, q.getDomaine());
            ps.setString(3, q.getNiveau());
            ps.setBoolean(4, q.isActif());
            ps.setInt(5, q.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        ensureSchema();
        String sql = "DELETE FROM questions WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Question map(ResultSet rs) throws SQLException {
        return new Question(
                rs.getInt("id"),
                rs.getString("enonce"),
                rs.getString("domaine"),
                rs.getString("niveau"),
                rs.getBoolean("actif")
        );
    }
}

