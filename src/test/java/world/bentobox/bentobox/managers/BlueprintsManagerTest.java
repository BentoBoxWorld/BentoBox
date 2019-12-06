package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintPaster;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {Bukkit.class, BentoBox.class, BlueprintPaster.class} )
public class BlueprintsManagerTest {

    public static int BUFFER_SIZE = 10240;

    @Mock
    private BentoBox plugin;
    @Mock
    private GameModeAddon addon;
    @Mock
    private Island island;
    @Mock
    private BukkitScheduler scheduler;

    private File dataFolder;
    private File jarFile;

    private TestClass test;

    private Blueprint defaultBp;

    @Mock
    private World world;

    @Mock
    private User user;

    @Mock
    private BukkitTask task;

    private int times;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Make the addon
        dataFolder = new File("dataFolder");
        jarFile = new File("addon.jar");
        makeAddon();
        test = new TestClass();
        test.setDataFolder(dataFolder);
        test.setFile(jarFile);
        // Default blueprint
        defaultBp = new Blueprint();
        defaultBp.setName("bedrock");
        defaultBp.setDescription(Collections.singletonList(ChatColor.AQUA + "A bedrock block"));
        defaultBp.setBedrock(new Vector(0,0,0));
        Map<Vector, BlueprintBlock> map = new HashMap<>();
        map.put(new Vector(0,0,0), new BlueprintBlock("minecraft:bedrock"));
        defaultBp.setBlocks(map);
        // Scheduler
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(scheduler);

    }

    /**
     * Fake Addon class
     *
     */
    private class TestClass extends Addon {
        @Override
        public void onEnable() { }

        @Override
        public void onDisable() { }
    }

    public void makeAddon() throws Exception {
        // Make a blueprint folder
        File blueprintFolder = new File(dataFolder, BlueprintsManager.FOLDER_NAME);
        blueprintFolder.mkdirs();
        // Make a blueprint file
        YamlConfiguration config = new YamlConfiguration();
        config.set("hello", "this is a test");
        File configFile = new File(blueprintFolder, "blueprint.blu");
        config.save(configFile);
        // Make a blueprint bundle
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("name", "TestAddon");
        File ymlFile = new File(blueprintFolder, "bundle.json");
        yml.save(ymlFile);
        // Make an archive file
        // Put them into a jar file
        createJarArchive(jarFile, blueprintFolder, Arrays.asList(configFile, ymlFile));
        // Clean up
        Files.deleteIfExists(configFile.toPath());
        Files.deleteIfExists(ymlFile.toPath());
        // Remove folder
        deleteDir(blueprintFolder.toPath());
        // Mocks
        when(addon.getDataFolder()).thenReturn(dataFolder);
        when(addon.getFile()).thenReturn(jarFile);
        when(addon.getOverWorld()).thenReturn(world);
        when(addon.getPermissionPrefix()).thenReturn("bskyblock.");
        // Desc
        AddonDescription desc = new AddonDescription.Builder("main", "name", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clean up file system
        deleteDir(dataFolder.toPath());
        // Delete addon.jar
        Files.deleteIfExists(jarFile.toPath());

        Mockito.framework().clearInlineMocks();
    }

    private void deleteDir(Path path) throws Exception {
        if (path.toFile().isDirectory()) {
            // Clean up file system
            Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
        Files.deleteIfExists(path);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#extractDefaultBlueprints(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     * @throws IOException
     */
    @Test
    public void testExtractDefaultBlueprintsFolderExists() throws IOException {
        // Make the default folder
        File bpFile = new File("datafolder", "blueprints");
        bpFile.mkdirs();
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.extractDefaultBlueprints(addon);
        // Nothing should happen
        assertTrue(bpFile.listFiles().length == 0);
        // Clean up
        Files.deleteIfExists(bpFile.toPath());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#extractDefaultBlueprints(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testExtractDefaultBlueprints() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.extractDefaultBlueprints(addon);
        verify(addon).saveResource(eq("blueprints/bundle.json"), eq(false));
        verify(addon).saveResource(eq("blueprints/blueprint.blu"), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#extractDefaultBlueprints(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testExtractDefaultBlueprintsThrowError() throws NullPointerException {
        // Give it a folder instead of a jar file
        when(addon.getFile()).thenReturn(dataFolder);
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.extractDefaultBlueprints(addon);
        verify(plugin).logError(Mockito.startsWith("Could not load blueprint files from addon jar dataFolder"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#getBlueprintBundles(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testGetBlueprintBundles() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        assertTrue(bpm.getBlueprintBundles(addon).isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#loadBlueprintBundles(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testLoadBlueprintBundlesNoBlueprintFolder() {
        // Set up running and verification
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(new Answer<BukkitTask>() {

            @Override
            public BukkitTask answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgument(1,Runnable.class).run();
                verify(plugin).logError(eq("There is no blueprint folder for addon name"));
                verify(plugin).logError(eq("No blueprint bundles found! Creating a default one."));
                File blueprints = new File(dataFolder, BlueprintsManager.FOLDER_NAME);
                File d = new File(blueprints, "default.json");
                assertTrue(d.exists());
                return task;
            }});

        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.loadBlueprintBundles(addon);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#loadBlueprintBundles(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testLoadBlueprintBundles() {
        // Set up running and verification
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(new Answer<BukkitTask>() {

            @Override
            public BukkitTask answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgument(1,Runnable.class).run();
                verify(plugin).logError(eq("No blueprint bundles found! Creating a default one."));
                return task;
            }});
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.extractDefaultBlueprints(addon);
        bpm.loadBlueprintBundles(addon);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#loadBlueprints(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testLoadBlueprintsFail() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.loadBlueprints(addon);
        verify(plugin).logError("No blueprints found for name");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#loadBlueprints(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testLoadBlueprintsFailZero() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.extractDefaultBlueprints(addon);
        bpm.loadBlueprints(addon);
        verify(plugin).logError("No blueprints found for name");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#loadBlueprints(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testLoadBlueprints() {
        // Set up running and verification
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(new Answer<BukkitTask>() {

            @Override
            public BukkitTask answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgument(1,Runnable.class).run();
                return task;
            }});
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        // Load once (makes default files too)
        bpm.loadBlueprintBundles(addon);
        // Load them again
        bpm.loadBlueprints(addon);
        verify(plugin, Mockito.times(2)).log("Loaded blueprint 'bedrock' for name");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#addBlueprint(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testAddBlueprint() {
        // add blueprint
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.addBlueprint(addon, defaultBp);
        verify(plugin).log(eq("Added blueprint 'bedrock' for name"));
        // Add it again, it should replace the previous one
        bpm.addBlueprint(addon, defaultBp);
        assertTrue(bpm.getBlueprints(addon).size() == 1);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#saveBlueprint(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testSaveBlueprint() {
        // Save it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.saveBlueprint(addon, defaultBp);
        File blueprints = new File(dataFolder, BlueprintsManager.FOLDER_NAME);
        File d = new File(blueprints, "bedrock.blu");
        assertTrue(d.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#saveBlueprintBundle(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle)}.
     */
    @Test
    public void testSaveBlueprintBundle() {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("bundle");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // Save it
        File blueprints = new File(dataFolder, BlueprintsManager.FOLDER_NAME);

        // Set up running and verification
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(new Answer<BukkitTask>() {

            @Override
            public BukkitTask answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgument(1,Runnable.class).run();
                File d = new File(blueprints, "bundle.json");
                assertTrue(d.exists());
                return task;
            }});

        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.saveBlueprintBundle(addon, bb);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#saveBlueprintBundles()}.
     */
    @Test
    public void testSaveBlueprintBundles() {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("bundle");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // Add it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.addBlueprintBundle(addon, bb);
        // Add another
        BlueprintBundle bb2 = new BlueprintBundle();
        bb2.setIcon(Material.PAPER);
        bb2.setUniqueId("bundle2");
        bb2.setDisplayName("A bundle2");
        bb2.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints2"));
        // Add
        bpm.addBlueprintBundle(addon, bb2);
        // check that there are 2 in there
        assertEquals(2, bpm.getBlueprintBundles(addon).size());
        File blueprints = new File(dataFolder, BlueprintsManager.FOLDER_NAME);
        File d = new File(blueprints, "bundle.json");
        File d2 = new File(blueprints, "bundle2.json");
        times = 0;
        // Set up running and verification
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(new Answer<BukkitTask>() {

            @Override
            public BukkitTask answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgument(1,Runnable.class).run();
                // Verify
                times++;
                if (times > 2) {
                    assertTrue(d.exists());
                    assertTrue(d2.exists());
                }
                return task;
            }});
        // Save
        bpm.saveBlueprintBundles();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#getBlueprints(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testGetBlueprints() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        assertTrue(bpm.getBlueprints(addon).isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#paste(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.database.objects.Island, java.lang.String)}.
     */
    @Test
    public void testPasteGameModeAddonIslandStringFail() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.paste(addon, island, "random");
        verify(plugin).logError("Tried to paste 'random' but the bundle is not loaded!");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#paste(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.database.objects.Island, java.lang.String)}.
     */
    @Test
    public void testPasteGameModeAddonIslandStringNoBlueprintsLoaded() {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("bundle");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // Add it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.addBlueprintBundle(addon, bb);
        // paste it
        bpm.paste(addon, island, "bundle");
        verify(plugin).logError("No blueprints loaded for bundle 'bundle'!");
    }


    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#paste(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.database.objects.Island, java.lang.String)}.
     */
    @Test
    public void testPasteGameModeAddonIslandStringNoNormalBlueprint() {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("bundle");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // Set no environments
        // Add it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.addBlueprintBundle(addon, bb);
        bpm.addBlueprint(addon, defaultBp);
        // paste it
        bpm.paste(addon, island, "bundle");
        verify(plugin).logError("Blueprint bundle has no normal world blueprint, using default");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#validate(world.bentobox.bentobox.api.addons.GameModeAddon, java.lang.String)}.
     */
    @Test
    public void testValidateNull() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        assertNull(bpm.validate(addon, null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#validate(world.bentobox.bentobox.api.addons.GameModeAddon, java.lang.String)}.
     */
    @Test
    public void testValidateInvalid() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        assertNull(bpm.validate(addon, "invalid"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#validate(world.bentobox.bentobox.api.addons.GameModeAddon, java.lang.String)}.
     */
    @Test
    public void testValidate() {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("bundle");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // This blueprint is used for all environments
        bb.setBlueprint(World.Environment.NORMAL, defaultBp);
        bb.setBlueprint(World.Environment.NETHER, defaultBp);
        bb.setBlueprint(World.Environment.THE_END, defaultBp);
        // Add it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.addBlueprintBundle(addon, bb);
        assertEquals("bundle", bpm.validate(addon, "bundle"));
        // Mixed case
        assertEquals("buNdle", bpm.validate(addon, "buNdle"));
        // Not there
        assertNull(bpm.validate(addon, "buNdle2"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#checkPerm(world.bentobox.bentobox.api.addons.Addon, world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    public void testCheckPermNoBundles() {
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        assertFalse(bpm.checkPerm(addon, user, "name"));
        verify(user).sendMessage(eq("general.errors.no-permission"), eq(TextVariables.PERMISSION), eq("bskyblock.island.create.name"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#checkPerm(world.bentobox.bentobox.api.addons.Addon, world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    public void testCheckPermBundlesNoPremissionRequired() {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("bundle");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // This blueprint is used for all environments
        bb.setBlueprint(World.Environment.NORMAL, defaultBp);
        bb.setBlueprint(World.Environment.NETHER, defaultBp);
        bb.setBlueprint(World.Environment.THE_END, defaultBp);
        // No permissions required
        bb.setRequirePermission(false);
        // Add it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.addBlueprintBundle(addon, bb);
        // Check perm
        assertTrue(bpm.checkPerm(addon, user, "bundle"));
        verify(user, Mockito.never()).sendMessage(eq("general.errors.no-permission"), eq(TextVariables.PERMISSION), eq("bskyblock.island.create.bundle"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#checkPerm(world.bentobox.bentobox.api.addons.Addon, world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    public void testCheckPermBundlesPremissionRequired() {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("bundle");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // This blueprint is used for all environments
        bb.setBlueprint(World.Environment.NORMAL, defaultBp);
        bb.setBlueprint(World.Environment.NETHER, defaultBp);
        bb.setBlueprint(World.Environment.THE_END, defaultBp);
        // Permission required
        bb.setRequirePermission(true);
        // Add it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.addBlueprintBundle(addon, bb);
        assertFalse(bpm.checkPerm(addon, user, "bundle"));
        verify(user).sendMessage(eq("general.errors.no-permission"), eq(TextVariables.PERMISSION), eq("bskyblock.island.create.bundle"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#checkPerm(world.bentobox.bentobox.api.addons.Addon, world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    public void testCheckPermBundlesDefault() {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("default");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // This blueprint is used for all environments
        bb.setBlueprint(World.Environment.NORMAL, defaultBp);
        bb.setBlueprint(World.Environment.NETHER, defaultBp);
        bb.setBlueprint(World.Environment.THE_END, defaultBp);
        // Permission required
        bb.setRequirePermission(true);
        // Add it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.addBlueprintBundle(addon, bb);
        assertTrue(bpm.checkPerm(addon, user, "default"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#deleteBlueprintBundle(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle)}.
     * @throws IOException
     */
    @Test
    public void testDeleteBlueprintBundle() throws IOException {
        // Make bundle
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId("bundle");
        bb.setDisplayName("A bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "A bundle of blueprints"));
        // Create a dummy file
        File blueprints = new File(dataFolder, BlueprintsManager.FOLDER_NAME);
        blueprints.mkdirs();
        File d = new File(blueprints, "bundle.json");
        Files.createFile(d.toPath());

        BlueprintsManager bpm = new BlueprintsManager(plugin);
        // Set up running and verification
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(new Answer<BukkitTask>() {

            @Override
            public BukkitTask answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgument(1,Runnable.class).run();

                // Verify
                assertFalse(d.exists());
                return task;
            }});

        // Delete it
        bpm.deleteBlueprintBundle(addon, bb);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintsManager#renameBlueprint(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.blueprints.Blueprint, java.lang.String)}.
     */
    @Test
    public void testRenameBlueprint() {
        // Save it
        BlueprintsManager bpm = new BlueprintsManager(plugin);
        bpm.saveBlueprint(addon, defaultBp);
        File blueprints = new File(dataFolder, BlueprintsManager.FOLDER_NAME);
        File d = new File(blueprints, "bedrock.blu");
        assertTrue(d.exists());
        // Rename it
        bpm.renameBlueprint(addon, defaultBp, "bedrock2");
        assertFalse(d.exists());
        d = new File(blueprints, "bedrock2.blu");
        assertTrue(d.exists());
    }

    /*
     * Utility methods
     */
    private void createJarArchive(File archiveFile, File folder, List<File> tobeJaredList) {
        byte buffer[] = new byte[BUFFER_SIZE];
        // Open archive file
        try (FileOutputStream stream = new FileOutputStream(archiveFile)) {
            try (JarOutputStream out = new JarOutputStream(stream, new Manifest())) {
                for (File j: tobeJaredList) addFile(folder, buffer, stream, out, j);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void addFile(File folder, byte[] buffer, FileOutputStream stream, JarOutputStream out, File tobeJared) throws IOException {
        if (tobeJared == null || !tobeJared.exists() || tobeJared.isDirectory())
            return;
        // Add archive entry
        JarEntry jarAdd = new JarEntry(folder.getName() + "/" + tobeJared.getName());
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
