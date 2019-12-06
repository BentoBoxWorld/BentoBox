/**
 *
 */
package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;



/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class ThrowingListenerTest {

    private Location location;
    private BentoBox plugin;
    private Notifier notifier;

    private ThrowingListener tl;
    private Player player;
    private World world;
    private Island island;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);
        world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pim = mock(PluginManager.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        ItemMeta meta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
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

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        User.setPlugin(plugin);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> (String)Arrays.asList(invocation.getArguments()).get(1);
        when(lm.get(any(), any())).thenAnswer(answer);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Island manager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        island = mock(Island.class);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(Mockito.any())).thenReturn(optional);
        // Default is that everything is allowed
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);

        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());

        // Thrown listener
        tl = new ThrowingListener();

        // Player
        player = mock(Player.class);
        when(player.getLocation()).thenReturn(location);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("tastybento");
        when(player.getWorld()).thenReturn(world);

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotion() {
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getShooter()).thenReturn(player);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        tl.onPlayerThrowPotion(e);
        assertFalse(e.isCancelled());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotionNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getShooter()).thenReturn(player);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        tl.onPlayerThrowPotion(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotionNonHuman() {
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        Witch witch = mock(Witch.class);
        when(witch.getLocation()).thenReturn(location);
        when(witch.getUniqueId()).thenReturn(UUID.randomUUID());
        when(witch.getName()).thenReturn("witch");
        when(witch.getWorld()).thenReturn(world);
        when(entity.getShooter()).thenReturn(witch);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        tl.onPlayerThrowPotion(e);
        assertFalse(e.isCancelled());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotionNotAllowedNonHuman() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        Witch witch = mock(Witch.class);
        when(witch.getLocation()).thenReturn(location);
        when(witch.getUniqueId()).thenReturn(UUID.randomUUID());
        when(witch.getName()).thenReturn("witch");
        when(witch.getWorld()).thenReturn(world);
        when(entity.getShooter()).thenReturn(witch);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        tl.onPlayerThrowPotion(e);
        assertFalse(e.isCancelled());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

}
