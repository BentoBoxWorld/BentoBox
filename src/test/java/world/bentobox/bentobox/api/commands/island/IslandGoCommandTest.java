package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Test for island go command
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class IslandGoCommandTest {
    @Mock
    private CompositeCommand ic;
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private PluginManager pim;
    @Mock
    private Settings s;
    @Mock
    private BukkitTask task;
    @Mock
    private Player player;
    private IslandGoCommand igc;
    @Mock
    private Notifier notifier;
    @Mock
    private World world;
    private @Nullable WorldSettings ws;

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
        when(plugin.getSettings()).thenReturn(s);

        // Player
        when(player.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("tastybento");
        when(player.getWorld()).thenReturn(world);
        user = User.getInstance(player);
        // Set the User class plugin as this one
        User.setPlugin(plugin);


        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        // Have the create command point to the ic command
        Optional<CompositeCommand> createCommand = Optional.of(ic);
        when(ic.getSubCommand(eq("create"))).thenReturn(createCommand);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);
        // Event register
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        ws = new MyWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        // Just return an empty addon for now
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        PowerMockito.mockStatic(Util.class);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Return the same string
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // Command
        igc = new IslandGoCommand(ic);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link IslandGoCommand#canExecute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(igc.canExecute(user, igc.getLabel(), Collections.emptyList()));
        verify(player).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link IslandGoCommand#canExecute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgs() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        assertTrue(igc.canExecute(user, igc.getLabel(), Collections.emptyList()));
    }

    /**
     * Test method for {@link IslandGoCommand#canExecute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsReservedIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(ic.call(any(), any(), any())).thenReturn(true);
        when(island.isReserved()).thenReturn(true);
        assertFalse(igc.canExecute(user, igc.getLabel(), Collections.emptyList()));
        verify(ic).call(any(), any(), any());
    }

    /**
     * Test method for {@link IslandGoCommand#canExecute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsReservedIslandNoCreateCommand() {
        when(ic.getSubCommand(eq("create"))).thenReturn(Optional.empty());
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(ic.call(any(), any(), any())).thenReturn(true);
        when(island.isReserved()).thenReturn(true);
        assertFalse(igc.canExecute(user, igc.getLabel(), Collections.emptyList()));
        verify(ic, Mockito.never()).call(any(), any(), any());
    }

    /**
     * Test method for {@link IslandGoCommand#canExecute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsNoTeleportWhenFalling() {
        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(world, true);
        when(player.getFallDistance()).thenReturn(10F);
        assertFalse(igc.canExecute(user, igc.getLabel(), Collections.emptyList()));
        verify(player).sendMessage(eq("protection.flags.PREVENT_TELEPORT_WHEN_FALLING.hint"));
    }

    /**
     * Test method for {@link IslandGoCommand#canExecute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsNoTeleportWhenFallingNotFalling() {
        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(world, true);
        when(player.getFallDistance()).thenReturn(0F);
        assertTrue(igc.canExecute(user, igc.getLabel(), Collections.emptyList()));
    }

    /**
     * Test method for {@link IslandGoCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsMultipleHomes() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        //when(user.getPermissionValue(anyString(), anyInt())).thenReturn(3);
        assertTrue(igc.execute(user, igc.getLabel(), Collections.emptyList()));
    }

    /**
     * Test method for {@link IslandGoCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteArgs1MultipleHomes() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        //when(user.getPermissionValue(anyString(), anyInt())).thenReturn(3);
        assertTrue(igc.execute(user, igc.getLabel(), Collections.singletonList("1")));
    }

    /**
     * Test method for {@link IslandGoCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteArgs2MultipleHomes() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        //when(user.getPermissionValue(anyString(), anyInt())).thenReturn(3);
        assertTrue(igc.execute(user, igc.getLabel(), Collections.singletonList("2")));
    }


    /**
     * Test method for {@link IslandGoCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteArgsJunkMultipleHomes() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        //when(user.getPermissionValue(anyString(), anyInt())).thenReturn(3);
        assertTrue(igc.execute(user, igc.getLabel(), Collections.singletonList("sdfghhj")));
    }

    /**
     * Test method for {@link IslandGoCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsDelay() {
        when(s.getDelayTime()).thenReturn(10);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        assertTrue(igc.execute(user, igc.getLabel(), Collections.emptyList()));
        verify(player).sendMessage(eq("commands.delay.stand-still"));
    }

    /**
     * Test method for {@link IslandGoCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsDelayTwice() {
        when(s.getDelayTime()).thenReturn(10);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        assertTrue(igc.execute(user, igc.getLabel(), Collections.emptyList()));
        // Twice
        assertTrue(igc.execute(user, igc.getLabel(), Collections.emptyList()));
        verify(task).cancel();
        verify(player).sendMessage(eq("commands.delay.previous-command-cancelled"));
        verify(player, Mockito.times(2)).sendMessage(eq("commands.delay.stand-still"));
    }

    /**
     * Test method for {@link IslandGoCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteNoArgsDelayMultiHome() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        //when(user.getPermissionValue(anyString(), anyInt())).thenReturn(3);
        when(s.getDelayTime()).thenReturn(10);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        assertTrue(igc.execute(user, igc.getLabel(), Collections.singletonList("2")));
        verify(player).sendMessage(eq("commands.delay.stand-still"));
    }

    /**
     * Test method for {@link IslandGoCommand#onPlayerMove(PlayerMoveEvent)}
     */
    @Test
    public void testOnPlayerMoveHeadMoveNothing() {
        Location l = mock(Location.class);
        Vector vector = mock(Vector.class);
        when(l.toVector()).thenReturn(vector);
        when(player.getLocation()).thenReturn(l);
        PlayerMoveEvent e = new PlayerMoveEvent(player, l, l);
        igc.onPlayerMove(e);
        verify(player, Mockito.never()).sendMessage(eq("commands.delay.moved-so-command-cancelled"));
    }

    /**
     * Test method for {@link IslandGoCommand#onPlayerMove(PlayerMoveEvent)}
     */
    @Test
    public void testOnPlayerMoveHeadMoveTeleportPending() {
        Location l = mock(Location.class);
        Vector vector = mock(Vector.class);
        when(l.toVector()).thenReturn(vector);
        when(player.getLocation()).thenReturn(l);
        testExecuteNoArgsDelay();
        PlayerMoveEvent e = new PlayerMoveEvent(player, l, l);
        igc.onPlayerMove(e);
        verify(player, Mockito.never()).sendMessage(eq("commands.delay.moved-so-command-cancelled"));
    }

    /**
     * Test method for {@link IslandGoCommand#onPlayerMove(PlayerMoveEvent)}
     */
    @Test
    public void testOnPlayerMovePlayerMoveTeleportPending() {
        Location l = mock(Location.class);
        Vector vector = mock(Vector.class);
        when(l.toVector()).thenReturn(vector);
        when(player.getLocation()).thenReturn(l);
        testExecuteNoArgsDelay();
        Location l2 = mock(Location.class);
        Vector vector2 = mock(Vector.class);
        when(l2.toVector()).thenReturn(vector2);
        PlayerMoveEvent e = new PlayerMoveEvent(player, l, l2);
        igc.onPlayerMove(e);
        verify(notifier).notify(any(), eq("commands.delay.moved-so-command-cancelled"));
    }

    class MyWorldSettings implements WorldSettings {

        private Map<String, Boolean> worldFlags = new HashMap<>();

        /**
         * @param worldFlags the worldFlags to set
         */
        public void setWorldFlags(Map<String, Boolean> worldFlags) {
            this.worldFlags = worldFlags;
        }

        @Override
        public GameMode getDefaultGameMode() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<Flag, Integer> getDefaultIslandFlags() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<Flag, Integer> getDefaultIslandSettings() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Difficulty getDifficulty() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setDifficulty(Difficulty difficulty) {
            // TODO Auto-generated method stub

        }

        @Override
        public String getFriendlyName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getIslandDistance() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getIslandHeight() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getIslandProtectionRange() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getIslandStartX() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getIslandStartZ() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getIslandXOffset() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getIslandZOffset() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public List<String> getIvSettings() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getMaxHomes() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getMaxIslands() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getMaxTeamSize() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getNetherSpawnRadius() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getPermissionPrefix() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<EntityType> getRemoveMobsWhitelist() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getSeaHeight() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public List<String> getHiddenFlags() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<String> getVisitorBannedCommands() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, Boolean> getWorldFlags() {
            return worldFlags;
        }

        @Override
        public String getWorldName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isDragonSpawn() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEndGenerate() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEndIslands() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isNetherGenerate() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isNetherIslands() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isOnJoinResetEnderChest() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isOnJoinResetInventory() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isOnJoinResetMoney() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isOnJoinResetHealth() {
            return false;
        }

        @Override
        public boolean isOnJoinResetHunger() {
            return false;
        }

        @Override
        public boolean isOnJoinResetXP() {
            return false;
        }

        @Override
        public boolean isOnLeaveResetEnderChest() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isOnLeaveResetInventory() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isOnLeaveResetMoney() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isOnLeaveResetHealth() {
            return false;
        }

        @Override
        public boolean isOnLeaveResetHunger() {
            return false;
        }

        @Override
        public boolean isOnLeaveResetXP() {
            return false;
        }

        @Override
        public boolean isUseOwnGenerator() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isWaterUnsafe() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public List<String> getGeoLimitSettings() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getResetLimit() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getResetEpoch() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setResetEpoch(long timestamp) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean isTeamJoinDeathReset() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int getDeathsMax() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean isDeathsCounted() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDeathsResetOnNewIsland() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isAllowSetHomeInNether() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isAllowSetHomeInTheEnd() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isRequireConfirmationToSetHomeInNether() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isRequireConfirmationToSetHomeInTheEnd() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int getBanLimit() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean isLeaversLoseReset() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isKickedKeepInventory() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isCreateIslandOnFirstLoginEnabled() {
            return false;
        }

        @Override
        public int getCreateIslandOnFirstLoginDelay() {
            return 0;
        }

        @Override
        public boolean isCreateIslandOnFirstLoginAbortOnLogout() {
            return false;
        }

        @Override
        public @NonNull List<String> getOnJoinCommands() {
            return null;
        }

        @Override
        public @NonNull List<String> getOnLeaveCommands() {
            return null;
        }
    }
}
