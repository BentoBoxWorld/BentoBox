/**
 *
 */
package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
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
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminSetspawnCommandTest {

    private CompositeCommand ac;
    private UUID uuid;
    private User user;
    private IslandsManager im;

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
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        ac = mock(CompositeCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getPermissionPrefix()).thenReturn("bskyblock.");

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(),Mockito.any())).thenReturn(true);
        when(im.getOwner(Mockito.any(),Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);


        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Return the reference (USE THIS IN THE FUTURE)
        when(user.getTranslation(Mockito.anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgumentAt(0, String.class));

        // Plugin Manager
        Server server = mock(Server.class);
        PluginManager pim = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pim);
        when(Bukkit.getServer()).thenReturn(server);

        // Confirmable command settings
        Settings settings = mock(Settings.class);
        when(settings.getConfirmationTime()).thenReturn(10);
        when(plugin.getSettings()).thenReturn(settings);
    }
    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#AdminSetspawnCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testAdminSetspawnCommand() {
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertEquals("setspawn", c.getLabel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#setup()}.
     */
    @Test
    public void testSetup() {
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertEquals("bskyblock.admin.setspawn", c.getPermission());
        assertTrue(c.isOnlyPlayer());
        assertEquals("commands.admin.setspawn.description", c.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        Island island = mock(Island.class);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertTrue(c.execute(user, "setspawn", Collections.emptyList()));
        Mockito.verify(user).getTranslation("commands.admin.setspawn.confirmation");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIsland() {
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(Optional.empty());
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertFalse(c.execute(user, "setspawn", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.setspawn.no-island-here");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringAlreadySpawn() {
        Island island = mock(Island.class);
        when(island.isSpawn()).thenReturn(true);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oi);
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertTrue(c.execute(user, "setspawn", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.setspawn.already-spawn");
    }
}
