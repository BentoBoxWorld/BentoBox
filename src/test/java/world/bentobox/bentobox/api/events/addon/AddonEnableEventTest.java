package world.bentobox.bentobox.api.events.addon;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.api.addons.Addon;

/**
 * @author tastybento
 *
 */
public class AddonEnableEventTest extends AbstractCommonSetup {

    private AddonEnableEvent aee;
    @Mock
    private Addon addon;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Map<String, Object> map = new HashMap<>();
        aee = new AddonEnableEvent(addon, map);
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
    @Disabled
    public void testSetKeyValues() {
        // No fields to set values for in the class
    }

}
