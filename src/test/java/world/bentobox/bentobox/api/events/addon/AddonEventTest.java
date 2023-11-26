package world.bentobox.bentobox.api.events.addon;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.api.addons.Addon;

/**
 * @author tastybento
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class })
public class AddonEventTest {

    @Mock
    private Addon mockAddon;

    @Mock
    private PluginManager mockPluginManager;

    @Before
    public void setUp() {
	PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
	when(Bukkit.getPluginManager()).thenReturn(mockPluginManager);
    }

    @Test
    public void testAddonEventBuilderWithEnableReason() {
	AddonEvent addonEvent = new AddonEvent();
	AddonBaseEvent event = addonEvent.builder().addon(mockAddon).reason(AddonEvent.Reason.ENABLE).build();

	assertTrue(event instanceof AddonEnableEvent);
	verify(mockPluginManager).callEvent(event);
    }

    @Test
    public void testAddonEventBuilderWithDisableReason() {
	AddonEvent addonEvent = new AddonEvent();
	AddonBaseEvent event = addonEvent.builder().addon(mockAddon).reason(AddonEvent.Reason.DISABLE).build();

	assertTrue(event instanceof AddonDisableEvent);
	verify(mockPluginManager).callEvent(event);
    }

    @Test
    public void testAddonEventBuilderWithLoadReason() {
	AddonEvent addonEvent = new AddonEvent();
	AddonBaseEvent event = addonEvent.builder().addon(mockAddon).reason(AddonEvent.Reason.LOAD).build();

	assertTrue(event instanceof AddonLoadEvent);
	verify(mockPluginManager).callEvent(event);
    }

    @Test
    public void testAddonEventBuilderWithUnknownReason() {
	AddonEvent addonEvent = new AddonEvent();
	AddonBaseEvent event = addonEvent.builder().addon(mockAddon).build(); // Default reason is UNKNOWN

	assertTrue(event instanceof AddonGeneralEvent);
	verify(mockPluginManager).callEvent(event);
    }

    // Add more tests for other aspects like testing keyValues map, etc.
}
