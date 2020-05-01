package world.bentobox.bentobox.listeners.flags.settings;

import static org.junit.Assert.assertEquals;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LingeringPotion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitScheduler;
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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
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
@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class PVPListenerTest {

    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private Player player;
    @Mock
    private Player player2;
    @Mock
    private Location loc;
    @Mock
    private Zombie zombie;
    @Mock
    private Creeper creeper;
    @Mock
    private World world;
    @Mock
    private Notifier notifier;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Make sure you set the plung for the User class otherwise it'll use an old object
        User.setPlugin(plugin);
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock.");
        // No visitor protection right now
        when(iwm.getIvSettings(any())).thenReturn(new ArrayList<>());
        when(plugin.getIWM()).thenReturn(iwm);

        Panel panel = mock(Panel.class);
        when(panel.getInventory()).thenReturn(mock(Inventory.class));


        // World
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);

        // Location
        when(loc.getWorld()).thenReturn(world);

        // Sometimes use Mockito.withSettings().verboseLogging()
        // Player
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(loc);
        when(player.getWorld()).thenReturn(world);
        User.getInstance(player);

        // Sometimes use Mockito.withSettings().verboseLogging()
        // Player 2
        UUID uuid2 = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(uuid2);

        when(player2.getWorld()).thenReturn(world);
        when(player2.getLocation()).thenReturn(loc);
        User.getInstance(player2);

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(mock(World.class));

        // Flags Manager
        FlagsManager fm = mock(FlagsManager.class);
        Flag flag = mock(Flag.class);
        when(flag.isSetForWorld(any())).thenReturn(false);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(mock(ItemStack.class));
        when(flag.toPanelItem(any(), any(), any(), eq(false))).thenReturn(item);
        when(fm.getFlag(Mockito.anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Island Manager
        // Default is that player in on their island
        when(im.userIsOnIsland(any(), any())).thenReturn(true);
        island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(island));
        // All flags are disallowed by default.
        when(island.isAllowed(any())).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Locales - this returns the string that was requested for translation
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = (Answer<String>) invocation -> invocation.getArgument(1, String.class);
        when(lm.get(any(), any())).thenAnswer(answer);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

        // Create some entities
        when(zombie.getWorld()).thenReturn(world);
        when(zombie.getUniqueId()).thenReturn(UUID.randomUUID());
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        when(creeper.getWorld()).thenReturn(world);
        when(creeper.getUniqueId()).thenReturn(UUID.randomUUID());
        when(creeper.getType()).thenReturn(EntityType.CREEPER);

        // Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        GameModeAddon gma = mock(GameModeAddon.class);
        Optional<GameModeAddon> opGma = Optional.of(gma );
        when(iwm.getAddon(any())).thenReturn(opGma);

        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    private void wrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageNotPlayer() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Creeper.class);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageSelfDamage() {
        Entity damager = mock(Player.class);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damager, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageOnPlayerByZombie() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());

        // Different attack type
        e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());

        // Wrong world
        e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        wrongWorld();
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }


    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageOnPlayerByZombieVisitorProtected() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);

        // Protect visitors
        List<String> visitorProtectionList = new ArrayList<>();
        visitorProtectionList.add("ENTITY_ATTACK");
        when(iwm.getIvSettings(world)).thenReturn(visitorProtectionList);
        // This player is on their island, i.e., not a visitor

        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
        // Wrong world
        e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        wrongWorld();
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageOnVisitorByZombieVisitorProtected() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);

        // Protect visitors
        when(iwm.getIvSettings(world)).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        // This player is a visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);

        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageOnVisitorByZombieVisitorProtectedWrongWorld() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);

        // Protect visitors
        List<String> visitorProtectionList = new ArrayList<>();
        visitorProtectionList.add("ENTITY_ATTACK");
        when(iwm.getIvSettings(world)).thenReturn(visitorProtectionList);
        // This player is a visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);

        EntityDamageByEntityEvent e  = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        wrongWorld();
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageOnVisitorByZombieVisitorProtectedWrongDamage() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);

        // Protect visitors
        List<String> visitorProtectionList = new ArrayList<>();
        visitorProtectionList.add("ENTITY_ATTACK");
        when(iwm.getIvSettings(world)).thenReturn(visitorProtectionList);
        // This player is a visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        // Damage is not entity attack
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.THORNS,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
        // Wrong world
        wrongWorld();
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageOnVisitorByZombieVisitorNotProtected() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);

        // This player is a visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);

        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
        // Wrong world
        e = new EntityDamageByEntityEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        wrongWorld();
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    // PVP TESTS
    /*
     * PVP Tests
     *
     * Variables:
     * PVP on/off -> Direct hit / Projectile
     * Visitor protection on/off -> protection type correct/incorrect
     *
     */


    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamagePVPNotAllowed() {
        // No visitor protection
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(player, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        // PVP should be banned
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamagePVPNotAllowedInvVisitor() {
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(player, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));

        // Enable visitor protection
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onEntityDamage(e);
        // visitor should be protected
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageOnPVPAllowed() {
        // PVP is allowed
        when(island.isAllowed(any())).thenReturn(true);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(player, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed
        assertFalse(e.isCancelled());
        verify(player, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());

        // Enable visitor protection
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onEntityDamage(e);
        // visitor should be protected
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageOnPVPNotAllowedProjectile() {
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(player);
        when(p.getLocation()).thenReturn(loc);
        when(p.getWorld()).thenReturn(world);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        // PVP should be banned
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));

        // Visitor protection
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onEntityDamage(e);
        // visitor should be protected
        assertTrue(e.isCancelled());
        // PVP trumps visitor protection
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageSelfDamageProjectile() {
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(player);
        when(p.getLocation()).thenReturn(loc);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, player, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        // Self damage okay
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamagePVPAllowedProjectile() {
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(player);
        when(p.getLocation()).thenReturn(loc);
        // PVP is allowed
        when(island.isAllowed(any())).thenReturn(true);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed
        assertFalse(e.isCancelled());
        verify(player, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());

        // Enable visitor protection
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onEntityDamage(e);
        // visitor should be protected
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamagePVPAllowedProjectileNullSource() {
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(null);
        when(p.getLocation()).thenReturn(loc);
        // PVP is allowed
        when(island.isAllowed(any())).thenReturn(true);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed
        assertFalse(e.isCancelled());
        verify(player, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamagePVPAllowedProjectileNonEntitySource() {
        Projectile p = mock(Projectile.class);
        BlockProjectileSource pSource = mock(BlockProjectileSource.class);
        when(p.getShooter()).thenReturn(pSource);
        when(p.getLocation()).thenReturn(loc);
        // PVP is allowed
        when(island.isAllowed(any())).thenReturn(true);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(DamageModifier.BASE, 0D)),
                new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE, Functions.constant(-0.0))));
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed
        assertFalse(e.isCancelled());
        verify(player, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
    }

    /**
     * Test method for {@link PVPListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishing() {
        // Fish hook
        FishHook hook = mock(FishHook.class);
        // Catch a zombie - fine
        Entity caught = mock(Zombie.class);
        PlayerFishEvent pfe = new PlayerFishEvent(player, caught, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());

        // Catch a player
        pfe = new PlayerFishEvent(player, player2, hook, null);
        new PVPListener().onFishing(pfe);

        // PVP should be banned
        assertTrue(pfe.isCancelled());
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));
        // Hook should be removed
        verify(hook).remove();

        // Wrong world
        wrongWorld();
        pfe = new PlayerFishEvent(player, player2, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());

        // Correct world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);
        pfe = new PlayerFishEvent(player, player2, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());

        // Wrong world
        wrongWorld();
        pfe = new PlayerFishEvent(player, player2, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingProtectVisitors() {
        // Fish hook
        FishHook hook = mock(FishHook.class);
        // Catch a player
        PlayerFishEvent pfe = new PlayerFishEvent(player, player2, hook, null);

        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);

        // Protect visitors
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onFishing(pfe);
        // visitor should be protected
        assertTrue(pfe.isCancelled());
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));
    }

    /**
     * Test method for {@link PVPListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingSelfDamage() {
        // Fish hook
        FishHook hook = mock(FishHook.class);
        // Catch a player
        PlayerFishEvent pfe = new PlayerFishEvent(player, player, hook, null);
        assertFalse(pfe.isCancelled());
        verify(player, never()).sendMessage(Mockito.anyString());
    }

    /**
     * Test method for {@link PVPListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingNoPVPProtectVisitors() {
        // Fish hook
        FishHook hook = mock(FishHook.class);
        // Catch a player
        PlayerFishEvent pfe = new PlayerFishEvent(player, player2, hook, null);

        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);

        // Protect visitors
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onFishing(pfe);
        // visitor should be protected
        assertTrue(pfe.isCancelled());
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));
    }

    /**
     * Test method for {@link PVPListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    public void testOnSplashPotionSplashWitch() {
        ThrownPotion tp = mock(ThrownPotion.class);
        ProjectileSource witch = mock(Witch.class);
        when(tp.getShooter()).thenReturn(witch);
        PotionSplashEvent e = new PotionSplashEvent(tp, new HashMap<>());
        new PVPListener().onSplashPotionSplash(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    public void testOnSplashPotionSplashNoPlayers() {
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        // Create a damage map
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(zombie, 100D);
        map.put(creeper, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new PVPListener().onSplashPotionSplash(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    public void testOnSplashPotionSplash() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);

        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        // Create a damage map
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(player2, 100D);
        map.put(zombie, 100D);
        map.put(creeper, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new PVPListener().onSplashPotionSplash(e);
        assertFalse(e.getAffectedEntities().contains(player2));
        assertTrue(e.getAffectedEntities().contains(zombie));
        assertTrue(e.getAffectedEntities().contains(creeper));
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));

        // Wrong world
        wrongWorld();
        e = new PotionSplashEvent(tp, map);
        new PVPListener().onSplashPotionSplash(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    public void testOnSplashPotionSplashSelfInflicted() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);

        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        // Create a damage map
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(player, 100D);
        map.put(zombie, 100D);
        map.put(creeper, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new PVPListener().onSplashPotionSplash(e);
        assertFalse(e.isCancelled());

        // Wrong world
        wrongWorld();
        e = new PotionSplashEvent(tp, map);
        new PVPListener().onSplashPotionSplash(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    public void testOnSplashPotionSplashAllowPVP() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(true);

        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        // Create a damage map
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(player2, 100D);
        map.put(zombie, 100D);
        map.put(creeper, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new PVPListener().onSplashPotionSplash(e);
        assertTrue(e.getAffectedEntities().contains(player2));
        assertTrue(e.getAffectedEntities().contains(zombie));
        assertTrue(e.getAffectedEntities().contains(creeper));
        verify(player, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
    }


    /**
     * Test method for {@link PVPListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    public void testOnSplashPotionSplashAllowPVPProtectVisitors() {
        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);

        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        // Create a damage map
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(player2, 100D);
        map.put(zombie, 100D);
        map.put(creeper, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        // Protect visitors
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onSplashPotionSplash(e);
        // visitor should be protected
        assertFalse(e.getAffectedEntities().contains(player2));
        assertTrue(e.getAffectedEntities().contains(zombie));
        assertTrue(e.getAffectedEntities().contains(creeper));
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));

        // Wrong world
        wrongWorld();
        e = new PotionSplashEvent(tp, map);
        new PVPListener().onSplashPotionSplash(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onLingeringPotionSplash(org.bukkit.event.entity.LingeringPotionSplashEvent)}.
     */
    @Test
    public void testOnLingeringPotionSplash() {
        LingeringPotion tp = mock(LingeringPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        new PVPListener().onLingeringPotionSplash(e);
        // Verify
        verify(player, times(3)).getUniqueId();
        verify(cloud).getEntityId();
        verify(tp, times(2)).getShooter();
        PowerMockito.verifyStatic(Bukkit.class);
        Bukkit.getScheduler();
    }

    /**
     * Test method for {@link PVPListener#onLingeringPotionSplash(org.bukkit.event.entity.LingeringPotionSplashEvent)}.
     */
    @Test
    public void testOnLingeringPotionSplashNonHuman() {
        LingeringPotion tp = mock(LingeringPotion.class);
        when(tp.getShooter()).thenReturn(creeper);
        when(tp.getWorld()).thenReturn(world);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        new PVPListener().onLingeringPotionSplash(e);
        // Verify
        verify(cloud, never()).getEntityId();
        verify(tp).getShooter();
        PowerMockito.verifyStatic(Bukkit.class, never());
        Bukkit.getScheduler();
    }

    /**
     * Test method for {@link PVPListener#onLingeringPotionDamage(org.bukkit.event.entity.AreaEffectCloudApplyEvent)}.
     */
    @Test
    public void testOnLingeringPotionDamageNoPVP() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);
        // Throw a potion
        LingeringPotion tp = mock(LingeringPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getWorld()).thenReturn(world);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        PVPListener listener = new PVPListener();
        listener.onLingeringPotionSplash(e);
        List<LivingEntity> list = new ArrayList<>();
        list.add(player); // This player will still suffer
        list.add(creeper);
        list.add(player2);
        list.add(zombie);
        // See who it affects
        AreaEffectCloudApplyEvent ae = new AreaEffectCloudApplyEvent(cloud, list);
        listener.onLingeringPotionDamage(ae);
        assertEquals(3, ae.getAffectedEntities().size());
        assertFalse(ae.getAffectedEntities().contains(player2));
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));
        // Wrong world
        wrongWorld();
        listener.onLingeringPotionSplash(e);
        // No change to results
        assertEquals(3, ae.getAffectedEntities().size());
        assertFalse(ae.getAffectedEntities().contains(player2));
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));
    }

    /**
     * Test method for {@link PVPListener#onLingeringPotionDamage(org.bukkit.event.entity.AreaEffectCloudApplyEvent)}.
     */
    @Test
    public void testOnLingeringPotionDamagePVP() {
        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);
        // Throw a potion
        LingeringPotion tp = mock(LingeringPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getWorld()).thenReturn(world);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        PVPListener listener = new PVPListener();
        listener.onLingeringPotionSplash(e);
        List<LivingEntity> list = new ArrayList<>();
        list.add(player); // This player will still suffer
        list.add(creeper);
        list.add(player2);
        list.add(zombie);
        // See who it affects
        AreaEffectCloudApplyEvent ae = new AreaEffectCloudApplyEvent(cloud, list);
        listener.onLingeringPotionDamage(ae);
        assertEquals(4, ae.getAffectedEntities().size());
        verify(player, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
        // Wrong world
        wrongWorld();
        listener.onLingeringPotionSplash(e);
        assertEquals(4, ae.getAffectedEntities().size());
        verify(player, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
    }


    /**
     * Test method for {@link PVPListener#onLingeringPotionDamage(org.bukkit.event.entity.AreaEffectCloudApplyEvent)}.
     */
    @Test
    public void testOnLingeringPotionDamageNoPVPVisitor() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);
        // Throw a potion
        LingeringPotion tp = mock(LingeringPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getWorld()).thenReturn(world);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        PVPListener listener = new PVPListener();
        listener.onLingeringPotionSplash(e);
        List<LivingEntity> list = new ArrayList<>();
        list.add(player); // This player will still suffer
        list.add(creeper);
        list.add(player2);
        list.add(zombie);
        // Protect visitor
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));

        // See who it affects
        AreaEffectCloudApplyEvent ae = new AreaEffectCloudApplyEvent(cloud, list);
        listener.onLingeringPotionDamage(ae);
        assertEquals(3, ae.getAffectedEntities().size());
        assertFalse(ae.getAffectedEntities().contains(player2));
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));
        // Wrong world
        wrongWorld();
        listener.onLingeringPotionSplash(e);
        assertEquals(3, ae.getAffectedEntities().size());
        assertFalse(ae.getAffectedEntities().contains(player2));
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));
    }

    /**
     * Test method for {@link PVPListener#onLingeringPotionDamage(org.bukkit.event.entity.AreaEffectCloudApplyEvent)}.
     */
    @Test
    public void testOnLingeringPotionDamagePVPVisitor() {
        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);
        // Throw a potion
        LingeringPotion tp = mock(LingeringPotion.class);
        when(tp.getShooter()).thenReturn(player);
        when(tp.getWorld()).thenReturn(world);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getWorld()).thenReturn(world);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        PVPListener listener = new PVPListener();
        listener.onLingeringPotionSplash(e);
        List<LivingEntity> list = new ArrayList<>();
        list.add(player); // This player will still suffer
        list.add(creeper);
        list.add(player2);
        list.add(zombie);
        // Protect visitor
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));

        // See who it affects
        AreaEffectCloudApplyEvent ae = new AreaEffectCloudApplyEvent(cloud, list);
        listener.onLingeringPotionDamage(ae);
        assertEquals(3, ae.getAffectedEntities().size());
        assertFalse(ae.getAffectedEntities().contains(player2));
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));
        // Wrong world
        wrongWorld();
        listener.onLingeringPotionSplash(e);
        assertEquals(3, ae.getAffectedEntities().size());
        assertFalse(ae.getAffectedEntities().contains(player2));
        verify(notifier).notify(any(), eq(Flags.INVINCIBLE_VISITORS.getHintReference()));
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    public void testOnPlayerShootFireworkEventNotPlayer() {
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(123);
        EntityShootBowEvent e = new EntityShootBowEvent(creeper, bow, firework, 0);
        listener.onPlayerShootFireworkEvent(e);

        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(firework, player, DamageCause.ENTITY_ATTACK, 0);
        listener.onEntityDamage(en);
        assertFalse(en.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    public void testOnPlayerShootFireworkEventNotFirework() {
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Arrow arrow = mock(Arrow.class);
        EntityShootBowEvent e = new EntityShootBowEvent(creeper, bow, arrow, 0);
        listener.onPlayerShootFireworkEvent(e);
        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(arrow, player, DamageCause.ENTITY_ATTACK, 0);
        listener.onEntityDamage(en);
        assertFalse(en.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    public void testOnPlayerShootFireworkEventNoPVPSelfDamage() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(123);
        when(firework.getLocation()).thenReturn(loc);
        EntityShootBowEvent e = new EntityShootBowEvent(player, bow, firework, 0);
        listener.onPlayerShootFireworkEvent(e);

        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(firework, player, DamageCause.ENTITY_EXPLOSION, 0);
        listener.onEntityDamage(en);
        assertFalse(en.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    public void testOnPlayerShootFireworkEventNoPVP() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(123);
        when(firework.getLocation()).thenReturn(loc);
        when(firework.getWorld()).thenReturn(world);
        EntityShootBowEvent e = new EntityShootBowEvent(player, bow, firework, 0);
        listener.onPlayerShootFireworkEvent(e);

        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(firework, player2, DamageCause.ENTITY_EXPLOSION, 0);
        listener.onEntityDamage(en);
        assertTrue(en.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    public void testOnPlayerShootFireworkEventPVPAllowed() {
        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(123);
        when(firework.getLocation()).thenReturn(loc);
        EntityShootBowEvent e = new EntityShootBowEvent(player, bow, firework, 0);
        listener.onPlayerShootFireworkEvent(e);

        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(firework, player2, DamageCause.ENTITY_EXPLOSION, 0);
        listener.onEntityDamage(en);
        assertFalse(en.isCancelled());
    }

}
