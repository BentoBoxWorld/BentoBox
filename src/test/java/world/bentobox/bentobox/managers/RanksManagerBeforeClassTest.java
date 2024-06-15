package world.bentobox.bentobox.managers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, DatabaseSetup.class, RanksManager.class, Bukkit.class })
public abstract class RanksManagerBeforeClassTest {

    // Constants that define the hard coded rank values
    public static final String ADMIN_RANK_REF = "ranks.admin";
    public static final String MOD_RANK_REF = "ranks.mod";
    public static final String OWNER_RANK_REF = "ranks.owner";
    public static final String SUB_OWNER_RANK_REF = "ranks.sub-owner";
    public static final String MEMBER_RANK_REF = "ranks.member";
    public static final String TRUSTED_RANK_REF = "ranks.trusted";
    public static final String COOP_RANK_REF = "ranks.coop";
    public static final String VISITOR_RANK_REF = "ranks.visitor";
    public static final String BANNED_RANK_REF = "ranks.banned";
    public static final int ADMIN_RANK = 10000;
    public static final int MOD_RANK = 5000;
    public static final int OWNER_RANK = 1000;
    public static final int SUB_OWNER_RANK = 900;
    public static final int MEMBER_RANK = 500;
    public static final int TRUSTED_RANK = 400;
    public static final int COOP_RANK = 200;
    public static final int VISITOR_RANK = 0;
    public static final int BANNED_RANK = -1;

    // The store of ranks
    public static final Map<String, Integer> DEFAULT_RANKS = Map.of(ADMIN_RANK_REF, ADMIN_RANK, MOD_RANK_REF, MOD_RANK,
            OWNER_RANK_REF, OWNER_RANK, SUB_OWNER_RANK_REF, SUB_OWNER_RANK, MEMBER_RANK_REF, MEMBER_RANK,
            TRUSTED_RANK_REF, TRUSTED_RANK, COOP_RANK_REF, COOP_RANK, VISITOR_RANK_REF, VISITOR_RANK, BANNED_RANK_REF,
            BANNED_RANK);

    @Mock
    public BentoBox plugin;
    @Mock
    public RanksManager rm;

    protected static AbstractDatabaseHandler<Object> h;
    protected static Object savedObject;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() throws IllegalAccessException, InvocationTargetException, IntrospectionException,
            InstantiationException, ClassNotFoundException, NoSuchMethodException {
        // This has to be done beforeClass otherwise the tests will interfere with each
        // other
        h = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(h);
        //when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
        // Capture the parameter passed to saveObject() and store it in savedObject
        doAnswer(invocation -> {
            savedObject = invocation.getArgument(0);
            return CompletableFuture.completedFuture(true);
        }).when(h).saveObject(any());

        // Now when loadObject() is called, return the savedObject
        when(h.loadObject(any())).thenAnswer(invocation -> savedObject);

        // Delete object
        doAnswer(invocation -> {
            savedObject = null;
            return null;
        }).when(h).deleteObject(any());
        
        doAnswer(invocation -> {
            savedObject = null;
            return null;
        }).when(h).deleteID(anyString());

    }

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getBukkitVersion()).thenReturn("");
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // RanksManager
        PowerMockito.mockStatic(RanksManager.class, Mockito.RETURNS_MOCKS);
        when(RanksManager.getInstance()).thenReturn(rm);
        when(rm.getRanks()).thenReturn(DEFAULT_RANKS);
        when(rm.getRank(anyInt())).thenReturn("");
        // Clear savedObject
        savedObject = null;
    }

    @After
    public void tearDown() throws IOException {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
    }

    private void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }

    }

}
