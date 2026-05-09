package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for {@link AdminTeamDisbandAllCommand}.
 */
class AdminTeamDisbandAllCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private BukkitScheduler scheduler;

    private UUID ownerUUID;
    private UUID memberUUID;
    private UUID subOwnerUUID;
    private UUID trustedUUID;
    private UUID coopUUID;

    private AdminTeamDisbandAllCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        when(user.isOp()).thenReturn(false);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("admin");
        when(user.getTranslation(any()))
                .thenAnswer((Answer<String>) i -> i.getArgument(0, String.class));

        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bsbadmin");
        when(ac.getWorld()).thenReturn(world);

        when(plugin.getPlayers()).thenReturn(pm);

        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenReturn("mock translation");

        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        @NonNull WorldSettings ws = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Scheduler — capture and run runnables on demand for confirmation flow.
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(mock(BukkitTask.class));

        // Owner + ranks setup
        ownerUUID = uuid;
        memberUUID = UUID.randomUUID();
        subOwnerUUID = UUID.randomUUID();
        trustedUUID = UUID.randomUUID();
        coopUUID = UUID.randomUUID();

        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.getCenter()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));
        // Members set with rank >= MEMBER_RANK and includeAboveRanks=true: owner, sub-owner, member.
        // Stub the generic form last-wins so it covers the call site (RanksManager.MEMBER_RANK, true).
        when(island.getMemberSet(anyInt(), anyBoolean()))
                .thenReturn(ImmutableSet.of(ownerUUID, subOwnerUUID, memberUUID));
        when(island.getRank(ownerUUID)).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRank(subOwnerUUID)).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(island.getRank(memberUUID)).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRank(trustedUUID)).thenReturn(RanksManager.TRUSTED_RANK);
        when(island.getRank(coopUUID)).thenReturn(RanksManager.COOP_RANK);

        Collection<Island> islandsInWorld = List.of(island);
        doReturn(islandsInWorld).when(im).getIslands(world);

        cmd = new AdminTeamDisbandAllCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * First execute should ask for confirmation; nothing should be removed yet.
     */
    @Test
    void testExecutePromptsConfirmation() {
        assertTrue(cmd.execute(user, cmd.getLabel(), List.of()));
        verify(im, never()).removePlayer(any(Island.class), any(UUID.class));
        verify(user).sendMessage(eq("commands.confirmation.confirm"), eq("[seconds]"),
                any(String.class));
    }

    /**
     * Second execute (after confirmation) should remove members and sub-owners but not the owner,
     * trusted, or coop ranks.
     */
    @Test
    void testExecuteRemovesMembersAndSubOwnersOnly() {
        // Prime confirmation
        assertTrue(cmd.execute(user, cmd.getLabel(), List.of()));
        // Capture the disband runnable scheduled by askConfirmation when it fires the second time.
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        // Confirm
        assertTrue(cmd.execute(user, cmd.getLabel(), List.of()));
        verify(scheduler).runTask(any(), runnableCaptor.capture());
        // Run the captured disband-all action.
        runnableCaptor.getValue().run();

        // Owner is preserved.
        verify(im, never()).removePlayer(island, ownerUUID);
        // Trust/coop are not in the >= MEMBER_RANK set, so should never be touched.
        verify(im, never()).removePlayer(island, trustedUUID);
        verify(im, never()).removePlayer(island, coopUUID);
        // Members and sub-owners are removed.
        verify(im).removePlayer(island, memberUUID);
        verify(im).removePlayer(island, subOwnerUUID);
        // TeamEvent + IslandEvent are each fired per removed player; the builders also dispatch
        // a deprecated copy each, so 3 callEvent invocations per player × 2 players = 6.
        verify(pim, times(6)).callEvent(any());
        verify(user).sendMessage(eq("commands.admin.team.disbandall.success"), any(), any(),
                any(), any());
    }
}
