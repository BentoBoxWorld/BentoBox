package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeleteChunksEvent;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class, BentoBox.class, Util.class, Location.class })
public class IslandDeletionManagerTest {

    @Mock
    private BentoBox plugin;
    // Class under test
    private IslandDeletionManager idm;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private Island island;
    @Mock
    private PluginManager pim;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private IslandWorldManager iwm;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        Server server = mock(Server.class);
        when(server.getWorld(anyString())).thenReturn(world);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(server.getPluginManager()).thenReturn(pim);
        when(Bukkit.getScheduler()).thenReturn(scheduler);

        // Clear any remaining database
        clearDatabase();
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);
        when(s.getDatabaseType()).thenReturn(DatabaseType.JSON);
        // Location
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("bskyblock");
        // Island
        when(island.getCenter()).thenReturn(location);
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandDistance(any())).thenReturn(64);

        // Island Deletion Manager
        idm = new IslandDeletionManager(plugin);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        clearDatabase();
        Mockito.framework().clearInlineMocks();
    }


    private void clearDatabase() throws Exception {
        //remove any database data
        File file = new File("database");
        Path pathToBeDeleted = file.toPath();
        if (file.exists()) {
            Files.walk(pathToBeDeleted)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandDeletionManager#onBentoBoxReady(world.bentobox.bentobox.api.events.BentoBoxReadyEvent)}.
     */
    @Test
    public void testOnBentoBoxReadyNullWorld() {
        when(location.getWorld()).thenReturn(null);
        // Delete island on previous server operation
        testOnIslandDelete();
        // Start server
        BentoBoxReadyEvent e = new BentoBoxReadyEvent();
        idm.onBentoBoxReady(e);
        verify(plugin).log("There are 1 islands pending deletion.");
        verify(plugin).logError("Island queued for deletion refers to a non-existant game world. Skipping...");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandDeletionManager#onBentoBoxReady(world.bentobox.bentobox.api.events.BentoBoxReadyEvent)}.
     */
    @Test
    public void testOnBentoBoxReady() {
        // Delete island on previous server operation
        testOnIslandDelete();
        // Add world
        when(location.getWorld()).thenReturn(world);
        // Start server
        BentoBoxReadyEvent e = new BentoBoxReadyEvent();
        idm.onBentoBoxReady(e);
        verify(plugin).log("There are 1 islands pending deletion.");
        verify(plugin, never()).logError("Island queued for deletion refers to a non-existant game world. Skipping...");
        verify(plugin).log("Resuming deletion of island at bskyblock 0,0,0");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandDeletionManager#onIslandDelete(world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeleteChunksEvent)}.
     */
    @Test
    public void testOnIslandDelete() {
        // Delete some islands
        IslandDeleteChunksEvent e = mock(IslandDeleteChunksEvent.class);
        IslandDeletion id = new IslandDeletion(island);
        when(e.getDeletedIslandInfo()).thenReturn(id);
        when(e.getIsland()).thenReturn(island);

        idm.onIslandDelete(e);
        verify(e, times(2)).getDeletedIslandInfo();
        // Verify database save
        File file = new File("database", "IslandDeletion");
        assertTrue(file.exists());
        File entry = new File(file, id.getUniqueId() + ".json");
        assertTrue(entry.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandDeletionManager#onIslandDeleted(world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeletedEvent)}.
     */
    @Ignore("To do")
    @Test
    public void testOnIslandDeleted() {

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandDeletionManager#inDeletion(org.bukkit.Location)}.
     */
    @Test
    public void testInDeletion() {
        assertFalse(idm.inDeletion(location));
    }

}
