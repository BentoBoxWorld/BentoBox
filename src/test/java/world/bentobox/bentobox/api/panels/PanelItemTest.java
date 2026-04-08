package world.bentobox.bentobox.api.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;

/**
 * @author tastybento
 *
 */
class PanelItemTest extends CommonTestSetup {

    @Mock
    private PanelItemBuilder pib;
    private PanelItem pi;
    @Mock
    private ClickHandler clickHandler;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Builder
        when(pib.getAmount()).thenReturn(2);
        when(pib.getClickHandler()).thenReturn(clickHandler);
        when(pib.getDescription()).thenReturn(List.of("Description", "hello"));
        ItemStack stone = mock(ItemStack.class);
        when(stone.getType()).thenReturn(Material.STONE); 
        when(pib.getIcon()).thenReturn(stone);
        when(pib.getName()).thenReturn("Name");
        when(pib.getPlayerHeadName()).thenReturn("tastybento");
        pi = new PanelItem(pib);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#empty()}.
     */
    @Test
    void testEmpty() {
        PanelItem panelItem = PanelItem.empty();
        assertTrue(panelItem.getName().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getItem()}.
     */
    @Test
    void testGetItem() {
        ItemStack i = pi.getItem();
        assertNotNull(i);
        assertEquals(Material.STONE, i.getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getDescription()}.
     */
    @Test
    void testGetDescription() {
        assertEquals(2, pi.getDescription().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setDescription(java.util.List)}.
     */
    @Test
    void testSetDescription() {
        assertEquals(2, pi.getDescription().size());
        pi.setDescription(List.of("1","2","3"));
        assertEquals(3, pi.getDescription().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getName()}.
     */
    @Test
    void testGetName() {
        assertEquals("Name", pi.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setName(java.lang.String)}.
     */
    @Test
    void testSetName() {
        assertEquals("Name", pi.getName());
        pi.setName("Name2");
        assertEquals("Name2", pi.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#isInvisible()}.
     */
    @Test
    void testIsInvisible() {
        assertFalse(pi.isInvisible());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setInvisible(boolean)}.
     */
    @Test
    void testSetInvisible() {
        assertFalse(pi.isInvisible());
        pi.setInvisible(true);
        assertTrue(pi.isInvisible());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getClickHandler()}.
     */
    @Test
    void testGetClickHandler() {
        assertEquals(clickHandler, pi.getClickHandler().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setClickHandler(world.bentobox.bentobox.api.panels.PanelItem.ClickHandler)}.
     */
    @Test
    void testSetClickHandler() {
        assertEquals(clickHandler, pi.getClickHandler().get());
        pi.setClickHandler(null);
        assertEquals(Optional.empty(), pi.getClickHandler());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#isGlow()}.
     */
    @Test
    void testIsGlow() {
        assertFalse(pi.isGlow());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setGlow(boolean)}.
     */
    @Test
    void testSetGlow() {
        assertFalse(pi.isGlow());
        pi.setGlow(true);
        assertTrue(pi.isGlow());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#isPlayerHead()}.
     */
    @Test
    void testIsPlayerHead() {
        assertTrue(pi.isPlayerHead());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getPlayerHeadName()}.
     */
    @Test
    void testGetPlayerHeadName() {
        assertEquals("tastybento", pi.getPlayerHeadName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setHead(org.bukkit.inventory.ItemStack)}.
     */
    @Test
    void testSetHead() {
        ItemStack ph = mock(ItemStack.class);
        when(ph.getType()).thenReturn(Material.PLAYER_HEAD);
        when(ph.getAmount()).thenReturn(1);
        
        ItemMeta itemMeta = mock(ItemMeta.class);
        when(ph.getItemMeta()).thenReturn(itemMeta);
        pi.setHead(ph);
        verify(itemMeta).addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        verify(itemMeta).addItemFlags(ItemFlag.HIDE_DESTROYS);
        verify(itemMeta).addItemFlags(ItemFlag.HIDE_PLACED_ON);
        verify(itemMeta).addItemFlags(ItemFlag.HIDE_ENCHANTS);
        verify(ph, times(3)).setItemMeta(itemMeta);
        // Verify TooltipDisplay is applied for additional tooltip hiding
        verify(ph).setData(eq(DataComponentTypes.TOOLTIP_DISPLAY), any(TooltipDisplay.class));
    }

    /**
     * Test that setName sets italic to false on the component to prevent
     * Minecraft's default italic rendering on item display names.
     */
    @Test
    void testSetNameDisablesDefaultItalic() {
        ItemStack itemStack = mock(ItemStack.class);
        ItemMeta itemMeta = mock(ItemMeta.class);
        when(itemStack.getType()).thenReturn(Material.STONE);
        when(itemStack.getItemMeta()).thenReturn(itemMeta);
        when(pib.getIcon()).thenReturn(itemStack);
        when(pib.getName()).thenReturn("<red>Test Name</red>");

        PanelItem item = new PanelItem(pib);

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(itemMeta).displayName(captor.capture());
        Component nameComponent = captor.getValue();
        assertEquals(TextDecoration.State.FALSE, nameComponent.decoration(TextDecoration.ITALIC));
    }

    /**
     * Test that setDescription sets italic to false on each lore component to prevent
     * Minecraft's default italic rendering on item lore.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testSetDescriptionDisablesDefaultItalic() {
        ItemStack itemStack = mock(ItemStack.class);
        ItemMeta itemMeta = mock(ItemMeta.class);
        when(itemStack.getType()).thenReturn(Material.STONE);
        when(itemStack.getItemMeta()).thenReturn(itemMeta);
        when(pib.getIcon()).thenReturn(itemStack);
        when(pib.getDescription()).thenReturn(List.of("<green>Line 1</green>", "<red>Line 2</red>"));

        PanelItem item = new PanelItem(pib);

        ArgumentCaptor<List<Component>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemMeta).lore(captor.capture());
        List<Component> loreComponents = captor.getValue();
        assertEquals(2, loreComponents.size());
        for (Component c : loreComponents) {
            assertEquals(TextDecoration.State.FALSE, c.decoration(TextDecoration.ITALIC));
        }
    }

}
