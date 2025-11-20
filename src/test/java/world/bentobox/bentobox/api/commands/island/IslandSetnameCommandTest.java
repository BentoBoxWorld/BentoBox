/**
 *
 */
package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class IslandSetnameCommandTest extends AbstractCommonSetup {

    @Mock
    private CompositeCommand ic;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Addon addon;

    private IslandSetnameCommand isc;
    private Settings settings;
    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        User.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // User
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getDisplayName()).thenReturn("&Ctastybento");
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(-1);
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getWorld()).thenReturn(world);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Player has island to begin with
        when(im.getIsland(world, user)).thenReturn(island);
        when(island.getName()).thenReturn("previous-name");

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // Test
        isc = new IslandSetnameCommand(ic);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#IslandSetnameCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandSetnameCommand() {
        assertEquals("setname", isc.getLabel());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(isc.isOnlyPlayer());
        assertEquals("commands.island.setname.parameters", isc.getParameters());
        assertEquals("commands.island.setname.description", isc.getDescription());
        assertEquals("island.name", isc.getPermission());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testIslandSetnameCommandNoArgs() {
        assertFalse(isc.canExecute(user, isc.getLabel(), new ArrayList<>()));
        verify(user).sendMessage("commands.help.header", "[label]", "BSkyBlock");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testIslandSetnameCommandNoIsland() {
        when(im.getIsland(world, user)).thenReturn(null);
        assertFalse(isc.canExecute(user, isc.getLabel(), List.of("name")));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTooLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(isc.canExecute(user, isc.getLabel(), List.of("name")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "ranks.member");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testIslandSetnameCommandNameTooShort() {
        assertFalse(isc.canExecute(user, isc.getLabel(), List.of("x")));
        verify(user).sendMessage("commands.island.setname.name-too-short", TextVariables.NUMBER, "4");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testIslandSetnameCommandNameOnlyColors() {
        assertFalse(isc.canExecute(user, isc.getLabel(), List.of("§b§c§d§e")));
        verify(user).sendMessage("commands.island.setname.name-too-short", TextVariables.NUMBER, "4");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testIslandSetnameCommandNameTooLong() {
        assertFalse(isc.canExecute(user, isc.getLabel(), List.of("This is a very long name that is not allowed and will have to be prevented")));
        verify(user).sendMessage("commands.island.setname.name-too-long", TextVariables.NUMBER, "20");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testIslandSetnameCommandAllOK() {
        assertTrue(isc.canExecute(user, isc.getLabel(), List.of("name-okay")));
        verify(user, never()).sendMessage(anyString());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSetnameCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        when(user.hasPermission(anyString())).thenReturn(true);
        assertTrue(isc.execute(user, isc.getLabel(), List.of("name-okay")));
        verify(island).setName("name-okay");
        verify(user).sendMessage("commands.island.setname.success", TextVariables.NAME, "name-okay");
        verify(pim, times(2)).callEvent(any());
    }

}
