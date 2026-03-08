package world.bentobox.bentobox.panels;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlaceholdersManager;

/**
 * Tests for {@link PlaceholderListPanel}.
 *
 * @since 3.2.0
 */
public class PlaceholderListPanelTest extends CommonTestSetup {

    /** Created manually so we can set a custom default Answer for getTranslation. */
    private User user;

    @Mock
    private CompositeCommand command;

    @Mock
    private PlaceholdersManager phm;

    @Mock
    private GameModeAddon addon;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Create user with a default answer that returns the translation key (first
        // String arg) for String-returning methods only. Returning a String for methods
        // with non-String return types (e.g. hasPermission -> boolean) causes
        // WrongTypeOfReturnValue during Mockito stub recording.
        user = mock(User.class, inv -> {
            Class<?> returnType = inv.getMethod().getReturnType();
            if (returnType == String.class) {
                for (Object arg : inv.getArguments()) {
                    if (arg instanceof String s && s != null) return s;
                }
                return "";
            }
            if (returnType == boolean.class || returnType == Boolean.class) return false;
            if (returnType == int.class || returnType == Integer.class) return 0;
            return null;
        });

        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getWorld()).thenReturn(world);

        User.setPlugin(plugin);
        User.getInstance(player);

        when(plugin.getDescription()).thenAnswer(
                (Answer<PluginDescriptionFile>) inv ->
                        new PluginDescriptionFile("BentoBox", "1.0", "world.bentobox.bentobox.BentoBox"));
        when(plugin.getDataFolder())
                .thenReturn(new File("src" + File.separator + "main" + File.separator + "resources"));
        when(plugin.getName()).thenReturn("BentoBox");

        when(command.getPlugin()).thenReturn(plugin);
        when(command.getAddon()).thenReturn(addon);
        when(command.getWorld()).thenReturn(world);
        when(command.getSubCommandAliases()).thenReturn(new HashMap<>());

        AddonDescription addonDescription = new AddonDescription.Builder("main", "TestAddon", "1.0").build();
        when(addon.getDescription()).thenReturn(addonDescription);

        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.getRegisteredBentoBoxPlaceholders()).thenReturn(Set.of());
        when(phm.getRegisteredPlaceholders(any(Addon.class))).thenReturn(Set.of());
        when(phm.getPlaceholderDescription(anyString())).thenReturn(Optional.empty());
        when(phm.getPlaceholderDescription(any(Addon.class), anyString())).thenReturn(Optional.empty());
        when(phm.replacePlaceholders(any(), anyString()))
                .thenAnswer((Answer<String>) inv -> inv.getArgument(1));
        when(phm.isPlaceholderEnabled(anyString())).thenReturn(true);
        when(phm.isPlaceholderEnabled(any(Addon.class), anyString())).thenReturn(true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testOpenPanelCoreNoPlaceholders() {
        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, null));
        verify(phm).getRegisteredBentoBoxPlaceholders();
    }

    @Test
    public void testOpenPanelCoreWithPlaceholders() {
        when(phm.getRegisteredBentoBoxPlaceholders())
                .thenReturn(Set.of("version", "online_players", "max_players"));
        when(phm.getPlaceholderDescription(eq("version")))
                .thenReturn(Optional.of("Current BentoBox version"));
        when(phm.getPlaceholderDescription(eq("online_players")))
                .thenReturn(Optional.of("Number of online players"));
        when(phm.getPlaceholderDescription(eq("max_players")))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, null));
        verify(phm).getRegisteredBentoBoxPlaceholders();
    }

    @Test
    public void testOpenPanelAddonNoPlaceholders() {
        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, addon));
        verify(phm).getRegisteredPlaceholders(addon);
    }

    @Test
    public void testOpenPanelAddonWithPlaceholders() {
        when(phm.getRegisteredPlaceholders(addon))
                .thenReturn(Set.of("island_level", "island_rank"));
        when(phm.getPlaceholderDescription(eq(addon), eq("island_level")))
                .thenReturn(Optional.of("The island's level"));
        when(phm.getPlaceholderDescription(eq(addon), eq("island_rank")))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, addon));
        verify(phm).getRegisteredPlaceholders(addon);
        verify(phm, never()).getPlaceholderDescription(eq("island_level"));
    }

    @Test
    public void testOpenPanelWithNumericSeries() {
        when(phm.getRegisteredBentoBoxPlaceholders())
                .thenReturn(Set.of(
                        "island_member_name_1", "island_member_name_2",
                        "island_member_name_3", "island_member_name_4",
                        "island_member_name_5"));
        when(phm.getPlaceholderDescription(anyString()))
                .thenReturn(Optional.of("Name of island member"));

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, null));
    }

    @Test
    public void testOpenPanelMixedPlaceholders() {
        when(phm.getRegisteredBentoBoxPlaceholders())
                .thenReturn(Set.of(
                        "version",
                        "island_member_name_1", "island_member_name_2",
                        "island_ban_1", "island_ban_2", "island_ban_3"));

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, null));
    }

    @Test
    public void testOpenPanelDisabledCorePlaceholder() {
        when(phm.getRegisteredBentoBoxPlaceholders()).thenReturn(Set.of("version"));
        when(phm.isPlaceholderEnabled("version")).thenReturn(false);

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, null));
        verify(phm).isPlaceholderEnabled("version");
    }

    @Test
    public void testOpenPanelDisabledAddonPlaceholder() {
        when(phm.getRegisteredPlaceholders(addon)).thenReturn(Set.of("island_level"));
        when(phm.isPlaceholderEnabled(addon, "island_level")).thenReturn(false);

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, addon));
        verify(phm).isPlaceholderEnabled(addon, "island_level");
    }

    @Test
    public void testOpenPanelLongDescription() {
        String longDesc = "This is a very long description that should definitely exceed the "
                + "thirty-eight character line width limit and therefore be wrapped across "
                + "multiple lines when rendered in the panel lore.";
        when(phm.getRegisteredBentoBoxPlaceholders()).thenReturn(Set.of("some_placeholder"));
        when(phm.getPlaceholderDescription("some_placeholder")).thenReturn(Optional.of(longDesc));

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, null));
    }

    @Test
    public void testAddonExpansionId() {
        when(phm.getRegisteredPlaceholders(addon)).thenReturn(Set.of("island_level"));

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, addon));
        verify(phm).getRegisteredPlaceholders(addon);
        verify(phm, never()).getRegisteredBentoBoxPlaceholders();
    }

    @Test
    public void testOpenPanelManyPlaceholders() {
        Set<String> keys = new java.util.LinkedHashSet<>();
        for (int i = 1; i <= 35; i++) {
            keys.add("placeholder_key_x" + i);
        }
        when(phm.getRegisteredBentoBoxPlaceholders()).thenReturn(keys);

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, null));
    }

    @Test
    public void testOpenPanelSingleNumericSuffix() {
        when(phm.getRegisteredBentoBoxPlaceholders()).thenReturn(Set.of("island_level_1"));

        assertDoesNotThrow(() -> PlaceholderListPanel.openPanel(command, user, null));
    }
}
