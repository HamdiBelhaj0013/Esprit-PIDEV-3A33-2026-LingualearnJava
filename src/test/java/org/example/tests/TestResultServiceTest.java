package org.example.tests;

import org.example.entity.User;
import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestResult;
import org.example.service.tests.TestResultService;
import org.example.util.MyDataBase;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestResultServiceTest {

    private static TestResultService service;
    private static User              user;
    private static MockTest          mockTest;
    private static Long              resultIdToCleanup = null;

    @BeforeAll
    static void setUp() {
        MyDataBase.getInstance();
        service = new TestResultService();

        // Charger le premier utilisateur JDBC
        try {
            Connection conn = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, first_name, last_name, email FROM users LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
            }
            rs.close(); ps.close();
        } catch (Exception e) {
            fail("Impossible de charger l'utilisateur : " + e.getMessage());
        }

        // Charger le premier test JDBC
        try {
            Connection conn = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, title, test_type FROM mock_test LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                mockTest = new MockTest();
                mockTest.setId(rs.getLong("id"));
                mockTest.setTitle(rs.getString("title"));
                mockTest.setTestType(rs.getString("test_type"));
            }
            rs.close(); ps.close();
        } catch (Exception e) {
            fail("Impossible de charger le test : " + e.getMessage());
        }

        assertNotNull(user,     "La table users doit contenir au moins 1 ligne.");
        assertNotNull(mockTest, "La table mock_test doit contenir au moins 1 ligne.");
    }

    @AfterEach
    void cleanup() {
        if (resultIdToCleanup != null) {
            try {
                Connection conn = MyDataBase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM test_result WHERE id = ?");
                ps.setLong(1, resultIdToCleanup);
                ps.executeUpdate();
                ps.close();
            } catch (Exception ignored) {}
            resultIdToCleanup = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("CREATE — résultat valide persisté")
    void testCreate_Success() {
        TestResult r = creerResultat(75.5f, 78.0f);
        assertNotNull(r.getId());
        assertTrue(r.getId() > 0);
        resultIdToCleanup = r.getId();
    }

    @Test @Order(2)
    @DisplayName("CREATE — score négatif → exception")
    void testCreate_ScoreNegatif() {
        TestResult r = new TestResult();
        r.setUser(user); r.setMockTest(mockTest);
        r.setOverallScore(-5f);
        r.setDateTaken(LocalDateTime.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(r));
        assertTrue(ex.getMessage().contains("0"));
    }

    @Test @Order(3)
    @DisplayName("CREATE — score > 100 → exception")
    void testCreate_ScoreTropEleve() {
        TestResult r = new TestResult();
        r.setUser(user); r.setMockTest(mockTest);
        r.setOverallScore(150f);
        r.setDateTaken(LocalDateTime.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(r));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test @Order(4)
    @DisplayName("CREATE — user null → exception")
    void testCreate_UserNull() {
        TestResult r = new TestResult();
        r.setUser(null); r.setMockTest(mockTest);
        r.setOverallScore(80f);
        r.setDateTaken(LocalDateTime.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(r));
        assertTrue(ex.getMessage().contains("utilisateur"));
    }

    @Test @Order(5)
    @DisplayName("CREATE — test null → exception")
    void testCreate_TestNull() {
        TestResult r = new TestResult();
        r.setUser(user); r.setMockTest(null);
        r.setOverallScore(80f);
        r.setDateTaken(LocalDateTime.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(r));
        assertTrue(ex.getMessage().contains("test"));
    }

    @Test @Order(6)
    @DisplayName("CREATE — score 0 accepté")
    void testCreate_ScoreZero() {
        TestResult r = new TestResult();
        r.setUser(user); r.setMockTest(mockTest);
        r.setOverallScore(0f);
        r.setDateTaken(LocalDateTime.now());
        assertDoesNotThrow(() -> service.create(r));
        resultIdToCleanup = r.getId();
    }

    @Test @Order(7)
    @DisplayName("CREATE — score 100 accepté")
    void testCreate_ScoreCent() {
        TestResult r = new TestResult();
        r.setUser(user); r.setMockTest(mockTest);
        r.setOverallScore(100f);
        r.setDateTaken(LocalDateTime.now());
        assertDoesNotThrow(() -> service.create(r));
        resultIdToCleanup = r.getId();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(8)
    @DisplayName("READ — findAll retourne liste non nulle")
    void testFindAll() { assertNotNull(service.findAll()); }

    @Test @Order(9)
    @DisplayName("READ — findByUserId filtre par user")
    void testFindByUserId() {
        List<TestResult> list = service.findByUserId(user.getId());
        assertNotNull(list);
        list.forEach(r -> assertEquals(user.getId(), r.getUser().getId()));
    }

    @Test @Order(10)
    @DisplayName("READ — findByMockTestId filtre par test")
    void testFindByMockTestId() {
        List<TestResult> list = service.findByMockTestId(mockTest.getId());
        assertNotNull(list);
        list.forEach(r -> assertEquals(mockTest.getId(), r.getMockTest().getId()));
    }

    @Test @Order(11)
    @DisplayName("READ — findById inexistant → exception")
    void testFindById_Inexistant() {
        assertThrows(Exception.class, () -> service.findById(99999L));
    }

    @Test @Order(12)
    @DisplayName("READ — filterByMinScore filtre correctement")
    void testFilterByMinScore() {
        List<TestResult> list = service.filterByMinScore(50f);
        assertNotNull(list);
        list.forEach(r -> assertTrue(r.getOverallScore() >= 50f));
    }

    @Test @Order(13)
    @DisplayName("READ — filterByScoreRange filtre entre deux scores")
    void testFilterByScoreRange() {
        List<TestResult> list = service.filterByScoreRange(40f, 80f);
        assertNotNull(list);
        list.forEach(r -> {
            assertTrue(r.getOverallScore() >= 40f);
            assertTrue(r.getOverallScore() <= 80f);
        });
    }

    @Test @Order(14)
    @DisplayName("READ — filterByScoreRange min > max → exception")
    void testFilterByScoreRange_MinSupMax() {
        assertThrows(IllegalArgumentException.class,
                () -> service.filterByScoreRange(80f, 40f));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRI
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(15)
    @DisplayName("TRI — sortByScoreDesc ordre décroissant")
    void testSortByScoreDesc() {
        List<TestResult> list = service.sortByScoreDesc();
        assertNotNull(list);
        for (int i = 1; i < list.size(); i++)
            assertTrue(list.get(i-1).getOverallScore() >= list.get(i).getOverallScore());
    }

    @Test @Order(16)
    @DisplayName("TRI — sortByScoreAsc ordre croissant")
    void testSortByScoreAsc() {
        List<TestResult> list = service.sortByScoreAsc();
        assertNotNull(list);
        for (int i = 1; i < list.size(); i++)
            assertTrue(list.get(i-1).getOverallScore() <= list.get(i).getOverallScore());
    }

    @Test @Order(17)
    @DisplayName("TRI — sortByDateDesc plus récent en premier")
    void testSortByDateDesc() {
        List<TestResult> list = service.sortByDateDesc();
        assertNotNull(list);
        for (int i = 1; i < list.size(); i++)
            assertFalse(list.get(i-1).getDateTaken().isBefore(list.get(i).getDateTaken()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(18)
    @DisplayName("STATS — countAll >= 0")
    void testCountAll() { assertTrue(service.countAll() >= 0); }

    @Test @Order(19)
    @DisplayName("STATS — averageScore >= 0")
    void testAverageScore() { assertTrue(service.averageScore() >= 0); }

    @Test @Order(20)
    @DisplayName("STATS — averageScoreByMockTestId >= 0")
    void testAverageByTest() {
        assertTrue(service.averageScoreByMockTestId(mockTest.getId()) >= 0);
    }

    @Test @Order(21)
    @DisplayName("STATS — averageScoreByUserId >= 0")
    void testAverageByUser() {
        assertTrue(service.averageScoreByUserId(user.getId()) >= 0);
    }

    @Test @Order(22)
    @DisplayName("STATS — maxScoreByMockTestId >= 0")
    void testMaxScore() {
        assertTrue(service.maxScoreByMockTestId(mockTest.getId()) >= 0);
    }

    @Test @Order(23)
    @DisplayName("STATS — minScoreByMockTestId >= 0")
    void testMinScore() {
        assertTrue(service.minScoreByMockTestId(mockTest.getId()) >= 0);
    }

    @Test @Order(24)
    @DisplayName("STATS — countByMockTest retourne map non nulle")
    void testCountByMockTest() { assertNotNull(service.countByMockTest()); }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(25)
    @DisplayName("UPDATE — modification du score sauvegardée")
    void testUpdate_Success() {
        TestResult r = creerResultat(60f, 65f);
        resultIdToCleanup = r.getId();
        r.setOverallScore(88f);
        service.update(r);
        TestResult updated = service.findById(r.getId());
        assertEquals(88f, updated.getOverallScore(), 0.01f);
    }

    @Test @Order(26)
    @DisplayName("UPDATE — sans id → exception")
    void testUpdate_SansId() {
        TestResult r = new TestResult();
        r.setUser(user); r.setMockTest(mockTest);
        r.setOverallScore(70f);
        assertThrows(IllegalArgumentException.class, () -> service.update(r));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(27)
    @DisplayName("DELETE — suppression valide")
    void testDelete_Success() {
        TestResult r = creerResultat(55f, 60f);
        Long id = r.getId();
        service.delete(id);
        assertThrows(Exception.class, () -> service.findById(id));
    }

    @Test @Order(28)
    @DisplayName("DELETE — id null → exception")
    void testDelete_Null() {
        assertThrows(IllegalArgumentException.class, () -> service.delete(null));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private TestResult creerResultat(float score, float aiScore) {
        TestResult r = new TestResult();
        r.setUser(user);
        r.setMockTest(mockTest);
        r.setOverallScore(score);
        r.setAiPredictedScore(aiScore);
        r.setDateTaken(LocalDateTime.now());
        service.create(r);
        return r;
    }
}