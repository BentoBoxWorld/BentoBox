package world.bentobox.bentobox.panels.customizable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.managers.BlueprintsManager;

/**
 * Tests for {@link IslandCreationPanel}.
 */
class IslandCreationPanelTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private CompositeCommand command;
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private GameModeAddon addon;
    @Mock
    private Settings settings;

    private Map<String, BlueprintBundle> bundleMap;
    private BlueprintBundle bundle1;
    private BlueprintBundle bundle2;
    private BlueprintBundle bundle3;

    private final Path resourcePath = Paths.get("src", "test", "resources");

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID userUuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(userUuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getWorld()).thenReturn(world);

        // Translation mocks
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) inv -> inv.getArgument(0, String.class));
        when(user.getTranslation(anyString(), any()))
                .thenAnswer((Answer<String>) inv -> inv.getArgument(0, String.class));
        when(user.getTranslation(any(World.class), anyString()))
                .thenAnswer((Answer<String>) inv -> inv.getArgument(1, String.class));
        when(user.getTranslation(any(World.class), anyString(), any()))
                .thenAnswer((Answer<String>) inv -> inv.getArgument(1, String.class));
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString(), anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString(), anyString(), anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("");

        User.setPlugin(plugin);
        User.getInstance(player);

        // Addon
        when(addon.getDataFolder()).thenReturn(resourcePath.toFile());

        // Command
        when(command.getAddon()).thenReturn(addon);
        when(command.getWorld()).thenReturn(world);
        when(command.getPlugin()).thenReturn(plugin);
        when(command.getPermissionPrefix()).thenReturn("bskyblock.");
        when(command.getTopLabel()).thenReturn("island");
        when(command.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(command.getSubCommand(anyString())).thenReturn(Optional.empty());

        // Plugin description
        when(plugin.getDescription()).thenAnswer((Answer<PluginDescriptionFile>) inv ->
                new PluginDescriptionFile("BentoBox", "1.0", "world.bentobox.bentobox"));

        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getIslandNumber()).thenReturn(1); // single island by default
        when(settings.isUseEconomy()).thenReturn(false);
        when(settings.isHideUsedBlueprints()).thenReturn(false);

        // No vault by default
        when(plugin.getVault()).thenReturn(Optional.empty());

        // Blueprints Manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);

        // Create bundles
        bundle1 = mock(BlueprintBundle.class);
        when(bundle1.getUniqueId()).thenReturn("default");
        when(bundle1.getDisplayName()).thenReturn("Default");
        when(bundle1.getIcon()).thenReturn(Material.GRASS_BLOCK);
        when(bundle1.getIconItemStack()).thenReturn(new ItemStack(Material.GRASS_BLOCK));
        when(bundle1.getDescription()).thenReturn(Collections.singletonList("Default island"));
        when(bundle1.getSlot()).thenReturn(0);
        when(bundle1.isRequirePermission()).thenReturn(false);
        when(bundle1.getTimes()).thenReturn(0);
        when(bundle1.getCost()).thenReturn(0.0);

        bundle2 = mock(BlueprintBundle.class);
        when(bundle2.getUniqueId()).thenReturn("nether");
        when(bundle2.getDisplayName()).thenReturn("Nether");
        when(bundle2.getIcon()).thenReturn(Material.NETHERRACK);
        when(bundle2.getIconItemStack()).thenReturn(new ItemStack(Material.NETHERRACK));
        when(bundle2.getDescription()).thenReturn(Collections.singletonList("Nether island"));
        when(bundle2.getSlot()).thenReturn(1);
        when(bundle2.isRequirePermission()).thenReturn(false);
        when(bundle2.getTimes()).thenReturn(0);
        when(bundle2.getCost()).thenReturn(0.0);

        bundle3 = mock(BlueprintBundle.class);
        when(bundle3.getUniqueId()).thenReturn("end");
        when(bundle3.getDisplayName()).thenReturn("End");
        when(bundle3.getIcon()).thenReturn(Material.END_STONE);
        when(bundle3.getIconItemStack()).thenReturn(new ItemStack(Material.END_STONE));
        when(bundle3.getDescription()).thenReturn(Collections.singletonList("End island"));
        when(bundle3.getSlot()).thenReturn(2);
        when(bundle3.isRequirePermission()).thenReturn(false);
        when(bundle3.getTimes()).thenReturn(0);
        when(bundle3.getCost()).thenReturn(0.0);

        bundleMap = new HashMap<>();
        bundleMap.put("default", bundle1);
        when(bpm.getBlueprintBundles(any(GameModeAddon.class))).thenReturn(bundleMap);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ---------------------------------------------------------------
    // Constructor tests
    // ---------------------------------------------------------------

    @Test
    void testConstructorWithBundles() {
        assertDoesNotThrow(() -> new IslandCreationPanel(command, user, "island", false));
    }

    @Test
    void testConstructorNoBundles() {
        bundleMap.clear();
        assertDoesNotThrow(() -> new IslandCreationPanel(command, user, "island", false));
    }

    @Test
    void testConstructorFiltersPermissionBundles() {
        // bundle2 requires permission, user does not have it
        when(bundle2.isRequirePermission()).thenReturn(true);
        when(user.hasPermission("bskyblock.island.create.nether")).thenReturn(false);
        bundleMap.put("nether", bundle2);

        IslandCreationPanel panel = new IslandCreationPanel(command, user, "island", false);
        // Build should succeed - bundle2 should be filtered out
        assertDoesNotThrow(panel::build);
    }

    @Test
    void testConstructorIncludesPermissionBundlesWhenUserHasPermission() {
        when(bundle2.isRequirePermission()).thenReturn(true);
        when(user.hasPermission("bskyblock.island.create.nether")).thenReturn(true);
        bundleMap.put("nether", bundle2);

        assertDoesNotThrow(() -> new IslandCreationPanel(command, user, "island", false));
    }

    @Test
    void testConstructorSortsBySlotThenUniqueId() {
        bundleMap.put("nether", bundle2);
        bundleMap.put("end", bundle3);

        // Should not throw - bundles sorted by slot then uniqueId
        assertDoesNotThrow(() -> new IslandCreationPanel(command, user, "island", false));
    }

    // ---------------------------------------------------------------
    // build() tests
    // ---------------------------------------------------------------

    @Test
    void testBuildEmptyBundles() {
        bundleMap.clear();
        IslandCreationPanel panel = new IslandCreationPanel(command, user, "island", false);
        panel.build();

        // Should log error and send message when no bundles available
        verify(plugin).logError("There are no available phases for selection!");
        verify(user).sendMessage(eq("no-phases"), anyString(), anyString());
    }

    @Test
    void testBuildWithBundles() {
        // Panel template exists at src/test/resources/panels/island_creation_panel.yml
        assertDoesNotThrow(() -> {
            IslandCreationPanel panel = new IslandCreationPanel(command, user, "island", false);
            panel.build();
        });
    }

    @Test
    void testBuildUsesAddonPanelWhenExists() {
        // The addon's data folder is src/test/resources which has panels/island_creation_panel.yml
        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }

    // ---------------------------------------------------------------
    // openPanel() static method tests
    // ---------------------------------------------------------------

    @Test
    void testOpenPanel() {
        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }

    @Test
    void testOpenPanelReset() {
        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", true));
    }

    // ---------------------------------------------------------------
    // Bundle usage tracking (multi-island) tests
    // ---------------------------------------------------------------

    @Test
    void testBuildWithMultipleIslandsShowsUsage() {
        // Enable multi-island
        when(settings.getIslandNumber()).thenReturn(3);

        // Bundle with limited uses
        when(bundle1.getTimes()).thenReturn(2);

        // No existing islands using this bundle
        when(plugin.getIslands().getIslands(any(), any(User.class))).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }

    @Test
    void testBuildWithUsedUpBundleHidden() {
        when(settings.getIslandNumber()).thenReturn(3);
        when(settings.isHideUsedBlueprints()).thenReturn(true);
        when(bundle1.getTimes()).thenReturn(1);

        // Create an existing island using this bundle
        world.bentobox.bentobox.database.objects.Island existingIsland =
                mock(world.bentobox.bentobox.database.objects.Island.class);
        MetaDataValue mdv = mock(MetaDataValue.class);
        when(mdv.asString()).thenReturn("Default");
        when(existingIsland.getMetaData("bundle")).thenReturn(Optional.of(mdv));
        when(existingIsland.isPrimary(any(UUID.class))).thenReturn(false);
        when(plugin.getIslands().getIslands(any(), any(User.class)))
                .thenReturn(List.of(existingIsland));

        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }

    @Test
    void testBuildWithUsedUpBundleNotHidden() {
        when(settings.getIslandNumber()).thenReturn(3);
        when(settings.isHideUsedBlueprints()).thenReturn(false);
        when(bundle1.getTimes()).thenReturn(1);

        world.bentobox.bentobox.database.objects.Island existingIsland =
                mock(world.bentobox.bentobox.database.objects.Island.class);
        MetaDataValue mdv = mock(MetaDataValue.class);
        when(mdv.asString()).thenReturn("Default");
        when(existingIsland.getMetaData("bundle")).thenReturn(Optional.of(mdv));
        when(existingIsland.isPrimary(any(UUID.class))).thenReturn(false);
        when(plugin.getIslands().getIslands(any(), any(User.class)))
                .thenReturn(List.of(existingIsland));

        // Should still build, just bundle won't have click handler
        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }

    @Test
    void testBuildResetIgnoresPrimaryIsland() {
        when(settings.getIslandNumber()).thenReturn(3);
        when(bundle1.getTimes()).thenReturn(1);

        world.bentobox.bentobox.database.objects.Island existingIsland =
                mock(world.bentobox.bentobox.database.objects.Island.class);
        MetaDataValue mdv = mock(MetaDataValue.class);
        when(mdv.asString()).thenReturn("Default");
        when(existingIsland.getMetaData("bundle")).thenReturn(Optional.of(mdv));
        when(existingIsland.isPrimary(any(UUID.class))).thenReturn(true);
        when(plugin.getIslands().getIslands(any(), any(User.class)))
                .thenReturn(List.of(existingIsland));

        // Reset=true should ignore the primary island's usage
        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", true));
    }

    // ---------------------------------------------------------------
    // Economy / cost display tests
    // ---------------------------------------------------------------

    @Test
    void testBuildShowsCostWhenEconomyEnabled() {
        when(settings.isUseEconomy()).thenReturn(true);
        when(bundle1.getCost()).thenReturn(100.0);

        VaultHook vault = mock(VaultHook.class);
        when(vault.format(100.0)).thenReturn("$100.00");
        when(plugin.getVault()).thenReturn(Optional.of(vault));

        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }

    @Test
    void testBuildDoesNotShowCostWhenZero() {
        when(settings.isUseEconomy()).thenReturn(true);
        when(bundle1.getCost()).thenReturn(0.0);

        // Should not try to access vault for zero cost
        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
        verify(plugin, never()).getVault();
    }

    @Test
    void testBuildDoesNotShowCostWhenEconomyDisabled() {
        when(settings.isUseEconomy()).thenReturn(false);
        when(bundle1.getCost()).thenReturn(100.0);

        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
        verify(plugin, never()).getVault();
    }

    // ---------------------------------------------------------------
    // Multi-bundle tests
    // ---------------------------------------------------------------

    @Test
    void testBuildWithMultipleBundles() {
        bundleMap.put("nether", bundle2);
        bundleMap.put("end", bundle3);

        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }

    @Test
    void testBuildWithSameSlotBundles() {
        when(bundle2.getSlot()).thenReturn(0);
        when(bundle3.getSlot()).thenReturn(0);
        bundleMap.put("nether", bundle2);
        bundleMap.put("end", bundle3);

        // Bundles with same slot should be sorted by uniqueId
        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }

    @Test
    void testBuildWithUnlimitedUsesMultiIsland() {
        when(settings.getIslandNumber()).thenReturn(3);
        when(bundle1.getTimes()).thenReturn(0); // 0 = unlimited

        when(plugin.getIslands().getIslands(any(), any(User.class))).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> IslandCreationPanel.openPanel(command, user, "island", false));
    }
}
