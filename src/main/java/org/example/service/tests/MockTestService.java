package org.example.service.tests;

import org.example.entity.tests.MockTest;
import org.example.entity.tests.PlatformLanguage;
import org.example.entity.tests.TestQuestion;
import org.example.entity.tests.TestResult;
import org.example.repository.tests.MockTestRepository;
import org.example.repository.tests.TestAnswerRepository;
import org.example.repository.tests.TestQuestionRepository;
import org.example.repository.tests.TestResultRepository;
import org.example.util.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

public class MockTestService implements IMockTestService {

    private static final List<String> VALID_TYPES =
            List.of("TOEFL", "IELTS", "DELF", "DALF", "TCF", "Cambridge", "WRITING");
    private static final List<String> VALID_LEVELS =
            List.of("A1", "A2", "B1", "B2", "C1", "C2");

    private final MockTestRepository     mockTestRepo;
    private final TestQuestionRepository questionRepo;
    private final TestResultRepository   resultRepo;

    public MockTestService() {
        this.mockTestRepo = new MockTestRepository();
        this.questionRepo = new TestQuestionRepository();
        this.resultRepo   = new TestResultRepository();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD — MockTest
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void create(MockTest mockTest) {
        validerMockTest(mockTest);
        if (mockTest.getTestCategory() == null || mockTest.getTestCategory().isBlank())
            mockTest.setTestCategory(mockTest.getTestType());
        mockTestRepo.save(mockTest);
    }

    @Override
    public MockTest findById(Long id) {
        if (id == null) throw new IllegalArgumentException("L'identifiant est obligatoire.");
        return mockTestRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aucun test trouvé avec l'id : " + id));
    }

    @Override
    public List<MockTest> findAll() {
        return mockTestRepo.findAll();
    }

    @Override
    public void update(MockTest mockTest) {
        if (mockTest.getId() == null)
            throw new IllegalArgumentException("Impossible de modifier un test sans identifiant.");
        validerMockTest(mockTest);
        if (mockTest.getTestCategory() == null || mockTest.getTestCategory().isBlank())
            mockTest.setTestCategory(mockTest.getTestType());
        mockTestRepo.save(mockTest);
    }

    @Override
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("L'identifiant est obligatoire.");

        Connection conn = MyDataBase.getInstance().getConnection();

        try {
            // 1. Récupérer tous les IDs de test_result liés à ce test
            //    puis supprimer test_answer pour chacun
            List<TestResult> results = resultRepo.findByMockTestId(id);
            TestAnswerRepository answerRepo = new TestAnswerRepository();
            for (TestResult r : results) {
                if (r.getId() != null) {
                    answerRepo.deleteByResultId(r.getId());
                }
            }

            // 2. Supprimer les test_result liés au test
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM test_result WHERE mock_test_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // 3. Supprimer les test_question liés au test
            questionRepo.deleteAllByMockTestId(id);

            // 4. Supprimer le test lui-même
            mockTestRepo.delete(id);

        } catch (Exception e) {
            throw new RuntimeException(
                    "delete MockTest échoué : " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD — TestQuestion
    // ══════════════════════════════════════════════════════════════════════════

    public void createQuestion(TestQuestion q) {
        validerQuestion(q);
        questionRepo.save(q);
    }

    public void updateQuestion(TestQuestion q) {
        if (q.getId() == null)
            throw new IllegalArgumentException("Impossible de modifier une question sans identifiant.");
        validerQuestion(q);
        questionRepo.save(q);
    }

    public void deleteQuestion(Long id) {
        if (id == null) throw new IllegalArgumentException("L'identifiant est obligatoire.");
        questionRepo.delete(id);
    }

    public List<TestQuestion> findQuestionsByTest(Long mockTestId) {
        return questionRepo.findByMockTestId(mockTestId);
    }

    public List<TestQuestion> searchQuestions(Long mockTestId, String keyword) {
        return questionRepo.search(mockTestId, keyword);
    }

    public List<TestQuestion> findQuestionsBySection(Long mockTestId, String section) {
        return questionRepo.findBySection(mockTestId, section);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD — TestResult
    // ══════════════════════════════════════════════════════════════════════════

    public void saveResult(TestResult result) {
        if (result.getUser()     == null) throw new IllegalArgumentException("L'utilisateur est obligatoire.");
        if (result.getMockTest() == null) throw new IllegalArgumentException("Le test est obligatoire.");
        resultRepo.save(result);
    }

    public List<TestResult> findAllResults() {
        return resultRepo.findAll();
    }

    public List<TestResult> findResultsByTest(Long mockTestId) {
        return resultRepo.findByMockTestId(mockTestId);
    }

    public List<TestResult> findResultsByUser(Long userId) {
        return resultRepo.findByUserId(userId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RECHERCHE & FILTRAGE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<MockTest> search(String keyword) {
        return mockTestRepo.search(keyword);
    }

    @Override
    public List<MockTest> filterByTestType(String testType) {
        return mockTestRepo.filterAdvanced(testType, null, null);
    }

    @Override
    public List<MockTest> filterByLevel(String level) {
        return mockTestRepo.filterAdvanced(null, level, null);
    }

    @Override
    public List<MockTest> filterByLanguageId(Long platformLanguageId) {
        return mockTestRepo.filterAdvanced(null, null, platformLanguageId);
    }

    @Override
    public List<MockTest> filterAdvanced(String testType, String level, Long languageId) {
        return mockTestRepo.filterAdvanced(testType, level, languageId);
    }

    @Override
    public List<PlatformLanguage> findAllLanguages() {
        return mockTestRepo.findAllLanguages();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRI
    // ══════════════════════════════════════════════════════════════════════════

    @Override public List<MockTest> sortByTitleAsc()    { return mockTestRepo.sortByTitleAsc(); }
    @Override public List<MockTest> sortByDurationAsc() { return mockTestRepo.sortByDurationAsc(); }
    @Override public List<MockTest> sortByDurationDesc(){ return mockTestRepo.sortByDurationDesc(); }
    @Override public List<MockTest> sortByDateDesc()    { return mockTestRepo.sortByDateDesc(); }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════

    @Override public long countAll()          { return mockTestRepo.countAll(); }
    public long countAllQuestions()           { return questionRepo.countAll(); }
    public long countAllResults()             { return resultRepo.countAll(); }
    public long countQuestionsByTest(Long id) { return questionRepo.countByMockTestId(id); }
    public int  sumPointsByTest(Long id)      { return questionRepo.sumPointsByMockTestId(id); }

    @Override public Map<String, Long> countByTestType() { return Map.of(); }
    @Override public Map<String, Long> countByLevel()    { return Map.of(); }
    @Override public double averageDuration()            { return mockTestRepo.averageDuration(); }
    @Override public MockTest findLongestTest()          { return mockTestRepo.sortByDurationDesc().stream().findFirst().orElse(null); }
    @Override public MockTest findShortestTest()         { return mockTestRepo.sortByDurationAsc().stream().findFirst().orElse(null); }

    // ══════════════════════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════════════════════

    private void validerMockTest(MockTest m) {
        if (m.getTitle() == null || m.getTitle().isBlank())
            throw new IllegalArgumentException("Le titre est obligatoire.");
        if (m.getTitle().trim().length() < 5)
            throw new IllegalArgumentException("Le titre doit avoir au moins 5 caractères.");
        if (m.getTestType() == null || !VALID_TYPES.contains(m.getTestType()))
            throw new IllegalArgumentException("Type invalide. Valeurs : " + VALID_TYPES);
        if (m.getLevel() == null || !VALID_LEVELS.contains(m.getLevel()))
            throw new IllegalArgumentException("Niveau invalide. Valeurs : " + VALID_LEVELS);
        if (m.getDurationMinutes() < 2 || m.getDurationMinutes() > 90)
            throw new IllegalArgumentException("La duree doit etre entre 2 et 90 minutes.");
        if (m.getPlatformLanguage() == null)
            throw new IllegalArgumentException("La langue est obligatoire.");
    }

    private void validerQuestion(TestQuestion q) {
        if (q.getMockTest() == null)
            throw new IllegalArgumentException("Le test est obligatoire.");
        if (q.getSectionCategory() == null || q.getSectionCategory().isBlank())
            throw new IllegalArgumentException("La section est obligatoire.");
        if (q.getQuestionText() == null || q.getQuestionText().isBlank())
            throw new IllegalArgumentException("Le texte de la question est obligatoire.");
        if (q.getPoints() < 1)
            throw new IllegalArgumentException("Les points doivent être au moins 1.");
    }
}