package world.bentobox.bentobox.panels.customizable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.listeners.flags.clicklisteners.CommandCycleClick;
import world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.ItemParser;

/**
 * Tests for {@link CommandRanksPanel}, using the shipped {@code panels/command_ranks_panel.yml}
 * template so that the default layout is what is tested.
 */
class CommandRanksPanelTest extends RanksManagerTestSetup {

    private static final Path RESOURCES = Path.of("src", "main", "resources");
    /** Slots of the default template */
    private static final int PREVIOUS = 46;
    private static final int NEXT = 52;

    @Mock
    private User user;
    @Mock
    private CompositeCommand command;
    @Mock
    private GameModeAddon gma;
    @Mock
    private CommandsManager cm;

    private UUID uuid;
    private final Map<String, CompositeCommand> subCommands = new LinkedHashMap<>();
    private CommandRankClickListener commandRanks;
    private MockedStatic<ItemParser> mockedItemParser;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Item parsing needs a real item factory, which the mocked Bukkit does not provide, so
        // parse plain material names directly
        mockedItemParser = Mockito.mockStatic(ItemParser.class);
        mockedItemParser.when(() -> ItemParser.parse(anyString()))
                .thenAnswer(inv -> new ItemStack(Material.valueOf(inv.getArgument(0, String.class))));
        mockedItemParser.when(() -> ItemParser.parse(any(), any())).thenAnswer(inv -> {
            String text = inv.getArgument(0);
            return text == null || text.isBlank() ? inv.getArgument(1) : new ItemStack(Material.valueOf(text));
        });
        when(plugin.getDataFolder()).thenReturn(RESOURCES.toFile());
        when(iwm.getHiddenFlags(any())).thenReturn(Collections.emptyList());

        // Command
        when(command.getPlugin()).thenReturn(plugin);
        when(command.getWorld()).thenReturn(world);

        // User
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getLocation()).thenReturn(location);
        when(user.getWorld()).thenReturn(world);
        when(user.hasPermission(anyString())).thenReturn(true);
        Answer<String> answer = inv -> {
            Object[] args = inv.getArguments();
            String text = (String) args[0];
            for (int i = 1; i + 1 < args.length; i += 2) {
                text = text.replace(String.valueOf(args[i]), String.valueOf(args[i + 1]));
            }
            return text;
        };
        when(user.getTranslation(anyString())).thenAnswer(answer);
        when(user.getTranslation(anyString(), any(String[].class))).thenAnswer(answer);
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), any(String[].class))).thenReturn("");

        // Island
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.MEMBER_RANK);

        // The game mode's command, with rank-configurable sub-commands
        when(plugin.getCommandsManager()).thenReturn(cm);
        CompositeCommand top = mock(CompositeCommand.class);
        when(top.getWorld()).thenReturn(world);
        when(top.getName()).thenReturn("island");
        when(top.testPermission(any())).thenReturn(true);
        when(top.getSubCommands()).thenReturn(subCommands);
        when(cm.getCommands()).thenReturn(Map.of("island", top));
        commandRanks = new CommandRankClickListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        TemplateReader.clearPanels();
        mockedItemParser.close();
        super.tearDown();
    }

    private void addCommands(int count) {
        for (int i = 0; i < count; i++) {
            CompositeCommand sub = mock(CompositeCommand.class);
            when(sub.getName()).thenReturn(String.format("sub%02d", i));
            when(sub.isConfigurableRankCommand()).thenReturn(true);
            when(sub.getSubCommands()).thenReturn(Collections.emptyMap());
            subCommands.put(sub.getName(), sub);
        }
    }

    private CommandRanksPanel open() {
        assertTrue(CommandRanksPanel.openPanel(command, user, world, island, commandRanks));
        return (CommandRanksPanel) PanelListenerManager.getOpenPanels().get(uuid).getListener().orElseThrow();
    }

    private static Map<Integer, PanelItem> items(CommandRanksPanel crp) {
        return crp.getPanel().getItems();
    }

    /**
     * The default template reproduces the old panel: one map per command from the first slot, the
     * command ranks title, and no paging when everything fits.
     */
    @Test
    void testDefaultLayout() {
        addCommands(3);
        CommandRanksPanel crp = open();
        Map<Integer, PanelItem> items = items(crp);

        assertEquals("protection.flags.COMMAND_RANKS.name", crp.getPanel().getName());
        assertEquals(Material.MAP, items.get(0).getItem().getType());
        assertEquals("protection.panel.flag-item.name-layout", items.get(0).getName());
        assertEquals(Material.MAP, items.get(2).getItem().getType());
        assertNull(items.get(3));
        assertNull(items.get(PREVIOUS));
        assertNull(items.get(NEXT));
        assertInstanceOf(CommandCycleClick.class, items.get(0).getClickHandler().orElseThrow());
        assertEquals(island, crp.getPanel().getIsland());
    }

    /**
     * More commands than fit on a page are paged, 45 to a page, instead of being cut off at 49.
     */
    @Test
    void testPaging() {
        addCommands(50);
        CommandRanksPanel crp = open();
        assertEquals(50, crp.getCommands().size());
        Map<Integer, PanelItem> items = items(crp);
        assertEquals(Material.MAP, items.get(44).getItem().getType());
        assertNull(items.get(PREVIOUS));
        assertEquals(Material.ARROW, items.get(NEXT).getItem().getType());

        items.get(NEXT).getClickHandler().orElseThrow().onClick(crp.getPanel(), user, ClickType.LEFT, NEXT);
        crp.refreshPanel();
        items = items(crp);
        assertEquals(1, crp.getPageIndex());
        assertEquals(Material.MAP, items.get(4).getItem().getType());
        assertNull(items.get(5));
        assertEquals(Material.ARROW, items.get(PREVIOUS).getItem().getType());
        assertNull(items.get(NEXT));
    }

    @Test
    void testTemplateMissingReturnsFalse(@TempDir Path empty) {
        when(plugin.getDataFolder()).thenReturn(empty.toFile());
        assertFalse(CommandRanksPanel.openPanel(command, user, world, island, commandRanks));
        assertNull(PanelListenerManager.getOpenPanels().get(uuid));
    }

    /**
     * A game mode's own template takes precedence, and its command button can set the icon and the
     * name layout.
     */
    @Test
    void testGameModeTemplateOverrides(@TempDir Path addonFolder) throws IOException {
        addCommands(1);
        Path panels = Files.createDirectories(addonFolder.resolve("panels"));
        String yml = Files.readString(RESOURCES.resolve("panels").resolve("command_ranks_panel.yml"),
                StandardCharsets.UTF_8)
                .replace("title: protection.flags.COMMAND_RANKS.name", "title: custom.title")
                .replace("    command_button:\n      data:",
                        "    command_button:\n      icon: PAPER\n      title: 'custom [name]'\n      data:");
        Files.writeString(panels.resolve("command_ranks_panel.yml"), yml, StandardCharsets.UTF_8);
        when(gma.getDataFolder()).thenReturn(addonFolder.toFile());
        when(command.getAddon()).thenReturn(gma);

        CommandRanksPanel crp = open();

        assertEquals("custom.title", crp.getPanel().getName());
        PanelItem item = items(crp).get(0);
        assertEquals(Material.PAPER, item.getItem().getType());
        assertEquals("custom /island sub00", item.getName());
    }
}
