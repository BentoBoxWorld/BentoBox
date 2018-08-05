package world.bentobox.bentobox.managers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagBuilder;
import world.bentobox.bentobox.listeners.flags.BreakBlocksListener;
import world.bentobox.bentobox.lists.Flags;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Bukkit.class} )
public class FlagsManagerTest {


    private static BentoBox plugin;
    private static Server server;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Plugin is loaded
        when(plugin.isLoaded()).thenReturn(true);

        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);


        server = mock(Server.class);
        World world = mock(World.class);
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
        PowerMockito.mockStatic(Flags.class);

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
    public void testRegisterDuplicateFlagIcons() {
        FlagsManager fm = new FlagsManager(plugin);
        // Change the ID to something random, but use every icon that is already used
        Flags.values().forEach(dupe -> assertFalse(fm.registerFlag(new FlagBuilder()
                .id(UUID.randomUUID().toString())
                .icon(dupe.getIcon())
                .listener(new BreakBlocksListener())
                .build())));
    }

    @Test
    public void testRegisteroriginalFlagPluginNotLoaded() {
        when(plugin.isLoaded()).thenReturn(false);
        FlagsManager fm = new FlagsManager(plugin);
        // This should pass
        Flag originalFlag = new FlagBuilder().id("ORIGINAL").icon(Material.EMERALD_BLOCK).listener(new BreakBlocksListener()).build();
        assertTrue(fm.registerFlag(originalFlag));
        // Verify no Bukkit listener registered
        Mockito.verify(server, Mockito.never()).getPluginManager();
    }


    @Test
    public void testRegisteroriginalFlagPluginLoadedOriginalListener() {
        when(plugin.isLoaded()).thenReturn(true);
        FlagsManager fm = new FlagsManager(plugin);
        // This should pass
        OriginalListener ol = new OriginalListener();
        Flag originalFlag = new FlagBuilder().id("ORIGINAL").icon(Material.EMERALD_BLOCK).listener(ol).build();
        assertTrue(fm.registerFlag(originalFlag));
        // Verify registered
        Mockito.verify(server).getPluginManager();
        // Register another flag with same listener
        Flag originalFlag2 = new FlagBuilder().id("ORIGINAL2").icon(Material.COAL_ORE).listener(ol).build();
        assertTrue(fm.registerFlag(originalFlag2));
        // Verify registered only once
        Mockito.verify(server).getPluginManager();
    }

    class OriginalListener implements Listener {
        // nothing here
    }


    @Test
    public void testGetFlags() {
        FlagsManager fm = new FlagsManager(plugin);
        assertThat(fm.getFlags(), is(Flags.values()));
    }

    @Test
    public void testGetFlagByID() {
        FlagsManager fm = new FlagsManager(plugin);
        // Test in forward and reverse order so that any duplicates are caught
        Flags.values().stream().sorted().forEach(flag -> assertEquals(flag, fm.getFlagByID(flag.getID())));
        Flags.values().stream().sorted(Comparator.reverseOrder()).forEach(flag -> assertEquals(flag, fm.getFlagByID(flag.getID())));

    }


}
