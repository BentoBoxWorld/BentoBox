package world.bentobox.bentobox.api.addons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { BentoBox.class, Bukkit.class })
public class AddonTest {

    @Mock
    static BentoBox plugin;
    static JavaPlugin javaPlugin;


    @Before
    public void setUp() throws Exception {
        Server server = mock(Server.class);
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pluginManager);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);
        ItemMeta itemMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(itemMeta);

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
    public void testGetPlugin() {
        TestClass test = new TestClass();
        assertEquals(plugin, test.getPlugin());
    }

    @Test
    public void testGetConfig() {
        TestClass test = new TestClass();
        // No config file
        assertNull(test.getConfig());
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


    @Test
    public void testSaveConfig() {
        //TestClass test = new TestClass();
        // This will wipe out the config.yml of BSB so I am commenting it out
        //test.saveConfig();
    }

    @Test
    public void testSaveDefaultConfig() {
        TestClass test = new TestClass();
        File jarFile = new File("addon.jar");
        File dataFolder = new File("dataFolder");
        test.setDataFolder(dataFolder);
        test.setAddonFile(jarFile);
        test.saveDefaultConfig();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveResourceStringBoolean() {
        TestClass test = new TestClass();
        test.saveResource("", true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveResourceStringBooleanNull() {
        TestClass test = new TestClass();
        test.saveResource(null, true);
    }

    @Test
    public void testSaveResourceStringBooleanNoFile() throws IOException {
        TestClass test = new TestClass();
        File jarFile = new File("addon.jar");
        File dataFolder = new File("dataFolder");
        test.setDataFolder(dataFolder);
        test.setAddonFile(jarFile);
        test.saveResource("no_such_file", true);
    }

    @Test
    public void testSaveResourceStringFileBooleanBoolean() {
        TestClass test = new TestClass();
        File jarFile = new File("addon.jar");
        File dataFolder = new File("dataFolder");
        test.setDataFolder(dataFolder);
        test.setAddonFile(jarFile);
        test.saveResource("no_such_file", jarFile, false, false);
        test.saveResource("no_such_file", jarFile, false, true);
        test.saveResource("no_such_file", jarFile, true, false);
        test.saveResource("no_such_file", jarFile, true, true);

    }

    @Test
    public void testGetResource() {
        TestClass test = new TestClass();
        File jarFile = new File("addon.jar");
        File dataFolder = new File("dataFolder");
        test.setDataFolder(dataFolder);
        test.setAddonFile(jarFile);
        assertNull(test.getResource("nothing"));
    }

    @Test
    public void testSetAddonFile() {
        TestClass test = new TestClass();
        File jarFile = new File("addon.jar");
        test.setAddonFile(jarFile);
        assertEquals(jarFile, test.getFile());
    }

    @Test
    public void testSetDataFolder() {
        TestClass test = new TestClass();
        File dataFolder = new File("dataFolder");
        test.setDataFolder(dataFolder);
        assertEquals(dataFolder, test.getDataFolder());
    }

    @Test
    public void testSetDescription() {
        TestClass test = new TestClass();
        AddonDescription desc = new AddonDescription();
        test.setDescription(desc);
        assertEquals(desc, test.getDescription());
    }

    @Test
    public void testSetEnabled() {
        TestClass test = new TestClass();
        test.setEnabled(false);
        assertFalse(test.isEnabled());
        test.setEnabled(true);
        assertTrue(test.isEnabled());
    }

    @Test
    public void testGetPlayers() {
        TestClass test = new TestClass();
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        assertEquals(pm, test.getPlayers());
    }

    @Test
    public void testGetIslands() {
        TestClass test = new TestClass();
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        assertEquals(im, test.getIslands());
    }

    @Test
    public void testGetAddonByName() {
        AddonsManager am = new AddonsManager(plugin);
        when(plugin.getAddonsManager()).thenReturn(am);
        TestClass test = new TestClass();
        assertEquals(Optional.empty(),test.getAddonByName("addon"));
    }

}
