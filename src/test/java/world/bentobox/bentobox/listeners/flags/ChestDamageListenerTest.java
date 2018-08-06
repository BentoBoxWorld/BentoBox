/**
 *
 */
package world.bentobox.bentobox.listeners.flags;

import static org.junit.Assert.assertEquals;
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
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.BeforeClass;
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
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class} )
public class ChestDamageListenerTest {

    private static Location location;
    private static BentoBox plugin;
    private static World world;

    @BeforeClass
    public static void setUpClass() {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);
        world = mock(World.class);
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
        IslandWorldManager iwm = mock(IslandWorldManager.class);
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
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        // Users
        //User user = mock(User.class);
        ///user.setPlugin(plugin);
        User.setPlugin(plugin);


        // Locales - final

        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> (String)Arrays.asList(invocation.getArguments()).get(1);
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
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(Mockito.any())).thenReturn(optional);

    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.ChestDamageListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionChestDamageNotAllowed() {
        Flags.CHEST_DAMAGE.setSetting(world, false);
        Entity entity = mock(Entity.class);
        when(entity.getType()).thenReturn(EntityType.PRIMED_TNT);
        List<Block> list = new ArrayList<>();
        Block chest = mock(Block.class);
        when(chest.getType()).thenReturn(Material.CHEST);
        when(chest.getLocation()).thenReturn(location);
        Block trappedChest = mock(Block.class);
        when(trappedChest.getType()).thenReturn(Material.TRAPPED_CHEST);
        when(trappedChest.getLocation()).thenReturn(location);
        Block stone = mock(Block.class);
        when(stone.getType()).thenReturn(Material.STONE);
        when(stone.getLocation()).thenReturn(location);
        list.add(chest);
        list.add(trappedChest);
        list.add(stone);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, list, 0);
        ChestDamageListener listener = new ChestDamageListener();
        listener.setPlugin(plugin);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertEquals(1, e.blockList().size());
        assertFalse(e.blockList().contains(chest));
        assertFalse(e.blockList().contains(trappedChest));
        assertTrue(e.blockList().contains(stone));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.ChestDamageListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionChestDamageAllowed() {
        Flags.CHEST_DAMAGE.setSetting(world, true);
        Entity entity = mock(Entity.class);
        when(entity.getType()).thenReturn(EntityType.PRIMED_TNT);
        List<Block> list = new ArrayList<>();
        Block chest = mock(Block.class);
        when(chest.getType()).thenReturn(Material.CHEST);
        when(chest.getLocation()).thenReturn(location);
        Block trappedChest = mock(Block.class);
        when(trappedChest.getType()).thenReturn(Material.TRAPPED_CHEST);
        when(trappedChest.getLocation()).thenReturn(location);
        Block stone = mock(Block.class);
        when(stone.getType()).thenReturn(Material.STONE);
        when(stone.getLocation()).thenReturn(location);
        list.add(chest);
        list.add(trappedChest);
        list.add(stone);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, list, 0);
        ChestDamageListener listener = new ChestDamageListener();
        listener.setPlugin(plugin);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertEquals(3, e.blockList().size());
        assertTrue(e.blockList().contains(chest));
        assertTrue(e.blockList().contains(trappedChest));
        assertTrue(e.blockList().contains(stone));
    }

}
