package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand;
import world.bentobox.bentobox.api.commands.island.team.IslandTeamKickCommand;
import world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand;
import world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand;

/**
 * Tests for {@link IslandTeamInviteGUI}.
 *
 * <p>Panel slot layout derived from team_invite_panel.yml:
 * <pre>
 *   Row 0 (border + nav): slot 1=PREVIOUS, slot 4=SEARCH, slot 7=NEXT
 *   Rows 1–5 (inner):     slot 10 = first PROSPECT (increments across rows)
 *   Row 5 last col:       slot 53=BACK
 * </pre>
 */
class IslandTeamInviteGUITest extends RanksManagerTestSetup {

    private static final int SLOT_PREVIOUS = 1;
    private static final int SLOT_SEARCH = 4;
    private static final int SLOT_NEXT = 7;
    private static final int SLOT_FIRST_PROSPECT = 10;
    private static final int SLOT_BACK = 53;

    @Mock
    private IslandTeamCommand itc;
    @Mock
    private IslandTeamInviteCommand itic;
    @Mock
    private IslandTeamCoopCommand coopCommand;
    @Mock
    private IslandTeamTrustCommand trustCommand;
    @Mock
    private IslandTeamKickCommand kickCommand;
    @Mock
    private IslandTeamSetownerCommand setOwnerCommand;
    @Mock
    private IslandTeamLeaveCommand leaveCommand;
    @Mock
    private IslandTeamInviteAcceptCommand acceptCommand;

    private IslandTeamInviteGUI gui;
    private User user;
    private File dataFolder;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        dataFolder = new File("test-invite-gui-" + uuid);
        new File(dataFolder, "panels").mkdirs();
        copyPanelYaml("panels/team_invite_panel.yml", new File(dataFolder, "panels/team_invite_panel.yml"));
        copyPanelYaml("panels/team_panel.yml", new File(dataFolder, "panels/team_panel.yml"));
        when(plugin.getDataFolder()).thenReturn(dataFolder);

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        when(itc.getPlugin()).thenReturn(plugin);
        when(itc.getInviteCommand()).thenReturn(itic);
        when(itc.getWorld()).thenReturn(world);
        when(itc.getCoopCommand()).thenReturn(coopCommand);
        when(itc.getTrustCommand()).thenReturn(trustCommand);
        when(itc.getKickCommand()).thenReturn(kickCommand);
        when(itc.getSetOwnerCommand()).thenReturn(setOwnerCommand);
        when(itc.getLeaveCommand()).thenReturn(leaveCommand);
        when(itc.getAcceptCommand()).thenReturn(acceptCommand);
        when(itc.getLabel()).thenReturn("team");

        when(itic.getLabel()).thenReturn("invite");

        when(im.getPrimaryIsland(world, uuid)).thenReturn(island);
        when(world.getPlayers()).thenReturn(Collections.emptyList());
        when(mockPlayer.getDisplayName()).thenReturn("tastybento");

        user = User.getInstance(mockPlayer);

        TemplateReader.clearPanels();
        PanelListenerManager.getOpenPanels().clear();

        // Panel file already present — constructor must NOT call saveResource
        gui = new IslandTeamInviteGUI(itc, true, island);
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

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    void testConstructor_fileAlreadyExists_doesNotSaveResource() {
        verify(plugin, never()).saveResource(anyString(), anyBoolean());
    }

    @Test
    void testConstructor_fileNotExists_savesResource() {
        new File(dataFolder, "panels/team_invite_panel.yml").delete();

        new IslandTeamInviteGUI(itc, true, island);

        verify(plugin).saveResource("panels/team_invite_panel.yml", false);
    }

    // -----------------------------------------------------------------------
    // build()
    // -----------------------------------------------------------------------

    @Test
    void testBuild_opensInventoryForUser() {
        gui.build(user);

        verify(mockPlayer).openInventory(any(Inventory.class));
    }

    @Test
    void testBuild_noPlayersInWorld_firstProspectSlotIsBlankBackground() {
        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        PanelItem item = panel.getItems().get(SLOT_FIRST_PROSPECT);
        assertNotNull(item);
        assertFalse(item.getClickHandler().isPresent(), "Blank background should have no click handler");
    }

