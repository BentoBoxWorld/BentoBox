package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PlayerEvent.class, PlayerInteractEvent.class})
public class ObsidianToLavaTest {

    private static World world;

    @Test
    public void testObsidianToLava() {
        BentoBox plugin = mock(BentoBox.class);
        assertNotNull(new ObsidianToLava(plugin));
    }

    @Test
    public void testOnPlayerInteract() {
        // Mock world
        world = mock(World.class);

        // Mock server
        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        // Set the server to the mock
        Bukkit.setServer(server);

        // Mock plugin
        BentoBox plugin = mock(BentoBox.class);

        // Create new object
        ObsidianToLava listener = new ObsidianToLava(plugin);

        // Mock settings
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.isAllowObsidianScooping()).thenReturn(false);
        // Allow scooping
        when(settings.isAllowObsidianScooping()).thenReturn(true);

        // Mock player
        Player who = mock(Player.class);
        when(who.getWorld()).thenReturn(world);

        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        when(who.getLocation()).thenReturn(location);

        // Worlds
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandWorld(Mockito.any())).thenReturn(world);
        when(iwm.getNetherWorld(Mockito.any())).thenReturn(world);
        when(iwm.getEndWorld(Mockito.any())).thenReturn(world);

        // Mock up IslandsManager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);

        // Mock up items and blocks
        ItemStack item = mock(ItemStack.class);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getX()).thenReturn(0);
        when(clickedBlock.getY()).thenReturn(0);
        when(clickedBlock.getZ()).thenReturn(0);
        when(clickedBlock.getWorld()).thenReturn(world);

        // Users
        User.setPlugin(plugin);

        // Put player in world
        when(iwm.inWorld(Mockito.any())).thenReturn(true);
        // Put player on island
        when(im.userIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        // Set as survival
        when(who.getGameMode()).thenReturn(GameMode.SURVIVAL);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");


        // Test all possible actions
        //for (Action action: Action.values()) {
        Action action = Action.RIGHT_CLICK_BLOCK;
        // Test incorrect items
        Material inHand = Material.ACACIA_DOOR;
        Material block = Material.BROWN_MUSHROOM;
        when(item.getType()).thenReturn(inHand);
        when(clickedBlock.getType()).thenReturn(block);
        // Create the event
        testEvent(plugin, who, action, item, clickedBlock);
        // Test with bucket in hand
        inHand = Material.BUCKET;
        block = Material.BROWN_MUSHROOM;
        when(item.getType()).thenReturn(inHand);
        when(clickedBlock.getType()).thenReturn(block);
        // Create the event
        testEvent(plugin, who, action, item, clickedBlock);
        // Test with obsidian in hand
        inHand = Material.ANVIL;
        block = Material.OBSIDIAN;
        when(item.getType()).thenReturn(inHand);
        when(clickedBlock.getType()).thenReturn(block);
        // Create the event
        testEvent(plugin, who, action, item, clickedBlock);
        // Test positive
        inHand = Material.BUCKET;
        block = Material.OBSIDIAN;
        when(item.getType()).thenReturn(inHand);
        when(clickedBlock.getType()).thenReturn(block);
        // Create the event
        testEvent(plugin, who, action, item, clickedBlock);


        PlayerInteractEvent event = new PlayerInteractEvent(who, action, item, clickedBlock, BlockFace.EAST);

        // Test not in world
        when(iwm.inWorld(Mockito.any())).thenReturn(false);
        assertFalse(listener.onPlayerInteract(event));
        // Put player in world
        when(iwm.inWorld(Mockito.any())).thenReturn(true);

        // Test different game modes
        for (GameMode gm : GameMode.values()) {
            when(who.getGameMode()).thenReturn(gm);
            if (!gm.equals(GameMode.SURVIVAL)) {
                assertFalse(listener.onPlayerInteract(event));
            }
        }
        // Set as survival
        when(who.getGameMode()).thenReturn(GameMode.SURVIVAL);

        // Test when player is not on island
        when(im.userIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(false);
        assertFalse(listener.onPlayerInteract(event));


    }

    private void testEvent(BentoBox plugin, Player who, Action action, ItemStack item, Block clickedBlock) {
        Block obsidianBlock = mock(Block.class);
        when(obsidianBlock.getType()).thenReturn(Material.OBSIDIAN);
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);

        ObsidianToLava listener = new ObsidianToLava(plugin);
        PlayerInteractEvent event = new PlayerInteractEvent(who, action, item, clickedBlock, BlockFace.EAST);
        if (!action.equals(Action.RIGHT_CLICK_BLOCK) || !item.getType().equals(Material.BUCKET) || !clickedBlock.getType().equals(Material.OBSIDIAN)) {
            assertFalse(listener.onPlayerInteract(event));
        } else {
            // Test with obby close by in any of the possible locations
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        when(world.getBlockAt(Mockito.eq(x), Mockito.eq(y), Mockito.eq(z))).thenReturn(obsidianBlock);
                        assertFalse(listener.onPlayerInteract(event));
                    }
                }
            }
            // Test where the area is free of obby
            when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(airBlock);
            assertTrue(listener.onPlayerInteract(event));
        }


    }
}
