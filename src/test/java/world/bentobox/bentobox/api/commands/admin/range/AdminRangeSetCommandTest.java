/**
 *
 */
package world.bentobox.bentobox.api.commands.admin.range;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
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
public class AdminRangeSetCommandTest {

    private CompositeCommand ac;
    private UUID uuid;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;


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

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        UUID notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        ac = mock(CompositeCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getWorld()).thenReturn(mock(World.class));

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(iwm.getIslandProtectionRange(Mockito.any())).thenReturn(200);
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(),Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(),Mockito.any())).thenReturn(uuid);
        Island island = mock(Island.class);
        when(island.getRange()).thenReturn(50);
        when(island.getProtectionRange()).thenReturn(50);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
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
        Answer<String> answer = invocation -> invocation.getArgumentAt(1, String.class);

        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer(answer );
        when(plugin.getLocalesManager()).thenReturn(lm);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteConsoleNoArgs() {
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        CommandSender sender = mock(CommandSender.class);
        User console = User.getInstance(sender);
        arc.execute(console, "", new ArrayList<>());
        // Show help
        Mockito.verify(sender).sendMessage("commands.help.header");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecutePlayerNoArgs() {
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        arc.execute(user, "", new ArrayList<>());
        // Show help
        Mockito.verify(user).sendMessage("commands.help.header","[label]","BSkyBlock");
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("100");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("general.errors.unknown-player");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteKnownPlayerNoIsland() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("100");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteTooHigh() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("100");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("commands.admin.range.set.invalid-value.too-high", TextVariables.NUMBER, "50");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteNotANumber() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("NAN");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("commands.admin.range.set.invalid-value.not-numeric", TextVariables.NUMBER, "NAN");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteDoubleNumber() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("3.141592654");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("commands.admin.range.set.invalid-value.not-numeric", TextVariables.NUMBER, "3.141592654");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteZero() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("0");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("commands.admin.range.set.invalid-value.too-low", TextVariables.NUMBER, "0");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteNegNumber() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("-437645");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("commands.admin.range.set.invalid-value.not-numeric", TextVariables.NUMBER, "-437645");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSame() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("50");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("commands.admin.range.set.invalid-value.same-as-before", TextVariables.NUMBER, "50");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeSetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecute() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        AdminRangeSetCommand arc = new AdminRangeSetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        args.add("48");
        arc.execute(user, "", args);
        Mockito.verify(user).sendMessage("commands.admin.range.set.success", TextVariables.NUMBER, "48");
    }

}
