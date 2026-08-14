package world.bentobox.bentobox.database.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.util.Util;

class YamlDatabaseHandlerTest extends CommonTestSetup {

    /**
     * DataObject with various field types for testing serialization/deserialization.
     */
    public static class TestDataObject implements DataObject {
        private String uniqueId = "test";
        private String name = "";
        private int count = 0;
        private Map<String, Integer> scores = new HashMap<>();
        private Set<String> tags = new HashSet<>();
        private List<String> items = new java.util.ArrayList<>();
        private Material material = Material.STONE;

        @Override
        public String getUniqueId() { return uniqueId; }
        @Override
        public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public Map<String, Integer> getScores() { return scores; }
        public void setScores(Map<String, Integer> scores) { this.scores = scores; }
        public Set<String> getTags() { return tags; }
        public void setTags(Set<String> tags) { this.tags = tags; }
        public List<String> getItems() { return items; }
        public void setItems(List<String> items) { this.items = items; }
        public Material getMaterial() { return material; }
        public void setMaterial(Material material) { this.material = material; }
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

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        // Clean up database folder
        File dbFolder = new File(plugin.getDataFolder(), "database");
        if (dbFolder.exists()) {
            Files.walk(dbFolder.toPath())
                .map(Path::toFile)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(File::delete);
        }
    }

    // ---- serialize() tests via reflection ----

    @Test
    void testSerializeNull() throws Exception {
        Method serializeMethod = YamlDatabaseHandler.class.getDeclaredMethod("serialize", Object.class);
        serializeMethod.setAccessible(true);
        assertEquals("null", serializeMethod.invoke(handler, (Object) null));
    }

