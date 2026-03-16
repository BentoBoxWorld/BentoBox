package world.bentobox.bentobox.panels.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;

class SettingsTabTest extends CommonTestSetup {

    private SettingsTab tab;
    private User user;
    @Mock
    private TabbedPanel parent;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getFlagsManager()).thenReturn(fm);

        when(parent.getIsland()).thenReturn(island);

        user = User.getInstance(mockPlayer);

    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testSettingsTabWorldUserType() {
        tab = new SettingsTab(world, user, Type.PROTECTION);
    }

    @Test
    void testSettingsTabWorldUserTypeMode() {
        tab = new SettingsTab(world, user, Type.PROTECTION, Mode.ADVANCED);
    }

    @Test
    void testGetFlags() {
        testSettingsTabWorldUserTypeMode();
        tab.getFlags();
    }

    @Test
    void testGetIcon() {
        testSettingsTabWorldUserTypeMode();
        tab.getIcon();
    }

    @Test
    void testGetName() {
        testSettingsTabWorldUserTypeMode();
        assertEquals("protection.panel.PROTECTION.title", tab.getName());
    }

    @Test
    void testGetPanelItems() {
        testSettingsTabWorldUserTypeMode();
        @NonNull
        List<@Nullable PanelItem> items = tab.getPanelItems();
        assertTrue(items.isEmpty());
    }

    @Test
    void testGetTabIcons_noVisibleFlags() {
        testSettingsTabWorldUserTypeMode();
        // No flags mocked (fm.getFlags() returns empty list by default) and no island
        // so mode icon should NOT be shown
        Map<Integer, PanelItem> icons = tab.getTabIcons();
        assertTrue(icons.isEmpty());
    }

    @Test
    void testGetTabIcons_withVisibleFlags() {
        testSettingsTabWorldUserTypeMode();
        // Set up a visible PROTECTION flag so hasVisibleFlags() returns true
        Flag testFlag = new Flag.Builder("TEST_FLAG", Material.STONE)
                .type(Type.PROTECTION).mode(Mode.BASIC).build();
        when(fm.getFlags()).thenReturn(List.of(testFlag));
        when(iwm.getHiddenFlags(any())).thenReturn(List.of());
        Map<Integer, PanelItem> icons = tab.getTabIcons();
        assertFalse(icons.isEmpty());
        assertTrue(icons.containsKey(7));
    }

    @Test
    void testGetTabIcons_allFlagsHidden() {
        testSettingsTabWorldUserTypeMode();
        // Set up a PROTECTION flag that is in the hidden flags list
        Flag testFlag = new Flag.Builder("TEST_FLAG", Material.STONE)
                .type(Type.PROTECTION).mode(Mode.BASIC).build();
        when(fm.getFlags()).thenReturn(List.of(testFlag));
        when(iwm.getHiddenFlags(any())).thenReturn(List.of("TEST_FLAG"));
        Map<Integer, PanelItem> icons = tab.getTabIcons();
        // Mode icon should NOT be shown because all flags are hidden from non-op user
        assertFalse(icons.containsKey(7));
    }

    @Test
    void testGetPermission() {
        testSettingsTabWorldUserTypeMode();
        assertEquals("", tab.getPermission());
    }

    @Test
    void testGetType() {
        testSettingsTabWorldUserTypeMode();
        assertEquals(Type.PROTECTION, tab.getType());
    }

    @Test
    void testGetUser() {
        testSettingsTabWorldUserTypeMode();
        assertSame(user, tab.getUser());
    }

    @Test
    void testGetWorld() {
        testSettingsTabWorldUserTypeMode();
        assertSame(world, tab.getWorld());
    }

    @Test
    void testGetIsland() {
        testSettingsTabWorldUserTypeMode();
        assertNull(tab.getIsland());
        tab.setParentPanel(parent);
        assertSame(island, tab.getIsland());
    }

    @Test
    void testOnClick() {
        testSettingsTabWorldUserTypeMode();
        Panel panel = mock(Panel.class);
        tab.onClick(panel, user, ClickType.LEFT, 0);
    }

    @Test
    void testGetParentPanel() {
        testSettingsTabWorldUserTypeMode();

        TabbedPanel pp = tab.getParentPanel();
        assertEquals(null, pp);
    }

    @Test
    void testSetParentPanel() {
        testSettingsTabWorldUserTypeMode();
        tab.setParentPanel(parent);
    }

}
