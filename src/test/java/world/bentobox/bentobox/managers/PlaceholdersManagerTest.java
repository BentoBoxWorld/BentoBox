package world.bentobox.bentobox.managers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.hooks.placeholders.MVdWPlaceholderAPIHook;
import world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook;
import world.bentobox.bentobox.lists.GameModePlaceholder;

/**
 * @author tastybento
 * @since 1.5.0
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class} )
public class PlaceholdersManagerTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private GameModeAddon addon;
    private PlaceholdersManager pm;
    @Mock
    private HooksManager hm;
    @Mock
    private PlaceholderAPIHook hook;
    @Mock
    private MVdWPlaceholderAPIHook hook2;

    @Before
    public void setUp() throws Exception {
        // Addon
        @NonNull
        AddonDescription desc = new AddonDescription.Builder("main", "bskyblock", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);

        when(plugin.getPlaceholdersManager()).thenReturn(pm);
        // No placeholders registered yet
        //when(pm.isPlaceholder(any(), any())).thenReturn(false);

        // Hooks
        when(plugin.getHooks()).thenReturn(hm);
        Optional<Hook> optionalHook = Optional.of(hook);
        when(hm.getHook(eq("PlaceholderAPI"))).thenReturn(optionalHook);
        when(hook.isPlaceholder(any(), any())).thenReturn(false);
        Optional<Hook> optionalHook2 = Optional.of(hook2);
        when(hm.getHook(eq("MVdWPlaceholderAPI"))).thenReturn(optionalHook2);

        // Placeholder manager
        pm = new PlaceholdersManager(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlaceholdersManager#registerDefaultPlaceholders(GameModeAddon)}.
     */
    @Test
    public void testRegisterGameModePlaceholdersAllDefaults() {
        pm.registerDefaultPlaceholders(addon);
        verify(hook, times(GameModePlaceholder.values().length)).registerPlaceholder(any(), anyString(), any());
        verify(hook2, times(GameModePlaceholder.values().length)).registerPlaceholder(any(), anyString(), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlaceholdersManager#registerDefaultPlaceholders(GameModeAddon)}.
     */
    @Test
    public void testRegisterDefaultPlaceholdersSomePreregistered() {
        // Some duplicates
        when(hook.isPlaceholder(any(), any())).thenReturn(false, true, true, false, false, true, false);

        pm.registerDefaultPlaceholders(addon);

        // 3 less registrations for this addon
        verify(hook, times(GameModePlaceholder.values().length - 3)).registerPlaceholder(any(), anyString(), any());
    }
}
