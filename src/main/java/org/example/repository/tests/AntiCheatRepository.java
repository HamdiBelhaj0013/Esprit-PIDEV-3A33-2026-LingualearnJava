package org.example.repository.tests;

import org.example.util.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository JDBC pour la table anticheat_log.
 */
public class AntiCheatRepository {

    private Connection getConn() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── Enregistrer un événement ──────────────────────────────────────────────

    public void logEvent(long userId, long testId,
                         String userFullName, String testTitle,
                         String eventType, int nbSorties,
                         boolean soumisAuto, int penalitePct,
                         float scoreAvant, float scoreApres,
                         String detail) {
        String sql = """
                INSERT INTO anticheat_log
                    (user_id, test_id, user_full_name, test_title,
                     event_type, nb_sorties, soumis_auto, penalite_pct,
                     score_avant, score_apres, detail, event_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, testId);
            ps.setString(3, userFullName != null ? userFullName : "");
            ps.setString(4, testTitle   != null ? testTitle   : "");
            ps.setString(5, eventType);
            ps.setInt(6, nbSorties);
            ps.setBoolean(7, soumisAuto);
            ps.setInt(8, penalitePct);
            ps.setFloat(9, scoreAvant);
            ps.setFloat(10, scoreApres);
            ps.setString(11, detail);
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("AntiCheatRepository.logEvent échoué : " + e.getMessage());
        }
    }

    // ── Statut d'un étudiant sur un test ─────────────────────────────────────

    public Map<String, Object> getStatus(long userId, long testId) {
        String sql = """
                SELECT COUNT(*) as total_events,
                       SUM(CASE WHEN event_type = 'FOCUS_LOST' THEN 1 ELSE 0 END)  as nb_sorties,
                       SUM(CASE WHEN event_type = 'COPY_ATTEMPT' THEN 1 ELSE 0 END) as nb_copies,
                       SUM(CASE WHEN event_type = 'PASTE_ATTEMPT' THEN 1 ELSE 0 END) as nb_pastes,
                       MAX(soumis_auto)  as soumis_auto,
                       MAX(penalite_pct) as penalite_pct,
                       MAX(score_apres)  as score_final,
                       MIN(event_at)     as premier_event,
                       MAX(event_at)     as dernier_event
                FROM anticheat_log
                WHERE user_id = ? AND test_id = ?
                """;
        Map<String, Object> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, testId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.put("total_events",  rs.getInt("total_events"));
                    result.put("nb_sorties",    rs.getInt("nb_sorties"));
                    result.put("nb_copies",     rs.getInt("nb_copies"));
                    result.put("nb_pastes",     rs.getInt("nb_pastes"));
                    result.put("soumis_auto",   rs.getBoolean("soumis_auto"));
                    result.put("penalite_pct",  rs.getInt("penalite_pct"));
                    result.put("score_final",   rs.getFloat("score_final"));
                    result.put("cheating",      rs.getBoolean("soumis_auto")
                            || rs.getInt("nb_sorties") > 0
                            || rs.getInt("nb_copies")  > 0
                            || rs.getInt("nb_pastes")  > 0);
                    Timestamp t1 = rs.getTimestamp("premier_event");
                    Timestamp t2 = rs.getTimestamp("dernier_event");
                    result.put("premier_event", t1 != null ? t1.toLocalDateTime().toString() : null);
                    result.put("dernier_event", t2 != null ? t2.toLocalDateTime().toString() : null);
                }
            }
        } catch (SQLException e) {
            System.err.println("getStatus échoué : " + e.getMessage());
        }
        return result;
    }

    // ── Tous les logs (pour l'admin) ──────────────────────────────────────────

    public List<Map<String, Object>> findAll(int limit) {
        String sql = """
                SELECT * FROM anticheat_log
                ORDER BY event_at DESC
                LIMIT ?
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("findAll échoué : " + e.getMessage());
        }
        return list;
    }

    // ── Logs par utilisateur ──────────────────────────────────────────────────

    public List<Map<String, Object>> findByUser(long userId) {
        String sql = "SELECT * FROM anticheat_log WHERE user_id = ? ORDER BY event_at DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("findByUser échoué : " + e.getMessage());
        }
        return list;
    }

    // ── Stats globales ────────────────────────────────────────────────────────

    public Map<String, Object> getGlobalStats() {
        String sql = """
                SELECT COUNT(DISTINCT user_id) as nb_users_suspects,
                       COUNT(DISTINCT test_id)  as nb_tests_concernes,
                       COUNT(*)                  as total_incidents,
                       SUM(CASE WHEN soumis_auto = 1 THEN 1 ELSE 0 END) as nb_soumissions_auto,
                       SUM(CASE WHEN event_type = 'COPY_ATTEMPT'  THEN 1 ELSE 0 END) as nb_copies,
                       SUM(CASE WHEN event_type = 'PASTE_ATTEMPT' THEN 1 ELSE 0 END) as nb_pastes
                FROM anticheat_log
                """;
        Map<String, Object> stats = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.put("nb_users_suspects",   rs.getInt("nb_users_suspects"));
                stats.put("nb_tests_concernes",  rs.getInt("nb_tests_concernes"));
                stats.put("total_incidents",      rs.getInt("total_incidents"));
                stats.put("nb_soumissions_auto",  rs.getInt("nb_soumissions_auto"));
                stats.put("nb_copies",            rs.getInt("nb_copies"));
                stats.put("nb_pastes",            rs.getInt("nb_pastes"));
            }
        } catch (SQLException e) {
            System.err.println("getGlobalStats échoué : " + e.getMessage());
        }
        return stats;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id",             rs.getLong("id"));
        row.put("user_id",        rs.getLong("user_id"));
        row.put("test_id",        rs.getLong("test_id"));
        row.put("user_full_name", rs.getString("user_full_name"));
        row.put("test_title",     rs.getString("test_title"));
        row.put("event_type",     rs.getString("event_type"));
        row.put("nb_sorties",     rs.getInt("nb_sorties"));
        row.put("soumis_auto",    rs.getBoolean("soumis_auto"));
        row.put("penalite_pct",   rs.getInt("penalite_pct"));
        row.put("score_avant",    rs.getFloat("score_avant"));
        row.put("score_apres",    rs.getFloat("score_apres"));
        row.put("detail",         rs.getString("detail"));
        Timestamp t = rs.getTimestamp("event_at");
        row.put("event_at", t != null ? t.toLocalDateTime().toString() : null);
        return row;
    }
}