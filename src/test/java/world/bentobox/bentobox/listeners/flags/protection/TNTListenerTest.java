package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Util.class, Bukkit.class} )
public class TNTListenerTest extends AbstractCommonSetup {

    @Mock
    private Block block;
    @Mock
    private Entity entity;

    // Class under test
    private TNTListener listener;

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

        // Block
        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);

        // Entity
        when(entity.getType()).thenReturn(EntityType.PRIMED_TNT);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);

        listener = new TNTListener();
        listener.setPlugin(plugin);

    }

    @Test
    public void testOnTNTPriming() {
        BlockFace clickedFace = BlockFace.DOWN;
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.TNT);
        when(clickedBlock.getLocation()).thenReturn(location);
        ItemStack item = new ItemStack(Material.FLINT_AND_STEEL);
        Action action = Action.RIGHT_CLICK_BLOCK;
        PlayerInteractEvent e = new PlayerInteractEvent(player , action, item, clickedBlock, clickedFace);

        listener.onTNTPriming(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    @Test
    public void testOnExplosion() {
        List<Block> list = new ArrayList<>();
        list.add(block);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, list, 0);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    public void testOnExplosionOutsideIsland() {
        Flags.WORLD_TNT_DAMAGE.setDefaultSetting(false);
        assertFalse(Flags.WORLD_TNT_DAMAGE.isSetForWorld(world));
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        List<Block> list = new ArrayList<>();
        list.add(block);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, list, 0);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    public void testOnExplosionOutsideIslandAllowed() {
        Flags.WORLD_TNT_DAMAGE.setDefaultSetting(true);
        assertTrue(Flags.WORLD_TNT_DAMAGE.isSetForWorld(world));
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        List<Block> list = new ArrayList<>();
        list.add(block);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, list, 0);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testOnExplosionWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        List<Block> list = new ArrayList<>();
        list.add(block);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, list, 0);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testOnTNTDamageInWorldTNTNotProjectile() {
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Entity is not a projectile
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(player, block, Material.AIR.createBlockData());
        assertFalse(listener.onTNTDamage(e));
        assertFalse(e.isCancelled());

    }
    @Test
    public void testOnTNTDamageTNTWrongWorld() {
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Out of world
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(player, block, Material.AIR.createBlockData());
        assertFalse(listener.onTNTDamage(e));
        assertFalse(e.isCancelled());
    }
    @Test
    public void testOnTNTDamageObsidianWrongWorld() {
        // Block on fire
        when(block.getType()).thenReturn(Material.OBSIDIAN);
        // Out of world
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(player, block, Material.AIR.createBlockData());
        assertFalse(listener.onTNTDamage(e));
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnTNTDamageInWorldTNTProjectileWitherSkelly() {
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Entity is a projectile
        // Entity is an arrow
        Arrow arrow = mock(Arrow.class);
        // Shooter is a skeleton
        WitherSkeleton skeleton = mock(WitherSkeleton.class);
        when(arrow.getShooter()).thenReturn(skeleton);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        assertFalse(listener.onTNTDamage(e));
        assertFalse(e.isCancelled());
        verify(arrow, never()).remove();
    }

    @Test
    public void testOnTNTDamageInWorldTNTProjectilePlayerNotFireArrow() {
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Entity is a projectile
        // Entity is an arrow
        Arrow arrow = mock(Arrow.class);
        // Shooter is a player
        when(arrow.getShooter()).thenReturn(player);
        // Not fire arrow
        when(arrow.getFireTicks()).thenReturn(0);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        assertFalse(listener.onTNTDamage(e));
        assertFalse(e.isCancelled());
        verify(arrow, never()).remove();

    }

    @Test
    public void testOnTNTDamageInWorldTNTProjectilePlayerFireArrow() {
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Entity is a projectile
        // Entity is an arrow
        Arrow arrow = mock(Arrow.class);
        // Shooter is a player
        when(arrow.getShooter()).thenReturn(player);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        assertTrue(listener.onTNTDamage(e));
        assertTrue(e.isCancelled());
        verify(arrow).remove();

    }

    @Test
    public void testOnTNTDamageInWorldTNTProjectilePlayerFireArrowAllowed() {
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Entity is a projectile
        // Entity is an arrow
        Arrow arrow = mock(Arrow.class);
        // Shooter is a player
        when(arrow.getShooter()).thenReturn(player);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);
        // Allowed on island
        when(island.isAllowed(any(), eq(Flags.TNT_PRIMING))).thenReturn(true);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        assertFalse(listener.onTNTDamage(e));
        assertFalse(e.isCancelled());
        verify(arrow, never()).remove();

    }

    @Test
    public void testOnTNTDamageInWorldTNTProjectilePlayerFireArrowNotIsland() {
        Flags.TNT_PRIMING.setSetting(world, false);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Entity is a projectile
        // Entity is an arrow
        Arrow arrow = mock(Arrow.class);
        // Shooter is a player
        when(arrow.getShooter()).thenReturn(player);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        assertTrue(listener.onTNTDamage(e));
        assertTrue(e.isCancelled());
        verify(arrow).remove();

    }

    @Test
    public void testOnTNTDamageInWorldTNTProjectilePlayerFireArrowNotIslandNotAllowed() {
        Flags.TNT_PRIMING.setSetting(world, true);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Entity is a projectile
        // Entity is an arrow
        Arrow arrow = mock(Arrow.class);
        // Shooter is a player
        when(arrow.getShooter()).thenReturn(player);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        assertFalse(listener.onTNTDamage(e));
        assertFalse(e.isCancelled());
        verify(arrow, never()).remove();

    }

    @Test
    public void testOnEntityExplosion() {
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, player, DamageCause.ENTITY_EXPLOSION, 20D);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    public void testOnEntityExplosionOutsideIsland() {
        Flags.WORLD_TNT_DAMAGE.setDefaultSetting(false);
        assertFalse(Flags.WORLD_TNT_DAMAGE.isSetForWorld(world));
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, player, DamageCause.ENTITY_EXPLOSION, 20D);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    public void testOnEntityExplosionOutsideIslandAllowed() {
        Flags.WORLD_TNT_DAMAGE.setDefaultSetting(true);
        assertTrue(Flags.WORLD_TNT_DAMAGE.isSetForWorld(world));
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, player, DamageCause.ENTITY_EXPLOSION, 20D);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnEntityExplosionWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, player, DamageCause.ENTITY_EXPLOSION, 20D);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }

}
