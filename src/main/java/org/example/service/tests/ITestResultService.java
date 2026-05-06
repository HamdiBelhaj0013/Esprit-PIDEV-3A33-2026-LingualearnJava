package org.example.service.tests;

import org.example.entity.tests.TestResult;
import java.util.List;
import java.util.Map;

public interface ITestResultService {

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════════════════════════════

    void create(TestResult result);
    TestResult findById(Long id);
    List<TestResult> findAll();
    void update(TestResult result);
    void delete(Long id);

    // ══════════════════════════════════════════════════════════════════════════
    //  RECHERCHE & FILTRAGE
    // ══════════════════════════════════════════════════════════════════════════

    List<TestResult> findByUserId(Long userId);
    List<TestResult> findByMockTestId(Long mockTestId);
    List<TestResult> filterByMinScore(float minScore);
    List<TestResult> filterByMaxScore(float maxScore);
    List<TestResult> filterByScoreRange(float minScore, float maxScore);

    // ══════════════════════════════════════════════════════════════════════════
    //  TRI
    // ══════════════════════════════════════════════════════════════════════════

    List<TestResult> sortByScoreDesc();
    List<TestResult> sortByScoreAsc();
    List<TestResult> sortByDateDesc();

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════

    long countAll();
    double averageScore();
    double averageScoreByMockTestId(Long mockTestId);
    double averageScoreByUserId(Long userId);
    float maxScoreByMockTestId(Long mockTestId);
    float minScoreByMockTestId(Long mockTestId);
    Map<String, Long> countByMockTest();

    // ══════════════════════════════════════════════════════════════════════════
    //  PROGRESSION (Metier #4 — Verrouillage par langue)
    // ══════════════════════════════════════════════════════════════════════════

    boolean hasPassedLevel(Long userId, String level, float minScore, Long languageId);
    float bestScoreInLevels(Long userId, String[] levels, Long languageId);
}