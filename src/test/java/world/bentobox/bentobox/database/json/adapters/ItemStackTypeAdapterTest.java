package world.bentobox.bentobox.database.json.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests for {@link ItemStackTypeAdapter}. Items are saved and loaded through MockBukkit's real
 * ItemStack serialization, which uses the modern id/count/components format.
 */
class ItemStackTypeAdapterTest extends CommonTestSetup {

    private static final Gson GSON = new Gson();

    private ItemStackTypeAdapter adapter;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // ItemStack (de)serialization goes through Bukkit.getUnsafe(), which the deep-stubbed
        // static Bukkit mock would replace with a mock. Use MockBukkit's real server instead.
        mockedBukkit.closeOnDemand();
        adapter = new ItemStackTypeAdapter();
    }

    private ItemStack named(Material material, int amount, String name) {
        ItemStack is = new ItemStack(material, amount);
        ItemMeta meta = is.getItemMeta();
        meta.setDisplayName(name);
        is.setItemMeta(meta);
        return is;
    }

    /**
     * Wraps stored YAML the way the database holds it: as a JSON string.
     */
    private ItemStack readYaml(String yaml) throws IOException {
        return adapter.fromJson(GSON.toJson(yaml));
    }

    // ---- write: amount clamping ----

    @Test
    void emptyAirIsAnExpectedLowAmountSentinel() {
        assertFalse(ItemStackTypeAdapter.isUnexpectedLowAmount(Material.AIR, 0));
        assertFalse(ItemStackTypeAdapter.isUnexpectedLowAmount(Material.CAVE_AIR, 0));
        assertFalse(ItemStackTypeAdapter.isUnexpectedLowAmount(Material.VOID_AIR, 0));
    }

    @Test
    void zeroAmountRealItemRemainsUnexpected() {
        assertTrue(ItemStackTypeAdapter.isUnexpectedLowAmount(Material.STONE, 0));
    }

    @Test
    void positiveAmountRealItemIsValid() {
        assertFalse(ItemStackTypeAdapter.isUnexpectedLowAmount(Material.STONE, 1));
    }

    @Test
    void writeClampsOversizedStackWithWarning() throws IOException {
        ItemStack result = adapter.fromJson(adapter.toJson(new ItemStack(Material.STONE, 150)));
        assertEquals(99, result.getAmount());
        verify(plugin).logWarning(contains("exceeds max"));
    }

    // ---- read ----

    @Test
    void nullJsonTokenReturnsNull() throws IOException {
        assertNull(adapter.fromJson("null"));
    }

    @Test
    void roundTripKeepsPlainItem() throws IOException {
        ItemStack result = adapter.fromJson(adapter.toJson(new ItemStack(Material.DIAMOND, 12)));
        assertEquals(Material.DIAMOND, result.getType());
        assertEquals(12, result.getAmount());
        verify(plugin, never()).logWarning(anyString());
        verify(plugin, never()).logError(anyString());
    }

    /**
     * Regression test: item data that contains the text "type:" used to be mistaken for the
     * material, and the item was replaced with AIR.
     */
    @Test
    void roundTripKeepsItemWhoseDataContainsTypeText() throws IOException {
        String json = adapter.toJson(named(Material.DIAMOND_SWORD, 3, "type: nonsense"));
        assertTrue(json.contains("type:"), "Serialized item should contain the trap text");

        ItemStack result = adapter.fromJson(json);

        assertEquals(Material.DIAMOND_SWORD, result.getType());
        assertEquals(3, result.getAmount());
        assertEquals("type: nonsense", result.getItemMeta().getDisplayName());
        verify(plugin, never()).logWarning(anyString());
        verify(plugin, never()).logError(anyString());
    }

    @Test
    void unloadableItemDataFallsBackToMaterialFromId() throws IOException {
        // Component data MockBukkit cannot build. The nested type: must not be read as the material.
        String yaml = """
                is:
                  ==: org.bukkit.inventory.ItemStack
                  DataVersion: 4440
                  id: minecraft:diamond_sword
                  count: 2
                  components:
                    minecraft:attribute_modifiers: '[{type:"minecraft:attack_damage",id:"test:x",amount:5.0d,operation:"add_value"}]'
                  schema_version: 1
                """;

        ItemStack result = readYaml(yaml);

        assertEquals(Material.DIAMOND_SWORD, result.getType());
        verify(plugin).logWarning(contains("Recovered it as a plain DIAMOND_SWORD"));
        verify(plugin, never()).logError(anyString());
    }

    @Test
    void unreadableDataBecomesAirAndLogsTheStoredData() throws IOException {
        String yaml = "is: [this is: not, an item";

        ItemStack result = readYaml(yaml);

        assertEquals(Material.AIR, result.getType());
        verify(plugin).logError(contains(yaml));
    }

    // ---- recoverMaterial ----

    @Test
    void recoverMaterialReadsModernId() {
        String yaml = """
                is:
                  ==: org.bukkit.inventory.ItemStack
                  id: minecraft:golden_apple
                  count: 1
                """;
        assertEquals(Material.GOLDEN_APPLE, ItemStackTypeAdapter.recoverMaterial(yaml));
    }

    @Test
    void recoverMaterialReadsLegacyTypeAndIgnoresNestedType() {
        String yaml = """
                is:
                  ==: org.bukkit.inventory.ItemStack
                  v: 3700
                  meta:
                    ==: ItemMeta
                    meta-type: FIREWORK
                    firework-effects:
                    - ==: Firework
                      type: BALL
                  type: FIREWORK_ROCKET
                """;
        assertEquals(Material.FIREWORK_ROCKET, ItemStackTypeAdapter.recoverMaterial(yaml));
    }

    @Test
    void recoverMaterialStripsQuotes() {
        String yaml = """
                is:
                  ==: org.bukkit.inventory.ItemStack
                  type: 'STONE'
                """;
        assertEquals(Material.STONE, ItemStackTypeAdapter.recoverMaterial(yaml));
    }

    @Test
    void recoverMaterialIgnoresTrailingWhitespace() {
        String yaml = "is:\n  ==: org.bukkit.inventory.ItemStack\n  type: 'STONE'  \t\n";
        assertEquals(Material.STONE, ItemStackTypeAdapter.recoverMaterial(yaml));
    }

    @Test
    void recoverMaterialReturnsNullForUnknownMaterial() {
        String yaml = """
                is:
                  ==: org.bukkit.inventory.ItemStack
                  type: NOT_A_REAL_MATERIAL
                """;
        assertNull(ItemStackTypeAdapter.recoverMaterial(yaml));
    }

    @Test
    void recoverMaterialReturnsNullWithoutItemMarker() {
        assertNull(ItemStackTypeAdapter.recoverMaterial("type: STONE\n"));
    }
}