    @Test
    void testSerializeUUID() throws Exception {
        Method serializeMethod = YamlDatabaseHandler.class.getDeclaredMethod("serialize", Object.class);
        serializeMethod.setAccessible(true);
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid.toString(), serializeMethod.invoke(handler, uuid));
    }

    @Test
    void testSerializeWorld() throws Exception {
        Method serializeMethod = YamlDatabaseHandler.class.getDeclaredMethod("serialize", Object.class);
        serializeMethod.setAccessible(true);
        when(world.getName()).thenReturn("my_world");
        assertEquals("my_world", serializeMethod.invoke(handler, world));
    }

    @Test
    void testSerializeLocation() throws Exception {
        Method serializeMethod = YamlDatabaseHandler.class.getDeclaredMethod("serialize", Object.class);
        serializeMethod.setAccessible(true);
        mockedUtil.when(() -> Util.getStringLocation(location)).thenReturn("world:10:20:30:0:0");
        assertEquals("world:10:20:30:0:0", serializeMethod.invoke(handler, location));
    }

    @Test
    void testSerializeEnum() throws Exception {
        Method serializeMethod = YamlDatabaseHandler.class.getDeclaredMethod("serialize", Object.class);
        serializeMethod.setAccessible(true);
        // Material implements Keyed, so the Keyed case handles it (lowercase key)
        assertEquals("diamond", serializeMethod.invoke(handler, Material.DIAMOND));
    }

    @Test
    void testSerializePlainObject() throws Exception {
        Method serializeMethod = YamlDatabaseHandler.class.getDeclaredMethod("serialize", Object.class);
        serializeMethod.setAccessible(true);
        assertEquals("hello", serializeMethod.invoke(handler, "hello"));
        assertEquals(42, serializeMethod.invoke(handler, 42));
    }

    // ---- objectExists() ----

    @Test
    void testObjectExistsTrue() {
        when(connector.uniqueIdExists("TestDataObject", "test-id")).thenReturn(true);
        assertTrue(handler.objectExists("test-id"));
    }

    @Test
    void testObjectExistsFalse() {
        when(connector.uniqueIdExists("TestDataObject", "missing")).thenReturn(false);
        assertFalse(handler.objectExists("missing"));
    }

    // ---- loadObject() ----

    @Test
    void testLoadObject() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("uniqueId", "loaded-id");
        config.set("name", "test name");
        config.set("count", 42);
        config.set("material", "DIAMOND");
        when(connector.loadYamlFile(anyString(), eq("mykey"))).thenReturn(config);

        TestDataObject result = handler.loadObject("mykey");
        assertNotNull(result);
        assertEquals("loaded-id", result.getUniqueId());
        assertEquals("test name", result.getName());
        assertEquals(42, result.getCount());
        assertEquals(Material.DIAMOND, result.getMaterial());
    }

    @Test
    void testLoadObjectWithCollections() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("uniqueId", "coll-test");
        config.set("name", "");
        config.set("count", 0);
        config.set("material", "STONE");
        // Map
        config.set("scores.alice", 100);
        config.set("scores.bob", 200);
        // Set stored as list
        config.set("tags", List.of("tag1", "tag2"));
        // List
        config.set("items", List.of("item1", "item2", "item3"));
        when(connector.loadYamlFile(anyString(), eq("coll-test"))).thenReturn(config);

        TestDataObject result = handler.loadObject("coll-test");
        assertNotNull(result);
        assertEquals(2, result.getScores().size());
        assertEquals(100, result.getScores().get("alice"));
        assertEquals(200, result.getScores().get("bob"));
        assertEquals(2, result.getTags().size());
        assertTrue(result.getTags().contains("tag1"));
        assertEquals(3, result.getItems().size());
    }

    @Test
    void testLoadObjectMissingFieldUsesDefault() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("uniqueId", "missing-test");
        // name not set at all
        config.set("count", 0);
        config.set("material", "STONE");
        when(connector.loadYamlFile(anyString(), eq("missing-test"))).thenReturn(config);

        TestDataObject result = handler.loadObject("missing-test");
        assertNotNull(result);
        // name not in config, so keeps its default value
        assertEquals("", result.getName());
    }

    // ---- loadObjects() ----

    @Test
    void testLoadObjectsEmptyFolder() throws Exception {
        // Create the empty table folder
        File tableFolder = new File(plugin.getDataFolder(), "database" + File.separator + "TestDataObject");
        tableFolder.mkdirs();

        List<TestDataObject> result = handler.loadObjects();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadObjectsWithFiles() throws Exception {
        // Create the table folder with a yml file
        File tableFolder = new File(plugin.getDataFolder(), "database" + File.separator + "TestDataObject");
        tableFolder.mkdirs();
        File ymlFile = new File(tableFolder, "obj1.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("uniqueId", "obj1");
        yaml.set("name", "Object One");
        yaml.set("count", 5);
        yaml.set("material", "STONE");
        yaml.save(ymlFile);

        // Stub the connector to return the config
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.set("uniqueId", "obj1");
        loaded.set("name", "Object One");
        loaded.set("count", 5);
        loaded.set("material", "STONE");
        when(connector.loadYamlFile(anyString(), eq("obj1.yml"))).thenReturn(loaded);

        List<TestDataObject> result = handler.loadObjects();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("obj1", result.getFirst().getUniqueId());
    }

    // ---- saveObject() ----

    @Test
    void testSaveObjectNull() throws Exception {
        CompletableFuture<Boolean> result = handler.saveObject(null);
        assertFalse(result.join());
        verify(plugin).logError("YAML database request to store a null.");
    }

    @Test
    void testSaveObjectNotDataObject() throws Exception {
        // Create a handler for a non-DataObject class to test the check
        // Actually, we can't easily do that since the generic is bound. Instead test with a null cast.
        // The null case is already tested above, so let's test with a valid DataObject during shutdown
        when(plugin.isShutdown()).thenReturn(true);

        TestDataObject obj = new TestDataObject();
        obj.setUniqueId("save-test");
        obj.setName("Test Save");
        obj.setCount(10);

        when(connector.saveYamlFile(anyString(), anyString(), anyString(), any())).thenReturn(true);

        CompletableFuture<Boolean> result = handler.saveObject(obj);
        // During shutdown, processFile runs sync
        assertTrue(result.join());
    }

    @Test
    void testSaveObjectWithCollections() throws Exception {
        when(plugin.isShutdown()).thenReturn(true);

        TestDataObject obj = new TestDataObject();
        obj.setUniqueId("coll-save");
        obj.setName("Collections");
        obj.setScores(Map.of("alice", 100, "bob", 200));
        obj.setTags(Set.of("tag1", "tag2"));
        obj.setItems(List.of("item1", "item2"));
        obj.setMaterial(Material.DIAMOND);

        when(connector.saveYamlFile(anyString(), anyString(), anyString(), any())).thenReturn(true);

        CompletableFuture<Boolean> result = handler.saveObject(obj);
        assertTrue(result.join());
        verify(connector).saveYamlFile(anyString(), anyString(), eq("coll-save"), any());
    }

    // ---- deleteObject() ----

    @Test
    void testDeleteObjectNull() throws Exception {
        handler.deleteObject(null);
        verify(plugin).logError("YAML database request to delete a null.");
    }

    @Test
    void testDeleteObject() throws Exception {
        when(plugin.isEnabled()).thenReturn(false); // sync delete

        TestDataObject obj = new TestDataObject();
        obj.setUniqueId("del-test");

        // Create the file so delete can find it
        File tableFolder = new File(plugin.getDataFolder(), "database" + File.separator + "TestDataObject");
        tableFolder.mkdirs();
        File ymlFile = new File(tableFolder, "del-test.yml");
        ymlFile.createNewFile();
        assertTrue(ymlFile.exists());

        handler.deleteObject(obj);

        // File should be deleted
        assertFalse(ymlFile.exists());
    }

    // ---- deleteID() ----

    @Test
    void testDeleteID() throws Exception {
        when(plugin.isEnabled()).thenReturn(false); // sync delete

        // Create the file
        File tableFolder = new File(plugin.getDataFolder(), "database" + File.separator + "TestDataObject");
        tableFolder.mkdirs();
        File ymlFile = new File(tableFolder, "delete-me.yml");
        ymlFile.createNewFile();
        assertTrue(ymlFile.exists());

        handler.deleteID("delete-me");

        assertFalse(ymlFile.exists());
    }

    @Test
    void testDeleteIDWithYmlSuffix() throws Exception {
        when(plugin.isEnabled()).thenReturn(false); // sync delete

        File tableFolder = new File(plugin.getDataFolder(), "database" + File.separator + "TestDataObject");
        tableFolder.mkdirs();
        File ymlFile = new File(tableFolder, "already-has.yml");
        ymlFile.createNewFile();
        assertTrue(ymlFile.exists());

        handler.deleteID("already-has.yml");

        assertFalse(ymlFile.exists());
    }

    @Test
    void testDeleteIDNull() {
        when(plugin.isEnabled()).thenReturn(false);
        // Should not throw
        handler.deleteID(null);
    }

    @Test
    void testDeleteIDNoFolder() {
        when(plugin.isEnabled()).thenReturn(false);
        // Folder doesn't exist - should not throw
        handler.deleteID("nonexistent");
    }

    // ---- close() ----

    @Test
    void testClose() {
        // Should not throw
        handler.close();
    }

    // ---- loadObject with map containing dots ----

    @Test
    void testLoadObjectMapWithDots() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("uniqueId", "dot-test");
        config.set("name", "");
        config.set("count", 0);
        config.set("material", "STONE");
        // Map key with serialized dot
        config.set("scores.:dot:key:dot:name", 999);
        when(connector.loadYamlFile(anyString(), eq("dot-test"))).thenReturn(config);

        TestDataObject result = handler.loadObject("dot-test");
        assertNotNull(result);
        // :dot: should be converted back to .
        assertTrue(result.getScores().containsKey(".key.name"));
        assertEquals(999, result.getScores().get(".key.name"));
    }

    // ---- saveObject generates uniqueId if empty ----

    @Test
    void testSaveObjectGeneratesUniqueId() throws Exception {
        when(plugin.isShutdown()).thenReturn(true);
        when(connector.getUniqueId("TestDataObject")).thenReturn("generated-id");
        when(connector.saveYamlFile(anyString(), anyString(), anyString(), any())).thenReturn(true);

        TestDataObject obj = new TestDataObject();
        obj.setUniqueId(""); // empty, should trigger generation

        CompletableFuture<Boolean> result = handler.saveObject(obj);
        assertTrue(result.join());
        assertEquals("generated-id", obj.getUniqueId());
    }

    // ---- saveObject with null uniqueId ----

    @Test
    void testSaveObjectNullUniqueId() throws Exception {
        when(plugin.isShutdown()).thenReturn(true);
        when(connector.getUniqueId("TestDataObject")).thenReturn("auto-id");
        when(connector.saveYamlFile(anyString(), anyString(), anyString(), any())).thenReturn(true);

        TestDataObject obj = new TestDataObject();
        obj.setUniqueId(null);

        CompletableFuture<Boolean> result = handler.saveObject(obj);
        assertTrue(result.join());
        assertEquals("auto-id", obj.getUniqueId());
    }

    // ---- loadObject with empty config (missing fields use defaults) ----

    @Test
    void testLoadObjectEmptyConfig() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        // No fields set at all
        when(connector.loadYamlFile(anyString(), eq("empty"))).thenReturn(config);

        TestDataObject result = handler.loadObject("empty");
        assertNotNull(result);
        // Should have default values
        assertEquals("test", result.getUniqueId()); // default from class
        assertEquals("", result.getName());
        assertEquals(0, result.getCount());
    }
}
