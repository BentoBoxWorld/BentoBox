package world.bentobox.bentobox.listeners.flags.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
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
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class PVPListenerTest extends CommonTestSetup {

    @Mock
    private Player player2;
    @Mock
    private Zombie zombie;
    @Mock
    private Creeper creeper;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
 
         // Make sure you set the plung for the User class otherwise it'll use an old object
        User.setPlugin(plugin);
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock.");
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // No visitor protection right now
        when(iwm.getIvSettings(any())).thenReturn(new ArrayList<>());

        Panel panel = mock(Panel.class);
        when(panel.getInventory()).thenReturn(mock(Inventory.class));


        // World
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);

        // Location
        when(location.getWorld()).thenReturn(world);

        // Sometimes use Mockito.withSettings().verboseLogging()
        // Player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.getLocation()).thenReturn(location);
        when(mockPlayer.getWorld()).thenReturn(world);
        User.getInstance(mockPlayer);

        // Sometimes use Mockito.withSettings().verboseLogging()
        // Player 2
        UUID uuid2 = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(uuid2);

        when(player2.getWorld()).thenReturn(world);
        when(player2.getLocation()).thenReturn(location);
        User.getInstance(player2);

        // Util
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(mock(World.class));

        // Flags Manager
        FlagsManager fm = mock(FlagsManager.class);
        Flag flag = mock(Flag.class);
        when(flag.isSetForWorld(any())).thenReturn(false);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(mock(ItemStack.class));
        when(flag.toPanelItem(any(), any(), any(), any(), eq(false))).thenReturn(item);
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

        // Util translate color codes (used in user translate methods)
        mockedUtil.when(() -> Util.translateColorCodes(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void wrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
    }

    private EntityDamageByEntityEvent createDamageEvent(Entity damager, Entity damagee, DamageCause cause) {
        return new EntityDamageByEntityEvent(damager, damagee, cause, null, 0);
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageNotPlayer() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Creeper.class);
        EntityDamageByEntityEvent e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageSelfDamage() {
        Entity damager = mock(Player.class);
        EntityDamageByEntityEvent e = createDamageEvent(damager, damager, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageNPC() {
        // Player 2 is an NPC
        when(player2.hasMetadata("NPC")).thenReturn(true);
        // PVP is not allowed
        when(island.isAllowed(any())).thenReturn(false);
        EntityDamageByEntityEvent e = createDamageEvent(mockPlayer, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed for NPC
        assertFalse(e.isCancelled());
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageNPCAttacks() {
        // Player 2 is an NPC
        when(player2.hasMetadata("NPC")).thenReturn(true);
        // PVP is not allowed
        when(island.isAllowed(any())).thenReturn(false);
        EntityDamageByEntityEvent e = createDamageEvent(player2, mockPlayer, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed for NPC
        assertFalse(e.isCancelled());
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageOnPlayerByZombie() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);
        when(damager.getLocation()).thenReturn(location);
        when(damagee.getLocation()).thenReturn(location);
        EntityDamageByEntityEvent e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());

        // Different attack type
        e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK);
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());

        // Wrong world
        e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        wrongWorld();
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }


    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageOnPlayerByZombieVisitorProtected() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);
        when(damager.getLocation()).thenReturn(location);
        when(damagee.getLocation()).thenReturn(location);
        // Protect visitors
        List<String> visitorProtectionList = new ArrayList<>();
        visitorProtectionList.add("ENTITY_ATTACK");
        when(iwm.getIvSettings(world)).thenReturn(visitorProtectionList);
        // This player is on their island, i.e., not a visitor

        EntityDamageByEntityEvent e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
        // Wrong world
        e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        wrongWorld();
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageOnVisitorByZombieVisitorProtected() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);
        when(damager.getLocation()).thenReturn(location);
        when(damagee.getLocation()).thenReturn(location);
        // Protect visitors
        when(iwm.getIvSettings(world)).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        // This player is a visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);

        EntityDamageByEntityEvent e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageOnVisitorByZombieVisitorProtectedWrongWorld() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);
        when(damager.getLocation()).thenReturn(location);
        when(damagee.getLocation()).thenReturn(location);
        // Protect visitors
        List<String> visitorProtectionList = new ArrayList<>();
        visitorProtectionList.add("ENTITY_ATTACK");
        when(iwm.getIvSettings(world)).thenReturn(visitorProtectionList);
        // This player is a visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);

        EntityDamageByEntityEvent e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        wrongWorld();
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageOnVisitorByZombieVisitorProtectedWrongDamage() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);
        when(damager.getLocation()).thenReturn(location);
        when(damagee.getLocation()).thenReturn(location);
        // Protect visitors
        List<String> visitorProtectionList = new ArrayList<>();
        visitorProtectionList.add("ENTITY_ATTACK");
        when(iwm.getIvSettings(world)).thenReturn(visitorProtectionList);
        // This player is a visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        // Damage is not entity attack
        EntityDamageByEntityEvent e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.THORNS);
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
    void testOnEntityDamageOnVisitorByZombieVisitorNotProtected() {
        Entity damager = mock(Zombie.class);
        Entity damagee = mock(Player.class);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(damager.getWorld()).thenReturn(world);
        when(damagee.getWorld()).thenReturn(world);
        when(damager.getLocation()).thenReturn(location);
        when(damagee.getLocation()).thenReturn(location);
        // This player is a visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);

        EntityDamageByEntityEvent e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        assertFalse(e.isCancelled());
        // Wrong world
        e = createDamageEvent(damager, damagee, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
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
    void testOnEntityDamagePVPNotAllowed() {
        // No visitor protection
        EntityDamageByEntityEvent e = createDamageEvent(mockPlayer, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        // PVP should be banned
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamagePVPNotAllowedInvVisitor() {
        EntityDamageByEntityEvent e = createDamageEvent(mockPlayer, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

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
    void testOnEntityDamageOnPVPAllowed() {
        // PVP is allowed
        when(island.isAllowed(any())).thenReturn(true);
        EntityDamageByEntityEvent e = createDamageEvent(mockPlayer, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed
        assertFalse(e.isCancelled());
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());

        // Enable visitor protection
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onEntityDamage(e);
        // visitor should not be protected
        assertFalse(e.isCancelled());

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamageOnPVPNotAllowedProjectile() {
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(mockPlayer);
        when(p.getLocation()).thenReturn(location);
        when(p.getWorld()).thenReturn(world);
        EntityDamageByEntityEvent e = createDamageEvent(p, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
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
    void testOnEntityDamageSelfDamageProjectile() {
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(mockPlayer);
        when(p.getLocation()).thenReturn(location);
        EntityDamageByEntityEvent e = createDamageEvent(p, mockPlayer, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        // Self damage okay
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamagePVPAllowedProjectile() {
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(mockPlayer);
        when(p.getLocation()).thenReturn(location);
        // PVP is allowed
        when(island.isAllowed(any())).thenReturn(true);
        EntityDamageByEntityEvent e = createDamageEvent(p, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed
        assertFalse(e.isCancelled());
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());

        // Enable visitor protection
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onEntityDamage(e);
        // visitor should not be protected
        assertFalse(e.isCancelled());

    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamagePVPAllowedProjectileNullSource() {
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(null);
        when(p.getLocation()).thenReturn(location);
        // PVP is allowed
        when(island.isAllowed(any())).thenReturn(true);
        EntityDamageByEntityEvent e = createDamageEvent(p, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed
        assertFalse(e.isCancelled());
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
    }

    /**
     * Test method for {@link PVPListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    void testOnEntityDamagePVPAllowedProjectileNonEntitySource() {
        Projectile p = mock(Projectile.class);
        BlockProjectileSource pSource = mock(BlockProjectileSource.class);
        when(p.getShooter()).thenReturn(pSource);
        when(p.getLocation()).thenReturn(location);
        // PVP is allowed
        when(island.isAllowed(any())).thenReturn(true);
        EntityDamageByEntityEvent e = createDamageEvent(p, player2, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        new PVPListener().onEntityDamage(e);
        // PVP should be allowed
        assertFalse(e.isCancelled());
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
    }

    /**
     * Test method for {@link PVPListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    void testOnFishing() {
        // Fish hook
        FishHook hook = mock(FishHook.class);
        // Catch a zombie - fine
        Entity caught = mock(Zombie.class);
        PlayerFishEvent pfe = new PlayerFishEvent(mockPlayer, caught, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());

        // Catch a player
        pfe = new PlayerFishEvent(mockPlayer, player2, hook, null);
        new PVPListener().onFishing(pfe);

        // PVP should be banned
        assertTrue(pfe.isCancelled());
        verify(notifier).notify(any(), eq(Flags.PVP_OVERWORLD.getHintReference()));
        // Hook should be removed
        verify(hook).remove();

        // Wrong world
        wrongWorld();
        pfe = new PlayerFishEvent(mockPlayer, player2, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());

        // Correct world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);
        pfe = new PlayerFishEvent(mockPlayer, player2, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());

        // Disallow PVP , attack on NPC
        when(player2.hasMetadata("NPC")).thenReturn(true);
        when(island.isAllowed(any())).thenReturn(false);
        pfe = new PlayerFishEvent(mockPlayer, player2, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());


        // Wrong world
        wrongWorld();
        pfe = new PlayerFishEvent(mockPlayer, player2, hook, null);
        new PVPListener().onFishing(pfe);
        assertFalse(pfe.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    void testOnFishingProtectVisitors() {
        // Fish hook
        FishHook hook = mock(FishHook.class);
        // Catch a player
        PlayerFishEvent pfe = new PlayerFishEvent(mockPlayer, player2, hook, null);

        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);

        // Protect visitors
        // This player is a visitor and any damage is not allowed
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(iwm.getIvSettings(any())).thenReturn(Collections.singletonList("ENTITY_ATTACK"));
        new PVPListener().onFishing(pfe);
        // visitor should not be protected
        assertFalse(pfe.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    void testOnFishingSelfDamage() {
        // Fish hook
        FishHook hook = mock(FishHook.class);
        // Catch a player
        PlayerFishEvent pfe = new PlayerFishEvent(mockPlayer, mockPlayer, hook, null);
        assertFalse(pfe.isCancelled());
        verify(mockPlayer, never()).sendMessage(Mockito.anyString());
    }

    /**
     * Test method for {@link PVPListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    void testOnFishingNoPVPProtectVisitors() {
        // Fish hook
        FishHook hook = mock(FishHook.class);
        // Catch a player
        PlayerFishEvent pfe = new PlayerFishEvent(mockPlayer, player2, hook, null);

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
    void testOnSplashPotionSplashWitch() {
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
    void testOnSplashPotionSplashNoPlayers() {
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
        // Create a damage map
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(zombie, 100D);
        map.put(creeper, 10D);
        when(zombie.getLocation()).thenReturn(location);
        when(creeper.getLocation()).thenReturn(location);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new PVPListener().onSplashPotionSplash(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    void testOnSplashPotionSplash() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);

        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
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
    void testOnSplashPotionSplashSelfInflicted() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);

        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
        // Create a damage map
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(mockPlayer, 100D);
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
    void testOnSplashPotionSplashAllowPVP() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(true);

        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
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
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
    }


    /**
     * Test method for {@link PVPListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    void testOnSplashPotionSplashAllowPVPProtectVisitors() {
        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);

        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
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
        // visitor should not be protected
        assertTrue(e.getAffectedEntities().contains(player2));
        assertTrue(e.getAffectedEntities().contains(zombie));
        assertTrue(e.getAffectedEntities().contains(creeper));

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
    void testOnLingeringPotionSplash() {
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        new PVPListener().onLingeringPotionSplash(e);
        // Verify
        verify(mockPlayer, times(5)).getUniqueId();
        verify(cloud).getEntityId();
        verify(tp).getShooter();
        mockedBukkit.verify(Bukkit::getScheduler);
    }

    /**
     * Test method for {@link PVPListener#onLingeringPotionSplash(org.bukkit.event.entity.LingeringPotionSplashEvent)}.
     */
    @Test
    void testOnLingeringPotionSplashNonHuman() {
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(creeper);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        new PVPListener().onLingeringPotionSplash(e);
        // Verify
        verify(cloud, never()).getEntityId();
        verify(tp).getShooter();
        mockedBukkit.verify(Bukkit::getScheduler, never());
    }

    /**
     * Test method for {@link PVPListener#onLingeringPotionDamage(org.bukkit.event.entity.AreaEffectCloudApplyEvent)}.
     */
    @Test
    void testOnLingeringPotionDamageNoPVP() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);
        // Throw a potion
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getWorld()).thenReturn(world);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        PVPListener listener = new PVPListener();
        listener.onLingeringPotionSplash(e);
        List<LivingEntity> list = new ArrayList<>();
        list.add(mockPlayer); // This player will still suffer
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
    void testOnLingeringPotionDamagePVP() {
        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);
        // Throw a potion
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getWorld()).thenReturn(world);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        PVPListener listener = new PVPListener();
        listener.onLingeringPotionSplash(e);
        List<LivingEntity> list = new ArrayList<>();
        list.add(mockPlayer); // This player will still suffer
        list.add(creeper);
        list.add(player2);
        list.add(zombie);
        // See who it affects
        AreaEffectCloudApplyEvent ae = new AreaEffectCloudApplyEvent(cloud, list);
        listener.onLingeringPotionDamage(ae);
        assertEquals(4, ae.getAffectedEntities().size());
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
        // Wrong world
        wrongWorld();
        listener.onLingeringPotionSplash(e);
        assertEquals(4, ae.getAffectedEntities().size());
        verify(mockPlayer, never()).sendMessage(Flags.PVP_OVERWORLD.getHintReference());
    }


    /**
     * Test method for {@link PVPListener#onLingeringPotionDamage(org.bukkit.event.entity.AreaEffectCloudApplyEvent)}.
     */
    @Test
    void testOnLingeringPotionDamageNoPVPVisitor() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);
        // Throw a potion
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getWorld()).thenReturn(world);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        PVPListener listener = new PVPListener();
        listener.onLingeringPotionSplash(e);
        List<LivingEntity> list = new ArrayList<>();
        list.add(mockPlayer); // This player will still suffer
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
    void testOnLingeringPotionDamagePVPVisitor() {
        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);
        // Throw a potion
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        when(tp.getWorld()).thenReturn(world);
        when(tp.getLocation()).thenReturn(location);
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getWorld()).thenReturn(world);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        PVPListener listener = new PVPListener();
        listener.onLingeringPotionSplash(e);
        List<LivingEntity> list = new ArrayList<>();
        list.add(mockPlayer); // This player will still suffer
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
    void testOnPlayerShootFireworkEventNotPlayer() {
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(123);
        EntityShootBowEvent e = new EntityShootBowEvent(creeper, bow, null, firework, EquipmentSlot.HAND, 1F, false);
        listener.onPlayerShootFireworkEvent(e);

        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(firework, mockPlayer, DamageCause.ENTITY_ATTACK, null,
                0);
        listener.onEntityDamage(en);
        assertFalse(en.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    void testOnPlayerShootFireworkEventNotFirework() {
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Arrow arrow = mock(Arrow.class);
        EntityShootBowEvent e = new EntityShootBowEvent(mockPlayer, bow, null, arrow, EquipmentSlot.HAND, 1F, false);
        listener.onPlayerShootFireworkEvent(e);
        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(arrow, mockPlayer, DamageCause.ENTITY_ATTACK, null, 0);
        listener.onEntityDamage(en);
        assertFalse(en.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    void testOnPlayerShootFireworkEventNoPVPSelfDamage() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(123);
        when(firework.getLocation()).thenReturn(location);
        EntityShootBowEvent e = new EntityShootBowEvent(mockPlayer, bow, null, firework, EquipmentSlot.HAND, 1F, false);
        listener.onPlayerShootFireworkEvent(e);

        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(firework, mockPlayer, DamageCause.ENTITY_EXPLOSION,
                null, 0);
        listener.onEntityDamage(en);
        assertFalse(en.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    void testOnPlayerShootFireworkEventNoPVP() {
        // Disallow PVP
        when(island.isAllowed(any())).thenReturn(false);
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(123);
        when(firework.getLocation()).thenReturn(location);
        when(firework.getWorld()).thenReturn(world);
        EntityShootBowEvent e = new EntityShootBowEvent(mockPlayer, bow, null, firework, EquipmentSlot.HAND, 1F, false);
        listener.onPlayerShootFireworkEvent(e);

        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(firework, player2, DamageCause.ENTITY_EXPLOSION,
                null, 0);
        listener.onEntityDamage(en);
        assertTrue(en.isCancelled());
    }

    /**
     * Test method for {@link PVPListener#onPlayerShootFireworkEvent(org.bukkit.event.entity.EntityShootBowEvent)}.
     */
    @Test
    void testOnPlayerShootFireworkEventPVPAllowed() {
        // Allow PVP
        when(island.isAllowed(any())).thenReturn(true);
        PVPListener listener = new PVPListener();
        ItemStack bow = new ItemStack(Material.CROSSBOW);
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(123);
        when(firework.getLocation()).thenReturn(location);
        EntityShootBowEvent e = new EntityShootBowEvent(mockPlayer, bow, null, firework, EquipmentSlot.HAND, 1F, false);
        listener.onPlayerShootFireworkEvent(e);

        // Now damage
        EntityDamageByEntityEvent en = new EntityDamageByEntityEvent(firework, player2, DamageCause.ENTITY_EXPLOSION,
                null, 0);
        listener.onEntityDamage(en);
        assertFalse(en.isCancelled());
    }

}
