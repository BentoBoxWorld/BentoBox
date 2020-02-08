package world.bentobox.bentobox.api.commands;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
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
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.command.CommandEvent;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, CommandEvent.class})
public class DelayedTeleportCommandTest {

    private static final String HELLO = "hello";
    @Mock
    private BentoBox plugin;
    @Mock
    private BukkitScheduler sch;
    @Mock
    private PluginManager pim;

    private TestClass dtc;
    @Mock
    private Addon addon;
    @Mock
    private User user;
    @Mock
    private Runnable command;
    @Mock
    private Settings settings;
    @Mock
    private BukkitTask task;
    @Mock
    private Player player;
    @Mock
    private Location from;
    @Mock
    private Location to;
    @Mock
    private Notifier notifier;

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
        // Notify
        when(plugin.getNotifier()).thenReturn(notifier);
        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getDelayTime()).thenReturn(10); // 10 seconds
        // Server & Scheduler
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        when(sch.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(task);
        // Plugin manager
        when(Bukkit.getPluginManager()).thenReturn(pim);
        // user
        User.setPlugin(plugin);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getLocation()).thenReturn(from);
        when(player.getUniqueId()).thenReturn(uuid);
        // Locations
        when(to.toVector()).thenReturn(new Vector(1,2,3));
        when(from.toVector()).thenReturn(new Vector(1,2,4)); // Player moved
        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getLocalesManager()).thenReturn(lm);


        String[] alias = {};
        // Class under test
        dtc = new TestClass(addon, "go", alias);

    }

    class TestClass extends DelayedTeleportCommand {

        public TestClass(Addon addon, CompositeCommand parent, String label, String[] aliases) {
            super(addon, parent, label, aliases);
        }


        public TestClass(Addon addon, String label, String[] aliases) {
            super(addon, label, aliases);
        }

        public TestClass(CompositeCommand parent, String label, String[] aliases) {
            super(parent, label, aliases);
        }

        @Override
        public void setup() {
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return false;
        }

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnPlayerMoveNoCooldown() {
        PlayerMoveEvent e = new PlayerMoveEvent(player, from, to);
        dtc.onPlayerMove(e);
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnPlayerMoveCommandCancelled() {
        testDelayCommandUserStringRunnableStandStill();
        PlayerMoveEvent e = new PlayerMoveEvent(player, from, to);
        dtc.onPlayerMove(e);
        verify(notifier).notify(any(), eq("commands.delay.moved-so-command-cancelled"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnPlayerMoveHeadMove() {
        testDelayCommandUserStringRunnableStandStill();
        PlayerMoveEvent e = new PlayerMoveEvent(player, from, from);
        dtc.onPlayerMove(e);
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#DelayedTeleportCommand(world.bentobox.bentobox.api.addons.Addon, java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testDelayedTeleportCommandAddonStringStringArray() {
        verify(pim).registerEvents(eq(dtc), eq(plugin));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#delayCommand(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.Runnable)}.
     */
    @Test
    public void testDelayCommandUserStringRunnableZeroDelay() {
        when(settings.getDelayTime()).thenReturn(0);
        dtc.delayCommand(user, HELLO, command);
        verify(sch).runTask(eq(plugin), eq(command));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#delayCommand(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.Runnable)}.
     */
    @Test
    public void testDelayCommandUserStringRunnableOp() {
        when(user.isOp()).thenReturn(true);
        dtc.delayCommand(user, HELLO, command);
        verify(sch).runTask(eq(plugin), eq(command));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#delayCommand(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.Runnable)}.
     */
    @Test
    public void testDelayCommandUserStringRunnablePermBypassCooldowns() {
        when(user.hasPermission(eq("nullmod.bypasscooldowns"))).thenReturn(true);
        dtc.delayCommand(user, HELLO, command);
        verify(sch).runTask(eq(plugin), eq(command));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#delayCommand(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.Runnable)}.
     */
    @Test
    public void testDelayCommandUserStringRunnablePermBypassDelay() {
        when(user.hasPermission(eq("nullmod.bypassdelays"))).thenReturn(true);
        dtc.delayCommand(user, HELLO, command);
        verify(sch).runTask(eq(plugin), eq(command));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#delayCommand(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.Runnable)}.
     */
    @Test
    public void testDelayCommandUserStringRunnableStandStill() {
        dtc.delayCommand(user, HELLO, command);
        verify(sch, never()).runTask(eq(plugin), eq(command));
        verify(user).sendRawMessage(eq(HELLO));
        verify(user).sendMessage(eq("commands.delay.stand-still"), eq("[seconds]"), eq("10"));
        verify(sch).runTaskLater(eq(plugin), any(Runnable.class), eq(200L));
        verify(user, never()).sendMessage("commands.delay.previous-command-cancelled");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#delayCommand(world.bentobox.bentobox.api.user.User, java.lang.String, java.lang.Runnable)}.
     */
    @Test
    public void testDelayCommandUserStringRunnableStandStillDuplicate() {
        dtc.delayCommand(user, HELLO, command);
        dtc.delayCommand(user, HELLO, command);
        verify(user).sendMessage("commands.delay.previous-command-cancelled");
        verify(task).cancel();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.DelayedTeleportCommand#delayCommand(world.bentobox.bentobox.api.user.User, java.lang.Runnable)}.
     */
    @Test
    public void testDelayCommandUserRunnable() {
        dtc.delayCommand(user, command);
        verify(sch, never()).runTask(eq(plugin), eq(command));
        verify(user, never()).sendRawMessage(anyString());
        verify(user).sendMessage(eq("commands.delay.stand-still"), eq("[seconds]"), eq("10"));
        verify(sch).runTaskLater(eq(plugin), any(Runnable.class), eq(200L));
        verify(user, never()).sendMessage("commands.delay.previous-command-cancelled");

    }

}
