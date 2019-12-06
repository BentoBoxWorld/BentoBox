/**
 *
 */
package world.bentobox.bentobox.api.commands.admin.blueprints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminBlueprintsListCommandTest {

    @Mock
    private AdminBlueprintCommand ac;
    @Mock
    private Addon addon;
    @Mock
    private User user;
    private AdminBlueprintListCommand list;
    private File dataFolder;

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

        // Settings
        Settings s = mock(Settings.class);
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Sometimes use Mockito.withSettings().verboseLogging()
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getAddon()).thenReturn(addon);
        when(ac.getLabel()).thenReturn("blueprint");
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("admin");

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Addon
        dataFolder = new File("dataFolder");
        dataFolder.mkdirs();

        when(addon.getDataFolder()).thenReturn(dataFolder);
        // Class
        list = new AdminBlueprintListCommand(ac);


    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Files.walk(dataFolder.toPath())
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintListCommand#AdminBlueprintListCommand(world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCommand)}.
     */
    @Test
    public void testAdminBlueprintListCommand() {

        assertEquals("list", list.getLabel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintListCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("commands.admin.blueprint.list.description", list.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintListCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {
        assertTrue(list.canExecute(user, "", Collections.emptyList()));
        assertFalse(list.canExecute(user, "", Collections.singletonList("extraneous")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintListCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoBlueprintsFolder() {
        assertFalse(list.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.blueprint.list.no-blueprints");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintListCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoBlueprintsFilesEmptyFolder() {
        File blueprintFolder = new File(dataFolder, "blueprints");
        blueprintFolder.mkdirs();
        assertFalse(list.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.blueprint.list.no-blueprints");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintListCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringNoBlueprintsFiles() throws IOException {
        File blueprintFolder = new File(dataFolder, "blueprints");
        blueprintFolder.mkdirs();
        new File(blueprintFolder, "random.txt").createNewFile();
        assertFalse(list.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.blueprint.list.no-blueprints");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintListCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringWithBlueprintsFiles() throws IOException {
        File blueprintFolder = new File(dataFolder, BlueprintsManager.FOLDER_NAME);
        blueprintFolder.mkdirs();
        new File(blueprintFolder, "island" + BlueprintsManager.BLUEPRINT_SUFFIX).createNewFile();
        new File(blueprintFolder, "nether-island" + BlueprintsManager.BLUEPRINT_SUFFIX).createNewFile();
        new File(blueprintFolder, "end-island" + BlueprintsManager.BLUEPRINT_SUFFIX).createNewFile();
        new File(blueprintFolder, "random.txt").createNewFile();

        assertTrue(list.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.blueprint.list.available-blueprints");
        Mockito.verify(user).sendRawMessage("island");
        Mockito.verify(user).sendRawMessage("nether-island");
        Mockito.verify(user).sendRawMessage("end-island");
        Mockito.verify(user, Mockito.times(3)).sendRawMessage(Mockito.anyString());
    }

}
