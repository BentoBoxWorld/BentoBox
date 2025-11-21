package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
public class AdminSetspawnCommandTest extends CommonTestSetup {

    private CompositeCommand ac;
    private UUID uuid;
    private User user;
 
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getLocation()).thenReturn(mock(Location.class));
        User.setPlugin(plugin);

        // Parent command has no aliases
        ac = mock(CompositeCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getPermissionPrefix()).thenReturn("bskyblock.");

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Return the reference (USE THIS IN THE FUTURE)
        when(user.getTranslation(Mockito.anyString()))
        .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Confirmable command settings
        Settings settings = mock(Settings.class);
        when(settings.getConfirmationTime()).thenReturn(10);
        when(plugin.getSettings()).thenReturn(settings);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#AdminSetspawnCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testAdminSetspawnCommand() {
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertEquals("setspawn", c.getLabel());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#setup()}.
     */
    @Test
    public void testSetup() {
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertEquals("bskyblock.admin.setspawn", c.getPermission());
        assertTrue(c.isOnlyPlayer());
        assertEquals("commands.admin.setspawn.description", c.getDescription());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        Island island = mock(Island.class);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(any(Location.class))).thenReturn(oi);
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertTrue(c.execute(user, "setspawn", Collections.emptyList()));
        Mockito.verify(user).getTranslation("commands.admin.setspawn.confirmation");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIsland() {
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.empty());
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertFalse(c.execute(user, "setspawn", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.setspawn.no-island-here");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.AdminSetspawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringAlreadySpawn() {
        Island island = mock(Island.class);
        when(island.isSpawn()).thenReturn(true);
        Optional<Island> oi = Optional.of(island);
        when(im.getIslandAt(any(Location.class))).thenReturn(oi);
        AdminSetspawnCommand c = new AdminSetspawnCommand(ac);
        assertTrue(c.execute(user, "setspawn", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.setspawn.already-spawn");
    }
}
