package services;

import entities.Reponse;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceReponse {
    private final Connection cnx;

    public ServiceReponse() {
        this.cnx = DBConnection.getInstance().getCnx();
    }

    public void ensureSchema() throws SQLException {
        if (cnx == null) {
            throw new SQLException("Database connection unavailable");
        }
        String questionsSql = "CREATE TABLE IF NOT EXISTS questions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "enonce TEXT NOT NULL,"
                + "domaine VARCHAR(120) NOT NULL,"
                + "niveau VARCHAR(80) NOT NULL,"
                + "actif TINYINT(1) NOT NULL DEFAULT 1"
                + ")";
        String reponsesSql = "CREATE TABLE IF NOT EXISTS reponses ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "question_id INT NOT NULL,"
                + "texte TEXT NOT NULL,"
                + "correcte TINYINT(1) NOT NULL DEFAULT 0,"
                + "CONSTRAINT fk_reponse_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE"
                + ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(questionsSql);
            st.execute(reponsesSql);
        }
    }

    public List<Reponse> getAll() throws SQLException {
        ensureSchema();
        List<Reponse> result = new ArrayList<>();
        String sql = "SELECT id, question_id, texte, correcte FROM reponses ORDER BY id DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public Optional<Reponse> getById(int id) throws SQLException {
        ensureSchema();
        String sql = "SELECT id, question_id, texte, correcte FROM reponses WHERE id = ?";
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

    public int add(Reponse r) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO reponses (question_id, texte, correcte) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getQuestionId());
            ps.setString(2, r.getTexte());
            ps.setBoolean(3, r.isCorrecte());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(Reponse r) throws SQLException {
        ensureSchema();
        String sql = "UPDATE reponses SET question_id = ?, texte = ?, correcte = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getQuestionId());
            ps.setString(2, r.getTexte());
            ps.setBoolean(3, r.isCorrecte());
            ps.setInt(4, r.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        ensureSchema();
        String sql = "DELETE FROM reponses WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Reponse map(ResultSet rs) throws SQLException {
        return new Reponse(
                rs.getInt("id"),
                rs.getInt("question_id"),
                rs.getString("texte"),
                rs.getBoolean("correcte")
        );
    }
}

