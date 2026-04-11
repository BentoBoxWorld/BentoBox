package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.web.catalog.CatalogEntry;
import world.bentobox.bentobox.web.credits.Contributor;

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
}
