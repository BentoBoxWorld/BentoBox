package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

/**
 * @author tastybento
 *
 */
public class IslandSpawnCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ic;
    private IslandSpawnCommand isc;
    private @Nullable User user;
    @Mock
    private @Nullable WorldSettings ws;
    private Map<String, Boolean> map;
    @Mock
    private BukkitTask task;
    @Mock
    private Settings s;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(mockPlayer.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);
        when(mockPlayer.getWorld()).thenReturn(world);
        User.setPlugin(plugin);
        // Set up user already
        user = User.getInstance(mockPlayer);

        // Addon
        GameModeAddon addon = mock(GameModeAddon.class);


        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getParameters()).thenReturn("parameters");
        when(ic.getDescription()).thenReturn("description");
        when(ic.getPermissionPrefix()).thenReturn("permission.");
        when(ic.getUsage()).thenReturn("");
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(Optional.empty());
        when(ic.getAddon()).thenReturn(addon);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(sch);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);

       // Settings
        when(plugin.getSettings()).thenReturn(s);

        // IWM
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        map = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(map);

        // Island Manager
        when(plugin.getIslands()).thenReturn(im);

        LocalesManager lm = mock(LocalesManager.class);
        // Locales
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        // Command
        isc = new IslandSpawnCommand(ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSpawnCommand#IslandSpawnCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandSpawnCommand() {
        assertEquals("spawn", isc.getLabel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSpawnCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("permission.island.spawn", isc.getPermission());
        assertTrue(isc.isOnlyPlayer());
        assertEquals("commands.island.spawn.description", isc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSpawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        assertTrue(isc.execute(user, "spawn", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSpawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringInWorldNoTeleportFalling() {
        when(mockPlayer.getFallDistance()).thenReturn(10F);
        map.put("PREVENT_TELEPORT_WHEN_FALLING", true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        assertFalse(isc.execute(user, "spawn", Collections.emptyList()));
        checkSpigotMessage("protection.flags.PREVENT_TELEPORT_WHEN_FALLING.hint");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSpawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringInWorldTeleportOkFalling() {
        when(mockPlayer.getFallDistance()).thenReturn(10F);
        map.put("PREVENT_TELEPORT_WHEN_FALLING", false);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        assertTrue(isc.execute(user, "spawn", Collections.emptyList()));
        checkSpigotMessage("protection.flags.PREVENT_TELEPORT_WHEN_FALLING.hint", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSpawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringWrongWorldTeleportOkFalling() {
        when(mockPlayer.getFallDistance()).thenReturn(10F);
        map.put("PREVENT_TELEPORT_WHEN_FALLING", true);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        assertTrue(isc.execute(user, "spawn", Collections.emptyList()));
        checkSpigotMessage("protection.flags.PREVENT_TELEPORT_WHEN_FALLING.hint", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSpawnCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringInWorldTeleportNotFalling() {
        when(mockPlayer.getFallDistance()).thenReturn(0F);
        map.put("PREVENT_TELEPORT_WHEN_FALLING", true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        assertTrue(isc.execute(user, "spawn", Collections.emptyList()));
        checkSpigotMessage("protection.flags.PREVENT_TELEPORT_WHEN_FALLING.hint", 0);
    }

}
