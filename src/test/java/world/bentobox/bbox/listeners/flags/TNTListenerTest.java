package world.bentobox.bbox.listeners.flags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.Settings;
import world.bentobox.bbox.api.configuration.WorldSettings;
import world.bentobox.bbox.api.user.Notifier;
import world.bentobox.bbox.api.user.User;
import world.bentobox.bbox.database.objects.Island;
import world.bentobox.bbox.lists.Flags;
import world.bentobox.bbox.managers.FlagsManager;
import world.bentobox.bbox.managers.IslandWorldManager;
import world.bentobox.bbox.managers.IslandsManager;
import world.bentobox.bbox.managers.LocalesManager;
import world.bentobox.bbox.managers.PlayersManager;
import world.bentobox.bbox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class} )
public class TNTListenerTest {

    private static Location location;
    private static BentoBox plugin;
    private static IslandWorldManager iwm;
    private static IslandsManager im;
    private static Notifier notifier;

    @BeforeClass
    public static void setUpClass() {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

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

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Worlds
        iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(any())).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Monsters and animals
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);
        Cow cow = mock(Cow.class);
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
        Answer<String> answer = new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String)Arrays.asList(invocation.getArguments()).get(1);
            }

        };
        when(lm.get(any(), any())).thenAnswer(answer);

        // Player name
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(Mockito.any())).thenReturn("tastybento");
        when(plugin.getPlayers()).thenReturn(pm);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Island manager
        im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(Mockito.any())).thenReturn(optional);

        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));
    }

    @Test
    public void testOnTNTPriming() {
        BlockFace clickedFace = BlockFace.DOWN;
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getType()).thenReturn(Material.TNT);
        when(clickedBlock.getLocation()).thenReturn(location);
        ItemStack item = new ItemStack(Material.FLINT_AND_STEEL);
        Action action = Action.RIGHT_CLICK_BLOCK;
        Player player = mock(Player.class);
        PlayerInteractEvent e = new PlayerInteractEvent(player , action, item, clickedBlock, clickedFace);

        TNTListener listener = new TNTListener();
        listener.setPlugin(plugin);
        listener.onTNTPriming(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    @Test
    public void testOnExplosion() {
        Entity entity = mock(Entity.class);
        when(entity.getType()).thenReturn(EntityType.PRIMED_TNT);
        List<Block> list = new ArrayList<>();
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        list.add(block);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, list, 0);
        TNTListener listener = new TNTListener();
        listener.setPlugin(plugin);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
        assertTrue(list.isEmpty());
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
        when(im.getProtectedIslandAt(Mockito.any())).thenReturn(Optional.of(island));

        // Block on fire
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getType()).thenReturn(Material.OBSIDIAN);
        EntityChangeBlockEvent e = mock(EntityChangeBlockEvent.class);
        when(e.getBlock()).thenReturn(block);


        // TNT listener
        TNTListener listener = new TNTListener();
        listener.setPlugin(plugin);

        // Obsidian is not TNT
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());
        // Out of world
        when(block.getLocation()).thenReturn(null);
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());

        // Now set to TNT
        when(block.getType()).thenReturn(Material.TNT);
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());

        // Back in world
        when(block.getLocation()).thenReturn(location);

        // Entity is not a projectile
        Player player = mock(Player.class);
        when(e.getEntity()).thenReturn(player);
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());

        // Entity is an arrow
        Arrow arrow = mock(Arrow.class);
        // Shooter is a skeleton
        WitherSkeleton skeleton = mock(WitherSkeleton.class);
        when(arrow.getShooter()).thenReturn(skeleton);
        // No fire arrow
        when(arrow.getFireTicks()).thenReturn(0);
        when(e.getEntity()).thenReturn(arrow);
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());
        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());

        // Shooter is a player
        when(arrow.getShooter()).thenReturn(player);
        // No fire arrow
        when(arrow.getFireTicks()).thenReturn(0);
        when(e.getEntity()).thenReturn(arrow);
        listener.onTNTDamage(e);
        assertFalse(e.isCancelled());

        // Fire arrow
        when(arrow.getFireTicks()).thenReturn(10);

        /*
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
        when(im.getProtectedIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // BREAK_BLOCKS spread is not allowed, so should be cancelled
        Flags.BREAK_BLOCKS.setDefaultSetting(false);
        assertTrue(listener.onTNTDamage(e));
        // BREAK_BLOCKS allowed
        Flags.BREAK_BLOCKS.setDefaultSetting(true);
        assertFalse(listener.onTNTDamage(e));
         */

    }

}
