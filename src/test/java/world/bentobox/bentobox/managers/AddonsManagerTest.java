package world.bentobox.bentobox.managers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import world.bentobox.bentobox.BentoBox;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { BentoBox.class} )
public class AddonsManagerTest {

    private static BentoBox plugin;

    @Before
    public void setup() {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
    }

    @Test
    public void testConstructor() {
        AddonsManager addonsManager = new AddonsManager(plugin);

        assertNotNull(addonsManager.getAddons());
    }


}
