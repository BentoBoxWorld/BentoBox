/**
 *
 */
package world.bentobox.bentobox.api.commands.admin.schem;

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
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminSchemListCommandTest {

    @Mock
    private AdminSchemCommand ac;
    @Mock
    private Addon addon;
    @Mock
    private User user;
    private AdminSchemListCommand list;
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
        when(ac.getLabel()).thenReturn("schem");
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
        list = new AdminSchemListCommand(ac);


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
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.schem.AdminSchemListCommand#AdminSchemListCommand(world.bentobox.bentobox.api.commands.admin.schem.AdminSchemCommand)}.
     */
    @Test
    public void testAdminSchemListCommand() {

        assertEquals("list", list.getLabel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.schem.AdminSchemListCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("commands.admin.schem.list.description", list.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.schem.AdminSchemListCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {
        assertTrue(list.canExecute(user, "", Collections.emptyList()));
        assertFalse(list.canExecute(user, "", Collections.singletonList("extraneous")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.schem.AdminSchemListCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoSchemsFolder() {
        assertFalse(list.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.schem.list.no-schems");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.schem.AdminSchemListCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoSchemsFilesEmptyFolder() {
        File schemFolder = new File(dataFolder, "schems");
        schemFolder.mkdirs();
        assertFalse(list.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.schem.list.no-schems");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.schem.AdminSchemListCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringNoSchemsFiles() throws IOException {
        File schemFolder = new File(dataFolder, "schems");
        schemFolder.mkdirs();
        new File(schemFolder, "random.txt").createNewFile();
        assertFalse(list.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.schem.list.no-schems");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.schem.AdminSchemListCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringWithSchemsFiles() throws IOException {
        File schemFolder = new File(dataFolder, "schems");
        schemFolder.mkdirs();
        new File(schemFolder, "island.schem").createNewFile();
        new File(schemFolder, "nether-island.schem").createNewFile();
        new File(schemFolder, "end-island.schem").createNewFile();
        new File(schemFolder, "random.txt").createNewFile();

        assertTrue(list.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.admin.schem.list.available-schems");
        Mockito.verify(user).sendRawMessage("island");
        Mockito.verify(user).sendRawMessage("nether-island");
        Mockito.verify(user).sendRawMessage("end-island");
        Mockito.verify(user, Mockito.times(3)).sendRawMessage(Mockito.anyString());
    }

}
