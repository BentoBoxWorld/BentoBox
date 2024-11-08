package world.bentobox.bentobox.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
import org.eclipse.jdt.annotation.NonNull;
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
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class })
public class BentoBoxPermsCommandTest extends AbstractCommonSetup {

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
    private Permission perm;

    private PermissionDefault defaultPerm = PermissionDefault.OP;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

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
        when(perm.getDefault()).thenReturn(defaultPerm);
        when(pim.getPermission(anyString())).thenReturn(perm);

        // Placeholders
        when(phm.replacePlaceholders(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        // BentoBox
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        cmd = new BentoBoxPermsCommand(ac);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
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
        CommandSender console = mock(CommandSender.class);
        when(console.spigot()).thenReturn(spigot);
        assertTrue(cmd.execute(console, "perms", args));
        checkSpigotMessage("general.errors.use-in-console", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxPermsCommand#execute(Player, java.lang.String, String[])}.
     */
    @Test
    public void testExecuteUserStringListOfStringIsPlayer() {
        when(user.isPlayer()).thenReturn(true);
        String[] args = new String[1];
        args[0] = "";
        assertFalse(cmd.execute(mockPlayer, "perms", args));
        checkSpigotMessage("general.errors.use-in-console");
    }
}
