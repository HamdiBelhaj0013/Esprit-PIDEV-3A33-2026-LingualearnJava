package org.example.service.tests;

import org.example.entity.tests.TestResult;
import org.example.repository.tests.TestResultRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class TestResultService implements ITestResultService {

    private final TestResultRepository repo;

    public TestResultService() {
        this.repo = new TestResultRepository();
    }

    @Override
    public void create(TestResult result) {
        valider(result);
        if (result.getDateTaken() == null)
            result.setDateTaken(LocalDateTime.now());
        repo.save(result);
    }

    @Override
    public TestResult findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Résultat introuvable : " + id));
    }

    @Override public List<TestResult> findAll()               { return repo.findAll(); }
    @Override public List<TestResult> findByUserId(Long uid)  { return repo.findByUserId(uid); }
    @Override public List<TestResult> findByMockTestId(Long tid) { return repo.findByMockTestId(tid); }

    @Override
    public void update(TestResult result) {
        if (result.getId() == null)
            throw new IllegalArgumentException("Impossible de modifier un résultat sans identifiant.");
        valider(result);
        repo.save(result);
    }

    @Override
    public void delete(Long id) {
        repo.delete(id);
    }

    @Override public long   countAll()                               { return repo.countAll(); }
    @Override public double averageScore()                           { return repo.averageScore(); }
    @Override public double averageScoreByMockTestId(Long id)        { return repo.averageScoreByMockTestId(id); }
    @Override public double averageScoreByUserId(Long id)            { return repo.averageScoreByUserId(id); }

    // Ces méthodes nécessitent des requêtes supplémentaires — implémentation simple
    @Override public List<TestResult> filterByMinScore(float min)    { return repo.findAll().stream().filter(r -> r.getOverallScore() >= min).toList(); }
    @Override public List<TestResult> filterByMaxScore(float max)    { return repo.findAll().stream().filter(r -> r.getOverallScore() <= max).toList(); }
    @Override public List<TestResult> filterByScoreRange(float min, float max) {
        if (min > max) throw new IllegalArgumentException("min ne peut pas etre superieur a max.");
        return repo.findAll().stream().filter(r -> r.getOverallScore() >= min && r.getOverallScore() <= max).toList();
    }
    @Override public List<TestResult> sortByScoreDesc()              { return repo.findAll().stream().sorted((a, b) -> Float.compare(b.getOverallScore(), a.getOverallScore())).toList(); }
    @Override public List<TestResult> sortByScoreAsc()               { return repo.findAll().stream().sorted((a, b) -> Float.compare(a.getOverallScore(), b.getOverallScore())).toList(); }
    @Override public List<TestResult> sortByDateDesc()               { return repo.findAll(); } // déjà trié DESC
    @Override public float maxScoreByMockTestId(Long id)             { return (float) repo.findByMockTestId(id).stream().mapToDouble(r -> r.getOverallScore()).max().orElse(0); }
    @Override public float minScoreByMockTestId(Long id)             { return (float) repo.findByMockTestId(id).stream().mapToDouble(r -> r.getOverallScore()).min().orElse(0); }
    @Override public Map<String, Long> countByMockTest()             { return Map.of(); }

    private void valider(TestResult r) {
        if (r.getUser()     == null) throw new IllegalArgumentException("L'utilisateur est obligatoire.");
        if (r.getMockTest() == null) throw new IllegalArgumentException("Le test est obligatoire.");
        if (r.getOverallScore() < 0f || r.getOverallScore() > 100f)
            throw new IllegalArgumentException("Le score doit être entre 0 et 100.");
    }

    // ── VERROU PROGRESSION ────────────────────────────────────────────────────

    @Override
    public boolean hasPassedLevel(Long userId, String level, float minScore, Long languageId) {
        return repo.hasPassedLevel(userId, level, minScore, languageId);
    }

    @Override
    public float bestScoreInLevels(Long userId, String[] levels, Long languageId) {
        return repo.bestScoreInLevels(userId, levels, languageId);
    }
}