package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Names;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class PlayersManagerTest extends CommonTestSetup {

    private AbstractDatabaseHandler<Players> playerHandler;
    private AbstractDatabaseHandler<Names> namesHandler;
    private AbstractDatabaseHandler<Island> islandHandler;
    private Database<Players> db;
    @Mock
    private World end;
    @Mock
    private Inventory inv;
    @Mock
    private World nether;
    private UUID notUUID;
    @Mock
    private Player p;
    @Mock
    private PlayerInventory playerInv;
    private PlayersManager pm;
    @Mock
    private Tameable tamed;
    private User user;
    private static UUID uuid = UUID.randomUUID();

    @Mock
    private VaultHook vault;
    private MockedStatic<DatabaseSetup> mockedDatabase;
    private @Nullable
    static UUID notThere = UUID.randomUUID();
    private static List<Names> names = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Clear any lingering database
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
        // This has to be done beforeClass otherwise the tests will interfere with each
        // other
        playerHandler = mock(AbstractDatabaseHandler.class);
        namesHandler = mock(AbstractDatabaseHandler.class);
        islandHandler = mock(AbstractDatabaseHandler.class);
        // Database
        mockedDatabase = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(eq(Players.class))).thenReturn(playerHandler);
        when(dbSetup.getHandler(eq(Names.class))).thenReturn(namesHandler);
        when(dbSetup.getHandler(eq(Island.class))).thenReturn(islandHandler);

        // Unknown UUID - nothing in database
        when(playerHandler.objectExists(notThere.toString())).thenReturn(false);
        when(namesHandler.objectExists(notThere.toString())).thenReturn(false);
        when(islandHandler.objectExists(any())).thenReturn(false);
        // Loading objects
        Players playerDB = new Players();
        when(playerHandler.loadObject(anyString())).thenReturn(playerDB);
        // Set up names database

        Names name = new Names();
        name.setUniqueId("tastybento");
        name.setUuid(uuid);
        names.add(name);
        when(namesHandler.loadObjects()).thenReturn(names);
        when(namesHandler.loadObject(any())).thenReturn(name);
        when(namesHandler.objectExists(uuid.toString())).thenReturn(true);
        // Save successfully
        when(namesHandler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(playerHandler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(islandHandler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));

        when(plugin.getVault()).thenReturn(Optional.of(vault));
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
        when(netherSpawn.toVector()).thenReturn(new Vector(0, 0, 0));
        when(nether.getSpawnLocation()).thenReturn(netherSpawn);
        when(iwm.getNetherSpawnRadius(Mockito.any())).thenReturn(100);

        // UUID
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
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
        when(p.getAttribute(Attribute.MAX_HEALTH)).thenReturn(at);
        when(p.getName()).thenReturn("tastybento");
        User.getInstance(p);

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
        mockedBukkit.when(() -> Bukkit.getOfflinePlayer(any(UUID.class))).thenAnswer(invocation -> {
            UUID inputUUID = invocation.getArgument(0);
            if (inputUUID.equals(uuid)) {
                return olp;
            } else {
                OfflinePlayer differentOlp = mock(OfflinePlayer.class);
                when(differentOlp.getUniqueId()).thenReturn(inputUUID);
                when(differentOlp.getName()).thenReturn("");
                return differentOlp;
            }
        });

        // Player has island to begin with
        IslandsManager im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        // when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        // when(im.getOwner(Mockito.any(), Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Util
        mockedUtil.when(() -> Util.sameWorld(any(), any())).thenCallRealMethod();
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);

        // Database
        db = mock(Database.class);

        // Leave commands
        when(iwm.getOnLeaveCommands(any())).thenReturn(Collections.emptyList());

        // Deaths
        when(iwm.getDeathsMax(world)).thenReturn(100);

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

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        mockedDatabase.closeOnDemand();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#addDeath(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testAddDeath() {
        int deaths = pm.getDeaths(world, uuid);
        pm.addDeath(world, uuid);
        assertEquals(deaths + 1, pm.getDeaths(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#addReset(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testAddReset() {
        int resets = pm.getResets(world, uuid);
        pm.addReset(world, uuid);
        assertEquals(resets + 1, pm.getResets(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#cleanLeavingPlayer(World, User, boolean)}.
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
        mockedUtil.verify(() -> Util.resetHealth(eq(p)));
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
        mockedUtil.verify(() -> Util.resetHealth(eq(p)));
        // Food
        verify(p).setFoodLevel(eq(20));
        // XP
        verify(p).setTotalExperience(eq(0));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#cleanLeavingPlayer(World, User, boolean)}.
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
        mockedUtil.verify(() ->  Util.resetHealth(eq(p)));
        // Food
        verify(p).setFoodLevel(eq(20));
        // XP
        verify(p).setTotalExperience(eq(0));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getDeaths(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetDeaths() {
        assertEquals(0, pm.getDeaths(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getLocale(java.util.UUID)}.
     */
    @Test
    public void testGetLocale() {
        assertTrue(pm.getLocale(uuid).isEmpty());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getName(java.util.UUID)}.
     */
    @Test
    public void testGetNameNull()  {
        // Null UUID
        assertTrue(pm.getName(null).isEmpty());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getName(java.util.UUID)}.
     */
    @Test
    public void testGetNameKnown()  {
        pm.setPlayerName(user);
        // Known UUID
        assertEquals("tastybento", pm.getName(uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getName(java.util.UUID)}.
     */
    @Test
    public void testGetNameUnknown()  {
        assertEquals("", pm.getName(notThere));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getPlayers()}.
     */
    @Test
    public void testGetPlayers() {
        assertTrue(pm.getPlayers().isEmpty());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getResets(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetResets() {
        assertEquals(0, pm.getResets(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getResetsLeft(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetResetsLeft() {
        assertEquals(0, pm.getResetsLeft(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#setResets(World, UUID, int)}.
     */
    @Test
    public void testGetSetResetsLeft()  {
        // Add a player
        pm.getPlayer(uuid);
        assertEquals(0, pm.getResets(world, uuid));
        pm.setResets(world, uuid, 20);
        assertEquals(20, pm.getResets(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getUser(java.lang.String)}.
     */
    @Test
    public void testGetUserString()  {
        User user = pm.getUser("random");
        assertNull(user);
        pm.getPlayer(uuid);
        user = pm.getUser("tastybento");
        assertEquals("tastybento", user.getName());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getUser(java.util.UUID)}.
     */
    @Test
    public void testGetUserUUID() {
        UUID uuid = pm.getUUID("unknown");
        assertNull(uuid);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUID() {
        pm.getPlayer(uuid);
        assertEquals(uuid, pm.getUUID("tastybento"));
        assertNull(pm.getUUID("unknown"));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUIDOfflinePlayer() {
        pm.setHandler(db);
        // Add a player to the cache
        pm.getPlayer(uuid);
        UUID uuidResult = pm.getUUID("tastybento");
        assertEquals(uuid, uuidResult);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUIDUnknownPlayer() {
        pm.setHandler(db);
        // Add a player to the cache
        pm.getPlayer(uuid);
        // Unknown player should return null
        assertNull(pm.getUUID("tastybento123"));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUIDwithUUID() {
        assertEquals(uuid, pm.getUUID(uuid.toString()));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#isInTeleport(java.util.UUID)}.
     */
    @Test
    public void testIsInTeleport() {
        assertFalse(pm.isInTeleport(uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#isKnown(java.util.UUID)}.
     */
    @Test
    public void testIsKnown() {

        pm.getPlayer(uuid);
        pm.getPlayer(notUUID);

        assertFalse(pm.isKnown(null));
        assertFalse(pm.isKnown(uuid));
        assertFalse(pm.isKnown(notUUID));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#setHandler(Database)}
     */
    @Test
    public void testSetHandler() {
        pm.setHandler(db);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#PlayersManager(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testPlayersManager() {
        assertNotNull(pm);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#removeInTeleport(java.util.UUID)}.
     */
    @Test
    public void testRemoveInTeleport() {
        pm.setInTeleport(uuid);
        assertTrue(pm.isInTeleport(uuid));
        pm.removeInTeleport(uuid);
        assertFalse(pm.isInTeleport(uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#removePlayer(org.bukkit.entity.Player)}.
     */
    @Test
    public void testRemovePlayer() {
        this.testGetUUID();
        pm.removePlayer(p);
        assertNull(pm.getUUID("tastybeto"));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#setPlayerName(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testSetandGetPlayerName() {
        pm.setHandler(db);
        // Add a player
        pm.getPlayer(uuid);
        assertEquals("tastybento", pm.getName(user.getUniqueId()));
        pm.setPlayerName(user);
        assertEquals(user.getName(), pm.getName(user.getUniqueId()));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#setDeaths(org.bukkit.World, java.util.UUID, int)}.
     */
    @Test
    public void testSetDeaths() {
        pm.setDeaths(world, uuid, 50);
        assertEquals(50, pm.getDeaths(world, uuid));

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#setInTeleport(java.util.UUID)}.
     */
    @Test
    public void testSetInTeleport() {
        assertFalse(pm.isInTeleport(uuid));
        pm.setInTeleport(uuid);
        assertTrue(pm.isInTeleport(uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#setLocale(java.util.UUID, java.lang.String)}.
     */
    @Test
    public void testSetLocale() {
        pm.setLocale(uuid, "en-UK");
        assertEquals("en-UK", pm.getLocale(uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#setPlayerName(world.bentobox.bentobox.api.user.User)}.
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    @Test
    public void testSetPlayerName() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        pm.setPlayerName(user).thenAccept(result -> assertTrue(result));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.PlayersManager#setResets(org.bukkit.World, java.util.UUID, int)}.
     */
    @Test
    public void testSetResets() {
        pm.setResets(world, uuid, 33);
        assertEquals(33, pm.getResets(world, uuid));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getPlayer(java.util.UUID)}.
     */
    @Test
    public void testGetPlayer() {
        Players p = pm.getPlayer(uuid);
        assertNotNull(p);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#loadPlayer(java.util.UUID)}.
     */
    @Test
    public void testLoadPlayer() {
        assertNotNull(pm.loadPlayer(uuid));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getName(java.util.UUID)}.
     */
    @Test
    public void testGetName() {
        assertEquals("tastybento", pm.getName(uuid));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#cleanLeavingPlayer(org.bukkit.World, world.bentobox.bentobox.api.user.User, boolean, world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testCleanLeavingPlayer() {
        when(user.isOnline()).thenReturn(true);
        when(iwm.isOnLeaveResetEnderChest(world)).thenReturn(true);
        when(iwm.isOnLeaveResetInventory(world)).thenReturn(true);
        when(iwm.isOnLeaveResetMoney(world)).thenReturn(true);
        pm.cleanLeavingPlayer(world, user, false, island);
        Util.runCommands(user, "", plugin.getIWM().getOnLeaveCommands(world), "leave");
        verify(world).getEntitiesByClass(Tameable.class);
        verify(inv).clear(); // Enderchest cleared
        verify(plugin).getVault(); // Clear money
        mockedUtil.verify(() ->  Util.resetHealth(p));
        verify(p).setFoodLevel(20);
        verify(p).setLevel(0);
        verify(p).setExp(0);
        // Player total XP (not displayed)
        verify(p).setTotalExperience(0);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#savePlayer(java.util.UUID)}.
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    @Test
    public void testSavePlayer() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        pm.savePlayer(uuid).thenAccept(result -> assertTrue(result));
    }

}
