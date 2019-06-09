package world.bentobox.bentobox.database.mysql;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
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
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class, BentoBox.class, Util.class })
public class MySQLDatabaseHandlerTest {

    private MySQLDatabaseHandler<Island> handler;
    private Island instance;
    private String UNIQUE_ID = "xyz";
    private MySQLDatabaseConnector dbConn;
    private World world;
    @Mock
    private Location location;
    @Mock
    static BentoBox plugin = mock(BentoBox.class);
    private static IslandWorldManager iwm;


    @Before
    public void setUp() throws Exception {
        Server server = mock(Server.class);
        world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Settings settings = mock(Settings.class);

        when(plugin.getSettings()).thenReturn(settings);

        iwm = mock(IslandWorldManager.class);
        when(iwm.getDeathsMax(any())).thenReturn(10);
        when(plugin.getIWM()).thenReturn(iwm);

        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        dbConn = mock(MySQLDatabaseConnector.class);
        Connection connection = mock(Connection.class);
        when(dbConn.createConnection()).thenReturn(connection);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(Mockito.anyString())).thenReturn(ps);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        ResultSet rs = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(rs);
        when(statement.executeQuery(Mockito.anyString())).thenReturn(rs);
        instance = new Island();
        instance.setUniqueId(UNIQUE_ID);
        handler = new MySQLDatabaseHandler<>(plugin, Island.class, dbConn);

        PowerMockito.mockStatic(Util.class);
        when(Util.sameWorld(any(), any())).thenReturn(true);

        // Flags
        FlagsManager fm = mock(FlagsManager.class);
        when(plugin.getFlagsManager()).thenReturn(fm);
        when(fm.getFlags()).thenReturn(new ArrayList<>());

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

    }

    @Test
    public void testSaveNullObject() {
        handler.saveObject(null);
        verify(plugin).logError(eq("MySQL database request to store a null. "));
    }

    @Test
    public void testSaveObject() {
        handler.saveObject(instance);
    }

    @Test
    public void testSaveObject2() {
        BentoBox plugin = mock(BentoBox.class);
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(iwm.getDeathsMax(any())).thenReturn(10);
        Players players = new Players();
        players.setUniqueId(UUID.randomUUID().toString());
        players.setDeaths(world, 23);

        players.setHomeLocation(location);
        players.setHomeLocation(location, 1);
        players.setHomeLocation(location, 2);
        Map<Location, Long> map = new HashMap<>();
        map.put(location, 324L);
        players.setLocale("sdfsd");
        players.setPlayerName("name");
        players.setPlayerUUID(UUID.randomUUID());
        players.setResets(world, 3);


        MySQLDatabaseHandler<Players> h = new MySQLDatabaseHandler<>(plugin, Players.class, dbConn);
        h.saveObject(players);

    }

    @Test
    public void testSaveObject3() {
        Island island = new Island();
        island.setUniqueId(UNIQUE_ID);
        island.setCenter(location);
        Map<Flag, Integer> flags = new HashMap<>();
        for (Flag fl : Flags.values()) {
            flags.put(fl, 100);
        }
        island.setFlags(flags);
        island.setLevelHandicap(10);
        Map<UUID, Integer> members = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            members.put(UUID.randomUUID(), i);
        }
        island.setMembers(members);
        island.setName("ytasdgfsdfg");
        island.setOwner(UUID.randomUUID());
        island.setProtectionRange(100);
        island.setPurgeProtected(true);
        island.setRange(100);
        island.setSpawn(true);
        island.setSpawnPoint(Environment.NORMAL, location);
        island.setWorld(world);

        MySQLDatabaseHandler<Island> ih = new MySQLDatabaseHandler<>(plugin, Island.class, dbConn);
        ih.saveObject(island);

    }

}
