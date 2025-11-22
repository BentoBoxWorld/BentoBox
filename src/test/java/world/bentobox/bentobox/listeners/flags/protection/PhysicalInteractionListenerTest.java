package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * @author tastybento
 *
 */
@Disabled("Issues with NotAMock")
public class PhysicalInteractionListenerTest extends CommonTestSetup {

    private ItemStack item;
    private Block clickedBlock;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Monsters and animals
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);
        Cow cow = mock(Cow.class);
        when(cow.getLocation()).thenReturn(location);

        // Item and clicked block
        item = mock(ItemStack.class);
        clickedBlock = mock(Block.class);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotPhysical() {
        when(clickedBlock.getType()).thenReturn(Material.STONE);
        PlayerInteractEvent e  = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_AIR, item, clickedBlock, BlockFace.UP);
        new PhysicalInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractWrongMaterial() {
        when(clickedBlock.getType()).thenReturn(Material.STONE);
        PlayerInteractEvent e  = new PlayerInteractEvent(mockPlayer, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        new PhysicalInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractFarmland() {
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(mockPlayer, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        assertEquals(Result.DENY, e.useItemInHand());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractFarmlandOp() {
        when(mockPlayer.isOp()).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(mockPlayer, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractFarmlandPermission() {
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(mockPlayer, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractTurtleEgg() {
        when(clickedBlock.getType()).thenReturn(Material.TURTLE_EGG);
        PlayerInteractEvent e  = new PlayerInteractEvent(mockPlayer, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        assertEquals(Result.DENY, e.useItemInHand());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractPressurePlate() {
        Arrays.stream(Material.values()).filter(m -> m.name().contains("PRESSURE_PLATE")).forEach(p -> {
            when(clickedBlock.getType()).thenReturn(p);
            PlayerInteractEvent e  = new PlayerInteractEvent(mockPlayer, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
            PhysicalInteractionListener i = new PhysicalInteractionListener();
            i.onPlayerInteract(e);
            assertEquals(Result.DENY, e.useInteractedBlock());
            assertEquals(Result.DENY, e.useItemInHand());
        });
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileHit(org.bukkit.event.entity.EntityInteractEvent)}.
     */
    @Test
    public void testOnProjectileHitNotProjectile() {
        Entity entity = mock(Entity.class);
        Block block = mock(Block.class);
        EntityInteractEvent e = new EntityInteractEvent(entity, block);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onProjectileHit(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileHit(org.bukkit.event.entity.EntityInteractEvent)}.
     */
    @Test
    public void testOnProjectileHitProjectileBlockNull() {
        Projectile entity = mock(Projectile.class);
        ProjectileSource source = mock(Creeper.class);
        when(entity.getShooter()).thenReturn(source);
        Block block = null;
        EntityInteractEvent e = new EntityInteractEvent(entity, block);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onProjectileHit(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileHit(org.bukkit.event.entity.EntityInteractEvent)}.
     */
    @Test
    public void testOnProjectileHitProjectile() {
        Projectile entity = mock(Projectile.class);
        ProjectileSource source = mock(Creeper.class);
        when(entity.getShooter()).thenReturn(source);
        Block block = mock(Block.class);
        EntityInteractEvent e = new EntityInteractEvent(entity, block);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onProjectileHit(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileHit(org.bukkit.event.entity.EntityInteractEvent)}.
     */
    @Test
    public void testOnProjectileHitProjectilePlayer() {
        Projectile entity = mock(Projectile.class);
        ProjectileSource source = mockPlayer ;
        when(entity.getShooter()).thenReturn(source);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        Arrays.stream(Material.values())
        .filter(m -> !m.name().contains("LEGACY"))
        .filter(m -> m.name().contains("PRESSURE_PLATE") || m.name().contains("BUTTON")).forEach(p -> {
            when(block.getType()).thenReturn(p);
            EntityInteractEvent e = new EntityInteractEvent(entity, block);
            PhysicalInteractionListener i = new PhysicalInteractionListener();
            i.onProjectileHit(e);
            assertTrue(e.isCancelled(), p.name() +" failed");
        });
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileExplode(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnProjectileExplodeNotProjectile() {
        Entity entity = mock(Entity.class);
        List<Block> blocks = new ArrayList<>();
        EntityExplodeEvent e = getExplodeEvent(entity, location, blocks);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onProjectileExplode(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileExplode(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnProjectileExplodeProjectileNoPlayer() {
        Projectile entity = mock(Projectile.class);
        ProjectileSource source = mock(Creeper.class);
        when(entity.getShooter()).thenReturn(source);
        List<Block> blocks = new ArrayList<>();
        EntityExplodeEvent e = getExplodeEvent(entity, location, blocks);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onProjectileExplode(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileExplode(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnProjectileExplodeProjectilePlayer() {
        Projectile entity = mock(Projectile.class);
        when(entity.getShooter()).thenReturn(mockPlayer);
        List<Block> blocks = new ArrayList<>();
        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);
        when(block1.getLocation()).thenReturn(location);
        when(block2.getLocation()).thenReturn(location);
        blocks.add(block1);
        blocks.add(block2);

        EntityExplodeEvent e = getExplodeEvent(entity, location, blocks);
        PhysicalInteractionListener i = new PhysicalInteractionListener();

        // Test with wooden button
        when(block1.getType()).thenReturn(Material.OAK_BUTTON);
        // Test with pressure plate
        when(block2.getType()).thenReturn(Material.STONE_PRESSURE_PLATE);

        i.onProjectileExplode(e);
        verify(notifier, times(2)).notify(any(), eq("protection.protected"));
    }
}
