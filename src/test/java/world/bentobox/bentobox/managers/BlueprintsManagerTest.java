package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * Tests for {@link BlueprintsManager}.
 */
class BlueprintsManagerTest extends CommonTestSetup {

    @Mock
    private GameModeAddon addon;

    private BlueprintsManager manager;
    private File dataFolder;
    private File blueprintsFolder;

    private static final String BUNDLE_NAME = "default";

    /** A minimal valid blueprint in JSON form (will be zipped into a .blu file). */
    private static final String BLUEPRINT_JSON = """
            {
                "name": "island",
                "attached": {},
                "entities": {},
                "blocks": [
                    [
                        [0.0, 0.0, 0.0], {
                            "blockData": "minecraft:bedrock"
                        }
                    ]
                ],
                "xSize": 10,
                "ySize": 10,
                "zSize": 10,
                "bedrock": [0.0, 0.0, 0.0]
            }""";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        dataFolder = new File("test-bm-" + System.nanoTime());
        blueprintsFolder = new File(dataFolder, BlueprintsManager.FOLDER_NAME);

        AddonDescription desc = new AddonDescription.Builder("main", "TestAddon", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);
        when(addon.getDataFolder()).thenReturn(dataFolder);
        when(addon.getPermissionPrefix()).thenReturn("testaddon.");

