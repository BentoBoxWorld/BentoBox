/**
 *
 */
package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class, Tag.class} )
public class PlaceBlocksListenerTest {

    private Location location;
    private BentoBox plugin;
    private Notifier notifier;

    private PlaceBlocksListener pbl;
    private Player player;
    private World world;
    private Island island;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);
        world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);

        ItemMeta meta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        PowerMockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Worlds
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        User.setPlugin(plugin);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> (String)Arrays.asList(invocation.getArguments()).get(1);
        when(lm.get(any(), any())).thenAnswer(answer);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Island manager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        island = mock(Island.class);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(Mockito.any())).thenReturn(optional);
        // Default is that everything is allowed
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);

        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());

        // Player
        player = mock(Player.class);
        when(player.getLocation()).thenReturn(location);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("tastybento");
        when(player.getWorld()).thenReturn(world);

        // Listener
        pbl = new PlaceBlocksListener();


    }

    @After
    public void cleanUp() {
        User.clearUsers();
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceFire() {
        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.FIRE);
        BlockState replacedBlockState = mock(BlockState.class);
        Block placedAgainst = mock(Block.class);
        ItemStack itemInHand = mock(ItemStack.class);
        EquipmentSlot hand = EquipmentSlot.HAND;
        BlockPlaceEvent e = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, player, true, hand);
        pbl.onBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlace() {
        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.STONE);
        when(placedBlock.getLocation()).thenReturn(location);
        BlockState replacedBlockState = mock(BlockState.class);
        Block placedAgainst = mock(Block.class);
        ItemStack itemInHand = mock(ItemStack.class);
        EquipmentSlot hand = EquipmentSlot.HAND;
        BlockPlaceEvent e = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, player, true, hand);
        pbl.onBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.STONE);
        when(placedBlock.getLocation()).thenReturn(location);
        BlockState replacedBlockState = mock(BlockState.class);
        Block placedAgainst = mock(Block.class);
        ItemStack itemInHand = mock(ItemStack.class);
        EquipmentSlot hand = EquipmentSlot.HAND;
        BlockPlaceEvent e = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, player, true, hand);
        pbl.onBlockPlace(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerHitItemFrame(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerHitItemFrameNotItemFrame() {
        Creeper creeper = mock(Creeper.class);
        when(creeper.getLocation()).thenReturn(location);
        when(creeper.getType()).thenReturn(EntityType.CREEPER);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, creeper, EquipmentSlot.HAND);
        pbl.onPlayerHitItemFrame(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerHitItemFrame(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerHitItemFrame() {
        ItemFrame itemFrame = mock(ItemFrame.class);
        when(itemFrame.getType()).thenReturn(EntityType.ITEM_FRAME);
        when(itemFrame.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, itemFrame, EquipmentSlot.HAND);
        pbl.onPlayerHitItemFrame(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerHitItemFrame(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerHitItemFrameNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        ItemFrame itemFrame = mock(ItemFrame.class);
        when(itemFrame.getType()).thenReturn(EntityType.ITEM_FRAME);
        when(itemFrame.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, itemFrame, EquipmentSlot.HAND);
        pbl.onPlayerHitItemFrame(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteract() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ARMOR_STAND, Material.FIREWORK_ROCKET, Material.ITEM_FRAME, Material.END_CRYSTAL, Material.CHEST, Material.TRAPPED_CHEST, Material.JUNGLE_BOAT);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        when(clickedBlock.getType()).thenReturn(Material.GRASS_BLOCK);
        for (int i = 0; i < 7; i++) {
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);
            pbl.onPlayerInteract(e);
            assertFalse("Failed on " + item.getType().toString(), e.isCancelled());
        }
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ARMOR_STAND, Material.FIREWORK_ROCKET, Material.ITEM_FRAME, Material.END_CRYSTAL, Material.CHEST, Material.TRAPPED_CHEST, Material.DARK_OAK_BOAT);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        when(clickedBlock.getType()).thenReturn(Material.GRASS_BLOCK);
        for (int i = 0; i < 7; i++) {
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);
            pbl.onPlayerInteract(e);
            assertTrue("Failed on " + item.getType().toString(), e.isCancelled());
        }
        Mockito.verify(notifier, Mockito.times(7)).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockForm(org.bukkit.event.block.EntityBlockFormEvent)}.
     */
    @Test
    public void testOnBlockForm() {
        // TODO
    }


}
