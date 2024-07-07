package world.bentobox.bentobox.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import dev.lone.itemsadder.api.CustomBlock;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.hooks.ItemsAdderHook.BlockInteractListener;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * Test class for ItemsAdder hook
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class, CustomBlock.class })
public class ItemsAdderHookTest {

    @Mock
    private BentoBox plugin;
    private ItemsAdderHook hook;
    @Mock
    private PluginManager pim;
    @Mock
    private Plugin itemsAdder;
    @Mock
    private FlagsManager fm;
    @Mock
    private Location location;
    @Mock
    private Player entity;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private World world;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private PlayersManager pm;
    @Mock
    private PlaceholdersManager phm;
    @Mock
    private Notifier notifier;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // User
        UUID uuid = UUID.randomUUID();
        when(entity.getUniqueId()).thenReturn(uuid);
        User.setPlugin(plugin);
        User.getInstance(entity);

        // Flags Manager
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(pim.getPlugin("ItemsAdder")).thenReturn(itemsAdder);

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(location)).thenReturn(true);

        // CustomBlock
        PowerMockito.mockStatic(CustomBlock.class, Mockito.RETURNS_MOCKS);

        // Location
        when(world.getName()).thenReturn("bskyblock");
        when(location.getWorld()).thenReturn(world);

        // Island manager
        when(plugin.getIslands()).thenReturn(im);

        when(im.getProtectedIslandAt(location)).thenReturn(Optional.of(island));

        // Players Manager
        when(plugin.getPlayers()).thenReturn(pm);
        @Nullable
        Players playerObject = new Players();
        playerObject.setUniqueId(uuid.toString());
        when(pm.getPlayer(uuid)).thenReturn(playerObject);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Return the same string
        when(phm.replacePlaceholders(any(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        hook = new ItemsAdderHook(plugin);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ItemsAdderHook#hook()}.
     */
    @Test
    public void testHook() {
        assertTrue(hook.hook());
        verify(pim).registerEvents(hook.getListener(), plugin);
        verify(fm).registerFlag(ItemsAdderHook.ITEMS_ADDER_EXPLOSIONS);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ItemsAdderHook#hook()}.
     */
    @Test
    public void testHookFail() {
        // No plugin
        when(pim.getPlugin("ItemsAdder")).thenReturn(null);
        assertFalse(hook.hook());
        verify(pim, never()).registerEvents(any(), any());
        verify(fm, never()).registerFlag(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ItemsAdderHook.BlockInteractListener#onExplosion(EntityExplodeEvent)}
     */
    @Test
    public void testListener() {
        // Make listener
        assertTrue(hook.hook());
        BlockInteractListener listener = hook.getListener();
        when(entity.getType()).thenReturn(EntityType.PLAYER);
        when(entity.hasPermission("XXXXXX")).thenReturn(true);
        List<Block> list = new ArrayList<>();
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0, null);
        listener.onExplosion(event);
        assertTrue(event.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ItemsAdderHook#ItemsAdderHook(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testItemsAdderHook() {
        assertNotNull(hook);
        assertEquals(Material.NETHER_STAR, hook.getIcon());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.ItemsAdderHook#clearBlockInfo(org.bukkit.Location)}.
     */
    @Ignore("Temp skip until this is optimized")
    @Test
    public void testClearBlockInfo() {
        hook.clearBlockInfo(location);
        PowerMockito.verifyStatic(CustomBlock.class);
        CustomBlock.remove(location);
    }

}
