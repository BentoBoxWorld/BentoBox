package world.bentobox.bentobox.api.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.heads.HeadGetter;

/**
 * Tests {@link PickerPanel}.
 *
 * @author tastybento
 */
class PickerPanelTest extends CommonTestSetup {

    @Mock
    private Inventory inv;
    private MockedStatic<HeadGetter> mockedHeadGetter;
    private User user;
    private final AtomicReference<Object> picked = new AtomicReference<>();

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

    private PickerPanel<String> fruit() {
        return PickerPanel.of(user, List.of("Apple", "Banana", "Cherry", "Pineapple"),
                s -> new ItemStack(Material.APPLE), s -> s).onPick(picked::set);
    }

    @Test
    void testGetShownAndFilter() {
        assertEquals(4, fruit().getShown().size());
        assertEquals(List.of("Banana"), fruit().filter(s -> s.startsWith("B")).getShown());
    }

    @Test
    void testQueryIgnoresCaseAndSpaces() {
        PickerPanel<String> p = fruit().query("  APPLE ");
        assertEquals("APPLE", p.getQuery());
        assertEquals(List.of("Apple", "Pineapple"), p.getShown());
        assertEquals(4, p.query("").getShown().size());
    }

    @Test
    void testOpenNeedsOnPick() {
        PickerPanel<String> p = PickerPanel.of(user, List.of("a"), s -> new ItemStack(Material.STONE), s -> s);
        assertThrows(IllegalStateException.class, p::open);
    }

    @Test
    void testPickRunsActionAndCloses() {
        assertTrue(fruit().pick("Cherry"));
        assertEquals("Cherry", picked.get());
        verify(mockPlayer).closeInventory();
    }

    @Test
    void testPickRejectsFiltered() {
        assertFalse(fruit().filter(s -> !s.equals("Cherry")).pick("Cherry"));
        assertNull(picked.get());
    }

    @Test
    void testClickingAnItemPicksIt() {
        PagedPanel panel = fruit().search(false).open();
        PanelItem banana = panel.getItems().get(1);
        assertEquals("Banana", banana.getName());
        banana.getClickHandler().orElseThrow().onClick(panel, user, ClickType.LEFT, 1);
        assertEquals("Banana", picked.get());
    }

    @Test
    void testBackButton() {
        AtomicReference<Boolean> back = new AtomicReference<>(false);
        PagedPanel panel = fruit().search(false).onCancel(() -> back.set(true)).open();
        PanelItem b = panel.getItems().get(45 + PickerPanel.BACK_COLUMN);
        assertEquals("panels.picker.back.name", b.getName());
        b.getClickHandler().orElseThrow().onClick(panel, user, ClickType.LEFT, 46);
        assertTrue(back.get());
    }

    @Test
    void testSearchButtonShownWhenDialogsSupported() {
        PagedPanel panel = fruit().open();
        assertEquals("panels.picker.search.name", panel.getItems().get(45 + PickerPanel.SEARCH_COLUMN).getName());
        panel = fruit().query("app").open();
        assertEquals("panels.picker.search.clear-name",
                panel.getItems().get(45 + PickerPanel.SEARCH_COLUMN).getName());
        // Only the matches are listed
        assertEquals("Apple", panel.getItems().get(0).getName());
        assertEquals("Pineapple", panel.getItems().get(1).getName());
        assertNull(panel.getItems().get(2));
    }

    @Test
    void testInventoryClickPicks() {
        PagedPanel panel = PickerPanel.materials(user).onPick(picked::set).open();
        assertEquals("panels.picker.inventory.name", panel.getItems().get(45 + PickerPanel.HINT_COLUMN).getName());
        PanelListener listener = panel.getListener().orElseThrow();
        // Reading inventory clicks means the player's inventory must not be left to vanilla
        assertFalse(listener.allowsPlayerInventoryClicks());

        Inventory bottom = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getBottomInventory()).thenReturn(bottom);
        InventoryClickEvent e = mock(InventoryClickEvent.class);
        when(e.getView()).thenReturn(view);
        when(e.getClickedInventory()).thenReturn(bottom);
        when(e.getCurrentItem()).thenReturn(new ItemStack(Material.OAK_LOG));
        listener.onInventoryClick(user, e);
        assertEquals(Material.OAK_LOG, picked.get());
    }

    @Test
    void testInventoryClickInPanelIgnored() {
        PagedPanel panel = PickerPanel.materials(user).onPick(picked::set).open();
        PanelListener listener = panel.getListener().orElseThrow();
        InventoryView view = mock(InventoryView.class);
        Inventory bottom = mock(Inventory.class);
        when(view.getBottomInventory()).thenReturn(bottom);
        InventoryClickEvent e = mock(InventoryClickEvent.class);
        when(e.getView()).thenReturn(view);
        when(e.getClickedInventory()).thenReturn(inv);
        when(e.getCurrentItem()).thenReturn(new ItemStack(Material.OAK_LOG));
        listener.onInventoryClick(user, e);
        assertNull(picked.get());
    }

    @Test
    void testMaterials() {
        List<Material> shown = PickerPanel.materials(user).getShown();
        assertTrue(shown.contains(Material.STONE));
        assertFalse(shown.contains(Material.AIR));
        assertTrue(shown.stream().allMatch(Material::isItem));
    }

    @Test
    void testEntities() {
        List<EntityType> shown = PickerPanel.entities(user).getShown();
        assertTrue(shown.contains(EntityType.COW));
        assertFalse(shown.contains(EntityType.PLAYER));
        assertFalse(shown.contains(EntityType.ARROW));
    }

    @Test
    void testSpawnEggs() {
        assertEquals(Optional.of(Material.COW_SPAWN_EGG), PickerPanel.spawnEgg(EntityType.COW));
        assertEquals(Optional.of(EntityType.COW), PickerPanel.entityFromEgg(new ItemStack(Material.COW_SPAWN_EGG)));
        assertTrue(PickerPanel.entityFromEgg(new ItemStack(Material.STONE)).isEmpty());
    }

    @Test
    void testBiomes() {
        List<Biome> shown = PickerPanel.biomes(user).getShown();
        assertFalse(shown.isEmpty());
        assertTrue(shown.contains(Biome.PLAINS));
    }

    @Test
    void testBiomeIcons() {
        assertEquals(Material.SAND, PickerPanel.biomeIcon(Biome.DESERT));
        assertEquals(Material.LILY_PAD, PickerPanel.biomeIcon(Biome.SWAMP));
        assertEquals(Material.MANGROVE_PROPAGULE, PickerPanel.biomeIcon(Biome.MANGROVE_SWAMP));
        assertEquals(Material.NETHERRACK, PickerPanel.biomeIcon(Biome.NETHER_WASTES));
        assertEquals(Material.WATER_BUCKET, PickerPanel.biomeIcon(Biome.DEEP_OCEAN));
        assertEquals(Material.GRASS_BLOCK, PickerPanel.biomeIcon(Biome.PLAINS));
        // Every biome gets some icon
        Registry.BIOME.forEach(b -> assertNotNull(PickerPanel.biomeIcon(b)));
    }

    @Test
    void testBadRows() {
        PickerPanel<String> p = fruit();
        assertThrows(IllegalArgumentException.class, () -> p.rows(1));
    }
}
