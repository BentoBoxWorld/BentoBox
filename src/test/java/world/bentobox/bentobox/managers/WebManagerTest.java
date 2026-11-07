package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.web.catalog.CatalogEntry;
import world.bentobox.bentobox.web.credits.Contributor;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.lang.reflect.Method;
import java.util.Iterator;

class WebManagerTest extends CommonTestSetup {

    private WebManager wm;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Disable GitHub data download so the constructor doesn't make HTTP calls
        plugin.getSettings().setGithubDownloadData(false);
        wm = new WebManager(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ---- Constructor ----

    @Test
    void testConstructorNoGitHub() {
        assertNotNull(wm);
        assertFalse(wm.getGitHub().isPresent());
    }

    // ---- Getters (empty state) ----

    @Test
    void testGetAddonsCatalogEmpty() {
        List<CatalogEntry> catalog = wm.getAddonsCatalog();
        assertNotNull(catalog);
        assertTrue(catalog.isEmpty());
    }

    @Test
    void testGetGamemodesCatalogEmpty() {
        List<CatalogEntry> catalog = wm.getGamemodesCatalog();
        assertNotNull(catalog);
        assertTrue(catalog.isEmpty());
    }

    @Test
    void testGetContributorsEmpty() {
        List<Contributor> contributors = wm.getContributors("BentoBoxWorld/BentoBox");
        assertNotNull(contributors);
        assertTrue(contributors.isEmpty());
    }

    @Test
    void testGetContributorsUnknownRepo() {
        List<Contributor> contributors = wm.getContributors("unknown/repo");
        assertNotNull(contributors);
        assertTrue(contributors.isEmpty());
    }

    @Test
    void testGetGitHubNotPresent() {
        assertFalse(wm.getGitHub().isPresent());
    }

    // ---- isNewerVersion ----

    @Test
    void testIsNewerVersion_newerMinor() {
        assertTrue(WebManager.isNewerVersion("3.11.2", "3.12.0"));
    }

    @Test
    void testIsNewerVersion_newerPatch() {
        assertTrue(WebManager.isNewerVersion("3.11.2", "3.11.3"));
    }

    @Test
    void testIsNewerVersion_equalVersions() {
        assertFalse(WebManager.isNewerVersion("3.11.2", "3.11.2"));
    }

    @Test
    void testIsNewerVersion_olderTag() {
        assertFalse(WebManager.isNewerVersion("3.11.2", "3.11.0"));
    }

    @Test
    void testIsNewerVersion_stripsSnapshotSuffix() {
        assertFalse(WebManager.isNewerVersion("3.11.2-b123-SNAPSHOT", "3.11.2"));
    }

    @Test
    void testIsNewerVersion_snapshotOlderThanNewer() {
        assertTrue(WebManager.isNewerVersion("3.11.2-b123-SNAPSHOT", "3.12.0"));
    }

    @Test
    void testIsNewerVersion_majorBump() {
        assertTrue(WebManager.isNewerVersion("3.11.2", "4.0.0"));
    }

    @Test
    void testIsNewerVersion_stripsLeadingV() {
        assertTrue(WebManager.isNewerVersion("3.11.2", "v3.12.0"));
    }

    @Test
    void testIsNewerVersion_differentLengths() {
        assertTrue(WebManager.isNewerVersion("3.11", "3.11.1"));
        assertFalse(WebManager.isNewerVersion("3.11.1", "3.11"));
    }

    @Test
    void testIsNewerVersion_singleDigit() {
        assertTrue(WebManager.isNewerVersion("3", "4"));
        assertFalse(WebManager.isNewerVersion("4", "3"));
    }

    @Test
    void testIsNewerVersion_majorOlderMinorNewer() {
        // 4.0.0 vs 3.99.99 - major takes precedence
        assertFalse(WebManager.isNewerVersion("4.0.0", "3.99.99"));
    }

    // ---- requestGitHubData with no GitHub ----

    @Test
    void testRequestGitHubDataNoGitHub() {
        // Should not throw when gitHub is not present
        wm.requestGitHubData();
    }

    // ---- Catalog refresh is a snapshot swap, not an in-place clear/refill ----

    private void parseCatalog(String json) throws Exception {
        Method m = WebManager.class.getDeclaredMethod("parseCatalogContent", String.class, String.class,
                String.class);
        m.setAccessible(true);
        m.invoke(wm, "", "", json);
    }

    private static final String CATALOG_ONE = """
            {"gamemodes":[{"slot":0,"icon":"GRASS_BLOCK","description":"d","name":"GM1","repository":"BentoBoxWorld/GM1","topic":"Gamemode","tag":"BentoBoxWorld/GM1@1"}],
             "addons":[{"slot":0,"icon":"CHEST","description":"d","name":"A1","repository":"BentoBoxWorld/A1","topic":"Addon","tag":"BentoBoxWorld/A1@1"}]}
            """;
    private static final String CATALOG_TWO = """
            {"gamemodes":[{"slot":0,"icon":"GRASS_BLOCK","description":"d","name":"GM1","repository":"BentoBoxWorld/GM1","topic":"Gamemode","tag":"BentoBoxWorld/GM1@1"},
                          {"slot":1,"icon":"STONE","description":"d","name":"GM2","repository":"BentoBoxWorld/GM2","topic":"Gamemode","tag":"BentoBoxWorld/GM2@1"}],
             "addons":[]}
            """;

    @Test
    void testCatalogRefreshDoesNotDisturbListHeldByReader() throws Exception {
        parseCatalog(CATALOG_ONE);
        List<CatalogEntry> heldByPanel = wm.getGamemodesCatalog();
        assertEquals(1, heldByPanel.size());
        // A panel is iterating the list on the main thread...
        Iterator<CatalogEntry> it = heldByPanel.iterator();

        // ...while the GitHub thread refreshes the catalog
        parseCatalog(CATALOG_TWO);

        // The reader's list is untouched and its iterator is still valid
        assertEquals(1, heldByPanel.size());
        assertNotNull(it.next());
        assertFalse(it.hasNext());
        // New readers see the complete new snapshot
        assertNotSame(heldByPanel, wm.getGamemodesCatalog());
        assertEquals(2, wm.getGamemodesCatalog().size());
        assertTrue(wm.getAddonsCatalog().isEmpty());
    }

    @Test
    void testCatalogGetterReturnsSameSnapshotUntilRefresh() throws Exception {
        parseCatalog(CATALOG_ONE);
        assertSame(wm.getAddonsCatalog(), wm.getAddonsCatalog());
        assertEquals(1, wm.getAddonsCatalog().size());
    }

}
