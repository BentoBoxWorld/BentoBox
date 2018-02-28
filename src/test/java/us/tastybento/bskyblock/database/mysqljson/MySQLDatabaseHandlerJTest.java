package us.tastybento.bskyblock.database.mysqljson;

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
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.database.mysql.MySQLDatabaseConnecter;
import us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandlerTestDataObject;

@RunWith(PowerMockRunner.class)
public class MySQLDatabaseHandlerJTest {
    
    private static MySQLDatabaseHandlerJ<MySQLDatabaseHandlerTestDataObject> handler;
    private static MySQLDatabaseHandlerTestDataObject instance;
    private static String UNIQUE_ID = "xyz";
    private static MySQLDatabaseConnecter dbConn;
    private static World world;


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
        
        BSkyBlock plugin = mock(BSkyBlock.class);
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
        instance = new MySQLDatabaseHandlerTestDataObject();
        instance.setUniqueId(UNIQUE_ID);
        handler = new MySQLDatabaseHandlerJ<>(plugin, MySQLDatabaseHandlerTestDataObject.class, dbConn);

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
       
        MySQLDatabaseHandlerJ<Players> h = new MySQLDatabaseHandlerJ<>(plugin, Players.class, dbConn);
        h.saveObject(players);
    }

}
