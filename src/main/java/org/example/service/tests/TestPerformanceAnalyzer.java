package org.example.service.tests;

import org.example.entity.tests.TestResult;
import org.example.repository.tests.TestAnswerRepository;
import org.example.util.MyDataBase;

import java.sql.*;
import java.util.*;

/**
 * Métier Avancé #1 — TestPerformanceAnalyzer
 * Calcule des statistiques avancées à partir des résultats de l'utilisateur.
 *
 * CORRECTION 1 : getPerformanceParSection() utilise maintenant la table
 * test_answer pour obtenir les vrais scores par section.
 */
public class TestPerformanceAnalyzer {

    private final Long                userId;
    private final TestAnswerRepository answerRepo;

    public TestPerformanceAnalyzer(Long userId) {
        this.userId    = userId;
        this.answerRepo = new TestAnswerRepository();
    }

    private Connection getConn() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── Score moyen global ─────────────────────────────────────────────────

    public double getScoreMoyenGlobal() {
        String sql = "SELECT AVG(overall_score) FROM test_result WHERE user_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) { return 0.0; }
    }

    // ── Nombre total de tests passés ──────────────────────────────────────

    public int getNombreTestsPassés() {
        String sql = "SELECT COUNT(*) FROM test_result WHERE user_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    // ── Meilleur score ────────────────────────────────────────────────────

    public double getMeilleurScore() {
        String sql = "SELECT MAX(overall_score) FROM test_result WHERE user_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) { return 0.0; }
    }

    // ── Niveau actuel atteint ─────────────────────────────────────────────
    // Le niveau le plus élevé où l'user a >= 50% dans au moins 1 test

    public String getNiveauActuel() {
        String[] niveaux = {"C2", "C1", "B2", "B1", "A2", "A1"};
        String sql = """
                SELECT COUNT(*) FROM test_result r
                JOIN mock_test m ON r.mock_test_id = m.id
                WHERE r.user_id = ? AND m.level = ? AND r.overall_score >= 50
                """;
        try {
            for (String niveau : niveaux) {
                try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                    ps.setLong(1, userId);
                    ps.setString(2, niveau);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) return niveau;
                }
            }
        } catch (SQLException e) { return "Débutant"; }
        return "Débutant";
    }

    // ── Progression temporelle ────────────────────────────────────────────
    // Liste de (index, score) du plus ancien au plus récent

    public List<double[]> getProgressionTemporelle() {
        String sql = """
                SELECT overall_score FROM test_result
                WHERE user_id = ? ORDER BY date_taken ASC LIMIT 20
                """;
        List<double[]> points = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            int i = 1;
            while (rs.next())
                points.add(new double[]{i++, rs.getDouble("overall_score")});
        } catch (SQLException e) { /* empty */ }
        return points;
    }

    // ── Performance par type de test ──────────────────────────────────────

    public Map<String, Double> getPerformanceParType() {
        String sql = """
                SELECT m.test_type, AVG(r.overall_score) as avg_score
                FROM test_result r JOIN mock_test m ON r.mock_test_id = m.id
                WHERE r.user_id = ? GROUP BY m.test_type ORDER BY avg_score DESC
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                result.put(rs.getString("test_type"), rs.getDouble("avg_score"));
        } catch (SQLException e) { /* empty */ }
        return result;
    }

    // ── Performance par niveau ────────────────────────────────────────────

    public Map<String, Double> getPerformanceParNiveau() {
        String sql = """
                SELECT m.level, AVG(r.overall_score) as avg_score
                FROM test_result r JOIN mock_test m ON r.mock_test_id = m.id
                WHERE r.user_id = ? GROUP BY m.level ORDER BY m.level ASC
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                result.put(rs.getString("level"), rs.getDouble("avg_score"));
        } catch (SQLException e) { /* empty */ }
        return result;
    }

    // ── CORRECTION 1 : Performance RÉELLE par section ─────────────────────
    /**
     * Utilise la table test_answer pour calculer :
     *   score_section = SUM(points_obtained) / SUM(points_max) * 100
     *
     * Si test_answer est vide (anciens résultats avant migration),
     * retourne un Map vide → le ProfileController affichera
     * "Passez des tests pour détecter vos faiblesses."
     */
    public Map<String, Double> getPerformanceParSection() {
        return answerRepo.getScoreParSectionPourUser(userId);
    }

    /**
     * Variante filtrée par langue — utilisée dans ProfileView
     * quand l'user a choisi une langue spécifique.
     */
    public Map<String, Double> getPerformanceParSectionEtLangue(Long langId) {
        return answerRepo.getScoreParSectionPourUserEtLangue(userId, langId);
    }

    // ── Tous les résultats de l'user ──────────────────────────────────────

    public List<TestResult> getTousLesResultats() {
        return new TestResultService().findByUserId(userId);
    }

    // ── Résumé complet ────────────────────────────────────────────────────

    public static class Resume {
        public double              scoreMoyen;
        public int                 testsPassés;
        public double              meilleurScore;
        public String              niveauActuel;
        public List<double[]>      progression;
        public Map<String, Double> parType;
        public Map<String, Double> parNiveau;
        public Map<String, Double> parSection;
    }

    public Resume calculerResume() {
        Resume r = new Resume();
        r.scoreMoyen    = getScoreMoyenGlobal();
        r.testsPassés   = getNombreTestsPassés();
        r.meilleurScore = getMeilleurScore();
        r.niveauActuel  = getNiveauActuel();
        r.progression   = getProgressionTemporelle();
        r.parType       = getPerformanceParType();
        r.parNiveau     = getPerformanceParNiveau();
        r.parSection    = getPerformanceParSection();
        return r;
    }

    /**
     * Résumé filtré par langue — pour ProfileView après sélection de langue.
     */
    public Resume calculerResumePourLangue(Long langId) {
        Resume r = calculerResume();
        // Remplacer parSection par les scores filtrés sur la langue
        r.parSection = getPerformanceParSectionEtLangue(langId);
        return r;
    }
}