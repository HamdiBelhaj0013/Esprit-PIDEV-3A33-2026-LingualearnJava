package org.example.service.tests;

import org.example.entity.tests.TestQuestion;
import org.example.repository.tests.TestQuestionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestQuestionService implements ITestQuestionService {

    private static final Set<String> VALID_SECTIONS =
            Set.of("Reading", "Listening", "Writing", "Speaking");

    private static final Set<String> SECTIONS_AVEC_OPTIONS =
            Set.of("Reading", "Listening");

    private final TestQuestionRepository repo;

    public TestQuestionService() {
        this.repo = new TestQuestionRepository();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void create(TestQuestion question) {
        valider(question);
        repo.save(question);
    }

    @Override
    public TestQuestion findById(Long id) {
        if (id == null)
            throw new IllegalArgumentException("L'identifiant est obligatoire.");
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune question trouvée avec l'id : " + id));
    }

    @Override
    public List<TestQuestion> findAll() {
        return repo.findAll();
    }

    @Override
    public void update(TestQuestion question) {
        if (question.getId() == null)
            throw new IllegalArgumentException("Impossible de modifier une question sans identifiant.");
        valider(question);
        repo.save(question);
    }

    @Override
    public void delete(Long id) {
        if (id == null)
            throw new IllegalArgumentException("L'identifiant est obligatoire.");
        // Vérifier que la question existe
        repo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Aucune question trouvée avec l'id : " + id));
        repo.delete(id);
    }

    @Override
    public void deleteAllByMockTestId(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        repo.deleteAllByMockTestId(mockTestId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RECHERCHE & FILTRAGE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<TestQuestion> findByMockTestId(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        return repo.findByMockTestId(mockTestId);
    }

    @Override
    public List<TestQuestion> findBySection(Long mockTestId, String sectionCategory) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        if (sectionCategory == null || sectionCategory.isBlank())
            throw new IllegalArgumentException("La section est obligatoire.");
        if (!VALID_SECTIONS.contains(sectionCategory))
            throw new IllegalArgumentException("Section invalide '" + sectionCategory
                    + "'. Valeurs : " + VALID_SECTIONS);
        return repo.findBySection(mockTestId, sectionCategory);
    }

    @Override
    public List<TestQuestion> search(Long mockTestId, String keyword) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        if (keyword == null || keyword.isBlank()) return findByMockTestId(mockTestId);
        return repo.search(mockTestId, keyword);
    }

    @Override
    public List<TestQuestion> filterByPoints(Long mockTestId, int points) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        if (points < 1 || points > 10)
            throw new IllegalArgumentException("Les points doivent être entre 1 et 10.");
        return repo.findByMockTestId(mockTestId).stream()
                .filter(q -> q.getPoints() == points)
                .toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRI
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<TestQuestion> sortBySectionAsc(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        return repo.findByMockTestId(mockTestId).stream()
                .sorted((a, b) -> a.getSectionCategory().compareTo(b.getSectionCategory()))
                .toList();
    }

    @Override
    public List<TestQuestion> sortByPointsAsc(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        return repo.findByMockTestId(mockTestId).stream()
                .sorted((a, b) -> Integer.compare(a.getPoints(), b.getPoints()))
                .toList();
    }

    @Override
    public List<TestQuestion> sortByPointsDesc(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        return repo.findByMockTestId(mockTestId).stream()
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public long countAll() {
        return repo.countAll();
    }

    @Override
    public long countByMockTestId(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        return repo.countByMockTestId(mockTestId);
    }

    @Override
    public int sumPointsByMockTestId(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        return repo.sumPointsByMockTestId(mockTestId);
    }

    @Override
    public double averagePointsByMockTestId(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        int sum   = repo.sumPointsByMockTestId(mockTestId);
        long count = repo.countByMockTestId(mockTestId);
        return count > 0 ? (double) sum / count : 0.0;
    }

    @Override
    public Map<String, Long> countBySectionForTest(Long mockTestId) {
        if (mockTestId == null)
            throw new IllegalArgumentException("L'identifiant du test est obligatoire.");
        Map<String, Long> result = new HashMap<>();
        for (String section : VALID_SECTIONS) {
            long count = repo.findBySection(mockTestId, section).size();
            if (count > 0) result.put(section, count);
        }
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════════════════════

    private void valider(TestQuestion q) {
        if (q.getMockTest() == null)
            throw new IllegalArgumentException("Le test associé est obligatoire.");
        if (q.getSectionCategory() == null || q.getSectionCategory().isBlank())
            throw new IllegalArgumentException("La section est obligatoire.");
        if (!VALID_SECTIONS.contains(q.getSectionCategory()))
            throw new IllegalArgumentException("Section invalide '" + q.getSectionCategory()
                    + "'. Valeurs : Reading, Listening, Writing, Speaking.");
        if (q.getQuestionText() == null || q.getQuestionText().isBlank())
            throw new IllegalArgumentException("Le texte de la question est obligatoire.");
        if (q.getQuestionText().trim().length() < 10)
            throw new IllegalArgumentException("Le texte doit contenir au moins 10 caractères.");
        if (q.getPoints() < 1)
            throw new IllegalArgumentException("Les points doivent être d'au moins 1.");
        if (q.getPoints() > 10)
            throw new IllegalArgumentException("Les points ne peuvent pas dépasser 10.");
        if (SECTIONS_AVEC_OPTIONS.contains(q.getSectionCategory())) {
            if (q.getOptions() == null || q.getOptions().isBlank())
                throw new IllegalArgumentException(
                        "Les sections Reading et Listening nécessitent au moins 2 options.");
            long nb = q.getOptions().chars().filter(c -> c == '"').count() / 2;
            if (nb < 2)
                throw new IllegalArgumentException(
                        "Minimum 2 options requises pour la section " + q.getSectionCategory() + ".");
        }
    }
}