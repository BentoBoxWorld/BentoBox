package world.bentobox.bentobox.panels.customizable;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
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
@Disabled("Unfinished - needs work")
public class LanguagePanelTest extends CommonTestSetup {

    @Mock
    private User user;

    private ArrayList<Locale> localeList;

    @Mock
    private Inventory inv;
    @Mock
    private ItemMeta meta;

    @Mock
    private CompositeCommand command;

    @Captor
    private ArgumentCaptor<ItemStack> argument;

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
        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(any(World.class), any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(user.getTranslation(any(String.class), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslationOrNothing(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getLocale()).thenReturn(Locale.ENGLISH);

        when(user.getTranslation(any(World.class), eq("panels.language.buttons.language.name"), any())).
            thenAnswer((Answer<String>) invocation -> invocation.getArgument(3, String.class));

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

        // Panel
        mockedBukkit.when(() -> Bukkit.createInventory(any(), Mockito.anyInt(), anyString())).thenReturn(inv);

        // Item Factory (needed for ItemStack)
        ItemFactory itemF = mock(ItemFactory.class);
        when(itemF.getItemMeta(Mockito.any())).thenReturn(meta);
        when(Bukkit.getItemFactory()).thenReturn(itemF);

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
        verify(lm).getAvailableLocales(eq(true));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.customizable.LanguagePanel#openPanel(world.bentobox.bentobox.api.commands.CompositeCommand,world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testOpenPanelLocalesNullBanner() {
        // Set up locales
        localeList.add(Locale.CANADA);
        localeList.add(Locale.CHINA);
        localeList.add(Locale.ENGLISH);
        BentoBoxLocale bbl = mock(BentoBoxLocale.class);
        map.put(Locale.CANADA, bbl);
        map.put(Locale.CHINA, bbl);
        map.put(Locale.ENGLISH, bbl);

        LanguagePanel.openPanel(command, user);
        verify(lm, times(3)).getLanguages();
        verify(bbl, times(3)).getBanner();
        verify(user).getTranslation("panels.language.title");
        // Other langs
        verify(user, times(3)).getTranslation(eq("panels.language.buttons.language.authors"));
        verify(user, times(1)).getTranslation(eq("panels.language.buttons.language.selected"));
        verify(user, times(3)).getTranslationOrNothing(eq("panels.language.buttons.language.description"), any());
        verify(user, times(2)).getTranslation(any(World.class), eq("panels.tips.click-to-choose"));

        verify(inv).setItem(eq(0), argument.capture());
        assertEquals(Material.WHITE_BANNER, argument.getValue().getType());
        assertEquals(1, argument.getValue().getAmount());
        assertEquals(meta, argument.getValue().getItemMeta());

        verify(meta).setDisplayName(eq("Chinese (China)"));
        verify(meta).setDisplayName(eq("English (Canada)"));
        verify(inv).setItem(eq(1), any());
        verify(inv).setItem(eq(2), any());
        verify(inv, Mockito.never()).setItem(eq(3), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.customizable.LanguagePanel#openPanel(world.bentobox.bentobox.api.commands.CompositeCommand,world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testOpenPanelLocalesNotNullBanner() {
        // Set up locales
        localeList.add(Locale.CANADA);
        BentoBoxLocale bbl = mock(BentoBoxLocale.class);
        map.put(Locale.CANADA, bbl);
        ItemStack banner = mock(ItemStack.class);
        when(banner.getType()).thenReturn(Material.CYAN_BANNER);
        when(bbl.getBanner()).thenReturn(banner);

        LanguagePanel.openPanel(command, user);
        verify(inv).setItem(eq(0), argument.capture());
        assertEquals(Material.CYAN_BANNER, argument.getValue().getType());
    }

}