    @Test
    void testBuild_visiblePlayer_prospectSlotHasClickHandler() {
        Player target = mockVisiblePlayer("target", UUID.randomUUID());
        when(world.getPlayers()).thenReturn(List.of(target));
        when(itc.isInvited(target.getUniqueId())).thenReturn(false);

        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        PanelItem item = panel.getItems().get(SLOT_FIRST_PROSPECT);
        assertNotNull(item);
        assertTrue(item.getClickHandler().isPresent());
    }

    @Test
    void testBuild_alreadyInvitedByUser_prospectHasNoClickHandler() {
        UUID targetUUID = UUID.randomUUID();
        Player target = mockVisiblePlayer("target", targetUUID);
        when(world.getPlayers()).thenReturn(List.of(target));
        when(itc.isInvited(targetUUID)).thenReturn(true);
        TeamInvite invite = mock(TeamInvite.class);
        when(itc.getInvite(targetUUID)).thenReturn(invite);
        when(invite.getInviter()).thenReturn(uuid);

        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        assertFalse(panel.getItems().get(SLOT_FIRST_PROSPECT).getClickHandler().isPresent(),
                "'Already invited' item should have no click handler");
    }

    @Test
    void testBuild_searchNameSet_filtersProspects() {
        UUID aliceUUID = UUID.randomUUID();
        UUID bobUUID = UUID.randomUUID();
        Player alice = mockVisiblePlayer("alice", aliceUUID);
        Player bob = mockVisiblePlayer("bob", bobUUID);
        when(world.getPlayers()).thenReturn(List.of(alice, bob));
        when(itc.isInvited(aliceUUID)).thenReturn(false);
        when(itc.isInvited(bobUUID)).thenReturn(false);

        gui.build(user);

        // Trigger a failed invite attempt to set searchName = "al"
        IslandTeamInviteGUI.InviteNamePrompt prompt = gui.new InviteNamePrompt();
        when(itic.canExecute(eq(user), anyString(), anyList())).thenReturn(false);
        prompt.acceptInput(mock(ConversationContext.class), "al");

        // Rebuild after search is set
        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        // "alice" matches "al" → first slot has handler; "bob" does not → second slot is blank
        assertTrue(panel.getItems().get(SLOT_FIRST_PROSPECT).getClickHandler().isPresent());
        assertFalse(panel.getItems().get(SLOT_FIRST_PROSPECT + 1).getClickHandler().isPresent());
    }

    // -----------------------------------------------------------------------
    // NEXT / PREVIOUS pagination buttons
    // -----------------------------------------------------------------------

    @Test
    void testBuild_nextButtonHasClickHandlerWhenPlayersExist() {
        Player p = mockVisiblePlayer("p", UUID.randomUUID());
        when(world.getPlayers()).thenReturn(List.of(p));

        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        assertTrue(panel.getItems().get(SLOT_NEXT).getClickHandler().isPresent());
    }

    @Test
    void testBuild_nextButtonIsBlankWhenNoVisiblePlayers() {
        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        assertFalse(panel.getItems().get(SLOT_NEXT).getClickHandler().isPresent());
    }

    @Test
    void testBuild_previousButtonIsBlankOnFirstPage() {
        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        assertFalse(panel.getItems().get(SLOT_PREVIOUS).getClickHandler().isPresent());
    }

    @Test
    void testNextButtonClick_advancesPage_previousButtonAppears() {
        Player target = mockVisiblePlayer("target", UUID.randomUUID());
        when(world.getPlayers()).thenReturn(List.of(target));
        when(itc.isInvited(target.getUniqueId())).thenReturn(false);

        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        clickHandler(panel, SLOT_NEXT, ClickType.LEFT);

        verify(mockPlayer).playSound(any(Location.class), eq(Sound.BLOCK_STONE_BUTTON_CLICK_ON), eq(1F), eq(1F));

        Panel newPanel = PanelListenerManager.getOpenPanels().get(uuid);
        assertTrue(newPanel.getItems().get(SLOT_PREVIOUS).getClickHandler().isPresent(),
                "PREVIOUS should be visible after advancing a page");
    }

