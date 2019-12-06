package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
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
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminResetFlagsCommandTest {

    @Mock
    private CompositeCommand ac;
    private UUID uuid = UUID.randomUUID();
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private FlagsManager fm;
    @Mock
    private Flag flag;
    @Mock
    private Flag flag2;
    @Mock
    private Flag flag3;
    @Mock
    private Player player;

    private AdminResetFlagsCommand arf;
    private @Nullable User user;

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

        // Parent command has no aliases
        ac = mock(CompositeCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getPermissionPrefix()).thenReturn("bskyblock.");

        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        user = User.getInstance(player);

        // Flags manager
        when(plugin.getFlagsManager()).thenReturn(fm);
        when(flag.getType()).thenReturn(Type.PROTECTION);
        when(flag2.getType()).thenReturn(Type.SETTING);
        when(flag3.getType()).thenReturn(Type.WORLD_SETTING);
        when(flag.getID()).thenReturn("FLAG1");
        when(flag2.getID()).thenReturn("FLAG2");
        when(flag3.getID()).thenReturn("FLAG3");
        @NonNull
        List<Flag> list = new ArrayList<>();
        list.add(flag);
        list.add(flag2);
        list.add(flag3);
        when(fm.getFlags()).thenReturn(list);

        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getLocalesManager()).thenReturn(lm);

        // Confirmation
        Settings settings = mock(Settings.class);
        when(settings.getConfirmationTime()).thenReturn(10);
        when(plugin.getSettings()).thenReturn(settings);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);


        // Class
        arf = new AdminResetFlagsCommand(ac);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminResetFlagsCommand#AdminResetFlagsCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testAdminResetFlagsCommand() {
        assertEquals("resetflags", arf.getLabel());
        verify(flag).getID();
        verify(flag2).getID();
        verify(flag3, never()).getID();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminResetFlagsCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertFalse(arf.isOnlyPlayer());
        assertEquals("bskyblock.admin.resetflags", arf.getPermission());
        assertEquals("commands.admin.resetflags.parameters", arf.getParameters());
        assertEquals("commands.admin.resetflags.description", arf.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminResetFlagsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringTwoArgs() {
        List<String> args = Arrays.asList("sdfsd", "werwerw");
        assertFalse(arf.execute(user, "", args));
        verify(player).sendMessage(eq("commands.help.header"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminResetFlagsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringOneArgNotFlag() {
        assertFalse(arf.execute(user, "", Collections.singletonList("FLAG3")));
        verify(player).sendMessage(eq("commands.help.header"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminResetFlagsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringOneArgFlag2() {
        assertTrue(arf.execute(user, "", Collections.singletonList("FLAG2")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminResetFlagsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringOneArgFlag1() {
        assertTrue(arf.execute(user, "", Collections.singletonList("FLAG1")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminResetFlagsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        assertTrue(arf.execute(user, "", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminResetFlagsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        Optional<List<String>> list = arf.tabComplete(user, "", Collections.emptyList());
        assertTrue(list.isPresent());
        assertTrue(list.get().size() == 2);
        assertTrue(list.get().contains("FLAG1"));
        assertTrue(list.get().contains("FLAG2"));
        assertFalse(list.get().contains("FLAG3"));
    }

}
