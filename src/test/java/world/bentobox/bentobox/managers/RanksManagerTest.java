/**
 * 
 */
package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;

/**
 * @author tastybento
 *
 */
public class RanksManagerTest {
    
    public static RanksManager ranksManager;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        BentoBox plugin = mock(BentoBox.class);
        Settings settings = mock(Settings.class);
        // Blank ranks for now
        Map<String, Integer> customRanks = new HashMap<>();
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getCustomRanks()).thenReturn(customRanks);
        
        ranksManager = new RanksManager(plugin);
        
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#addRank(java.lang.String, int)}.
     */
    @Test
    public void testAddRank() {
        assertTrue(ranksManager.addRank("test.rank.reference", 750));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#removeRank(java.lang.String)}.
     */
    @Test
    public void testRemoveRank() {
        assertTrue(ranksManager.addRank("test.rank.reference2", 650));
        assertTrue(ranksManager.removeRank("test.rank.reference2"));
        // Second time should fail
        assertFalse(ranksManager.removeRank("test.rank.reference2"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRankValue(java.lang.String)}.
     */
    @Test
    public void testGetRankValue() {
        ranksManager.addRank("test.rank.reference.value", 600);
        assertEquals(600, ranksManager.getRankValue("test.rank.reference.value"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRanks()}.
     */
    @Test
    public void testGetRanks() {
        Map<String, Integer> ranks = ranksManager.getRanks();
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
        assertEquals(RanksManager.BANNED_RANK, ranksManager.getRankUpValue(-20));
        assertEquals(RanksManager.VISITOR_RANK, ranksManager.getRankUpValue(RanksManager.BANNED_RANK));
        assertEquals(RanksManager.COOP_RANK, ranksManager.getRankUpValue(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.SUB_OWNER_RANK, ranksManager.getRankUpValue(RanksManager.MEMBER_RANK));
        assertEquals(RanksManager.OWNER_RANK, ranksManager.getRankUpValue(RanksManager.OWNER_RANK));
        assertEquals(RanksManager.OWNER_RANK, ranksManager.getRankUpValue(2000));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRankDownValue(int)}.
     */
    @Test
    public void testGetPreviousRankValue() {
        // Lowest rank is Visitor
        assertEquals(RanksManager.VISITOR_RANK, ranksManager.getRankDownValue(-20));
        assertEquals(RanksManager.VISITOR_RANK, ranksManager.getRankDownValue(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.TRUSTED_RANK, ranksManager.getRankDownValue(RanksManager.MEMBER_RANK));
        assertEquals(RanksManager.SUB_OWNER_RANK, ranksManager.getRankDownValue(RanksManager.OWNER_RANK));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.RanksManager#getRank(int)}.
     */
    @Test
    public void testGetRank() {
        assertEquals(RanksManager.BANNED_RANK_REF, ranksManager.getRank(RanksManager.BANNED_RANK));
        assertEquals(RanksManager.VISITOR_RANK_REF, ranksManager.getRank(RanksManager.VISITOR_RANK));
        assertEquals(RanksManager.MEMBER_RANK_REF, ranksManager.getRank(RanksManager.MEMBER_RANK));
        assertEquals(RanksManager.OWNER_RANK_REF, ranksManager.getRank(RanksManager.OWNER_RANK));
        assertEquals("", ranksManager.getRank(-999));
    }

}
