package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event.Result;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Util.class, Bukkit.class })
public class EnderChestListenerTest extends AbstractCommonSetup {

    @Mock
    private ItemStack item;
    @Mock
    private Block clickedBlock;
    private Action action;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Ender chest use is not allowed by default
        Flags.ENDER_CHEST.setSetting(world, false);

        // No special perms
        when(player.hasPermission(anyString())).thenReturn(false);

        // Action, Item and clicked block
        action = Action.RIGHT_CLICK_BLOCK;
        when(item.getType()).thenReturn(Material.ENDER_CHEST);
        when(clickedBlock.getLocation()).thenReturn(location);
        when(clickedBlock.getType()).thenReturn(Material.ENDER_CHEST);
    }

    @Test
    public void testOnEnderChestOpenNotRightClick() {
        action = Action.LEFT_CLICK_AIR;
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        new BlockInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    @Test
    public void testOnEnderChestOpenEnderChestNotInWorld() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        new BlockInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    @Test
    public void testOnEnderChestOpenEnderChestOpPlayer() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Op player
        when(player.isOp()).thenReturn(true);
        new BlockInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    @Test
    public void testOnEnderChestOpenEnderChestHasBypassPerm() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Has bypass perm
        when(player.hasPermission(anyString())).thenReturn(true);
        new BlockInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    @Test
    public void testOnEnderChestOpenEnderChestOkay() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Enderchest use is okay
        Flags.ENDER_CHEST.setSetting(world, true);
        BlockInteractionListener bil = new BlockInteractionListener();
        bil.onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
        verify(notifier, Mockito.never()).notify(any(), anyString());
    }

    @Test
    public void testOnEnderChestOpenEnderChestBlocked() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Enderchest use is blocked
        Flags.ENDER_CHEST.setSetting(world, false);
        new BlockInteractionListener().onPlayerInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        verify(notifier).notify(any(User.class), eq("protection.world-protected"));
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
