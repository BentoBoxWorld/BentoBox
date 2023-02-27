package world.bentobox.bentobox.database.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.managers.IslandWorldManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class IslandDeletionTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Island island;
    @Mock
    private Location location;
    private IslandDeletion id;
    @Mock
    private @Nullable WorldSettings ws;
    @Mock
    private UUID uuid;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Max range
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandDistance(any())).thenReturn(100);
        when(iwm.getWorldSettings(any())).thenReturn(ws);

        // Island
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(location);

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(1245);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(-5245);

        PowerMockito.mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(uuid);
        /*
         *         uniqueId = UUID.randomUUID().toString();
        location = island.getCenter();
        minX = location.getBlockX() - range;
        minXChunk =  minX >> 4;
        maxX = range + location.getBlockX();
        maxXChunk = maxX >> 4;
        minZ = location.getBlockZ() - range;
        minZChunk = minZ >> 4;
        maxZ = range + location.getBlockZ();
        maxZChunk = maxZ >> 4;
        box = BoundingBox.of(new Vector(minX, 0, minZ), new Vector(maxX, 255, maxZ));
         */
        id = new IslandDeletion(island);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#getLocation()}.
     */
    @Test
    public void testGetLocation() {
        assertEquals(location, id.getLocation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#getMaxXChunk()}.
     */
    @Test
    public void testGetMaxXChunk() {
        assertEquals(77, id.getMaxXChunk());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#getMaxZChunk()}.
     */
    @Test
    public void testGetMaxZChunk() {
        assertEquals(-328, id.getMaxZChunk());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#getMinXChunk()}.
     */
    @Test
    public void testGetMinXChunk() {
        assertEquals(77, id.getMinXChunk());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#getMinZChunk()}.
     */
    @Test
    public void testGetMinZChunk() {
        assertEquals(-328, id.getMinZChunk());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#getUniqueId()}.
     */
    @Test
    public void testGetUniqueId() {
        assertNotNull(id.getUniqueId());
        assertFalse(id.getUniqueId().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#getWorld()}.
     */
    @Test
    public void testGetWorld() {
        assertEquals(world, id.getWorld());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#getBox()}.
     */
    @Test
    public void testBox() {
        assertNull(id.getBox());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.IslandDeletion#toString()}.
     */
    @Test
    public void testToString() {
        assertTrue(id.toString().endsWith(
                "minXChunk=77, maxXChunk=77, minZChunk=-328,"
                        + " maxZChunk=-328, minX=1245, minZ=-5245, maxX=1245, maxZ=-5245, box=null]"));
    }

}
