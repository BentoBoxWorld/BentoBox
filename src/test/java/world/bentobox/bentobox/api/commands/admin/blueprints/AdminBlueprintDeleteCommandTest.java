package world.bentobox.bentobox.api.commands.admin.blueprints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminBlueprintDeleteCommandTest {

    @Mock
    private AdminBlueprintCommand ac;
    @Mock
    private GameModeAddon addon;
    @Mock
    private User user;
    private UUID uuid = UUID.randomUUID();
    @Mock
    private BlueprintsManager bm;
    private Blueprint bp = new Blueprint();
    private AdminBlueprintDeleteCommand abcc;
    private Map<String, Blueprint> map;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Blueprints Manager
        when(plugin.getBlueprintsManager()).thenReturn(bm);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Sometimes use Mockito.withSettings().verboseLogging()
        User.setPlugin(plugin);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getTranslation(anyString())).thenReturn("translation");

        // Parent command
        when(ac.getAddon()).thenReturn(addon);
        when(ac.getLabel()).thenReturn("blueprint");
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("admin");

        map = new HashMap<>();
        map.put("key", bp);
        when(bm.getBlueprints(any())).thenReturn(map);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);


        abcc = new AdminBlueprintDeleteCommand(ac);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintDeleteCommand#AdminBlueprintDeleteCommand(world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCommand)}.
     */
    @Test
    public void testAdminBlueprintDeleteCommand() {
        assertNotNull(abcc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintDeleteCommand#setup()}.
     */
    @Test
    public void testSetup() {
        abcc.setup();
        assertEquals("commands.admin.blueprint.delete.description", abcc.getDescription());
        assertEquals("commands.admin.blueprint.delete.parameters", abcc.getParameters());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintDeleteCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringHelp() {
        assertFalse(abcc.execute(user, "", List.of("1", "2", "3")));
        verify(user).sendMessage("commands.help.header", "[label]", "translation");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintDeleteCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoBp() {
        assertFalse(abcc.execute(user, "", List.of(" iSlAnd  ")));
        verify(user).sendMessage("commands.admin.blueprint.delete.no-blueprint", TextVariables.NAME, "_island__");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintDeleteCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringSuccessCaps() {
        assertTrue(abcc.execute(user, "", List.of("KEY")));
        verify(user).getTranslation("commands.admin.blueprint.delete.confirmation");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintDeleteCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        Optional<List<String>> o = abcc.tabComplete(user, "", List.of(""));
        assertTrue(o.isPresent());
        assertEquals("key", o.get().get(0));
    }

}