package world.bentobox.bentobox.database.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.util.Util;

class YamlDatabaseHandlerTest extends CommonTestSetup {

    /**
     * Minimal DataObject for constructing the handler.
     */
    public static class TestDataObject implements DataObject {
        private String uniqueId = "test";
        @Override
        public String getUniqueId() { return uniqueId; }
        @Override
        public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }
    }

    @Mock
    private YamlDatabaseConnector connector;

    private YamlDatabaseHandler<TestDataObject> handler;

    // Reflection handles for private methods
    private Method deserializeMethod;
    private Method deserializeStringMethod;
    private Method deserializeEnumMethod;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        handler = new YamlDatabaseHandler<>(plugin, TestDataObject.class, connector);

        deserializeMethod = YamlDatabaseHandler.class.getDeclaredMethod("deserialize", Object.class, Class.class);
        deserializeMethod.setAccessible(true);

        deserializeStringMethod = YamlDatabaseHandler.class.getDeclaredMethod("deserializeString", String.class, Class.class);
        deserializeStringMethod.setAccessible(true);

        deserializeEnumMethod = YamlDatabaseHandler.class.getDeclaredMethod("deserializeEnum", String.class, Class.class);
        deserializeEnumMethod.setAccessible(true);
    }

    // ---- deserialize() tests ----

    @Test
    void testDeserializeNullReturnsNull() throws Exception {
        assertNull(deserializeMethod.invoke(handler, null, String.class));
    }

    @Test
    void testDeserializeNullStringReturnsNull() throws Exception {
        assertNull(deserializeMethod.invoke(handler, "null", String.class));
    }

    @Test
    void testDeserializeSameClassReturnsValue() throws Exception {
        String value = "hello";
        assertSame(value, deserializeMethod.invoke(handler, value, String.class));
    }

    @Test
    void testDeserializeIntegerToLong() throws Exception {
        Object result = deserializeMethod.invoke(handler, 42, Long.class);
        assertEquals(42L, result);
        assertEquals(Long.class, result.getClass());
    }

    @Test
    void testDeserializeStringToInteger() throws Exception {
        Object result = deserializeMethod.invoke(handler, "123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    void testDeserializeStringToLong() throws Exception {
        Object result = deserializeMethod.invoke(handler, "999", Long.class);
        assertEquals(999L, result);
    }

    @Test
    void testDeserializeStringToDouble() throws Exception {
        Object result = deserializeMethod.invoke(handler, "3.14", Double.class);
        assertEquals(3.14, result);
    }

    @Test
    void testDeserializeStringToFloat() throws Exception {
        Object result = deserializeMethod.invoke(handler, "2.5", Float.class);
        assertEquals(2.5f, result);
    }

    @Test
    void testDeserializeStringToUUID() throws Exception {
        UUID expected = UUID.randomUUID();
        Object result = deserializeMethod.invoke(handler, expected.toString(), UUID.class);
        assertEquals(expected, result);
    }

    @Test
    void testDeserializeStringToLocation() throws Exception {
        String locString = "world:10:20:30:0:0";
        mockedUtil.when(() -> Util.getLocationString(locString)).thenReturn(location);
        Object result = deserializeMethod.invoke(handler, locString, Location.class);
        assertSame(location, result);
    }

    @Test
    void testDeserializeStringToWorld() throws Exception {
        mockedBukkit.when(() -> org.bukkit.Bukkit.getWorld("my_world")).thenReturn(world);
        Object result = deserializeMethod.invoke(handler, "my_world", World.class);
        assertSame(world, result);
    }

    @Test
    void testDeserializeEnum() throws Exception {
        Object result = deserializeMethod.invoke(handler, "DIAMOND", Material.class);
        assertEquals(Material.DIAMOND, result);
    }

    @Test
    void testDeserializeEnumCaseInsensitive() throws Exception {
        Object result = deserializeMethod.invoke(handler, "diamond", Material.class);
        assertEquals(Material.DIAMOND, result);
    }

    @Test
    void testDeserializeUnhandledTypeReturnsValueAsIs() throws Exception {
        // When value type doesn't match clazz and no conversion exists, return as-is
        Double value = 3.14;
        Object result = deserializeMethod.invoke(handler, value, Double.class);
        assertEquals(value, result);
    }

    // ---- deserializeString() tests ----

    @Test
    void testDeserializeStringInteger() throws Exception {
        assertEquals(42, deserializeStringMethod.invoke(handler, "42", Integer.class));
    }

    @Test
    void testDeserializeStringLong() throws Exception {
        assertEquals(100L, deserializeStringMethod.invoke(handler, "100", Long.class));
    }

    @Test
    void testDeserializeStringDouble() throws Exception {
        assertEquals(1.5, deserializeStringMethod.invoke(handler, "1.5", Double.class));
    }

    @Test
    void testDeserializeStringFloat() throws Exception {
        assertEquals(1.5f, deserializeStringMethod.invoke(handler, "1.5", Float.class));
    }

    @Test
    void testDeserializeStringUUID() throws Exception {
        UUID expected = UUID.fromString("12345678-1234-1234-1234-123456789012");
        assertEquals(expected, deserializeStringMethod.invoke(handler, "12345678-1234-1234-1234-123456789012", UUID.class));
    }

    @Test
    void testDeserializeStringLocation() throws Exception {
        String locString = "world:1:2:3:0:0";
        mockedUtil.when(() -> Util.getLocationString(locString)).thenReturn(location);
        assertSame(location, deserializeStringMethod.invoke(handler, locString, Location.class));
    }

    @Test
    void testDeserializeStringWorld() throws Exception {
        mockedBukkit.when(() -> org.bukkit.Bukkit.getWorld("test_world")).thenReturn(world);
        assertSame(world, deserializeStringMethod.invoke(handler, "test_world", World.class));
    }

    @Test
    void testDeserializeStringUnhandledClassReturnsNull() throws Exception {
        assertNull(deserializeStringMethod.invoke(handler, "value", Boolean.class));
    }

    // ---- deserializeEnum() tests ----

    @Test
    void testDeserializeEnumValid() throws Exception {
        assertEquals(Material.STONE, deserializeEnumMethod.invoke(handler, "STONE", Material.class));
    }

    @Test
    void testDeserializeEnumLowercase() throws Exception {
        assertEquals(Material.STONE, deserializeEnumMethod.invoke(handler, "stone", Material.class));
    }

    @Test
    void testDeserializeEnumMixedCase() throws Exception {
        assertEquals(Material.STONE, deserializeEnumMethod.invoke(handler, "Stone", Material.class));
    }

    @Test
    void testDeserializeEnumInvalidReturnsNull() throws Exception {
        Object result = deserializeEnumMethod.invoke(handler, "NOT_A_REAL_MATERIAL", Material.class);
        assertNull(result);
        verify(plugin).logError("Error in YML file: NOT_A_REAL_MATERIAL is not a valid value in the enum org.bukkit.Material!");
    }

    @Test
    void testDeserializeEnumEntityType() throws Exception {
        Object result = deserializeEnumMethod.invoke(handler, "PIG", EntityType.class);
        assertEquals(EntityType.PIG, result);
    }

    @Test
    void testDeserializeEnumZombifiedPiglin() throws Exception {
        // Backwards compatibility: PIG_ZOMBIE should resolve
        Object result = deserializeEnumMethod.invoke(handler, "PIG_ZOMBIE", EntityType.class);
        // Should return ZOMBIFIED_PIGLIN if present, or PIG_ZOMBIE, or PIG as fallback
        assertSame(EntityType.class, result.getClass().getDeclaringClass() != null ? result.getClass().getDeclaringClass() : result.getClass());
    }

    @Test
    void testDeserializeEnumZombifiedPiglinDirect() throws Exception {
        Object result = deserializeEnumMethod.invoke(handler, "ZOMBIFIED_PIGLIN", EntityType.class);
        assertSame(EntityType.class, result.getClass().getDeclaringClass() != null ? result.getClass().getDeclaringClass() : result.getClass());
    }
}
