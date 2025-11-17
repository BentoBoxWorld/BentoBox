package world.bentobox.bentobox.database.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.bukkit.Bukkit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.util.Util;

/**
 * Test class
 * @author tastybento
 */

//@PrepareForTest( { Bukkit.class, BentoBox.class, Util.class })
public class JSONDatabaseHandlerTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private DatabaseConnector connector;
    private TestClass test;

    private JSONDatabaseHandler<TestClass> handler;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        // Setup plugin
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.isEnabled()).thenReturn(false);  // Force sync actions
        //Bukkit
        //PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        test = new TestClass();
        test.setUniqueId("test");
        handler = new JSONDatabaseHandler<>(plugin, TestClass.class, connector);
    }

    class TestClass implements DataObject {

        @Expose
        private String uniqueId;

        @Override
        public String getUniqueId() {
            return uniqueId;
        }

        @Override
        public void setUniqueId(String uniqueId) {
            this.uniqueId = uniqueId;

        }

    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        // Clean up the folders
        File dataFolder = new File("database");
        if (dataFolder.exists()) {
            Files.walk(dataFolder.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
        dataFolder = new File("database_backup");
        if (dataFolder.exists()) {
            Files.walk(dataFolder.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.JSONDatabaseHandler#loadObjects()}.
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    @Test
    public void testLoadObjects() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        assertTrue(handler.loadObjects().isEmpty());
        handler.saveObject(test);
        assertEquals(test.getUniqueId(), handler.loadObjects().getFirst().getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.JSONDatabaseHandler#loadObject(java.lang.String)}.
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    @Test
    public void testLoadObject() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        assertNull(handler.loadObject("test"));
        handler.saveObject(test);
        assertEquals(test.getUniqueId(), handler.loadObject("test").getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.JSONDatabaseHandler#deleteObject(java.lang.Object)}.
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    @Test
    public void testDeleteObject() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        handler.saveObject(test);
        assertNotNull(handler.loadObject("test"));
        handler.deleteObject(test);
        assertNull(handler.loadObject("test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.JSONDatabaseHandler#objectExists(java.lang.String)}.
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    @Test
    public void testObjectExists() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        assertFalse(handler.objectExists("test"));
        when(connector.uniqueIdExists(anyString(), anyString())).thenReturn(true);
        assertTrue(handler.objectExists("test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.json.JSONDatabaseHandler#deleteID(java.lang.String)}.
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    @Test
    public void testDeleteID() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        assertNull(handler.loadObject("test"));
        handler.saveObject(test);
        handler.deleteID("test");
        assertNull(handler.loadObject("test"));
    }

}
