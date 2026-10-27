package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandReservedEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.TaskScheduler;

/**
 * @author tastybento
 *
 */
class AdminRegisterCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;

    private UUID notUUID;

    private IslandDeletionManager idm;
    private AdminRegisterCommand itl;
    @Mock
    private Block block;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        Settings settings = new Settings();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        // World
        when(ac.getWorld()).thenReturn(world);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(location);
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.getInstance(mockPlayer);
        User.setPlugin(plugin);

        // Util
        mockedUtil.when(() -> Util.getUUID("tastybento")).thenReturn(uuid);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Deletion Manager
        idm = mock(IslandDeletionManager.class);
        when(idm.inDeletion(any())).thenReturn(false);
        when(plugin.getIslandDeletionManager()).thenReturn(idm);

        // Island
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(location);
        when(im.createIsland(any(), eq(uuid))).thenReturn(island);
        when(location.getBlock()).thenReturn(block);

        // DUT
        itl = new AdminRegisterCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    void testCanExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    void testCanExecuteWrongWorld() {
        when(user.getWorld()).thenReturn(mock(World.class));
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("general.errors.wrong-world");
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    void testCanExecuteUnknownPlayer() {
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento2")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento2");
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    void testCanExecuteNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).getTranslation("commands.admin.register.no-island-here");
    }


    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    void testCanExecuteAlreadyOwnedIsland() {
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(pm.getUUID(any())).thenReturn(notUUID);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(1, 2, 3));
        // Island has owner
        when(island.getOwner()).thenReturn(uuid);
        when(island.isOwned()).thenReturn(true);
        when(island.getCenter()).thenReturn(loc);
        Optional<Island> opi = Optional.of(island);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.register.already-owned");
    }

    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     * An island pending deletion can be registered after confirmation.
     */
    @Test
    void testCanExecuteInDeletionIsland() {
        when(idm.inDeletion(any())).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(pm.getUUID(any())).thenReturn(notUUID);
        Location loc = mock(Location.class);

        // Island is orphaned (owner cleared by the soft-delete)
        when(island.getOwner()).thenReturn(null);
        Optional<Island> opi = Optional.of(island);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);

        // canExecute defers to a confirmation rather than registering immediately
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).getTranslation("commands.admin.register.in-deletion");
    }

    /**
     * Test method for {@link AdminRegisterCommand#register(User, String)}.
     * Registering an island clears its deletion status so the region purge skips it.
     */
    @Test
    void testRegisterClearsDeletionStatus() {
        testCanExecuteSuccess();
        itl.register(user, uuid, island, "tastybento");
        verify(im).undeleteIsland(island);
    }

    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    void testCanExecuteSuccess() {
        when(location.toVector()).thenReturn(new Vector(123,123,432));
        when(island.getCenter()).thenReturn(location);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        Optional<Island> opi = Optional.of(island);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(location);
        when(pm.getUUID(any())).thenReturn(notUUID);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
    }

    /**
     * Test method for {@link AdminRegisterCommand#register(User, String)}.
     */
    @Test
    void testRegister() {
        testCanExecuteSuccess();
        when(island.isSpawn()).thenReturn(true);
        itl.register(user, uuid, island, "tastybento");
        verify(im).setOwner(user, uuid, island, RanksManager.VISITOR_RANK);
        verify(im).clearSpawn(world);
        verify(user).sendMessage("commands.admin.register.registered-island", TextVariables.XYZ, "123,123,432", TextVariables.NAME,
                "tastybento");
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link AdminRegisterCommand#reserve(User, String)}.
     */
    @Test
    void testReserveCannotMakeIsland() {
        when(im.createIsland(any(), eq(uuid))).thenReturn(null);
        testCanExecuteNoIsland();
        itl.reserve(user, uuid, location, "tastybento");
        verify(im).createIsland(any(), eq(uuid));
        verify(user).sendMessage("commands.admin.register.cannot-make-island");
    }

    /**
     * Test method for {@link AdminRegisterCommand#reserve(User, String)}.
     */
    @Test
    void testReserveCanMakeIsland() {
        testCanExecuteNoIsland();
        itl.reserve(user, uuid, location, "tastybento");
        verify(im).createIsland(any(), eq(uuid));
        verify(user, never()).sendMessage("commands.admin.register.cannot-make-island");
        verify(block).setType(Material.BEDROCK);
        verify(user).sendMessage("commands.admin.register.reserved-island", TextVariables.XYZ, "0,0,0", TextVariables.NAME,
                "tastybento");
    }

    @Test
    void interleavedRegistrationsKeepPlayerAndIslandTogether() {
        Island other = mock(Island.class);
        User otherAdmin = mock(User.class);
        Location otherLocation = mock(Location.class);
        when(otherAdmin.getWorld()).thenReturn(world);
        when(otherAdmin.getLocation()).thenReturn(otherLocation);
        when(other.getCenter()).thenReturn(otherLocation);
        when(otherLocation.toVector()).thenReturn(new Vector(512, 64, 512));
        when(location.toVector()).thenReturn(new Vector(0, 64, 0));
        mockedUtil.when(() -> Util.getUUID("first")).thenReturn(uuid);
        mockedUtil.when(() -> Util.getUUID("second")).thenReturn(notUUID);
        when(island.getOwner()).thenReturn(null);
        when(im.getIslandAt(location)).thenReturn(Optional.of(island));
        when(im.getIslandAt(otherLocation)).thenReturn(Optional.of(other));
        assertTrue(itl.canExecute(user, "register", List.of("first")));
        assertTrue(itl.canExecute(otherAdmin, "register", List.of("second")));
        assertTrue(itl.execute(user, "register", List.of("first")));
        verify(im).setOwner(user, uuid, island, RanksManager.VISITOR_RANK);
        verify(im, never()).setOwner(any(), any(), eq(other), anyInt());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void pendingRegistrationRejectsChangedOwnerOrReplacedIsland(boolean ownerChanged) {
        itl = Mockito.spy(itl);
        List<Runnable> pending = new ArrayList<>();
        Mockito.doAnswer(call -> {
            pending.add(call.getArgument(2));
            return null;
        }).when(itl).askConfirmation(any(), anyString(), any(Runnable.class));
        when(island.getOwner()).thenReturn(null);
        when(island.isSpawn()).thenReturn(true);
        when(im.getIslandAt(location)).thenReturn(Optional.of(island));
        assertFalse(itl.canExecute(user, "register", List.of("tastybento")));
        if (ownerChanged) when(island.getOwner()).thenReturn(notUUID);
        else when(im.getIslandAt(location)).thenReturn(Optional.of(mock(Island.class)));
        pending.getFirst().run();
        verify(im, never()).setOwner(any(), any(), any(), anyInt());
        verify(user).sendMessage("commands.admin.register.cannot-make-island");
    }

    @Test
    void confirmedRegistrationUsesCapturedIslandAfterAdminMoves() {
        itl = Mockito.spy(itl);
        List<Runnable> pending = new ArrayList<>();
        Mockito.doAnswer(call -> {
            pending.add(call.getArgument(2));
            return null;
        }).when(itl).askConfirmation(any(), anyString(), any(Runnable.class));
        when(island.getOwner()).thenReturn(null);
        when(island.isSpawn()).thenReturn(true);
        when(location.toVector()).thenReturn(new Vector(0, 64, 0));
        when(im.getIslandAt(location)).thenReturn(Optional.of(island));
        assertFalse(itl.canExecute(user, "register", List.of("tastybento")));
        when(user.getLocation()).thenReturn(mock(Location.class));
        pending.getFirst().run();
        verify(im).setOwner(user, uuid, island, RanksManager.VISITOR_RANK);
        verify(im).clearSpawn(world);
        verify(im).undeleteIsland(island);
    }

    @Test
    void foliaReservationWaitsForChunkAndRegionMarkerBeforeEvent() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        when(location.clone()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));
        var loaded = new java.util.concurrent.CompletableFuture<org.bukkit.Chunk>();
        mockedUtil.when(() -> Util.getChunkAtAsync(location)).thenReturn(loaded);
        List<Runnable> regionWork = new ArrayList<>();
        var written = new java.util.concurrent.CompletableFuture<Void>();
        try (var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.regionFuture(eq(plugin), eq(location), any(Runnable.class)))
                    .thenAnswer(call -> {
                        regionWork.add(call.getArgument(2));
                        return written;
                    });
            itl.reserve(user, uuid, location, "target");
            verify(island).setReserved(true);
            verify(block, never()).setType(any());
            verify(pim, never()).callEvent(any());
            loaded.complete(mock(org.bukkit.Chunk.class));
            verify(block, never()).setType(any());
            when(im.getIslandAt(location)).thenReturn(Optional.of(island));
            when(island.isReserved()).thenReturn(true);
            when(island.getOwner()).thenReturn(uuid);
            regionWork.getFirst().run();
            verify(block).setType(Material.BEDROCK);
            verify(pim, never()).callEvent(any());
            written.complete(null);
            verify(pim).callEvent(any(IslandReservedEvent.class));
        }
    }

    @Test
    void failedReservationMarkerDropsTheUnannouncedIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        when(location.clone()).thenReturn(location);
        when(island.getUniqueId()).thenReturn("reservation");
        when(island.isReserved()).thenReturn(true);
        when(island.getOwner()).thenReturn(uuid);
        mockedUtil.when(() -> Util.getChunkAtAsync(location)).thenReturn(
                java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("chunk load failed")));
        try (var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.regionFuture(eq(plugin), any(), any(Runnable.class)))
                    .thenAnswer(call -> {
                        ((Runnable) call.getArgument(2)).run();
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    });
            itl.reserve(user, uuid, location, "target");
            verify(island).setReserved(true);
            verify(block, never()).setType(any());
            verify(pim, never()).callEvent(any());
            verify(plugin).logError(Mockito.contains("reservation could not place its marker"));
            verify(im).deleteIsland(island, true, uuid);
            tasks.verify(() -> TaskScheduler.user(eq(plugin), eq(user), any(Runnable.class)));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"island", "reserved", "owner"})
    void changedReservationCannotPlaceMarker(String change) {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        when(location.clone()).thenReturn(location);
        var loaded = new java.util.concurrent.CompletableFuture<org.bukkit.Chunk>();
        mockedUtil.when(() -> Util.getChunkAtAsync(location)).thenReturn(loaded);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.regionFuture(eq(plugin), eq(location), any(Runnable.class)))
                    .thenAnswer(call -> {
                        try {
                            ((Runnable) call.getArgument(2)).run();
                            return java.util.concurrent.CompletableFuture.completedFuture(null);
                        } catch (RuntimeException error) {
                            return java.util.concurrent.CompletableFuture.failedFuture(error);
                        }
                    });
            itl.reserve(user, uuid, location, "target");
            when(im.getIslandAt(location)).thenReturn(Optional.of(island));
            when(island.isReserved()).thenReturn(true);
            when(island.getOwner()).thenReturn(uuid);
            switch (change) {
            case "island" -> when(im.getIslandAt(location)).thenReturn(Optional.empty());
            case "reserved" -> when(island.isReserved()).thenReturn(false);
            case "owner" -> when(island.getOwner()).thenReturn(UUID.randomUUID());
            default -> throw new AssertionError(change);
            }
            loaded.complete(mock(org.bukkit.Chunk.class));
            verify(block, never()).setType(any());
            verify(pim, never()).callEvent(any(IslandReservedEvent.class));
            verify(plugin).logError(Mockito.contains("Reservation changed before marker placement"));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void reservationRejectsNewlyOccupiedOrDeletingSpot(boolean occupied) {
        when(im.getIslandAt(location)).thenReturn(occupied ? Optional.of(island) : Optional.empty());
        when(idm.inDeletion(location)).thenReturn(!occupied);
        itl.reserve(user, uuid, location, "target");
        verify(im, never()).createIsland(any(), any(UUID.class));
        verify(block, never()).setType(any());
        verify(user).sendMessage("commands.admin.register.cannot-make-island");
    }

}
