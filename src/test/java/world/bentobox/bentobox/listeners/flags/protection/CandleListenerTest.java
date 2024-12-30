package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Flags.class, Util.class, Bukkit.class , ServerBuildInfo.class})
public class CandleListenerTest extends AbstractCommonSetup {

    private CandleListener l;
    @Mock
    private Block block;

    /**
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Island manager
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        when(block.getLocation()).thenReturn(location);

        // Tags
        when(Tag.CANDLES.isTagged(any(Material.class))).thenReturn(true);
        when(Tag.CANDLE_CAKES.isTagged(any(Material.class))).thenReturn(true);

        // Listener
        l = new CandleListener();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.CandleListener#onCandleInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnCandleInteract() {
        // Block
        when(block.getType()).thenReturn(Material.CANDLE);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        l.onCandleInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.CandleListener#onCandleInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnCandleCakeInteract() {
        // Block
        when(block.getType()).thenReturn(Material.CANDLE_CAKE);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        l.onCandleInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.CandleListener#onCandleInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnCandleInteractFail() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        // Block
        when(block.getType()).thenReturn(Material.CANDLE);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        l.onCandleInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.CandleListener#onCandleInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnCandleCakeInteractFail() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        // Block
        when(block.getType()).thenReturn(Material.CANDLE_CAKE);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        l.onCandleInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

}
