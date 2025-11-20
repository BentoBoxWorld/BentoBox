package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Ranks;

/**
 * @author tastybento
 *
 */
public class RanksManagerTest extends AbstractCommonSetup {

    private static AbstractDatabaseHandler<Ranks> handler;

    @Mock
    public BentoBox plugin;

    @SuppressWarnings("unchecked")
    @BeforeAll
    public static void beforeClass() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // This has to be done beforeClass otherwise the tests will interfere with each other
        handler = mock(AbstractDatabaseHandler.class);
        // Database
        Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(eq(Ranks.class))).thenReturn(handler);
        when(handler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
    }

    private void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#addRank(java.lang.String, int)}.
     */
    @Test
    public void testAddRank() {
        assertTrue(RanksManager.getInstance().addRank("test.rank.reference", 750));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#removeRank(java.lang.String)}.
     */
    @Test
    public void testRemoveRank() {
        assertTrue(RanksManager.getInstance().addRank("test.rank.reference2", 650));
        assertTrue(RanksManager.getInstance().removeRank("test.rank.reference2"));
        // Second time should fail
        assertFalse(RanksManager.getInstance().removeRank("test.rank.reference2"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRankValue(java.lang.String)}.
     */
    @Test
    public void testGetRankValue() {
        RanksManager.getInstance().addRank("test.rank.reference.value", 600);
        assertEquals(600, RanksManager.getInstance().getRankValue("test.rank.reference.value"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRanks()}.
     */
    @Test
    public void testGetRanks() {
        Map<String, Integer> ranks = RanksManager.getInstance().getRanks();
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
        assertEquals(RanksManager.BANNED_RANK, RanksManager.getInstance().getRankUpValue(-20));
        assertEquals(RanksManager.VISITOR_RANK, RanksManager.getInstance().getRankUpValue(RanksManager.BANNED_RANK));
        assertEquals(RanksManager.COOP_RANK, RanksManager.getInstance().getRankUpValue(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.SUB_OWNER_RANK, RanksManager.getInstance().getRankUpValue(800));
        assertEquals(RanksManager.OWNER_RANK, RanksManager.getInstance().getRankUpValue(RanksManager.OWNER_RANK));
        assertEquals(RanksManager.OWNER_RANK, RanksManager.getInstance().getRankUpValue(2000));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRankDownValue(int)}.
     */
    @Test
    public void testGetPreviousRankValue() {
        // Lowest rank is Visitor
        assertEquals(RanksManager.VISITOR_RANK, RanksManager.getInstance().getRankDownValue(-20));
        assertEquals(RanksManager.VISITOR_RANK, RanksManager.getInstance().getRankDownValue(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.TRUSTED_RANK, RanksManager.getInstance().getRankDownValue(RanksManager.MEMBER_RANK));
        assertEquals(RanksManager.SUB_OWNER_RANK, RanksManager.getInstance().getRankDownValue(RanksManager.OWNER_RANK));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRank(int)}.
     */
    @Test
    public void testGetRank() {
        assertEquals(RanksManager.BANNED_RANK_REF, RanksManager.getInstance().getRank(RanksManager.BANNED_RANK));
        assertEquals(RanksManager.VISITOR_RANK_REF, RanksManager.getInstance().getRank(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.MEMBER_RANK_REF, RanksManager.getInstance().getRank(RanksManager.MEMBER_RANK));
        assertEquals(RanksManager.OWNER_RANK_REF, RanksManager.getInstance().getRank(RanksManager.OWNER_RANK));
        assertEquals("", RanksManager.getInstance().getRank(-999));
    }
}
