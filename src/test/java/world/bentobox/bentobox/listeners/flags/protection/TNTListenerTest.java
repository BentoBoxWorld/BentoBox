package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

public class TNTListenerTest extends CommonTestSetup {

    @Mock
    private Block block;
    @Mock
    private Entity entity;

    // Class under test
    private ExplosionListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // IWM - for some reason, this doesn't work in the CommonTestSetup
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        @Nullable
        WorldSettings worldSet = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSet);

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
        when(entity.getType()).thenReturn(EntityType.TNT);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);

        // Util
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), anyString())).thenCallRealMethod();

        listener = new ExplosionListener();
        listener.setPlugin(plugin);

    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testOnTNTPriming() {
        BlockFace clickedFace = BlockFace.DOWN;
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.TNT);
        when(clickedBlock.getLocation()).thenReturn(location);
        ItemStack item = new ItemStack(Material.FLINT_AND_STEEL);
        Action action = Action.RIGHT_CLICK_BLOCK;
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer , action, item, clickedBlock, clickedFace);

        listener.onTNTPriming(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    @Test
    public void testOnExplosion() {
        List<Block> list = new ArrayList<>();
        list.add(block);
        EntityExplodeEvent e = getExplodeEvent(entity, location, list);
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
        EntityExplodeEvent e = getExplodeEvent(entity, location, list);
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
        EntityExplodeEvent e = getExplodeEvent(entity, location, list);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testOnExplosionWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        List<Block> list = new ArrayList<>();
        list.add(block);
        EntityExplodeEvent e = getExplodeEvent(entity, location, list);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testOnTNTDamageInWorldTNTNotProjectile() {
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Entity is not a projectile
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(mockPlayer, block, Material.AIR.createBlockData());
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());

    }
    @Test
    public void testOnTNTDamageTNTWrongWorld() {
        // Block on fire
        when(block.getType()).thenReturn(Material.TNT);
        // Out of world
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(mockPlayer, block, Material.AIR.createBlockData());
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());
    }
    @Test
    public void testOnTNTDamageObsidianWrongWorld() {
        // Block on fire
        when(block.getType()).thenReturn(Material.OBSIDIAN);
        // Out of world
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(mockPlayer, block, Material.AIR.createBlockData());
        listener.onTNTDamage(e);
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
        listener.onTNTDamage(e);
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
        when(arrow.getShooter()).thenReturn(mockPlayer);
        // Not fire arrow
        when(arrow.getFireTicks()).thenReturn(0);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        listener.onTNTDamage(e);
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
        when(arrow.getShooter()).thenReturn(mockPlayer);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        listener.onTNTDamage(e);
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
        when(arrow.getShooter()).thenReturn(mockPlayer);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);
        // Allowed on island
        when(island.isAllowed(any(), eq(Flags.TNT_PRIMING))).thenReturn(true);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        listener.onTNTDamage(e);
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
        when(arrow.getShooter()).thenReturn(mockPlayer);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        listener.onTNTDamage(e);
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
        when(arrow.getShooter()).thenReturn(mockPlayer);
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);

        EntityChangeBlockEvent e = new EntityChangeBlockEvent(arrow, block, Material.AIR.createBlockData());
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());
        verify(arrow, never()).remove();

    }

    @Test
    public void testOnEntityExplosion() {
        /*
         * org.bukkit.event.entity.EntityDamageByEntityEvent.EntityDamageByEntityEvent(
         * @NotNull @NotNull Entity damager, 
         * @NotNull @NotNull Entity damagee, 
         * @NotNull @NotNull DamageCause cause, 
         * @NotNull @NotNull DamageSource damageSource, 
         * @NotNull @NotNull Map<DamageModifier, Double> modifiers, 
         * @NotNull @NotNull Map<DamageModifier, ?> modifierFunctions, 
         * boolean critical)
        
         Attempt to use newer event. This works but then other errors appear. Go figure.
        
        @NotNull
        Map<DamageModifier, Double> modifiers = new HashMap<>();
        modifiers.put(DamageModifier.BASE, 0.0D);
        @NotNull
        Map<DamageModifier, ? extends Function<? super Double, Double>> modifier = new HashMap<>();
        modifier.put(DamageModifier.BASE, null);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, mockPlayer, DamageCause.ENTITY_EXPLOSION,
                DamageSource.builder(DamageType.EXPLOSION).build(), modifiers, modifier, false);
                */
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, mockPlayer, DamageCause.ENTITY_EXPLOSION,
                null, 20D);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    public void testOnEntityExplosionOutsideIsland() {
        Flags.WORLD_TNT_DAMAGE.setDefaultSetting(false);
        assertFalse(Flags.WORLD_TNT_DAMAGE.isSetForWorld(world));
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, mockPlayer, DamageCause.ENTITY_EXPLOSION, null,
                20D);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    public void testOnEntityExplosionOutsideIslandAllowed() {
        Flags.WORLD_TNT_DAMAGE.setDefaultSetting(true);
        assertTrue(Flags.WORLD_TNT_DAMAGE.isSetForWorld(world));
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, mockPlayer, DamageCause.ENTITY_EXPLOSION, null,
                20D);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnEntityExplosionWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(entity, mockPlayer, DamageCause.ENTITY_EXPLOSION, null,
                20D);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }

}
