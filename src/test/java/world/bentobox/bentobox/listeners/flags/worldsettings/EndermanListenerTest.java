package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests enderman related listeners
 * @author tastybento
 *
 */
public class EndermanListenerTest extends CommonTestSetup {

    private Enderman enderman;
    private Slime slime;
    private BlockData bd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
         when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        Mockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Worlds
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
 
        // Monsters and animals
        enderman = mock(Enderman.class);
        when(enderman.getLocation()).thenReturn(location);
        when(enderman.getWorld()).thenReturn(world);
        bd = mock(BlockData.class);
        when(bd.getMaterial()).thenReturn(Material.STONE);
        when(enderman.getCarriedBlock()).thenReturn(bd);
        slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

         mockedUtil.when(() -> Util.getWorld(Mockito.any())).thenReturn(mock(World.class));
        // Not allowed to start
        Flags.ENDERMAN_GRIEFING.setSetting(world, false);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link EndermanListener#onEndermanGrief(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testNotEnderman() {
        EndermanListener listener = new EndermanListener();
        Block to = mock(Block.class);
        Material block = Material.ACACIA_DOOR;
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(slime, to, block.createBlockData());
        listener.onEndermanGrief(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link EndermanListener#onEndermanGrief(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testOnEndermanGriefWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EndermanListener listener = new EndermanListener();
        Block to = mock(Block.class);
        Material block = Material.ACACIA_DOOR;
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(enderman, to, block.createBlockData());
        listener.onEndermanGrief(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link EndermanListener#onEndermanGrief(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testOnEndermanGriefAllowed() {
        Flags.ENDERMAN_GRIEFING.setSetting(world, true);
        EndermanListener listener = new EndermanListener();
        Block to = mock(Block.class);
        Material block = Material.ACACIA_DOOR;
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(enderman, to, block.createBlockData());
        listener.onEndermanGrief(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link EndermanListener#onEndermanGrief(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testOnEndermanGrief() {
        EndermanListener listener = new EndermanListener();
        Block to = mock(Block.class);
        Material block = Material.ACACIA_DOOR;
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(enderman, to, block.createBlockData());
        listener.onEndermanGrief(e);
        assertTrue(e.isCancelled());
    }

}
