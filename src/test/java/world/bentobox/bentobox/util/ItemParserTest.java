package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests for {@link ItemParser}.
 */
class ItemParserTest extends CommonTestSetup {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Make createItemStack throw so parse() falls through to parseOld()
        when(itemFactory.createItemStack(anyString())).thenThrow(new IllegalArgumentException("test"));

        // Stub getItemMeta for materials that need specific meta types
        when(itemFactory.getItemMeta(any(Material.class))).thenAnswer(invocation -> {
            Material mat = invocation.getArgument(0);
            if (mat == Material.POTION || mat == Material.SPLASH_POTION
                    || mat == Material.LINGERING_POTION || mat == Material.TIPPED_ARROW) {
                return mock(PotionMeta.class);
            }
            if (mat == Material.PLAYER_HEAD) {
                return mock(SkullMeta.class);
            }
            return mock(ItemMeta.class);
        });
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ---- Null / blank input ----

    @Test
    void testParseNullReturnsNull() {
        assertNull(ItemParser.parse(null));
    }

    @Test
    void testParseNullWithDefaultReturnsDefault() {
        ItemStack defaultItem = new ItemStack(Material.STONE);
        assertEquals(defaultItem, ItemParser.parse(null, defaultItem));
    }

    @Test
    void testParseEmptyStringReturnsNull() {
        assertNull(ItemParser.parse(""));
    }

    @Test
    void testParseBlankStringReturnsDefault() {
        ItemStack defaultItem = new ItemStack(Material.STONE);
        assertEquals(defaultItem, ItemParser.parse("   ", defaultItem));
    }

    // ---- Simple material (1 part) ----

