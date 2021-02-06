package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
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
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class PhysicalInteractionListenerTest extends AbstractCommonSetup {

    private ItemStack item;
    private Block clickedBlock;

    @Override
    @Before
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

        // Tags
        when(Tag.PRESSURE_PLATES.isTagged(any(Material.class))).thenReturn(true);
        when(Tag.WOODEN_BUTTONS.isTagged(any(Material.class))).thenReturn(true);
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotPhysical() {
        when(clickedBlock.getType()).thenReturn(Material.STONE);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, clickedBlock, BlockFace.UP);
        new PhysicalInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractWrongMaterial() {
        when(clickedBlock.getType()).thenReturn(Material.STONE);
        when(Tag.PRESSURE_PLATES.isTagged(clickedBlock.getType())).thenReturn(false);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        new PhysicalInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractFarmland() {
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
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
        when(player.isOp()).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractFarmlandPermission() {
        when(player.hasPermission(anyString())).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
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
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
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
            PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
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
        ProjectileSource source = player ;
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
            assertTrue(p.name() +" failed", e.isCancelled());
        });
    }
}
