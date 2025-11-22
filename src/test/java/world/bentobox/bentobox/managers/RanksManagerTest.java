package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Ranks;

/**
 * @author tastybento
 *
 */
public class RanksManagerTest extends CommonTestSetup {

    private  AbstractDatabaseHandler<Ranks> handler;

    @Mock
    public BentoBox plugin;
    
    private RanksManager rm;

    private MockedStatic<DatabaseSetup> mockedDatabaseSetup;

    @SuppressWarnings("unchecked")
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Clear any lingering database
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));

        // This has to be done beforeClass otherwise the tests will interfere with each other
        handler = (AbstractDatabaseHandler<Ranks>)mock(AbstractDatabaseHandler.class);
        // Database
        mockedDatabaseSetup = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockedDatabaseSetup.when(() -> DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(eq(Ranks.class))).thenReturn(handler);
        when(handler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));

        rm = new RanksManager();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        mockedDatabaseSetup.closeOnDemand();
        handler.close();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#addRank(java.lang.String, int)}.
     */
    @Test
    public void testAddRank() {
        assertTrue(rm.addRank("test.rank.reference", 750));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#removeRank(java.lang.String)}.
     */
    @Test
    public void testRemoveRank() {
        assertTrue(rm.addRank("test.rank.reference2", 650));
        assertTrue(rm.removeRank("test.rank.reference2"));
        // Second time should fail
        assertFalse(rm.removeRank("test.rank.reference2"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRankValue(java.lang.String)}.
     */
    @Test
    public void testGetRankValue() {
        rm.addRank("test.rank.reference.value", 600);
        assertEquals(600, rm.getRankValue("test.rank.reference.value"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRanks()}.
     */
    @Test
    public void testGetRanks() {
        Map<String, Integer> ranks = rm.getRanks();
        assertTrue(ranks.containsKey(RanksManager.BANNED_RANK_REF));
        assertTrue(ranks.containsKey(RanksManager.VISITOR_RANK_REF));
        assertTrue(ranks.containsKey(RanksManager.MEMBER_RANK_REF));
        assertTrue(ranks.containsKey(RanksManager.OWNER_RANK_REF));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRankUpValue(int)}.
     */
    @Test
    public void testGetNextRankValue() {
        assertEquals(RanksManager.BANNED_RANK, rm.getRankUpValue(-20));
        assertEquals(RanksManager.VISITOR_RANK, rm.getRankUpValue(RanksManager.BANNED_RANK));
        assertEquals(RanksManager.COOP_RANK, rm.getRankUpValue(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.SUB_OWNER_RANK, rm.getRankUpValue(800));
        assertEquals(RanksManager.OWNER_RANK, rm.getRankUpValue(RanksManager.OWNER_RANK));
        assertEquals(RanksManager.OWNER_RANK, rm.getRankUpValue(2000));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRankDownValue(int)}.
     */
    @Test
    public void testGetPreviousRankValue() {
        // Lowest rank is Visitor
        assertEquals(RanksManager.VISITOR_RANK, rm.getRankDownValue(-20));
        assertEquals(RanksManager.VISITOR_RANK, rm.getRankDownValue(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.TRUSTED_RANK, rm.getRankDownValue(RanksManager.MEMBER_RANK));
        assertEquals(RanksManager.SUB_OWNER_RANK, rm.getRankDownValue(RanksManager.OWNER_RANK));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRank(int)}.
     */
    @Test
    @Disabled
    public void testGetRank() {
        assertEquals(RanksManager.BANNED_RANK_REF, rm.getRank(RanksManager.BANNED_RANK));
        assertEquals(RanksManager.VISITOR_RANK_REF, rm.getRank(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.MEMBER_RANK_REF, rm.getRank(RanksManager.MEMBER_RANK));
        assertEquals(RanksManager.OWNER_RANK_REF, rm.getRank(RanksManager.OWNER_RANK));
        assertEquals("", rm.getRank(-999));
    }
}
