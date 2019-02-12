package world.bentobox.bentobox.listeners.flags.worldsettings;

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
import org.junit.After;
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
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class })
public class EnderChestListenerTest {

    private World world;
    private Player player;
    private IslandWorldManager iwm;
    private Notifier notifier;
    private ItemStack item;
    private Block clickedBlock;
    private Action action;

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
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

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
        User.setPlugin(plugin);
        User.getInstance(player);

        // Locales - this returns the string that was requested for translation
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgumentAt(1, String.class));

        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        // Action, Item and clicked block
        action = Action.RIGHT_CLICK_BLOCK;
        item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ENDER_CHEST);
        clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(inside);
        when(clickedBlock.getType()).thenReturn(Material.ENDER_CHEST);
        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());

        Settings settings = mock(Settings.class);
        // Fake players
        when(plugin.getSettings()).thenReturn(settings);

    }

    @After
    public void cleanUp() {
        User.clearUsers();
    }

    @Test
    public void testOnEnderChestOpenNotRightClick() {
        action = Action.LEFT_CLICK_AIR;
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        new BlockInteractionListener().onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnEnderChestOpenEnderChestNotInWorld() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        new BlockInteractionListener().onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnEnderChestOpenEnderChestOpPlayer() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Op player
        when(player.isOp()).thenReturn(true);
        new BlockInteractionListener().onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnEnderChestOpenEnderChestHasBypassPerm() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Has bypass perm
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        new BlockInteractionListener().onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnEnderChestOpenEnderChestOkay() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Enderchest use is okay
        Flags.ENDER_CHEST.setSetting(world, true);
        BlockInteractionListener bil = new BlockInteractionListener();
        bil.onPlayerInteract(e);
        assertFalse(e.isCancelled());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.anyObject(), Mockito.anyString());
    }

    @Test
    public void testOnEnderChestOpenEnderChestBlocked() {
        BlockFace clickedBlockFace = BlockFace.EAST;
        PlayerInteractEvent e = new PlayerInteractEvent(player, action, item, clickedBlock, clickedBlockFace);
        // Enderchest use is blocked
        Flags.ENDER_CHEST.setSetting(world, false);
        new BlockInteractionListener().onPlayerInteract(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(User.class), Mockito.eq("protection.protected"));
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
