package world.bentobox.bentobox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Ranks;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public abstract class RanksManagerBeforeClassTest extends AbstractCommonSetup {

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
    public RanksManager rm;
    protected AbstractDatabaseHandler<Ranks> ranksHandler;
    protected AbstractDatabaseHandler<TeamInvite> invitesHandler;
    private MockedStatic<DatabaseSetup> mockedDatabaseSetup;

    protected Object savedObject;
    protected MockedStatic<RanksManager> mockedRanksManager;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Clear any lingering database
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));

        // This has to be done beforeClass otherwise the tests will interfere with each other
        ranksHandler = (AbstractDatabaseHandler<Ranks>)mock(AbstractDatabaseHandler.class);
        invitesHandler = (AbstractDatabaseHandler<TeamInvite>)mock(AbstractDatabaseHandler.class);
        // Database
        mockedDatabaseSetup = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockedDatabaseSetup.when(() -> DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(eq(Ranks.class))).thenReturn(ranksHandler);
        when(ranksHandler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(dbSetup.getHandler(eq(TeamInvite.class))).thenReturn(invitesHandler);
        when(invitesHandler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
        
        // Capture the parameter passed to saveObject() and store it in savedObject
        doAnswer(invocation -> {
            savedObject = invocation.getArgument(0);
            return CompletableFuture.completedFuture(true);
        }).when(ranksHandler).saveObject(any());
        doAnswer(invocation -> {
            savedObject = invocation.getArgument(0);
            return CompletableFuture.completedFuture(true);
        }).when(invitesHandler).saveObject(any());

        // Now when loadObject() is called, return the savedObject
        when(ranksHandler.loadObject(any())).thenAnswer(invocation -> savedObject);
        when(invitesHandler.loadObject(any())).thenAnswer(invocation -> savedObject);
        
        // Delete object
        doAnswer(invocation -> {
            savedObject = null;
            return null;
        }).when(ranksHandler).deleteObject(any());
        doAnswer(invocation -> {
            savedObject = null;
            return null;
        }).when(invitesHandler).deleteObject(any());
        
        doAnswer(invocation -> {
            savedObject = null;
            return null;
        }).when(ranksHandler).deleteID(anyString());
        doAnswer(invocation -> {
            savedObject = null;
            return null;
        }).when(invitesHandler).deleteID(anyString());

        // RanksManager
        mockedRanksManager = Mockito.mockStatic(RanksManager.class, Mockito.RETURNS_MOCKS);
        mockedRanksManager.when(() -> RanksManager.getInstance()).thenReturn(rm);
        when(rm.getRanks()).thenReturn(DEFAULT_RANKS);
        when(rm.getRank(anyInt())).thenReturn("");
        // Clear savedObject
        savedObject = null;
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
        ranksHandler.close();
        invitesHandler.close();
    }

    private void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }

    }

}
