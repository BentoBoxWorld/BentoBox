package us.tastybento.bskyblock.api.panels.builders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.panels.ClickType;
import us.tastybento.bskyblock.api.panels.PanelItem;

@RunWith(PowerMockRunner.class)
public class PanelItemBuilderTest {

    private static PanelItemBuilder builder;

    @SuppressWarnings("deprecation")
    @BeforeClass
    public static void setUp() throws Exception {
        builder = new PanelItemBuilder();
        Server server = mock(Server.class);
        World world = mock(World.class);
        world = mock(World.class);
        Mockito.when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        Mockito.when(server.getWorld("world")).thenReturn(world);
        Mockito.when(server.getVersion()).thenReturn("BSB_Mocking");
        
        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);
        
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);
        
        Bukkit.setServer(server);
        
        SkullMeta skullMeta = mock(SkullMeta.class);
        when(skullMeta.getOwner()).thenReturn("tastybento");
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("tastybento");

        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        //when(Bukkit.getServer()).thenReturn(server);
    }

    @Test
    public void testIconMaterial() {
        builder.icon(Material.STONE);
        PanelItem item = builder.build();
        assertNotNull(item.getItem().getType());
        assertEquals(Material.STONE, item.getItem().getType());
    }

    @Test
    public void testIconItemStack() {
        builder.icon(new ItemStack(Material.IRON_ORE));
        PanelItem item = builder.build();
        assertNotNull(item.getItem().getType());
        assertEquals(Material.IRON_ORE, item.getItem().getType());
    }

    @Test
    public void testIconUUID() {
        builder.icon(UUID.fromString("5988eecd-1dcd-4080-a843-785b62419"));
        PanelItem item = builder.build();
        assertNotNull(item.getItem().getType());
        assertEquals(Material.SKULL_ITEM, item.getItem().getType());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIconString() {
        builder.icon("tastybento");
        PanelItem item = builder.build();
        assertNotNull(item.getItem().getType());
        SkullMeta skullMeta = (SkullMeta)item.getItem().getItemMeta();
        assertEquals("tastybento",skullMeta.getOwner());
        assertEquals(Material.SKULL_ITEM, item.getItem().getType());
    }

    @Test
    public void testName() {
        builder.name("test");
        PanelItem item = builder.build();
        assertEquals("test",item.getName());
    }

    @Test
    public void testDescriptionListOfString() {
        List<String> test = Arrays.asList("test line 1", "test line 2");
        builder.description(test);
        PanelItem item = builder.build();
        assertEquals(test, item.getDescription());
    }

    @Test
    public void testDescriptionStringArray() {
        List<String> test = Arrays.asList("test line 1", "test line 2", "test line 3", "test line 4");
        builder.description("test line 3", "test line 4");
        PanelItem item = builder.build();
        assertEquals(test, item.getDescription());  
    }

    @Test
    public void testDescriptionString() {
        List<String> test = Arrays.asList("test line 1", "test line 2", "test line 3", "test line 4", "test line 5");
        builder.description("test line 5");
        PanelItem item = builder.build();
        assertEquals(test, item.getDescription());  
    }

    @Test
    public void testClickHandler() {
        // Test without click handler
        PanelItem item = builder.clickHandler(null).build();
        assertFalse(item.getClickHandler().isPresent());
        
        item = builder.clickHandler(new Clicker()).build();
        assertTrue(item.getClickHandler().isPresent());
        assertTrue(item.getClickHandler().map(x -> x.onClick(null, ClickType.LEFT)).orElse(false));
    }

    public class Clicker implements PanelItem.ClickHandler {

        @Override
        public boolean onClick(User user, ClickType click) {
            
            return true;
        }
        
    }
}
