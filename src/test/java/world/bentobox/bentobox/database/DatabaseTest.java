/**
 *
 */
package world.bentobox.bentobox.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.objects.Island;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, DatabaseSetup.class} )
public class DatabaseTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private Addon addon;
    private AbstractDatabaseHandler<Object> handler;
    private DatabaseSetup dbSetup;
    @Mock
    private Logger logger;
    @Captor
    private ArgumentCaptor<Supplier<String>> registerMessageLambdaCaptor;
    private List<Object> objectList;
    @Mock
    private Island island;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.getLogger()).thenReturn(logger);

        PowerMockito.mockStatic(DatabaseSetup.class);

        dbSetup = mock(DatabaseSetup.class);
        handler = mock(AbstractDatabaseHandler.class);
        when(dbSetup.getHandler(Mockito.any())).thenReturn(handler);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);

        when(handler.loadObject(Mockito.anyString())).thenReturn(island);
        objectList = new ArrayList<>();
        objectList.add(mock(Island.class));
        objectList.add(mock(Island.class));
        objectList.add(mock(Island.class));
        objectList.add(mock(Island.class));
        when(handler.loadObjects()).thenReturn(objectList);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Check if logger logged a severe string
     * @param stringToCheck
     */
    private void checkSevereLog(String stringToCheck) {
        // This magic obtains the lambda from an argument
        Mockito.verify(logger).severe(registerMessageLambdaCaptor.capture());
        Supplier<String> lambda = registerMessageLambdaCaptor.getValue();
        assertEquals(stringToCheck,lambda.get());
    }
    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#Database(world.bentobox.bentobox.BentoBox, java.lang.Class)}.
     */
    @Test
    public void testDatabaseBentoBoxClassOfT() {
        new Database<Island>(plugin, Island.class);
        Mockito.verify(plugin).getLogger();
        Mockito.verify(dbSetup).getHandler(Mockito.any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#Database(world.bentobox.bentobox.api.addons.Addon, java.lang.Class)}.
     */
    @Test
    public void testDatabaseAddonClassOfT() {
        new Database<Island>(addon, Island.class);
        Mockito.verify(addon).getLogger();
        Mockito.verify(dbSetup).getHandler(Mockito.any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#loadObjects()}.
     * @throws IntrospectionException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Test
    public void testLoadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        Database<Island> db = new Database<Island>(plugin, Island.class);
        assertEquals(objectList, db.loadObjects());
        Mockito.verify(handler).loadObjects();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#loadObjects()}.
     * @throws IntrospectionException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Test
    public void testLoadObjectsThrowException() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        when(handler.loadObjects()).thenThrow(new IllegalAccessException("No bad dog! No biscuit!"));
        Database<Island> db = new Database<Island>(plugin, Island.class);
        db.loadObjects();
        Mockito.verify(handler).loadObjects();
        checkSevereLog("Could not load objects from database! Error: No bad dog! No biscuit!");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#loadObject(java.lang.String)}.
     * @throws IntrospectionException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Test
    public void testLoadObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        Database<Island> db = new Database<Island>(plugin, Island.class);
        String uniqueId = UUID.randomUUID().toString();
        assertEquals(island, db.loadObject(uniqueId));
        Mockito.verify(handler).loadObject(Mockito.eq(uniqueId));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#saveObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testSaveObject() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        Database<Island> db = new Database<Island>(plugin, Island.class);
        assertTrue(db.saveObject(island));
        Mockito.verify(handler).saveObject(Mockito.eq(island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#saveObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testSaveObjectFail() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        Mockito.doThrow(new IntrospectionException("No means no!")).when(handler).saveObject(Mockito.any(Island.class));
        Database<Island> db = new Database<Island>(plugin, Island.class);
        assertFalse(db.saveObject(island));
        checkSevereLog("Could not save object to database! Error: No means no!");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#objectExists(java.lang.String)}.
     */
    @Test
    public void testObjectExists() {
        when(handler.objectExists(Mockito.eq("test"))).thenReturn(false);
        when(handler.objectExists(Mockito.eq("exists"))).thenReturn(true);
        Database<Island> db = new Database<Island>(plugin, Island.class);
        assertFalse(db.objectExists("test"));
        assertTrue(db.objectExists("exists"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#deleteID(java.lang.String)}.
     */
    @Test
    public void testDeleteID() {
        Database<Island> db = new Database<Island>(plugin, Island.class);
        db.deleteID("test");
        Mockito.verify(handler).deleteID(Mockito.eq("test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#deleteObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testDeleteObject() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        Database<Island> db = new Database<Island>(plugin, Island.class);
        db.deleteObject(island);
        Mockito.verify(handler).deleteObject(Mockito.eq(island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#deleteObject(java.lang.Object)}.
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Test
    public void testDeleteObjectFail() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        Mockito.doThrow(new IllegalArgumentException("Wot?!")).when(handler).deleteObject(Mockito.any());
        Database<Island> db = new Database<Island>(plugin, Island.class);
        db.deleteObject(island);
        checkSevereLog("Could not delete object! Error: Wot?!");
    }


    /**
     * Test method for {@link world.bentobox.bentobox.database.Database#close()}.
     */
    @Test
    public void testClose() {
        Database<Island> db = new Database<Island>(plugin, Island.class);
        db.close();
        Mockito.verify(handler).close();
    }

}
