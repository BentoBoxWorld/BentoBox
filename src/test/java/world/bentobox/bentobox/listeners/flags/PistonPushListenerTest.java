package world.bentobox.bentobox.listeners.flags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class })
public class PistonPushListenerTest {

    private Island island;
    private World world;
    private Block block;
    private List<Block> blocks;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        
        // World
        world = mock(World.class);
        
        // Owner
        UUID uuid = UUID.randomUUID();
        
        // Island initialization
        island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);

        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);

        Location inside = mock(Location.class);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(opIsland);

        // Blocks
        block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(inside);

        Block blockPushed = mock(Block.class);
        
        when(block.getRelative(Mockito.any(BlockFace.class))).thenReturn(blockPushed);
        
        // The blocks in the pushed list are all inside the island
        when(blockPushed.getLocation()).thenReturn(inside);
        
        // Make a list of ten blocks
        blocks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            blocks.add(block);
        }

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);
                
        // World Settings
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        
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
        when(island.onIsland(Mockito.any())).thenReturn(true);
        
        BlockPistonExtendEvent e = new BlockPistonExtendEvent(block, blocks, BlockFace.EAST);
        new PistonPushListener().onPistonExtend(e);
        
        // Should fail because on island
        assertFalse(e.isCancelled());
    }
    
    @Test
    public void testOnPistonExtendFlagSetOffIsland() {
        // The blocks in the pushed list are all outside the island
        when(island.onIsland(Mockito.any())).thenReturn(false);
        
        BlockPistonExtendEvent e = new BlockPistonExtendEvent(block, blocks, BlockFace.EAST);
        new PistonPushListener().onPistonExtend(e);
        
        // Should fail because on island
        assertTrue(e.isCancelled());
    }

}
