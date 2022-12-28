package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.protection.TestWorldSettings;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Bukkit.class})
public class CoarseDirtTillingListenerTest {

    @SuppressWarnings("deprecation")
    private static final List<Material> HOES = Collections.unmodifiableList(Arrays.stream(Material.values())
            .filter(m -> !m.isLegacy()).filter(m -> m.name().endsWith("_HOE")).toList());
    private static final List<Material> NOT_HOES = Collections.unmodifiableList(Arrays.stream(Material.values())
            .filter(m -> !m.name().endsWith("_HOE")).toList());

    // Class under test
    private CoarseDirtTillingListener ctl;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private World world;
    @Mock
    private Block clickedBlock;
    @Mock
    private Player player;
    @Mock
    private Notifier notifier;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        @Nullable
        WorldSettings worldSet = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSet);
        when(plugin.getIWM()).thenReturn(iwm);

        // Block
        when(clickedBlock.getWorld()).thenReturn(world);
        when(clickedBlock.getType()).thenReturn(Material.COARSE_DIRT);

        // Player
        User.setPlugin(plugin);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        User.getInstance(player);

        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);


        // Flag
        Flags.COARSE_DIRT_TILLING.setDefaultSetting(world, false);

        // Class under test
        ctl = new CoarseDirtTillingListener();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onTillingCoarseDirt(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnTillingCoarseDirtNotAllowed() {
        ItemStack itemStack = mock(ItemStack.class);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, itemStack, clickedBlock, BlockFace.UP);

        HOES.forEach(m -> {
            when(itemStack.getType()).thenReturn(m);
            ctl.onTillingCoarseDirt(e);
            assertEquals(Result.DENY, e.useInteractedBlock());
        });
        verify(notifier, times(HOES.size())).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onTillingCoarseDirt(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnTillingCoarseDirtAllowed() {
        // Flag
        Flags.COARSE_DIRT_TILLING.setDefaultSetting(world, true);
        ItemStack itemStack = mock(ItemStack.class);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, itemStack, clickedBlock, BlockFace.UP);
        HOES.forEach(m -> {
            when(itemStack.getType()).thenReturn(m);
            ctl.onTillingCoarseDirt(e);
            assertEquals(Result.ALLOW, e.useInteractedBlock());
        });
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onTillingCoarseDirt(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnTillingCoarseDirtNotHoe() {
        ItemStack itemStack = mock(ItemStack.class);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, itemStack, clickedBlock, BlockFace.UP);
        NOT_HOES.forEach(m -> {
            when(itemStack.getType()).thenReturn(m);
            ctl.onTillingCoarseDirt(e);
            assertEquals(Result.ALLOW, e.useInteractedBlock());
        });
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onTillingCoarseDirt(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnTillingCoarseDirtWrongAction() {
        ItemStack itemStack = mock(ItemStack.class);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, itemStack, clickedBlock, BlockFace.UP);
        ctl.onTillingCoarseDirt(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onTillingCoarseDirt(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnTillingCoarseDirtNullItem() {
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, clickedBlock, BlockFace.UP);
        ctl.onTillingCoarseDirt(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onTillingCoarseDirt(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnTillingCoarseDirtNotCoarseDirt() {
        when(clickedBlock.getType()).thenReturn(Material.DIRT);
        ItemStack itemStack = mock(ItemStack.class);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, itemStack, clickedBlock, BlockFace.UP);
        ctl.onTillingCoarseDirt(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onTillingCoarseDirt(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnTillingCoarseDirtWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        ItemStack itemStack = mock(ItemStack.class);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, itemStack, clickedBlock, BlockFace.UP);

        HOES.forEach(m -> {
            when(itemStack.getType()).thenReturn(m);
            ctl.onTillingCoarseDirt(e);
            assertEquals(Result.ALLOW, e.useInteractedBlock());
        });
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onBreakingPodzol(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBreakingPodzolNotPodzol() {
        BlockBreakEvent e = new BlockBreakEvent(clickedBlock, player);
        ctl.onBreakingPodzol(e);
        verify(clickedBlock, never()).setType(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onBreakingPodzol(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBreakingPodzol() {
        when(clickedBlock.getType()).thenReturn(Material.PODZOL);
        BlockBreakEvent e = new BlockBreakEvent(clickedBlock, player);
        ctl.onBreakingPodzol(e);
        verify(clickedBlock).setType(eq(Material.AIR));
        verify(world).dropItemNaturally(any(), any());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onBreakingPodzol(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBreakingPodzolWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(clickedBlock.getType()).thenReturn(Material.PODZOL);
        BlockBreakEvent e = new BlockBreakEvent(clickedBlock, player);
        ctl.onBreakingPodzol(e);
        verify(clickedBlock, never()).setType(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onBreakingPodzol(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBreakingPodzolCreative() {
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        when(clickedBlock.getType()).thenReturn(Material.PODZOL);
        BlockBreakEvent e = new BlockBreakEvent(clickedBlock, player);
        ctl.onBreakingPodzol(e);
        verify(clickedBlock, never()).setType(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener#onBreakingPodzol(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBreakingPodzolFlagAllowed() {
        // Flag
        Flags.COARSE_DIRT_TILLING.setDefaultSetting(world, true);
        when(clickedBlock.getType()).thenReturn(Material.PODZOL);
        BlockBreakEvent e = new BlockBreakEvent(clickedBlock, player);
        ctl.onBreakingPodzol(e);
        verify(clickedBlock, never()).setType(any());
    }
}
