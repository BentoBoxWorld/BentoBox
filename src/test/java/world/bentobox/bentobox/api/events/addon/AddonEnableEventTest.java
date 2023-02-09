package world.bentobox.bentobox.api.events.addon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.api.addons.Addon;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class AddonEnableEventTest {

    private AddonEnableEvent aee;
    @Mock
    private Addon addon;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        Map<String, Object> map = new HashMap<>();
        aee = new AddonEnableEvent(addon, map);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.addon.AddonEnableEvent#getHandlers()}.
     */
    @Test
    public void testGetHandlers() {
        assertNotNull(aee.getHandlers());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.addon.AddonEnableEvent#getHandlerList()}.
     */
    @Test
    public void testGetHandlerList() {
        assertNotNull(AddonEnableEvent.getHandlerList());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.addon.AddonEnableEvent#AddonEnableEvent(world.bentobox.bentobox.api.addons.Addon, java.util.Map)}.
     */
    @Test
    public void testAddonEnableEvent() {
        assertNotNull(aee);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.addon.AddonBaseEvent#getKeyValues()}.
     */
    @Test
    public void testGetKeyValues() {
        assertTrue(aee.getKeyValues().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.addon.AddonBaseEvent#getAddon()}.
     */
    @Test
    public void testGetAddon() {
        assertEquals(addon, aee.getAddon());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.addon.AddonBaseEvent#getNewEvent()}.
     */
    @Test
    public void testGetNewEvent() {
        assertTrue(aee.getNewEvent().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.addon.AddonBaseEvent#setNewEvent(world.bentobox.bentobox.api.events.addon.AddonBaseEvent)}.
     */
    @Test
    public void testSetNewEvent() {
        aee.setNewEvent(aee);
        assertEquals(aee, aee.getNewEvent().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.events.BentoBoxEvent#setKeyValues(java.util.Map)}.
     */
    @Test
    @Ignore
    public void testSetKeyValues() {
        // No fields to set values for in the class
    }

}
