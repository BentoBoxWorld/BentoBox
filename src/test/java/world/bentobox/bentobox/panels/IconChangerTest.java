package world.bentobox.bentobox.panels;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;

/**
 * Tests for {@link IconChanger}.
 */
class IconChangerTest extends CommonTestSetup {

    @Mock
    private GameModeAddon addon;
    @Mock
    private BlueprintManagementPanel bmp;
    @Mock
    private BlueprintBundle bb;
    @Mock
    private BlueprintsManager bpManager;
    @Mock
    private User user;
    @Mock
    private InventoryClickEvent event;

    private IconChanger iconChanger;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getBlueprintsManager()).thenReturn(bpManager);
        when(bmp.getSelected()).thenReturn(null); // no blueprint selected by default
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getLocation()).thenReturn(location);
        iconChanger = new IconChanger(plugin, addon, bmp, bb);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Clicking a plain item (no item model) in the player inventory sets the bundle icon by Material.
     */
    @Test
    void testOnInventoryClickBundlePlainMaterial() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        ItemMeta meta = mock(ItemMeta.class);
        when(meta.hasItemModel()).thenReturn(false);
        when(item.getItemMeta()).thenReturn(meta);

        when(event.getCurrentItem()).thenReturn(item);
        when(event.getRawSlot()).thenReturn(45); // player inventory slot

        iconChanger.onInventoryClick(user, event);

        verify(bb).setIcon(Material.STONE);
        verify(bb, never()).setIcon(any(String.class));
        verify(bpManager).saveBlueprintBundle(addon, bb);
    }

    /**
     * Clicking an item that has a custom item model sets the bundle icon by model key string.
     */
    @Test
    void testOnInventoryClickBundleItemModel() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PAPER);
        ItemMeta meta = mock(ItemMeta.class);
        NamespacedKey modelKey = new NamespacedKey("myserver", "island_tropical");
        when(meta.hasItemModel()).thenReturn(true);
        when(meta.getItemModel()).thenReturn(modelKey);
        when(item.getItemMeta()).thenReturn(meta);

        when(event.getCurrentItem()).thenReturn(item);
        when(event.getRawSlot()).thenReturn(45);

        iconChanger.onInventoryClick(user, event);

        verify(bb).setIcon("myserver:island_tropical");
        verify(bb, never()).setIcon(any(Material.class));
        verify(bpManager).saveBlueprintBundle(addon, bb);
    }

    /**
     * Clicking a plain item when a blueprint is selected changes the blueprint icon, not the bundle.
     */
    @Test
    void testOnInventoryClickBlueprintSelected() {
        Blueprint bp = mock(Blueprint.class);
        when(bmp.getSelected()).thenReturn(new AbstractMap.SimpleEntry<>(1, bp));

        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.BEACON);
        when(event.getCurrentItem()).thenReturn(item);
        when(event.getRawSlot()).thenReturn(45);

        iconChanger.onInventoryClick(user, event);

        verify(bp).setIcon(Material.BEACON);
        verify(bpManager).saveBlueprint(addon, bp);
        verify(bb, never()).setIcon(any(Material.class));
        verify(bb, never()).setIcon(any(String.class));
    }

    /**
     * Clicking inside the panel (slot ≤ 44) does nothing.
     */
    @Test
    void testOnInventoryClickInsidePanel() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(event.getCurrentItem()).thenReturn(item);
        when(event.getRawSlot()).thenReturn(10); // inside the GUI

        iconChanger.onInventoryClick(user, event);

        verify(bb, never()).setIcon(any(Material.class));
        verify(bb, never()).setIcon(any(String.class));
        verify(bpManager, never()).saveBlueprintBundle(any(), any());
    }

    /**
     * Clicking an AIR slot does nothing.
     */
    @Test
    void testOnInventoryClickAirItem() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.AIR);
        when(event.getCurrentItem()).thenReturn(item);
        when(event.getRawSlot()).thenReturn(45);

        iconChanger.onInventoryClick(user, event);

        verify(bb, never()).setIcon(any(Material.class));
        verify(bb, never()).setIcon(any(String.class));
        verify(bpManager, never()).saveBlueprintBundle(any(), any());
    }

    /**
     * Clicking a null item does nothing.
     */
    @Test
    void testOnInventoryClickNullItem() {
        when(event.getCurrentItem()).thenReturn(null);
        when(event.getRawSlot()).thenReturn(45);

        iconChanger.onInventoryClick(user, event);

        verify(bb, never()).setIcon(any(Material.class));
        verify(bb, never()).setIcon(any(String.class));
        verify(bpManager, never()).saveBlueprintBundle(any(), any());
    }
}
