package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Bukkit.class, Util.class, HandlerList.class} )
public class FlagsManagerTest {

    /**
     * Update this value if the number of registered listeners changes
     */
    private static final int NUMBER_OF_LISTENERS = 48;
    @Mock
    private BentoBox plugin;
    @Mock
    private Server server;
    @Mock
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Util class to handle PaperLib
        PowerMockito.mockStatic(Util.class);
        when(Util.isPaper()).thenReturn(false);

        // Plugin is loaded
        when(plugin.isLoaded()).thenReturn(true);

        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);


        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        //PowerMockito.mockStatic(Flags.class);

    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testFlagsManager() {
        assertNotNull(new FlagsManager(plugin));
    }

    @Test
    public void testRegisterDuplicateFlag() {
        FlagsManager fm = new FlagsManager(plugin);
        // Try to register every single flag - it should fail every time
        Flags.values().forEach(dupe -> assertFalse(fm.registerFlag(dupe)));
    }

    @Test
    public void testRegisterOriginalFlagOriginalListener() {
        when(plugin.isLoaded()).thenReturn(true);
        FlagsManager fm = new FlagsManager(plugin);
        verify(pluginManager, times(NUMBER_OF_LISTENERS)).registerEvents(any(), eq(plugin));
        verify(pluginManager, times(NUMBER_OF_LISTENERS)).registerEvents(any(), eq(plugin));
        // This should pass
        OriginalListener ol = new OriginalListener();
        Flag originalFlag = new Flag.Builder("ORIGINAL", Material.EMERALD_BLOCK).listener(ol).build();
        assertTrue(fm.registerFlag(originalFlag));
        // Verify registered one more
        verify(pluginManager, times(NUMBER_OF_LISTENERS+1)).registerEvents(any(), eq(plugin));
        // Register another flag with same listener
        Flag originalFlag2 = new Flag.Builder("ORIGINAL2", Material.COAL_ORE).listener(ol).build();
        assertTrue(fm.registerFlag(originalFlag2));
        // Verify registered only once more
        verify(pluginManager, times(NUMBER_OF_LISTENERS+1)).registerEvents(any(), eq(plugin));
    }

    class OriginalListener implements Listener {
        // nothing here
    }


    /**
     * Test for {@link FlagsManager#getFlags()}
     */
    @Test
    public void testGetFlags() {
        FlagsManager fm = new FlagsManager(plugin);
        assertTrue(Flags.values().containsAll(fm.getFlags()));
        assertTrue(fm.getFlags().containsAll(Flags.values()));
    }

    /**
     * Test for {@link FlagsManager#getFlag(String)}
     */
    @Test
    public void testGetFlagByID() {
        FlagsManager fm = new FlagsManager(plugin);
        // Test in forward and reverse order so that any duplicates are caught
        Flags.values().stream().sorted().forEach(flag -> assertEquals(flag, fm.getFlag(flag.getID()).get()));
        Flags.values().stream().sorted(Comparator.reverseOrder()).forEach(flag -> assertEquals(flag, fm.getFlag(flag.getID()).get()));

    }

    /**
     * Test for {@link FlagsManager#unregister(Flag)}
     */
    @Test
    public void testUnregisterFlag() {
        PowerMockito.mockStatic(HandlerList.class);
        when(plugin.isLoaded()).thenReturn(true);
        FlagsManager fm = new FlagsManager(plugin);
        // Listener
        OriginalListener ol = new OriginalListener();
        Flag originalFlag = new Flag.Builder("ORIGINAL", Material.EMERALD_BLOCK).listener(ol).build();
        assertTrue(fm.registerFlag(originalFlag));
        assertEquals(originalFlag, fm.getFlag("ORIGINAL").get());
        // Remove
        fm.unregister(originalFlag);
        assertFalse(fm.getFlag("ORIGINAL").isPresent());
        // Verify the listener was removed
        PowerMockito.verifyStatic(HandlerList.class);
        HandlerList.unregisterAll(ol);
    }

}
