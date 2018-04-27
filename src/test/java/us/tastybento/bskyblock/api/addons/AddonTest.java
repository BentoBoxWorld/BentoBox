package us.tastybento.bskyblock.api.addons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { BSkyBlock.class })
public class AddonTest {
    
    @Mock
    static BSkyBlock plugin;
    static JavaPlugin javaPlugin;
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

        Bukkit.setServer(server);

        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
    }

    class TestClass extends Addon {

        @Override
        public void onEnable() {

        }

        @Override
        public void onDisable() {

        }

    }

    @Test
    public void testAddon() {
        TestClass test = new TestClass();
        assertNotNull(test);
        assertFalse(test.isEnabled());
    }

    @Test
    public void testGetBSkyBlock() {
        TestClass test = new TestClass();
        assertEquals(plugin, test.getBSkyBlock());
    }

    @Test
    public void testGetConfig() {
        TestClass test = new TestClass();
        assertNotNull(test.getConfig());
    }

    @Test
    public void testGetDataFolder() {
        TestClass test = new TestClass();
        File file = mock(File.class);
        assertNull(test.getDataFolder());
        test.setDataFolder(file);
        assertEquals(file, test.getDataFolder());
    }

    @Test
    public void testGetDescription() {
        TestClass test = new TestClass();
        AddonDescription d = new AddonDescription();
        assertNull(test.getDescription());
        test.setDescription(d);
        assertEquals(d, test.getDescription());
    }

    @Test
    public void testGetFile() {
        TestClass test = new TestClass();
        File file = mock(File.class);
        assertNull(test.getFile());
        test.setAddonFile(file);
        assertEquals(file, test.getFile());
    }

    @Test
    public void testGetLogger() {
        TestClass test = new TestClass();
        assertEquals(plugin.getLogger(), test.getLogger());
    }

    @Test
    public void testGetServer() {
        TestClass test = new TestClass();
        assertEquals(plugin.getServer(), test.getServer());
    }

    @Test
    public void testIsEnabled() {
        TestClass test = new TestClass();
        assertFalse(test.isEnabled());
    }

    @Test
    public void testRegisterListener() {
        class TestListener implements Listener {}
        TestListener listener = new TestListener();
        TestClass test = new TestClass();
        test.registerListener(listener);
    }

    /*
    @Test
    public void testSaveConfig() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSaveDefaultConfig() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSaveResourceStringBoolean() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSaveResourceStringFileBooleanBoolean() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testGetResource() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSetAddonFile() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSetDataFolder() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSetDescription() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSetEnabled() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testGetPlayers() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testGetIslands() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testGetAddonByName() {
        fail("Not yet implemented"); // TODO
    }
*/
}
