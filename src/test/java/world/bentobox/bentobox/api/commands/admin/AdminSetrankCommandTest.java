package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class AdminSetrankCommandTest extends RanksManagerTestSetup {

    private static final String TARGET = "target";
    private static final String OWNER = "owner";
    private static final String XYZ = "0,0,0";
    private static final String XYZ2 = "100,64,100";

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Island island2;

    private AdminSetrankCommand c;

    private UUID targetUUID;
    private UUID ownerUUID;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);
        User.setPlugin(plugin);

        when(ac.getWorld()).thenReturn(world);

        // Players Manager
        when(plugin.getPlayers()).thenReturn(pm);
        targetUUID = UUID.randomUUID();
        ownerUUID = UUID.randomUUID();
        when(pm.getUUID(TARGET)).thenReturn(targetUUID);
        when(pm.getUUID(OWNER)).thenReturn(ownerUUID);
        when(pm.getName(ownerUUID)).thenReturn(OWNER);

        // island2 is a team island owned by "owner"; the target is a plain member of it
        when(island2.getOwner()).thenReturn(ownerUUID);
        when(island2.getCenter()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));
        when(island2.getRank(targetUUID)).thenReturn(RanksManager.MEMBER_RANK);
        when(im.getIslands(world, targetUUID)).thenReturn(List.of(island2));
        when(im.getIslands(world, ownerUUID)).thenReturn(List.of(island2));

        // Online players
        mockedUtil.when(() -> Util.getOnlinePlayerList(any())).thenReturn(Collections.singletonList("tastybento"));
        mockedUtil.when(() -> Util.getUUID(anyString())).thenCallRealMethod();
        mockedUtil.when(() -> Util.xyz(any())).thenCallRealMethod();
        mockedUtil.when(() -> Util.tabLimit(any(), any())).thenCallRealMethod();
        mockedUtil.when(() -> Util.stripColor(anyString())).thenCallRealMethod();

        // Translations
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Ranks: the mocked manager returns the reference for each value
        when(rm.getRank(RanksManager.MEMBER_RANK)).thenReturn(RanksManager.MEMBER_RANK_REF);
        when(rm.getRank(RanksManager.SUB_OWNER_RANK)).thenReturn(RanksManager.SUB_OWNER_RANK_REF);
        when(rm.getRank(RanksManager.TRUSTED_RANK)).thenReturn(RanksManager.TRUSTED_RANK_REF);

        // Command
        c = new AdminSetrankCommand(ac);
    }

    /**
     * Adds a second team island that the target is also a member of.
     */
    private void addSecondIsland(UUID owner) {
        when(island.getOwner()).thenReturn(owner);
        Location loc2 = mock(Location.class);
        when(loc2.toVector()).thenReturn(new Vector(100, 64, 100));
        when(island.getCenter()).thenReturn(loc2);
        when(island.getRank(targetUUID)).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(im.getIslands(world, targetUUID)).thenReturn(List.of(island, island2));
        when(im.getIslands(world, owner)).thenReturn(List.of(island, island2));
    }

    @Test
    void testAdminSetrankCommand() {
        assertEquals("setrank", c.getLabel());
    }

    @Test
    void testSetup() {
        assertEquals("admin.setrank", c.getPermission());
        assertFalse(c.isOnlyPlayer());
        assertEquals("commands.admin.setrank.parameters", c.getParameters());
        assertEquals("commands.admin.setrank.description", c.getDescription());
    }

    @Test
    void testCanExecuteNoArgs() {
        assertFalse(c.canExecute(user, "", Collections.emptyList()));
        verify(user).getTranslation("commands.admin.setrank.description");
    }

    @Test
    void testCanExecuteOneArg() {
        assertFalse(c.canExecute(user, "", Collections.singletonList("test")));
        verify(user).getTranslation("commands.admin.setrank.description");
    }

    @Test
    void testCanExecuteTooManyArgs() {
        assertFalse(c.canExecute(user, "", List.of("a", "b", "c", "d")));
        verify(user).getTranslation("commands.admin.setrank.description");
    }

    @Test
    void testCanExecuteUnknownPlayer() {
        assertFalse(c.canExecute(user, "", List.of("tastybento", "member")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    @Test
    void testCanExecuteUnknownRank() {
        assertFalse(c.canExecute(user, "", List.of(TARGET, "xxx")));
        verify(user).sendMessage("commands.admin.setrank.unknown-rank", TextVariables.RANK, "xxx", "[ranks]",
                "coop, trusted, member, sub-owner");
    }

    @Test
    void testCanExecuteTooLowRank() {
        assertFalse(c.canExecute(user, "", List.of(TARGET, "visitor")));
        verify(user).sendMessage("commands.admin.setrank.not-possible");
    }

    @Test
    void testCanExecuteBannedRank() {
        assertFalse(c.canExecute(user, "", List.of(TARGET, "banned")));
        verify(user).sendMessage("commands.admin.setrank.not-possible");
    }

    @Test
    void testCanExecuteOwnerRankRefused() {
        assertFalse(c.canExecute(user, "", List.of(TARGET, "owner")));
        verify(user).sendMessage("commands.admin.setrank.cannot-set-owner");
    }

    @Test
    void testCanExecuteAdminRankRefused() {
        assertFalse(c.canExecute(user, "", List.of(TARGET, "admin")));
        verify(user).sendMessage("commands.admin.setrank.cannot-set-owner");
    }

    /**
     * The legacy form - the rank's locale reference - still works because the mocked translation
     * returns the reference itself.
     */
    @Test
    void testCanExecuteRankByReference() {
        assertTrue(c.canExecute(user, "", List.of(TARGET, "ranks.sub-owner")));
    }

    @Test
    void testCanExecuteRankByTranslatedName() {
        when(user.getTranslation(RanksManager.SUB_OWNER_RANK_REF)).thenReturn("§bSous-Chef");
        assertTrue(c.canExecute(user, "", List.of(TARGET, "SOUS-chef")));
    }

    @Test
    void testCanExecuteRankByNumber() {
        assertTrue(c.canExecute(user, "", List.of(TARGET, "900")));
    }

    @Test
    void testCanExecuteNoIslandKnownPlayer() {
        when(im.getIslands(world, targetUUID)).thenReturn(List.of());
        assertFalse(c.canExecute(user, "", List.of(TARGET, "member")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    void testCanExecuteTargetOnlyOwnsIsland() {
        when(island2.getOwner()).thenReturn(targetUUID);
        when(im.hasIsland(world, targetUUID)).thenReturn(true);
        assertFalse(c.canExecute(user, "", List.of(TARGET, "member")));
        verify(user).sendMessage("commands.admin.setrank.cannot-set-owner");
    }

    @Test
    void testCanExecuteAlreadyRank() {
        assertFalse(c.canExecute(user, "", List.of(TARGET, "member")));
        verify(user).sendMessage("commands.admin.setrank.already-rank", TextVariables.NAME, TARGET,
                TextVariables.RANK, RanksManager.MEMBER_RANK_REF);
    }

    @Test
    void testCanExecuteSingleIslandSuccess() {
        assertTrue(c.canExecute(user, "", List.of(TARGET, "sub-owner")));
    }

    @Test
    void testCanExecuteMultipleIslandsRequiresIsland() {
        addSecondIsland(UUID.randomUUID());
        assertFalse(c.canExecute(user, "", List.of(TARGET, "sub-owner")));
        verify(user).sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
        verify(user).sendMessage("commands.admin.unregister.errors.specify-island-location", TextVariables.XYZ, XYZ);
        verify(user).sendMessage("commands.admin.unregister.errors.specify-island-location", TextVariables.XYZ, XYZ2);
    }

    @Test
    void testCanExecuteIslandByOwner() {
        addSecondIsland(UUID.randomUUID());
        assertTrue(c.canExecute(user, "", List.of(TARGET, "sub-owner", OWNER)));
        assertTrue(c.execute(user, "", List.of(TARGET, "sub-owner", OWNER)));
        verify(island2).setRank(targetUUID, RanksManager.SUB_OWNER_RANK);
        verify(island, never()).setRank(any(UUID.class), any(Integer.class));
    }

    @Test
    void testCanExecuteIslandByOwnerUnknownPlayer() {
        assertFalse(c.canExecute(user, "", List.of(TARGET, "sub-owner", "stranger")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "stranger");
    }

    @Test
    void testCanExecuteIslandByOwnerNotOwner() {
        when(im.getIslands(world, ownerUUID)).thenReturn(List.of());
        assertFalse(c.canExecute(user, "", List.of(TARGET, "sub-owner", OWNER)));
        verify(user).sendMessage("general.errors.player-is-not-owner", TextVariables.NAME, OWNER);
    }

    @Test
    void testCanExecuteIslandByOwnerWithConcurrentIslands() {
        addSecondIsland(ownerUUID);
        assertFalse(c.canExecute(user, "", List.of(TARGET, "sub-owner", OWNER)));
        verify(user).sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
        verify(user, times(2)).sendMessage(eq("commands.admin.unregister.errors.specify-island-location"),
                eq(TextVariables.XYZ), any());
    }

    @Test
    void testCanExecuteIslandByXyz() {
        addSecondIsland(ownerUUID);
        when(im.getIslandAt(any())).thenAnswer(inv -> {
            Location l = inv.getArgument(0);
            return l.getBlockX() == 100 ? Optional.of(island) : Optional.of(island2);
        });
        assertTrue(c.canExecute(user, "", List.of(TARGET, "member", XYZ2)));
        assertTrue(c.execute(user, "", List.of(TARGET, "member", XYZ2)));
        verify(island).setRank(targetUUID, RanksManager.MEMBER_RANK);
        verify(island2, never()).setRank(any(UUID.class), any(Integer.class));
    }

    @Test
    void testCanExecuteIslandByXyzUnknown() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertFalse(c.canExecute(user, "", List.of(TARGET, "member", "9,9,9")));
        verify(user).sendMessage("commands.admin.unregister.errors.unknown-island-location");
    }

    @Test
    void testCanExecuteIslandByXyzTargetIsOwner() {
        when(island2.getOwner()).thenReturn(targetUUID);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island2));
        assertFalse(c.canExecute(user, "", List.of(TARGET, "member", XYZ)));
        verify(user).sendMessage("commands.admin.setrank.cannot-set-owner");
    }

    /**
     * Granting a rank on an island the target has no standing on yet, e.g. making them trusted.
     */
    @Test
    void testExecuteGrantTrustedOnOwnersIsland() {
        when(im.getIslands(world, targetUUID)).thenReturn(List.of());
        when(island2.getRank(targetUUID)).thenReturn(RanksManager.VISITOR_RANK);
        assertTrue(c.canExecute(user, "", List.of(TARGET, "trusted", OWNER)));
        assertTrue(c.execute(user, "", List.of(TARGET, "trusted", OWNER)));
        verify(island2).setRank(targetUUID, RanksManager.TRUSTED_RANK);
    }

    @Test
    void testExecutePromote() {
        assertTrue(c.canExecute(user, "", List.of(TARGET, "sub-owner")));
        assertTrue(c.execute(user, "", List.of(TARGET, "sub-owner")));
        verify(island2).setRank(targetUUID, RanksManager.SUB_OWNER_RANK);
        verify(user).sendMessage("commands.admin.setrank.rank-set", "[from]", RanksManager.MEMBER_RANK_REF, "[to]",
                RanksManager.SUB_OWNER_RANK_REF, TextVariables.NAME, OWNER);
        // IslandEvent.build fires the generic IslandEvent plus the specific rank change event
        verify(pim, times(2)).callEvent(any());
    }

    @Test
    void testExecuteDemote() {
        when(island2.getRank(targetUUID)).thenReturn(RanksManager.SUB_OWNER_RANK);
        assertTrue(c.canExecute(user, "", List.of(TARGET, "member")));
        assertTrue(c.execute(user, "", List.of(TARGET, "member")));
        verify(island2).setRank(targetUUID, RanksManager.MEMBER_RANK);
        verify(user).sendMessage("commands.admin.setrank.rank-set", "[from]", RanksManager.SUB_OWNER_RANK_REF, "[to]",
                RanksManager.MEMBER_RANK_REF, TextVariables.NAME, OWNER);
        verify(pim, times(2)).callEvent(any());
    }

    @Test
    void testTabCompletePlayer() {
        Optional<List<String>> result = c.tabComplete(user, "", List.of(""));
        assertTrue(result.isPresent());
        assertEquals(List.of("tastybento"), result.get());
    }

    @Test
    void testTabCompleteRank() {
        Optional<List<String>> result = c.tabComplete(user, "", List.of(TARGET, ""));
        assertTrue(result.isPresent());
        assertEquals(List.of("coop", "trusted", "member", "sub-owner"), result.get());
    }

    @Test
    void testTabCompleteRankPartial() {
        Optional<List<String>> result = c.tabComplete(user, "", List.of(TARGET, "s"));
        assertTrue(result.isPresent());
        assertEquals(List.of("sub-owner"), result.get());
    }

    @Test
    void testTabCompleteIsland() {
        Optional<List<String>> result = c.tabComplete(user, "", List.of(TARGET, "member", ""));
        assertTrue(result.isPresent());
        assertTrue(result.get().containsAll(List.of("tastybento", XYZ)));
    }

    @Test
    void testTabCompleteTooManyArgs() {
        assertTrue(c.tabComplete(user, "", List.of(TARGET, "member", OWNER, "")).isEmpty());
    }
}
