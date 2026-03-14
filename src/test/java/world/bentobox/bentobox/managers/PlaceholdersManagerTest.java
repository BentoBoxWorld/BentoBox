package world.bentobox.bentobox.managers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook;
import world.bentobox.bentobox.lists.GameModePlaceholder;

/**
 * @author tastybento
 * @since 1.5.0
 */
class PlaceholdersManagerTest extends CommonTestSetup {

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


        // Hooks
        when(plugin.getHooks()).thenReturn(hm);
        Optional<Hook> optionalHook = Optional.of(hook);
        when(hm.getHook("PlaceholderAPI")).thenReturn(optionalHook);
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
    void testRegisterGameModePlaceholdersAllDefaults() {
        pm.registerDefaultPlaceholders(addon);
        // + 304 because we register team member placeholders up to 50 members (6 per member * 50 = 300) + 4 extra
        // All registrations now use the 4-arg overload (addon, placeholder, description, replacer)
        verify(hook, times(GameModePlaceholder.values().length + 304))
                .registerPlaceholder(any(GameModeAddon.class), anyString(), anyString(), any(PlaceholderReplacer.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlaceholdersManager#registerDefaultPlaceholders(GameModeAddon)}.
     */
    @Test
    void testRegisterDefaultPlaceholdersSomePreregistered() {
        // Some duplicates
        when(hook.isPlaceholder(any(), any())).thenReturn(false, true, true, false, false, true, false);

        pm.registerDefaultPlaceholders(addon);

        // 3 less registrations for this addon (3 GameModePlaceholder values pre-registered)
        verify(hook, times(GameModePlaceholder.values().length - 3 + 304))
                .registerPlaceholder(any(GameModeAddon.class), anyString(), anyString(), any(PlaceholderReplacer.class));
    }
}
