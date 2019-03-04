package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFactory;
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
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class InventoryListenerTest {

    private final static List<Class<?>> HOLDERS = Arrays.asList(Horse.class, Chest.class,ShulkerBox.class, StorageMinecart.class, Dispenser.class,
            Dropper.class, Hopper.class, Furnace.class, BrewingStand.class, Beacon.class);

    private Location location;
    private BentoBox plugin;
    private Notifier notifier;

    private InventoryListener l;
    private Player player;
    private World world;
    private Island island;
    private IslandWorldManager iwm;

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
        iwm = mock(IslandWorldManager.class);
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
        l = new InventoryListener();
    }


    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickAllowed() {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getSize()).thenReturn(9);

        HOLDERS.forEach(c -> {
            Object holder = mock(c);
            when(inv.getHolder()).thenReturn((InventoryHolder) holder);
            when(view.getTopInventory()).thenReturn(inv);
            when(inv.getLocation()).thenReturn(location);
            when(view.getBottomInventory()).thenReturn(inv);
            SlotType slotType = SlotType.CONTAINER;
            InventoryAction action = InventoryAction.PICKUP_ONE;
            InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
            l.onInventoryClick(e);
            assertFalse(e.isCancelled());
        });

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNullHolder() {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = null;
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNotPlayer() {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(null);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = mock(InventoryHolder.class);
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        HOLDERS.forEach(c -> {
            Object holder = mock(c);
            when(inv.getHolder()).thenReturn((InventoryHolder) holder);
            when(view.getTopInventory()).thenReturn(inv);
            when(view.getBottomInventory()).thenReturn(inv);
            SlotType slotType = SlotType.CONTAINER;
            InventoryAction action = InventoryAction.PICKUP_ONE;
            InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
            l.onInventoryClick(e);
            assertTrue(e.isCancelled());
        });
        Mockito.verify(notifier, Mockito.times(HOLDERS.size())).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOtherHolder() {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = mock(InventoryHolder.class);
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOtherHolderStillAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = mock(InventoryHolder.class);
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertFalse(e.isCancelled());
    }

}
