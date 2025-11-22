package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class AdminSwitchCommandTest extends CommonTestSetup {

    private AdminSwitchCommand asc;
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private Player p;
    private UUID notUUID;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.isPlayer()).thenReturn(true);
        User.setPlugin(plugin);

        // Parent command has no aliases
        ac = mock(CompositeCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bskyblock");

        asc = new AdminSwitchCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSwitchCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("mod.switch", asc.getPermission());
        assertTrue(asc.isOnlyPlayer());
        assertEquals("commands.admin.switch.parameters", asc.getParameters());
        assertEquals("commands.admin.switch.description", asc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSwitchCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {
        assertFalse(asc.canExecute(user, "", Collections.singletonList("hello")));
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, null);
        assertTrue(asc.canExecute(user, "", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSwitchCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoMetaData() {
        when(user.getMetaData(eq("AdminCommandSwitch"))).thenReturn(Optional.empty());
        asc.execute(user, "", Collections.emptyList());
        verify(user).getMetaData("AdminCommandSwitch");
        verify(user).sendMessage("commands.admin.switch.removing");
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSwitchCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringMetaFalse() {
        MetaDataValue md = new MetaDataValue(false);
        when(user.getMetaData(eq("AdminCommandSwitch"))).thenReturn(Optional.of(md));
        asc.execute(user, "", Collections.emptyList());
        verify(user).getMetaData("AdminCommandSwitch");
        verify(user).sendMessage("commands.admin.switch.removing");
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSwitchCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringMetaTrue() {
        MetaDataValue md = new MetaDataValue(true);
        when(user.getMetaData(eq("AdminCommandSwitch"))).thenReturn(Optional.of(md));
        asc.execute(user, "", Collections.emptyList());
        verify(user).getMetaData("AdminCommandSwitch");
        verify(user).sendMessage("commands.admin.switch.adding");
        verify(user).sendMessage("general.success");
    }

}
