package world.bentobox.bentobox.api.addons;

import static org.junit.Assert.*;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class AddonDescriptionTest {

    private @NonNull AddonDescription ad;
    private ConfigurationSection configSec;

    /**
     */
    @Before
    public void setUp() throws Exception {
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
    public void testGetName() {
        assertEquals("name", ad.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getMain()}.
     */
    @Test
    public void testGetMain() {
        assertEquals("main", ad.getMain());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getVersion()}.
     */
    @Test
    public void testGetVersion() {
        assertEquals("version", ad.getVersion());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getDescription()}.
     */
    @Test
    public void testGetDescription() {
        assertEquals("description", ad.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getAuthors()}.
     */
    @Test
    public void testGetAuthors() {
        assertEquals("tastybento", ad.getAuthors().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getDependencies()}.
     */
    @Test
    public void testGetDependencies() {
        assertEquals("dep1", ad.getDependencies().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getSoftDependencies()}.
     */
    @Test
    public void testGetSoftDependencies() {
        assertEquals("sdep1", ad.getSoftDependencies().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#isMetrics()}.
     */
    @Test
    public void testIsMetrics() {
        assertTrue(ad.isMetrics());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getRepository()}.
     */
    @Test
    public void testGetRepository() {
        assertEquals("repo", ad.getRepository());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getIcon()}.
     */
    @Test
    public void testGetIcon() {
        assertEquals(Material.ACACIA_BOAT, ad.getIcon());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getApiVersion()}.
     */
    @Test
    public void testGetApiVersion() {
        assertEquals("api", ad.getApiVersion());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#getPermissions()}.
     */
    @Test
    public void testGetPermissions() {
        assertEquals(configSec, ad.getPermissions());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonDescription#toString()}.
     */
    @Test
    public void testToString() {
        assertEquals("AddonDescription [name=name, version=version]", ad.toString());
    }

}
