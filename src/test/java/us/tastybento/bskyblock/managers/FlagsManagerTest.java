package us.tastybento.bskyblock.managers;

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
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.flags.FlagBuilder;
import us.tastybento.bskyblock.listeners.flags.BreakBlocksListener;
import us.tastybento.bskyblock.lists.Flags;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {BSkyBlock.class, Flags.class} )
public class FlagsManagerTest {
    

    private static BSkyBlock plugin;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);


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
        PowerMockito.mockStatic(Flags.class);
  
    }

    @Test
    public void testFlagsManager() {
        assertNotNull(new FlagsManager(plugin));
    }

    @Test
    public void testRegisterFlag() {
        FlagsManager fm = new FlagsManager(plugin);
        // Try to register every single flag - it should fail every time
        Flags.values().forEach(dupe -> assertFalse(fm.registerFlag(dupe)));
        // Change the ID to something random, but use every icon that is already used
        Flags.values().forEach(dupe -> {
            assertFalse(fm.registerFlag(new FlagBuilder()
                    .id(UUID.randomUUID().toString())
                    .icon(dupe.getIcon())
                    .listener(new BreakBlocksListener())
                    .build()));
        });
        // This should pass
        Flag originalFlag = new FlagBuilder().id("ORIGINAL").icon(Material.EMERALD_BLOCK).listener(new BreakBlocksListener()).build();
        assertTrue(fm.registerFlag(originalFlag));
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
