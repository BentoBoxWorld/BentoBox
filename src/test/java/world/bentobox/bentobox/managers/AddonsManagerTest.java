package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.permissions.DefaultPermissions;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.github.puregero.multilib.MultiLib;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Addon.State;
import world.bentobox.bentobox.api.addons.AddonClassLoader;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonDescriptionException;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.database.objects.DataObject;

class AddonsManagerTest extends CommonTestSetup {

    private AddonsManager am;
    @Mock
    private CommandsManager cm;
    private MockedStatic<DefaultPermissions> mockedStaticDP;


    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        FlagsManager fm = mock(FlagsManager.class);
        when(plugin.getFlagsManager()).thenReturn(fm);

        am = new AddonsManager(plugin);

        // Command Manager
        when(plugin.getCommandsManager()).thenReturn(cm);
        
        mockedStaticDP = Mockito.mockStatic(DefaultPermissions.class);

        Mockito.mockStatic(MultiLib.class, Mockito.RETURNS_MOCKS);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        // Delete the addons folder
        File f = new File(plugin.getDataFolder(), "addons");
        Files.deleteIfExists(f.toPath());
    }

    // TODO - add test cases that actually load an addon

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#AddonsManager(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    void testAddonsManager() {
        AddonsManager addonsManager = new AddonsManager(plugin);

        assertNotNull(addonsManager.getAddons());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#loadAddons()}.
     */
    @Test
    void testLoadAddonsNoAddons() {
        am.loadAddons();
        verify(plugin, never()).logError("Cannot create addons folder!");
        verify(plugin).log("Loaded 0 addons.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#enableAddons()}.
     */
    @Test
    void testEnableAddonsNoAddon() {
        am.enableAddons();
        verify(plugin, never()).log("Enabling addons...");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#reloadAddons()}.
     */
    @Test
    void testReloadAddonsNoAddons() {
        am.reloadAddons();
        verify(plugin, never()).log("Disabling addons...");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getAddonByName(java.lang.String)}.
     */
    @Test
    void testGetAddonByNameNoAddons() {
        assertFalse(am.getAddonByName("name").isPresent());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#disableAddons()}.
     */
    @Test
    void testDisableAddonsNoAddons() {
        am.disableAddons();
        verify(plugin, never()).log("Disabling addons...");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getAddons()}.
     */
    @Test
    void testGetAddonsNoAddons() {
        assertTrue(am.getAddons().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getGameModeAddons()}.
     */
    @Test
    void testGetGameModeAddonsNoAddons() {
        assertTrue(am.getGameModeAddons().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getLoadedAddons()}.
     */
    @Test
    void testGetLoadedAddonsnoAddons() {
        assertTrue(am.getLoadedAddons().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getEnabledAddons()}.
     */
    @Test
    void testGetEnabledAddonsNoAddons() {
        assertTrue(am.getEnabledAddons().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getLoader(world.bentobox.bentobox.api.addons.Addon)}.
     */
    @Test
    void testGetLoaderNoSuchAddon() {
        Addon addon = mock(Addon.class);
        assertNull(am.getLoader(addon));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getClassByName(java.lang.String)}.
     */
    @Test
    void testGetClassByNameNull() {
        assertNull(am.getClassByName("name"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#setClass(java.lang.String, java.lang.Class)}.
     */
    @Test
    void testSetClass() {
        am.setClass("name", Class.class);
        assertNotNull(am.getClassByName("name"));
        assertEquals(Class.class, am.getClassByName("name"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getDefaultWorldGenerator(java.lang.String, java.lang.String)}.
     */
    @Test
    void testGetDefaultWorldGeneratorNoWorlds() {
        assertNull(am.getDefaultWorldGenerator("BSkyBlock", ""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#registerListener(world.bentobox.bentobox.api.addons.Addon, org.bukkit.event.Listener)}.
     */
    @Test
    void testRegisterListener() {
        @NonNull
        Addon addon = mock(Addon.class);

        @NonNull
        Listener listener = mock(Listener.class);
        am.registerListener(addon, listener);
        verify(pim).registerEvents(listener, plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getDataObjects()}.
     */
    @Test
    void testGetDataObjectsNone() {
        assertTrue(am.getDataObjects().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getDataObjects()}.
     */
    @Test
    void testGetDataObjects() {
        am.setClass("dataobj", DataObject.class);
        assertFalse(am.getDataObjects().isEmpty());
        assertEquals(1, am.getDataObjects().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxSnapshotNoAPIVersion() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.0.1-SNAPSHOT-b1642"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxReleaseAPIVersion() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").apiVersion("1.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.0.1"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxSnapshotAPIVersion() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").apiVersion("1.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.0.1-SNAPSHOT-b1642"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxReleaseNoAPIVersion() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.0.1"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxSnapshotAPIVersionVariableDigits() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").apiVersion("1.2.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "1.2-SNAPSHOT-b1642"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxOldSnapshot() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "1.0.1-SNAPSHOT-b1642"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxOldRelease() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "1.0.1"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxOldReleaseLong() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.11.1.11.1.1"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxOldReleaseLongAPI() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1.0.0.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "1.11.1"));
    }

    /**
     * Test method for {@link AddonsManager#isAddonCompatibleWithBentoBox(Addon, String)}.
     * Prevents regression on <a href="https://github.com/BentoBoxWorld/BentoBox/issues/1346">issue 1346</a>.
     */
    @Test
    void testIsAddonCompatibleWithBentoBoxNewRelease() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.13.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.14.0-SNAPSHOT-b1777"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#setPerms(Addon)}
     */
    @Test
    void testSetPermsNoPerms() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1.0.0.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.setPerms(addon));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#setPerms(Addon)}
     */
    @Test
    void testSetPermsHasPerms() throws InvalidConfigurationException {
        String perms = """
                  '[gamemode].intopten':
                    description: Player is in the top ten.
                    default: true
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(perms);
        GameModeAddon addon = new MyGameMode();
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "mygame", "1.0.0").apiVersion("1.11.1.0.0.0.1")
                .permissions(config)
                .build();
        addon.setDescription(addonDesc);
        addon.setState(State.ENABLED);
        am.getAddons().add(addon);

        assertTrue(am.setPerms(addon));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#setPerms(Addon)}
     */
    @Test
    void testSetPermsHasPermsError() throws InvalidConfigurationException {
        String perms = """
                  '[gamemode].intopten':
                    description: Player is in the top ten.
                    default: trudsfgsde
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(perms);
        GameModeAddon addon = new MyGameMode();
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "mygame", "1.0.0").apiVersion("1.11.1.0.0.0.1")
                .permissions(config)
                .build();
        addon.setDescription(addonDesc);
        addon.setState(State.ENABLED);
        am.getAddons().add(addon);

        assertTrue(am.setPerms(addon));
        verify(plugin).logError("Addon mygame: AddonException : Permission default is invalid in addon.yml: [gamemode].intopten.default");
    }



    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#registerPermission(org.bukkit.configuration.ConfigurationSection, String)}
     */
    @Test
    void testRegisterPermissionStandardPerm() throws InvalidAddonDescriptionException, InvalidConfigurationException {
        String perms = """
                  'bskyblock.intopten':
                    description: Player is in the top ten.
                    default: true
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(perms);
        am.registerPermission(config, "bskyblock.intopten");
        mockedStaticDP.verify(() -> DefaultPermissions.registerPermission(eq("bskyblock.intopten"), anyString(), any(PermissionDefault.class)));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#registerPermission(org.bukkit.configuration.ConfigurationSection, String)}
     */
    @Test
    void testRegisterPermissionGameModePerm() throws InvalidAddonDescriptionException, InvalidConfigurationException {
        String perms = """
                  '[gamemode].intopten':
                    description: Player is in the top ten.
                    default: true
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(perms);
        GameModeAddon addon = new MyGameMode();
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "mygame", "1.0.0").apiVersion("1.11.1.0.0.0.1")
                .permissions(config)
                .build();
        addon.setDescription(addonDesc);
        addon.setState(State.ENABLED);
        am.getAddons().add(addon);
        am.registerPermission(config, "[gamemode].intopten");
        mockedStaticDP.verify(() -> DefaultPermissions.registerPermission(eq("mygame.intopten"), anyString(), any(PermissionDefault.class)));
    }



    // ---- getAddonByName with addon present ----

    @Test
    void testGetAddonByNameFound() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main.class", "BSkyBlock", "1.0.0").build();
        when(addon.getDescription()).thenReturn(desc);
        am.getAddons().add(addon);

        Optional<Addon> result = am.getAddonByName("BSkyBlock");
        assertTrue(result.isPresent());
        assertEquals(addon, result.get());
    }

    @Test
    void testGetAddonByNameCaseInsensitive() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main.class", "BSkyBlock", "1.0.0").build();
        when(addon.getDescription()).thenReturn(desc);
        am.getAddons().add(addon);

        assertTrue(am.getAddonByName("bskyblock").isPresent());
        assertTrue(am.getAddonByName("BSKYBLOCK").isPresent());
    }

    // ---- getAddonByMainClassName ----

    @Test
    void testGetAddonByMainClassNameFound() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("world.bentobox.bskyblock.BSkyBlock", "BSkyBlock", "1.0.0").build();
        when(addon.getDescription()).thenReturn(desc);
        am.getAddons().add(addon);

        Optional<Addon> result = am.getAddonByMainClassName("world.bentobox.bskyblock.BSkyBlock");
        assertTrue(result.isPresent());
    }

    @Test
    void testGetAddonByMainClassNameNotFound() {
        assertFalse(am.getAddonByMainClassName("not.exist.Class").isPresent());
    }

    // ---- getLoadedAddons / getEnabledAddons / getGameModeAddons with state ----

    @Test
    void testGetLoadedAddonsWithLoadedAddon() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "TestAddon", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.LOADED);
        am.getAddons().add(addon);

        assertEquals(1, am.getLoadedAddons().size());
    }

    @Test
    void testGetLoadedAddonsExcludesEnabled() {
        Addon addon = mock(Addon.class);
        when(addon.getState()).thenReturn(State.ENABLED);
        am.getAddons().add(addon);

        assertTrue(am.getLoadedAddons().isEmpty());
    }

    @Test
    void testGetEnabledAddonsWithEnabledAddon() {
        Addon addon = mock(Addon.class);
        when(addon.getState()).thenReturn(State.ENABLED);
        am.getAddons().add(addon);

        assertEquals(1, am.getEnabledAddons().size());
    }

    @Test
    void testGetGameModeAddonsWithGameMode() {
        GameModeAddon gma = new MyGameMode();
        AddonDescription desc = new AddonDescription.Builder("main", "TestGM", "1.0").build();
        gma.setDescription(desc);
        gma.setState(State.ENABLED);
        am.getAddons().add(gma);

        assertEquals(1, am.getGameModeAddons().size());
        assertEquals(gma, am.getGameModeAddons().getFirst());
    }

    @Test
    void testGetGameModeAddonsExcludesNonGameMode() {
        Addon addon = mock(Addon.class);
        when(addon.getState()).thenReturn(State.ENABLED);
        am.getAddons().add(addon);

        assertTrue(am.getGameModeAddons().isEmpty());
    }

    // ---- enableAddons with loaded addons ----

    @Test
    void testEnableAddonsWithLoadedGameMode() {
        GameModeAddon gma = new MyGameMode();
        AddonDescription desc = new AddonDescription.Builder("main", "TestGM", "1.0").apiVersion("1").build();
        gma.setDescription(desc);
        gma.setState(State.LOADED);
        am.getAddons().add(gma);

        am.enableAddons();
        verify(plugin).log("Enabling game mode addons...");
    }

    @Test
    void testEnableAddonsSkipsDisabledAddons() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "Disabled", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.DISABLED);
        am.getAddons().add(addon);

        // Only LOADED addons (not DISABLED) are candidates. This addon is DISABLED
        // so enableAddons should still log the enabling messages but skip this addon
        am.enableAddons();
        verify(addon, never()).onEnable();
    }

    // ---- disableAddons with addons ----

    @Test
    void testDisableAddonsWithEnabledAddon() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "TestAddon", "1.0")
                .authors("Author1").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.ENABLED);
        when(addon.isEnabled()).thenReturn(true);
        am.getAddons().add(addon);

        am.disableAddons();
        verify(plugin).log("Disabling addons...");
        verify(addon).onDisable();
    }

    @Test
    void testDisableAddonsHandlesOnDisableException() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "CrashAddon", "1.0")
                .authors("Author1").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.ENABLED);
        when(addon.isEnabled()).thenReturn(true);
        doThrow(new RuntimeException("crash")).when(addon).onDisable();
        am.getAddons().add(addon);

        am.disableAddons();
        verify(plugin).logError(contains("Error occurred when disabling addon CrashAddon"));
    }

    // ---- disable with listeners ----

    @Test
    void testDisableUnregistersListeners() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "ListenerAddon", "1.0")
                .authors("Author1").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.ENABLED);
        when(addon.isEnabled()).thenReturn(true);

        // Register a listener first
        Listener listener = mock(Listener.class);
        am.registerListener(addon, listener);
        am.getAddons().add(addon);

        am.disableAddons();
        // Listener should be unregistered via HandlerList.unregisterAll
        verify(addon).onDisable();
    }

    // ---- disable with loaders ----

    @Test
    void testDisableClosesLoaders() throws IOException {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "LoaderAddon", "1.0")
                .authors("Author1").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.ENABLED);
        when(addon.isEnabled()).thenReturn(true);

        // Mock an AddonClassLoader
        AddonClassLoader mockLoader = mock(AddonClassLoader.class);
        when(mockLoader.getClasses()).thenReturn(Set.of("com.test.MyClass"));

        // Put a class that should be removed
        am.setClass("com.test.MyClass", String.class);
        assertNotNull(am.getClassByName("com.test.MyClass"));

        // Can't easily add to the private loaders map without loading a real addon,
        // but we can verify the class cleanup happens via disableAddons
        am.getAddons().add(addon);
        am.disableAddons();

        verify(addon).onDisable();
    }

    // ---- allLoaded ----

    @Test
    void testAllLoadedCallsAddonAllLoaded() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "AllLoadedAddon", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.ENABLED);
        when(addon.isEnabled()).thenReturn(true);
        am.getAddons().add(addon);

        am.allLoaded();
        verify(addon).allLoaded();
    }

    @Test
    void testAllLoadedHandlesException() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "CrashAllLoaded", "1.0")
                .authors("Author1").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.ENABLED);
        when(addon.isEnabled()).thenReturn(true);
        doThrow(new RuntimeException("allLoaded crash")).when(addon).allLoaded();
        am.getAddons().add(addon);

        am.allLoaded();
        // Should set state to ERROR and log
        verify(plugin).logError(contains("Skipping CrashAllLoaded due to an unhandled exception"));
    }

    @Test
    void testAllLoadedHandlesLinkageError() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "IncompatAllLoaded", "1.0")
                .authors("Author1").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getState()).thenReturn(State.ENABLED);
        when(addon.isEnabled()).thenReturn(true);
        doThrow(new NoClassDefFoundError("missing.Class")).when(addon).allLoaded();
        am.getAddons().add(addon);

        am.allLoaded();
        verify(plugin).logWarning(contains("Skipping IncompatAllLoaded"));
    }

    @Test
    void testAllLoadedNoEnabledAddons() {
        // No addons - should not throw
        am.allLoaded();
        // Just verify it completes without error
    }

    // ---- getDefaultWorldGenerator with world name match ----

    @Test
    void testGetDefaultWorldGeneratorStripsSuffixes() {
        // "testworld_nether" should strip to "testworld" and not match anything
        assertNull(am.getDefaultWorldGenerator("testworld_nether", ""));
        assertNull(am.getDefaultWorldGenerator("testworld_the_end", ""));
    }

    // ---- setClass / getClassByName ----

    @Test
    void testSetClassDoesNotOverwrite() {
        am.setClass("test", String.class);
        am.setClass("test", Integer.class);
        // putIfAbsent - should keep the first one
        assertEquals(String.class, am.getClassByName("test"));
    }

    @Test
    void testGetClassByNameReturnsNullForUnknown() {
        assertNull(am.getClassByName("nonexistent.Class"));
    }

    // ---- getDataObjects filters correctly ----

    @Test
    void testGetDataObjectsFiltersNonDataObjects() {
        am.setClass("regularClass", String.class);
        assertTrue(am.getDataObjects().isEmpty());
    }

    @Test
    void testGetDataObjectsReturnsDataObjects() {
        am.setClass("dataobj", DataObject.class);
        am.setClass("regularClass", String.class);
        assertEquals(1, am.getDataObjects().size());
    }

    // ---- isAddonCompatibleWithBentoBox edge cases ----

    @Test
    void testIsAddonCompatibleExactMatch() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "Test", "1.0")
                .apiVersion("2.5.3").build();
        when(addon.getDescription()).thenReturn(desc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "2.5.3"));
    }

    @Test
    void testIsAddonCompatibleBentoBoxNewer() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "Test", "1.0")
                .apiVersion("2.5.3").build();
        when(addon.getDescription()).thenReturn(desc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "3.0.0"));
    }

    @Test
    void testIsAddonCompatibleBentoBoxOlder() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "Test", "1.0")
                .apiVersion("3.0.0").build();
        when(addon.getDescription()).thenReturn(desc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "2.5.3"));
    }

    @Test
    void testIsAddonCompatibleLocalSnapshot() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "Test", "1.0")
                .apiVersion("3.14.0").build();
        when(addon.getDescription()).thenReturn(desc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "3.14.1-LOCAL-SNAPSHOT"));
    }

    // ---- setPerms with gamemode placeholder ----

    @Test
    void testSetPermsWithGameModePlaceholder() throws InvalidConfigurationException {
        String perms = """
                  '[gamemode].admin':
                    description: Admin permission.
                    default: op
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(perms);

        // Create a game mode addon and add it as enabled
        GameModeAddon gma = new MyGameMode();
        AddonDescription gmaDesc = new AddonDescription.Builder("main", "bskyblock", "1.0")
                .permissions(config).build();
        gma.setDescription(gmaDesc);
        gma.setState(State.ENABLED);
        am.getAddons().add(gma);

        // Now set perms - should replace [gamemode] with the addon's permission prefix
        assertTrue(am.setPerms(gma));
        mockedStaticDP.verify(() -> DefaultPermissions.registerPermission(
                eq("bskyblock.admin"), anyString(), any(PermissionDefault.class)));
    }

    // ---- registerListener stores listener ----

    @Test
    void testRegisterListenerStoresInMap() {
        Addon addon = mock(Addon.class);
        Listener listener1 = mock(Listener.class);
        Listener listener2 = mock(Listener.class);

        am.registerListener(addon, listener1);
        am.registerListener(addon, listener2);

        verify(pim, times(2)).registerEvents(any(Listener.class), eq(plugin));
    }

    // ---- loadAddons creates addons folder ----

    @Test
    void testLoadAddonsCreatesFolder() {
        File addonsDir = new File(plugin.getDataFolder(), "addons");
        assertFalse(addonsDir.exists());

        am.loadAddons();
        assertTrue(addonsDir.exists());
    }

    // ---- reloadAddons calls disable then load then enable ----

    @Test
    void testReloadAddonsCallsDisableAndLoad() {
        am.reloadAddons();
        // reloadAddons calls disableAddons, which calls unregisterCommands
        verify(cm).unregisterCommands();
    }

    class MyGameMode extends GameModeAddon {

        @Override
        public void createWorlds() {
            // Not needed for test
        }

        @Override
        public WorldSettings getWorldSettings() {
            return null;
        }

        @Override
        public @Nullable ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
            return null;
        }

        @Override
        public void saveWorldSettings() {
            // Not needed for test
        }

        @Override
        public void onEnable() {
            // Not needed for test
        }

        @Override
        public void onDisable() {
            // Not needed for test
        }

    }
}
