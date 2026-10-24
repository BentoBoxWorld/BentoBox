package world.bentobox.bentobox.database.json.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.StringReader;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import com.google.gson.stream.JsonReader;

/**
 * @author tastybento
 */
class EnumTypeAdapterTest {

    private static String readName(EnumTypeAdapter<EntityType> adapter, String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            EntityType type = adapter.read(reader);
            return type == null ? null : type.name();
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.adapters.EnumTypeAdapter#read(com.google.gson.stream.JsonReader)}.
     */
    @Test
    void testReadKnownEntity() throws IOException {
        assertEquals("ZOMBIFIED_PIGLIN", readName(new EnumTypeAdapter<>(EntityType.class), "\"ZOMBIFIED_PIGLIN\""));
    }

    /**
     * Blueprints saved before 1.16.1 use PIG_ZOMBIE, which no longer exists in the enum.
     */
    @Test
    void testReadLegacyPigZombie() throws IOException {
        assertEquals("ZOMBIFIED_PIGLIN", readName(new EnumTypeAdapter<>(EntityType.class), "\"PIG_ZOMBIE\""));
    }

    @Test
    void testReadUnknownEntityIsNull() throws IOException {
        assertNull(readName(new EnumTypeAdapter<>(EntityType.class), "\"NOT_AN_ENTITY\""));
    }
}
