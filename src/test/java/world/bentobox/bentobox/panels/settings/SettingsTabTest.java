package world.bentobox.bentobox.panels.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Flags.class, Util.class, Bukkit.class, IslandsManager.class, ServerBuildInfo.class })
public class SettingsTabTest extends AbstractCommonSetup {

    private SettingsTab tab;
    private User user;
    @Mock
    private TabbedPanel parent;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getFlagsManager()).thenReturn(fm);

        when(parent.getIsland()).thenReturn(island);

        user = User.getInstance(mockPlayer);

    }

    @Test
    public void testSettingsTabWorldUserType() {
        tab = new SettingsTab(world, user, Type.PROTECTION);
    }

    @Test
    public void testSettingsTabWorldUserTypeMode() {
        tab = new SettingsTab(world, user, Type.PROTECTION, Mode.ADVANCED);
    }

    @Test
    public void testGetFlags() {
        testSettingsTabWorldUserTypeMode();
        tab.getFlags();
    }

    @Ignore("Issue with Materials and item checking")
    @Test
    public void testGetIcon() {
        testSettingsTabWorldUserTypeMode();
        PanelItem icon = tab.getIcon();
    }

    @Test
    public void testGetName() {
        testSettingsTabWorldUserTypeMode();
        assertEquals("protection.panel.PROTECTION.title", tab.getName());
    }

    @Test
    public void testGetPanelItems() {
        testSettingsTabWorldUserTypeMode();
        @NonNull
        List<@Nullable PanelItem> items = tab.getPanelItems();
        assertTrue(items.isEmpty());
    }

    @Ignore("Issue with Materials and item checking")
    @Test
    public void testGetTabIcons() {
        testSettingsTabWorldUserTypeMode();
        Map<Integer, PanelItem> icons = tab.getTabIcons();
        assertTrue(icons.isEmpty());
    }

    @Test
    public void testGetPermission() {
        testSettingsTabWorldUserTypeMode();
        assertEquals("", tab.getPermission());
    }

    @Test
    public void testGetType() {
        testSettingsTabWorldUserTypeMode();
        assertEquals(Type.PROTECTION, tab.getType());
    }

    @Test
    public void testGetUser() {
        testSettingsTabWorldUserTypeMode();
        assertEquals(user, tab.getUser());
    }

    @Test
    public void testGetWorld() {
        testSettingsTabWorldUserTypeMode();
        assertEquals(world, tab.getWorld());
    }

    @Test
    public void testGetIsland() {
        testSettingsTabWorldUserTypeMode();
        assertEquals(null, tab.getIsland());
        tab.setParentPanel(parent);
        assertEquals(island, tab.getIsland());
    }

    @Test
    public void testOnClick() {
        testSettingsTabWorldUserTypeMode();
        Panel panel = mock(Panel.class);
        tab.onClick(panel, user, ClickType.LEFT, 0);
    }

    @Test
    public void testGetParentPanel() {
        testSettingsTabWorldUserTypeMode();
        TabbedPanel pp = tab.getParentPanel();
        assertEquals(pp, parent);
    }

    @Test
    public void testSetParentPanel() {
        testSettingsTabWorldUserTypeMode();
        tab.setParentPanel(parent);
    }

}
