package world.bentobox.bentobox.database.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.FlagsManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Bukkit.class} )
public class YamlDatabaseHandlerTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private Logger logger;
    @Captor
    private ArgumentCaptor<Runnable> registerLambdaCaptor;
    @Mock
    YamlDatabaseConnector dbConnector;
    @Mock
    private Island island;
    @Mock
    private BukkitTask task;

    private YamlDatabaseHandler<Island> handler;

    // File system
    private static File database;
    private File islandTable;
    private File record;
    private File record2;
    private UUID uuid;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.isEnabled()).thenReturn(true);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(scheduler);

        when(scheduler.runTaskAsynchronously(any(), any(Runnable.class))).thenReturn(task);
        Server server = mock(Server.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("cleanroom");
        when(server.getWorld(anyString())).thenReturn(world);
        when(Bukkit.getServer()).thenReturn(server);

        // A YAML file representing island
        uuid = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(getYaml(uuid));
        YamlConfiguration config2 = new YamlConfiguration();
        config2.loadFromString(getYaml2(uuid2));
        when(dbConnector.loadYamlFile(anyString(), anyString())).thenReturn(config, config2);

        // Flags Manager
        FlagsManager fm = mock(FlagsManager.class);
        when(fm.getFlag(anyString())).thenReturn(Optional.empty());
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Island
        when(island.getUniqueId()).thenReturn(uuid.toString());

        // File system
        database = new File("database");
        islandTable = new File(database, "Island");
        islandTable.mkdirs();
        record = new File(islandTable, uuid.toString() + ".yml");
        record2 = new File(islandTable, uuid2.toString() + ".yml");
        config.save(record);
        config2.save(record2);

        // Handler
        handler = new YamlDatabaseHandler<Island>(plugin, Island.class, dbConnector);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        // Clean up file system
        Files.walk(database.toPath())
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
        framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#loadObjects()}.
     */
    @Test
    public void testLoadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        List<Island> list = handler.loadObjects();
        assertFalse(list.isEmpty());
        // Check at least one contains correct info
        assertTrue(list.stream().anyMatch(i -> i.getOwner().toString().equals("5988eecd-1dcd-4080-a843-785b62419abb")));
        assertTrue(list.stream().anyMatch(i -> i.getUniqueId().equals(uuid.toString())));
        assertTrue(list.stream().anyMatch(i -> i.getCreatedDate() == 1552264678424L));
        assertTrue(list.stream().anyMatch(i -> i.getMembers().get(UUID.fromString("5988eecd-1dcd-4080-a843-785b62419abb")) == 1000));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#loadObject(java.lang.String)}.
     * @throws IntrospectionException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Test
    public void testLoadObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        String name = UUID.randomUUID().toString();
        Island is = handler.loadObject(name);
        assertEquals(uuid.toString(), is.getUniqueId());
        assertEquals("5988eecd-1dcd-4080-a843-785b62419abb", is.getOwner().toString());
        assertEquals(1552264678424L, is.getCreatedDate());
        assertEquals((Integer)1000, is.getMembers().get(UUID.fromString("5988eecd-1dcd-4080-a843-785b62419abb")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#saveObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveObject() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        when(plugin.isEnabled()).thenReturn(false);
        Island is = new Island();
        is.setUniqueId("unique");
        Location center = mock(Location.class);
        is.setCenter(center);
        handler.saveObject(is);
        verify(dbConnector).saveYamlFile(anyString(), eq("database/Island"), eq("unique"), isA(Map.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#saveObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testSaveObjectNull() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        handler.saveObject(null);
        verify(plugin).logError("YAML database request to store a null.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#saveObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testSaveObjectNotDO() throws IllegalAccessException, InvocationTargetException, IntrospectionException{
        YamlDatabaseHandler<String> h = new YamlDatabaseHandler<String>(plugin, String.class, dbConnector);
        String test = "";
        h.saveObject(test);
        verify(plugin).logError("This class is not a DataObject: java.lang.String");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#deleteObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testDeleteObject() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        handler.deleteObject(island);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#deleteObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testDeleteObjectNull() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        handler.deleteObject(null);
        verify(plugin).logError("YAML database request to delete a null.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#deleteObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testDeleteObjectNotDO() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        YamlDatabaseHandler<String> h = new YamlDatabaseHandler<String>(plugin, String.class, dbConnector);
        String test = "";
        h.deleteObject(test);
        verify(plugin).logError("This class is not a DataObject: java.lang.String");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#objectExists(java.lang.String)}.
     */
    @Test
    public void testObjectExists() {
        when(dbConnector.uniqueIdExists(eq(Island.class.getSimpleName()), eq(uuid.toString()))).thenReturn(true);
        assertTrue(handler.objectExists(uuid.toString()));
        assertFalse(handler.objectExists("nope"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#deleteID(java.lang.String)}.
     */
    @Test
    public void testDeleteID() {
        handler.deleteID(uuid.toString());
        // Handled by queue
        assertTrue(record.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#deleteID(java.lang.String)}.
     */
    @Test
    public void testDeleteIDPluginNotEnabled() {
        when(plugin.isEnabled()).thenReturn(false);
        handler.deleteID(uuid.toString());
        assertFalse(record.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#deleteID(java.lang.String)}.
     */
    @Test
    public void testDeleteIDNotEnabledWithYML() {
        when(plugin.isEnabled()).thenReturn(false);
        handler.deleteID(uuid.toString() + ".yml");
        assertFalse(record.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.yaml.YamlDatabaseHandler#YamlDatabaseHandler(world.bentobox.bentobox.BentoBox, java.lang.Class, world.bentobox.bentobox.database.DatabaseConnector)}.
     */
    @Test
    public void testYamlDatabaseHandler() {
        verify(scheduler).runTaskAsynchronously(eq(plugin), registerLambdaCaptor.capture());
        Runnable lamda = registerLambdaCaptor.getValue();
        // Cannot run with true otherwise it'll infinite loop
        when(plugin.isShutdown()).thenReturn(true);
        lamda.run();
        verify(task).cancel();

    }

    // YAML
    private String getYaml(UUID uuid) {
        return "deleted: false\n" +
                "uniqueId: " + uuid.toString() + "\n" +
                "center: cleanroom:384:100:-768:0:0\n" +
                "range: 192\n" +
                "protectionRange: 100\n" +
                "maxEverProtectionRange: 100\n" +
                "world: cleanroom\n" +
                "name: 'null'\n" +
                "createdDate: 1552264678424\n" +
                "updatedDate: 1552264678424\n" +
                "owner: 5988eecd-1dcd-4080-a843-785b62419abb\n" +
                "members:\n" +
                "  5988eecd-1dcd-4080-a843-785b62419abb: 1000\n" +
                "spawn: false\n" +
                "purgeProtected: false\n" +
                "flags:\n" +
                "  HURT_ANIMALS: 500\n" +
                "  DRAGON_EGG: 500\n" +
                "  REDSTONE: 500\n" +
                "  BUCKET: 500\n" +
                "  LOCK: 0\n" +
                "  ENDER_PEARL: 500\n" +
                "  DOOR: 500\n" +
                "  FURNACE: 500\n" +
                "  MINECART: 500\n" +
                "  ANVIL: 500\n" +
                "  FISH_SCOOPING: 500\n" +
                "  FIRE_IGNITE: 500\n" +
                "  END_PORTAL: 500\n" +
                "  BREEDING: 500\n" +
                "  TNT: 500\n" +
                "  HURT_VILLAGERS: 500\n" +
                "  FROST_WALKER: 500\n" +
                "  TURTLE_EGGS: 500\n" +
                "  CHALLENGES_ISLAND_PROTECTION: 0\n" +
                "  LEAF_DECAY: 500\n" +
                "  COLLECT_LAVA: 500\n" +
                "  LEVER: 500\n" +
                "  RIDING: 500\n" +
                "  HURT_MONSTERS: 500\n" +
                "  ARMOR_STAND: 500\n" +
                "  NAME_TAG: 500\n" +
                "  FIRE_SPREAD: 500\n" +
                "  TRADING: 500\n" +
                "  EGGS: 500\n" +
                "  ITEM_DROP: 500\n" +
                "  PVP_OVERWORLD: -1\n" +
                "  NOTE_BLOCK: 500\n" +
                "  FLINT_AND_STEEL: 500\n" +
                "  NETHER_PORTAL: 500\n" +
                "  CROP_TRAMPLE: 500\n" +
                "  ITEM_PICKUP: 500\n" +
                "  DROPPER: 500\n" +
                "  BREWING: 500\n" +
                "  PVP_END: -1\n" +
                "  COLLECT_WATER: 500\n" +
                "  GREENHOUSE: 500\n" +
                "  BUTTON: 500\n" +
                "  FIRE_EXTINGUISH: 500\n" +
                "  BEACON: 500\n" +
                "  TRAPDOOR: 500\n" +
                "  PRESSURE_PLATE: 500\n" +
                "  EXPERIENCE_BOTTLE_THROWING: 500\n" +
                "  ITEM_FRAME: 500\n" +
                "  PLACE_BLOCKS: 500\n" +
                "  CRAFTING: 500\n" +
                "  ENCHANTING: 500\n" +
                "  SHEARING: 500\n" +
                "  BOAT: 500\n" +
                "  SPAWN_EGGS: 500\n" +
                "  BED: 500\n" +
                "  PVP_NETHER: -1\n" +
                "  MILKING: 500\n" +
                "  MONSTER_SPAWN: 500\n" +
                "  DISPENSER: 500\n" +
                "  GATE: 500\n" +
                "  FIRE_BURNING: 500\n" +
                "  EXPERIENCE_PICKUP: 500\n" +
                "  HOPPER: 500\n" +
                "  ANIMAL_SPAWN: 500\n" +
                "  LEASH: 500\n" +
                "  BREAK_BLOCKS: 500\n" +
                "  MOUNT_INVENTORY: 500\n" +
                "  CHORUS_FRUIT: 500\n" +
                "  CONTAINER: 500\n" +
                "  POTION_THROWING: 500\n" +
                "  JUKEBOX: 500\n" +
                "history: []\n" +
                "levelHandicap: 0\n" +
                "spawnPoint:\n" +
                "  THE_END: cleanroom_the_end:383:106:-769:1134395392:1106247680\n" +
                "  NORMAL: cleanroom:384:105:-766:0:1106247680\n" +
                "doNotLoad: false\n";
    }

    private String getYaml2(UUID uuid) {
        return "deleted: false\n" +
                "uniqueId: " + uuid.toString() + "\n" +
                "center: cleanroom:0:100:0:0:0\n" +
                "range: 192\n" +
                "protectionRange: 100\n" +
                "maxEverProtectionRange: 100\n" +
                "world: cleanroom\n" +
                "name: 'null'\n" +
                "createdDate: 1552264640164\n" +
                "updatedDate: 1552264640164\n" +
                "owner: 'null'\n" +
                "members: {}\n" +
                "spawn: false\n" +
                "purgeProtected: false\n" +
                "flags: {}\n" +
                "history: []\n" +
                "levelHandicap: 0\n" +
                "spawnPoint: {}\n" +
                "doNotLoad: false\n";
    }
}
