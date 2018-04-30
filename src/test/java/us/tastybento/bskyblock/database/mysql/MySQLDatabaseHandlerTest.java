package us.tastybento.bskyblock.database.mysql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.plugin.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Players;
import us.tastybento.bskyblock.lists.Flags;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { BSkyBlock.class })
public class MySQLDatabaseHandlerTest {
    
    private static MySQLDatabaseHandler<Island> handler;
    private static Island instance;
    private static String UNIQUE_ID = "xyz";
    private static MySQLDatabaseConnecter dbConn;
    private static World world;
    @Mock
    static BSkyBlock plugin = mock(BSkyBlock.class);


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Server server = mock(Server.class);
        world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        Bukkit.setServer(server);

        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        
        Settings settings = mock(Settings.class);
        
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getDeathsMax()).thenReturn(10);
        
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        dbConn = mock(MySQLDatabaseConnecter.class);
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

    }

    @Test
    public void testSaveObject() {
        handler.saveObject(instance);
        BSkyBlock plugin = mock(BSkyBlock.class);
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getDeathsMax()).thenReturn(10);
        Players players = new Players();
        players.setUniqueId(UUID.randomUUID().toString());
        players.setDeaths(23);
        Location location = mock(Location.class);
        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getBlockX()).thenReturn(0);
        Mockito.when(location.getBlockY()).thenReturn(0);
        Mockito.when(location.getBlockZ()).thenReturn(0);

        players.setHomeLocation(location);
        players.setHomeLocation(location, 1);
        players.setHomeLocation(location, 2);
        Map<Location, Long> map = new HashMap<>();
        map.put(location, 324L);
        players.setKickedList(map);
        players.setLocale("sdfsd");
        players.setPlayerName("name");
        players.setPlayerUUID(UUID.randomUUID());
        players.setResetsLeft(3);
       

        MySQLDatabaseHandler<Players> h = new MySQLDatabaseHandler<>(plugin, Players.class, dbConn);
        h.saveObject(players);
        
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
        island.setMinProtectedX(-100);
        island.setMinProtectedZ(-300);
        island.setMinX(-121);
        island.setMinZ(-23423);
        island.setName("ytasdgfsdfg");
        island.setOwner(UUID.randomUUID());
        island.setProtectionRange(100);
        island.setPurgeProtected(true);
        island.setRange(100);
        island.setSpawn(true);
        island.setSpawnPoint(location);
        island.setWorld(world);
        
        MySQLDatabaseHandler<Island> ih = new MySQLDatabaseHandler<>(plugin, Island.class, dbConn);
        ih.saveObject(island);
        
    }

}
