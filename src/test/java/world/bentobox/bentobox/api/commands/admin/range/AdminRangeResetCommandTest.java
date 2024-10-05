package world.bentobox.bentobox.api.commands.admin.range;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class })
public class AdminRangeResetCommandTest extends AbstractCommonSetup {

    @Mock
    private CompositeCommand ac;
    private UUID uuid;
    @Mock
    private User user;
    private PlayersManager pm;
    @Mock
    private PluginManager pim;

    /**
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        UUID notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getWorld()).thenReturn(mock(World.class));

        // Island World Manager
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(iwm.getIslandProtectionRange(Mockito.any())).thenReturn(200);
        when(plugin.getIWM()).thenReturn(iwm);

        // Player has island to begin with
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        Island island = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        Answer<String> answer = invocation -> invocation.getArgument(1, String.class);

        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer(answer);
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeResetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteConsoleNoArgs() {
        AdminRangeResetCommand arc = new AdminRangeResetCommand(ac);
        CommandSender sender = mock(CommandSender.class);
        when(sender.spigot()).thenReturn(spigot);
        User console = User.getInstance(sender);
        arc.execute(console, "", new ArrayList<>());
        // Show help
        checkSpigotMessage("commands.help.header");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeResetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecutePlayerNoArgs() {
        AdminRangeResetCommand arc = new AdminRangeResetCommand(ac);
        arc.execute(user, "", new ArrayList<>());
        // Show help
        verify(user).sendMessage("commands.help.header", "[label]", "BSkyBlock");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeResetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminRangeResetCommand arc = new AdminRangeResetCommand(ac);
        String[] name = { "tastybento" };
        arc.execute(user, "", Arrays.asList(name));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeResetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteKnownPlayerNotOwnerNoTeam() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        AdminRangeResetCommand arc = new AdminRangeResetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        arc.execute(user, "", args);
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    public void testExecuteKnownPlayerNotOwnerButInTeam() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        AdminRangeResetCommand arc = new AdminRangeResetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        arc.execute(user, "", args);
        verify(user, never()).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeResetCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteKnownPlayer() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        AdminRangeResetCommand arc = new AdminRangeResetCommand(ac);
        List<String> args = new ArrayList<>();
        args.add("tastybento");
        arc.execute(user, "", args);
        verify(user).sendMessage("commands.admin.range.reset.success", TextVariables.NUMBER, "200");
    }

}
