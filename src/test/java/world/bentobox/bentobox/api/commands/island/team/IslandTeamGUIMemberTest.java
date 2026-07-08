package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
 * rank line come from customisable locale keys (with a fallback when the keys
 * are missing). See issue #3009.
 *
 * <p>Panel slot layout derived from team_panel.yml: the first member button is
 * at row 2, column 2 → slot 10.
 */
class IslandTeamGUIMemberTest extends RanksManagerTestSetup {

    private static final int SLOT_FIRST_MEMBER = 10;
    private static final String MEMBER_NAME_REF = "commands.island.team.gui.buttons.member.name";
    private static final String MEMBER_DESC_REF = "commands.island.team.gui.buttons.member.description";
    private static final String MEMBER_LAST_SEEN_REF = "commands.island.team.gui.buttons.member.last-seen";
    private static final String LAST_SEEN_LAYOUT_REF = "commands.island.team.info.last-seen.layout";

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
        when(mockPlayer.getName()).thenReturn("tastybento");
        when(mockPlayer.getDisplayName()).thenReturn("Bento");

        // Concrete locale values so the panel renders real strings, and we can verify placeholder substitution.
        when(rm.getRank(OWNER_RANK)).thenReturn("ranks.owner");
        when(lm.get(any(), eq("ranks.owner"))).thenReturn("Owner");
        when(lm.get(any(), eq(MEMBER_NAME_REF))).thenReturn("Member: [display_name]");
        when(lm.get(any(), eq(MEMBER_DESC_REF))).thenReturn("Rank: [rank]");

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

    private PanelItem buildMemberButton() {
        new IslandTeamGUI(plugin, itc, user, island).build();
        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        PanelItem item = panel.getItems().get(SLOT_FIRST_MEMBER);
        assertNotNull(item, "Expected a member button at slot " + SLOT_FIRST_MEMBER);
        return item;
    }

    @Test
    void testMemberButtonName_rendersFromLocaleKeyWithPlaceholder() {
        // member.name = "Member: [display_name]" → [display_name] substituted with the member's display name
        assertEquals("Member: Bento", buildMemberButton().getName());
    }

    @Test
    void testMemberButtonDescription_rankLineRendersFromLocaleKeyWithPlaceholder() {
        // member.description = "Rank: [rank]" → [rank] substituted with the resolved rank name
        assertEquals("Rank: Owner", buildMemberButton().getDescription().get(0));
    }

    @Test
    void testMemberButtonName_fallsBackToDisplayNameWhenKeyMissing() {
        // Simulate an older/custom locale that lacks the key: getTranslation echoes the reference back
        when(lm.get(any(), eq(MEMBER_NAME_REF))).thenReturn(MEMBER_NAME_REF);

        assertEquals("Bento", buildMemberButton().getName());
    }

    @Test
    void testMemberButtonDescription_fallsBackToRankNameWhenKeyMissing() {
        // Simulate an older/custom locale that lacks the key: rank line falls back to the raw rank name
        when(lm.get(any(), eq(MEMBER_DESC_REF))).thenReturn(MEMBER_DESC_REF);

        assertEquals("Owner", buildMemberButton().getDescription().get(0));
    }

    @Test
    void testMemberButtonName_offlineMemberUsesSameNameKeyWithLastSeenSuffix() {
        // The offline member's name must come from the same customisable member.name key (issue #3011
        // follow-up), with the last-seen status supplied through the [last_seen] placeholder.
        when(mockPlayer.isOnline()).thenReturn(false);
        // Member rank or above -> a last-seen status is shown
        when(island.getMemberSet(MEMBER_RANK, true)).thenReturn(ImmutableSet.of(uuid));

        OfflinePlayer offline = mock(OfflinePlayer.class);
        when(offline.getUniqueId()).thenReturn(uuid);
        when(offline.getName()).thenReturn("tastybento");
        when(offline.getLastSeen()).thenReturn(System.currentTimeMillis());
        mockedBukkit.when(() -> Bukkit.getOfflinePlayer(uuid)).thenReturn(offline);

        when(lm.get(any(), eq(MEMBER_NAME_REF))).thenReturn("Member: [display_name][last_seen]");
        when(lm.get(any(), eq(MEMBER_LAST_SEEN_REF))).thenReturn(" ([last_seen])");
        when(lm.get(any(), eq(LAST_SEEN_LAYOUT_REF))).thenReturn("recently");

        assertEquals("Member: Bento (recently)", buildMemberButton().getName());
    }

    @Test
    void testMemberButtonName_offlineTrustedMemberHasNoLastSeenSuffix() {
        // Trusted or below -> no last-seen status, so the [last_seen] suffix is empty
        when(mockPlayer.isOnline()).thenReturn(false);
        when(island.getMemberSet(MEMBER_RANK, true)).thenReturn(ImmutableSet.of());

        when(lm.get(any(), eq(MEMBER_NAME_REF))).thenReturn("Member: [display_name][last_seen]");

        assertEquals("Member: Bento", buildMemberButton().getName());
    }
}
