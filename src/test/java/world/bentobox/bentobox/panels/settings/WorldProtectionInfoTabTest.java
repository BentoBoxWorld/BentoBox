package world.bentobox.bentobox.panels.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;

class WorldProtectionInfoTabTest extends CommonTestSetup {

    private WorldProtectionInfoTab tab;
    private User user;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getFlagsManager()).thenReturn(fm);

        user = User.getInstance(mockPlayer);
        tab = new WorldProtectionInfoTab(world, user);
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
        assertEquals(Material.STONE_BRICKS, icon.getItem().getType());
    }

    @Test
    void testGetName() {
        assertEquals("protection.panel.WORLD_DEFAULTS.title", tab.getName());
    }

    /**
     * Any player may see this tab - it only tells them what they can do where
     * they are standing.
     */
    @Test
    void testGetPermission() {
        assertEquals("", tab.getPermission());
    }

    @Test
    void testGetPanelItemsNoFlags() {
        assertTrue(tab.getPanelItems().isEmpty());
    }

    /**
     * The items must not be clickable: a player cannot change the world's
     * protection settings.
     */
    @Test
    void testGetPanelItemsAreNotClickable() {
        Flag testFlag = new Flag.Builder("TEST_FLAG", Material.STONE).type(Type.PROTECTION).mode(Mode.BASIC).build();
        when(fm.getFlags()).thenReturn(List.of(testFlag));

        List<PanelItem> items = tab.getPanelItems();

        assertFalse(items.isEmpty());
        items.forEach(i -> assertTrue(i.getClickHandler().isEmpty()));
    }
}
