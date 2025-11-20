package world.bentobox.bentobox.api.commands.admin.blueprints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
public class AdminBlueprintCopyCommandTest extends AbstractCommonSetup {

    @Mock
    private AdminBlueprintCommand ac;
    @Mock
    private GameModeAddon addon;
    @Mock
    private User user;
    @Mock
    private BlueprintClipboard clip;
    private UUID uuid = UUID.randomUUID();
    @Mock
    private BlueprintsManager bm;
    private AdminBlueprintCopyCommand abcc;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Required for NamespacedKey
        when(plugin.getName()).thenReturn("BentoBox");

        clip = mock(BlueprintClipboard.class);
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

        // Clipboard
        when(clip.copy(any(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(true);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        abcc = new AdminBlueprintCopyCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCopyCommand#AdminBlueprintCopyCommand(world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCommand)}.
     */
    @Test
    public void testAdminBlueprintCopyCommand() {
        assertNotNull(abcc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCopyCommand#setup()}.
     */
    @Test
    public void testSetup() {
        abcc.setup();
        assertEquals("commands.admin.blueprint.copy.description", abcc.getDescription());
        assertEquals("commands.admin.blueprint.copy.parameters", abcc.getParameters());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCopyCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringHelp() {
        assertFalse(abcc.execute(user, "", List.of("1", "2", "3")));
        verify(user).sendMessage("commands.help.header", "[label]", "translation");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCopyCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringSuccess() {
        assertTrue(abcc.execute(user, "", List.of("air", "biome")));
        verify(clip).copy(user, true, true, false);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCopyCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringSuccessCaps() {
        assertTrue(abcc.execute(user, "", List.of("AIR", "BIOME")));
        verify(clip).copy(user, true, true, false);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCopyCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringJunk() {
        assertTrue(abcc.execute(user, "", List.of("junk", "junk")));
        verify(clip).copy(user, false, false, false);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCopyCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNothing() {
        assertTrue(abcc.execute(user, "", Collections.emptyList()));
        verify(clip).copy(user, false, false, false);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCopyCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        Optional<List<String>> o = abcc.tabComplete(user, "", List.of(""));
        assertTrue(o.isPresent());
        assertEquals("air", o.get().getFirst());
        assertEquals("biome", o.get().get(1));
    }

}
