/**
 * 
 */
package us.tastybento.bskyblock.listeners.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.managers.PlayersManager;
import us.tastybento.bskyblock.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, User.class, Util.class })
public class FlyingMobEventsTest {

    private BSkyBlock plugin;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;
    private BukkitScheduler sch;
    private IslandWorldManager iwm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);

        // Settings
        s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Player has island to begin with 
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        
        // Normally in world
        Util.setPlugin(plugin);
        
        // Worlds
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any())).thenReturn(true);
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#FlyingMobEvents(us.tastybento.bskyblock.BSkyBlock)}.
     */
    @Test
    public void testFlyingMobEvents() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        assertNotNull(fme);
        Mockito.verify(sch).runTaskTimer(Mockito.eq(plugin), Mockito.any(Runnable.class), Mockito.eq(20L), Mockito.eq(20L));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent)}.
     */
    @Test
    public void testOnMobSpawnNotInWorld() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        LivingEntity le = mock(LivingEntity.class);
        CreatureSpawnEvent e = new CreatureSpawnEvent(le, SpawnReason.BUILD_WITHER);
        // Not in world
        when(iwm.inWorld(any())).thenReturn(false);
        fme.onMobSpawn(e);
        Mockito.verify(im, Mockito.never()).getIslandAt(Mockito.any(Location.class));       
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent)}.
     */
    @Test
    public void testOnMobSpawnInWorldWrongType() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        LivingEntity le = mock(LivingEntity.class);
        when(le.getType()).thenReturn(EntityType.AREA_EFFECT_CLOUD);
        CreatureSpawnEvent e = new CreatureSpawnEvent(le, SpawnReason.BUILD_WITHER);
        fme.onMobSpawn(e);
        Mockito.verify(im, Mockito.never()).getIslandAt(Mockito.any(Location.class));       
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent)}.
     */
    @Test
    public void testOnMobSpawnInWorldCorrectType() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        LivingEntity le = mock(LivingEntity.class);
        when(le.getLocation()).thenReturn(mock(Location.class));
        Optional<Island> oi = Optional.of(mock(Island.class));
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        // Wither
        when(le.getType()).thenReturn(EntityType.WITHER);
        CreatureSpawnEvent e = new CreatureSpawnEvent(le, SpawnReason.BUILD_WITHER);
        fme.onMobSpawn(e);
        Mockito.verify(im).getIslandAt(Mockito.any(Location.class));
        // Blaze
        when(le.getType()).thenReturn(EntityType.BLAZE);
        e = new CreatureSpawnEvent(le, SpawnReason.NATURAL);
        fme.onMobSpawn(e);
        Mockito.verify(im, Mockito.times(2)).getIslandAt(Mockito.any(Location.class));
        // Ghast
        when(le.getType()).thenReturn(EntityType.GHAST);
        e = new CreatureSpawnEvent(le, SpawnReason.NATURAL);
        fme.onMobSpawn(e);
        Mockito.verify(im, Mockito.times(3)).getIslandAt(Mockito.any(Location.class));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onMobExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnMobExplosionFail() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        // Entity, Location, list of Blocks, yield
        EntityExplodeEvent e = new EntityExplodeEvent(null, null, null, 0);
        // null entity
        assertFalse(fme.onMobExplosion(e));
        
        // Not in world
        Entity ent = mock(Entity.class);
        when(iwm.inWorld(any())).thenReturn(false);
        e = new EntityExplodeEvent(ent, null, null, 0);
        assertFalse(fme.onMobExplosion(e));
        
        // Unknown entity (not in the list)
        when(iwm.inWorld(any())).thenReturn(true);
        assertFalse(fme.onMobExplosion(e));
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onMobExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnMobExplosionOnIsland() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        // Spawn an entity
        LivingEntity le = mock(LivingEntity.class);
        when(le.getLocation()).thenReturn(mock(Location.class));
        Island island = mock(Island.class);
        // Start with ghast exploding in island space
        when(island.inIslandSpace(Mockito.any(Location.class))).thenReturn(true);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        // Wither
        when(le.getType()).thenReturn(EntityType.WITHER);
        CreatureSpawnEvent cee = new CreatureSpawnEvent(le, SpawnReason.BUILD_WITHER);
        fme.onMobSpawn(cee);
        // Make the wither explode
        // Entity, Location, list of Blocks, yield
        Block block = mock(Block.class);
        // One block will be blown up by the wither
        List<Block> affectedBlocks = new ArrayList<>();
        affectedBlocks.add(block);
        // Create event
        EntityExplodeEvent e = new EntityExplodeEvent(le, mock(Location.class), affectedBlocks, 0);   
        // Nothing blocked
        assertFalse(fme.onMobExplosion(e));
        assertFalse(e.isCancelled());
        assertFalse(e.blockList().isEmpty());
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onMobExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnMobExplosionOffIsland() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        // Spawn an entity
        LivingEntity le = mock(LivingEntity.class);
        when(le.getLocation()).thenReturn(mock(Location.class));
        Island island = mock(Island.class);
        // Ghast exploding outside of island space
        when(island.inIslandSpace(Mockito.any(Location.class))).thenReturn(false);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        // Wither
        when(le.getType()).thenReturn(EntityType.WITHER);
        CreatureSpawnEvent cee = new CreatureSpawnEvent(le, SpawnReason.BUILD_WITHER);
        fme.onMobSpawn(cee);
        // Make the wither explode
        // Entity, Location, list of Blocks, yield
        Block block = mock(Block.class);
        // One block will be blown up by the wither
        List<Block> affectedBlocks = new ArrayList<>();
        affectedBlocks.add(block);
        // Create event
        EntityExplodeEvent e = new EntityExplodeEvent(le, mock(Location.class), affectedBlocks, 0);   
        // Blocked
        assertTrue(fme.onMobExplosion(e));
        assertTrue(e.isCancelled());
        assertTrue(e.blockList().isEmpty());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onWitherExplode(org.bukkit.event.entity.ExplosionPrimeEvent)}.
     */
    @Test
    public void testOnWitherExplode() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        // Spawn an entity
        LivingEntity le = mock(LivingEntity.class);
        when(le.getLocation()).thenReturn(mock(Location.class));
        Island island = mock(Island.class);
        // Ghast exploding outside of island space
        when(island.inIslandSpace(Mockito.any(Location.class))).thenReturn(false);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        // Wither
        when(le.getType()).thenReturn(EntityType.WITHER);
        CreatureSpawnEvent cee = new CreatureSpawnEvent(le, SpawnReason.BUILD_WITHER);
        fme.onMobSpawn(cee);
        // Make the wither explode
        // Create event
        ExplosionPrimeEvent e = new ExplosionPrimeEvent(le, 0, false);   
        // Blocked
        assertTrue(fme.onWitherExplode(e));
        assertTrue(e.isCancelled());
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onWitherExplode(org.bukkit.event.entity.ExplosionPrimeEvent)}.
     */
    @Test
    public void testOnWitherSkullExplode() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        // Spawn a wither
        Wither wither = mock(Wither.class);
        when(wither.getLocation()).thenReturn(mock(Location.class));
        Island island = mock(Island.class);
        // Ghast exploding outside of island space
        when(island.inIslandSpace(Mockito.any(Location.class))).thenReturn(false);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        // Wither
        when(wither.getType()).thenReturn(EntityType.WITHER);
        CreatureSpawnEvent cee = new CreatureSpawnEvent(wither, SpawnReason.BUILD_WITHER);
        fme.onMobSpawn(cee);
        // Make the wither shoot a skull
        Projectile skull = mock(Projectile.class);
        when(skull.getType()).thenReturn(EntityType.WITHER_SKULL);
        when(skull.getShooter()).thenReturn(wither);
        
        // Create event
        ExplosionPrimeEvent e = new ExplosionPrimeEvent(skull, 0, false);   
        // Blocked
        assertTrue(fme.onWitherExplode(e));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onWitherChangeBlocks(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testOnWitherChangeBlocks() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        // Spawn a wither
        Wither wither = mock(Wither.class);
        when(wither.getLocation()).thenReturn(mock(Location.class));
        Island island = mock(Island.class);
        // Ghast exploding outside of island space
        when(island.inIslandSpace(Mockito.any(Location.class))).thenReturn(false);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        // Wither
        when(wither.getType()).thenReturn(EntityType.WITHER);
        CreatureSpawnEvent cee = new CreatureSpawnEvent(wither, SpawnReason.BUILD_WITHER);
        fme.onMobSpawn(cee);
        // Create event
        /**
        *
        * @param what the Entity causing the change
        * @param block the block (before the change)
        * @param to the future material being changed to
        * @param data the future block data
        * @deprecated Magic value
        */
        @SuppressWarnings("deprecation")
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(wither, mock(Block.class), Material.AIR, (byte) 0);
        // Blocked
        fme.onWitherChangeBlocks(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.listeners.protection.FlyingMobEvents#onMobDeath(org.bukkit.event.entity.EntityDeathEvent)}.
     */
    @Test
    public void testOnMobDeath() {
        FlyingMobEvents fme = new FlyingMobEvents(plugin);
        // Spawn a wither
        Wither wither = mock(Wither.class);
        // Wither
        when(wither.getType()).thenReturn(EntityType.WITHER);
        CreatureSpawnEvent cee = new CreatureSpawnEvent(wither, SpawnReason.BUILD_WITHER);
        Island island = mock(Island.class);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        fme.onMobSpawn(cee);
        // Kill it
        EntityDeathEvent e = new EntityDeathEvent(wither, null);
        assertNotNull(fme.onMobDeath(e));
    }

}
