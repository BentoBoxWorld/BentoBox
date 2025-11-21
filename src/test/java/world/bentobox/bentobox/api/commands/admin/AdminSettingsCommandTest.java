package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class AdminSettingsCommandTest extends RanksManagerTestSetup {

    private AdminSettingsCommand asc;
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Location spawnPoint;
    @Mock
    private World netherWorld;
    @Mock
    private World endWorld;
 
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.isPlayer()).thenReturn(true);
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bskyblock");
        when(ac.getWorld()).thenReturn(world);

        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Players manager
        when(plugin.getPlayers()).thenReturn(pm);
        // Island - player has island
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);

        // Util
        mockedUtil.when(() -> Util.getUUID(anyString())).thenReturn(uuid);
        mockedUtil.when(() -> Util.tabLimit(any(), any())).thenCallRealMethod();
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // Settings
        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // Bukkit
        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        ItemMeta bannerMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        mockedBukkit.when(() -> Bukkit.getItemFactory()).thenReturn(itemFactory);
        Inventory inventory = mock(Inventory.class);
        mockedBukkit.when(() -> Bukkit.createInventory(any(), Mockito.anyInt(), anyString())).thenReturn(inventory);
        // Flags manager
        mockedBukkit.when(() -> Bukkit.getPluginManager()).thenReturn(pim);
        FlagsManager fm = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(fm);

        asc = new AdminSettingsCommand(ac);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertFalse(asc.isOnlyPlayer());
        assertEquals("admin.settings", asc.getPermission());
        assertEquals("commands.admin.settings.parameters", asc.getParameters());
        assertEquals("commands.admin.settings.description", asc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteEmpty() {
        assertTrue(asc.canExecute(user, "", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgUnknownPlayer() {
        mockedUtil.when(() -> Util.getUUID(anyString())).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgKnownPlayerNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgKnownPlayerIslandNotOwner() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgKnownPlayer() {
        assertTrue(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgSpawnNoSpawn() {
        mockedUtil.when(() -> Util.getUUID(anyString())).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("spawn-island")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "spawn-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgSpawnSpawn() {
        when(im.getSpawn(any())).thenReturn(Optional.of(island));
        assertTrue(asc.canExecute(user, "", Collections.singletonList("spawn-island")));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsConsole() {
        when(user.isPlayer()).thenReturn(false);
        assertFalse(asc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("general.errors.use-in-game");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringArgs() {
        assertTrue(asc.execute(user, "", Arrays.asList("blah", "blah")));
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringTwoArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b", "WORLD_TNT"));
        assertFalse(r.isEmpty());
        assertEquals("WORLD_TNT_DAMAGE", r.get().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringThreeArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b", "WORLD_TNT", "BEACO"));
        assertFalse(r.isEmpty());
        assertEquals("BEACON", r.get().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringFourArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b", "b", "PVP_OVERWORLD", "t"));
        assertFalse(r.isEmpty());
        // TODO - finish this.
    }

}
