package world.bentobox.bentobox.api.commands.admin.purge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandDeletedEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
public class AdminPurgeCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;

    private AdminPurgeCommand apc;
    @Mock
    private Addon addon;
    @Mock
    private PlayersManager pm;
    @Mock
    private BukkitScheduler scheduler;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock the method to immediately run the Runnable
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run(); // Immediately run the Runnable
            return null; // or return a mock of the Task if needed
        });
        mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        when(ac.getWorld()).thenReturn(world);

        when(ac.getAddon()).thenReturn(addon);
        when(ac.getTopLabel()).thenReturn("bsb");

        // No islands by default
        when(im.getIslands()).thenReturn(Collections.emptyList());
        when(im.getIslandsASync()).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        // IWM
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Island
        when(island.isOwned()).thenReturn(true); // Default owned
        when(location.toVector()).thenReturn(new Vector(1, 2, 3));
        when(island.getCenter()).thenReturn(location);

        // Player manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getName(any())).thenReturn("name");

        Settings settings = new Settings();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        // Command
        apc = new AdminPurgeCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#AdminPurgeCommand(CompositeCommand)}.
     */
    @Test
    public void testConstructor() {
        verify(addon).registerListener(apc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("admin.purge", apc.getPermission());
        assertFalse(apc.isOnlyPlayer());
        assertEquals("commands.admin.purge.parameters", apc.getParameters());
        assertEquals("commands.admin.purge.description", apc.getDescription());
        assertEquals(6, apc.getSubCommands().size());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringEmptyArgs() {
        assertFalse(apc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.help.header",
                "[label]",
                "BSkyBlock");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringWithArg() {
        assertTrue(apc.canExecute(user, "", Collections.singletonList("23")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNotNumber() {
        assertFalse(apc.execute(user, "", Collections.singletonList("abc")));
        verify(user).sendMessage(eq("commands.admin.purge.number-error"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringZero() {
        assertFalse(apc.execute(user, "", Collections.singletonList("0")));
        verify(user).sendMessage(eq("commands.admin.purge.days-one-or-more"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIslands() {
        assertTrue(apc.execute(user, "", Collections.singletonList("10")));
        verify(user).sendMessage(eq("commands.admin.purge.purgable-islands"), eq("[number]"), eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIslandsPurgeProtected() {
        when(island.isPurgeProtected()).thenReturn(true);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apc.execute(user, "", Collections.singletonList("10")));
        verify(user).sendMessage(eq("commands.admin.purge.purgable-islands"), eq("[number]"), eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIslandsWrongWorld() {
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.getWorld()).thenReturn(mock(World.class));
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apc.execute(user, "", Collections.singletonList("10")));
        verify(user).sendMessage(eq("commands.admin.purge.purgable-islands"), eq("[number]"), eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIslandsUnowned() {
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.getWorld()).thenReturn(world);
        when(island.getOwner()).thenReturn(null);
        when(island.isUnowned()).thenReturn(true);
        when(island.isOwned()).thenReturn(false);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apc.execute(user, "", Collections.singletonList("10")));
        verify(user).sendMessage(eq("commands.admin.purge.purgable-islands"), eq("[number]"), eq("0"));
    }

    /**
     * Makes sure that no spawn islands are deleted
     */
    @Test
    public void testExecuteUserStringListOfStringOnlyIslandSpawn() {
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.getWorld()).thenReturn(world);
        when(island.isSpawn()).thenReturn(true);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apc.execute(user, "", Collections.singletonList("10")));
        verify(user).sendMessage(eq("commands.admin.purge.purgable-islands"), eq("[number]"), eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testExecuteUserStringListOfStringNoIslandsTeamIsland() {
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.getWorld()).thenReturn(world);
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(UUID.randomUUID(), UUID.randomUUID()));
        when(im.getIslands()).thenReturn(Collections.singleton(island));

        // All players are up to date
         OfflinePlayer op = mock(OfflinePlayer.class);
        when(op.getLastPlayed()).thenReturn(System.currentTimeMillis());
        mockedBukkit.when(() -> Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(op);

        assertTrue(apc.execute(user, "", Collections.singletonList("10")));
        verify(user).sendMessage(eq("commands.admin.purge.purgable-islands"), eq("[number]"), eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIslandsRecentLogin() {
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.getWorld()).thenReturn(world);
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(UUID.randomUUID()));
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        OfflinePlayer op = mock(OfflinePlayer.class);
        when(op.getLastPlayed()).thenReturn(System.currentTimeMillis());
        mockedBukkit.when(() -> Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(op);
        assertTrue(apc.execute(user, "", Collections.singletonList("10")));
        verify(user).sendMessage(eq("commands.admin.purge.purgable-islands"), eq("[number]"), eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringIslandsFound() {
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.getWorld()).thenReturn(world);
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(island.isOwned()).thenReturn(true);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(UUID.randomUUID()));
        when(im.getIslandsASync()).thenReturn(CompletableFuture.completedFuture(List.of(island)));
        when(pm.getLastLoginTimestamp(any())).thenReturn(962434800L);
        assertTrue(apc.execute(user, "", Collections.singletonList("10"))); // 10 days ago
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.total-islands", "[number]", "1");
        verify(user, never()).sendMessage("commands.admin.purge.none-found");
        verify(user).sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, "bsb");
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#removeIslands()}.
     */
    @Test
    public void testRemoveIslands() {
        @NonNull
        Optional<Island> opIsland = Optional.of(island);
        when(im.getIslandById(any())).thenReturn(opIsland);
        testExecuteUserStringListOfStringIslandsFound();
        assertTrue(apc.execute(user, "", Collections.singletonList("confirm")));
        verify(im).deleteIsland(eq(island), eq(true), eq(null));
        verify(plugin).log(any());
        verify(user).sendMessage(eq("commands.admin.purge.see-console-for-status"), eq("[label]"), eq("bsb"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#onIslandDeleted(world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeletedEvent)}.
     */
    @Test
    public void testOnIslandDeletedNotInPurge() {
        IslandDeletedEvent e = mock(IslandDeletedEvent.class);
        apc.onIslandDeleted(e);
        verify(user, Mockito.never()).sendMessage(any());
        verify(plugin, Mockito.never()).log(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#onIslandDeleted(world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeletedEvent)}.
     */
    @Test
    public void testOnIslandDeletedPurgeCompleted() {
        testRemoveIslands();
        IslandDeletedEvent e = mock(IslandDeletedEvent.class);
        apc.onIslandDeleted(e);
        verify(user).sendMessage(eq("commands.admin.purge.completed"));
        verify(plugin, Mockito.never()).log("");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#isInPurge()}.
     */
    @Test
    public void testIsInPurge() {
        assertFalse(apc.isInPurge());
        testRemoveIslands();
        assertTrue(apc.isInPurge());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#stop()}.
     */
    @Test
    public void testStop() {
        testRemoveIslands();
        assertTrue(apc.isInPurge());
        apc.stop();
        assertFalse(apc.isInPurge());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#setUser(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testSetUser() {
        apc.setUser(user);
        apc.removeIslands();
        verify(user, Mockito.times(1)).sendMessage(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand#getOldIslands(int)}
     * @throws TimeoutException 
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    @Test
    public void testGetOldIslands() throws InterruptedException, ExecutionException, TimeoutException {
        assertTrue(apc.execute(user, "", Collections.singletonList("10"))); // 10 days ago
        // First, ensure that the result is empty
        CompletableFuture<Set<String>> result = apc.getOldIslands(10);
        Set<String> set = result.join();
        assertTrue(set.isEmpty());
        // Mocking Islands and their retrieval
        Island island1 = mock(Island.class);
        Island island2 = mock(Island.class);
        
        when(im.getIslandsASync()).thenReturn(CompletableFuture.completedFuture(List.of(island1, island2)));
        // Now, check again after mocking islands
        CompletableFuture<Set<String>> futureWithIslands = apc.getOldIslands(10);
        assertTrue(futureWithIslands.get(5, TimeUnit.SECONDS).isEmpty()); // Adjust this assertion based on the expected behavior of getOldIslands

    }

}
