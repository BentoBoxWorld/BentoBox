package world.bentobox.bentobox.panels;

import world.bentobox.bentobox.api.prompts.PromptTestHelper;
import world.bentobox.bentobox.api.prompts.ChatInputListener;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;

/**
 * @author tastybento
 *
 */
class BlueprintManagementPanelTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private GameModeAddon addon;
    @Mock
    BlueprintBundle bb;
    @Mock
    BlueprintBundle bb2;
    @Mock
    BlueprintBundle bb3;

    private BlueprintManagementPanel bmp;
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private Inventory inv;
    @Mock
    private Blueprint blueprint;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(player);
        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);
        // Bundles
        Map<String, BlueprintBundle> map = new HashMap<>();
        when(bb.getUniqueId()).thenReturn("test");
        when(bb.getDisplayName()).thenReturn("test");
        when(bb.getIcon()).thenReturn(Material.STONE);
        when(bb.getIconItemStack()).thenReturn(new ItemStack(Material.STONE));
        when(bb.getDescription()).thenReturn(Collections.singletonList("A description"));
        when(bb.getCommands()).thenReturn(Collections.emptyList());
        when(bb.getSlot()).thenReturn(5);
        // Too small slot for panel
        when(bb2.getUniqueId()).thenReturn("test2");
        when(bb2.getDisplayName()).thenReturn("test2");
        when(bb2.getIcon()).thenReturn(Material.ACACIA_BOAT);
        when(bb2.getIconItemStack()).thenReturn(new ItemStack(Material.ACACIA_BOAT));
        when(bb2.getDescription()).thenReturn(Collections.singletonList("A description 2"));
        when(bb2.getSlot()).thenReturn(-5);
        // Too large slot for panel
        when(bb3.getUniqueId()).thenReturn("test3");
        when(bb3.getDisplayName()).thenReturn("test3");
        when(bb3.getIcon()).thenReturn(Material.BAKED_POTATO);
        when(bb3.getIconItemStack()).thenReturn(new ItemStack(Material.BAKED_POTATO));
        when(bb3.getDescription()).thenReturn(Collections.singletonList("A description 3"));
        when(bb3.getSlot()).thenReturn(65);

        map.put("test", bb);
        map.put("test2", bb2);
        map.put("test3", bb3);
        when(bpm.getBlueprintBundles(any())).thenReturn(map);

        // Blueprint
        when(blueprint.getName()).thenReturn("blueprint name");

        // Settings
        Settings settings = new Settings();
        // Ask in chat: a dialog cannot be built without a server
        settings.setDialogPrompts(false);
        when(plugin.getSettings()).thenReturn(settings);
        when(user.getTranslation(anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Set up
        bmp = new BlueprintManagementPanel(plugin, user, addon);

    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        PromptTestHelper.clear(user.getUniqueId());
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#openPanel()}.
     */
    @Test
    void testOpenPanel() {
        bmp.openPanel();
        verify(bpm).getBlueprintBundles(addon);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#openBB(world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle)}.
     */
    @Test
    void testOpenBB() {
        bmp.openBB(bb);
        verify(bb).getDisplayName();
        verify(bb, times(3)).getBlueprint(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getBundleIcon(world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle)}.
     */
    @Test
    void testGetBundleIcon() {
        PanelItem pi = bmp.getBundleIcon(bb);
        assertEquals("commands.admin.blueprint.management.edit-description", pi.getName());
        assertEquals(Material.STONE, pi.getItem().getType());
        assertEquals("A description", pi.getDescription().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getBlueprintItem(world.bentobox.bentobox.api.addons.GameModeAddon, int, world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle, world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    void testGetBlueprintItem() {
        PanelItem pi = bmp.getBlueprintItem(addon, 0, bb, blueprint);
        assertEquals("blueprint name", pi.getName());
        assertEquals(Material.PAPER, pi.getItem().getType());
        assertEquals("commands.admin.blueprint.management.blueprint-instruction", pi.getDescription().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getBlueprintItem(world.bentobox.bentobox.api.addons.GameModeAddon, int, world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle, world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    void testGetBlueprintItemWithDisplayNameAndIcon() {
        when(blueprint.getDisplayName()).thenReturn("Display Name");
        when(blueprint.getIcon()).thenReturn(Material.BEACON);
        when(blueprint.getIconItemStack()).thenReturn(new ItemStack(Material.BEACON));
        PanelItem pi = bmp.getBlueprintItem(addon, 0, bb, blueprint);
        assertEquals("Display Name", pi.getName());
        assertEquals(Material.BEACON, pi.getItem().getType());
        assertEquals("commands.admin.blueprint.management.blueprint-instruction", pi.getDescription().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getBlueprintItem(world.bentobox.bentobox.api.addons.GameModeAddon, int, world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle, world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    void testGetBlueprintItemWithDisplayNameAndIconInWorldSlot() {
        when(blueprint.getDisplayName()).thenReturn("Display Name");
        when(blueprint.getIcon()).thenReturn(Material.BEACON);
        when(blueprint.getIconItemStack()).thenReturn(new ItemStack(Material.BEACON));
        PanelItem pi = bmp.getBlueprintItem(addon, 5, bb, blueprint);
        assertEquals("Display Name", pi.getName());
        assertEquals(Material.BEACON, pi.getItem().getType());
        assertEquals("commands.admin.blueprint.management.remove", pi.getDescription().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getCommandsIcon}.
     */
    @Test
    void testGetCommandsIconNoCommands() {
        when(bb.getCommands()).thenReturn(Collections.emptyList());
        PanelItem pi = bmp.getCommandsIcon(addon, bb);
        assertEquals("commands.admin.blueprint.management.edit-commands", pi.getName());
        assertEquals(Material.COMMAND_BLOCK, pi.getItem().getType());
        assertEquals("commands.admin.blueprint.management.no-commands", pi.getDescription().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getCommandsIcon}.
     */
    @Test
    void testGetCommandsIconWithCommands() {
        when(bb.getCommands()).thenReturn(List.of("say hello [player]", "give [player] diamond 1"));
        PanelItem pi = bmp.getCommandsIcon(addon, bb);
        assertEquals("commands.admin.blueprint.management.edit-commands", pi.getName());
        assertEquals(Material.COMMAND_BLOCK, pi.getItem().getType());
        assertEquals("say hello [player]", pi.getDescription().getFirst());
        assertEquals("give [player] diamond 1", pi.getDescription().get(1));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#openPanel()}.
     * Verifies that many bundles (more than 36) are handled via pagination.
     */
    @Test
    void testOpenPanelWithManyBundles() {
        // Create 40 bundles (more than 36 per page)
        Map<String, BlueprintBundle> map = new HashMap<>();
        for (int i = 0; i < 40; i++) {
            BlueprintBundle bundle = mock(BlueprintBundle.class);
            when(bundle.getUniqueId()).thenReturn("bundle" + i);
            when(bundle.getDisplayName()).thenReturn("Bundle " + String.format("%02d", i));
            when(bundle.getIcon()).thenReturn(Material.STONE);
            when(bundle.getIconItemStack()).thenReturn(new ItemStack(Material.STONE));
            when(bundle.getDescription()).thenReturn(Collections.singletonList("Desc"));
            map.put("bundle" + i, bundle);
        }
        when(bpm.getBlueprintBundles(any())).thenReturn(map);

        bmp.openPanel();
        // Should not throw and should call getBlueprintBundles
        verify(bpm).getBlueprintBundles(addon);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#openBB(BlueprintBundle)}.
     * Verifies that many blueprints (more than 18) are handled via pagination.
     */
    @Test
    void testOpenBBWithManyBlueprints() {
        // Create 25 blueprints (more than 18 per page)
        Map<String, Blueprint> bpMap = new HashMap<>();
        for (int i = 0; i < 25; i++) {
            Blueprint bp = mock(Blueprint.class);
            when(bp.getName()).thenReturn("bp" + i);
            when(bp.getIcon()).thenReturn(Material.PAPER);
            bpMap.put("bp" + i, bp);
        }
        when(bpm.getBlueprints(any())).thenReturn(bpMap);

        bmp.openBB(bb);
        verify(bb).getDisplayName();
    }


    // ------------------------------------------------------------------
    // Prompts
    // ------------------------------------------------------------------

    private static final String MGMT = "commands.admin.blueprint.management.";

    private void say(String text) {
        PromptTestHelper.say(user.getUniqueId(), text);
    }

    @Test
    void testAskForNameMakesNewBundle() {
        bmp.askForName(user, addon, (BlueprintBundle) null);
        assertTrue(ChatInputListener.isWaitingFor(user.getUniqueId()));

        say("My Island");

        ArgumentCaptor<BlueprintBundle> made = ArgumentCaptor.forClass(BlueprintBundle.class);
        verify(bpm).addBlueprintBundle(eq(addon), made.capture());
        assertEquals("My Island", made.getValue().getDisplayName());
        assertEquals("my_island", made.getValue().getUniqueId());
        assertEquals(Material.RED_WOOL, made.getValue().getIcon());
        verify(bpm).saveBlueprintBundle(addon, made.getValue());
        verify(bpm, never()).deleteBlueprintBundle(any(), any());
        verify(user).sendMessage(MGMT + "name.success");
        assertFalse(ChatInputListener.isWaitingFor(user.getUniqueId()));
    }

    @Test
    void testAskForNameRefusesTakenName() {
        bmp.askForName(user, addon, (BlueprintBundle) null);

        say("Test2");

        verify(user).sendRawMessage(MGMT + "name.pick-a-unique-name");
        verify(bpm, never()).addBlueprintBundle(any(), any());
        assertTrue(ChatInputListener.isWaitingFor(user.getUniqueId()));
    }

    @Test
    void testAskForNameRefusesLongName() {
        bmp.askForName(user, addon, (BlueprintBundle) null);

        say("x".repeat(33));

        verify(user).sendRawMessage(MGMT + "name.too-long");
        verify(bpm, never()).addBlueprintBundle(any(), any());
    }

    /** A bundle may be given a name that still makes its own id, e.g. to change its colors */
    @Test
    void testAskForNameRenamesBundleKeepingItsId() {
        bmp.askForName(user, addon, bb);

        say("&aTest");

        verify(bpm).deleteBlueprintBundle(addon, bb);
        verify(bb).setDisplayName("&aTest");
        verify(bb).setUniqueId("test");
        verify(bpm).addBlueprintBundle(addon, bb);
        verify(bpm).saveBlueprintBundle(addon, bb);
    }

    @Test
    void testAskForNameDefaultBundleKeepsDefaultId() {
        when(bb.getUniqueId()).thenReturn(BlueprintsManager.DEFAULT_BUNDLE_NAME);

        bmp.askForName(user, addon, bb);
        say("Starter");

        verify(bb).setDisplayName("Starter");
        verify(bb).setUniqueId(BlueprintsManager.DEFAULT_BUNDLE_NAME);
    }

    @Test
    void testAskForBlueprintName() {
        bmp.askForBlueprintName(user, addon, blueprint, bb);

        say("New Name");

        verify(bpm).renameBlueprint(addon, blueprint, "new_name", "New Name");
        verify(bpm, never()).addBlueprintBundle(any(), any());
    }

    @Test
    void testAskForNameCancel() {
        bmp.askForName(user, addon, bb);

        say("prompts.chat.cancel-word");

        verify(bpm, never()).deleteBlueprintBundle(any(), any());
        verify(user).sendMessage("general.errors.command-cancelled");
        assertFalse(ChatInputListener.isWaitingFor(user.getUniqueId()));
    }

    @Test
    void testAskForDescription() {
        bmp.askForDescription(user, addon, bb);

        say("&aA green line");
        say("Plain line");
        verify(bb, never()).setDescription(anyList());
        say("prompts.chat.done-word");

        verify(bb).setDescription(List.of("\u00A7aA green line", "Plain line"));
        verify(bpm).saveBlueprintBundle(addon, bb);
        verify(user).sendMessage(MGMT + "description.success");
    }

    @Test
    void testAskForCommandsDropsBlankLines() {
        bmp.askForCommands(user, addon, bb);

        say("say hello [player]");
        say("");
        say("[SUDO] spawn");
        say("prompts.chat.done-word");

        verify(bb).setCommands(List.of("say hello [player]", "[SUDO] spawn"));
        verify(bpm).saveBlueprintBundle(addon, bb);
        verify(user).sendMessage(MGMT + "commands.success");
    }

    /** The overloads that take Bukkit's Conversable still work while Paper has that type */
    @SuppressWarnings("removal")
    @Test
    void testDeprecatedConversableOverloadStillAsks() {
        Player player = mockPlayer;
        User real = User.getInstance(player);
        try {
            bmp.askForName((org.bukkit.conversations.Conversable) player, addon, bb);
            assertTrue(ChatInputListener.isWaitingFor(real.getUniqueId()));
        } finally {
            PromptTestHelper.clear(real.getUniqueId());
        }
    }
}
