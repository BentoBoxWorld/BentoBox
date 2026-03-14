package world.bentobox.bentobox.api.addons;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author tastybento
 *
 */

class AddonDescriptionTest {

    private @NonNull AddonDescription ad;
    private ConfigurationSection configSec;

    @BeforeEach
    void setUp() {
        configSec = new YamlConfiguration();
        ad = new AddonDescription.Builder("main", "name", "version")
                .apiVersion("api")
                .authors("tastybento", "poslovitch")
                .dependencies(List.of("dep1", "dep2"))
                .description("description")
                .icon(Material.ACACIA_BOAT)
                .metrics(true)
                .permissions(configSec)
                .repository("repo")
                .softDependencies(List.of("sdep1", "sdep2"))
                .build();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getName()}.
     */
    @Test
    void testGetName() {
        assertEquals("name", ad.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getMain()}.
     */
    @Test
    void testGetMain() {
        assertEquals("main", ad.getMain());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getVersion()}.
     */
    @Test
    void testGetVersion() {
        assertEquals("version", ad.getVersion());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getDescription()}.
     */
    @Test
    void testGetDescription() {
        assertEquals("description", ad.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getAuthors()}.
     */
    @Test
    void testGetAuthors() {
        assertEquals("tastybento", ad.getAuthors().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getDependencies()}.
     */
    @Test
    void testGetDependencies() {
        assertEquals("dep1", ad.getDependencies().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getSoftDependencies()}.
     */
    @Test
    void testGetSoftDependencies() {
        assertEquals("sdep1", ad.getSoftDependencies().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#isMetrics()}.
     */
    @Test
    void testIsMetrics() {
        assertTrue(ad.isMetrics());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getRepository()}.
     */
    @Test
    void testGetRepository() {
        assertEquals("repo", ad.getRepository());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getIcon()}.
     */
    @Test
    void testGetIcon() {
        assertEquals(Material.ACACIA_BOAT, ad.getIcon());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getApiVersion()}.
     */
    @Test
    void testGetApiVersion() {
        assertEquals("api", ad.getApiVersion());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getPermissions()}.
     */
    @Test
    void testGetPermissions() {
        assertEquals(configSec, ad.getPermissions());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#toString()}.
     */
    @Test
    void testToString() {
        assertEquals("AddonDescription [name=name, version=version]", ad.toString());
    }

}
