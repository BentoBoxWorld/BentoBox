package world.bentobox.bentobox.panels;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class BlueprintManagementPanelTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private User user;
    @Mock
    private GameModeAddon addon;
    @Mock
    BlueprintBundle bb;
    @Mock
    BlueprintBundle bb2;
    @Mock
    BlueprintBundle bb3;

    private BlueprintManagementPanel bmp;
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private Inventory inv;
    @Mock
    private Blueprint blueprint;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemFac = mock(ItemFactory.class);
        when(Bukkit.getItemFactory()).thenReturn(itemFac);
        // Panel inventory
        when(Bukkit.createInventory(any(), Mockito.anyInt(), any())).thenReturn(inv);

        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(player);
        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);
        // Bundles
        Map<String, BlueprintBundle> map = new HashMap<>();
        when(bb.getUniqueId()).thenReturn("test");
        when(bb.getDisplayName()).thenReturn("test");
        when(bb.getIcon()).thenReturn(Material.STONE);
        when(bb.getDescription()).thenReturn(Collections.singletonList("A description"));
        when(bb.getSlot()).thenReturn(5);
        // Too small slot for panel
        when(bb2.getUniqueId()).thenReturn("test2");
        when(bb2.getDisplayName()).thenReturn("test2");
        when(bb2.getIcon()).thenReturn(Material.ACACIA_BOAT);
        when(bb2.getDescription()).thenReturn(Collections.singletonList("A description 2"));
        when(bb2.getSlot()).thenReturn(-5);
        // Too large slot for panel
        when(bb3.getUniqueId()).thenReturn("test3");
        when(bb3.getDisplayName()).thenReturn("test3");
        when(bb3.getIcon()).thenReturn(Material.BAKED_POTATO);
        when(bb3.getDescription()).thenReturn(Collections.singletonList("A description 3"));
        when(bb3.getSlot()).thenReturn(65);

        map.put("test", bb);
        map.put("test2", bb2);
        map.put("test3", bb3);
        when(bpm.getBlueprintBundles(any())).thenReturn(map);

        // Blueprint
        when(blueprint.getName()).thenReturn("blueprint name");


        // Set up
        bmp = new BlueprintManagementPanel(plugin, user, addon);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#openPanel()}.
     */
    @Test
    public void testOpenPanel() {
        bmp.openPanel();
        verify(bpm).getBlueprintBundles(addon);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#openBB(world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle)}.
     */
    @Test
    public void testOpenBB() {
        bmp.openBB(bb);
        verify(bb).getDisplayName();
        verify(bb, times(3)).getBlueprint(any());
        verify(inv, times(20)).setItem(anyInt(), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getBundleIcon(world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle)}.
     */
    @Test
    public void testGetBundleIcon() {
        PanelItem pi = bmp.getBundleIcon(bb);
        assertEquals("commands.admin.blueprint.management.edit-description", pi.getName());
        assertEquals(Material.STONE, pi.getItem().getType());
        assertEquals("A description", pi.getDescription().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getBlueprintItem(world.bentobox.bentobox.api.addons.GameModeAddon, int, world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle, world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testGetBlueprintItem() {
        PanelItem pi = bmp.getBlueprintItem(addon, 0, bb, blueprint);
        assertEquals("blueprint name", pi.getName());
        assertEquals(Material.PAPER, pi.getItem().getType());
        assertEquals("commands.admin.blueprint.management.blueprint-instruction", pi.getDescription().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getBlueprintItem(world.bentobox.bentobox.api.addons.GameModeAddon, int, world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle, world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testGetBlueprintItemWithDisplayNameAndIcon() {
        when(blueprint.getDisplayName()).thenReturn("Display Name");
        when(blueprint.getIcon()).thenReturn(Material.BEACON);
        PanelItem pi = bmp.getBlueprintItem(addon, 0, bb, blueprint);
        assertEquals("Display Name", pi.getName());
        assertEquals(Material.BEACON, pi.getItem().getType());
        assertEquals("commands.admin.blueprint.management.blueprint-instruction", pi.getDescription().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.BlueprintManagementPanel#getBlueprintItem(world.bentobox.bentobox.api.addons.GameModeAddon, int, world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle, world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testGetBlueprintItemWithDisplayNameAndIconInWorldSlot() {
        when(blueprint.getDisplayName()).thenReturn("Display Name");
        when(blueprint.getIcon()).thenReturn(Material.BEACON);
        PanelItem pi = bmp.getBlueprintItem(addon, 5, bb, blueprint);
        assertEquals("Display Name", pi.getName());
        assertEquals(Material.BEACON, pi.getItem().getType());
        assertEquals("commands.admin.blueprint.management.remove", pi.getDescription().get(0));
    }

}
