package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.addons.Addon;
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

    // ---------------------------------------------------------------
    // registerDefaultPlaceholders tests
    // ---------------------------------------------------------------

    /**
     * Test method for {@link PlaceholdersManager#registerDefaultPlaceholders(GameModeAddon)}.
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
     * Test method for {@link PlaceholdersManager#registerDefaultPlaceholders(GameModeAddon)}.
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

    // ---------------------------------------------------------------
    // registerPlaceholder (BentoBox core) tests
    // ---------------------------------------------------------------

    @Test
    void testRegisterPlaceholderBentoBox() {
        PlaceholderReplacer replacer = user -> "value";
        pm.registerPlaceholder("test_placeholder", replacer);
        verify(hook).registerPlaceholder("test_placeholder", replacer);
    }

    @Test
    void testRegisterPlaceholderBentoBoxWithDescription() {
        PlaceholderReplacer replacer = user -> "value";
        pm.registerPlaceholder("test_placeholder", "A test placeholder", replacer);
        verify(hook).registerPlaceholder("test_placeholder", "A test placeholder", replacer);
    }

    @Test
    void testRegisterPlaceholderBentoBoxNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        PlaceholderReplacer replacer = user -> "value";
        pm.registerPlaceholder("test_placeholder", replacer);
        // Should not throw, just silently do nothing
        verify(hook, never()).registerPlaceholder(anyString(), any(PlaceholderReplacer.class));
    }

    @Test
    void testRegisterPlaceholderBentoBoxWithDescriptionNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        PlaceholderReplacer replacer = user -> "value";
        pm.registerPlaceholder("test_placeholder", "desc", replacer);
        verify(hook, never()).registerPlaceholder(anyString(), anyString(), any(PlaceholderReplacer.class));
    }

    // ---------------------------------------------------------------
    // registerPlaceholder (addon) tests
    // ---------------------------------------------------------------

    @Test
    void testRegisterPlaceholderAddon() {
        PlaceholderReplacer replacer = user -> "value";
        pm.registerPlaceholder(addon, "addon_placeholder", replacer);
        verify(hook).registerPlaceholder(addon, "addon_placeholder", replacer);
    }

    @Test
    void testRegisterPlaceholderAddonWithDescription() {
        PlaceholderReplacer replacer = user -> "value";
        pm.registerPlaceholder(addon, "addon_placeholder", "An addon placeholder", replacer);
        verify(hook).registerPlaceholder(addon, "addon_placeholder", "An addon placeholder", replacer);
    }

    @Test
    void testRegisterPlaceholderAddonNullFallsToBentoBox() {
        PlaceholderReplacer replacer = user -> "value";
        pm.registerPlaceholder((Addon) null, "test_placeholder", replacer);
        // Should fall back to BentoBox registration (no addon arg)
        verify(hook).registerPlaceholder("test_placeholder", replacer);
        verify(hook, never()).registerPlaceholder(any(Addon.class), anyString(), any(PlaceholderReplacer.class));
    }

    @Test
    void testRegisterPlaceholderAddonNullWithDescriptionFallsToBentoBox() {
        PlaceholderReplacer replacer = user -> "value";
        pm.registerPlaceholder((Addon) null, "test_placeholder", "desc", replacer);
        verify(hook).registerPlaceholder("test_placeholder", "desc", replacer);
        verify(hook, never()).registerPlaceholder(any(Addon.class), anyString(), anyString(),
                any(PlaceholderReplacer.class));
    }

    // ---------------------------------------------------------------
    // unregisterPlaceholder tests
    // ---------------------------------------------------------------

    @Test
    void testUnregisterPlaceholderBentoBox() {
        pm.unregisterPlaceholder("test_placeholder");
        verify(hook).unregisterPlaceholder("test_placeholder");
    }

    @Test
    void testUnregisterPlaceholderBentoBoxNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        pm.unregisterPlaceholder("test_placeholder");
        verify(hook, never()).unregisterPlaceholder(anyString());
    }

    @Test
    void testUnregisterPlaceholderAddon() {
        pm.unregisterPlaceholder(addon, "addon_placeholder");
        verify(hook).unregisterPlaceholder(addon, "addon_placeholder");
    }

    @Test
    void testUnregisterPlaceholderAddonNullFallsToBentoBox() {
        pm.unregisterPlaceholder((Addon) null, "test_placeholder");
        verify(hook).unregisterPlaceholder("test_placeholder");
        verify(hook, never()).unregisterPlaceholder(any(Addon.class), anyString());
    }

    // ---------------------------------------------------------------
    // isPlaceholder tests
    // ---------------------------------------------------------------

    @Test
    void testIsPlaceholderTrue() {
        when(hook.isPlaceholder(addon, "existing")).thenReturn(true);
        assertTrue(pm.isPlaceholder(addon, "existing"));
    }

    @Test
    void testIsPlaceholderFalse() {
        when(hook.isPlaceholder(addon, "nonexistent")).thenReturn(false);
        assertFalse(pm.isPlaceholder(addon, "nonexistent"));
    }

    @Test
    void testIsPlaceholderNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        assertFalse(pm.isPlaceholder(addon, "any_placeholder"));
    }

    // ---------------------------------------------------------------
    // replacePlaceholders tests
    // ---------------------------------------------------------------

    @Test
    void testReplacePlaceholdersWithPlayer() {
        Player player = mock(Player.class);
        when(hook.replacePlaceholders(player, "Hello %bentobox_test%")).thenReturn("Hello World");

        assertEquals("Hello World", pm.replacePlaceholders(player, "Hello %bentobox_test%"));
        verify(hook).replacePlaceholders(player, "Hello %bentobox_test%");
    }

    @Test
    void testReplacePlaceholdersWithNullPlayer() {
        when(hook.replacePlaceholders(eq(null), anyString())).thenReturn("replaced");

        assertEquals("replaced", pm.replacePlaceholders(null, "some string"));
    }

    @Test
    void testReplacePlaceholdersNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        // Should return the original string when no hook is available
        assertEquals("Hello %bentobox_test%", pm.replacePlaceholders(null, "Hello %bentobox_test%"));
    }

    // ---------------------------------------------------------------
    // unregisterAll tests
    // ---------------------------------------------------------------

    @Test
    void testUnregisterAll() {
        pm.unregisterAll();
        verify(hook).unregisterAll();
    }

    @Test
    void testUnregisterAllNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        pm.unregisterAll();
        verify(hook, never()).unregisterAll();
    }

    // ---------------------------------------------------------------
    // getRegisteredBentoBoxPlaceholders tests
    // ---------------------------------------------------------------

    @Test
    void testGetRegisteredBentoBoxPlaceholders() {
        Set<String> expected = Set.of("placeholder1", "placeholder2");
        when(hook.getBentoBoxPlaceholders()).thenReturn(expected);

        assertEquals(expected, pm.getRegisteredBentoBoxPlaceholders());
    }

    @Test
    void testGetRegisteredBentoBoxPlaceholdersNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        assertTrue(pm.getRegisteredBentoBoxPlaceholders().isEmpty());
    }

    // ---------------------------------------------------------------
    // getRegisteredPlaceholders (addon) tests
    // ---------------------------------------------------------------

    @Test
    void testGetRegisteredPlaceholders() {
        Set<String> expected = Set.of("addon_ph1", "addon_ph2");
        when(hook.getAddonPlaceholders(addon)).thenReturn(expected);

        assertEquals(expected, pm.getRegisteredPlaceholders(addon));
    }

    @Test
    void testGetRegisteredPlaceholdersNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        assertTrue(pm.getRegisteredPlaceholders(addon).isEmpty());
    }

    // ---------------------------------------------------------------
    // getAddonsWithPlaceholders tests
    // ---------------------------------------------------------------

    @Test
    void testGetAddonsWithPlaceholders() {
        Set<Addon> expected = Set.of(addon);
        when(hook.getAddonsWithPlaceholders()).thenReturn(expected);

        assertEquals(expected, pm.getAddonsWithPlaceholders());
    }

    @Test
    void testGetAddonsWithPlaceholdersNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        assertTrue(pm.getAddonsWithPlaceholders().isEmpty());
    }

    // ---------------------------------------------------------------
    // getPlaceholderDescription tests
    // ---------------------------------------------------------------

    @Test
    void testGetPlaceholderDescriptionBentoBox() {
        when(hook.getDescription("test")).thenReturn(Optional.of("A test description"));

        Optional<String> result = pm.getPlaceholderDescription("test");
        assertTrue(result.isPresent());
        assertEquals("A test description", result.get());
    }

    @Test
    void testGetPlaceholderDescriptionBentoBoxEmpty() {
        when(hook.getDescription("unknown")).thenReturn(Optional.empty());

        assertTrue(pm.getPlaceholderDescription("unknown").isEmpty());
    }

    @Test
    void testGetPlaceholderDescriptionBentoBoxNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        assertTrue(pm.getPlaceholderDescription("test").isEmpty());
    }

    @Test
    void testGetPlaceholderDescriptionAddon() {
        when(hook.getDescription(addon, "addon_ph")).thenReturn(Optional.of("Addon placeholder desc"));

        Optional<String> result = pm.getPlaceholderDescription(addon, "addon_ph");
        assertTrue(result.isPresent());
        assertEquals("Addon placeholder desc", result.get());
    }

    @Test
    void testGetPlaceholderDescriptionAddonEmpty() {
        when(hook.getDescription(addon, "unknown")).thenReturn(Optional.empty());

        assertTrue(pm.getPlaceholderDescription(addon, "unknown").isEmpty());
    }

    @Test
    void testGetPlaceholderDescriptionAddonNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        assertTrue(pm.getPlaceholderDescription(addon, "test").isEmpty());
    }

    // ---------------------------------------------------------------
    // setPlaceholderEnabled tests
    // ---------------------------------------------------------------

    @Test
    void testSetPlaceholderEnabledBentoBox() {
        pm.setPlaceholderEnabled("test", false);
        verify(hook).setEnabled("test", false);
    }

    @Test
    void testSetPlaceholderEnabledBentoBoxTrue() {
        pm.setPlaceholderEnabled("test", true);
        verify(hook).setEnabled("test", true);
    }

    @Test
    void testSetPlaceholderEnabledBentoBoxNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        pm.setPlaceholderEnabled("test", false);
        verify(hook, never()).setEnabled(anyString(), eq(false));
    }

    @Test
    void testSetPlaceholderEnabledAddon() {
        pm.setPlaceholderEnabled(addon, "addon_ph", false);
        verify(hook).setEnabled(addon, "addon_ph", false);
    }

    @Test
    void testSetPlaceholderEnabledAddonNoHook() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        pm.setPlaceholderEnabled(addon, "addon_ph", false);
        verify(hook, never()).setEnabled(any(Addon.class), anyString(), eq(false));
    }

    // ---------------------------------------------------------------
    // isPlaceholderEnabled tests
    // ---------------------------------------------------------------

    @Test
    void testIsPlaceholderEnabledBentoBoxTrue() {
        when(hook.isEnabled("test")).thenReturn(true);
        assertTrue(pm.isPlaceholderEnabled("test"));
    }

    @Test
    void testIsPlaceholderEnabledBentoBoxFalse() {
        when(hook.isEnabled("test")).thenReturn(false);
        assertFalse(pm.isPlaceholderEnabled("test"));
    }

    @Test
    void testIsPlaceholderEnabledBentoBoxNoHookDefaultsTrue() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        // When no hook, defaults to true
        assertTrue(pm.isPlaceholderEnabled("test"));
    }

    @Test
    void testIsPlaceholderEnabledAddonTrue() {
        when(hook.isEnabled(addon, "addon_ph")).thenReturn(true);
        assertTrue(pm.isPlaceholderEnabled(addon, "addon_ph"));
    }

    @Test
    void testIsPlaceholderEnabledAddonFalse() {
        when(hook.isEnabled(addon, "addon_ph")).thenReturn(false);
        assertFalse(pm.isPlaceholderEnabled(addon, "addon_ph"));
    }

    @Test
    void testIsPlaceholderEnabledAddonNoHookDefaultsTrue() {
        when(hm.getHook("PlaceholderAPI")).thenReturn(Optional.empty());
        pm = new PlaceholdersManager(plugin);

        assertTrue(pm.isPlaceholderEnabled(addon, "addon_ph"));
    }
}
