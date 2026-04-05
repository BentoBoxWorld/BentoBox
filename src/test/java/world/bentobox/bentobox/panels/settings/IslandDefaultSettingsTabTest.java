package world.bentobox.bentobox.panels.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Material;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;

class IslandDefaultSettingsTabTest extends CommonTestSetup {

    private IslandDefaultSettingsTab tab;
    private User user;
    @Mock
    private TabbedPanel parent;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getFlagsManager()).thenReturn(fm);
        when(parent.getIsland()).thenReturn(island);
        when(iwm.getPermissionPrefix(world)).thenReturn("bskyblock.");

        user = User.getInstance(mockPlayer);
        tab = new IslandDefaultSettingsTab(world, user);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testGetIcon() {
        PanelItem icon = tab.getIcon();
        assertNotNull(icon);
        assertEquals(Material.CRACKED_STONE_BRICKS, icon.getItem().getType());
    }

    @Test
    void testGetName() {
        assertEquals("protection.panel.ISLAND_DEFAULTS.title", tab.getName());
    }

    @Test
    void testGetPermission() {
        assertEquals("bskyblock.admin.set-world-defaults", tab.getPermission());
    }

    @Test
    void testGetPanelItems() {
        @NonNull
        List<PanelItem> items = tab.getPanelItems();
        assertTrue(items.isEmpty());
    }

    @Test
    void testGetPanelItemsWithFlags() {
        // Set up a visible PROTECTION flag
        Flag testFlag = new Flag.Builder("TEST_FLAG", Material.STONE)
                .type(Type.PROTECTION).mode(Mode.BASIC).build();
        when(fm.getFlags()).thenReturn(List.of(testFlag));
        @NonNull
        List<PanelItem> items = tab.getPanelItems();
        assertFalse(items.isEmpty());
    }
}
