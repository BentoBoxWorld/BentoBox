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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
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
public class BreakBlocksListenerTest {

    @Mock
    private Location location;
    @Mock
    private BentoBox plugin;
    @Mock
    private Notifier notifier;

    private BreakBlocksListener bbl;
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

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(mock(World.class));

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        PowerMockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Island World Manager
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
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Island manager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(any())).thenReturn(optional);
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Player
        when(player.getLocation()).thenReturn(location);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("tastybento");
        when(player.getWorld()).thenReturn(world);

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // Listener
        bbl = new BreakBlocksListener();
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakAllowed() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bbl.onBlockBreak(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bbl.onBlockBreak(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBreakHanging(org.bukkit.event.hanging.HangingBreakByEntityEvent)}.
     */
    @Test
    public void testOnBreakHangingAllowed() {
        Hanging hanging = mock(Hanging.class);
        when(hanging.getLocation()).thenReturn(location);
        RemoveCause cause = RemoveCause.ENTITY;
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, player, cause);
        bbl.onBreakHanging(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBreakHanging(org.bukkit.event.hanging.HangingBreakByEntityEvent)}.
     */
    @Test
    public void testOnBreakHangingNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Hanging hanging = mock(Hanging.class);
        when(hanging.getLocation()).thenReturn(location);
        RemoveCause cause = RemoveCause.ENTITY;
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, player, cause);
        bbl.onBreakHanging(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBreakHanging(org.bukkit.event.hanging.HangingBreakByEntityEvent)}.
     */
    @Test
    public void testOnBreakHangingNotPlayer() {
        Hanging hanging = mock(Hanging.class);
        when(hanging.getLocation()).thenReturn(location);
        RemoveCause cause = RemoveCause.EXPLOSION;
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, mock(Creeper.class), cause);
        bbl.onBreakHanging(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBreakHanging(org.bukkit.event.hanging.HangingBreakByEntityEvent)}.
     */
    @Test
    public void testOnBreakHangingNotPlayerProjectile() {
        Hanging hanging = mock(Hanging.class);
        when(hanging.getLocation()).thenReturn(location);
        RemoveCause cause = RemoveCause.PHYSICS;
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(mock(Skeleton.class));
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, arrow, cause);
        bbl.onBreakHanging(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBreakHanging(org.bukkit.event.hanging.HangingBreakByEntityEvent)}.
     */
    @Test
    public void testOnBreakHangingPlayerProjectileNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Hanging hanging = mock(Hanging.class);
        when(hanging.getLocation()).thenReturn(location);
        RemoveCause cause = RemoveCause.PHYSICS;
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, arrow, cause);
        bbl.onBreakHanging(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBreakHanging(org.bukkit.event.hanging.HangingBreakByEntityEvent)}.
     */
    @Test
    public void testOnBreakHangingPlayerProjectileAllowed() {
        Hanging hanging = mock(Hanging.class);
        when(hanging.getLocation()).thenReturn(location);
        RemoveCause cause = RemoveCause.PHYSICS;
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, arrow, cause);
        bbl.onBreakHanging(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }
    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotHit() {
        ItemStack item = mock(ItemStack.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Result.ALLOW));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractHitWrongType() {
        ItemStack item = mock(ItemStack.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.STONE);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Result.ALLOW));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testOnPlayerInteractHitCakeSpawnerDragonEggOK() {
        ItemStack item = mock(ItemStack.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.CAKE);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
        when(block.getType()).thenReturn(Material.SPAWNER);
        e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
        when(block.getType()).thenReturn(Material.DRAGON_EGG);
        e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractHitCakeSpawnerDragonEggNotOK() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        ItemStack item = mock(ItemStack.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.CAKE);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Result.DENY));
        when(block.getType()).thenReturn(Material.SPAWNER);
        e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Result.DENY));
        when(block.getType()).thenReturn(Material.DRAGON_EGG);
        e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock().equals(Result.DENY));
        verify(notifier, times(3)).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onVehicleDamageEvent(org.bukkit.event.vehicle.VehicleDamageEvent)}.
     */
    @Test
    public void testOnVehicleDamageEventAllowed() {
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getLocation()).thenReturn(location);
        when(vehicle.getType()).thenReturn(EntityType.MINECART);
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, player, 10);
        bbl.onVehicleDamageEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onVehicleDamageEvent(org.bukkit.event.vehicle.VehicleDamageEvent)}.
     */
    @Test
    public void testOnVehicleDamageEventNotAllowedMinecart() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getLocation()).thenReturn(location);
        when(vehicle.getType()).thenReturn(EntityType.MINECART);
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, player, 10);
        bbl.onVehicleDamageEvent(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onVehicleDamageEvent(org.bukkit.event.vehicle.VehicleDamageEvent)}.
     */
    @Test
    public void testOnVehicleDamageEventNotAllowedBoat() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getLocation()).thenReturn(location);
        when(vehicle.getType()).thenReturn(EntityType.BOAT);
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, player, 10);
        bbl.onVehicleDamageEvent(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onVehicleDamageEvent(org.bukkit.event.vehicle.VehicleDamageEvent)}.
     */
    @Test
    public void testOnVehicleDamageEventNotAllowedElse() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getLocation()).thenReturn(location);
        when(vehicle.getType()).thenReturn(EntityType.TRIDENT);
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, player, 10);
        bbl.onVehicleDamageEvent(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onVehicleDamageEvent(org.bukkit.event.vehicle.VehicleDamageEvent)}.
     */
    @Test
    public void testOnVehicleDamageEventWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getLocation()).thenReturn(location);
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, player, 10);
        bbl.onVehicleDamageEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onVehicleDamageEvent(org.bukkit.event.vehicle.VehicleDamageEvent)}.
     */
    @Test
    public void testOnVehicleDamageEventNotPlayer() {
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getLocation()).thenReturn(location);
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, mock(Creeper.class), 10);
        bbl.onVehicleDamageEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageNotCovered() {
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Entity damagee = player;
        Entity damager = player;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageAllowed() {
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Entity damagee = mock(ArmorStand.class);
        Entity damager = player;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(ItemFrame.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(EnderCrystal.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Entity damagee = mock(ArmorStand.class);
        when(damagee.getLocation()).thenReturn(location);
        Entity damager = player;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        damagee = mock(ItemFrame.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        damagee = mock(EnderCrystal.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        verify(notifier, times(3)).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageAllowedProjectile() {
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Entity damagee = mock(ArmorStand.class);
        Projectile damager = mock(Projectile.class);
        when(damager.getShooter()).thenReturn(player);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(ItemFrame.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(EnderCrystal.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageAllowedProjectileNotPlayer() {
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Entity damagee = mock(ArmorStand.class);
        Projectile damager = mock(Projectile.class);
        when(damager.getShooter()).thenReturn(mock(Skeleton.class));
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(ItemFrame.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(EnderCrystal.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageNotAllowedProjectile() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Entity damagee = mock(ArmorStand.class);
        when(damagee.getLocation()).thenReturn(location);
        Projectile damager = mock(Projectile.class);
        when(damager.getShooter()).thenReturn(player);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        verify(damagee).setFireTicks(0);

        damagee = mock(ItemFrame.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        verify(damagee).setFireTicks(0);

        damagee = mock(EnderCrystal.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        verify(notifier, times(3)).notify(any(), eq("protection.protected"));
        verify(damagee).setFireTicks(0);
    }
}
