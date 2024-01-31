package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
@PrepareForTest({ BentoBox.class, DatabaseSetup.class })
public class RanksManagerTest {

    private static AbstractDatabaseHandler<Object> h;

    @Mock
    public BentoBox plugin;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // This has to be done beforeClass otherwise the tests will interfere with each other
        h = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(h);
        when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
    }

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
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
