package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.event.world.StructureGrowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.lists.Flags;

class PodzolListenerTest extends CommonTestSetup {

    private PodzolListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(iwm.inWorld(any(World.class))).thenReturn(true);

        // Default: podzol generation is NOT allowed
        Flags.PODZOL.setDefaultSetting(false);

        listener = new PodzolListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnTreeGrowOutsideIslandCancelled() {
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        List<BlockState> blocks = new ArrayList<>();
        StructureGrowEvent e = new StructureGrowEvent(location, TreeType.BIG_TREE, false, mockPlayer, blocks);
        listener.onTreeGrow(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnTreeGrowInsideIslandPodzolRemoved() {
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(island));
        List<BlockState> blocks = new ArrayList<>();
        BlockState podzol = mock(BlockState.class);
        when(podzol.getType()).thenReturn(Material.PODZOL);
        BlockState log = mock(BlockState.class);
        when(log.getType()).thenReturn(Material.OAK_LOG);
        blocks.add(podzol);
        blocks.add(log);
        StructureGrowEvent e = new StructureGrowEvent(location, TreeType.BIG_TREE, false, mockPlayer, blocks);
        listener.onTreeGrow(e);
        assertFalse(e.isCancelled());
        // Podzol should be removed, log should remain
        assertFalse(blocks.stream().anyMatch(b -> b.getType() == Material.PODZOL));
        assertTrue(blocks.stream().anyMatch(b -> b.getType() == Material.OAK_LOG));
    }

    @Test
    void testOnTreeGrowFlagAllowed() {
        Flags.PODZOL.setDefaultSetting(true);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        List<BlockState> blocks = new ArrayList<>();
        StructureGrowEvent e = new StructureGrowEvent(location, TreeType.BIG_TREE, false, mockPlayer, blocks);
        listener.onTreeGrow(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnTreeGrowNotInWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        List<BlockState> blocks = new ArrayList<>();
        StructureGrowEvent e = new StructureGrowEvent(location, TreeType.BIG_TREE, false, mockPlayer, blocks);
        listener.onTreeGrow(e);
        assertFalse(e.isCancelled());
    }
}
