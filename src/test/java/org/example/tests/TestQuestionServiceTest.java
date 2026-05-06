package org.example.tests;

import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestQuestion;
import org.example.service.tests.MockTestService;
import org.example.util.MyDataBase;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestQuestionServiceTest {

    private static MockTestService service;
    private static MockTest        mockTest;
    private static Long            questionIdToCleanup = null;

    @BeforeAll
    static void setUp() {
        MyDataBase.getInstance();
        service = new MockTestService();

        // Charger le premier test disponible via JDBC
        try {
            Connection conn = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, title, test_type, level, duration_minutes FROM mock_test LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                mockTest = new MockTest();
                mockTest.setId(rs.getLong("id"));
                mockTest.setTitle(rs.getString("title"));
                mockTest.setTestType(rs.getString("test_type"));
                mockTest.setLevel(rs.getString("level"));
                mockTest.setDurationMinutes(rs.getInt("duration_minutes"));
            }
            rs.close(); ps.close();
        } catch (Exception e) {
            fail("Impossible de charger le test : " + e.getMessage());
        }
        assertNotNull(mockTest, "mock_test doit contenir au moins 1 ligne.");
    }

    @AfterEach
    void cleanup() {
        if (questionIdToCleanup != null) {
            try {
                Connection conn = MyDataBase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM test_question WHERE id = ?");
                ps.setLong(1, questionIdToCleanup);
                ps.executeUpdate();
                ps.close();
            } catch (Exception ignored) {}
            questionIdToCleanup = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("CREATE — question Reading valide")
    void testCreate_ReadingValide() {
        TestQuestion q = creerQuestion("Reading",
                "What is the main idea of the passage about renewable energy resources?",
                "[\"Energy costs\",\"Environmental benefits\",\"Political impact\",\"Economic growth\"]",
                "Environmental benefits", 3);
        assertNotNull(q.getId());
        assertTrue(q.getId() > 0);
        questionIdToCleanup = q.getId();
    }

    @Test @Order(2)
    @DisplayName("CREATE — question Writing valide")
    void testCreate_WritingValide() {
        TestQuestion q = creerQuestion("Writing",
                "Write an essay about the impact of technology on modern education systems.",
                null, null, 5);
        assertNotNull(q.getId());
        questionIdToCleanup = q.getId();
    }

    @Test @Order(3)
    @DisplayName("CREATE — section vide → exception")
    void testCreate_SectionVide() {
        TestQuestion q = new TestQuestion();
        q.setMockTest(mockTest);
        q.setSectionCategory("");
        q.setQuestionText("What is the correct form of the verb in this sentence context?");
        q.setPoints(2);
        assertThrows(IllegalArgumentException.class, () -> service.createQuestion(q));
    }

    @Test @Order(4)
    @DisplayName("CREATE — texte < 10 caractères → exception")
    void testCreate_TexteTropCourt() {
        TestQuestion q = new TestQuestion();
        q.setMockTest(mockTest);
        q.setSectionCategory("Reading");
        q.setQuestionText("Court?");
        q.setOptions("[\"A\",\"B\"]");
        q.setCorrectAnswer("A");
        q.setPoints(2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createQuestion(q));
        assertTrue(ex.getMessage().contains("10") || ex.getMessage().contains("texte"));
    }

    @Test @Order(5)
    @DisplayName("CREATE — points = 0 → exception")
    void testCreate_PointsZero() {
        TestQuestion q = new TestQuestion();
        q.setMockTest(mockTest);
        q.setSectionCategory("Speaking");
        q.setQuestionText("Describe a memorable experience from your childhood years.");
        q.setPoints(0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createQuestion(q));
        assertTrue(ex.getMessage().contains("1"));
    }

    @Test @Order(6)
    @DisplayName("CREATE — test associé null → exception")
    void testCreate_TestNull() {
        TestQuestion q = new TestQuestion();
        q.setMockTest(null);
        q.setSectionCategory("Writing");
        q.setQuestionText("Describe advantages and disadvantages of living in a big city.");
        q.setPoints(3);
        assertThrows(IllegalArgumentException.class, () -> service.createQuestion(q));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(7)
    @DisplayName("READ — findQuestionsByTest retourne liste non nulle")
    void testFindByMockTestId() {
        List<TestQuestion> list = service.findQuestionsByTest(mockTest.getId());
        assertNotNull(list);
    }

    @Test @Order(8)
    @DisplayName("READ — findQuestionsBySection filtre par section")
    void testFindBySection() {
        List<TestQuestion> list = service.findQuestionsBySection(mockTest.getId(), "Reading");
        assertNotNull(list);
        list.forEach(q -> assertEquals("Reading", q.getSectionCategory()));
    }

    @Test @Order(9)
    @DisplayName("READ — searchQuestions retourne résultats non nuls")
    void testSearch() {
        List<TestQuestion> list = service.searchQuestions(mockTest.getId(), "what");
        assertNotNull(list);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(10)
    @DisplayName("STATS — countAllQuestions >= 0")
    void testCountAll() {
        assertTrue(service.countAllQuestions() >= 0);
    }

    @Test @Order(11)
    @DisplayName("STATS — countQuestionsByTest >= 0")
    void testCountByMockTestId() {
        assertTrue(service.countQuestionsByTest(mockTest.getId()) >= 0);
    }

    @Test @Order(12)
    @DisplayName("STATS — sumPointsByTest >= 0")
    void testSumPoints() {
        assertTrue(service.sumPointsByTest(mockTest.getId()) >= 0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(13)
    @DisplayName("UPDATE — modification valide sauvegardée")
    void testUpdate_Success() {
        TestQuestion q = creerQuestion("Speaking",
                "Describe a place that had a significant impact on your personal life.",
                null, "oral response evaluated", 4);
        questionIdToCleanup = q.getId();
        q.setCorrectAnswer("modifié avec succès");
        service.updateQuestion(q);
        // Vérifier via findQuestionsByTest
        List<TestQuestion> list = service.findQuestionsByTest(mockTest.getId());
        boolean found = list.stream()
                .anyMatch(tq -> tq.getId().equals(q.getId())
                        && "modifié avec succès".equals(tq.getCorrectAnswer()));
        assertTrue(found);
    }

    @Test @Order(14)
    @DisplayName("UPDATE — sans id → exception")
    void testUpdate_SansId() {
        TestQuestion q = new TestQuestion();
        q.setMockTest(mockTest);
        q.setSectionCategory("Writing");
        q.setQuestionText("Write about your childhood memories and their influence on your values.");
        q.setPoints(3);
        assertThrows(IllegalArgumentException.class, () -> service.updateQuestion(q));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(15)
    @DisplayName("DELETE — suppression valide")
    void testDelete_Success() {
        TestQuestion q = creerQuestion("Listening",
                "According to the audio, what is the speaker main argument presented here?",
                null, null, 3);
        Long id = q.getId();
        service.deleteQuestion(id);
        // Vérifier que la question n'existe plus
        List<TestQuestion> list = service.findQuestionsByTest(mockTest.getId());
        boolean stillExists = list.stream().anyMatch(tq -> tq.getId().equals(id));
        assertFalse(stillExists);
    }

    @Test @Order(16)
    @DisplayName("DELETE — id null → exception")
    void testDelete_Null() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteQuestion(null));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private TestQuestion creerQuestion(String section, String text,
                                       String options, String answer, int points) {
        TestQuestion q = new TestQuestion();
        q.setMockTest(mockTest);
        q.setSectionCategory(section);
        q.setQuestionText(text);
        q.setOptions(options);
        q.setCorrectAnswer(answer);
        q.setPoints(points);
        q.setActive(true);
        service.createQuestion(q);
        return q;
    }
}