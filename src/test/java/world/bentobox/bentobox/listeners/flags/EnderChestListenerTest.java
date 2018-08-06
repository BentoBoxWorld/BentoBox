package world.bentobox.bentobox.listeners.flags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, User.class })
public class EnderChestListenerTest {

    private World world;
    private Player player;
    private IslandWorldManager iwm;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        
        // World
        world = mock(World.class);
        
        // Owner
        UUID uuid1 = UUID.randomUUID();
        
        // Island initialization
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid1);

        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);

        Location inside = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(opIsland);
        // On island
        when(im.locationIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);
                
        // World Settings
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        // By default everything is in world
        when(iwm.inWorld(Mockito.any())).thenReturn(true);
        
        // Ender chest use is not allowed by default
        Flags.ENDER_CHEST.setSetting(world, false);
        
        // Sometimes use Mockito.withSettings().verboseLogging()
        player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOp()).thenReturn(false);
        // No special perms
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        when(player.getWorld()).thenReturn(world);

        // Locales - this returns the string that was requested for translation 
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgumentAt(1, String.class));
        
    }

    @Test
    public void testOnEnderChestOpenNotRightClick() {
        Action action = Action.LEFT_CLICK_AIR;
        ItemStack item = mock(ItemStack.class);
        Block clickedBlock = mock(Block.class);
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        new EnderChestListener().onEnderChestOpen(e);
        assertFalse(e.isCancelled());
    }
    
    @Test
    public void testOnEnderChestOpenNotEnderChest() {
        Action action = Action.RIGHT_CLICK_BLOCK;
        ItemStack item = mock(ItemStack.class);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.STONE);
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        new EnderChestListener().onEnderChestOpen(e);
        assertFalse(e.isCancelled());
    }
    
    @Test
    public void testOnEnderChestOpenEnderChestNotInWorld() {
        Action action = Action.RIGHT_CLICK_BLOCK;
        ItemStack item = mock(ItemStack.class);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.ENDER_CHEST);
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Not in world
        when(iwm.inWorld(Mockito.any())).thenReturn(false);
        new EnderChestListener().onEnderChestOpen(e);
        assertFalse(e.isCancelled());
    }
    
    @Test
    public void testOnEnderChestOpenEnderChestOpPlayer() {
        Action action = Action.RIGHT_CLICK_BLOCK;
        ItemStack item = mock(ItemStack.class);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.ENDER_CHEST);
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Op player
        when(player.isOp()).thenReturn(true);
        new EnderChestListener().onEnderChestOpen(e);
        assertFalse(e.isCancelled());
    }
    
    @Test
    public void testOnEnderChestOpenEnderChestHasBypassPerm() {
        Action action = Action.RIGHT_CLICK_BLOCK;
        ItemStack item = mock(ItemStack.class);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.ENDER_CHEST);
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Has bypass perm
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        new EnderChestListener().onEnderChestOpen(e);
        assertFalse(e.isCancelled());
    }
    
    @Test
    public void testOnEnderChestOpenEnderChestOkay() {
        Action action = Action.RIGHT_CLICK_BLOCK;
        ItemStack item = mock(ItemStack.class);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.ENDER_CHEST);
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Enderchest use is okay
        Flags.ENDER_CHEST.setSetting(world, true);
        new EnderChestListener().onEnderChestOpen(e);
        assertFalse(e.isCancelled());
    }
    
    @Test
    public void testOnEnderChestOpenEnderChestBlocked() {
        Action action = Action.RIGHT_CLICK_BLOCK;
        ItemStack item = mock(ItemStack.class);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.ENDER_CHEST);
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Enderchest use is okay
        Flags.ENDER_CHEST.setSetting(world, false);
        new EnderChestListener().onEnderChestOpen(e);
        assertTrue(e.isCancelled());
        Mockito.verify(player).sendMessage("protection.protected");
    }

    @Test
    public void testOnCraftNotEnderChest() {
        Recipe recipe = mock(Recipe.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(recipe.getResult()).thenReturn(item);
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory top = mock(Inventory.class);
        when(top.getSize()).thenReturn(9);
        when(view.getTopInventory()).thenReturn(top);
        SlotType type = SlotType.RESULT;
        ClickType click = ClickType.LEFT;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        CraftItemEvent e = new CraftItemEvent(recipe, view, type, 0, click, action);
        new EnderChestListener().onCraft(e);
        assertFalse(e.isCancelled());
    }
    
    @Test
    public void testOnCraftEnderChest() {
        Recipe recipe = mock(Recipe.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ENDER_CHEST);
        when(recipe.getResult()).thenReturn(item);
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory top = mock(Inventory.class);
        when(top.getSize()).thenReturn(9);
        when(view.getTopInventory()).thenReturn(top);
        SlotType type = SlotType.RESULT;
        ClickType click = ClickType.LEFT;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        CraftItemEvent e = new CraftItemEvent(recipe, view, type, 0, click, action);
        new EnderChestListener().onCraft(e);
        assertTrue(e.isCancelled());
    }

}
