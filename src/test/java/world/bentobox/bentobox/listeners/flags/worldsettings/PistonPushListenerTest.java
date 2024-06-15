package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPistonExtendEvent;
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
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Util.class, Bukkit.class })
public class PistonPushListenerTest {

    @Mock
    private Island island;
    @Mock
    private World world;
    @Mock
    private Block block;
    private List<Block> blocks;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Owner
        UUID uuid = UUID.randomUUID();

        // Island initialization
        when(island.getOwner()).thenReturn(uuid);

        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        Location inside = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(opIsland);

        // Blocks
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(inside);

        Block blockPushed = mock(Block.class);

        when(block.getRelative(any(BlockFace.class))).thenReturn(blockPushed);

        // The blocks in the pushed list are all inside the island
        when(blockPushed.getLocation()).thenReturn(inside);
        when(blockPushed.getWorld()).thenReturn(world);

        // Make a list of ten blocks
        blocks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            blocks.add(block);
        }

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);
        when(Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // World Settings
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        GameModeAddon gma = mock(GameModeAddon.class);
        Optional<GameModeAddon> opGma = Optional.of(gma );
        when(iwm.getAddon(any())).thenReturn(opGma);
        when(iwm.inWorld(world)).thenReturn(true);

        // Set default on
        Flags.PISTON_PUSH.setSetting(world, true);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testOnPistonExtendFlagNotSet() {
        Flags.PISTON_PUSH.setSetting(world, false);
        BlockPistonExtendEvent e = new BlockPistonExtendEvent(block, blocks, BlockFace.EAST);
        new PistonPushListener().onPistonExtend(e);

        // Should fail because flag is not set
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnPistonExtendFlagSetOnIsland() {

        // The blocks in the pushed list are all inside the island
        when(island.onIsland(any())).thenReturn(true);

        BlockPistonExtendEvent e = new BlockPistonExtendEvent(block, blocks, BlockFace.EAST);
        new PistonPushListener().onPistonExtend(e);

        // Should fail because on island
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnPistonExtendFlagSetOffIsland() {
        // The blocks in the pushed list are all outside the island
        when(island.onIsland(any())).thenReturn(false);

        BlockPistonExtendEvent e = new BlockPistonExtendEvent(block, blocks, BlockFace.EAST);
        new PistonPushListener().onPistonExtend(e);

        // Should fail because on island
        assertTrue(e.isCancelled());
    }

}
