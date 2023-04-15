package world.bentobox.bentobox.api.commands.admin.blueprints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.util.Vector;
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
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminBlueprintSaveCommandTest {

    private AdminBlueprintSaveCommand absc;
    @Mock
    private AdminBlueprintCommand ac;
    @Mock
    private GameModeAddon addon;
    @Mock
    private User user;
    private BlueprintClipboard clip = new BlueprintClipboard();
    private UUID uuid = UUID.randomUUID();
    private File blueprintsFolder;
    @Mock
    private BlueprintsManager bm;
    private Blueprint bp = new Blueprint();

    /**
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

        Map<UUID, BlueprintClipboard> map = new HashMap<>();
        map.put(uuid , clip);
        when(ac.getClipboards()).thenReturn(map);
        blueprintsFolder = new File("blueprints");
        when(ac.getBlueprintsFolder()).thenReturn(blueprintsFolder);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);


        absc = new AdminBlueprintSaveCommand(ac);
    }

    /**
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        if (blueprintsFolder.exists()) {
            Files.walk(blueprintsFolder.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintSaveCommand#AdminBlueprintSaveCommand(world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCommand)}.
     */
    @Test
    public void testAdminBlueprintSaveCommand() {
        assertNotNull(absc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintSaveCommand#setup()}.
     */
    @Test
    public void testSetup() {
        absc.setup();
        assertEquals("commands.admin.blueprint.save.description", absc.getDescription());
        assertEquals("commands.admin.blueprint.save.parameters", absc.getParameters());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintSaveCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteShowHelp() {
        assertFalse(absc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.help.header", "[label]", "translation");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintSaveCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoClipboard() {
        when(ac.getClipboards()).thenReturn(new HashMap<>());
        assertFalse(absc.canExecute(user, "", List.of("")));
        verify(user).sendMessage("commands.admin.blueprint.copy-first");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintSaveCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoBedrock() {
        Blueprint bp = new Blueprint();
        clip.setBlueprint(bp);
        assertFalse(absc.canExecute(user, "", List.of("")));
        verify(user).sendMessage("commands.admin.blueprint.bedrock-required");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintSaveCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {

        bp.setBedrock(new Vector(1,2,3));
        clip.setBlueprint(bp);
        assertTrue(absc.canExecute(user, "", List.of("")));
        verify(user, never()).sendMessage(anyString());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintSaveCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        testCanExecute();
        assertTrue(absc.execute(user, "", List.of("island")));
        verify(ac).hideClipboard(user);
        verify(bm).addBlueprint(addon, bp);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintSaveCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringFileExists() {
        testCanExecute();
        assertTrue(absc.execute(user, "", List.of("island")));
        assertFalse(absc.execute(user, "", List.of("island")));
        verify(user).getTranslation("commands.admin.blueprint.file-exists");
    }

}