    @Test
    void testPreviousButtonClick_decrementsPage() {
        Player target = mockVisiblePlayer("target", UUID.randomUUID());
        when(world.getPlayers()).thenReturn(List.of(target));
        when(itc.isInvited(target.getUniqueId())).thenReturn(false);

        gui.build(user);

        // Go to page 1
        Panel p0 = PanelListenerManager.getOpenPanels().get(uuid);
        clickHandler(p0, SLOT_NEXT, ClickType.LEFT);

        // Return to page 0
        Panel p1 = PanelListenerManager.getOpenPanels().get(uuid);
        assertTrue(p1.getItems().get(SLOT_PREVIOUS).getClickHandler().isPresent());
        clickHandler(p1, SLOT_PREVIOUS, ClickType.LEFT);

        verify(mockPlayer, times(2))
                .playSound(any(Location.class), eq(Sound.BLOCK_STONE_BUTTON_CLICK_ON), eq(1F), eq(1F));

        Panel p0Again = PanelListenerManager.getOpenPanels().get(uuid);
        assertFalse(p0Again.getItems().get(SLOT_PREVIOUS).getClickHandler().isPresent(),
                "PREVIOUS should be hidden after returning to page 0");
    }

    // -----------------------------------------------------------------------
    // BACK button
    // -----------------------------------------------------------------------

    @Test
    void testBackButtonClick_closesInventory() {
        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertTrue(panel.getItems().get(SLOT_BACK).getClickHandler().isPresent());
        clickHandler(panel, SLOT_BACK, ClickType.LEFT);

        verify(mockPlayer).closeInventory();
    }

    @Test
    void testBackButtonClick_notInviteCmd_closesInventory() {
        IslandTeamInviteGUI guiNotInvite = new IslandTeamInviteGUI(itc, false, island);
        guiNotInvite.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        clickHandler(panel, SLOT_BACK, ClickType.LEFT);

        verify(mockPlayer).closeInventory();
    }

    // -----------------------------------------------------------------------
    // Prospect click handlers
    // -----------------------------------------------------------------------

    @Test
    void testProspectLeftClick_canExecute_invitesPlayer() {
        setupSingleVisibleProspect("target", UUID.randomUUID());
        when(itic.canExecute(eq(user), anyString(), anyList())).thenReturn(true);
        when(itic.execute(eq(user), anyString(), anyList())).thenReturn(true);

        gui.build(user);
        clickProspect(ClickType.LEFT);

        verify(sch).runTask(eq(plugin), any(Runnable.class));
        verify(itic).canExecute(eq(user), eq("invite"), eq(List.of("target")));
        verify(itic).execute(eq(user), eq("invite"), eq(List.of("target")));
    }

    @Test
    void testProspectLeftClick_cannotExecute_doesNotCallExecute() {
        setupSingleVisibleProspect("target", UUID.randomUUID());
        when(itic.canExecute(eq(user), anyString(), anyList())).thenReturn(false);

        gui.build(user);
        clickProspect(ClickType.LEFT);

        verify(itic, never()).execute(any(), anyString(), anyList());
    }

    @Test
    void testProspectRightClick_canExecute_coopsPlayer() {
        setupSingleVisibleProspect("target", UUID.randomUUID());
        when(coopCommand.canExecute(eq(user), anyString(), anyList())).thenReturn(true);
        when(coopCommand.execute(eq(user), anyString(), anyList())).thenReturn(true);

        gui.build(user);
        clickProspect(ClickType.RIGHT);

        verify(sch).runTask(eq(plugin), any(Runnable.class));
        verify(coopCommand).canExecute(eq(user), eq("invite"), eq(List.of("target")));
        verify(coopCommand).execute(eq(user), eq("invite"), eq(List.of("target")));
    }

    @Test
    void testProspectShiftLeftClick_canExecute_trustsPlayer() {
        setupSingleVisibleProspect("target", UUID.randomUUID());
        when(trustCommand.canExecute(eq(user), anyString(), anyList())).thenReturn(true);
        when(trustCommand.execute(eq(user), anyString(), anyList())).thenReturn(true);

        gui.build(user);
        clickProspect(ClickType.SHIFT_LEFT);

        verify(sch).runTask(eq(plugin), any(Runnable.class));
        verify(trustCommand).canExecute(eq(user), eq("invite"), eq(List.of("target")));
        verify(trustCommand).execute(eq(user), eq("invite"), eq(List.of("target")));
    }

