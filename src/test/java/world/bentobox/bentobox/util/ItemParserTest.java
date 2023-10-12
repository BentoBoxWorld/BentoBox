package world.bentobox.bentobox.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;


@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Bukkit.class})
public class ItemParserTest {

    @Mock
    private PotionMeta potionMeta;
    @Mock
    private BannerMeta bannerMeta;
    @Mock
    private ItemMeta itemMeta;
    @Mock
    private ItemFactory itemFactory;
    private ItemStack defaultItem;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        /*
        when(itemFactory.getItemMeta(Mockito.eq(Material.POTION))).thenReturn(potionMeta);
        when(itemFactory.getItemMeta(Mockito.eq(Material.SPLASH_POTION))).thenReturn(potionMeta);
        when(itemFactory.getItemMeta(Mockito.eq(Material.LINGERING_POTION))).thenReturn(potionMeta);
        when(itemFactory.getItemMeta(Mockito.eq(Material.TIPPED_ARROW))).thenReturn(potionMeta);
         */
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.getDataVersion()).thenReturn(777);
        when(Bukkit.getUnsafe()).thenReturn(unsafe);
        when(itemFactory.getItemMeta(any())).thenReturn(itemMeta);
        /*
        when(itemFactory.getItemMeta(any())).thenAnswer((Answer<ItemMeta>) invocation -> {
            return switch (invocation.getArgument(0, Material.class)) {
            case RED_BANNER, WHITE_BANNER -> bannerMeta;
            case POTION, SPLASH_POTION, LINGERING_POTION, TIPPED_ARROW -> potionMeta;
            default -> itemMeta;
            };
        });
         */
        defaultItem = new ItemStack(Material.STONE);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testParseNull() {
        assertNull(ItemParser.parse(null));
        assertEquals(defaultItem, ItemParser.parse(null, defaultItem));
    }

    @Test
    public void testParseBlank() {
        assertNull(ItemParser.parse(""));
        assertEquals(defaultItem, ItemParser.parse("", defaultItem));
    }

    @Test
    public void testParseNoColons() {
        assertNull(ItemParser.parse("NOCOLONS"));
        assertEquals(defaultItem, ItemParser.parse("NOCOLONS", defaultItem));
    }

    /*
     * # Format POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY
        # LEVEL, EXTENDED, SPLASH, LINGER are optional.
        # LEVEL is a number, 1 or 2
        # LINGER is for V1.9 servers and later
        # Examples:
        # POTION:STRENGTH:1:EXTENDED:SPLASH:1
        # POTION:INSTANT_DAMAGE:2::LINGER:2
        # POTION:JUMP:2:NOTEXTENDED:NOSPLASH:1
        # POTION:WEAKNESS::::1   -  any weakness potion
     */

    @Ignore("Extended potions now have their own names and are not extended like this")
    @Test
    public void testParsePotionStrengthExtended() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        ItemStack result = ItemParser.parse("POTION:STRENGTH:1:EXTENDED::5");
        assertNotNull(result);
        assertEquals(Material.POTION, result.getType());
        PotionType type = PotionType.STRENGTH;
        boolean isExtended = true;
        boolean isUpgraded = false;
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        verify(potionMeta).setBasePotionData(Mockito.eq(data));
        assertEquals(5, result.getAmount());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testParsePotionStrengthNotExtended() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        ItemStack result = ItemParser.parse("POTION:STRENGTH:1:::4");
        assertNotNull(result);
        assertEquals(Material.POTION, result.getType());
        PotionType type = PotionType.STRENGTH;
        boolean isExtended = false;
        boolean isUpgraded = false;
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        verify(potionMeta).setBasePotionData(Mockito.eq(data));
        assertEquals(4, result.getAmount());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testParsePotionStrengthNotExtendedSplash() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        ItemStack result = ItemParser.parse("POTION:STRENGTH:1::SPLASH:3");
        assertNotNull(result);
        assertEquals(Material.SPLASH_POTION, result.getType());
        PotionType type = PotionType.STRENGTH;
        boolean isExtended = false;
        boolean isUpgraded = false;
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        verify(potionMeta).setBasePotionData(Mockito.eq(data));
        assertEquals(3, result.getAmount());
    }

    @SuppressWarnings("deprecation")
    @Ignore("Potions are no longer upgraded like this")
    @Test
    public void testParsePotionStrengthNotExtendedUpgradedSplash() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        ItemStack result = ItemParser.parse("POTION:STRENGTH:2::SPLASH:3");
        assertNotNull(result);
        assertEquals(Material.SPLASH_POTION, result.getType());
        PotionType type = PotionType.STRENGTH;
        boolean isExtended = false;
        boolean isUpgraded = true;
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        verify(potionMeta).setBasePotionData(Mockito.eq(data));
        assertEquals(3, result.getAmount());
    }

    enum extend {
        NOT_EXTENDED,
        EXTENDED
    }

    enum type {
        NO_SPLASH,
        SPLASH,
        LINGER
    }

    List<PotionType> notExtendable = Arrays.asList(
            PotionType.UNCRAFTABLE,
            PotionType.WATER,
            PotionType.MUNDANE,
            PotionType.THICK,
            PotionType.AWKWARD,
            PotionType.INSTANT_HEAL,
            PotionType.INSTANT_DAMAGE,
            PotionType.LUCK,
            PotionType.NIGHT_VISION
            );

    @SuppressWarnings("deprecation")
    @Test
    public void testParsePotion() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        for (PotionType type : PotionType.values()) {
            if (type.name().contains("LONG") || type.name().contains("STRONG")) {
                continue;
            }
            for (ItemParserTest.type t: ItemParserTest.type.values()) {
                for (int up = 1; up < 2; up++) {
                    boolean isUpgraded = up > 1;
                    String req = "POTION:" + type.name() + ":" + up + "::"+ t.name() + ":3";
                    ItemStack result = ItemParser.parse(req);
                    assertNotNull(result);
                    switch (t) {
                    case LINGER:
                        assertEquals(Material.LINGERING_POTION, result.getType());
                        PotionData data = new PotionData(type, false, isUpgraded);
                        verify(potionMeta, times(3)).setBasePotionData(Mockito.eq(data));
                        break;
                    case NO_SPLASH:
                        assertEquals(Material.POTION, result.getType());
                        data = new PotionData(type, false, isUpgraded);
                        verify(potionMeta).setBasePotionData(Mockito.eq(data));
                        break;
                    case SPLASH:
                        assertEquals(Material.SPLASH_POTION, result.getType());
                        data = new PotionData(type, false, isUpgraded);
                        verify(potionMeta, times(2)).setBasePotionData(Mockito.eq(data));
                        break;
                    default:
                        break;
                    }

                    assertEquals(3, result.getAmount());
                }
            }
        }
    }

    @Test
    public void testParseTippedArrow() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        ItemStack result = ItemParser.parse("TIPPED_ARROW:WEAKNESS::::1");
        assertNotNull(result);
        assertEquals(Material.TIPPED_ARROW, result.getType());
    }


    @Test
    public void testParseBannerSimple() {
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        ItemStack result = ItemParser.parse("WHITE_BANNER:2");
        assertNotNull(result);
        assertEquals(Material.WHITE_BANNER, result.getType());
        assertEquals(2, result.getAmount());
    }

    @Test
    public void testParseBannerThreeArgs() {
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        // Germany
        ItemStack result = ItemParser.parse("RED_BANNER:1");
        assertNotNull(result);
        assertEquals(Material.RED_BANNER, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    public void testParseBanner() {
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        // Germany - two patterns
        ItemParser.parse("RED_BANNER:1:STRIPE_RIGHT:BLACK:STRIPE_LEFT:YELLOW");
        verify(bannerMeta, Mockito.times(2)).addPattern(any());
    }

    @Test
    public void testParseBannerTooManyColons() {
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        ItemStack result = ItemParser.parse("WHITE_BANNER:1:::::::::::::");
        assertNotNull(result);
        verify(bannerMeta, never()).addPattern(any());
        assertEquals(Material.WHITE_BANNER, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    public void testParseTwoItem() {
        ItemStack result = ItemParser.parse("STONE:5");
        assertEquals(Material.STONE, result.getType());
        assertEquals(5, result.getAmount());
    }

    @Test
    public void testParseBadTwoItem() {
        assertNull(ItemParser.parse("STNE:5"));
        assertEquals(defaultItem, ItemParser.parse("STNE:3", defaultItem));
        assertEquals(defaultItem, ItemParser.parse("STNE:Z", defaultItem));
    }

    @Test
    public void testParseThreeItem() {
        ItemStack result = ItemParser.parse("WOODEN_SWORD:3:2");
        assertNotNull(result);
        assertEquals(Material.WOODEN_SWORD, result.getType());
        assertEquals(2, result.getAmount());
    }

    @Test
    public void testParseBadThreeItem() {
        assertNull(ItemParser.parse("STNE:5:5"));
        assertEquals(defaultItem, ItemParser.parse("STNE:5:5", defaultItem));
        assertEquals(defaultItem, ItemParser.parse("STNE:AA:5", defaultItem));
        assertEquals(defaultItem, ItemParser.parse("WOODEN_SWORD:4:AA", defaultItem));
    }

    @Ignore("This doesn't work for some reason")
    @Test
    public void parseCustomModelData() {
        ItemStack result = ItemParser.parse("WOODEN_SWORD:CMD-23151212:2");
        assertNotNull(result);
        assertEquals(Material.WOODEN_SWORD, result.getType());
        assertEquals(2, result.getAmount());
        assertNull(ItemParser.parse("WOODEN_SWORD:CMD-23151212:2:CMD-23151212"));
    }
}
