package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
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
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class})
public class AdminSwitchCommandTest {

    private AdminSwitchCommand asc;
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private Player p;
    private UUID notUUID;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
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

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
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
