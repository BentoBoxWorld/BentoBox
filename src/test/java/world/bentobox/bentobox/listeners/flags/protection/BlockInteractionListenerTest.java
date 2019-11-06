package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class BlockInteractionListenerTest {

    @Mock
    private Player player;
    @Mock
    private PluginManager pim;
    @Mock
    private ItemFactory itemFactory;
    @Mock
    private Location location;
    @Mock
    private World world;
    private UUID uuid = UUID.randomUUID();
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private BentoBox plugin;
    @Mock
    private PlayerInventory inv;
    @Mock
    private Notifier notifier;

    private EquipmentSlot hand;

    private BlockInteractionListener bil;
    @Mock
    private ItemStack item;
    @Mock
    private Block clickedBlock;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

        // Clicked block
        when(clickedBlock.getLocation()).thenReturn(location);
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);

        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        User.setPlugin(plugin);
        User.getInstance(player);

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        @Nullable
        WorldSettings worldSet = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSet);

        // Island Manager
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optionalIsland = Optional.of(island);
        when(im.getProtectedIslandAt(any())).thenReturn(optionalIsland);

        // Island - nothing is allowed by default
        when(island.isAllowed(any())).thenReturn(false);
        when(island.isAllowed(any(), any())).thenReturn(false);

        // Inventory
        hand = EquipmentSlot.HAND;
        // Nothing in hand right now
        when(item.getType()).thenReturn(Material.AIR);
        when(player.getInventory()).thenReturn(inv);

        // Enable reporting from Flags class
        MetadataValue mdv = new FixedMetadataValue(plugin, "_why_debug");
        when(player.getMetadata(anyString())).thenReturn(Collections.singletonList(mdv));

        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        // Class under test
        bil = new BlockInteractionListener();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractItemFrameNotAllowed() {
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Event.Result.DENY));
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractItemFrameNotAllowedOtherFlagsOkay() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.PLACE_BLOCKS))).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Event.Result.DENY));
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNothingInHandPotsNotAllowed() {
        Arrays.stream(Material.values()).filter(m -> m.name().startsWith("POTTED")).forEach(bm -> {
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertTrue("Failure " + bm, e.useInteractedBlock().equals(Event.Result.DENY));
        });
        verify(notifier, times((int)Arrays.stream(Material.values()).filter(m -> m.name().startsWith("POTTED")).count())).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNothingInHandNotAllowed() {
        int count = 0;
        int worldSettingCount = 0;
        for (Material bm : BlockInteractionListener.getClickedBlocks().keySet()) {
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertTrue("Failure " + bm, e.useInteractedBlock().equals(Event.Result.DENY));
            if (BlockInteractionListener.getClickedBlocks().get(bm).getType().equals(Type.PROTECTION)) {
                count++;
            } else if (BlockInteractionListener.getClickedBlocks().get(bm).getType().equals(Type.WORLD_SETTING)) {
                worldSettingCount++;
            }
            verify(notifier, times(count)).notify(any(), eq("protection.protected"));
            verify(notifier, times(worldSettingCount)).notify(any(), eq("protection.world-protected"));
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNothingInHandAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        for (Material bm : BlockInteractionListener.getClickedBlocks().keySet()) {
            // Allow flags
            if (BlockInteractionListener.getClickedBlocks().get(bm).getType().equals(Type.PROTECTION)) {
                when(island.isAllowed(any(), eq(BlockInteractionListener.getClickedBlocks().get(bm)))).thenReturn(true);
            } else if (BlockInteractionListener.getClickedBlocks().get(bm).getType().equals(Type.WORLD_SETTING)) {
                BlockInteractionListener.getClickedBlocks().get(bm).setSetting(world, true);
            }
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertFalse("Failure " + bm, e.useInteractedBlock().equals(Event.Result.DENY));
            verify(notifier, never()).notify(any(), eq("protection.protected"));
            verify(notifier, never()).notify(any(), eq("protection.world-protected"));
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractSpawnEggInHandNotAllowed() {
        when(clickedBlock.getType()).thenReturn(Material.SPAWNER);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Event.Result.DENY));
        assertTrue(e.useItemInHand().equals(Event.Result.DENY));
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractSpawnEggInHandAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.SPAWNER);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertFalse(e.useInteractedBlock().equals(Event.Result.DENY));
        assertFalse(e.useItemInHand().equals(Event.Result.DENY));
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractSpawnEggInHandOnItemFrameNotAllowed() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.PLACE_BLOCKS))).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Event.Result.DENY));
        assertTrue(e.useItemInHand().equals(Event.Result.DENY));
        verify(notifier, times(2)).notify(any(), eq("protection.protected"));
    }



    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Ignore("TODO")
    @Test
    public void testOnBlockBreak() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onDragonEggTeleport(org.bukkit.event.block.BlockFromToEvent)}.
     */
    @Ignore("TODO")
    @Test
    public void testOnDragonEggTeleport() {
        fail("Not yet implemented"); // TODO
    }

}
