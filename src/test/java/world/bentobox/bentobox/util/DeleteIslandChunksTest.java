/**
 *
 */
package world.bentobox.bentobox.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;

/**
 * Tests the island delete class
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class})
public class DeleteIslandChunksTest {

    private BentoBox plugin;
    private Island island;
    private Location location;
    private World world;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class);
        Server server = mock(Server.class);
        PluginManager pim = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pim);
        when(Bukkit.getServer()).thenReturn(server);
        plugin = mock(BentoBox.class);
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        // No Nether or End by default
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(false);
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(false);
        when(iwm.isEndGenerate(Mockito.any())).thenReturn(false);
        when(iwm.isEndIslands(Mockito.any())).thenReturn(false);

        when(plugin.getIWM()).thenReturn(iwm);
        // World
        //world = mock(World.class, Mockito.withSettings().verboseLogging());
        world = mock(World.class);

        island = new Island();
        island.setRange(64);

        location = mock(Location.class);

        when(location.getWorld()).thenReturn(world);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.DeleteIslandChunks#DeleteIslandChunks(world.bentobox.bentobox.BentoBox, world.bentobox.bentobox.database.objects.Island)}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeleteIslandChunksNegativeX() {

        // Island adjacent to an island at 0,0
        Location location2 = mock(Location.class);

        when(location2.getWorld()).thenReturn(world);
        when(location2.getBlockX()).thenReturn(-128);
        when(location2.getBlockY()).thenReturn(120);
        when(location2.getBlockZ()).thenReturn(0);
        island.setCenter(location2);

        new DeleteIslandChunks(plugin, island);
        Mockito.verify(world, Mockito.times(64)).regenerateChunk(Mockito.anyInt(), Mockito.anyInt());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.DeleteIslandChunks#DeleteIslandChunks(world.bentobox.bentobox.BentoBox, world.bentobox.bentobox.database.objects.Island)}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeleteIslandChunksNegativeXX() {

        // Island adjacent to an island at 0,0
        Location location2 = mock(Location.class);

        when(location2.getWorld()).thenReturn(world);
        when(location2.getBlockX()).thenReturn(-256);
        when(location2.getBlockY()).thenReturn(120);
        when(location2.getBlockZ()).thenReturn(0);
        island.setCenter(location2);

        new DeleteIslandChunks(plugin, island);
        Mockito.verify(world, Mockito.times(64)).regenerateChunk(Mockito.anyInt(), Mockito.anyInt());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.DeleteIslandChunks#DeleteIslandChunks(world.bentobox.bentobox.BentoBox, world.bentobox.bentobox.database.objects.Island)}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeleteIslandChunksIslandPositiveX() {

        // Island adjacent to an island at 0,0
        Location location2 = mock(Location.class);

        when(location2.getWorld()).thenReturn(world);
        when(location2.getBlockX()).thenReturn(0);
        when(location2.getBlockY()).thenReturn(120);
        when(location2.getBlockZ()).thenReturn(0);
        island.setCenter(location2);

        new DeleteIslandChunks(plugin, island);
        Mockito.verify(world, Mockito.times(64)).regenerateChunk(Mockito.anyInt(), Mockito.anyInt());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.DeleteIslandChunks#DeleteIslandChunks(world.bentobox.bentobox.BentoBox, world.bentobox.bentobox.database.objects.Island)}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeleteIslandChunksPositiveXX() {

        // Island adjacent to an island at 0,0
        Location location2 = mock(Location.class);

        when(location2.getWorld()).thenReturn(world);
        when(location2.getBlockX()).thenReturn(256);
        when(location2.getBlockY()).thenReturn(120);
        when(location2.getBlockZ()).thenReturn(0);
        island.setCenter(location2);

        new DeleteIslandChunks(plugin, island);
        Mockito.verify(world, Mockito.times(64)).regenerateChunk(Mockito.anyInt(), Mockito.anyInt());
    }

}
