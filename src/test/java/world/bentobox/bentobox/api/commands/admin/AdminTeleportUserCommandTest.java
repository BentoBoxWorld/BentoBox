package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand.IslandInfo;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for {@link AdminTeleportUserCommand}.
 */
class AdminTeleportUserCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Location spawnPoint;
    @Mock
    private World netherWorld;
    @Mock
    private World endWorld;
    @Mock
    private PlaceholdersManager phm;

    /**
     * UUID of the player being teleported ("teleportee").
     * Set to {@code uuid} so that {@code User.getInstance(teleporteeUUID)}
     * resolves to the pre-registered {@code mockPlayer}.
     */
    private UUID teleporteeUUID;
    /** UUID of the destination player ("target"). */
    private UUID targetUUID;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Admin user running the command
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("admin");
        when(user.isPlayer()).thenReturn(true);
        when(user.hasPermission(anyString())).thenReturn(true);

        // Teleportee: re-use mockPlayer (already registered in User cache with uuid)
        teleporteeUUID = uuid;
        when(mockPlayer.isOnline()).thenReturn(true);

        // Target: a different UUID
        targetUUID = UUID.randomUUID();

        User.setPlugin(plugin);

        // Parent command
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bskyblock");
        when(ac.getLabel()).thenReturn("bskyblock");
        when(ac.getWorld()).thenReturn(world);
        when(ac.getPermission()).thenReturn("admin");

        // World environments
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);

        // IWM
        when(iwm.getNetherWorld(any())).thenReturn(netherWorld);
        when(iwm.getEndWorld(any())).thenReturn(endWorld);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Target has an island by default
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        when(plugin.getPlayers()).thenReturn(pm);

        // Name → UUID resolution via Util.getUUID (which delegates to pm.getUUID)
        mockedUtil.when(() -> Util.getUUID(anyString())).thenCallRealMethod();
        when(pm.getUUID("teleportee")).thenReturn(teleporteeUUID);
        when(pm.getUUID("target")).thenReturn(targetUUID);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) inv -> inv.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) inv -> inv.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) inv -> inv.getArgument(0, String.class));

        // Island location
        Vector vector = mock(Vector.class);
        when(vector.toLocation(any())).thenReturn(location);
        when(location.toVector()).thenReturn(vector);
        when(location.getWorld()).thenReturn(world);
        when(location.clone()).thenReturn(location);
        when(island.getCenter()).thenReturn(location);
        when(island.getProtectionCenter()).thenReturn(location);
        when(spawnPoint.getWorld()).thenReturn(world);

        // Prevent actual async chunk loading during teleport
        CompletableFuture<Chunk> chunk = new CompletableFuture<>();
        mockedUtil.when(() -> Util.getChunkAtAsync(any())).thenReturn(chunk);

        // Placeholders
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any()))
                .thenAnswer((Answer<String>) inv -> inv.getArgument(1, String.class));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // -------------------------------------------------------------------------
    // setup()
    // -------------------------------------------------------------------------

    @Test
    void testSetupMetadata() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertEquals("tpuser", cmd.getLabel());
        assertEquals("admin.tpuser", cmd.getPermission());
        assertEquals("commands.admin.tpuser.parameters", cmd.getParameters());
        assertEquals("commands.admin.tpuser.description", cmd.getDescription());
    }

    @Test
    void testSetupAllLabels() {
        assertEquals("tpuser",      new AdminTeleportUserCommand(ac, "tpuser").getLabel());
        assertEquals("tpusernether", new AdminTeleportUserCommand(ac, "tpusernether").getLabel());
        assertEquals("tpuserend",    new AdminTeleportUserCommand(ac, "tpuserend").getLabel());
    }

    // -------------------------------------------------------------------------
    // canExecute() – argument count guards
    // -------------------------------------------------------------------------

    @Test
    void testCanExecuteNoArgs() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertFalse(cmd.canExecute(user, "tpuser", List.of()));
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "BSkyBlock");
    }

    @Test
    void testCanExecuteOneArgShowsHelp() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertFalse(cmd.canExecute(user, "tpuser", List.of("teleportee")));
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "BSkyBlock");
    }

    // -------------------------------------------------------------------------
    // canExecute() – player resolution
    // -------------------------------------------------------------------------

    @Test
    void testCanExecuteUnknownTeleportee() {
        // pm.getUUID returns null for unknown names (default Mockito behaviour)
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertFalse(cmd.canExecute(user, "tpuser", List.of("unknown", "target")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "unknown");
    }

    @Test
    void testCanExecuteTeleporteeOffline() {
        when(mockPlayer.isOnline()).thenReturn(false);
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertFalse(cmd.canExecute(user, "tpuser", List.of("teleportee", "target")));
        verify(user).sendMessage("general.errors.offline-player");
    }

    /**
     * The source uses {@code args.getFirst()} (the teleportee's name) in the error
     * message even when it is the second arg (the target) that is unresolvable.
     * This test documents that behaviour.
     */
    @Test
    void testCanExecuteUnknownTarget() {
        // "bad-target" is not registered → pm.getUUID returns null
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertFalse(cmd.canExecute(user, "tpuser", List.of("teleportee", "bad-target")));
        // Source sends args.getFirst() ("teleportee"), not the actual unknown name
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "teleportee");
    }

    // -------------------------------------------------------------------------
    // canExecute() – island checks
    // -------------------------------------------------------------------------

    @Test
    void testCanExecuteTargetHasNoIslandOrTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertFalse(cmd.canExecute(user, "tpuser", List.of("teleportee", "target")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    void testCanExecuteTargetInTeamWithNoOwnIsland() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target")));
    }

    @Test
    void testCanExecuteGetSpotReturnsNullIsland() {
        // getIsland returns null → getSpot returns null → no-safe-location
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertFalse(cmd.canExecute(user, "tpuser", List.of("teleportee", "target")));
        verify(user).sendMessage("general.errors.no-safe-location-found");
    }

    @Test
    void testCanExecuteGetSpotUsesSpawnPointWhenAvailable() {
        when(island.getSpawnPoint(Environment.NORMAL)).thenReturn(spawnPoint);
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target")));
    }

    // -------------------------------------------------------------------------
    // canExecute() – dimension routing
    // -------------------------------------------------------------------------

    @Test
    void testCanExecuteTwoArgsOverworld() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target")));
        verify(iwm, never()).getNetherWorld(any());
        verify(iwm, never()).getEndWorld(any());
    }

    @Test
    void testCanExecuteTwoArgsNether() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpusernether");
        assertTrue(cmd.canExecute(user, "tpusernether", List.of("teleportee", "target")));
        verify(iwm).getNetherWorld(world);
        verify(iwm, never()).getEndWorld(world);
    }

    @Test
    void testCanExecuteTwoArgsEnd() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuserend");
        assertTrue(cmd.canExecute(user, "tpuserend", List.of("teleportee", "target")));
        verify(iwm, never()).getNetherWorld(world);
        verify(iwm).getEndWorld(world);
    }

    @Test
    void testCanExecuteNetherWorldNull() {
        when(iwm.getNetherWorld(any())).thenReturn(null);
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpusernether");
        assertFalse(cmd.canExecute(user, "tpusernether", List.of("teleportee", "target")));
        verify(user).sendMessage("general.errors.no-safe-location-found");
    }

    @Test
    void testCanExecuteEndWorldNull() {
        when(iwm.getEndWorld(any())).thenReturn(null);
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuserend");
        assertFalse(cmd.canExecute(user, "tpuserend", List.of("teleportee", "target")));
        verify(user).sendMessage("general.errors.no-safe-location-found");
    }

    // -------------------------------------------------------------------------
    // canExecute() – island/home name (3+ args)
    // -------------------------------------------------------------------------

    @Test
    void testCanExecuteUnknownIslandName() {
        try (MockedStatic<IslandGoCommand> mockedGo = Mockito.mockStatic(IslandGoCommand.class)) {
            mockedGo.when(() -> IslandGoCommand.getNameIslandMap(any(User.class), any(World.class)))
                    .thenReturn(Map.of("existing-home", new IslandInfo(island, false)));
            AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
            assertFalse(cmd.canExecute(user, "tpuser", List.of("teleportee", "target", "bad-home")));
            verify(user).sendMessage("commands.island.go.unknown-home");
            verify(user).sendMessage("commands.island.sethome.homes-are");
            verify(user).sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, "existing-home");
        }
    }

    @Test
    void testCanExecuteKnownIslandNameSingleEntry() {
        // names.size() == 1 → warpSpot is not updated, just returns true
        try (MockedStatic<IslandGoCommand> mockedGo = Mockito.mockStatic(IslandGoCommand.class)) {
            mockedGo.when(() -> IslandGoCommand.getNameIslandMap(any(User.class), any(World.class)))
                    .thenReturn(Map.of("myhome", new IslandInfo(island, false)));
            AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
            assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target", "myhome")));
        }
    }

    @Test
    void testCanExecuteKnownIslandNameMultipleEntriesNoSpawnPoint() {
        Island island2 = mock(Island.class);
        when(island2.getProtectionCenter()).thenReturn(location);
        try (MockedStatic<IslandGoCommand> mockedGo = Mockito.mockStatic(IslandGoCommand.class)) {
            Map<String, IslandInfo> names = new HashMap<>();
            names.put("myhome", new IslandInfo(island, false));
            names.put("other",  new IslandInfo(island2, true));
            mockedGo.when(() -> IslandGoCommand.getNameIslandMap(any(User.class), any(World.class)))
                    .thenReturn(names);
            AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
            // warpSpot updated to island.getProtectionCenter().toVector().toLocation(world)
            assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target", "myhome")));
        }
    }

    @Test
    void testCanExecuteKnownIslandNameMultipleEntriesWithSpawnPoint() {
        Island island2 = mock(Island.class);
        when(island.getSpawnPoint(Environment.NORMAL)).thenReturn(spawnPoint);
        try (MockedStatic<IslandGoCommand> mockedGo = Mockito.mockStatic(IslandGoCommand.class)) {
            Map<String, IslandInfo> names = new HashMap<>();
            names.put("myhome", new IslandInfo(island, true));
            names.put("other",  new IslandInfo(island2, true));
            mockedGo.when(() -> IslandGoCommand.getNameIslandMap(any(User.class), any(World.class)))
                    .thenReturn(names);
            AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
            // warpSpot updated to island.getSpawnPoint(world.getEnvironment())
            assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target", "myhome")));
        }
    }

    @Test
    void testCanExecuteMultiWordIslandName() {
        try (MockedStatic<IslandGoCommand> mockedGo = Mockito.mockStatic(IslandGoCommand.class)) {
            mockedGo.when(() -> IslandGoCommand.getNameIslandMap(any(User.class), any(World.class)))
                    .thenReturn(Map.of("my cool island", new IslandInfo(island, true)));
            AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
            // args.subList(2, size) = ["my", "cool", "island"] → joined = "my cool island"
            assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target", "my", "cool", "island")));
        }
    }

    // -------------------------------------------------------------------------
    // execute()
    // -------------------------------------------------------------------------

    @Test
    void testExecuteTwoArgsUsesNoSafeLocationFailureMessage() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target")));
        assertTrue(cmd.execute(user, "tpuser", List.of("teleportee", "target")));
        // 2-arg path: failureMessage = NOT_SAFE translation
        verify(user).getTranslation("general.errors.no-safe-location-found");
    }

    @Test
    void testExecuteThreeArgsUsesManualLocationFailureMessage() {
        try (MockedStatic<IslandGoCommand> mockedGo = Mockito.mockStatic(IslandGoCommand.class)) {
            mockedGo.when(() -> IslandGoCommand.getNameIslandMap(any(User.class), any(World.class)))
                    .thenReturn(Map.of("myhome", new IslandInfo(island, false)));
            AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
            assertTrue(cmd.canExecute(user, "tpuser", List.of("teleportee", "target", "myhome")));
            assertTrue(cmd.execute(user, "tpuser", List.of("teleportee", "target", "myhome")));
            // 3-arg path: failureMessage = "commands.admin.tp.manual" with coordinates
            verify(user).getTranslation(eq("commands.admin.tp.manual"), eq("[location]"), anyString());
        }
    }

    // -------------------------------------------------------------------------
    // tabComplete()
    // -------------------------------------------------------------------------

    @Test
    void testTabCompleteEmptyArgs() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertTrue(cmd.tabComplete(user, "tpuser", List.of()).isEmpty());
    }

    @Test
    void testTabCompleteOneArg() {
        // size == 1 → no matching branch → empty
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertTrue(cmd.tabComplete(user, "tpuser", List.of("t")).isEmpty());
    }

    @Test
    void testTabCompleteTwoArgs() {
        mockedUtil.when(() -> Util.getOnlinePlayerList(any())).thenReturn(List.of("alice", "bob"));
        mockedUtil.when(() -> Util.tabLimit(any(), anyString())).thenCallRealMethod();
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        Optional<List<String>> result = cmd.tabComplete(user, "tpuser", List.of("teleportee", ""));
        assertTrue(result.isPresent());
        assertTrue(result.get().containsAll(List.of("alice", "bob")));
    }

    @Test
    void testTabCompleteThreeArgs() {
        mockedUtil.when(() -> Util.getOnlinePlayerList(any())).thenReturn(List.of("alice"));
        mockedUtil.when(() -> Util.tabLimit(any(), anyString())).thenCallRealMethod();
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        Optional<List<String>> result = cmd.tabComplete(user, "tpuser", List.of("teleportee", "target", ""));
        assertTrue(result.isPresent());
    }

    @Test
    void testTabCompleteFourArgsUnresolvableIndex2() {
        // args.get(2) = "badname" → Util.getUUID → null → empty
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        Optional<List<String>> result = cmd.tabComplete(user, "tpuser", List.of("teleportee", "target", "badname", ""));
        assertTrue(result.isEmpty());
    }

    @Test
    void testTabCompleteFourArgsKnownUUIDAtIndex2() {
        // args.get(2) can be a UUID string (edge case in the source that uses index 2 for target)
        try (MockedStatic<IslandGoCommand> mockedGo = Mockito.mockStatic(IslandGoCommand.class)) {
            mockedGo.when(() -> IslandGoCommand.getNameIslandMap(any(User.class), any(World.class)))
                    .thenReturn(Map.of("home1", new IslandInfo(island, false),
                                      "home2", new IslandInfo(island, false)));
            mockedUtil.when(() -> Util.tabLimit(any(), anyString())).thenCallRealMethod();
            // Use the targetUUID string at index 2 so Util.getUUID resolves it
            AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
            Optional<List<String>> result = cmd.tabComplete(user, "tpuser",
                    List.of("teleportee", "target", targetUUID.toString(), ""));
            assertTrue(result.isPresent());
            assertTrue(result.get().containsAll(List.of("home1", "home2")));
        }
    }

    @Test
    void testTabCompleteFiveOrMoreArgs() {
        AdminTeleportUserCommand cmd = new AdminTeleportUserCommand(ac, "tpuser");
        assertTrue(cmd.tabComplete(user, "tpuser", List.of("a", "b", "c", "d", "e")).isEmpty());
    }
}
