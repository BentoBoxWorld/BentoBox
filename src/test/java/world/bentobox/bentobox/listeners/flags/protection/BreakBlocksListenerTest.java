package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
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
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Flags.class, Util.class, Bukkit.class })
public class BreakBlocksListenerTest extends AbstractCommonSetup {

    private BreakBlocksListener bbl;
    @Mock
    private Block mockBlock;
    @Mock
    private ItemStack mockItem;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Island manager
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Listener
        bbl = new BreakBlocksListener();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakAllowed() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.DIRT);
        BlockBreakEvent e = new BlockBreakEvent(block, mockPlayer);
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
        when(block.getType()).thenReturn(Material.DIRT);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, mockPlayer);
        bbl.onBlockBreak(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockHarvestNotAllowed() {
        when(island.isAllowed(any(), 
                eq(Flags.HARVEST))).thenReturn(false);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.PUMPKIN);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, mockPlayer);
        bbl.onBlockBreak(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockHarvestAllowed() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(false);
        when(island.isAllowed(any(), eq(Flags.HARVEST))).thenReturn(true);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.PUMPKIN);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent e = new BlockBreakEvent(block, mockPlayer);
        bbl.onBlockBreak(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onBreakHanging(org.bukkit.event.hanging.HangingBreakByEntityEvent)}.
     */
    @Test
    public void testOnBreakHangingAllowed() {
        Hanging hanging = mock(Hanging.class);
        when(hanging.getLocation()).thenReturn(location);
        RemoveCause cause = RemoveCause.ENTITY;
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, mockPlayer, cause);
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
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(hanging, mockPlayer, cause);
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
        when(arrow.getShooter()).thenReturn(mockPlayer);
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
        when(arrow.getShooter()).thenReturn(mockPlayer);
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
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_AIR, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertEquals(e.useInteractedBlock(), Result.ALLOW);
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
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, item, block,
                BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
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
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, item, block,
                BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
        when(block.getType()).thenReturn(Material.SPAWNER);
        e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
        when(block.getType()).thenReturn(Material.DRAGON_EGG);
        e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
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
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, item, block,
                BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        when(block.getType()).thenReturn(Material.SPAWNER);
        e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        when(block.getType()).thenReturn(Material.DRAGON_EGG);
        e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.EAST);
        bbl.onPlayerInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
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
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, mockPlayer, 10);
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
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, mockPlayer, 10);
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
        when(vehicle.getType()).thenReturn(EntityType.OAK_BOAT);
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, mockPlayer, 10);
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
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, mockPlayer, 10);
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
        VehicleDamageEvent e = new VehicleDamageEvent(vehicle, mockPlayer, 10);
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
        Entity damagee = mockPlayer;
        Entity damager = mockPlayer;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
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
        Entity damager = mockPlayer;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(ItemFrame.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(EnderCrystal.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
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
        Entity damager = mockPlayer;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        damagee = mock(ItemFrame.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        damagee = mock(EnderCrystal.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
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
        when(damager.getShooter()).thenReturn(mockPlayer);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(ItemFrame.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(EnderCrystal.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
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
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(ItemFrame.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        damagee = mock(EnderCrystal.class);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
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
        when(damager.getShooter()).thenReturn(mockPlayer);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        verify(damagee).setFireTicks(0);

        damagee = mock(ItemFrame.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        verify(damagee).setFireTicks(0);

        damagee = mock(EnderCrystal.class);
        when(damagee.getLocation()).thenReturn(location);
        e = new EntityDamageByEntityEvent(damager, damagee, cause, null, 10);
        bbl.onEntityDamage(e);
        assertTrue(e.isCancelled());
        verify(notifier, times(3)).notify(any(), eq("protection.protected"));
        verify(damagee).setFireTicks(0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(PlayerInteractEvent)}
     */
    @Test
    public void testRightClickCaveVinesWithBerries() {
        when(mockBlock.getType()).thenReturn(Material.CAVE_VINES);
        when(mockBlock.getLocation()).thenReturn(location);
        CaveVinesPlant mockCaveVinesPlant = mock(CaveVinesPlant.class);
        when(mockBlock.getBlockData()).thenReturn(mockCaveVinesPlant);
        when(mockCaveVinesPlant.isBerries()).thenReturn(true);

        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, mockItem, mockBlock,
                BlockFace.UP);
        bbl.onPlayerInteract(e);

        assertTrue(e.useInteractedBlock() == Result.ALLOW);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(PlayerInteractEvent)}
     */
    @Test
    public void testRightClickSweetBerryBush() {
        when(mockBlock.getType()).thenReturn(Material.SWEET_BERRY_BUSH);
        when(mockBlock.getLocation()).thenReturn(location);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, mockItem, mockBlock,
                BlockFace.UP);
        bbl.onPlayerInteract(e);

        assertTrue(e.useInteractedBlock() == Result.ALLOW);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(PlayerInteractEvent)}
     */
    @Test
    public void testRightClickRootedDirt() {
        when(mockBlock.getType()).thenReturn(Material.ROOTED_DIRT);
        when(mockBlock.getLocation()).thenReturn(location);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, mockItem, mockBlock,
                BlockFace.UP);
        bbl.onPlayerInteract(e);

        assertTrue(e.useInteractedBlock() == Result.ALLOW);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(PlayerInteractEvent)}
     */
    @Test
    public void testLeftClickCake() {
        when(mockBlock.getType()).thenReturn(Material.CAKE);
        when(mockBlock.getLocation()).thenReturn(location);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, mockItem, mockBlock,
                BlockFace.UP);
        bbl.onPlayerInteract(e);
        assertTrue(e.useInteractedBlock() == Result.ALLOW);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(PlayerInteractEvent)}
     */
    @Test
    public void testLeftClickSpawner() {
        when(mockBlock.getType()).thenReturn(Material.SPAWNER);
        when(mockBlock.getLocation()).thenReturn(location);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, mockItem, mockBlock,
                BlockFace.UP);
        bbl.onPlayerInteract(e);

        assertTrue(e.useInteractedBlock() == Result.ALLOW);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(PlayerInteractEvent)}
     */
    @Test
    public void testLeftClickDragonEgg() {
        when(mockBlock.getType()).thenReturn(Material.DRAGON_EGG);
        when(mockBlock.getLocation()).thenReturn(location);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, mockItem, mockBlock,
                BlockFace.UP);
        bbl.onPlayerInteract(e);

        assertTrue(e.useInteractedBlock() == Result.ALLOW);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(PlayerInteractEvent)}
     */
    @Test
    public void testLeftClickHopper() {
        when(mockBlock.getType()).thenReturn(Material.HOPPER);
        when(mockBlock.getLocation()).thenReturn(location);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, mockItem, mockBlock,
                BlockFace.UP);
        bbl.onPlayerInteract(e);

        assertTrue(e.useInteractedBlock() == Result.ALLOW);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener#onPlayerInteract(PlayerInteractEvent)}
     */
    @Test
    public void testNoClick() {
        PlayerInteractEvent e = mock(PlayerInteractEvent.class);
        when(e.getClickedBlock()).thenReturn(null);
        bbl.onPlayerInteract(e);
        verify(e).getClickedBlock();
    }
}
