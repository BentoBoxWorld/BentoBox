package world.bentobox.bentobox.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class})
public class ItemParserTest {

    private PotionMeta potionMeta;
    private BannerMeta bannerMeta;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        potionMeta = mock(PotionMeta.class);
        /*
        when(itemFactory.getItemMeta(Mockito.eq(Material.POTION))).thenReturn(potionMeta);
        when(itemFactory.getItemMeta(Mockito.eq(Material.SPLASH_POTION))).thenReturn(potionMeta);
        when(itemFactory.getItemMeta(Mockito.eq(Material.LINGERING_POTION))).thenReturn(potionMeta);
        when(itemFactory.getItemMeta(Mockito.eq(Material.TIPPED_ARROW))).thenReturn(potionMeta);
         */
        bannerMeta = mock(BannerMeta.class);
        when(itemFactory.getItemMeta(Mockito.any())).thenAnswer((Answer<ItemMeta>) invocation -> {
            switch (invocation.getArgumentAt(0, Material.class)) {
            case RED_BANNER:
            case WHITE_BANNER:
                return bannerMeta;
            case POTION:
            case SPLASH_POTION:
            case LINGERING_POTION:
            case TIPPED_ARROW:
                return potionMeta;
            default:
                return mock(ItemMeta.class);
            }
        });

    }

    @Test
    public void testParseNull() {
        assertNull(ItemParser.parse(null));
    }

    @Test
    public void testParseBlank() {
        assertNull(ItemParser.parse(""));
    }

    @Test
    public void testParseNoColons() {
        assertNull(ItemParser.parse("NOCOLONS"));
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

    @Test
    public void testParsePotionStrengthExtended() {
        ItemStack result = ItemParser.parse("POTION:STRENGTH:1:EXTENDED::5");
        assertEquals(Material.POTION, result.getType());
        PotionType type = PotionType.STRENGTH;
        boolean isExtended = true;
        boolean isUpgraded = false;
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        Mockito.verify(potionMeta).setBasePotionData(Mockito.eq(data));
        assertEquals(5, result.getAmount());
    }

    @Test
    public void testParsePotionStrengthNotExtended() {
        ItemStack result = ItemParser.parse("POTION:STRENGTH:1:::4");
        assertEquals(Material.POTION, result.getType());
        PotionType type = PotionType.STRENGTH;
        boolean isExtended = false;
        boolean isUpgraded = false;
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        Mockito.verify(potionMeta).setBasePotionData(Mockito.eq(data));
        assertEquals(4, result.getAmount());
    }

    @Test
    public void testParsePotionStrengthNotExtendedSplash() {
        ItemStack result = ItemParser.parse("POTION:STRENGTH:1::SPLASH:3");
        assertEquals(Material.SPLASH_POTION, result.getType());
        PotionType type = PotionType.STRENGTH;
        boolean isExtended = false;
        boolean isUpgraded = false;
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        Mockito.verify(potionMeta).setBasePotionData(Mockito.eq(data));
        assertEquals(3, result.getAmount());
    }

    @Test
    public void testParsePotionStrengthNotExtendedUpgradedSplash() {
        ItemStack result = ItemParser.parse("POTION:STRENGTH:2::SPLASH:3");
        assertEquals(Material.SPLASH_POTION, result.getType());
        PotionType type = PotionType.STRENGTH;
        boolean isExtended = false;
        boolean isUpgraded = true;
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        Mockito.verify(potionMeta).setBasePotionData(Mockito.eq(data));
        assertEquals(3, result.getAmount());
    }

    enum ex {
        NOT_EXTENDED,
        EXTENDED
    }

    enum ty {
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
            PotionType.LUCK
            );

    @Test
    public void testParsePotion() {
        for (PotionType type : PotionType.values()) {
            for (ex e : ex.values()) {
                for (ty t: ty.values()) {
                    for (int up = 1; up < 2; up++) {
                        boolean isExtended = e.equals(ex.EXTENDED);
                        boolean isUpgraded = up > 1;
                        if (isExtended && notExtendable.contains(type)) {
                            continue;
                        }
                        String req = "POTION:" + type.name() + ":" + up + ":" + e.name() + ":"+ t.name() + ":3";
                        ItemStack result = ItemParser.parse(req);
                        switch (t) {
                        case LINGER:
                            assertEquals(Material.LINGERING_POTION, result.getType());
                            PotionData data = new PotionData(type, isExtended, isUpgraded);
                            Mockito.verify(potionMeta, Mockito.times(3)).setBasePotionData(Mockito.eq(data));
                            break;
                        case NO_SPLASH:
                            assertEquals(Material.POTION, result.getType());
                            data = new PotionData(type, isExtended, isUpgraded);
                            Mockito.verify(potionMeta).setBasePotionData(Mockito.eq(data));
                            break;
                        case SPLASH:
                            assertEquals(Material.SPLASH_POTION, result.getType());
                            data = new PotionData(type, isExtended, isUpgraded);
                            Mockito.verify(potionMeta, Mockito.times(2)).setBasePotionData(Mockito.eq(data));
                            break;
                        default:
                            break;
                        }

                        assertEquals(3, result.getAmount());
                    }
                }
            }
        }
    }

    @Test
    public void testParseTippedArrow() {
        ItemStack result = ItemParser.parse("TIPPED_ARROW:WEAKNESS::::1");
        assertEquals(Material.TIPPED_ARROW, result.getType());
    }


    @Test
    public void testParseBannerSimple() {
        ItemStack result = ItemParser.parse("WHITE_BANNER:2");
        assertEquals(Material.WHITE_BANNER, result.getType());
        assertEquals(2, result.getAmount());
    }

    @Test
    public void testParseBannerThreeArgs() {
        // Germany
        ItemStack result = ItemParser.parse("RED_BANNER:1");
        assertEquals(Material.RED_BANNER, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    public void testParseBanner() {
        // Germany - two patterns
        ItemParser.parse("RED_BANNER:1:STRIPE_RIGHT:BLACK:STRIPE_LEFT:YELLOW");
        Mockito.verify(bannerMeta, Mockito.times(2)).addPattern(Mockito.any());
    }

    @Test
    public void testParseBannerTooManyColons() {
        ItemStack result = ItemParser.parse("WHITE_BANNER:1:::::::::::::");
        Mockito.verify(bannerMeta, Mockito.never()).addPattern(Mockito.any());
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
    }

    @Test
    public void testParseThreeItem() {
        ItemStack result = ItemParser.parse("WOODEN_SWORD:3:2");
        assertEquals(Material.WOODEN_SWORD, result.getType());
        assertEquals(2, result.getAmount());
    }

    @Test
    public void testParseBadThreeItem() {
        assertNull(ItemParser.parse("STNE:5:5"));
    }
}
