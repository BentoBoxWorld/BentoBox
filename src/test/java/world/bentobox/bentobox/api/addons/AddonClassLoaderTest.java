package world.bentobox.bentobox.api.addons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.jdt.annotation.NonNull;
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

import com.github.puregero.multilib.MultiLib;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonDescriptionException;
import world.bentobox.bentobox.managers.AddonsManager;

/**
 * Test class for addon class loading
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class, MultiLib.class })
public class AddonClassLoaderTest {

    private enum mandatoryTags {
        MAIN,
        NAME,
        VERSION,
        AUTHORS,
        ICON
    }
    /**
     * Used for file writing etc.
     */
    public static final int BUFFER_SIZE = 10240;

    // Test addon fields
    private File dataFolder;
    private File jarFile;
    private TestClass testAddon;


    // Class under test
    private AddonClassLoader acl;

    // Mocks
    @Mock
    private AddonsManager am;

    private BentoBox plugin;

    /**
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        PowerMockito.mockStatic(MultiLib.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // To start include everything
        makeAddon(List.of());
        testAddon = new TestClass();
        testAddon.setDataFolder(dataFolder);
        testAddon.setFile(jarFile);

    }

    public void makeAddon(List<mandatoryTags> missingTags) throws IOException {
        // Make the addon
        dataFolder = new File("dataFolder");
        jarFile = new File("addon.jar");
        // Make a config file
        YamlConfiguration config = new YamlConfiguration();
        config.set("hello", "this is a test");
        File configFile = new File("config.yml");
        config.save(configFile);
        // Make addon.yml
        YamlConfiguration yml = getYaml(missingTags);
        File ymlFile = new File("addon.yml");
        yml.save(ymlFile);
        // Make an archive file
        // Put them into a jar file
        createJarArchive(jarFile, Arrays.asList(configFile, ymlFile));
        // Clean up
        Files.deleteIfExists(configFile.toPath());
        Files.deleteIfExists(ymlFile.toPath());
    }

    private YamlConfiguration getYaml(List<mandatoryTags> missingTags) {
        YamlConfiguration r = new YamlConfiguration();
        if (!missingTags.contains(mandatoryTags.NAME)) {
            r.set("name", "TestAddon");
        }
        if (!missingTags.contains(mandatoryTags.MAIN)) {
            r.set("main", "world.bentobox.test.Test");
        }
        if (!missingTags.contains(mandatoryTags.VERSION)) {
            r.set("version", "1.0.0");
        }
        if (!missingTags.contains(mandatoryTags.AUTHORS)) {
            r.set("authors", "tastybento");
        }
        r.set("metrics", false);
        r.set("repository", "repo");
        r.set("depend", "Level, Warps");
        r.set("softdepend", "Boxed, AcidIsland");
        if (!missingTags.contains(mandatoryTags.ICON)) {
            r.set("icon", "IRON_INGOT");
        } else {
            r.set("icon", "unkOwnMateriaL");
        }
        r.set("api-version", "1.21-SNAPSHOT");
        return r;
    }

    /*
     * Utility methods
     */
    private void createJarArchive(File archiveFile, List<File> tobeJaredList) {
        byte[] buffer = new byte[BUFFER_SIZE];
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

    /**
     */
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


    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#AddonClassLoader(world.bentobox.bentobox.managers.AddonsManager, org.bukkit.configuration.file.YamlConfiguration, java.io.File, java.lang.ClassLoader)}.
     */
    @Test
    public void testAddonClassLoader() throws MalformedURLException {
        acl = new AddonClassLoader(testAddon, am, jarFile);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#asDescription(org.bukkit.configuration.file.YamlConfiguration)}.
     */
    @Test
    public void testAsDescription() throws InvalidAddonDescriptionException {
        YamlConfiguration yml = this.getYaml(List.of());
        @NonNull
        AddonDescription desc = AddonClassLoader.asDescription(yml);
        assertEquals("1.21-SNAPSHOT", desc.getApiVersion());
        assertFalse(desc.isMetrics());
        assertEquals(List.of("tastybento"), desc.getAuthors());
        assertEquals(List.of("Level", "Warps"), desc.getDependencies());
        assertEquals("", desc.getDescription());
        assertEquals(Material.IRON_INGOT, desc.getIcon());
        assertEquals("world.bentobox.test.Test", desc.getMain());
        assertEquals("TestAddon", desc.getName());
        assertEquals("repo", desc.getRepository());
        assertEquals(List.of("Boxed", "AcidIsland"), desc.getSoftDependencies());
        assertEquals("1.0.0", desc.getVersion());
        assertNull(desc.getPermissions());
        verify(plugin).logWarning("TestAddon addon depends on development version of BentoBox plugin. Some functions may be not implemented.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#asDescription(org.bukkit.configuration.file.YamlConfiguration)}.
     */
    @Test
    public void testAsDescriptionNoName() {
        YamlConfiguration yml = this.getYaml(List.of(mandatoryTags.NAME));
        try {
            AddonClassLoader.asDescription(yml);
        } catch (InvalidAddonDescriptionException e) {
            assertEquals("AddonException : Missing 'name' tag. An addon name must be listed in addon.yml", e.getMessage());
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#asDescription(org.bukkit.configuration.file.YamlConfiguration)}.
     */
    @Test
    public void testAsDescriptionNoAuthors() {
        YamlConfiguration yml = this.getYaml(List.of(mandatoryTags.AUTHORS));
        try {
            AddonClassLoader.asDescription(yml);
        } catch (InvalidAddonDescriptionException e) {
            assertEquals("AddonException : Missing 'authors' tag. At least one author must be listed in addon.yml", e.getMessage());
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#asDescription(org.bukkit.configuration.file.YamlConfiguration)}.
     */
    @Test
    public void testAsDescriptionNoVersion() {
        YamlConfiguration yml = this.getYaml(List.of(mandatoryTags.VERSION));
        try {
            AddonClassLoader.asDescription(yml);
        } catch (InvalidAddonDescriptionException e) {
            assertEquals("AddonException : Missing 'version' tag. A version must be listed in addon.yml", e.getMessage());
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#asDescription(org.bukkit.configuration.file.YamlConfiguration)}.
     */
    @Test
    public void testAsDescriptionNoMain() {
        YamlConfiguration yml = this.getYaml(List.of(mandatoryTags.MAIN));
        try {
            AddonClassLoader.asDescription(yml);
        } catch (InvalidAddonDescriptionException e) {
            assertEquals("AddonException : Missing 'main' tag. A main class must be listed in addon.yml", e.getMessage());
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#asDescription(org.bukkit.configuration.file.YamlConfiguration)}.
     */
    @Test
    public void testAsDescriptionUnknownIconMaterial() {
        YamlConfiguration yml = this.getYaml(List.of(mandatoryTags.ICON));
        try {
            AddonClassLoader.asDescription(yml);
        } catch (InvalidAddonDescriptionException e) {
            assertEquals("AddonException : 'icon' tag refers to an unknown Material: unkOwnMateriaL", e.getMessage());
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#findClass(java.lang.String)}.
     */
    @Test
    public void testFindClassString() throws IOException {
        acl = new AddonClassLoader(testAddon, am, jarFile);
        assertNull(acl.findClass(""));
        assertNull(acl.findClass("world.bentobox.bentobox"));
        acl.close();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#findClass(java.lang.String, boolean)}.
     */
    @Test
    public void testFindClassStringBoolean() throws IOException {
        acl = new AddonClassLoader(testAddon, am, jarFile);
        assertNull(acl.findClass("", false));
        assertNull(acl.findClass("world.bentobox.bentobox", false));
        acl.close();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#getAddon()}.
     */
    @Test
    public void testGetAddon() throws IOException {
        acl = new AddonClassLoader(testAddon, am, jarFile);
        Addon addon = acl.getAddon();
        assertEquals(addon, testAddon);
        acl.close();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.addons.AddonClassLoader#getClasses()}.
     */
    @Test
    public void testGetClasses() throws IOException {
        acl = new AddonClassLoader(testAddon, am, jarFile);
        Set<String> set = acl.getClasses();
        assertTrue(set.isEmpty());
        acl.close();
    }

}
