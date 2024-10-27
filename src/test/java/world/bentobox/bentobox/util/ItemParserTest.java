package world.bentobox.bentobox.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
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
@PrepareForTest({ BentoBox.class, Bukkit.class, Objects.class })
public class ItemParserTest {

    @Mock
    private PotionMeta potionMeta;
    @Mock
    private BannerMeta bannerMeta;
    @Mock
    private ItemMeta itemMeta;
    @Mock
    private ItemFactory itemFactory;
    @Mock
    private SkullMeta skullMeta;

    private ItemStack defaultItem;


    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        // Do not test Bukkit createItemStack method output as I assume Bukkit has their tests covered.
        when(itemFactory.createItemStack(any())).thenThrow(IllegalArgumentException.class);
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.getDataVersion()).thenReturn(777);
        when(Bukkit.getUnsafe()).thenReturn(unsafe);
        when(itemFactory.getItemMeta(any())).thenReturn(itemMeta);

        defaultItem = new ItemStack(Material.STONE);
    }

    class dummy implements Registry {
        NamespacedKey get(String string) {
            return null;
        }

        @Override
        public Iterator iterator() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Keyed get(NamespacedKey key) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Stream stream() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Keyed getOrThrow(NamespacedKey key) {
            // TODO Auto-generated method stub
            return null;
        }
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

    @Test
    public void testParsePotion() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        for (PotionType type : PotionType.values()) {
            ItemStack itemStack = ItemParser.parse("POTION:" + type.name() + ":1");
            assertEquals(itemStack.getType(), Material.POTION);
            // Not sure how this can be tested.
            // assertEquals(type, ((PotionMeta) itemStack.getItemMeta()).getBasePotionType());
            assertEquals(1, itemStack.getAmount());
        }
    }

    @Test
    public void testParseSplashPotion() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        for (PotionType type : PotionType.values()) {
            ItemStack itemStack = ItemParser.parse("SPLASH_POTION:" + type.name() + ":1");
            assertEquals(itemStack.getType(), Material.SPLASH_POTION);
            // Not sure how this can be tested.
            // assertEquals(type, ((PotionMeta) itemStack.getItemMeta()).getBasePotionType());
            assertEquals(1, itemStack.getAmount());
        }
    }

    @Test
    public void testParseLingeringPotion() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        for (PotionType type : PotionType.values()) {
            ItemStack itemStack = ItemParser.parse("LINGERING_POTION:" + type.name() + ":1");
            assertEquals(itemStack.getType(), Material.LINGERING_POTION);
            // Not sure how this can be tested.
            // assertEquals(type, ((PotionMeta) itemStack.getItemMeta()).getBasePotionType());
            assertEquals(1, itemStack.getAmount());
        }
    }

    @Test
    public void testParseTippedArrow() {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        for (PotionType type : PotionType.values()) {
            ItemStack itemStack = ItemParser.parse("TIPPED_ARROW:" + type.name() + ":1");
            assertEquals(itemStack.getType(), Material.TIPPED_ARROW);
            // Not sure how this can be tested.
            // assertEquals(type, ((PotionMeta) itemStack.getItemMeta()).getBasePotionType());
            assertEquals(1, itemStack.getAmount());
        }
    }

    @Test
    public void testParseBadPotion()
    {
        when(itemFactory.getItemMeta(any())).thenReturn(potionMeta);
        ItemStack itemStack = ItemParser.parse("POTION::5");
        assertEquals(5, itemStack.getAmount());
        // Not sure how this can be tested
        // assertEquals(PotionType.WATER, ((PotionMeta) itemStack.getItemMeta()).getBasePotionType());
        itemStack = ItemParser.parse("POTION:NO_POTION:1");
        assertEquals(1, itemStack.getAmount());
        // Not sure how this can be tested
        // assertEquals(PotionType.WATER, ((PotionMeta) itemStack.getItemMeta()).getBasePotionType());
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
    @Ignore("Doesn't work on 1.21")
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

    @Test
    public void parseCustomModelData() {
        ItemStack result = ItemParser.parse("WOODEN_SWORD:CMD-23151212:2");
        assertNotNull(result);
        assertEquals(Material.WOODEN_SWORD, result.getType());
        assertEquals(2, result.getAmount());
        assertNull(ItemParser.parse("WOODEN_SWORD:CMD-23151212:2:CMD-23151212"));
    }

    @Test
    public void parsePlayerHead() {
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        ItemStack result = ItemParser.parse("PLAYER_HEAD:2");
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
        assertEquals(2, result.getAmount());

        result = ItemParser.parse("PLAYER_HEAD:BONNe1704");
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
        assertEquals(1, result.getAmount());

        // I do not know if it is possible to test metadata, as skull meta is not applied to player heads in testing.
        //assertEquals("BONNe1704", ((SkullMeta) result.getItemMeta()).getOwnerProfile().getName());
    }
}
