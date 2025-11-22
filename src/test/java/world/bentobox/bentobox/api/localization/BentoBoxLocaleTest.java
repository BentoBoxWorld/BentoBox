package world.bentobox.bentobox.api.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.util.ItemParser;

/**
 * Tests BentoBoxLocale class
 * @author tastybento
 *
 */
public class BentoBoxLocaleTest extends CommonTestSetup {

    private BentoBoxLocale localeObject;
    //private BannerMeta bannerMeta;
    @Mock
    private ItemStack  banner;
   private  MockedStatic<ItemParser> mockedItemParser;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        
        mockedItemParser = Mockito.mockStatic(ItemParser.class, Mockito.RETURNS_MOCKS);
        when(banner.getType()).thenReturn(Material.WHITE_BANNER);
        mockedItemParser.when(() -> ItemParser.parse(anyString())).thenReturn(banner);
        /*
        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        bannerMeta = mock(BannerMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        when(itemFactory.createItemStack(any())).thenThrow(IllegalArgumentException.class);
        mockedBukkit.when(() -> Bukkit.getItemFactory()).thenReturn(itemFactory);
*/
        Locale locale = Locale.US;
        YamlConfiguration config = new YamlConfiguration();
        config.set("meta.banner", "WHITE_BANNER:1:SMALL_STRIPES:RED:SQUARE_TOP_RIGHT:CYAN:SQUARE_TOP_RIGHT:BLUE");
        List<String> authors = new ArrayList<>();
        authors.add("tastybento");
        authors.add("tastybento2");
        config.set("meta.authors", authors );
        config.set("reference.to.test", "test result");
        localeObject = new BentoBoxLocale(locale, config);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.localization.BentoBoxLocale#get(java.lang.String)}.
     */
    @Test
    public void testGet() {
        assertEquals("test result", localeObject.get("reference.to.test"));
        assertEquals("missing.reference", localeObject.get("missing.reference"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.localization.BentoBoxLocale#getLanguage()}.
     */
    @Test
    public void testGetLanguage() {
        assertEquals(Locale.US.getDisplayLanguage(), localeObject.getLanguage());
        assertEquals("unknown", new BentoBoxLocale(null, new YamlConfiguration()).getLanguage());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.localization.BentoBoxLocale#getCountry()}.
     */
    @Test
    public void testGetCountry() {
        assertEquals(Locale.US.getDisplayCountry(), localeObject.getCountry());
        assertEquals("unknown", new BentoBoxLocale(null, new YamlConfiguration()).getCountry());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.localization.BentoBoxLocale#toLanguageTag()}.
     */
    @Test
    public void testToLanguageTag() {
        assertEquals(Locale.US.toLanguageTag(), localeObject.toLanguageTag());
        assertEquals("unknown", new BentoBoxLocale(null, new YamlConfiguration()).toLanguageTag());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.localization.BentoBoxLocale#getBanner()}.
     */
    @Test
    public void testGetBanner() {
        ItemStack banner = localeObject.getBanner();
        assertEquals(Material.WHITE_BANNER, banner.getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.localization.BentoBoxLocale#getAuthors()}.
     */
    @Test
    public void testGetAuthors() {
        assertEquals("tastybento", localeObject.getAuthors().getFirst());
        assertEquals("tastybento2", localeObject.getAuthors().get(1));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.localization.BentoBoxLocale#merge(org.bukkit.configuration.file.YamlConfiguration)}.
     */
    @Test
    public void testMerge() {
        YamlConfiguration config2 = new YamlConfiguration();
        config2.set("meta.banner", "SHOULD NOT BE MERGED");
        List<String> authors = new ArrayList<>();
        authors.add("new author");
        config2.set("meta.authors", authors );
        config2.set("reference.to.test", "DO NOT OVERWRITE");
        config2.set("new.string", "this is okay");
        localeObject.merge(config2);
        assertEquals("test result", localeObject.get("reference.to.test"));
        assertEquals("missing.reference", localeObject.get("missing.reference"));
        assertEquals("this is okay", localeObject.get("new.string"));
        assertEquals("test result", localeObject.get("reference.to.test"));
        assertEquals("tastybento", localeObject.getAuthors().getFirst());
        assertEquals("tastybento2", localeObject.getAuthors().get(1));
        assertEquals("new author", localeObject.getAuthors().get(2));
        assertEquals(3, localeObject.getAuthors().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.localization.BentoBoxLocale#contains(java.lang.String)}.
     */
    @Test
    public void testContains() {
        assertTrue(localeObject.contains("reference.to.test"));
        assertFalse(localeObject.contains("false.reference.to.test"));
    }

}
