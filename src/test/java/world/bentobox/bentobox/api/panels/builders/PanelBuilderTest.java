/**
 * 
 */
package world.bentobox.bentobox.api.panels.builders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class })
public class PanelBuilderTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class);
        Inventory inv = mock(Inventory.class);
        when(Bukkit.createInventory(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(inv);
        
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#name(java.lang.String)}.
     */
    @Test
    public void testName() {
        PanelBuilder pb = new PanelBuilder();
        assertTrue(pb.name("test") instanceof PanelBuilder);    
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#item(world.bentobox.bentobox.api.panels.PanelItem)}.
     */
    @Test
    public void testItemPanelItem() {
        PanelItem pi = mock(PanelItem.class);
        PanelBuilder pb = new PanelBuilder();
        pb = pb.item(pi);
        // Add multiple items
        pb = pb.item(pi);
        pb = pb.item(pi);
        pb = pb.item(pi);
        
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#item(int, world.bentobox.bentobox.api.panels.PanelItem)}.
     */
    @Test
    public void testItemIntPanelItem() {
        PanelItem pi = mock(PanelItem.class);
        PanelBuilder pb = new PanelBuilder();
        pb = pb.item(0, pi);
        // Add multiple items
        pb = pb.item(1, pi);
        pb = pb.item(10, pi);
        pb = pb.item(20, pi);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#size(int)}.
     */
    @Test
    public void testSize() {
        PanelBuilder pb = new PanelBuilder();
        pb = pb.size(45);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#user(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testUser() {
        User user = mock(User.class);
        PanelBuilder pb = new PanelBuilder();
        pb = pb.user(user);
        // Change user
        User user2 = mock(User.class);
        pb = pb.user(user2);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#listener(world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    public void testListener() {
        PanelBuilder pb = new PanelBuilder();
        PanelListener listener = mock(PanelListener.class);
        pb = pb.listener(listener);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#nextSlot()}.
     */
    @Test
    public void testNextSlot() {
        PanelItem pi = mock(PanelItem.class);
        PanelBuilder pb = new PanelBuilder();
        assertEquals(0, pb.nextSlot());
        pb = pb.item(0, pi);
        assertEquals(1, pb.nextSlot());
        // Add multiple items
        pb = pb.item(1, pi);
        assertEquals(2, pb.nextSlot());
        pb = pb.item(10, pi);
        assertEquals(11, pb.nextSlot());
        pb = pb.item(20, pi);
        assertEquals(21, pb.nextSlot());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#slotOccupied(int)}.
     */
    @Test
    public void testSlotOccupied() {
        PanelItem pi = mock(PanelItem.class);
        PanelBuilder pb = new PanelBuilder();
        assertEquals(0, pb.nextSlot());
        assertFalse(pb.slotOccupied(0));
        pb = pb.item(0, pi);
        assertTrue(pb.slotOccupied(0));
        
        // Add multiple items
        pb = pb.item(1, pi);
        assertTrue(pb.slotOccupied(1));
        pb = pb.item(10, pi);
        assertTrue(pb.slotOccupied(10));
        pb = pb.item(20, pi);
        assertTrue(pb.slotOccupied(20));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#build()}.
     */
    @Test
    public void testBuild() {
        PanelBuilder pb = new PanelBuilder();
        pb.build();
     }

}
