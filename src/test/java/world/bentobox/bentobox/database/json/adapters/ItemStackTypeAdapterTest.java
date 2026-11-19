package world.bentobox.bentobox.database.json.adapters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import com.google.gson.stream.JsonReader;

class ItemStackTypeAdapterTest {

    @Test
    void emptyAirIsAnExpectedLowAmountSentinel() {
        assertFalse(ItemStackTypeAdapter.isUnexpectedLowAmount(Material.AIR, 0));
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
    void nullJsonTokenReturnsNull() throws IOException {
        ItemStackTypeAdapter adapter = new ItemStackTypeAdapter();
        try (JsonReader reader = new JsonReader(new StringReader("null"))) {
            assertNull(adapter.read(reader));
        }
    }
}
