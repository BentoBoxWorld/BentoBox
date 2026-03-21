package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

class SpawnerSpawnEggsListenerTest extends CommonTestSetup {

    private SpawnerSpawnEggsListener listener;
    @Mock
    private Block block;
    @Mock
    private ItemStack item;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        // Block is a spawner
        when(block.getType()).thenReturn(Material.TRIAL_SPAWNER);
        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);

        // Item is a spawn egg
        when(item.getType()).thenReturn(Material.ZOMBIE_SPAWN_EGG);

        // Player has no bypass permission
        when(mockPlayer.hasPermission(anyString())).thenReturn(false);

        // Default: flag is NOT set (spawn eggs on spawners are blocked)
        Flags.SPAWNER_SPAWN_EGGS.setDefaultSetting(false);

        // User
        User.getInstance(mockPlayer);

        listener = new SpawnerSpawnEggsListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnSpawnerChangeBlocked() {
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        listener.onSpawnerChange(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnSpawnerChangeFlagAllowed() {
        Flags.SPAWNER_SPAWN_EGGS.setDefaultSetting(true);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        listener.onSpawnerChange(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSpawnerChangeNotInWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        listener.onSpawnerChange(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSpawnerChangeNotSpawner() {
        when(block.getType()).thenReturn(Material.STONE);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        listener.onSpawnerChange(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSpawnerChangeNotSpawnEgg() {
        when(item.getType()).thenReturn(Material.DIAMOND);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        listener.onSpawnerChange(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSpawnerChangeBypassPermission() {
        when(mockPlayer.hasPermission("bskyblock.mod.bypass.SPAWNER_SPAWN_EGGS.everywhere")).thenReturn(true);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        listener.onSpawnerChange(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSpawnerChangeBypassProtectPermission() {
        when(mockPlayer.hasPermission("bskyblock.mod.bypassprotect")).thenReturn(true);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        listener.onSpawnerChange(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSpawnerChangeNullItem() {
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, null, block,
                null, EquipmentSlot.HAND);
        listener.onSpawnerChange(e);
        assertFalse(e.isCancelled());
    }
}