    @Test
    void testProspectUnknownClickType_isIgnored() {
        setupSingleVisibleProspect("target", UUID.randomUUID());

        gui.build(user);

        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        ClickHandler ch = panel.getItems().get(SLOT_FIRST_PROSPECT).getClickHandler().orElseThrow();
        boolean result = ch.onClick(panel, user, ClickType.MIDDLE, SLOT_FIRST_PROSPECT);

        assertTrue(result);
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }

    // -----------------------------------------------------------------------
    // InviteNamePrompt inner class
    // -----------------------------------------------------------------------

    @Test
    void testInviteNamePrompt_getPromptText_returnsTranslation() {
        gui.build(user);
        IslandTeamInviteGUI.InviteNamePrompt prompt = gui.new InviteNamePrompt();
        ConversationContext ctx = mock(ConversationContext.class);

        assertEquals("commands.island.team.invite.gui.enter-name", prompt.getPromptText(ctx));
    }

    @Test
    void testInviteNamePrompt_acceptInput_success_endsConversationWithoutRebuild() {
        gui.build(user);
        IslandTeamInviteGUI.InviteNamePrompt prompt = gui.new InviteNamePrompt();
        when(itic.canExecute(eq(user), anyString(), anyList())).thenReturn(true);
        when(itic.execute(eq(user), anyString(), anyList())).thenReturn(true);

        Prompt result = prompt.acceptInput(mock(ConversationContext.class), "validPlayer");

        assertEquals(Prompt.END_OF_CONVERSATION, result);
        verify(itic).canExecute(user, "invite", List.of("validPlayer"));
        verify(itic).execute(user, "invite", List.of("validPlayer"));
        verify(sch, never()).runTaskLater(any(), any(Runnable.class), anyLong());
    }

    @Test
    void testInviteNamePrompt_acceptInput_cannotExecute_schedulesRebuild() {
        gui.build(user);
        IslandTeamInviteGUI.InviteNamePrompt prompt = gui.new InviteNamePrompt();
        when(itic.canExecute(eq(user), anyString(), anyList())).thenReturn(false);

        Prompt result = prompt.acceptInput(mock(ConversationContext.class), "unknownPlayer");

        assertEquals(Prompt.END_OF_CONVERSATION, result);
        verify(sch).runTaskLater(eq(plugin), any(Runnable.class), eq(20L));
    }

    @Test
    void testInviteNamePrompt_acceptInput_executeFails_schedulesRebuild() {
        gui.build(user);
        IslandTeamInviteGUI.InviteNamePrompt prompt = gui.new InviteNamePrompt();
        when(itic.canExecute(eq(user), anyString(), anyList())).thenReturn(true);
        when(itic.execute(eq(user), anyString(), anyList())).thenReturn(false);

        Prompt result = prompt.acceptInput(mock(ConversationContext.class), "somePlayer");

        assertEquals(Prompt.END_OF_CONVERSATION, result);
        verify(sch).runTaskLater(eq(plugin), any(Runnable.class), eq(20L));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Player mockVisiblePlayer(String name, UUID id) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(id);
        when(p.getName()).thenReturn(name);
        when(p.getDisplayName()).thenReturn(name);
        when(mockPlayer.canSee(p)).thenReturn(true);
        return p;
    }

    private void setupSingleVisibleProspect(String name, UUID id) {
        Player target = mockVisiblePlayer(name, id);
        when(world.getPlayers()).thenReturn(List.of(target));
        when(itc.isInvited(id)).thenReturn(false);
    }

    /** Retrieves and fires the click handler at the given slot. */
    private void clickHandler(Panel panel, int slot, ClickType clickType) {
        PanelItem item = panel.getItems().get(slot);
        assertNotNull(item, "Expected a panel item at slot " + slot);
        ClickHandler ch = item.getClickHandler()
                .orElseThrow(() -> new AssertionError("No click handler at slot " + slot));
        ch.onClick(panel, user, clickType, slot);
    }

    /** Builds the panel (must already have a single visible prospect) then clicks it. */
    private void clickProspect(ClickType clickType) {
        Panel panel = PanelListenerManager.getOpenPanels().get(uuid);
        assertNotNull(panel);
        clickHandler(panel, SLOT_FIRST_PROSPECT, clickType);
    }
}
