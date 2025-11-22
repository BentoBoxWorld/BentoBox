package world.bentobox.bentobox.api.panels.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;

public class PanelItemBuilderTest extends CommonTestSetup {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(skullMeta.getOwner()).thenReturn("tastybento");
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        mockedBukkit.when(() -> Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("tastybento");
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test
    @Disabled("Hitting item check issue")
    public void testIconMaterial() {
        PanelItemBuilder builder = new PanelItemBuilder();
        Material m = mock(Material.class);
        when(m.isItem()).thenReturn(true);
        builder.icon(m);
        PanelItem item = builder.build();
        assertNotNull(item.getItem().getType());
        assertEquals(m, item.getItem().getType());
    }

    @Test
    public void testIconItemStack() {
        PanelItemBuilder builder = new PanelItemBuilder();
        ItemStack ironOre = mock(ItemStack.class);
        when(ironOre.getType()).thenReturn(Material.IRON_ORE);
        builder.icon(ironOre);
        PanelItem item = builder.build();
        assertNotNull(item.getItem().getType());
        assertEquals(Material.IRON_ORE, item.getItem().getType());
    }

    @Test
    @Disabled("Incompatibility with Player Head not being an item")
    public void testIconString() {
        PanelItemBuilder builder = new PanelItemBuilder();
        builder.icon("tastybento");
        PanelItem item = builder.build();
        assertNotNull(item.getItem().getType());
        SkullMeta skullMeta = (SkullMeta) item.getItem().getItemMeta();
        assertEquals(null, skullMeta.getOwningPlayer());
        assertEquals(Material.PLAYER_HEAD, item.getItem().getType());
    }

    @Test
    public void testName() {
        PanelItemBuilder builder = new PanelItemBuilder();
        builder.name("test");
        PanelItem item = builder.build();
        assertEquals("test", item.getName());
    }

    @Test
    public void testDescriptionListOfString() {
        PanelItemBuilder builder = new PanelItemBuilder();
        List<String> test = Arrays.asList("test line 1", "test line 2");
        builder.description(test);
        PanelItem item = builder.build();
        assertEquals(test, item.getDescription());
    }

    @Test
    public void testDescriptionStringArray() {
        PanelItemBuilder builder = new PanelItemBuilder();
        List<String> test = Arrays.asList("test line 3", "test line 4");
        builder.description("test line 3", "test line 4");
        PanelItem item = builder.build();
        assertEquals(test, item.getDescription());
    }

    @Test
    public void testDescriptionString() {
        PanelItemBuilder builder = new PanelItemBuilder();
        List<String> test = Collections.singletonList("test line 5");
        builder.description("test line 5");
        PanelItem item = builder.build();
        assertEquals(test, item.getDescription());
    }

    @Test
    public void testClickHandler() {
        PanelItemBuilder builder = new PanelItemBuilder();
        // Test without click handler
        PanelItem item = builder.clickHandler(null).build();
        assertFalse(item.getClickHandler().isPresent());

        item = builder.clickHandler(new Clicker()).build();
        assertTrue(item.getClickHandler().isPresent());
        assertTrue(item.getClickHandler().map(x -> x.onClick(null, null, ClickType.LEFT, 0)).orElse(false));
    }

    @Test
    public void testGlow() {
        PanelItemBuilder builder = new PanelItemBuilder();
        // Test without glowing
        PanelItem item = builder.glow(false).build();
        assertFalse(item.isGlow());

        // Test with glowing
        item = builder.glow(true).build();
        assertTrue(item.isGlow());
    }

    public class Clicker implements PanelItem.ClickHandler {

        @Override
        public boolean onClick(Panel panel, User user, ClickType click, int slot) {
            return true;
        }

    }
}
