package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class AdminTeamSetownerCommandTest extends AbstractCommonSetup {

    @Mock
    private CompositeCommand ac;
    private UUID uuid = UUID.randomUUID();
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private UUID notUUID = UUID.randomUUID();
    private AdminTeamSetownerCommand itl;

   @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        Settings settings = new Settings();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.getName()).thenReturn("tastybento");
        User.getInstance(mockPlayer);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);

        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getLocalesManager()).thenReturn(lm);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        @NonNull
        WorldSettings worldSettings = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSettings);

        // Location
        when(location.toVector()).thenReturn(new Vector(1, 2, 3));
        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getCenter()).thenReturn(location);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // DUT
        itl = new AdminTeamSetownerCommand(ac);

    }

   @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "commands.help.console");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#setup()}
     */
    @Test
    public void testSetup() {
        assertEquals("commands.admin.team.setowner.description", itl.getDescription());
        assertEquals("commands.admin.team.setowner.parameters", itl.getParameters());
        assertTrue(itl.isOnlyPlayer());
        assertEquals("mod.team.setowner", itl.getPermission());
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteMakeOwnerAlreadyOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(uuid);
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.team.setowner.already-owner", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSuccess() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(notUUID);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        assertTrue(itl.execute(user, itl.getLabel(), List.of("tastybento")));
        // Add other verifications
        verify(user).getTranslation("commands.admin.team.setowner.confirmation", TextVariables.NAME, "tastybento",
                TextVariables.XYZ, "1,2,3");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#changeOwner(User)}
     */
    @Test
    public void testChangeOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(notUUID);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        itl.changeOwner(user);
        // Add other verifications
        verify(user).sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#changeOwner(User)}
     */
    @Test
    public void testChangeOwnerNoOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(null);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        itl.changeOwner(user);
        // Add other verifications
        verify(user).sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, "tastybento");
    }
}
