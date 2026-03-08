package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WebManagerTest {

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
}
