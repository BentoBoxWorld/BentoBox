package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.managers.CommandsManager;

/**
 * Tests for {@link IslandTeamGUI}, focused on the icons being sourced from the
 * panel template (team_panel.yml) rather than hardcoded. See issue #3008.
 *
 * <p>Panel slot layout derived from team_panel.yml:
 * <pre>
 *   Row 0 (nav):  slot 0=STATUS, slot 2=RANK filter, slot 4=INVITED, slot 6=INVITE
 * </pre>
 */
class IslandTeamGUITest extends RanksManagerTestSetup {

    private static final int SLOT_RANK = 2;

    @Mock
    private IslandTeamCommand itc;
    @Mock
    private IslandTeamInviteCommand itic;

    private User user;
    private File dataFolder;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        dataFolder = new File("test-team-gui-" + uuid);
        new File(dataFolder, "panels").mkdirs();
        copyPanelYaml("panels/team_panel.yml", new File(dataFolder, "panels/team_panel.yml"));
        when(plugin.getDataFolder()).thenReturn(dataFolder);

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Make createItemStack throw so ItemParser.parse() falls through to parseOld(),
        // which resolves plain material names (e.g. AMETHYST_SHARD, HOPPER) via Material.valueOf.
        when(itemFactory.createItemStack(anyString())).thenThrow(new IllegalArgumentException("test"));

        when(itc.getPlugin()).thenReturn(plugin);
        when(itc.getWorld()).thenReturn(world);
        when(itc.getLabel()).thenReturn("team");
        when(itc.getInviteCommand()).thenReturn(itic);
        when(itic.getPermission()).thenReturn("island.team.invite");

        // Island with no members so the member/status buttons resolve to blanks without NPE
        when(im.getPrimaryIsland(world, uuid)).thenReturn(island);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of());
        when(island.getMemberSet(anyInt())).thenReturn(ImmutableSet.of());
        when(island.getMemberSet(anyInt(), anyBoolean())).thenReturn(ImmutableSet.of());
        when(island.getRank(any(User.class))).thenReturn(OWNER_RANK);
        when(im.getMaxMembers(any(), anyInt())).thenReturn(4);

        user = User.getInstance(mockPlayer);

        TemplateReader.clearPanels();
        PanelListenerManager.getOpenPanels().clear();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        deleteAll(dataFolder);
        TemplateReader.clearPanels();
        PanelListenerManager.getOpenPanels().clear();
    }

    private void copyPanelYaml(String resource, File dest) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "Missing test fixture on the classpath: " + resource);
            Files.copy(in, dest.toPath());
        }
    }

    /**
     * The shipped template sets the rank filter icon to AMETHYST_SHARD, so the
     * built button must use that material rather than an internally hardcoded one.
     */
    @Test
    void testRankFilterIcon_comesFromTemplate() {
        new IslandTeamGUI(plugin, itc, user, island).build();

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        PanelItem item = panel.getItems().get(SLOT_RANK);
        assertNotNull(item, "Expected rank filter button at slot " + SLOT_RANK);
        assertEquals(Material.AMETHYST_SHARD, item.getItem().getType());
    }

    /**
     * When an admin overrides the rank filter icon in team_panel.yml (e.g. to a
     * HOPPER), the override must be honoured rather than ignored. This is the
     * core of issue #3008.
     */
    @Test
    void testRankFilterIcon_templateOverrideIsRespected() throws IOException {
        // Rewrite the panel's rank filter icon to HOPPER
        File panelFile = new File(dataFolder, "panels/team_panel.yml");
        String yaml = Files.readString(panelFile.toPath()).replace("icon: AMETHYST_SHARD", "icon: HOPPER");
        Files.writeString(panelFile.toPath(), yaml);
        TemplateReader.clearPanels();

        new IslandTeamGUI(plugin, itc, user, island).build();

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        PanelItem item = panel.getItems().get(SLOT_RANK);
        assertNotNull(item, "Expected rank filter button at slot " + SLOT_RANK);
        assertEquals(Material.HOPPER, item.getItem().getType(),
                "Rank filter icon should come from the template, allowing admin override");
    }
}
