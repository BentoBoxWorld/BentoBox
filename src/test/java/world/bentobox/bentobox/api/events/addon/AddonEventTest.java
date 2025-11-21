package world.bentobox.bentobox.api.events.addon;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.api.addons.Addon;

/**
 * @author tastybento
 */

public class AddonEventTest extends AbstractCommonSetup {

    @Mock
    private Addon mockAddon;

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

    @Test
    public void testAddonEventBuilderWithEnableReason() {
        AddonEvent addonEvent = new AddonEvent();
        AddonBaseEvent event = addonEvent.builder().addon(mockAddon).reason(AddonEvent.Reason.ENABLE).build();

        assertTrue(event instanceof AddonEnableEvent);
        verify(pim).callEvent(event);
    }

    @Test
    public void testAddonEventBuilderWithDisableReason() {
        AddonEvent addonEvent = new AddonEvent();
        AddonBaseEvent event = addonEvent.builder().addon(mockAddon).reason(AddonEvent.Reason.DISABLE).build();

        assertTrue(event instanceof AddonDisableEvent);
        verify(pim).callEvent(event);
    }

    @Test
    public void testAddonEventBuilderWithLoadReason() {
        AddonEvent addonEvent = new AddonEvent();
        AddonBaseEvent event = addonEvent.builder().addon(mockAddon).reason(AddonEvent.Reason.LOAD).build();

        assertTrue(event instanceof AddonLoadEvent);
        verify(pim).callEvent(event);
    }

    @Test
    public void testAddonEventBuilderWithUnknownReason() {
        AddonEvent addonEvent = new AddonEvent();
        AddonBaseEvent event = addonEvent.builder().addon(mockAddon).build(); // Default reason is UNKNOWN

        assertTrue(event instanceof AddonGeneralEvent);
        verify(pim).callEvent(event);
    }

    // Add more tests for other aspects like testing keyValues map, etc.
}
