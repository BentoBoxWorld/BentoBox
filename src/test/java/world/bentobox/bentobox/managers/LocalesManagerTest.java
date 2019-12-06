package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class, BentoBox.class, Util.class })
public class LocalesManagerTest {

    private BentoBox plugin;
    private static final String LOCALE_FOLDER = "locales";
    private static final String BENTOBOX = "BentoBox";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Settings settings = mock(Settings.class);
        when(settings.getDefaultLanguage()).thenReturn(Locale.US.toLanguageTag());
        when(plugin.getSettings()).thenReturn(settings);
    }

    private void makeFakeLocaleFile() throws IOException {
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        File english = new File(localeDir, Locale.US.toLanguageTag() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("test.test", "test string");
        yaml.save(english);

        File french = new File(localeDir, Locale.FRANCE.toLanguageTag() + ".yml");
        yaml.set("test.test", "chaîne de test");
        yaml.save(french);
    }

    /**
     * Deletes the fake locales folder
     * @throws Exception
     */
    @After
    public void cleanUp() throws Exception {
        // Delete locale folder
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER);
        if (localeDir.exists()) {
            // Remove it
            Files.walk(localeDir.toPath())
            .map(Path::toFile)
            .sorted((o1, o2) -> -o1.compareTo(o2))
            .forEach(File::delete);

        }

        // Delete addon folder
        localeDir = new File(plugin.getDataFolder(), "addons");
        if (localeDir.exists()) {
            // Remove it
            Files.walk(localeDir.toPath())
            .map(Path::toFile)
            .sorted((o1, o2) -> -o1.compareTo(o2))
            .forEach(File::delete);

        }

        Mockito.framework().clearInlineMocks();

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#LocalesManager(BentoBox)}.
     */
    @Test
    public void testConstructor() {
        new LocalesManager(plugin);
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        assertTrue(localeDir.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertEquals("test string", lm.get("test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetStringFail() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertNull(lm.get("test.test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getOrDefault(java.lang.String, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetOrDefaultStringString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertEquals("test string", lm.getOrDefault("test.test", ""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getOrDefault(java.lang.String, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetOrDefaultStringStringFail() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertEquals("", lm.getOrDefault("test.test.test",""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetNullUserString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = null;
        assertEquals("test string", lm.get(user, "test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetUserString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        assertEquals("test string", lm.get(user, "test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getOrDefault(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetOrDefaultUserStringString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        assertEquals("test string", lm.getOrDefault(user, "test.test", ""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetCanadianUserString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.CANADA_FRENCH);
        assertEquals("test string", lm.get(user, "test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetUserStringFail() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        assertNull(lm.get(user, "test.test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getOrDefault(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testGetOrDefaultUserStringStringFail() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        assertEquals("", lm.getOrDefault(user, "test.test.test", ""));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getAvailableLocales(boolean)}.
     * @throws IOException
     */
    @Test
    public void testGetAvailableLocales() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);

        // Unsorted
        List<Locale> localeList = lm.getAvailableLocales(false);
        assertEquals(Locale.FRANCE, localeList.get(0));
        assertEquals(Locale.US, localeList.get(1));
        // Sorted
        localeList = lm.getAvailableLocales(true);
        assertEquals(Locale.US, localeList.get(0));
        assertEquals(Locale.FRANCE, localeList.get(1));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getLanguages()}.
     * @throws IOException
     */
    @Test
    public void testGetLanguages() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        lm.getLanguages().forEach((k,v) -> assertEquals(k.toLanguageTag(), v.toLanguageTag()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#reloadLanguages()}.
     * @throws IOException
     */
    @Test
    public void testReloadLanguagesNoAddons() throws IOException {
        AddonsManager am = mock(AddonsManager.class);
        List<Addon> none = new ArrayList<>();
        when(am.getAddons()).thenReturn(none);
        when(plugin.getAddonsManager()).thenReturn(am);
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        lm.reloadLanguages();
        Mockito.verify(am).getAddons();
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        assertTrue(localeDir.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#reloadLanguages()}.
     * @throws IOException
     */
    @Test
    public void testReloadLanguages() throws IOException {
        AddonsManager am = mock(AddonsManager.class);
        List<Addon> none = new ArrayList<>();
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("", "AcidIsland", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);
        // Create a tmp folder to jar up
        File localeDir = new File(LOCALE_FOLDER);
        localeDir.mkdirs();
        // Create a fake locale file for this jar
        File english = new File(localeDir, Locale.US.toLanguageTag() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("test.test", "test string");
        yaml.save(english);
        // Create a temporary jar file
        File jar = new File("addons", "AcidIsland.jar");
        jar.getParentFile().mkdirs();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        JarOutputStream target = new JarOutputStream(new FileOutputStream("addons" + File.separator + "AcidIsland.jar"), manifest);
        add(english, target);
        target.close();
        // When the file is requested, return it
        when(addon.getFile()).thenReturn(jar);
        none.add(addon);
        when(am.getAddons()).thenReturn(none);
        when(plugin.getAddonsManager()).thenReturn(am);
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);

        // RELOAD!!!
        lm.reloadLanguages();

        // Verify that the resources have been saved (note that they are not actually saved because addon is a mock)
        Mockito.verify(addon).saveResource(
                Mockito.eq("locales/en-US.yml"),
                Mockito.any(),
                Mockito.eq(false),
                Mockito.eq(true)
                );

        // Clean up
        // Delete the temp folder we made. Other clean up is done globally
        localeDir = new File(plugin.getDataFolder(), "AcidIsland");
        if (localeDir.exists()) {
            // Remove it
            Files.walk(localeDir.toPath())
            .map(Path::toFile)
            .sorted((o1, o2) -> -o1.compareTo(o2))
            .forEach(File::delete);
        }

    }

    private void add(File source, JarOutputStream target) throws IOException
    {
        BufferedInputStream in = null;
        try
        {
            if (source.isDirectory())
            {
                String name = source.getPath().replace("\\", "/");
                if (!name.isEmpty())
                {
                    if (!name.endsWith("/"))
                        name += "/";
                    JarEntry entry = new JarEntry(name);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile: source.listFiles())
                    add(nestedFile, target);
                return;
            }

            JarEntry entry = new JarEntry(source.getPath().replace("\\", "/"));
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        }
        finally
        {
            if (in != null)
                in.close();
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#reloadLanguages()}.
     * @throws IOException
     */
    @Test
    public void testReloadLanguagesNoLocaleFolder() throws IOException {
        AddonsManager am = mock(AddonsManager.class);
        List<Addon> none = new ArrayList<>();
        when(am.getAddons()).thenReturn(none);
        when(plugin.getAddonsManager()).thenReturn(am);
        LocalesManager lm = new LocalesManager(plugin);
        lm.reloadLanguages();
        Mockito.verify(am).getAddons();
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        assertTrue(localeDir.exists());
    }

}
