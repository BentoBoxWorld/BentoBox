package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.bukkit.World;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for {@link AdminUndeleteCommand}.
 *
 * @author tastybento
 */
class AdminUndeleteCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;

    private AdminUndeleteCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);
        when(ac.getWorld()).thenReturn(world);

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(location);
        when(user.isPlayer()).thenReturn(true);
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.getInstance(mockPlayer);
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Island - by default it is pending deletion
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(location);
        when(island.isDeletable()).thenReturn(true);
        when(location.toVector()).thenReturn(new Vector(1, 2, 3));
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // DUT
        cmd = new AdminUndeleteCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testCanExecuteTooManyArgs() {
        assertFalse(cmd.canExecute(user, cmd.getLabel(), List.of("extra")));
        // Shows help
    }

    @Test
    void testCanExecuteWrongWorld() {
        when(user.getWorld()).thenReturn(mock(World.class));
        assertFalse(cmd.canExecute(user, cmd.getLabel(), new ArrayList<>()));
        verify(user).sendMessage("general.errors.wrong-world");
    }

    @Test
    void testCanExecuteNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertFalse(cmd.canExecute(user, cmd.getLabel(), new ArrayList<>()));
        verify(user).sendMessage("commands.admin.undelete.not-in-deletion");
    }

    @Test
    void testCanExecuteIslandNotDeletable() {
        when(island.isDeletable()).thenReturn(false);
        assertFalse(cmd.canExecute(user, cmd.getLabel(), new ArrayList<>()));
        verify(user).sendMessage("commands.admin.undelete.not-in-deletion");
    }

    @Test
    void testCanExecuteSuccess() {
        assertTrue(cmd.canExecute(user, cmd.getLabel(), new ArrayList<>()));
        verify(user, never()).sendMessage(anyString());
    }

    @Test
    void testExecute() {
        // Set up the island reference via canExecute
        assertTrue(cmd.canExecute(user, cmd.getLabel(), new ArrayList<>()));
        assertTrue(cmd.execute(user, cmd.getLabel(), new ArrayList<>()));
        // Island is unowned and no longer pending deletion
        verify(island).setOwner(null);
        verify(im).undeleteIsland(island);
        verify(user).sendMessage("commands.admin.undelete.undeleted-island", TextVariables.XYZ, "1,2,3");
        verify(user).sendMessage("general.success");
    }
}
