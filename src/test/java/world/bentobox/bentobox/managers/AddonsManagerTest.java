package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { BentoBox.class} )
public class AddonsManagerTest {

    private BentoBox plugin;

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
