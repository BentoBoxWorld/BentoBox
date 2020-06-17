package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class ElytraListenerTest {
    @Mock
    private BentoBox plugin;
    @Mock
    private Player player;
    @Mock
    private Location location;
    @Mock
    private World world;
    private UUID uuid = UUID.randomUUID();

    private ElytraListener el;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    private Island island;
    @Mock
    private PluginManager pim;
    @Mock
    private ItemFactory itemFactory;
    @Mock
    private Notifier notifier;
    @Mock
    private PlayerInventory inv;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        when(player.isGliding()).thenReturn(true);
        User.setPlugin(plugin);
        User.getInstance(player);
        
        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);
        // Worlds
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);
        // Island manager
        when(plugin.getIslands()).thenReturn(im);
        island = mock(Island.class);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(any())).thenReturn(optional);
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);
        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> (String)Arrays.asList(invocation.getArguments()).get(1);
        when(lm.get(any(), any())).thenAnswer(answer);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);
        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);


        // Class under test
        el = new ElytraListener();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGlide(org.bukkit.event.entity.EntityToggleGlideEvent)}.
     */
    @Test
    public void testOnGlideAllowed() {
        EntityToggleGlideEvent e = new EntityToggleGlideEvent(player, false);
        el.onGlide(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGlide(org.bukkit.event.entity.EntityToggleGlideEvent)}.
     */
    @Test
    public void testOnGlideNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        EntityToggleGlideEvent e = new EntityToggleGlideEvent(player, false);
        el.onGlide(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGliding(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testGlidingAllowed() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, location);
        el.onGliding(e);
        verify(notifier, never()).notify(any(), anyString());
        assertFalse(e.isCancelled());
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGliding(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testGlidingNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, location);
        el.onGliding(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }
    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGliding(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testGlidingNotGliding() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        when(player.isGliding()).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, location);
        el.onGliding(e);
        verify(notifier, never()).notify(any(), anyString());
        assertFalse(e.isCancelled());
    }

}
