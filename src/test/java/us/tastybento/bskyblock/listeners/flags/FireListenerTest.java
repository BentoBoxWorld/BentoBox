package us.tastybento.bskyblock.listeners.flags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.Notifier;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.lists.Flags;
import us.tastybento.bskyblock.managers.FlagsManager;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BSkyBlock.class, Flags.class, Util.class} )
public class FireListenerTest {

    private static Location location;
    private static BSkyBlock plugin;
    private static FlagsManager flagsManager;
    private static Zombie zombie;
    private static Slime slime;
    private static Cow cow;
    private static IslandWorldManager iwm;

    @BeforeClass
    public static void setUp() {
        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);

        Server server = mock(Server.class);
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        Bukkit.setServer(server);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        PowerMockito.mockStatic(Flags.class);

        flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Worlds
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandWorld()).thenReturn(world);
        when(iwm.getNetherWorld()).thenReturn(world);
        when(iwm.getEndWorld()).thenReturn(world);
        when(iwm.inWorld(any())).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);
        
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);
        
        // Monsters and animals
        zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);
        cow = mock(Cow.class);
        when(cow.getLocation()).thenReturn(location);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<String>());

        // Users
        //User user = mock(User.class);
        ///user.setPlugin(plugin);
        User.setPlugin(plugin);
        
        // Locales - final
        
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");
        
    }

    @Test
    public void testCheckFire() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.of(island));
        
        // Block on fire
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBurnEvent e = new BlockBurnEvent(block, block);

        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);
        
        // Disallow fire
        when(island.isAllowed(Mockito.any())).thenReturn(false);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Flags.FIRE.setDefaultSetting(false);
        assertTrue(listener.checkFire(e, location, Flags.FIRE));
        Flags.FIRE.setDefaultSetting(true);
        assertTrue(listener.checkFire(e, location, Flags.FIRE));
        
        // Allow fire
        when(island.isAllowed(Mockito.any())).thenReturn(true);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.FIRE.setDefaultSetting(false);
        assertFalse(listener.checkFire(e, location, Flags.FIRE));
        Flags.FIRE.setDefaultSetting(true);
        assertFalse(listener.checkFire(e, location, Flags.FIRE));
        
        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // Fire is not allowed, so should be cancelled
        Flags.FIRE.setDefaultSetting(false);
        assertTrue(listener.checkFire(e, location, Flags.FIRE));
        // Fire allowed
        Flags.FIRE.setDefaultSetting(true);
        assertFalse(listener.checkFire(e, location, Flags.FIRE));
    }

    @Test
    public void testOnBlockBurn() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.of(island));
        
        // Block on fire
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBurnEvent e = new BlockBurnEvent(block, block);

        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);
        
        // Disallow fire
        when(island.isAllowed(Mockito.any())).thenReturn(false);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Flags.FIRE.setDefaultSetting(false);
        assertTrue(listener.onBlockBurn(e));
        Flags.FIRE.setDefaultSetting(true);
        assertTrue(listener.onBlockBurn(e));
        
        // Allow fire
        when(island.isAllowed(Mockito.any())).thenReturn(true);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.FIRE.setDefaultSetting(false);
        assertFalse(listener.onBlockBurn(e));
        Flags.FIRE.setDefaultSetting(true);
        assertFalse(listener.onBlockBurn(e));
        
        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // Fire is not allowed, so should be cancelled
        Flags.FIRE.setDefaultSetting(false);
        assertTrue(listener.onBlockBurn(e));
        // Fire allowed
        Flags.FIRE.setDefaultSetting(true);
        assertFalse(listener.onBlockBurn(e));
    }

    @Test
    public void testOnBlockSpread() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.of(island));
        
        // Block on fire spread
        
        Block block = mock(Block.class);
        Block fire = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(fire.getLocation()).thenReturn(location);
        when(fire.getType()).thenReturn(Material.FIRE);
        
        BlockSpreadEvent e = new BlockSpreadEvent(block, fire, null);

        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);
        
        // Disallow fire
        when(island.isAllowed(Mockito.any())).thenReturn(false);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Flags.FIRE_SPREAD.setDefaultSetting(false);
        assertTrue(listener.onBlockSpread(e));
        Flags.FIRE_SPREAD.setDefaultSetting(true);
        assertTrue(listener.onBlockSpread(e));
        
        // Allow fire spread
        when(island.isAllowed(Mockito.any())).thenReturn(true);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.FIRE_SPREAD.setDefaultSetting(false);
        assertFalse(listener.onBlockSpread(e));
        Flags.FIRE_SPREAD.setDefaultSetting(true);
        assertFalse(listener.onBlockSpread(e));
        
        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // Fire spread is not allowed, so should be cancelled
        Flags.FIRE_SPREAD.setDefaultSetting(false);
        assertTrue(listener.onBlockSpread(e));
        // Fire allowed
        Flags.FIRE_SPREAD.setDefaultSetting(true);
        assertFalse(listener.onBlockSpread(e));
    }

    @Test
    public void testOnBlockIgnite() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.of(island));
        
        // Block on fire spread

        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.OBSIDIAN);

        BlockIgniteEvent e = new BlockIgniteEvent(block, null, block);

        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);
        
        // Obsidian is okay to ignite
        assertFalse(listener.onBlockIgnite(e));
        
        // Now set to something flammable
        when(block.getType()).thenReturn(Material.WOOD);
        
        // Disallow fire
        when(island.isAllowed(Mockito.any())).thenReturn(false);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Flags.FIRE.setDefaultSetting(false);
        assertTrue(listener.onBlockIgnite(e));
        Flags.FIRE.setDefaultSetting(true);
        assertTrue(listener.onBlockIgnite(e));
        
        // Allow fire spread
        when(island.isAllowed(Mockito.any())).thenReturn(true);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.FIRE.setDefaultSetting(false);
        assertFalse(listener.onBlockIgnite(e));
        Flags.FIRE.setDefaultSetting(true);
        assertFalse(listener.onBlockIgnite(e));
        
        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // Fire spread is not allowed, so should be cancelled
        Flags.FIRE.setDefaultSetting(false);
        assertTrue(listener.onBlockIgnite(e));
        // Fire allowed
        Flags.FIRE.setDefaultSetting(true);
        assertFalse(listener.onBlockIgnite(e));
    }

    /*
    @Test
    public void testOnPlayerInteract() {
        fail("Not yet implemented"); // TODO
    }
*/
    @Test
    public void testOnTNTPrimed() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.of(island));
        
        // Block on fire spread
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.OBSIDIAN);
        EntityChangeBlockEvent e = mock(EntityChangeBlockEvent.class);
        when(e.getBlock()).thenReturn(block);


        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);
        
        // Obsidian is not TNT
        assertFalse(listener.onTNTPrimed(e));
        // Out of world
        when(iwm.inWorld(any())).thenReturn(false);
        assertFalse(listener.onTNTPrimed(e));
        
        // Now set to TNT
        when(block.getType()).thenReturn(Material.TNT);
        assertFalse(listener.onTNTPrimed(e));
        
        // Back in world
        when(iwm.inWorld(any())).thenReturn(true);

        // Disallow fire
        when(island.isAllowed(Mockito.any())).thenReturn(false);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Flags.FIRE.setDefaultSetting(false);
        assertTrue(listener.onTNTPrimed(e));
        Flags.FIRE.setDefaultSetting(true);
        assertTrue(listener.onTNTPrimed(e));
        
        // Allow fire spread
        when(island.isAllowed(Mockito.any())).thenReturn(true);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.FIRE.setDefaultSetting(false);
        assertFalse(listener.onTNTPrimed(e));
        Flags.FIRE.setDefaultSetting(true);
        assertFalse(listener.onTNTPrimed(e));
        
        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // Fire spread is not allowed, so should be cancelled
        Flags.FIRE.setDefaultSetting(false);
        assertTrue(listener.onTNTPrimed(e));
        // Fire allowed
        Flags.FIRE.setDefaultSetting(true);
        assertFalse(listener.onTNTPrimed(e));
    }

    @Test
    public void testOnTNTDamage() {
        // Notifier
        Notifier notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);
        
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.of(island));
        
        // Block on fire
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.OBSIDIAN);
        EntityChangeBlockEvent e = mock(EntityChangeBlockEvent.class);
        when(e.getBlock()).thenReturn(block);


        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);
        
        // Obsidian is not TNT
        assertFalse(listener.onTNTDamage(e));
        // Out of world
        when(block.getLocation()).thenReturn(null);
        assertFalse(listener.onTNTDamage(e));
        
        // Now set to TNT
        when(block.getType()).thenReturn(Material.TNT);
        assertFalse(listener.onTNTDamage(e));
        
        // Back in world
        when(block.getLocation()).thenReturn(location);

        // Entity is not a projectile
        Player player = mock(Player.class);
        when(e.getEntity()).thenReturn(player);
        assertFalse(listener.onTNTDamage(e));
        
        // Entity is an arrow
        Arrow arrow = mock(Arrow.class);
        // Shooter is a skeleton
        WitherSkeleton skeleton = mock(WitherSkeleton.class);
        when(arrow.getShooter()).thenReturn(skeleton);
        // No fire arrow
        when(arrow.getFireTicks()).thenReturn(0);
        when(e.getEntity()).thenReturn(arrow);
        assertFalse(listener.onTNTDamage(e));
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);
        assertFalse(listener.onTNTDamage(e));
        
        // Shooter is a player
        when(arrow.getShooter()).thenReturn(player);
        // No fire arrow
        when(arrow.getFireTicks()).thenReturn(0);
        when(e.getEntity()).thenReturn(arrow);
        assertFalse(listener.onTNTDamage(e));

        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);
        
        
        // Break blocks not allowed, general flag should have no effect
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Flags.BREAK_BLOCKS.setDefaultSetting(false);
        assertTrue(listener.onTNTDamage(e));
        Flags.BREAK_BLOCKS.setDefaultSetting(true);
        assertTrue(listener.onTNTDamage(e));
        
        // Allow BREAK_BLOCKS spread
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.BREAK_BLOCKS.setDefaultSetting(false);
        assertFalse(listener.onTNTDamage(e));
        Flags.BREAK_BLOCKS.setDefaultSetting(true);
        assertFalse(listener.onTNTDamage(e));
        
        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // BREAK_BLOCKS spread is not allowed, so should be cancelled
        Flags.BREAK_BLOCKS.setDefaultSetting(false);
        assertTrue(listener.onTNTDamage(e));
        // BREAK_BLOCKS allowed
        Flags.BREAK_BLOCKS.setDefaultSetting(true);
        assertFalse(listener.onTNTDamage(e));
        

    }

}
