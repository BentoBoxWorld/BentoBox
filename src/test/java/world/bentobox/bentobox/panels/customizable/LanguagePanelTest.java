package world.bentobox.bentobox.panels.customizable;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.BentoBoxLocale;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
public class LanguagePanelTest extends CommonTestSetup {

    @Mock
    private User user;

    private ArrayList<Locale> localeList;

    @Mock
    private CompositeCommand command;

    private Map<Locale, BentoBoxLocale> map;

    /**
     * Location of the resources folder
     */
    private final Path resourcePath = Paths.get("src","test","resources");

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(anyString())).thenReturn(true);
        // Set up translation mocks - more specific ones last
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(any(World.class), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(user.getTranslation(any(World.class), anyString(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(user.getTranslation(any(World.class), eq("panels.language.buttons.language.name"), any())).
            thenAnswer((Answer<String>) invocation -> invocation.getArgument(3, String.class));
        // getTranslationOrNothing should return empty string - it's mocked since user is a mock
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString(), anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString(), anyString(), anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("");
        when(user.getLocale()).thenReturn(Locale.ENGLISH);

        GameModeAddon addon = mock(GameModeAddon.class);
        when(command.getAddon()).thenReturn(addon);
        when(command.getPlugin()).thenReturn(plugin);
        when(addon.getDataFolder()).thenReturn(resourcePath.toFile());

        World world = mock(World.class);
        when(command.getWorld()).thenReturn(world);

        when(plugin.getDescription()).thenAnswer((Answer<PluginDescriptionFile>) invocation ->
            new PluginDescriptionFile("BentoBox", "1.0", "world.bentobox.bentobox"));

        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(player);


        // Locales Manager
        when(plugin.getLocalesManager()).thenReturn(lm);
        localeList = new ArrayList<>();
        when(lm.getAvailableLocales(any(Boolean.class))).thenReturn(localeList);
        map = new HashMap<>();
        when(lm.getLanguages()).thenReturn(map);

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.customizable.LanguagePanel#openPanel(world.bentobox.bentobox.api.commands.CompositeCommand,world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testOpenPanelNoLocales() {
        LanguagePanel.openPanel(command, user);
        verify(plugin).getLocalesManager();
        verify(lm).getAvailableLocales(true);
        // Verify error was logged
        verify(plugin).logError("There are no available locales for selection!");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.customizable.LanguagePanel#LanguagePanel(world.bentobox.bentobox.api.commands.CompositeCommand, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testConstructor() {
        // Set up locales
        localeList.add(Locale.CANADA);
        localeList.add(Locale.CHINA);
        localeList.add(Locale.ENGLISH);

        assertDoesNotThrow(() -> new LanguagePanel(command, user));
    }

    /**
     * Test method to verify panel creation with locales
     */
    @Test
    public void testLanguagePanelWithLocales() {
        // Set up locales
        localeList.add(Locale.CANADA);
        localeList.add(Locale.ENGLISH);

        BentoBoxLocale bblCanada = mock(BentoBoxLocale.class);
        when(bblCanada.getAuthors()).thenReturn(new ArrayList<>());

        BentoBoxLocale bblEnglish = mock(BentoBoxLocale.class);
        when(bblEnglish.getAuthors()).thenReturn(new ArrayList<>());

        map.put(Locale.CANADA, bblCanada);
        map.put(Locale.ENGLISH, bblEnglish);

        assertDoesNotThrow(() -> new LanguagePanel(command, user));
    }

}



