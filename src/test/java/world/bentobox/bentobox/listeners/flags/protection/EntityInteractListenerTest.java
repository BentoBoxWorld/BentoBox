package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
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
public class EntityInteractListenerTest {

    private EntityInteractListener eil;
    @Mock
    private Player player;
    @Mock
    private Entity clickedEntity;
    private Vector position;
    private EquipmentSlot hand;
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
        when(clickedEntity.getLocation()).thenReturn(location);

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

        // Island Manager
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optionalIsland = Optional.of(island);
        when(im.getProtectedIslandAt(any())).thenReturn(optionalIsland);

        // Island - nothing is allowed by default
        when(island.isAllowed(any())).thenReturn(false);
        when(island.isAllowed(any(), any())).thenReturn(false);

        // Hand - main hand
        hand = EquipmentSlot.HAND;
        position = new Vector(10,10,10);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(new ItemStack(Material.NAME_TAG));

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
        eil = new EntityInteractListener();
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
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAtEntityArmorStandNoInteraction() {
        clickedEntity = mock(ArmorStand.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position, hand);
        eil.onPlayerInteractAtEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAtEntityArmorStandAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        clickedEntity = mock(ArmorStand.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position, hand);
        eil.onPlayerInteractAtEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityHorseNoInteraction() {
        clickedEntity = mock(Horse.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityHorseAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        clickedEntity = mock(Horse.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityMinecartNoInteraction() {
        clickedEntity = mock(RideableMinecart.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityMinecartAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        clickedEntity = mock(RideableMinecart.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityBoatNoInteraction() {
        clickedEntity = mock(Boat.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityBoatAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        clickedEntity = mock(Boat.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityVillagerNoInteraction() {
        clickedEntity = mock(Villager.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, times(2)).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAtEntityVillagerAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        clickedEntity = mock(Villager.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityNamingVillagerAllowedNoTrading() {
        when(island.isAllowed(any(), eq(Flags.TRADING))).thenReturn(false);
        when(island.isAllowed(any(), eq(Flags.NAME_TAG))).thenReturn(true);
        clickedEntity = mock(Villager.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityNamingVillagerAllowedTradingNoNaming() {
        when(island.isAllowed(any(), eq(Flags.TRADING))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.NAME_TAG))).thenReturn(false);
        clickedEntity = mock(Villager.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityWanderingTraderNoInteraction() {
        clickedEntity = mock(WanderingTrader.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, times(2)).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAtEntityWanderingTraderAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        clickedEntity = mock(WanderingTrader.class);
        when(clickedEntity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityNamingWanderingTraderAllowedNoTrading() {
        when(island.isAllowed(any(), eq(Flags.TRADING))).thenReturn(false);
        when(island.isAllowed(any(), eq(Flags.NAME_TAG))).thenReturn(true);
        clickedEntity = mock(WanderingTrader.class);
        when(clickedEntity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityNamingWanderingTraderAllowedTradingNoNaming() {
        when(island.isAllowed(any(), eq(Flags.TRADING))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.NAME_TAG))).thenReturn(false);
        clickedEntity = mock(WanderingTrader.class);
        when(clickedEntity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntitySheepAllowed() {
        clickedEntity = mock(Sheep.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.SHEEP);
        when(inv.getItemInMainHand()).thenReturn(new ItemStack(Material.AIR));
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntitySheepNameTagNoInteraction() {
        clickedEntity = mock(Sheep.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.SHEEP);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

}
