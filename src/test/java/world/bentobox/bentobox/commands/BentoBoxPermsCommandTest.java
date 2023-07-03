package world.bentobox.bentobox.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class BentoBoxPermsCommandTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private LocalesManager lm;

    BentoBoxPermsCommand cmd;
    @Mock
    private PlaceholdersManager phm;
    @Mock
    private PluginManager pim;
    @Mock
    private Permission perm;

    private PermissionDefault defaultPerm = PermissionDefault.OP;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        @NonNull
        Map<String, CompositeCommand> cmdMap = new HashMap<>();
        cmdMap.put("test", ac);
        when(cm.getCommands()).thenReturn(cmdMap);


        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getSubCommands()).thenReturn(new HashMap<>());
        when(ac.getLabel()).thenReturn("bbox");
        when(ac.getTopLabel()).thenReturn("bbox");
        when(ac.getPermission()).thenReturn("admin.bbox");
        when(ac.getDescription()).thenReturn("description");


        // User
        when(user.getTranslation(Mockito.anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.isPlayer()).thenReturn(false);
        User.setPlugin(plugin);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(perm.getDefault()).thenReturn(defaultPerm);
        when(pim.getPermission(anyString())).thenReturn(perm);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Placeholders
        when(phm.replacePlaceholders(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        // BentoBox
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);

        // Commands for perms


        cmd = new BentoBoxPermsCommand(ac);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxPermsCommand#BentoBoxPermsCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testBentoBoxPermsCommand() {
        assertNotNull(cmd);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxPermsCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(cmd.isOnlyConsole());
        assertFalse(cmd.isOnlyPlayer());
        assertEquals("bentobox.admin.perms", cmd.getPermission());
        assertEquals("commands.bentobox.perms.description", cmd.getDescription());
        assertEquals("commands.bentobox.perms.parameters", cmd.getParameters());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxPermsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        assertTrue(cmd.execute(user, "perms", List.of()));
        verify(user).sendMessage("*** BentoBox effective perms:");
        verify(user).sendRawMessage("permissions:");
        verify(user).sendRawMessage("  admin.bbox:");
        verify(user).sendRawMessage("    description: Allow use of '/bbox' command - null");
        verify(user).sendRawMessage("  bentobox.admin.perms:");
        verify(user).sendRawMessage("    description: Allow use of '/bbox perms' command - null");
        verify(user, times(2)).sendRawMessage("    default: OP");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxPermsCommand#execute(Player, java.lang.String, String[])}.
     */
    @Test
    public void testExecuteUserStringListOfStringConsole() {
        String[] args = new String[1];
        args[0] = "";
        CommandSender p = mock(CommandSender.class);
        assertTrue(cmd.execute(p, "perms", args));
        verify(p, never()).sendMessage("general.errors.use-in-console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxPermsCommand#execute(Player, java.lang.String, String[])}.
     */
    @Test
    public void testExecuteUserStringListOfStringIsPlayer() {
        when(user.isPlayer()).thenReturn(true);
        String[] args = new String[1];
        args[0] = "";
        Player p = mock(Player.class);
        assertFalse(cmd.execute(p, "perms", args));
        verify(p).sendMessage("general.errors.use-in-console");
    }
}
