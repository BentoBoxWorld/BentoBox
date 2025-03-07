package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.bukkit.Tag;
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

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.AbstractCommonSetup;
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
@PrepareForTest({ Bukkit.class, BentoBox.class, Flags.class, Util.class, ServerBuildInfo.class })
public class ChestDamageListenerTest extends AbstractCommonSetup
{

    private BentoBox plugin;
    private World world;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Tags
        when(Tag.SHULKER_BOXES.isTagged(any(Material.class))).thenReturn(false);

        Server server = mock(Server.class);
        world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pim = mock(PluginManager.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        PowerMockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Worlds
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
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

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link ChestDamageListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionChestDamageNotAllowed() {
        // Srt the flag to not allow chest damage
        Flags.CHEST_DAMAGE.setSetting(world, false);
        // Set the entity that is causing the damage (TNT)
        Entity entity = mock(Entity.class);
        when(entity.getType()).thenReturn(EntityType.TNT);

        // Create a list of blocks that will potentially be damaged by TNT
        List<Block> list = new ArrayList<>();
        Block chest = mock(Block.class);
        when(chest.getType()).thenReturn(Material.CHEST); // Regular chest
        when(chest.getLocation()).thenReturn(location);

        Block trappedChest = mock(Block.class);
        when(trappedChest.getType()).thenReturn(Material.TRAPPED_CHEST);// Trapped chest
        when(trappedChest.getLocation()).thenReturn(location);

        Block stone = mock(Block.class);
        when(stone.getType()).thenReturn(Material.STONE); // Stone
        when(stone.getLocation()).thenReturn(location);
        list.add(chest);
        list.add(trappedChest);
        list.add(stone);
        // Create the event
        EntityExplodeEvent e = getExplodeEvent(entity, location, list);
        // Listener to test
        ChestDamageListener listener = new ChestDamageListener();
        listener.setPlugin(plugin);
        listener.onExplosion(e);

        // Verify
        assertFalse(e.isCancelled());
        assertEquals(1, e.blockList().size());
        assertFalse(e.blockList().contains(chest));
        assertFalse(e.blockList().contains(trappedChest));
        assertTrue(e.blockList().contains(stone));
    }

    /**
     * Test method for {@link ChestDamageListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionChestDamageAllowed() {
        Flags.CHEST_DAMAGE.setSetting(world, true);
        Entity entity = mock(Entity.class);
        when(entity.getType()).thenReturn(EntityType.TNT);
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
        EntityExplodeEvent e = getExplodeEvent(entity, location, list);
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
