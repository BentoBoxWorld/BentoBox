package world.bentobox.bentobox.api.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.panels.builders.PagedPanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.heads.HeadGetter;

/**
 * Tests {@link PagedPanel} and {@link PagedPanelBuilder}.
 *
 * @author tastybento
 */
class PagedPanelTest extends CommonTestSetup {

    @Mock
    private Inventory inv;
    private MockedStatic<HeadGetter> mockedHeadGetter;
    private User user;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockedBukkit.when(() -> Bukkit.createInventory(any(), anyInt(), any(net.kyori.adventure.text.Component.class)))
                .thenReturn(inv);
        when(inv.getSize()).thenReturn(54);
        mockedHeadGetter = Mockito.mockStatic(HeadGetter.class);
        user = User.getInstance(mockPlayer);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        mockedHeadGetter.closeOnDemand();
        super.tearDown();
    }

    private static PanelItem item(String name) {
        return new PanelItemBuilder().icon(Material.STONE).name(name).build();
    }

    private PagedPanelBuilder builderWith(int count) {
        PagedPanelBuilder b = new PagedPanelBuilder().user(user).name("Things");
        IntStream.range(0, count).forEach(i -> b.item(item("item" + i)));
        return b;
    }

    // ---- Builder ----

    @Test
    void testPageCount() {
        assertEquals(1, builderWith(0).getPageCount());
        assertEquals(1, builderWith(45).getPageCount());
        assertEquals(2, builderWith(46).getPageCount());
        assertEquals(2, builderWith(10).rows(2).getPageCount()); // 9 per page
        assertEquals(3, builderWith(19).rows(2).getPageCount());
    }

    @Test
    void testItemsPerPage() {
        assertEquals(45, new PagedPanelBuilder().getItemsPerPage());
        assertEquals(9, new PagedPanelBuilder().rows(2).getItemsPerPage());
    }

    @Test
    void testBadRowsAndColumnsThrow() {
        PagedPanelBuilder b = new PagedPanelBuilder();
        assertThrows(IllegalArgumentException.class, () -> b.rows(1));
        assertThrows(IllegalArgumentException.class, () -> b.rows(7));
        PanelItem i = item("x");
        assertThrows(IllegalArgumentException.class, () -> b.control(0, i));
        assertThrows(IllegalArgumentException.class, () -> b.control(8, i));
    }

    @Test
    void testPageIsClamped() {
        assertEquals(1, builderWith(50).page(9).getPage());
        assertEquals(0, builderWith(50).page(-3).getPage());
    }

    // ---- Panel ----

    @Test
    void testOpensOnFirstPageWithNextOnly() {
        PagedPanel p = builderWith(50).build();
        verify(mockPlayer).openInventory(inv);
        assertEquals(0, p.getPage());
        assertEquals(2, p.getPageCount());
        Map<Integer, PanelItem> slots = p.getItems();
        // 45 items, then the control row
        assertEquals("item0", slots.get(0).getName());
        assertEquals("item44", slots.get(44).getName());
        // No previous button: filler instead
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, slots.get(45).getItem().getType());
        // Next button
        assertEquals(Material.ARROW, slots.get(53).getItem().getType());
        assertEquals("panels.buttons.next.name", slots.get(53).getName());
    }

    @Test
    void testLastPageHasPreviousOnly() {
        PagedPanel p = builderWith(50).page(1).build();
        Map<Integer, PanelItem> slots = p.getItems();
        assertEquals("item45", slots.get(0).getName());
        assertEquals("item49", slots.get(4).getName());
        assertNull(slots.get(5));
        assertEquals(Material.ARROW, slots.get(45).getItem().getType());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, slots.get(53).getItem().getType());
    }

    @Test
    void testNextButtonTurnsThePageInPlace() {
        PagedPanel p = builderWith(100).build();
        PanelItem next = p.getItems().get(53);
        assertTrue(next.getClickHandler().orElseThrow().onClick(p, user, ClickType.LEFT, 53));
        assertEquals(1, p.getPage());
        assertEquals("item45", p.getItems().get(0).getName());
        // Refreshed in place: the inventory was created and opened only once
        mockedBukkit.verify(() -> Bukkit.createInventory(any(), anyInt(), any(net.kyori.adventure.text.Component.class)),
                times(1));
        // Middle page has both buttons
        assertEquals(Material.ARROW, p.getItems().get(45).getItem().getType());
        assertEquals(Material.ARROW, p.getItems().get(53).getItem().getType());
    }

    @Test
    void testShowPageClamps() {
        PagedPanel p = builderWith(100).build();
        p.showPage(99);
        assertEquals(2, p.getPage());
        p.showPage(-1);
        assertEquals(0, p.getPage());
    }

    @Test
    void testControlsOnEveryPage() {
        PanelItem back = item("back");
        PagedPanel p = builderWith(100).control(4, back).build();
        assertSame(back, p.getItems().get(49));
        p.showPage(1);
        assertSame(back, p.getItems().get(49));
    }

    @Test
    void testNoFiller() {
        PagedPanel p = builderWith(3).filler(null).build();
        assertEquals(3, p.getItems().size());
    }

    @Test
    void testEmptyListStillShowsControls() {
        PagedPanel p = builderWith(0).control(1, item("back")).build();
        assertFalse(p.getItems().isEmpty());
        assertEquals("back", p.getItems().get(46).getName());
    }
}
