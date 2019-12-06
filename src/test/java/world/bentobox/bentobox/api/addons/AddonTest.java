package world.bentobox.bentobox.api.addons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.After;
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
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { BentoBox.class, Bukkit.class })
public class AddonTest {

    public static int BUFFER_SIZE = 10240;

    @Mock
    static BentoBox plugin;
    static JavaPlugin javaPlugin;
    private Server server;
    @Mock
    private AddonsManager am;
    private File dataFolder;

    private File jarFile;

    private TestClass test;

    @Before
    public void setUp() throws Exception {
        server = mock(Server.class);
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);


        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pluginManager);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pluginManager);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Addons manager
        when(plugin.getAddonsManager()).thenReturn(am);


        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);
        ItemMeta itemMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(itemMeta);

        // Make the addon
        dataFolder = new File("dataFolder");
        jarFile = new File("addon.jar");
        makeAddon();
        test = new TestClass();
        test.setDataFolder(dataFolder);
        test.setFile(jarFile);

    }

    public void makeAddon() throws IOException {
        // Make a config file
        YamlConfiguration config = new YamlConfiguration();
        config.set("hello", "this is a test");
        File configFile = new File("config.yml");
        config.save(configFile);
        // Make addon.yml
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("name", "TestAddon");
        yml.set("main", "world.bentobox.test.Test");
        yml.set("version", "1.0.0");
        File ymlFile = new File("addon.yml");
        yml.save(ymlFile);
        // Make an archive file
        // Put them into a jar file
        createJarArchive(jarFile, Arrays.asList(configFile, ymlFile));
        // Clean up
        Files.deleteIfExists(configFile.toPath());
        Files.deleteIfExists(ymlFile.toPath());
    }

    @After
    public void TearDown() throws IOException {
        Files.deleteIfExists(jarFile.toPath());
        if (dataFolder.exists()) {
            Files.walk(dataFolder.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
        Mockito.framework().clearInlineMocks();
    }

    class TestClass extends Addon {
        @Override
        public void onEnable() { }

        @Override
        public void onDisable() { }
    }

    @Test
    public void testAddon() {
        assertNotNull(test);
        assertFalse(test.isEnabled());
    }

    @Test
    public void testGetPlugin() {
        assertEquals(plugin, test.getPlugin());
    }

    @Test
    public void testGetConfig() {
        // No config file
        assertNull(test.getConfig());
    }

    @Test
    public void testGetDataFolder() {
        assertEquals(dataFolder, test.getDataFolder());
    }

    @Test
    public void testGetDescription() {
        AddonDescription d = new AddonDescription.Builder("main", "name", "1.0").build();
        assertNull(test.getDescription());
        test.setDescription(d);
        assertEquals(d, test.getDescription());
    }

    @Test
    public void testGetFile() {
        assertEquals(jarFile, test.getFile());
    }

    @Test
    public void testGetLogger() {
        assertEquals(plugin.getLogger(), test.getLogger());
    }

    @Test
    public void testGetServer() {
        assertEquals(server, test.getServer());
    }

    @Test
    public void testIsEnabled() {
        assertFalse(test.isEnabled());
    }

    @Test
    public void testRegisterListener() {
        class TestListener implements Listener {}
        TestListener listener = new TestListener();
        test.registerListener(listener);
        Mockito.verify(am).registerListener(Mockito.any(), Mockito.eq(listener));
    }

    @Test
    public void testSaveDefaultConfig() {
        test.saveDefaultConfig();
        File testConfig = new File(dataFolder, "config.yml");
        assertTrue(testConfig.exists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveResourceStringBooleanEmptyName() {
        test.saveResource("", true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveResourceStringBooleanSaveANull() {
        test.saveResource(null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveResourceStringBooleanNoFile() {
        test.saveResource("no_such_file", true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveResourceStringFileBooleanBoolean() {
        test.saveResource("no_such_file", jarFile, false, false);
        test.saveResource("no_such_file", jarFile, false, true);
        test.saveResource("no_such_file", jarFile, true, false);
        test.saveResource("no_such_file", jarFile, true, true);
    }

    @Test
    public void testGetResource() {
        assertNull(test.getResource("nothing"));
    }

    @Test
    public void testGetResourceSomething() {
        assertNotNull(test.getResource("addon.yml"));
    }

    @Test
    public void testSetAddonFile() {
        File af = new File("af");
        test.setFile(af);
        assertEquals(af, test.getFile());
    }

    @Test
    public void testSetDataFolder() {
        File df = new File("df");
        test.setDataFolder(df);
        assertEquals(df, test.getDataFolder());
    }

    @Test
    public void testSetDescription() {
        AddonDescription desc = new AddonDescription.Builder("main", "name", "2.0").build();
        test.setDescription(desc);
        assertEquals(desc, test.getDescription());
    }

    @Test
    public void testSetEnabled() {
        test.setState(Addon.State.DISABLED);
        assertFalse(test.isEnabled());
        test.setState(Addon.State.ENABLED);
        assertTrue(test.isEnabled());
    }

    @Test
    public void testGetPlayers() {
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        assertEquals(pm, test.getPlayers());
    }

    @Test
    public void testGetIslands() {
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        assertEquals(im, test.getIslands());
    }

    @Test
    public void testGetAddonByName() {
        AddonsManager am = new AddonsManager(plugin);
        when(plugin.getAddonsManager()).thenReturn(am);
        assertEquals(Optional.empty(),test.getAddonByName("addon"));
    }


    /*
     * Utility methods
     */
    private void createJarArchive(File archiveFile, List<File> tobeJaredList) {
        byte buffer[] = new byte[BUFFER_SIZE];
        // Open archive file
        try (FileOutputStream stream = new FileOutputStream(archiveFile)) {
            try (JarOutputStream out = new JarOutputStream(stream, new Manifest())) {
                for (File j: tobeJaredList) addFile(buffer, stream, out, j);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void addFile(byte[] buffer, FileOutputStream stream, JarOutputStream out, File tobeJared) throws IOException {
        if (tobeJared == null || !tobeJared.exists() || tobeJared.isDirectory())
            return;
        // Add archive entry
        JarEntry jarAdd = new JarEntry(tobeJared.getName());
        jarAdd.setTime(tobeJared.lastModified());
        out.putNextEntry(jarAdd);
        // Write file to archive
        try (FileInputStream in = new FileInputStream(tobeJared)) {
            while (true) {
                int nRead = in.read(buffer, 0, buffer.length);
                if (nRead <= 0)
                    break;
                out.write(buffer, 0, nRead);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
