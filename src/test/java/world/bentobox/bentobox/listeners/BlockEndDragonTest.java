package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class BlockEndDragonTest {

    @Mock
    private Player player;
    private BlockEndDragon bed;
    @Mock
    private World world;
    @Mock
    private Location loc;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Block block;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        @Nullable
        WorldSettings ws = new MyWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        // World is the end to start
        when(iwm.isIslandEnd(any())).thenReturn(true);
        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);

        // World
        when(block.getType()).thenReturn(Material.AIR);
        when(block.getY()).thenReturn(255);
        when(block.getX()).thenReturn(0);
        when(block.getZ()).thenReturn(0);
        when(block.getWorld()).thenReturn(world);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(world.getEnvironment()).thenReturn(Environment.THE_END);
        // Player
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(loc);
        when(loc.getWorld()).thenReturn(world);
        User.getInstance(player);

        // Set flag
        Flags.REMOVE_END_EXIT_ISLAND.setSetting(world, true);

        // Class
        bed = new BlockEndDragon(plugin);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerChangeWorld() {
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        bed.onPlayerChangeWorld(event);
        verify(block).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerChangeWorldNotEnd() {
        when(iwm.isIslandEnd(any())).thenReturn(false);
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        bed.onPlayerChangeWorld(event);
        verify(block, never()).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerChangeWorldBlockSet() {
        when(block.getType()).thenReturn(Material.END_PORTAL);
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        bed.onPlayerChangeWorld(event);
        verify(block, never()).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerChangeWorldNoFlag() {
        Flags.REMOVE_END_EXIT_ISLAND.setSetting(world, false);
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        bed.onPlayerChangeWorld(event);
        verify(block, never()).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerJoinWorld(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinWorld() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        bed.onPlayerJoinWorld(event);
        verify(block).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlace() {
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, player, false, null);
        bed.onEndBlockPlace(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceX() {
        when(block.getX()).thenReturn(23);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, player, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }
    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceZ() {
        when(block.getZ()).thenReturn(23);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, player, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceY() {
        when(block.getY()).thenReturn(23);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, player, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceNether() {
        when(world.getEnvironment()).thenReturn(Environment.NETHER);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, player, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceNoFlag() {
        Flags.REMOVE_END_EXIT_ISLAND.setSetting(world, false);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, player, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceWrongWorld() {
        when(iwm.isEndGenerate(any())).thenReturn(false);
        when(iwm.isEndIslands(any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, player, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());

        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(false);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());

        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnEndBlockBreak() {
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bed.onEndBlockBreak(e);
        assertTrue(e.isCancelled());
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
