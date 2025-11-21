package world.bentobox.bentobox.api.commands.admin;

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
import java.util.concurrent.CompletableFuture;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

public class AdminTeleportCommandTest extends CommonTestSetup {

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
    @Mock
    private PlaceholdersManager phm;

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
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.hasPermission("admin.tp")).thenReturn(true);
        when(mockPlayer.hasPermission("admin")).thenReturn(false);
        when(mockPlayer.getWorld()).thenReturn(endWorld);

        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.isPlayer()).thenReturn(true);
        when(user.hasPermission("admin.tp")).thenReturn(true);
        when(user.hasPermission("admin")).thenReturn(false);

        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bskyblock");
        when(ac.getLabel()).thenReturn("bskyblock");
        when(ac.getWorld()).thenReturn(world);
        when(ac.getPermission()).thenReturn("admin");

        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);

        // Island World Manager
        when(iwm.getNetherWorld(any())).thenReturn(netherWorld);
        when(iwm.getEndWorld(any())).thenReturn(endWorld);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);

        when(user.getTranslation(anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Island location
        Vector vector = mock(Vector.class);
        when(vector.toLocation(any())).thenReturn(location);
        when(location.toVector()).thenReturn(vector);
        when(location.getWorld()).thenReturn(world);
        when(spawnPoint.getWorld()).thenReturn(world);
        when(world.getMaxHeight()).thenReturn(255);
        when(im.getIslandLocation(any(), any())).thenReturn(location);
        // We do no actually want to teleport in this test, so return no island
        Optional<Island> nothing = Optional.empty();
        when(im.getIslandAt(any())).thenReturn(nothing);

        // Return an island for spawn checking
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        when(island.getCenter()).thenReturn(location);
        when(location.clone()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));
        when(island.getProtectionCenter()).thenReturn(location);
        // Util
        mockedUtil.when(() -> Util.getUUID(anyString())).thenCallRealMethod();
        CompletableFuture<Chunk> chunk = new CompletableFuture<>();
        mockedUtil.when(() -> Util.getChunkAtAsync(any())).thenReturn(chunk);

        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
    }

     @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test all the various commands
     */
    @Test
    public void testExecuteUserStringListOfString() {
        AdminTeleportCommand c = new AdminTeleportCommand(ac, "tp");
        assertEquals("tp", c.getLabel());
        c = new AdminTeleportCommand(ac, "tpnether");
        assertEquals("tpnether", c.getLabel());
        c = new AdminTeleportCommand(ac, "tpend");
        assertEquals("tpend", c.getLabel());
    }

    /**
     * Test no args
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgs() {
        AdminTeleportCommand atc = new AdminTeleportCommand(ac, "tp");
        assertFalse(atc.canExecute(user, "tp", new ArrayList<>()));
        verify(user).sendMessage(eq("commands.help.header"), eq(TextVariables.LABEL), eq("BSkyBlock"));
    }

    @Test
    public void testExecuteUserStringListOfStringUnknownTarget() {
        AdminTeleportCommand atc = new AdminTeleportCommand(ac, "tp");
        assertFalse(atc.canExecute(user, "tp", List.of("tastybento")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq(TextVariables.NAME), eq("tastybento"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetNoIsland() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertFalse(atc.canExecute(user, "tp", List.of("tastybento")));
        verify(user).sendMessage(eq("general.errors.player-has-no-island"));
    }

    /**
     * Test for {@link AdminTeleportCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIsland() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", List.of("tastybento")));
        assertTrue(atc.execute(user, "tp", List.of("tastybento")));
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    /**
     * Test for {@link AdminTeleportCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandSpawnPoint() {
        when(island.getSpawnPoint(any())).thenReturn(spawnPoint);
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", List.of("tastybento")));
        assertTrue(atc.execute(user, "tp", List.of("tastybento")));
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetIsTeamMember() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", List.of("tastybento")));
        assertTrue(atc.execute(user, "tp", List.of("tastybento")));
        verify(iwm, Mockito.never()).getNetherWorld(any());
        verify(iwm, Mockito.never()).getEndWorld(any());
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandNether() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpnether");
        assertTrue(atc.canExecute(user, "tpnether", List.of("tastybento")));
        assertTrue(atc.execute(user, "tpnether", List.of("tastybento")));
        verify(iwm).getNetherWorld(any());
        verify(iwm, Mockito.never()).getEndWorld(any());
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandEnd() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpend");
        assertTrue(atc.canExecute(user, "tpend", List.of("tastybento")));
        assertTrue(atc.execute(user, "tpend", List.of("tastybento")));
        verify(iwm, Mockito.never()).getNetherWorld(any());
        verify(iwm).getEndWorld(any());
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    @Test
    public void testPermissionsNoRootPermission() {
        when(mockPlayer.hasPermission("admin.tp")).thenReturn(true);
        when(mockPlayer.hasPermission("admin")).thenReturn(false);
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpend");
        assertTrue(atc.canExecute(user, "tpend", List.of("tastybento")));
        String[] list = new String[2];
        list[0] = "tpend";
        list[1] = "tastybento";
        // Should fail
        assertFalse(atc.execute(mockPlayer, "tpend", list));
    }

    @Test
    public void testPermissionsHasRootPermission() {
        when(mockPlayer.hasPermission("admin.tp")).thenReturn(true);
        when(mockPlayer.hasPermission("admin")).thenReturn(true);
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpend");
        assertTrue(atc.canExecute(user, "tpend", List.of("tastybento")));
        String[] list = new String[2];
        list[0] = "tpend";
        list[1] = "tastybento";
        // Should pass
        assertTrue(atc.execute(mockPlayer, "tpend", list));
        verify(mockPlayer).hasPermission("admin.tp");
        verify(mockPlayer).hasPermission("admin");
    }

}
