package world.bentobox.bentobox.listeners.flags.clicklisteners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab.EntityLimitTabType;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class GeoMobLimitTabTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private TabbedPanel panel;
    @Mock
    private @NonNull Inventory inv;
    private List<String> list;
    @Mock
    private GameModeAddon gma;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
         // IWM
        when(iwm.getAddon(any())).thenReturn(Optional.of(gma));
        // Make  list of the first 4 creatures on the list - it's alphabetical and follows the list of Living Entities
        list = new ArrayList<>();
        list.add("ARMADILLO");
        list.add("AXOLOTL");
        list.add("BAT");
        list.add("COW");
        when(iwm.getMobLimitSettings(any())).thenReturn(list);
        when(iwm.getGeoLimitSettings(any())).thenReturn(list);
        // Panel
        when(panel.getInventory()).thenReturn(inv);
        // User
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        // Util
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClick() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        // ARMADILLO, AXOLOTL, BAT, and COW in list
        assertEquals(4, list.size());
        assertEquals("COW", list.get(3));
        assertEquals("BAT", list.get(2));
        assertEquals("AXOLOTL", list.get(1));
        assertEquals("ARMADILLO", list.getFirst());

        // Click on ARMADILLO
        tab.onClick(panel, user, ClickType.LEFT, 10);
        list.forEach(System.out::println);
        assertEquals(3, list.size());
        assertEquals("COW", list.get(2));
        assertEquals("BAT", list.get(1));
        assertEquals("AXOLOTL", list.getFirst());
        // Click on ARMADILLO again to have it added to the end of the list
        tab.onClick(panel, user, ClickType.LEFT, 10);
        assertEquals(4, list.size());
        assertEquals("COW", list.get(2));
        assertEquals("BAT", list.get(1));
        assertEquals("AXOLOTL", list.getFirst());
        assertEquals("ARMADILLO", list.get(3));
        verify(gma, times(2)).saveWorldSettings();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getIcon()}.
     */
    @Test
    public void testGetIcon() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.MOB_LIMIT, world);
        PanelItem icon = tab.getIcon();
        assertEquals("protection.flags.LIMIT_MOBS.name", icon.getName());
        assertEquals(Material.IRON_BOOTS, icon.getItem().getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getIcon()}.
     */
    @Test
    public void testGetIconGeoLimit() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        PanelItem icon = tab.getIcon();
        assertEquals("protection.flags.GEO_LIMIT_MOBS.name", icon.getName());
        assertEquals(Material.CHAINMAIL_CHESTPLATE, icon.getItem().getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getName()}.
     */
    @Test
    public void testGetName() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.MOB_LIMIT, world);
        assertEquals("protection.flags.LIMIT_MOBS.name", tab.getName());
        tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        assertEquals("protection.flags.GEO_LIMIT_MOBS.name", tab.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getPanelItems()}.
     */
    @Test
    public void testGetPanelItemsMobLimit() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.MOB_LIMIT, world);
        List<@Nullable PanelItem> items = tab.getPanelItems();
        assertFalse(items.isEmpty());
        items.forEach(i -> {
            if (i.getName().equals("Armadillo") || i.getName().equals("Axolotl") || i.getName().equals("Cow")
                    || i.getName().equals("Bat")) {
                assertEquals(Material.RED_SHULKER_BOX, i.getItem().getType(), "Name : " + i.getName());
            } else {
                assertEquals(Material.GREEN_SHULKER_BOX, i.getItem().getType(), "Name : " + i.getName());
            }
        });
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getPanelItems()}.
     */
    @Test
    public void testGetPanelItemsGeoLimit() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        List<@Nullable PanelItem> items = tab.getPanelItems();
        assertFalse(items.isEmpty());
        items.forEach(i -> {
            if (i.getName().equals("Armadillo") || i.getName().equals("Axolotl") || i.getName().equals("Cow")
                    || i.getName().equals("Bat")) {
                assertEquals(Material.GREEN_SHULKER_BOX, i.getItem().getType(), "Name : " + i.getName());
            } else {
                assertEquals(Material.RED_SHULKER_BOX, i.getItem().getType(), "Name : " + i.getName());
            }
        });
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getPermission()}.
     */
    @Test
    public void testGetPermission() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        assertTrue(tab.getPermission().isEmpty());
    }

}
