package world.bentobox.bentobox.listeners.flags.clicklisteners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab.EntityLimitTabType;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@Ignore("Needs update to work with PaperAPI")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class, ServerBuildInfo.class})
public class GeoMobLimitTabTest {

    @Mock
    private User user;
    @Mock
    private World world;
    @Mock
    private TabbedPanel panel;
    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private @NonNull Inventory inv;
    private List<String> list;
    @Mock
    private GameModeAddon gma;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
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
        PowerMockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS);
        when(Util.getWorld(any())).thenReturn(world);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
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
        assertEquals("ARMADILLO", list.get(0));

        // Click on ARMADILLO
        tab.onClick(panel, user, ClickType.LEFT, 10);
        list.forEach(System.out::println);
        assertEquals(3, list.size());
        assertEquals("COW", list.get(2));
        assertEquals("BAT", list.get(1));
        assertEquals("AXOLOTL", list.get(0));
        // Click on ARMADILLO again to have it added to the end of the list
        tab.onClick(panel, user, ClickType.LEFT, 10);
        assertEquals(4, list.size());
        assertEquals("COW", list.get(2));
        assertEquals("BAT", list.get(1));
        assertEquals("AXOLOTL", list.get(0));
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
                assertEquals("Name : " + i.getName(), Material.RED_SHULKER_BOX, i.getItem().getType());
            } else {
                assertEquals("Name : " + i.getName(), Material.GREEN_SHULKER_BOX, i.getItem().getType());
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
                assertEquals("Name : " + i.getName(), Material.GREEN_SHULKER_BOX, i.getItem().getType());
            } else {
                assertEquals("Name : " + i.getName(), Material.RED_SHULKER_BOX, i.getItem().getType());
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
