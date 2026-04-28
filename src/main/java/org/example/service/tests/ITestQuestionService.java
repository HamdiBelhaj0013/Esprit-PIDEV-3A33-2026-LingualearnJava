package org.example.service.tests;

import org.example.entity.tests.TestQuestion;
import java.util.List;
import java.util.Map;

public interface ITestQuestionService {

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════════════════════════════

    void create(TestQuestion question);
    TestQuestion findById(Long id);
    List<TestQuestion> findAll();
    void update(TestQuestion question);
    void delete(Long id);
    void deleteAllByMockTestId(Long mockTestId);

    // ══════════════════════════════════════════════════════════════════════════
    //  RECHERCHE & FILTRAGE
    // ══════════════════════════════════════════════════════════════════════════

    List<TestQuestion> findByMockTestId(Long mockTestId);
    List<TestQuestion> findBySection(Long mockTestId, String sectionCategory);
    List<TestQuestion> search(Long mockTestId, String keyword);
    List<TestQuestion> filterByPoints(Long mockTestId, int points);

    // ══════════════════════════════════════════════════════════════════════════
    //  TRI
    // ══════════════════════════════════════════════════════════════════════════

    List<TestQuestion> sortBySectionAsc(Long mockTestId);
    List<TestQuestion> sortByPointsAsc(Long mockTestId);
    List<TestQuestion> sortByPointsDesc(Long mockTestId);

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════

    long countAll();
    long countByMockTestId(Long mockTestId);
    int sumPointsByMockTestId(Long mockTestId);
    double averagePointsByMockTestId(Long mockTestId);
    Map<String, Long> countBySectionForTest(Long mockTestId);
}
