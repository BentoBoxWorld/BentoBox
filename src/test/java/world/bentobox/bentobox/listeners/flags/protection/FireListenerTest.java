package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.bukkit.entity.Cow;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Bukkit.class, Flags.class, Util.class} )
public class FireListenerTest {

    private Location location;
    private BentoBox plugin;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Bukkit.class);
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
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
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
        User.setPlugin(plugin);

        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Player name
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(Mockito.any())).thenReturn("tastybento");
        when(plugin.getPlayers()).thenReturn(pm);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));
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
        Flags.FLINT_AND_STEEL.setDefaultSetting(false);
        assertTrue(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));
        Flags.FLINT_AND_STEEL.setDefaultSetting(true);
        assertTrue(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));

        // Allow fire
        when(island.isAllowed(Mockito.any())).thenReturn(true);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.FLINT_AND_STEEL.setDefaultSetting(false);
        assertFalse(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));
        Flags.FLINT_AND_STEEL.setDefaultSetting(true);
        assertFalse(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));

        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // Fire is not allowed, so should be cancelled
        Flags.FLINT_AND_STEEL.setDefaultSetting(false);
        assertTrue(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));
        // Fire allowed
        Flags.FLINT_AND_STEEL.setDefaultSetting(true);
        assertFalse(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));
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
        Flags.FIRE_BURNING.setDefaultSetting(false);
        assertTrue(listener.onBlockBurn(e));
        Flags.FIRE_BURNING.setDefaultSetting(true);
        assertTrue(listener.onBlockBurn(e));

        // Allow fire
        when(island.isAllowed(Mockito.any())).thenReturn(true);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.FIRE_BURNING.setDefaultSetting(false);
        assertFalse(listener.onBlockBurn(e));
        Flags.FIRE_BURNING.setDefaultSetting(true);
        assertFalse(listener.onBlockBurn(e));

        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // Fire is not allowed, so should be cancelled
        Flags.FIRE_BURNING.setDefaultSetting(false);
        assertTrue(listener.onBlockBurn(e));
        // Fire allowed
        Flags.FIRE_BURNING.setDefaultSetting(true);
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
        when(block.getType()).thenReturn(Material.OAK_PLANKS);

        // Disallow fire
        when(island.isAllowed(Mockito.any())).thenReturn(false);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        Flags.FIRE_IGNITE.setDefaultSetting(false);
        assertTrue(listener.onBlockIgnite(e));
        Flags.FIRE_IGNITE.setDefaultSetting(true);
        assertTrue(listener.onBlockIgnite(e));

        // Allow fire spread
        when(island.isAllowed(Mockito.any())).thenReturn(true);
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        Flags.FIRE_IGNITE.setDefaultSetting(false);
        assertFalse(listener.onBlockIgnite(e));
        Flags.FIRE_IGNITE.setDefaultSetting(true);
        assertFalse(listener.onBlockIgnite(e));

        // Check with no island
        when(im.getIslandAt(Matchers.any())).thenReturn(Optional.empty());
        // Fire spread is not allowed, so should be cancelled
        Flags.FIRE_IGNITE.setDefaultSetting(false);
        assertTrue(listener.onBlockIgnite(e));
        // Fire allowed
        Flags.FIRE_IGNITE.setDefaultSetting(true);
        assertFalse(listener.onBlockIgnite(e));
    }
}
