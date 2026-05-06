package org.example.tests;

import org.example.entity.tests.MockTest;
import org.example.entity.tests.PlatformLanguage;
import org.example.service.tests.MockTestService;
import org.example.util.MyDataBase;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MockTestServiceTest {

    private static MockTestService service;
    private static PlatformLanguage langue;
    private static Long testIdToCleanup = null;

    @BeforeAll
    static void setUp() {
        // Initialiser la connexion JDBC
        MyDataBase.getInstance();
        service = new MockTestService();

        // Charger la première langue disponible
        try {
            Connection conn = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, code FROM platform_language WHERE is_enabled = 1 LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                langue = new PlatformLanguage();
                langue.setId(rs.getLong("id"));
                langue.setName(rs.getString("name"));
                langue.setCode(rs.getString("code"));
                langue.setEnabled(true);
            }
            rs.close(); ps.close();
        } catch (Exception e) {
            fail("Impossible de charger la langue : " + e.getMessage());
        }
        assertNotNull(langue, "platform_language doit contenir au moins 1 ligne.");
    }

    @AfterEach
    void cleanup() {
        // Nettoyer les tests créés après chaque test
        if (testIdToCleanup != null) {
            try {
                Connection conn = MyDataBase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM mock_test WHERE id = ?");
                ps.setLong(1, testIdToCleanup);
                ps.executeUpdate();
                ps.close();
            } catch (Exception ignored) {}
            testIdToCleanup = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("CREATE — test valide persisté avec succès")
    void testCreate_Success() {
        MockTest t = creerTest("TOEFL", "Mon test TOEFL B2 complet", "B2", 120);
        assertNotNull(t.getId());
        assertTrue(t.getId() > 0);
        testIdToCleanup = t.getId();
    }

    @Test @Order(2)
    @DisplayName("CREATE — titre vide → exception")
    void testCreate_TitreVide() {
        MockTest t = new MockTest();
        t.setTitle("   "); t.setTestType("IELTS");
        t.setLevel("C1"); t.setDurationMinutes(90);
        t.setPlatformLanguage(langue);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(t));
        assertTrue(ex.getMessage().toLowerCase().contains("titre"));
    }

    @Test @Order(3)
    @DisplayName("CREATE — titre < 5 caractères → exception")
    void testCreate_TitreTropCourt() {
        MockTest t = new MockTest();
        t.setTitle("AB"); t.setTestType("DELF");
        t.setLevel("B1"); t.setDurationMinutes(90);
        t.setPlatformLanguage(langue);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(t));
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test @Order(4)
    @DisplayName("CREATE — type invalide → exception")
    void testCreate_TypeInvalide() {
        MockTest t = new MockTest();
        t.setTitle("Test type inconnu"); t.setTestType("GMAT");
        t.setLevel("B2"); t.setDurationMinutes(90);
        t.setPlatformLanguage(langue);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(t));
        assertTrue(ex.getMessage().contains("TOEFL"));
    }

    @Test @Order(5)
    @DisplayName("CREATE — durée < 30 → exception")
    void testCreate_DureeTropCourte() {
        MockTest t = new MockTest();
        t.setTitle("Test durée invalide"); t.setTestType("TCF");
        t.setLevel("B1"); t.setDurationMinutes(15);
        t.setPlatformLanguage(langue);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(t));
        assertTrue(ex.getMessage().contains("30"));
    }

    @Test @Order(6)
    @DisplayName("CREATE — durée > 300 → exception")
    void testCreate_DureeTropLongue() {
        MockTest t = new MockTest();
        t.setTitle("Test durée trop longue"); t.setTestType("IELTS");
        t.setLevel("C1"); t.setDurationMinutes(500);
        t.setPlatformLanguage(langue);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(t));
        assertTrue(ex.getMessage().contains("300"));
    }

    @Test @Order(7)
    @DisplayName("CREATE — langue null → exception")
    void testCreate_LangueNull() {
        MockTest t = new MockTest();
        t.setTitle("Test sans langue"); t.setTestType("DALF");
        t.setLevel("C2"); t.setDurationMinutes(90);
        t.setPlatformLanguage(null);
        assertThrows(IllegalArgumentException.class, () -> service.create(t));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(8)
    @DisplayName("READ — findAll retourne liste non vide")
    void testFindAll() {
        List<MockTest> list = service.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
    }

    @Test @Order(9)
    @DisplayName("READ — findById id inexistant → exception")
    void testFindById_Inexistant() {
        assertThrows(Exception.class, () -> service.findById(99999L));
    }

    @Test @Order(10)
    @DisplayName("READ — findById id null → exception")
    void testFindById_Null() {
        assertThrows(IllegalArgumentException.class, () -> service.findById(null));
    }

    @Test @Order(11)
    @DisplayName("READ — filterByTestType filtre correctement")
    void testFilterByTestType() {
        List<MockTest> list = service.filterByTestType("TOEFL");
        assertNotNull(list);
        list.forEach(t -> assertEquals("TOEFL", t.getTestType()));
    }

    @Test @Order(12)
    @DisplayName("READ — search retourne résultats non nuls")
    void testSearch() {
        List<MockTest> result = service.search("test");
        assertNotNull(result);
    }

    @Test @Order(13)
    @DisplayName("READ — filterAdvanced combinaison type+niveau")
    void testFilterAdvanced() {
        List<MockTest> list = service.filterAdvanced("TOEFL", "B2", null);
        assertNotNull(list);
        list.forEach(t -> {
            assertEquals("TOEFL", t.getTestType());
            assertEquals("B2", t.getLevel());
        });
    }

    @Test @Order(14)
    @DisplayName("READ — findAllLanguages retourne liste non vide")
    void testFindAllLanguages() {
        List<PlatformLanguage> langs = service.findAllLanguages();
        assertNotNull(langs);
        assertFalse(langs.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRI
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(15)
    @DisplayName("TRI — sortByDurationAsc ordre croissant")
    void testSortByDurationAsc() {
        List<MockTest> list = service.sortByDurationAsc();
        assertNotNull(list);
        for (int i = 1; i < list.size(); i++)
            assertTrue(list.get(i-1).getDurationMinutes() <= list.get(i).getDurationMinutes());
    }

    @Test @Order(16)
    @DisplayName("TRI — sortByDurationDesc ordre décroissant")
    void testSortByDurationDesc() {
        List<MockTest> list = service.sortByDurationDesc();
        assertNotNull(list);
        for (int i = 1; i < list.size(); i++)
            assertTrue(list.get(i-1).getDurationMinutes() >= list.get(i).getDurationMinutes());
    }

    @Test @Order(17)
    @DisplayName("TRI — sortByTitleAsc ordre alphabétique")
    void testSortByTitleAsc() {
        List<MockTest> list = service.sortByTitleAsc();
        assertNotNull(list);
        for (int i = 1; i < list.size(); i++)
            assertTrue(list.get(i-1).getTitle().compareToIgnoreCase(list.get(i).getTitle()) <= 0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(18)
    @DisplayName("STATS — countAll >= 0")
    void testCountAll() { assertTrue(service.countAll() >= 0); }

    @Test @Order(19)
    @DisplayName("STATS — countByTestType retourne map non nulle")
    void testCountByTestType() {
        Map<String, Long> map = service.countByTestType();
        assertNotNull(map);
    }

    @Test @Order(20)
    @DisplayName("STATS — countByLevel retourne map non nulle")
    void testCountByLevel() { assertNotNull(service.countByLevel()); }

    @Test @Order(21)
    @DisplayName("STATS — averageDuration >= 0")
    void testAverageDuration() { assertTrue(service.averageDuration() >= 0); }

    @Test @Order(22)
    @DisplayName("STATS — countAllQuestions >= 0")
    void testCountAllQuestions() { assertTrue(service.countAllQuestions() >= 0); }

    @Test @Order(23)
    @DisplayName("STATS — countAllResults >= 0")
    void testCountAllResults() { assertTrue(service.countAllResults() >= 0); }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(24)
    @DisplayName("UPDATE — modification valide sauvegardée")
    void testUpdate_Success() {
        MockTest t = creerTest("Cambridge", "Test Cambridge update valide", "C1", 180);
        testIdToCleanup = t.getId();
        t.setTitle("Titre modifié avec succès");
        service.update(t);
        MockTest updated = service.findById(t.getId());
        assertEquals("Titre modifié avec succès", updated.getTitle());
    }

    @Test @Order(25)
    @DisplayName("UPDATE — sans id → exception")
    void testUpdate_SansId() {
        MockTest t = new MockTest();
        t.setTitle("Sans identifiant test"); t.setTestType("IELTS");
        t.setLevel("C2"); t.setDurationMinutes(120);
        t.setPlatformLanguage(langue);
        assertThrows(IllegalArgumentException.class, () -> service.update(t));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(26)
    @DisplayName("DELETE — suppression valide")
    void testDelete_Success() {
        MockTest t = creerTest("DALF", "Test DALF à supprimer valide", "C2", 210);
        Long id = t.getId();
        service.delete(id);
        assertThrows(Exception.class, () -> service.findById(id));
    }

    @Test @Order(27)
    @DisplayName("DELETE — id null → exception")
    void testDelete_Null() {
        assertThrows(IllegalArgumentException.class, () -> service.delete(null));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private MockTest creerTest(String type, String title, String level, int duration) {
        MockTest t = new MockTest();
        t.setTestType(type);
        t.setTitle(title);
        t.setLevel(level);
        t.setDurationMinutes(duration);
        t.setPlatformLanguage(langue);
        t.setActive(true);
        service.create(t);
        return t;
    }
}