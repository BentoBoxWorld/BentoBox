package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class BannedCommandsTest {

    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Player player;
    @Mock
    private BentoBox plugin;
    @Mock
    private IslandsManager im;
    @Mock
    private World world;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(new ArrayList<>());
        when(iwm.getFallingBannedCommands(any())).thenReturn(new ArrayList<>());
        WorldSettings ws = new MyWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        when(plugin.getIWM()).thenReturn(iwm);

        // Player
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(mock(Location.class));
        User.getInstance(player);
        Server server = mock(Server.class);
        Set<Player> onlinePlayers = new HashSet<>();
        for (int j = 0; j < 10; j++) {
            Player p = mock(Player.class);
            UUID uuid = UUID.randomUUID();
            when(p.getUniqueId()).thenReturn(uuid);
            when(p.getName()).thenReturn(uuid.toString());
            onlinePlayers.add(p);
        }
        when(server.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> onlinePlayers);
        when(player.getServer()).thenReturn(server);

        // Island manager
        // Default not on island, so is a visitor
        when(im.locationIsOnIsland(any(), any())).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // Notifier
        Notifier notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Set flag
        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(world, true);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testInstantReturn() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);

        // Not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);

        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());

        // In world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        // Op
        when(player.isOp()).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());

        // Not op
        when(player.isOp()).thenReturn(false);
        // Has bypass perm
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());

        // Does not have perm
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        // Not a visitor
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testEmptyBannedCommands() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommands() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
        verify(iwm).getVisitorBannedCommands(any());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/blah with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
        verify(iwm).getVisitorBannedCommands(any());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommand() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommandWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/banned_command with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testAnotherBannedCommandsWithBannedCommandWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/another_banned_command with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }


    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommand() {
        when(player.getFallDistance()).thenReturn(10F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommandNotFalling() {
        when(player.getFallDistance()).thenReturn(0F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommandNoFlag() {
        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(world, false);
        when(player.getFallDistance()).thenReturn(0F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertFalse(e.isCancelled());

    }

    /*
     * internal storage class
     */
    class MyWorldSettings implements WorldSettings {

        private Map<String, Boolean> worldFlags = new HashMap<>();

        @Override
        public @NonNull List<String> getOnLeaveCommands() {
            return null;
        }

        @Override
        public @NonNull List<String> getOnJoinCommands() {
            return null;
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

    }
}
