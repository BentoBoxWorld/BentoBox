package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class, Logger.class, DatabaseSetup.class,})
public class PlayersManagerTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private World nether;
    @Mock
    private World end;
    private UUID uuid;
    private User user;
    private UUID notUUID;
    private Database<Players> db;
    private PlayersManager pm;
    @Mock
    private Tameable tamed;
    @Mock
    private Player p;
    @Mock
    private Inventory inv;
    @Mock
    private VaultHook vault;
    @Mock
    private PlayerInventory playerInv;
    @Mock
    private Island island;
    
    private static AbstractDatabaseHandler<Object> h;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // This has to be done beforeClass otherwise the tests will interfere with each other
        h = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(h);
        when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
    }

    /**
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // Clear any lingering database
        tearDown();
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.getVault()).thenReturn(Optional.of(vault));
        // Settings
        Settings s = mock(Settings.class);
        // The database type has to be created one line before the thenReturn() to work!
        DatabaseType value = DatabaseType.JSON;
        when(plugin.getSettings()).thenReturn(s);
        when(s.getDatabaseType()).thenReturn(value);
        when(s.isUseEconomy()).thenReturn(true);

        // island world mgr
        when(world.getName()).thenReturn("world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(nether.getName()).thenReturn("world_nether");
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(end.getName()).thenReturn("world_the_end");
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);


        // Set up spawn
        Location netherSpawn = mock(Location.class);
        when(netherSpawn.toVector()).thenReturn(new Vector(0,0,0));
        when(nether.getSpawnLocation()).thenReturn(netherSpawn);
        when(iwm.getNetherSpawnRadius(Mockito.any())).thenReturn(100);

        // UUID
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        
        // Island
        when(island.getOwner()).thenReturn(uuid);

        // Player
        when(p.getEnderChest()).thenReturn(inv);
        when(p.getInventory()).thenReturn(playerInv);
        when(p.getUniqueId()).thenReturn(uuid);
        AttributeInstance at = mock(AttributeInstance.class);
        when(at.getValue()).thenReturn(20D);
        when(p.getAttribute(Attribute.GENERIC_MAX_HEALTH)).thenReturn(at);

        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.isOnline()).thenReturn(true);
        when(user.isPlayer()).thenReturn(true);
        User.setPlugin(plugin);


        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getUniqueId()).thenReturn(uuid);
        when(olp.getName()).thenReturn("tastybento");
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);


        // Player has island to begin with
        IslandsManager im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getOwner(Mockito.any(), Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);


        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Util
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.sameWorld(any(), any())).thenCallRealMethod();

        // Database
        db = mock(Database.class);

        // Leave commands
        when(iwm.getOnLeaveCommands(any())).thenReturn(Collections.emptyList());


        // Leave settings
        when(iwm.isOnLeaveResetEnderChest(any())).thenReturn(true);
        when(iwm.isOnLeaveResetInventory(any())).thenReturn(true);
        when(iwm.isKickedKeepInventory(any())).thenReturn(true);
        when(iwm.isOnLeaveResetMoney(any())).thenReturn(true);
        when(iwm.isOnLeaveResetHealth(any())).thenReturn(true);
        when(iwm.isOnLeaveResetHunger(any())).thenReturn(true);
        when(iwm.isOnLeaveResetXP(any())).thenReturn(true);

        // Tamed animals
        List<Tameable> list = new ArrayList<>();
        list.add(tamed);
        when(tamed.isTamed()).thenReturn(true);
        when(tamed.getOwner()).thenReturn(p);
        when(world.getEntitiesByClass(Tameable.class)).thenReturn(list);

        // Class under test
        pm = new PlayersManager(plugin);
    }

    /**
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
    }

    private void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }

    }
    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#PlayersManager(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testPlayersManager() {
        assertNotNull(pm);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#load()}.
     */
    @Test
    public void testLoad() {
        pm.setHandler(db);
        pm.load();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#saveAll()}.
     */
    @Test
    public void testSaveBoolean() {
        pm.saveAll();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        pm.shutdown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getPlayer(java.util.UUID)}.
     */
    @Test
    public void testGetPlayer() {
        Players player = pm.getPlayer(uuid);
        assertEquals("tastybento", player.getPlayerName());
        assertEquals(uuid.toString(), player.getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#addPlayer(java.util.UUID)}.
     */
    @Test
    public void testAddPlayer() {

        pm.addPlayer(null);
        // Add twice
        assertFalse(pm.isKnown(uuid));
        pm.addPlayer(uuid);
        assertTrue(pm.isKnown(uuid));
        pm.addPlayer(uuid);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#isKnown(java.util.UUID)}.
     */
    @Test
    public void testIsKnown() {

        pm.addPlayer(uuid);
        pm.addPlayer(notUUID);

        assertFalse(pm.isKnown(null));
        assertTrue(pm.isKnown(uuid));
        assertTrue(pm.isKnown(notUUID));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUIDwithUUID() {
        assertEquals(uuid,pm.getUUID(uuid.toString()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUIDOfflinePlayer() {
        pm.setHandler(db);
        // Add a player to the cache
        pm.addPlayer(uuid);
        UUID uuidResult = pm.getUUID("tastybento");
        assertEquals(uuid, uuidResult);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#setPlayerName(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testSetandGetPlayerName() {
        pm.setHandler(db);
        // Add a player
        pm.addPlayer(uuid);
        assertEquals("tastybento", pm.getName(user.getUniqueId()));
        pm.setPlayerName(user);
        assertEquals(user.getName(), pm.getName(user.getUniqueId()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUIDUnknownPlayer() {
        pm.setHandler(db);
        // Add a player to the cache
        pm.addPlayer(uuid);
        // Unknown player should return null
        assertNull(pm.getUUID("tastybento123"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#setResets(World, UUID, int)}.
     */
    @Test
    public void testGetSetResetsLeft() {
        // Add a player
        pm.addPlayer(uuid);
        assertEquals(0, pm.getResets(world, uuid));
        pm.setResets(world, uuid, 20);
        assertEquals(20, pm.getResets(world, uuid));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#save(java.util.UUID)}.
     */
    @Test
    public void testSaveUUID() {
        // Add a player
        pm.addPlayer(uuid);
        //pm.save(uuid);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#cleanLeavingPlayer(World, User, boolean)}.
     */
    @Test
    public void testCleanLeavingPlayerLeave() {
        pm.cleanLeavingPlayer(world, user, false, island);
        // Tamed animals
        verify(tamed).setOwner(eq(null));
        // Economy
        verify(vault).withdraw(eq(user), eq(0D), eq(world));
        // Enderchest
        verify(inv).clear();
        // Player inventory
        verify(playerInv).clear();
        // Health
        PowerMockito.verifyStatic(Util.class);
        Util.resetHealth(eq(p));
        // Food
        verify(p).setFoodLevel(eq(20));
        // XP
        verify(p).setTotalExperience(eq(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#cleanLeavingPlayer(World, User, boolean)}.
     */
    @Test
    public void testCleanLeavingPlayerKicked() {
        // Player is kicked
        pm.cleanLeavingPlayer(world, user, true, island);
        // Tamed animals
        verify(tamed).setOwner(eq(null));
        // Economy
        verify(vault).withdraw(eq(user), eq(0D), eq(world));
        // Enderchest
        verify(inv).clear();
        // Player inventory should NOT be cleared by default when kicked
        verify(playerInv, never()).clear();
        // Health
        PowerMockito.verifyStatic(Util.class);
        Util.resetHealth(eq(p));
        // Food
        verify(p).setFoodLevel(eq(20));
        // XP
        verify(p).setTotalExperience(eq(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#cleanLeavingPlayer(World, User, boolean)}.
     */
    @Test
    public void testCleanLeavingPlayerKickedOffline() {
        when(user.isOnline()).thenReturn(false);
        // Player is kicked
        pm.cleanLeavingPlayer(world, user, true, island);
        // Tamed animals
        verify(tamed).setOwner(eq(null));
        // Economy
        verify(vault).withdraw(eq(user), eq(0D), eq(world));
        // Enderchest
        verify(inv, never()).clear();
        // Player inventory should NOT be cleared by default when kicked
        verify(playerInv, never()).clear();
        // Health
        PowerMockito.verifyStatic(Util.class);
        Util.resetHealth(eq(p));
        // Food
        verify(p).setFoodLevel(eq(20));
        // XP
        verify(p).setTotalExperience(eq(0));
    }
}
