/**
 *
 */
package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER);
        if (localeDir.exists()) {
            // Remove it
            Files.walk(localeDir.toPath())
            .map(Path::toFile)
            .sorted((o1, o2) -> -o1.compareTo(o2))
            .forEach(File::delete);

        }

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
        AddonDescription desc = new AddonDescription();
        desc.setName(BENTOBOX);
        when(addon.getDescription()).thenReturn(desc);
        none.add(addon);
        when(am.getAddons()).thenReturn(none);
        when(plugin.getAddonsManager()).thenReturn(am);
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        lm.reloadLanguages();
        Mockito.verify(addon).getDescription();
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        assertTrue(localeDir.exists());
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
