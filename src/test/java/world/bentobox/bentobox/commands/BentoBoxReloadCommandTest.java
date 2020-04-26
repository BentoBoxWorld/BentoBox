package world.bentobox.bentobox.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
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
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, PanelListenerManager.class })
public class BentoBoxReloadCommandTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private AddonsManager am;
    @Mock
    private LocalesManager lm;
    private BentoBoxReloadCommand reload;

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
        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        // Addons manager
        when(plugin.getAddonsManager()).thenReturn(am);
        // Addons
        Addon addon1 = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "BSkyBlock", "1.0.0").build();
        when(addon1.getDescription()).thenReturn(desc);
        Addon addon2 = mock(Addon.class);
        AddonDescription desc2 = new AddonDescription.Builder("main", "AcidIsland", "1.0.0").build();
        when(addon2.getDescription()).thenReturn(desc2);
        // Linking
        Optional<Addon> optionalAddon1 = Optional.of(addon1);
        Optional<Addon> optionalAddon2 = Optional.of(addon2);
        when(am.getAddonByName(Mockito.eq("bskyblock"))).thenReturn(optionalAddon1);
        when(am.getAddonByName(Mockito.eq("acidisland"))).thenReturn(optionalAddon2);
        when(am.getAddonByName(Mockito.eq("warps"))).thenReturn(Optional.empty());
        when(am.getAddons()).thenReturn(Arrays.asList(addon1, addon2));

        // Confirmable command settings
        Settings settings = mock(Settings.class);
        when(settings.getConfirmationTime()).thenReturn(10);
        when(plugin.getSettings()).thenReturn(settings);

        // Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // User
        when(user.getTranslation(Mockito.anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Panels
        PowerMockito.mockStatic(PanelListenerManager.class);

        // Command
        reload = new BentoBoxReloadCommand(ac);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxReloadCommand#BentoBoxReloadCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testBentoBoxReloadCommand() {
        assertNotNull(reload);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxReloadCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("bentobox.admin.reload", reload.getPermission());
        assertEquals("commands.bentobox.reload.description", reload.getDescription());
        assertEquals("commands.bentobox.reload.parameters", reload.getParameters());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxReloadCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringReloadAll() {
        reload.execute(user, "", Collections.emptyList());
        Mockito.verify(user).sendMessage("commands.confirmation.confirm",
                "[seconds]",
                "10");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.commands.BentoBoxReloadCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringHelp() {
        reload.execute(user, "", Collections.singletonList("sdfsdfs"));
        Mockito.verify(user).sendMessage(
                "commands.help.header",
                "[label]",
                "commands.help.console"
                );
    }
}
