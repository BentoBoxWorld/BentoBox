package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableSet;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.RanksManagerBeforeClassTest;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.TeamInvite.Type;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class , ServerBuildInfo.class})
public class IslandTeamCommandTest extends RanksManagerBeforeClassTest {

    @Mock
    private CompositeCommand ic;

    private IslandTeamCommand tc;

    private UUID invitee;

    @Mock
    private User user;

    @Mock
    private GameModeAddon addon;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Parent command
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        when(ic.getWorld()).thenReturn(world);
        when(ic.getAddon()).thenReturn(addon);

        // user
        invitee = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPermissionValue(eq("bskyblock.team.maxsize"), anyInt())).thenReturn(3);

        // island Manager
        // is owner of island
        when(im.getPrimaryIsland(world, uuid)).thenReturn(island);
        when(im.getIsland(world, user)).thenReturn(island);
        // Max members
        when(im.getMaxMembers(eq(island), eq(RanksManager.MEMBER_RANK))).thenReturn(3);
        // No team members
        // when(im.getMembers(any(),
        // any(UUID.class))).thenReturn(Collections.emptySet());
        // Add members
        ImmutableSet<UUID> set = new ImmutableSet.Builder<UUID>().build();
        // No members
        when(island.getMemberSet(anyInt(), any(Boolean.class))).thenReturn(set);
        when(island.getMemberSet(anyInt())).thenReturn(set);
        when(island.getMemberSet()).thenReturn(set);
        when(island.getOwner()).thenReturn(uuid);
        // island
        when(im.getIsland(any(), eq(uuid))).thenReturn(island);

        // IWM
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        // Command under test
        tc = new IslandTeamCommand(ic);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#IslandTeamCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandTeamCommand() {
        assertEquals("team", tc.getLabel());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("bskyblock.island.team", tc.getPermission());
        assertTrue(tc.isOnlyPlayer());
        assertEquals("commands.island.team.description", tc.getDescription());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringNoIsland() {
        when(im.getPrimaryIsland(world, uuid)).thenReturn(null);
        assertFalse(tc.canExecute(user, "team", Collections.emptyList()));
        verify(user).sendMessage(eq("general.errors.no-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringIslandIsFull() {
        // Max members
        when(im.getMaxMembers(eq(island), eq(RanksManager.MEMBER_RANK))).thenReturn(0);
        assertTrue(tc.canExecute(user, "team", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.island.team.invite.errors.island-is-full"));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#addInvite(world.bentobox.bentobox.api.commands.island.team.Invite.Type, java.util.UUID, java.util.UUID)}.
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    @Test
    public void testAddInvite() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        tc.addInvite(Type.TEAM, uuid, invitee, island);
        verify(h, atLeast(1)).saveObject(any());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#isInvited(java.util.UUID)}.
     */
    @Test
    public void testIsInvited() {
        assertFalse(tc.isInvited(invitee));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#getInviter(java.util.UUID)}.
     */
    @Test
    public void testGetInviter() {
        assertNull(tc.getInviter(invitee));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#getInviter(java.util.UUID)}.
     */
    @Test
    public void testGetInviterNoInvite() {
        assertNull(tc.getInviter(invitee));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#getInvite(java.util.UUID)}.
     */
    @Test
    public void testGetInvite() {
        assertNull(tc.getInvite(invitee));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand#removeInvite(java.util.UUID)}.
     */
    @Test
    public void testRemoveInvite() {
        assertNull(tc.getInvite(invitee));
        tc.addInvite(Type.TEAM, uuid, invitee, island);
        tc.removeInvite(invitee);
        assertNull(tc.getInvite(invitee));
    }
}
