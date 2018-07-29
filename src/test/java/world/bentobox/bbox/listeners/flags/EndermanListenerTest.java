/**
 *
 */
package world.bentobox.bbox.listeners.flags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.Settings;
import world.bentobox.bbox.api.configuration.WorldSettings;
import world.bentobox.bbox.database.objects.Island;
import world.bentobox.bbox.lists.Flags;
import world.bentobox.bbox.managers.FlagsManager;
import world.bentobox.bbox.managers.IslandWorldManager;
import world.bentobox.bbox.managers.IslandsManager;
import world.bentobox.bbox.util.Util;

/**
 * Tests enderman related listeners
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class EndermanListenerTest {

    private static Location location;
    private static BentoBox plugin;
    private static IslandWorldManager iwm;
    private static IslandsManager im;
    private static World world;
    private static Enderman enderman;
    private static Slime slime;

    @Before
    public void setUp() {
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

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);

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
        enderman = mock(Enderman.class);
        when(enderman.getLocation()).thenReturn(location);
        when(enderman.getWorld()).thenReturn(world);
        when(enderman.getCarriedMaterial()).thenReturn(new MaterialData(Material.STONE));
        slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<String>());

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

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));
        // Not allowed to start
        Flags.ENDERMAN_GRIEFING.setSetting(world, false);
        // Allowed to start
        Flags.ENDERMAN_DEATH_DROP.setSetting(world, true);

    }


    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanGrief(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testNotEnderman() {
        EndermanListener listener = new EndermanListener();
        Block to = mock(Block.class);
        Material block = Material.ACACIA_DOOR;
        byte data = 0;
        @SuppressWarnings("deprecation")
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(slime, to, block, data);
        listener.onEndermanGrief(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanGrief(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testOnEndermanGriefWrongWorld() {
        when(iwm.inWorld(Mockito.any())).thenReturn(false);
        EndermanListener listener = new EndermanListener();
        Block to = mock(Block.class);
        Material block = Material.ACACIA_DOOR;
        byte data = 0;
        @SuppressWarnings("deprecation")
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(enderman, to, block, data);
        listener.onEndermanGrief(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanGrief(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testOnEndermanGriefAllowed() {
        Flags.ENDERMAN_GRIEFING.setSetting(world, true);
        EndermanListener listener = new EndermanListener();
        Block to = mock(Block.class);
        Material block = Material.ACACIA_DOOR;
        byte data = 0;
        @SuppressWarnings("deprecation")
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(enderman, to, block, data);
        listener.onEndermanGrief(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanGrief(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testOnEndermanGrief() {
        EndermanListener listener = new EndermanListener();
        Block to = mock(Block.class);
        Material block = Material.ACACIA_DOOR;
        byte data = 0;
        @SuppressWarnings("deprecation")
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(enderman, to, block, data);
        listener.onEndermanGrief(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanDeath(org.bukkit.event.entity.EntityDeathEvent)}.
     */
    @Test
    public void testOnNotEndermanDeath() {
        EndermanListener listener = new EndermanListener();
        EntityDeathEvent e = new EntityDeathEvent(slime, new ArrayList<ItemStack>());
        listener.onEndermanDeath(e);
        Mockito.verify(world, Mockito.never()).dropItemNaturally(Mockito.any(), Mockito.any());

    }

    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanDeath(org.bukkit.event.entity.EntityDeathEvent)}.
     */
    @Test
    public void testOnEndermanDeathCarryAir() {
        when(enderman.getCarriedMaterial()).thenReturn(new MaterialData(Material.AIR));
        EndermanListener listener = new EndermanListener();
        EntityDeathEvent e = new EntityDeathEvent(enderman, new ArrayList<ItemStack>());
        listener.onEndermanDeath(e);
        Mockito.verify(world, Mockito.never()).dropItemNaturally(Mockito.any(), Mockito.any());
    }

    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanDeath(org.bukkit.event.entity.EntityDeathEvent)}.
     */
    @Test
    public void testOnEndermanDeathNotInWorld() {
        when(iwm.inWorld(Mockito.any())).thenReturn(false);
        EndermanListener listener = new EndermanListener();
        EntityDeathEvent e = new EntityDeathEvent(enderman, new ArrayList<ItemStack>());
        listener.onEndermanDeath(e);
        Mockito.verify(world, Mockito.never()).dropItemNaturally(Mockito.any(), Mockito.any());

    }

    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanDeath(org.bukkit.event.entity.EntityDeathEvent)}.
     */
    @Test
    public void testOnEndermanDeathNoFlag() {
        Flags.ENDERMAN_DEATH_DROP.setSetting(world, false);
        EndermanListener listener = new EndermanListener();
        EntityDeathEvent e = new EntityDeathEvent(enderman, new ArrayList<ItemStack>());
        listener.onEndermanDeath(e);
        Mockito.verify(world, Mockito.never()).dropItemNaturally(Mockito.any(), Mockito.any());

    }

    /**
     * Test method for {@link world.bentobox.bbox.listeners.flags.EndermanListener#onEndermanDeath(org.bukkit.event.entity.EntityDeathEvent)}.
     */
    @Test
    public void testOnEndermanDeath() {
        EndermanListener listener = new EndermanListener();
        EntityDeathEvent e = new EntityDeathEvent(enderman, new ArrayList<ItemStack>());
        listener.onEndermanDeath(e);
        Mockito.verify(world).dropItemNaturally(Mockito.any(), Mockito.any());

    }

}
