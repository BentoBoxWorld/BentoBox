package world.bentobox.bentobox.managers;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;

import static org.mockito.Mockito.when;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class} )
public class GameModePlaceholderManagerTest {

	@Mock
    private BentoBox plugin;
	@Mock
	private GameModeAddon addon;
	@Mock
	private PlaceholdersManager pm;
	
	private GameModePlaceholderManager gpm;

	@Before
	public void setUp() throws Exception {
		gpm = new GameModePlaceholderManager(plugin);
		// Addon
		@NonNull
		AddonDescription desc = new AddonDescription.Builder("main", "bskyblock", "1.0").build();
		when(addon.getDescription()).thenReturn(desc);
		
		when(plugin.getPlaceholdersManager()).thenReturn(pm);
		// No placeholders registered yet
		when(pm.isPlaceholder(Mockito.any(), Mockito.any())).thenReturn(false);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link world.bentobox.bentobox.managers.GameModePlaceholderManager#registerGameModePlaceholders(world.bentobox.bentobox.api.addons.GameModeAddon)}.
	 */
	@Test
	public void testRegisterGameModePlaceholdersAllDefaults() {
		gpm.registerGameModePlaceholders(addon);
		// 7 registrations for this addon
		Mockito.verify(pm, Mockito.times(7)).registerPlaceholder(Mockito.anyString(), Mockito.any());
	}
	
	/**
	 * Test method for {@link world.bentobox.bentobox.managers.GameModePlaceholderManager#registerGameModePlaceholders(world.bentobox.bentobox.api.addons.GameModeAddon)}.
	 */
	@Test
	public void testRegisterGameModePlaceholdersSomePreregistered() {
		// Some duplicates
		when(pm.isPlaceholder(Mockito.any(), Mockito.any())).thenReturn(false, true, true, false, false, true, false);

		gpm.registerGameModePlaceholders(addon);
		
		// 3 registrations for this addon
		Mockito.verify(pm, Mockito.times(4)).registerPlaceholder(Mockito.anyString(), Mockito.any());
	}

}
