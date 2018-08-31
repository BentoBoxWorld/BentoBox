/**
 *
 */
package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandTeamKickCommandTest {

    private CompositeCommand ic;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;
    private IslandWorldManager iwm;
    private Player player;
    private CompositeCommand subCommand;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        player = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        subCommand = mock(CompositeCommand.class);
        Optional<CompositeCommand> optionalCommand = Optional.of(subCommand);
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(optionalCommand);

        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // IWM friendly name
        iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteNoTeam() {
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.no-team"));
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteNotTeamLeader() {
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(notUUID);
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.not-leader"));
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteNoTarget() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteUnknownPlayer() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.unknown-player"));
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteSamePlayer() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(uuid);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.kick.cannot-kick"));
    }


    /**
     * Test method for .
     */
    @Test
    public void testExecuteDifferentPlayerNotInTeam() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(new HashSet<>());
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.not-in-team"));
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteNoConfirmation() {
        when(s.isKickConfirmation()).thenReturn(false);

        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);

        Set<UUID> members = new HashSet<>();
        members.add(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(im).removePlayer(Mockito.any(), Mockito.eq(notUUID));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteWithConfirmation() {
        when(s.isKickConfirmation()).thenReturn(true);

        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);

        Set<UUID> members = new HashSet<>();
        members.add(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        // Confirmation required
        Mockito.verify(user).sendMessage(Mockito.eq("commands.confirmation.confirm"), Mockito.eq("[seconds]"), Mockito.eq("0"));
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteTestResets() {
        when(s.isKickConfirmation()).thenReturn(false);

        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);

        Set<UUID> members = new HashSet<>();
        members.add(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);

        // Require resets
        when(iwm.isOnLeaveResetEnderChest(Mockito.any())).thenReturn(true);
        Inventory enderChest = mock(Inventory.class);
        when(player.getEnderChest()).thenReturn(enderChest);
        when(iwm.isOnLeaveResetInventory(Mockito.any())).thenReturn(true);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(iwm.isOnLeaveResetMoney(Mockito.any())).thenReturn(true);

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(im).removePlayer(Mockito.any(), Mockito.eq(notUUID));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));

        Mockito.verify(enderChest).clear();
        Mockito.verify(inv).clear();
    }

    /**
     * Test method for .
     */
    @Test
    public void testCooldown() {
        // 10 minutes = 600 seconds
        when(s.getInviteCooldown()).thenReturn(10);
        testExecuteNoConfirmation();
        Mockito.verify(subCommand).setCooldown(uuid, notUUID, 600);
    }
}
