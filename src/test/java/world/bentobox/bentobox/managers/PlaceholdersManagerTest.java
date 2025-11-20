package world.bentobox.bentobox.managers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook;
import world.bentobox.bentobox.lists.GameModePlaceholder;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class PlaceholdersManagerTest extends AbstractCommonSetup {

    @Mock
    private GameModeAddon addon;
    private PlaceholdersManager pm;
    @Mock
    private HooksManager hm;
    @Mock
    private PlaceholderAPIHook hook;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

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

        // World settings

        @NonNull
        WorldSettings ws = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        when(plugin.getIWM()).thenReturn(iwm);

        // Placeholder manager
        pm = new PlaceholdersManager(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlaceholdersManager#registerDefaultPlaceholders(GameModeAddon)}.
     */
    @Test
    public void testRegisterGameModePlaceholdersAllDefaults() {
        pm.registerDefaultPlaceholders(addon);
        // + 300 because we register team member placeholders up to 50 members
        verify(hook, times(GameModePlaceholder.values().length + 304)).registerPlaceholder(any(), anyString(), any());
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
        verify(hook, times(GameModePlaceholder.values().length - 3 + 304)).registerPlaceholder(any(), anyString(),
                any());
    }
}
