package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.TropicalFish;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class BucketListenerTest {

    @Mock
    private Location location;
    @Mock
    private BentoBox plugin;
    @Mock
    private Notifier notifier;

    private BucketListener l;
    @Mock
    private Player player;
    @Mock
    private World world;
    @Mock
    private Island island;
    @Mock
    private IslandWorldManager iwm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pim = mock(PluginManager.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        ItemMeta meta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        PowerMockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Worlds
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

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

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
        when(plugin.getNotifier()).thenReturn(notifier);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));
        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());

        // Player
        when(player.getLocation()).thenReturn(location);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("tastybento");
        when(player.getWorld()).thenReturn(world);

        // Listener
        l = new BucketListener();
    }


    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent)}.
     */
    @Test
    public void testOnBucketEmptyAllowed() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketEmpty(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent)}.
     */
    @Test
    public void testOnBucketEmptyNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketEmpty(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent)}.
     */
    @Test
    public void testOnBucketFillAllowed() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketFillEvent e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        when(item.getType()).thenReturn(Material.BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        when(item.getType()).thenReturn(Material.LAVA_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        when(item.getType()).thenReturn(Material.MILK_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent)}.
     */
    @Test
    public void testOnBucketFillNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketFillEvent e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        when(item.getType()).thenReturn(Material.BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        when(item.getType()).thenReturn(Material.LAVA_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        when(item.getType()).thenReturn(Material.MILK_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        verify(notifier, times(4)).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onTropicalFishScooping(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnTropicalFishScoopingNotFish() {
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, player);
        l.onTropicalFishScooping(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onTropicalFishScooping(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnTropicalFishScoopingFishNoWaterBucket() {
        TropicalFish fish = mock(TropicalFish.class);
        when(fish.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, fish );
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(player.getInventory()).thenReturn(inv);
        l.onTropicalFishScooping(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onTropicalFishScooping(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnTropicalFishScoopingFishWaterBucket() {
        TropicalFish fish = mock(TropicalFish.class);
        when(fish.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, fish );
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(player.getInventory()).thenReturn(inv);
        l.onTropicalFishScooping(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onTropicalFishScooping(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnTropicalFishScoopingFishWaterBucketNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        TropicalFish fish = mock(TropicalFish.class);
        when(fish.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, fish );
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(player.getInventory()).thenReturn(inv);
        l.onTropicalFishScooping(e);
        assertTrue(e.isCancelled());

        verify(notifier).notify(any(), eq("protection.protected"));
    }
}