        // Run async tasks synchronously so file-writing tests are deterministic.
        when(sch.runTaskAsynchronously(any(), any(Runnable.class))).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return mock(BukkitTask.class);
        });

        manager = new BlueprintsManager(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        deleteFolder(dataFolder);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void deleteFolder(File folder) throws IOException {
        if (folder != null && folder.exists()) {
            Files.walk(folder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Zips {@code sourceFile} into {@code <parent>/<entryName>.blu} (legacy format), then
     * deletes the original file. Mirrors what the old BlueprintClipboardManager used to do.
     */
    private void zipBlueprint(File sourceFile, String entryName) throws IOException {
        File zipFile = new File(sourceFile.getParentFile(),
                entryName + BlueprintsManager.LEGACY_BLUEPRINT_SUFFIX);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
                FileInputStream fis = new FileInputStream(sourceFile)) {
            zos.putNextEntry(new ZipEntry(sourceFile.getName()));
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) >= 0) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
        }
        Files.delete(sourceFile.toPath());
    }

    // -----------------------------------------------------------------------
    // Constructor / isBlueprintsLoaded
    // -----------------------------------------------------------------------

    @Test
    void testConstructorCreatesManager() {
        assertNotNull(manager);
    }

    @Test
    void testIsBlueprintsLoadedTrueInitially() {
        assertTrue(manager.isBlueprintsLoaded());
    }

    // -----------------------------------------------------------------------
    // getBlueprintBundles
    // -----------------------------------------------------------------------

    @Test
    void testGetBlueprintBundlesUnregistered() {
        Map<String, BlueprintBundle> result = manager.getBlueprintBundles(addon);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetBlueprintBundlesWithBundle() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BUNDLE_NAME);
        manager.addBlueprintBundle(addon, bb);

        Map<String, BlueprintBundle> result = manager.getBlueprintBundles(addon);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(BUNDLE_NAME));
        assertEquals(bb, result.get(BUNDLE_NAME));
    }

    // -----------------------------------------------------------------------
    // getDefaultBlueprintBundle
    // -----------------------------------------------------------------------

    @Test
    void testGetDefaultBlueprintBundleUnregistered() {
        assertNull(manager.getDefaultBlueprintBundle(addon));
    }

    @Test
    void testGetDefaultBlueprintBundleReturnsDefault() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BlueprintsManager.DEFAULT_BUNDLE_NAME);
        manager.addBlueprintBundle(addon, bb);

        BlueprintBundle result = manager.getDefaultBlueprintBundle(addon);
        assertNotNull(result);
        assertEquals(BlueprintsManager.DEFAULT_BUNDLE_NAME, result.getUniqueId());
    }

    @Test
    void testGetDefaultBlueprintBundleNoDefaultBundle() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId("custom");
        manager.addBlueprintBundle(addon, bb);

        assertNull(manager.getDefaultBlueprintBundle(addon));
    }

    // -----------------------------------------------------------------------
    // validate
    // -----------------------------------------------------------------------

    @Test
    void testValidateNullName() {
        assertNull(manager.validate(addon, null));
    }

    @Test
    void testValidateNameNotFound() {
        assertNull(manager.validate(addon, "nonexistent"));
    }

    @Test
    void testValidateNameFound() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BUNDLE_NAME);
        manager.addBlueprintBundle(addon, bb);

        assertEquals(BUNDLE_NAME, manager.validate(addon, BUNDLE_NAME));
    }

    // -----------------------------------------------------------------------
    // addBlueprint / getBlueprints
    // -----------------------------------------------------------------------

    @Test
    void testGetBlueprintsUnregistered() {
        Map<String, Blueprint> result = manager.getBlueprints(addon);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAddBlueprintAddsEntry() {
        Blueprint bp = new Blueprint();
        bp.setName("island");
        manager.addBlueprint(addon, bp);

        Map<String, Blueprint> result = manager.getBlueprints(addon);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("island"));
        verify(plugin).log("Added blueprint 'island' for TestAddon");
    }

    @Test
    void testAddBlueprintReplacesExistingByName() {
        Blueprint bp1 = new Blueprint();
        bp1.setName("island");
        Blueprint bp2 = new Blueprint();
        bp2.setName("island");

        manager.addBlueprint(addon, bp1);
        manager.addBlueprint(addon, bp2);

        Map<String, Blueprint> result = manager.getBlueprints(addon);
        assertEquals(1, result.size());
        assertEquals(bp2, result.get("island"));
    }

    // -----------------------------------------------------------------------
    // addBlueprintBundle
    // -----------------------------------------------------------------------

    @Test
    void testAddBlueprintBundleAddsEntry() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId("vip");
        manager.addBlueprintBundle(addon, bb);

        assertTrue(manager.getBlueprintBundles(addon).containsKey("vip"));
    }

    @Test
    void testAddBlueprintBundleReplacesExisting() {
        BlueprintBundle bb1 = new BlueprintBundle();
        bb1.setUniqueId(BUNDLE_NAME);
        bb1.setDisplayName("Old");
        BlueprintBundle bb2 = new BlueprintBundle();
        bb2.setUniqueId(BUNDLE_NAME);
        bb2.setDisplayName("New");

        manager.addBlueprintBundle(addon, bb1);
        manager.addBlueprintBundle(addon, bb2);

        Map<String, BlueprintBundle> bundles = manager.getBlueprintBundles(addon);
        assertEquals(1, bundles.size());
        assertEquals("New", bundles.get(BUNDLE_NAME).getDisplayName());
    }

    // -----------------------------------------------------------------------
    // checkPerm
    // -----------------------------------------------------------------------

    @Test
    void testCheckPermBundleNotFound() {
        User user = mock(User.class);
        assertFalse(manager.checkPerm(addon, user, "nonexistent"));
        verify(user).sendMessage(eq("general.errors.no-permission"),
                eq(TextVariables.PERMISSION), anyString());
    }

    @Test
    void testCheckPermNoPermission() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId("vip");
        bb.setRequirePermission(true);
        manager.addBlueprintBundle(addon, bb);

        User user = mock(User.class);
        when(user.hasPermission(anyString())).thenReturn(false);

        assertFalse(manager.checkPerm(addon, user, "vip"));
        verify(user).sendMessage(eq("general.errors.no-permission"),
                eq(TextVariables.PERMISSION), anyString());
    }

    @Test
    void testCheckPermAllowed() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId("vip");
        bb.setRequirePermission(true);
        manager.addBlueprintBundle(addon, bb);

        User user = mock(User.class);
        when(user.hasPermission("testaddon.island.create.vip")).thenReturn(true);

        assertTrue(manager.checkPerm(addon, user, "vip"));
    }

    @Test
    void testCheckPermDefaultBundleAlwaysAllowed() {
        // Even with requirePermission=true, the default bundle is always allowed.
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BlueprintsManager.DEFAULT_BUNDLE_NAME);
        bb.setRequirePermission(true);
        manager.addBlueprintBundle(addon, bb);

        User user = mock(User.class);
        assertTrue(manager.checkPerm(addon, user, BlueprintsManager.DEFAULT_BUNDLE_NAME));
    }

    @Test
    void testCheckPermNoPermissionRequired() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId("vip");
        bb.setRequirePermission(false);
        manager.addBlueprintBundle(addon, bb);

        User user = mock(User.class);
        assertTrue(manager.checkPerm(addon, user, "vip"));
    }

    // -----------------------------------------------------------------------
    // deleteBlueprint
    // -----------------------------------------------------------------------

    @Test
    void testDeleteBlueprintLeavesOtherBlueprints() {
        Blueprint bp = new Blueprint();
        bp.setName("island");
        manager.addBlueprint(addon, bp);

        manager.deleteBlueprint(addon, "nonexistent");

        assertEquals(1, manager.getBlueprints(addon).size());
    }

    @Test
    void testDeleteBlueprintRemovesFromListAndFile() throws IOException {
        blueprintsFolder.mkdirs();
        Blueprint bp = new Blueprint();
        bp.setName("island");
        manager.addBlueprint(addon, bp);

        File file = new File(blueprintsFolder, "island" + BlueprintsManager.BLUEPRINT_SUFFIX);
        Files.writeString(file.toPath(), "dummy");
        assertTrue(file.exists());

        manager.deleteBlueprint(addon, "island");

        assertTrue(manager.getBlueprints(addon).isEmpty());
        assertFalse(file.exists());
    }

    @Test
    void testDeleteBlueprintCaseInsensitive() {
        Blueprint bp = new Blueprint();
        bp.setName("island");
        manager.addBlueprint(addon, bp);

        manager.deleteBlueprint(addon, "ISLAND");

        assertTrue(manager.getBlueprints(addon).isEmpty());
    }

    // -----------------------------------------------------------------------
    // deleteBlueprintBundle
    // -----------------------------------------------------------------------

    @Test
    void testDeleteBlueprintBundleRemovesFromMapAndFile() throws IOException {
        blueprintsFolder.mkdirs();
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BUNDLE_NAME);
        manager.addBlueprintBundle(addon, bb);

        File file = new File(blueprintsFolder, BUNDLE_NAME + ".json");
        Files.writeString(file.toPath(), "{}");
        assertTrue(file.exists());

        manager.deleteBlueprintBundle(addon, bb);

        assertTrue(manager.getBlueprintBundles(addon).isEmpty());
        assertFalse(file.exists());
    }

    @Test
    void testDeleteBlueprintBundleAddonNotRegisteredDoesNotThrow() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BUNDLE_NAME);
        assertDoesNotThrow(() -> manager.deleteBlueprintBundle(addon, bb));
    }

    // -----------------------------------------------------------------------
    // loadBlueprints
    // -----------------------------------------------------------------------

    @Test
    void testLoadBlueprintsNoFolder() {
        manager.loadBlueprints(addon);
        verify(plugin).logError("There is no blueprint folder for addon TestAddon");
    }

    @Test
    void testLoadBlueprintsEmptyFolder() {
        blueprintsFolder.mkdirs();
        manager.loadBlueprints(addon);
        verify(plugin).logError("No blueprints found for TestAddon");
    }

    @Test
    void testLoadBlueprintsLoadsFile() throws IOException {
        blueprintsFolder.mkdirs();
        // Write the raw JSON as a plain .blueprint file
        File jsonFile = new File(blueprintsFolder, "island" + BlueprintsManager.BLUEPRINT_SUFFIX);
        Files.writeString(jsonFile.toPath(), BLUEPRINT_JSON);

        manager.loadBlueprints(addon);

        Map<String, Blueprint> blueprints = manager.getBlueprints(addon);
        assertEquals(1, blueprints.size());
        assertTrue(blueprints.containsKey("island"));
        verify(plugin).log("Loaded blueprint 'island' for TestAddon");
    }

    @Test
    void testLoadBlueprintsLoadsLegacyBluFile() throws IOException {
        blueprintsFolder.mkdirs();
        // Write the raw JSON, then zip it into "island.blu" (legacy format)
        File jsonFile = new File(blueprintsFolder, "island");
        Files.writeString(jsonFile.toPath(), BLUEPRINT_JSON);
        zipBlueprint(jsonFile, "island");

        manager.loadBlueprints(addon);

        Map<String, Blueprint> blueprints = manager.getBlueprints(addon);
        assertEquals(1, blueprints.size());
        assertTrue(blueprints.containsKey("island"));
        verify(plugin).log("Loaded blueprint 'island' for TestAddon");
    }

    // -----------------------------------------------------------------------
    // extractDefaultBlueprints
    // -----------------------------------------------------------------------

    @Test
    void testExtractDefaultBlueprintsFolderAlreadyExists() {
        blueprintsFolder.mkdirs();
        // If the folder exists the method should return immediately without errors.
        manager.extractDefaultBlueprints(addon);
        verify(plugin, never()).logError(anyString());
    }

    // -----------------------------------------------------------------------
    // paste
    // -----------------------------------------------------------------------

    @Test
    void testPasteBundleNotRegistered() {
        boolean result = manager.paste(addon, island, BUNDLE_NAME, null, true);
        assertFalse(result);
        verify(plugin).logError("Tried to paste 'default' but the bundle is not loaded!");
    }

    @Test
    void testPasteNoBlueprintsForBundle() {
        // Bundle is registered but no blueprints are loaded.
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BUNDLE_NAME);
        manager.addBlueprintBundle(addon, bb);

        boolean result = manager.paste(addon, island, BUNDLE_NAME, null, true);
        assertFalse(result);
        verify(plugin).logError("No blueprints loaded for bundle 'default'!");
    }

    // -----------------------------------------------------------------------
    // saveBlueprint
    // -----------------------------------------------------------------------

    @Test
    void testSaveBlueprintCreatesFile() {
        blueprintsFolder.mkdirs();
        Blueprint bp = new Blueprint();
        bp.setName("island");

        boolean result = manager.saveBlueprint(addon, bp);

        assertTrue(result);
        File saved = new File(blueprintsFolder, "island" + BlueprintsManager.BLUEPRINT_SUFFIX);
        assertTrue(saved.exists());
    }

    // -----------------------------------------------------------------------
    // saveBlueprintBundle (async, runs synchronously in test)
    // -----------------------------------------------------------------------

    @Test
    void testSaveBlueprintBundleCreatesFile() throws IOException {
        blueprintsFolder.mkdirs();
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BUNDLE_NAME);
        bb.setDisplayName("Test Bundle");

        manager.saveBlueprintBundle(addon, bb);

        File savedFile = new File(blueprintsFolder, BUNDLE_NAME + ".json");
        assertTrue(savedFile.exists());
        String content = Files.readString(savedFile.toPath());
        assertTrue(content.contains(BUNDLE_NAME));
    }

    // -----------------------------------------------------------------------
    // renameBlueprint
    // -----------------------------------------------------------------------

    @Test
    void testRenameBlueprintSameNameIsNoOp() {
        Blueprint bp = new Blueprint();
        bp.setName("island");
        manager.addBlueprint(addon, bp);

        manager.renameBlueprint(addon, bp, "island", "Island Display");

        // Name should be unchanged; method returns early.
        assertEquals("island", bp.getName());
        assertTrue(manager.getBlueprints(addon).containsKey("island"));
    }

    @Test
    void testRenameBlueprintNewName() throws IOException {
        blueprintsFolder.mkdirs();
        // Create the "old" .blueprint file so deleteIfExists has something to remove.
        File oldFile = new File(blueprintsFolder,
                "island" + BlueprintsManager.BLUEPRINT_SUFFIX);
        Files.writeString(oldFile.toPath(), "dummy");

        Blueprint bp = new Blueprint();
        bp.setName("island");
        manager.addBlueprint(addon, bp);

        manager.renameBlueprint(addon, bp, "newisland", "New Island");

        assertFalse(oldFile.exists());
        assertEquals("newisland", bp.getName());
        assertEquals("New Island", bp.getDisplayName());

        Map<String, Blueprint> blueprints = manager.getBlueprints(addon);
        assertFalse(blueprints.containsKey("island"));
        assertTrue(blueprints.containsKey("newisland"));
    }

    // -----------------------------------------------------------------------
    // BlueprintBundle commands
    // -----------------------------------------------------------------------

    @Test
    void testBlueprintBundleCommandsDefaultEmpty() {
        BlueprintBundle bb = new BlueprintBundle();
        assertNotNull(bb.getCommands());
        assertTrue(bb.getCommands().isEmpty());
    }

    @Test
    void testBlueprintBundleSetCommands() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setCommands(List.of("say hello", "give [player] diamond 1"));
        assertEquals(2, bb.getCommands().size());
        assertEquals("say hello", bb.getCommands().get(0));
        assertEquals("give [player] diamond 1", bb.getCommands().get(1));
    }

    @Test
    void testBlueprintBundleCommandsSerializedInJson() throws IOException {
        blueprintsFolder.mkdirs();
        BlueprintBundle bb = new BlueprintBundle();
        bb.setUniqueId(BUNDLE_NAME);
        bb.setCommands(List.of("say hello [player]"));
        manager.saveBlueprintBundle(addon, bb);

        File savedFile = new File(blueprintsFolder, BUNDLE_NAME + ".json");
        String content = Files.readString(savedFile.toPath());
        assertTrue(content.contains("say hello [player]"), "Commands should be serialised into JSON");
    }
}
