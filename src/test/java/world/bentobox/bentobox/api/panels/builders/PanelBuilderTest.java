package world.bentobox.bentobox.api.panels.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
public class PanelBuilderTest extends CommonTestSetup {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
        assertTrue(pb.slotOccupied(3));
        assertFalse(pb.slotOccupied(4));
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
        assertTrue(pb.slotOccupied(0));
        assertTrue(pb.slotOccupied(1));
        assertTrue(pb.slotOccupied(10));
        assertTrue(pb.slotOccupied(20));
        assertFalse(pb.slotOccupied(4));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#size(int)}.
     */
    @Test
    public void testSize() {
        PanelBuilder pb = new PanelBuilder();
        assertEquals(pb, pb.size(45));
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
        assertEquals(pb, pb.user(user2));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.builders.PanelBuilder#listener(world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    public void testListener() {
        PanelBuilder pb = new PanelBuilder();
        PanelListener listener = mock(PanelListener.class);
        assertEquals(pb, pb.listener(listener));
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
        pb.name("test");
        Panel p = pb.build();
        assertEquals("test", p.getName());
    }

}