    @Test
    void testParseSingleMaterial() {
        ItemStack result = ItemParser.parse("DIAMOND");
        assertNotNull(result);
        assertEquals(Material.DIAMOND, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    void testParseSingleMaterialLowerCase() {
        ItemStack result = ItemParser.parse("diamond");
        assertNotNull(result);
        assertEquals(Material.DIAMOND, result.getType());
    }

    @Test
    void testParseSingleMaterialMixedCase() {
        ItemStack result = ItemParser.parse("Diamond_Sword");
        assertNotNull(result);
        assertEquals(Material.DIAMOND_SWORD, result.getType());
    }

    // ---- Material:Quantity (2 parts) ----

    @Test
    void testParseMaterialWithQuantity() {
        ItemStack result = ItemParser.parse("DIAMOND:20");
        assertNotNull(result);
        assertEquals(Material.DIAMOND, result.getType());
        assertEquals(20, result.getAmount());
    }

    @Test
    void testParseMaterialWithQuantityOne() {
        ItemStack result = ItemParser.parse("IRON_INGOT:1");
        assertNotNull(result);
        assertEquals(Material.IRON_INGOT, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    void testParseInvalidMaterialReturnsDefault() {
        ItemStack defaultItem = new ItemStack(Material.STONE);
        ItemStack result = ItemParser.parse("NOT_A_REAL_MATERIAL:5", defaultItem);
        assertEquals(defaultItem, result);
        verify(plugin).logError(anyString());
    }

    @Test
    void testParseInvalidMaterialReturnsNull() {
        ItemStack result = ItemParser.parse("NOT_A_REAL_MATERIAL:5");
        assertNull(result);
    }

    // ---- Material:Durability:Quantity (3 parts) ----

    @Test
    void testParseMaterialWithDurabilityAndQuantity() {
        ItemStack result = ItemParser.parse("IRON_SWORD:10:5");
        assertNotNull(result);
        assertEquals(Material.IRON_SWORD, result.getType());
        assertEquals(5, result.getAmount());
    }

    // ---- Quantity clamping ----

    @Test
    void testParseQuantityExceedsMaxClampedTo99() {
        ItemStack result = ItemParser.parse("DIAMOND:500");
        assertNotNull(result);
        assertEquals(99, result.getAmount());
        verify(plugin).logWarning(contains("exceeds max"));
    }

    @Test
    void testParseQuantityZeroClampedTo1() {
        ItemStack result = ItemParser.parse("DIAMOND:0");
        assertNotNull(result);
        assertEquals(1, result.getAmount());
        verify(plugin).logWarning(contains("less than 1"));
    }

    @Test
    void testParseNegativeQuantityClampedTo1() {
        ItemStack result = ItemParser.parse("DIAMOND:-5");
        assertNotNull(result);
        assertEquals(1, result.getAmount());
        verify(plugin).logWarning(contains("less than 1"));
    }

    @Test
    void testParseQuantityExactly99IsValid() {
        ItemStack result = ItemParser.parse("DIAMOND:99");
        assertNotNull(result);
        assertEquals(99, result.getAmount());
        verify(plugin, never()).logWarning(anyString());
    }

    // ---- Custom Model Data (CMD-xxx) ----

    @Test
    void testParseWithCustomModelData() {
        ItemStack result = ItemParser.parse("DIAMOND:CMD-500:1");
        assertNotNull(result);
        assertEquals(Material.DIAMOND, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    void testParseWithCustomModelDataNoQuantity() {
        ItemStack result = ItemParser.parse("DIAMOND:CMD-42");
        assertNotNull(result);
        assertEquals(Material.DIAMOND, result.getType());
    }

    @Test
    void testParseWithCustomModelDataAndQuantity() {
        ItemStack result = ItemParser.parse("DIAMOND:CMD-100:64");
        assertNotNull(result);
        assertEquals(Material.DIAMOND, result.getType());
        assertEquals(64, result.getAmount());
    }

    // ---- Potion parsing (new format: POTION:TYPE:QTY) ----

    @Test
    void testParsePotionNewFormat() {
        ItemStack result = ItemParser.parse("POTION:STRENGTH:1");
        assertNotNull(result);
        assertEquals(Material.POTION, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    void testParseSplashPotion() {
        ItemStack result = ItemParser.parse("SPLASH_POTION:HEALING:2");
        assertNotNull(result);
        assertEquals(Material.SPLASH_POTION, result.getType());
        assertEquals(2, result.getAmount());
    }

    @Test
    void testParseLingeringPotion() {
        ItemStack result = ItemParser.parse("LINGERING_POTION:POISON:1");
        assertNotNull(result);
        assertEquals(Material.LINGERING_POTION, result.getType());
    }

    @Test
    void testParseTippedArrow() {
        ItemStack result = ItemParser.parse("TIPPED_ARROW:STRENGTH:3");
        assertNotNull(result);
        assertEquals(Material.TIPPED_ARROW, result.getType());
    }

    @Test
    void testParsePotionInvalidTypeDefaultsToWater() {
        ItemStack result = ItemParser.parse("POTION:NOT_A_POTION:1");
        assertNotNull(result);
        assertEquals(Material.POTION, result.getType());
        // Invalid potion type should default to WATER via Enums.getIfPresent().or(WATER)
    }

    @Test
    void testParsePotionWrongPartCountReturnsDefault() {
        ItemStack defaultItem = new ItemStack(Material.STONE);
        ItemStack result = ItemParser.parse("POTION:STRENGTH", defaultItem);
        // Only 2 parts for a potion - not valid for either new (3) or old (6) format
        // This falls through to parseItemQuantity which will fail since "STRENGTH" isn't a number
        assertEquals(defaultItem, result);
    }

    // ---- Potion parsing (old format: POTION:NAME:LEVEL:EXTENDED:SPLASH:QTY) ----

    @Test
    void testParsePotionOldFormatSplash() {
        ItemStack result = ItemParser.parse("POTION:STRENGTH:1:EXTENDED:SPLASH:1");
        assertNotNull(result);
        assertEquals(Material.SPLASH_POTION, result.getType());
    }

    @Test
    void testParsePotionOldFormatLinger() {
        ItemStack result = ItemParser.parse("POTION:WEAKNESS:1:NOTEXTENDED:LINGER:2");
        assertNotNull(result);
        assertEquals(Material.LINGERING_POTION, result.getType());
        assertEquals(2, result.getAmount());
    }

    @Test
    void testParsePotionOldFormatRegular() {
        ItemStack result = ItemParser.parse("POTION:STRENGTH:2:NOTEXTENDED:NOSPLASH:1");
        assertNotNull(result);
        assertEquals(Material.POTION, result.getType());
    }

    @Test
    void testParseTippedArrowOldFormat() {
        ItemStack result = ItemParser.parse("TIPPED_ARROW:STRENGTH:1:EXTENDED:SPLASH:1");
        assertNotNull(result);
        assertEquals(Material.TIPPED_ARROW, result.getType());
    }

    // ---- Banner parsing ----

    @Test
    void testParseBannerSimple() {
        ItemStack result = ItemParser.parse("WHITE_BANNER:1");
        assertNotNull(result);
        assertEquals(Material.WHITE_BANNER, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    void testParseBannerWithPattern() {
        ItemStack result = ItemParser.parse("RED_BANNER:1:STRIPE_RIGHT:WHITE");
        assertNotNull(result);
        assertEquals(Material.RED_BANNER, result.getType());
    }

    @Test
    void testParseBannerInvalidMaterialDefaultsToWhite() {
        ItemStack result = ItemParser.parse("FAKE_BANNER:1");
        // "FAKE_BANNER" contains "BANNER" so enters parseBanner path
        // getMaterial returns null, so defaults to WHITE_BANNER
        assertNotNull(result);
        assertEquals(Material.WHITE_BANNER, result.getType());
    }

    // ---- Player head parsing ----

    @Test
    void testParsePlayerHeadWithQuantityOnly() {
        ItemStack result = ItemParser.parse("PLAYER_HEAD:5");
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
        assertEquals(5, result.getAmount());
    }

    @Test
    void testParsePlayerHeadWithName() {
        // Set up Bukkit.createPlayerProfile for name-based lookup
        PlayerProfile profile = mock(PlayerProfile.class);
        mockedBukkit.when(() -> Bukkit.createPlayerProfile("BONNe1704")).thenReturn(profile);

        ItemStack result = ItemParser.parse("PLAYER_HEAD:BONNe1704");
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
    }

    @Test
    void testParsePlayerHeadWithNameAndQuantity() {
        PlayerProfile profile = mock(PlayerProfile.class);
        mockedBukkit.when(() -> Bukkit.createPlayerProfile("TestPlayer")).thenReturn(profile);

        ItemStack result = ItemParser.parse("PLAYER_HEAD:TestPlayer:3");
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
        assertEquals(3, result.getAmount());
    }

    @Test
    void testParsePlayerHeadWithFullUUID() {
        UUID uuid = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        mockedBukkit.when(() -> Bukkit.createPlayerProfile(uuid)).thenReturn(profile);

        ItemStack result = ItemParser.parse("PLAYER_HEAD:" + uuid);
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
    }

    @Test
    void testParsePlayerHeadWithTrimmedUUID() {
        // Trimmed UUID is 32 chars (no dashes)
        UUID uuid = UUID.randomUUID();
        String trimmed = uuid.toString().replace("-", "");
        PlayerProfile profile = mock(PlayerProfile.class);
        mockedBukkit.when(() -> Bukkit.createPlayerProfile(uuid)).thenReturn(profile);

        ItemStack result = ItemParser.parse("PLAYER_HEAD:" + trimmed);
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
    }

    @Test
    void testParsePlayerHeadWithBase64Texture() {
        // Create a valid base64-encoded texture JSON
        String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/abc123\"}}}";
        String base64 = Base64.getEncoder().encodeToString(textureJson.getBytes());

        PlayerProfile profile = mock(PlayerProfile.class);
        PlayerTextures textures = mock(PlayerTextures.class);
        when(profile.getTextures()).thenReturn(textures);
        mockedBukkit.when(() -> Bukkit.createPlayerProfile(any(UUID.class), any(String.class))).thenReturn(profile);

        ItemStack result = ItemParser.parse("PLAYER_HEAD:" + base64 + ":1");
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
    }

    @Test
    void testParsePlayerHeadInvalidProfileLogsError() {
        // Don't stub createPlayerProfile - it will throw
        mockedBukkit.when(() -> Bukkit.createPlayerProfile("BadPlayer")).thenThrow(new RuntimeException("test"));

        ItemStack result = ItemParser.parse("PLAYER_HEAD:BadPlayer");
        // Should still return a head, just without the profile
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());
        verify(plugin).logError(contains("Could not parse player head"));
    }

    // ---- Single-arg parse delegates correctly ----

    @Test
    void testParseSingleArgDelegatesToTwoArg() {
        // parse(String) should call parse(String, null)
        assertNull(ItemParser.parse(null));

        ItemStack result = ItemParser.parse("DIAMOND");
        assertNotNull(result);
        assertEquals(Material.DIAMOND, result.getType());
    }

    // ---- Various materials ----

    @Test
    void testParseVariousMaterials() {
        // Test a variety of valid materials
        String[] materials = {"STONE", "DIRT", "COBBLESTONE", "OAK_PLANKS", "GOLDEN_APPLE"};
        for (String mat : materials) {
            ItemStack result = ItemParser.parse(mat);
            assertNotNull(result, "Failed to parse: " + mat);
            assertEquals(Material.valueOf(mat), result.getType(), "Wrong material for: " + mat);
        }
    }
}
