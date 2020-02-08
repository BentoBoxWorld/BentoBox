package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.DataObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {Bukkit.class, BentoBox.class} )
public class AddonsManagerTest {

    private BentoBox plugin;
    private AddonsManager am;
    @Mock
    private PluginManager pm;
    @Mock
    private CommandsManager cm;

    /**
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pm);
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        FlagsManager fm = mock(FlagsManager.class);
        when(plugin.getFlagsManager()).thenReturn(fm);

        am = new AddonsManager(plugin);

        // Command Manager
        when(plugin.getCommandsManager()).thenReturn(cm);
        Settings s = mock(Settings.class);
        when(s.getDatabaseType()).thenReturn(DatabaseType.MYSQL);
        // settings
        when(plugin.getSettings()).thenReturn(s);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // Delete the addons folder
        File f = new File(plugin.getDataFolder(), "addons");
        Files.deleteIfExists(f.toPath());
        Mockito.framework().clearInlineMocks();
    }

    // TODO - add test cases that actually load an addon

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#AddonsManager(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testAddonsManager() {
        AddonsManager addonsManager = new AddonsManager(plugin);

        assertNotNull(addonsManager.getAddons());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#loadAddons()}.
     */
    @Test
    public void testLoadAddonsNoAddons() {
        am.loadAddons();
        verify(plugin, never()).logError("Cannot create addons folder!");
        verify(plugin).log("Loaded 0 addons.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#enableAddons()}.
     */
    @Test
    public void testEnableAddonsNoAddon() {
        am.enableAddons();
        verify(plugin, never()).log("Enabling addons...");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#reloadAddons()}.
     */
    @Test
    public void testReloadAddonsNoAddons() {
        am.reloadAddons();
        verify(plugin, never()).log("Disabling addons...");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getAddonByName(java.lang.String)}.
     */
    @Test
    public void testGetAddonByNameNoAddons() {
        assertFalse(am.getAddonByName("name").isPresent());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#disableAddons()}.
     */
    @Test
    public void testDisableAddonsNoAddons() {
        am.disableAddons();
        verify(plugin, never()).log("Disabling addons...");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getAddons()}.
     */
    @Test
    public void testGetAddonsNoAddons() {
        assertTrue(am.getAddons().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getGameModeAddons()}.
     */
    @Test
    public void testGetGameModeAddonsNoAddons() {
        assertTrue(am.getGameModeAddons().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getLoadedAddons()}.
     */
    @Test
    public void testGetLoadedAddonsnoAddons() {
        assertTrue(am.getLoadedAddons().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getEnabledAddons()}.
     */
    @Test
    public void testGetEnabledAddonsNoAddons() {
        assertTrue(am.getEnabledAddons().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getLoader(world.bentobox.bentobox.api.addons.Addon)}.
     */
    @Test
    public void testGetLoaderNoSuchAddon() {
        Addon addon = mock(Addon.class);
        assertNull(am.getLoader(addon));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getClassByName(java.lang.String)}.
     */
    @Test
    public void testGetClassByNameNull() {
        assertNull(am.getClassByName("name"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#setClass(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testSetClass() {
        am.setClass("name", Class.class);
        assertNotNull(am.getClassByName("name"));
        assertEquals(Class.class, am.getClassByName("name"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getDefaultWorldGenerator(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testGetDefaultWorldGeneratorNoWorlds() {
        assertNull(am.getDefaultWorldGenerator("BSkyBlock", ""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#registerListener(world.bentobox.bentobox.api.addons.Addon, org.bukkit.event.Listener)}.
     */
    @Test
    public void testRegisterListener() {
        @NonNull
        Addon addon = mock(Addon.class);

        @NonNull
        Listener listener = mock(Listener.class);
        am.registerListener(addon, listener);
        verify(pm).registerEvents(listener, plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getDataObjects()}.
     */
    @Test
    public void testGetDataObjectsNone() {
        assertTrue(am.getDataObjects().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#getDataObjects()}.
     */
    @Test
    public void testGetDataObjects() {
        am.setClass("dataobj", DataObject.class);
        assertFalse(am.getDataObjects().isEmpty());
        assertTrue(am.getDataObjects().size() == 1);
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxSnapshotNoAPIVersion() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.0.1-SNAPSHOT-b1642"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxReleaseAPIVersion() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").apiVersion("1.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.0.1"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxSnapshotAPIVersion() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").apiVersion("1.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.0.1-SNAPSHOT-b1642"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxReleaseNoAPIVersion() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.0.1"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxSnapshotAPIVersionVariableDigits() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.1").apiVersion("1.2.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "1.2-SNAPSHOT-b1642"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxOldSnapshot() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "1.0.1-SNAPSHOT-b1642"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxOldRelease() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "1.0.1"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxOldReleaseLong() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertTrue(am.isAddonCompatibleWithBentoBox(addon, "1.11.1.11.1.1"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.AddonsManager#isAddonCompatibleWithBentoBox(Addon)}.
     */
    @Test
    public void testIsAddonCompatibleWithBentoBoxOldReleaseLongAPI() {
        Addon addon = mock(Addon.class);
        AddonDescription addonDesc = new AddonDescription.Builder("main.class", "Addon-name", "1.0.0").apiVersion("1.11.1.0.0.0.1").build();
        when(addon.getDescription()).thenReturn(addonDesc);
        assertFalse(am.isAddonCompatibleWithBentoBox(addon, "1.11.1"));
    }


}
