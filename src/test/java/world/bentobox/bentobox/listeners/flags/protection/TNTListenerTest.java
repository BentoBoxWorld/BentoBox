package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Util.class, Bukkit.class} )
public class TNTListenerTest {

    @Mock
    private Location location;
    @Mock
    private BentoBox plugin;
    @Mock
    private Notifier notifier;
    @Mock
    private Block block;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private Player player;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private World world;
    @Mock
    private Entity entity;

    private Map<String, Boolean> worldFlags;
    // Class under test
    private TNTListener listener;

    @Before
    public void setUp() {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);

        // Worlds
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Monsters and animals
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);
        Cow cow = mock(Cow.class);
        when(cow.getLocation()).thenReturn(location);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        // Users
        User.setPlugin(plugin);


        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> (String)Arrays.asList(invocation.getArguments()).get(1);
        when(lm.get(any(), any())).thenAnswer(answer);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

        // Player name
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(any())).thenReturn("tastybento");
        when(plugin.getPlayers()).thenReturn(pm);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Island manager
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(any())).thenReturn(optional);

        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // Block
        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);

        // Entity
        when(entity.getType()).thenReturn(EntityType.PRIMED_TNT);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);

        // Player
        when(player.getLocation()).thenReturn(location);

        // In world
        when(iwm.inWorld(any(Location.class))).thenReturn(true);


        listener = new TNTListener();
        listener.setPlugin(plugin);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testOnTNTPriming() {
        BlockFace clickedFace = BlockFace.DOWN;
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.TNT);
        when(clickedBlock.getLocation()).thenReturn(location);
        ItemStack item = new ItemStack(Material.FLINT_AND_STEEL);
        Action action = Action.RIGHT_CLICK_BLOCK;
        Player player = mock(Player.class);
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
        assertTrue(list.isEmpty());
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
        assertTrue(list.isEmpty());
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
        assertFalse(list.isEmpty());
    }

    @Test
    public void testOnExplosionWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        List<Block> list = new ArrayList<>();
        list.add(block);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, list, 0);
        listener.onExplosion(e);
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
