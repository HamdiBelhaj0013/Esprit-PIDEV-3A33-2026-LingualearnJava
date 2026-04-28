package org.example.service.tests;

import org.example.entity.tests.MockTest;
import org.example.entity.tests.PlatformLanguage;

import java.util.List;
import java.util.Map;

public interface IMockTestService {

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════════════════════════════

    void create(MockTest mockTest);
    MockTest findById(Long id);
    List<MockTest> findAll();
    void update(MockTest mockTest);
    void delete(Long id);

    // ══════════════════════════════════════════════════════════════════════════
    //  RECHERCHE & FILTRAGE
    // ══════════════════════════════════════════════════════════════════════════

    List<MockTest> search(String keyword);
    List<MockTest> filterByTestType(String testType);
    List<MockTest> filterByLevel(String level);
    List<MockTest> filterByLanguageId(Long platformLanguageId);
    List<MockTest> filterAdvanced(String testType, String level, Long languageId);
    public List<PlatformLanguage> findAllLanguages();
    // ══════════════════════════════════════════════════════════════════════════
    //  TRI
    // ══════════════════════════════════════════════════════════════════════════

    List<MockTest> sortByTitleAsc();
    List<MockTest> sortByDurationAsc();
    List<MockTest> sortByDurationDesc();
    List<MockTest> sortByDateDesc();

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════

    long countAll();
    Map<String, Long> countByTestType();
    Map<String, Long> countByLevel();
    double averageDuration();
    MockTest findLongestTest();
    MockTest findShortestTest();
}
