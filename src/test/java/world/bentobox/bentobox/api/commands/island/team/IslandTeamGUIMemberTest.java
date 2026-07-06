package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Tests for the member button in {@link IslandTeamGUI}, verifying its name and
 * rank line come from customisable locale keys rather than being hardcoded.
 * See issue #3009.
 *
 * <p>Panel slot layout derived from team_panel.yml: the first member button is
 * at row 2, column 2 → slot 10.
 */
class IslandTeamGUIMemberTest extends RanksManagerTestSetup {

    private static final int SLOT_FIRST_MEMBER = 10;

    @Mock
    private IslandTeamCommand itc;
    @Mock
    private IslandTeamInviteCommand itic;
    @Mock
    private IslandTeamKickCommand kickCommand;
    @Mock
    private IslandTeamSetownerCommand setOwnerCommand;
    @Mock
    private IslandTeamLeaveCommand leaveCommand;

    private User user;
    private File dataFolder;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        dataFolder = new File("test-team-gui-member-" + uuid);
        new File(dataFolder, "panels").mkdirs();
        copyPanelYaml("panels/team_panel.yml", new File(dataFolder, "panels/team_panel.yml"));
        when(plugin.getDataFolder()).thenReturn(dataFolder);

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        when(itc.getWorld()).thenReturn(world);
        when(itc.getLabel()).thenReturn("team");
        when(itc.getInviteCommand()).thenReturn(itic);
        when(itic.getPermission()).thenReturn("island.team.invite");
        when(itc.getKickCommand()).thenReturn(kickCommand);
        when(kickCommand.getPermission()).thenReturn("island.team.kick");
        when(itc.getSetOwnerCommand()).thenReturn(setOwnerCommand);
        when(setOwnerCommand.getPermission()).thenReturn("island.team.setowner");
        when(itc.getLeaveCommand()).thenReturn(leaveCommand);
        when(leaveCommand.getPermission()).thenReturn("island.team.leave");

        when(im.getPrimaryIsland(world, uuid)).thenReturn(island);
        when(im.getMaxMembers(any(), anyInt())).thenReturn(4);
        // Status button member sets are empty; the member button set (rank above visitor -> 0) holds the viewer
        when(island.getMemberSet()).thenReturn(ImmutableSet.of());
        when(island.getMemberSet(anyInt())).thenReturn(ImmutableSet.of());
        when(island.getMemberSet(anyInt(), anyBoolean())).thenReturn(ImmutableSet.of());
        when(island.getMemberSet(0)).thenReturn(ImmutableSet.of(uuid));
        when(island.getRank(any(User.class))).thenReturn(OWNER_RANK);

        // The single member is the viewer, and is online
        when(mockPlayer.isOnline()).thenReturn(true);
        when(mockPlayer.getDisplayName()).thenReturn("tastybento");

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
            if (in != null) {
                Files.copy(in, dest.toPath());
            }
        }
    }

    @Test
    void testMemberButtonName_comesFromCustomisableLocaleKey() {
        new IslandTeamGUI(plugin, itc, user, island).build();

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        PanelItem item = panel.getItems().get(SLOT_FIRST_MEMBER);
        assertNotNull(item, "Expected a member button at slot " + SLOT_FIRST_MEMBER);
        // Online member name now comes from the member.name locale key, not the hardcoded display name
        assertEquals("commands.island.team.gui.buttons.member.name", item.getName());
    }

    @Test
    void testMemberButtonDescription_rankLineComesFromCustomisableLocaleKey() {
        new IslandTeamGUI(plugin, itc, user, island).build();

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        PanelItem item = panel.getItems().get(SLOT_FIRST_MEMBER);
        assertNotNull(item, "Expected a member button at slot " + SLOT_FIRST_MEMBER);
        assertFalse(item.getDescription().isEmpty(), "Member description should have a rank line");
        // The rank line is routed through the customisable member.description key
        assertEquals("commands.island.team.gui.buttons.member.description", item.getDescription().get(0));
    }
}
