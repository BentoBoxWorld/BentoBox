package world.bentobox.bentobox.managers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
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
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.util.DeleteIslandChunks;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Bukkit.class, DeleteIslandChunks.class})
public class IslandChunkDeletionManagerTest {

    @Mock
    private BentoBox plugin;
    private IslandChunkDeletionManager icdm;
    @Mock
    private DeleteIslandChunks dic;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandDeletion id;

    private Settings settings;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);

        // Scheduler
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        
        // DeleteIslandChunks
        PowerMockito.whenNew(DeleteIslandChunks.class).withAnyArguments().thenReturn(dic);
        

        settings = new Settings();
        settings.setSlowDeletion(true);
        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        icdm = new IslandChunkDeletionManager(plugin);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandChunkDeletionManager#IslandChunkDeletionManager(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testIslandChunkDeletionManager() {        
        PowerMockito.verifyStatic(Bukkit.class, times(1));
        Bukkit.getScheduler();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandChunkDeletionManager#run()}.
     */
    @Test
    public void testRun() {
        icdm.add(id);
        icdm.run();
        verify(id, times(3)).getWorld();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandChunkDeletionManager#add(world.bentobox.bentobox.database.objects.IslandDeletion)}.
     */
    @Test
    public void testAdd() {
        settings.setSlowDeletion(false);
        icdm = new IslandChunkDeletionManager(plugin);
        icdm.add(id);
        verify(id, times(3)).getWorld();
    }

}
