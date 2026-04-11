package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.localization.BentoBoxLocale;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
class LocalesManagerTest  extends CommonTestSetup {

    private static final String LOCALE_FOLDER = "locales";
    private static final String BENTOBOX = "BentoBox";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Makes fake English and French local files
     * @throws IOException if the file saving fails
     */
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
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#LocalesManager(BentoBox)}.
     */
    @Test
    void testConstructor() {
        new LocalesManager(plugin);
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        assertTrue(localeDir.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(java.lang.String)}.
     */
    @Test
    void testGetString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertEquals("test string", lm.get("test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(java.lang.String)}.
     */
    @Test
    void testGetStringFail() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertNull(lm.get("test.test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getOrDefault(java.lang.String, java.lang.String)}.
     */
    @Test
    void testGetOrDefaultStringString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertEquals("test string", lm.getOrDefault("test.test", ""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getOrDefault(java.lang.String, java.lang.String)}.
     */
    @Test
    void testGetOrDefaultStringStringFail() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertEquals("", lm.getOrDefault("test.test.test",""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    void testGetNullUserString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = null;
        assertEquals("test string", lm.get(user, "test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    void testGetUserString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        assertEquals("test string", lm.get(user, "test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getOrDefault(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.String)}.
     */
    @Test
    void testGetOrDefaultUserStringString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        assertEquals("test string", lm.getOrDefault(user, "test.test", ""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    void testGetCanadianUserString() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.CANADA_FRENCH);
        assertEquals("test string", lm.get(user, "test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#get(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    void testGetUserStringFail() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        assertNull(lm.get(user, "test.test.test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getOrDefault(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.String)}.
     */
    @Test
    void testGetOrDefaultUserStringStringFail() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        assertEquals("", lm.getOrDefault(user, "test.test.test", ""));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getAvailableLocales(boolean)}.
     */
    @Test
    void testGetAvailableLocales() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);

        // Unsorted
        List<Locale> localeList = lm.getAvailableLocales(false);
        assertEquals(Locale.FRANCE, localeList.getFirst());
        assertEquals(Locale.US, localeList.get(1));
        // Sorted
        localeList = lm.getAvailableLocales(true);
        assertEquals(Locale.US, localeList.getFirst());
        assertEquals(Locale.FRANCE, localeList.get(1));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#getLanguages()}.
     */
    @Test
    void testGetLanguages() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        lm.getLanguages().forEach((k,v) -> assertEquals(k.toLanguageTag(), v.toLanguageTag()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#reloadLanguages()}.
     */
    @Test
    void testReloadLanguagesNoAddons() throws IOException {
        AddonsManager am = mock(AddonsManager.class);
        List<Addon> none = new ArrayList<>();
        when(am.getAddons()).thenReturn(none);
        when(plugin.getAddonsManager()).thenReturn(am);
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        lm.reloadLanguages();
        verify(am).getAddons();
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        assertTrue(localeDir.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#reloadLanguages()}.
     */
    @Test
    void testReloadLanguages() throws IOException {
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
        verify(addon).saveResource(
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
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source)))
        {
            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
        }
        target.closeEntry();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#reloadLanguages()}.
     */
    @Test
    void testReloadLanguagesNoLocaleFolder() {
        AddonsManager am = mock(AddonsManager.class);
        List<Addon> none = new ArrayList<>();
        when(am.getAddons()).thenReturn(none);
        when(plugin.getAddonsManager()).thenReturn(am);
        LocalesManager lm = new LocalesManager(plugin);
        lm.reloadLanguages();
        verify(am).getAddons();
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        assertTrue(localeDir.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#setTranslation(Locale, String, String)}.
     */
    @Test
    void testSetTranslationUnknownLocale() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertFalse(lm.setTranslation(Locale.GERMAN, "anything.ref", "a translation"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.LocalesManager#setTranslation(Locale, String, String)}.
     */
    @Test
    void testSetTranslationKnownLocale() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertEquals("test string", lm.get("test.test"));
        assertTrue(lm.setTranslation(Locale.US, "test.test", "a translation"));
        assertEquals("a translation", lm.get("test.test"));
    }

    // ---- isLocaleAvailable ----

    @Test
    void testIsLocaleAvailableTrue() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertTrue(lm.isLocaleAvailable(Locale.US));
    }

    @Test
    void testIsLocaleAvailableFalse() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        assertFalse(lm.isLocaleAvailable(Locale.JAPANESE));
    }

    // ---- getAvailablePrefixes ----

    /**
     * Makes a locale file that includes a prefixes section
     */
    private void makeFakeLocaleFileWithPrefixes() throws IOException {
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        localeDir.mkdirs();
        File english = new File(localeDir, Locale.US.toLanguageTag() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("test.test", "test string");
        yaml.set("prefixes.bskyblock", "[BSkyBlock]");
        yaml.set("prefixes.acidisland", "[AcidIsland]");
        yaml.save(english);

        File french = new File(localeDir, Locale.FRANCE.toLanguageTag() + ".yml");
        YamlConfiguration frYaml = new YamlConfiguration();
        frYaml.set("test.test", "chaîne de test");
        frYaml.set("prefixes.bskyblock", "[BSkyBlock-FR]");
        frYaml.save(french);
    }

    @Test
    void testGetAvailablePrefixes() throws IOException {
        makeFakeLocaleFileWithPrefixes();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.US);
        java.util.Set<String> prefixes = lm.getAvailablePrefixes(user);
        assertNotNull(prefixes);
        assertTrue(prefixes.contains("bskyblock"));
        assertTrue(prefixes.contains("acidisland"));
    }

    @Test
    void testGetAvailablePrefixesUnknownUserLocale() throws IOException {
        makeFakeLocaleFileWithPrefixes();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        // User has a locale that is not loaded
        when(user.getLocale()).thenReturn(Locale.JAPANESE);
        java.util.Set<String> prefixes = lm.getAvailablePrefixes(user);
        assertNotNull(prefixes);
        // Should still contain prefixes from en-US and server default
        assertTrue(prefixes.contains("bskyblock"));
    }

    @Test
    void testGetAvailablePrefixesFrenchUser() throws IOException {
        makeFakeLocaleFileWithPrefixes();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.FRANCE);
        java.util.Set<String> prefixes = lm.getAvailablePrefixes(user);
        assertNotNull(prefixes);
        // Should contain prefixes from all locales
        assertTrue(prefixes.contains("bskyblock"));
        assertTrue(prefixes.contains("acidisland"));
    }

    // ---- get(String) with server default language different from en-US ----

    @Test
    void testGetStringFromServerDefaultLocale() throws IOException {
        // Create locale files with different content for a non-US server default
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        localeDir.mkdirs();

        // en-US file
        File english = new File(localeDir, "en-US.yml");
        YamlConfiguration enYaml = new YamlConfiguration();
        enYaml.set("shared.key", "english value");
        enYaml.set("english.only", "only in english");
        enYaml.save(english);

        // fr-FR file with a different translation
        File french = new File(localeDir, "fr-FR.yml");
        YamlConfiguration frYaml = new YamlConfiguration();
        frYaml.set("shared.key", "valeur française");
        frYaml.set("french.only", "seulement en français");
        frYaml.save(french);

        // Set server default to fr-FR
        Settings settings = new Settings();
        settings.setDefaultLanguage("fr-FR");
        when(plugin.getSettings()).thenReturn(settings);

        LocalesManager lm = new LocalesManager(plugin);

        // Should return from the server default language (fr-FR)
        assertEquals("valeur française", lm.get("shared.key"));
        // Key only in French should be found via server default
        assertEquals("seulement en français", lm.get("french.only"));
        // Key only in English - not in fr-FR, so falls through to en-US
        assertEquals("only in english", lm.get("english.only"));
    }

    // ---- get(User, String) when user locale has translation ----

    @Test
    void testGetUserStringFromUserLocale() throws IOException {
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        localeDir.mkdirs();

        File english = new File(localeDir, "en-US.yml");
        YamlConfiguration enYaml = new YamlConfiguration();
        enYaml.set("greeting", "Hello");
        enYaml.save(english);

        File french = new File(localeDir, "fr-FR.yml");
        YamlConfiguration frYaml = new YamlConfiguration();
        frYaml.set("greeting", "Bonjour");
        frYaml.save(french);

        LocalesManager lm = new LocalesManager(plugin);
        User frenchUser = mock(User.class);
        when(frenchUser.getLocale()).thenReturn(Locale.FRANCE);

        // Should get translation from the user's locale (French)
        assertEquals("Bonjour", lm.get(frenchUser, "greeting"));
    }

    @Test
    void testGetUserStringFallsBackToServerDefault() throws IOException {
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        localeDir.mkdirs();

        File english = new File(localeDir, "en-US.yml");
        YamlConfiguration enYaml = new YamlConfiguration();
        enYaml.set("greeting", "Hello");
        enYaml.set("farewell", "Goodbye");
        enYaml.save(english);

        File french = new File(localeDir, "fr-FR.yml");
        YamlConfiguration frYaml = new YamlConfiguration();
        frYaml.set("greeting", "Bonjour");
        // No "farewell" in French
        frYaml.save(french);

        LocalesManager lm = new LocalesManager(plugin);
        User frenchUser = mock(User.class);
        when(frenchUser.getLocale()).thenReturn(Locale.FRANCE);

        // "farewell" not in French, should fall back to en-US
        assertEquals("Goodbye", lm.get(frenchUser, "farewell"));
    }

    // ---- getOrDefault(User, ...) ----

    @Test
    void testGetOrDefaultUserFallsBackToDefault() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        User user = mock(User.class);
        when(user.getLocale()).thenReturn(Locale.JAPANESE); // not loaded
        assertEquals("fallback", lm.getOrDefault(user, "nonexistent.key", "fallback"));
    }

    // ---- getAvailableLocales sorting edge cases ----

    @Test
    void testGetAvailableLocalesSortDefaultFirst() throws IOException {
        // Create 3 locale files: en-US, fr-FR, de-DE
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        localeDir.mkdirs();

        for (String tag : new String[]{"en-US", "fr-FR", "de-DE"}) {
            File f = new File(localeDir, tag + ".yml");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("test", tag);
            yaml.save(f);
        }

        LocalesManager lm = new LocalesManager(plugin);
        List<Locale> sorted = lm.getAvailableLocales(true);

        // en-US is the default language in Settings, so it should be first
        assertEquals(Locale.US, sorted.get(0));
        // Remaining English locales are second
        assertEquals(3, sorted.size());
    }

    // ---- loadLocalesFromFile with invalid tag ----

    @Test
    void testLoadLocalesFromFileInvalidTag() throws IOException {
        // Create a locale file with underscore separator (invalid BCP-47)
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        localeDir.mkdirs();

        // Valid file
        File english = new File(localeDir, "en-US.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("test", "value");
        yaml.save(english);

        // Invalid tag file - uses underscore
        File invalid = new File(localeDir, "zh_CN.yml");
        YamlConfiguration invalidYaml = new YamlConfiguration();
        invalidYaml.set("test", "value");
        invalidYaml.save(invalid);

        LocalesManager lm = new LocalesManager(plugin);

        // Should have loaded en-US but skipped zh_CN
        assertTrue(lm.isLocaleAvailable(Locale.US));
        // zh_CN with underscore yields Locale.ROOT which has empty language
        assertFalse(lm.isLocaleAvailable(Locale.ROOT));
        verify(plugin).logWarning(Mockito.contains("zh_CN"));
    }

    // ---- loadLocalesFromFile with nonexistent folder ----

    @Test
    void testLoadLocalesFromFileNonexistentFolder() {
        LocalesManager lm = new LocalesManager(plugin);
        // Should not throw
        lm.loadLocalesFromFile("NonExistentAddon");
    }

    // ---- getLanguages returns correct map ----

    @Test
    void testGetLanguagesMap() throws IOException {
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);
        java.util.Map<Locale, BentoBoxLocale> languages = lm.getLanguages();
        assertNotNull(languages);
        assertTrue(languages.containsKey(Locale.US));
        assertTrue(languages.containsKey(Locale.FRANCE));
        assertEquals(2, languages.size());
    }

    // ---- loadLocalesFromFile merges into existing locale ----

    @Test
    void testLoadLocalesFromFileMerge() throws IOException {
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        localeDir.mkdirs();

        File english = new File(localeDir, "en-US.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("key1", "value1");
        yaml.save(english);

        LocalesManager lm = new LocalesManager(plugin);
        assertEquals("value1", lm.get("key1"));

        // Now add a second file with more keys in a different addon folder
        File addonDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + "TestAddon");
        addonDir.mkdirs();
        File addonEnglish = new File(addonDir, "en-US.yml");
        YamlConfiguration addonYaml = new YamlConfiguration();
        addonYaml.set("key2", "value2");
        addonYaml.save(addonEnglish);

        lm.loadLocalesFromFile("TestAddon");

        // Both keys should be accessible
        assertEquals("value1", lm.get("key1"));
        assertEquals("value2", lm.get("key2"));
    }

    // ---- copyFile (via constructor) doesn't overwrite existing ----

    @Test
    void testCopyFileDoesNotOverwrite() throws IOException {
        // Create locale files manually first
        makeFakeLocaleFile();
        LocalesManager lm = new LocalesManager(plugin);

        // Get the current translation
        assertEquals("test string", lm.get("test.test"));

        // Modify the file
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + BENTOBOX);
        File english = new File(localeDir, "en-US.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("test.test", "modified value");
        yaml.save(english);

        // Create a new LocalesManager - should NOT overwrite the modified file
        LocalesManager lm2 = new LocalesManager(plugin);
        assertEquals("modified value", lm2.get("test.test"));
    }
}
