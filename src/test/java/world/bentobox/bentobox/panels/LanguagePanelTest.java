package world.bentobox.bentobox.panels;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Panel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import net.md_5.bungee.api.ChatColor;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.BentoBoxLocale;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class LanguagePanelTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private User user;
    @Mock
    private LocalesManager lm;

    private ArrayList<Locale> localeList;

    @Mock
    private PanelBuilder pb;
    @Mock
    private Panel panel;
    @Mock
    private Inventory inv;
    @Mock
    private ItemMeta meta;

    @Captor
    private ArgumentCaptor<ItemStack> argument;

    private Map<Locale, BentoBoxLocale> map;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getLocale()).thenReturn(Locale.ENGLISH);
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
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.createInventory(any(), Mockito.anyInt(), any())).thenReturn(inv);

        // Item Factory (needed for ItemStack)
        ItemFactory itemF = mock(ItemFactory.class);
        when(itemF.getItemMeta(Mockito.any())).thenReturn(meta);
        when(Bukkit.getItemFactory()).thenReturn(itemF);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.LanguagePanel#openPanel(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testOpenPanelNoLocales() {
        LanguagePanel.openPanel(user);
        verify(plugin).getLocalesManager();
        verify(lm).getAvailableLocales(eq(true));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.LanguagePanel#openPanel(world.bentobox.bentobox.api.user.User)}.
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

        LanguagePanel.openPanel(user);
        verify(lm, times(3)).getLanguages();
        verify(bbl, times(3)).getBanner();
        verify(user).getTranslation("language.panel-title");
        // Other langs
        verify(user, times(2)).getTranslation(eq("language.description.click-to-select"));
        verify(user, times(3)).getTranslation(eq("language.description.authors"));
        // Selected language
        verify(user, Mockito.atMostOnce()).getTranslation(eq("language.description.selected"));

        verify(inv).setItem(eq(0), argument.capture());
        assertEquals(Material.WHITE_BANNER, argument.getValue().getType());
        assertEquals(1, argument.getValue().getAmount());
        assertEquals(meta, argument.getValue().getItemMeta());
        verify(meta).setDisplayName(eq(ChatColor.WHITE + "Chinese (China)"));
        verify(meta).setDisplayName(eq(ChatColor.WHITE + "English (Canada)"));
        verify(inv).setItem(eq(1), any());
        verify(inv).setItem(eq(2), any());
        verify(inv, Mockito.never()).setItem(eq(3), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.LanguagePanel#openPanel(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testOpenPanelLocalesNotNullBanner() {
        // Set up locales
        localeList.add(Locale.CANADA);
        BentoBoxLocale bbl = mock(BentoBoxLocale.class);
        map.put(Locale.CANADA, bbl);
        when(bbl.getBanner()).thenReturn(new ItemStack(Material.CYAN_BANNER));

        LanguagePanel.openPanel(user);
        verify(inv).setItem(eq(0), argument.capture());
        assertEquals(Material.CYAN_BANNER, argument.getValue().getType());
    }

}
