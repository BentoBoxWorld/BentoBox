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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Vehicle;
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
public class BreakBlocksListenerTest {

    private Location location;
    private BentoBox plugin;
    private Notifier notifier;

    private BreakBlocksListener bbl;
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
        bbl = new BreakBlocksListener();
    }

    @After
    public void cleanUp() {
        User.clearUsers();
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
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bbl.onBlockBreak(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
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
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Hanging hanging = mock(Hanging.class);
        when(hanging.getLocation()).thenReturn(location);
        RemoveCause cause = RemoveCause.ENTITY;
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, player, cause);
        bbl.onBreakHanging(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
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
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotHit() {
        ItemStack item = mock(ItemStack.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
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
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
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
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        ItemStack item = mock(ItemStack.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.CAKE);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertTrue(e.isCancelled());
        when(block.getType()).thenReturn(Material.SPAWNER);
        e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertTrue(e.isCancelled());
        when(block.getType()).thenReturn(Material.DRAGON_EGG);
        e = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier, Mockito.times(3)).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onVehicleDamageEvent(org.bukkit.event.vehicle.VehicleDamageEvent)}.
     */
    @Test
    public void testOnVehicleDamageEventAllowed() {
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
    public void testOnVehicleDamageEventNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getLocation()).thenReturn(location);
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, player, 10);
        bbl.onVehicleDamageEvent(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
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
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
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
        Mockito.verify(notifier, Mockito.times(3)).notify(Mockito.any(), Mockito.eq("protection.protected"));
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
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Entity damagee = mock(ArmorStand.class);
        when(damagee.getLocation()).thenReturn(location);
        Projectile damager = mock(Projectile.class);
        when(damager.getShooter()).thenReturn(player);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        Mockito.verify(damagee).setFireTicks(0);

        damagee = mock(ItemFrame.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        Mockito.verify(damagee).setFireTicks(0);

        damagee = mock(EnderCrystal.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier, Mockito.times(3)).notify(Mockito.any(), Mockito.eq("protection.protected"));
        Mockito.verify(damager, Mockito.times(3)).remove();
        Mockito.verify(damagee).setFireTicks(0);

    }
}
