package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.util.PlaceholderGrouper.PlaceholderItem;
import world.bentobox.bentobox.util.PlaceholderGrouper.Series;
import world.bentobox.bentobox.util.PlaceholderGrouper.Single;
import world.bentobox.bentobox.util.PlaceholderNode.NodeType;

/**
 * Tests for {@link PlaceholderNode}.
 */
class PlaceholderNodeTest {

    // -------------------------------------------------------------------------
    // buildTrie — basic cases
    // -------------------------------------------------------------------------

    @Test
    void testBuildTrieEmpty() {
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of());
        assertTrue(root.getDisplayChildren().isEmpty());
        assertEquals(NodeType.FOLDER, root.getType());
        assertEquals(0, root.totalPlaceholderCount());
    }

    @Test
    void testBuildTrieSingleSegmentLeaf() {
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of(new Single("deaths", "desc")));
        List<PlaceholderNode> children = root.getDisplayChildren();
        assertEquals(1, children.size());
        PlaceholderNode node = children.get(0);
        assertEquals(NodeType.LEAF, node.getType());
        assertEquals("deaths", node.getLabel());
        assertEquals("deaths", node.getPath());
        assertNotNull(node.getLeaf());
        assertEquals("deaths", node.getLeaf().key());
        assertEquals("desc", node.getLeaf().description());
    }

    @Test
    void testBuildTrieSeriesNode() {
        Series series = new Series("member", 1, 3, "desc",
                List.of("member_1", "member_2", "member_3"));
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of(series));
        List<PlaceholderNode> children = root.getDisplayChildren();
        assertEquals(1, children.size());
        PlaceholderNode node = children.get(0);
        assertEquals(NodeType.SERIES, node.getType());
        assertEquals("member", node.getLabel());
        assertEquals(series, node.getSeries());
        assertNull(node.getLeaf());
    }

    // -------------------------------------------------------------------------
    // Path compression
    // -------------------------------------------------------------------------

    @Test
    void testSingleSegmentChainCompressedToLeaf() {
        // "a_b_c" splits into 3 segments — all single-child FOLDER hops compress into one leaf
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of(new Single("a_b_c", "")));
        List<PlaceholderNode> children = root.getDisplayChildren();
        assertEquals(1, children.size());
        PlaceholderNode node = children.get(0);
        assertEquals("a_b_c", node.getLabel());
        assertEquals("a_b_c", node.getPath());
        assertEquals(NodeType.LEAF, node.getType());
    }

    @Test
    void testCompressionStopsAtMultipleChildren() {
        // "a_b" and "a_c" share parent "a", which has 2 children → "a" is NOT compressed away
        List<PlaceholderItem> items = List.of(new Single("a_b", ""), new Single("a_c", ""));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        List<PlaceholderNode> rootChildren = root.getDisplayChildren();
        assertEquals(1, rootChildren.size());
        PlaceholderNode aNode = rootChildren.get(0);
        assertEquals("a", aNode.getLabel());
        assertEquals(NodeType.FOLDER, aNode.getType());
        // Both "b" and "c" are reachable as direct display children
        assertEquals(2, aNode.getDisplayChildren().size());
    }

    @Test
    void testCompressionStopsAtLeafNode() {
        // "a" is itself a registered placeholder AND has a child "a_b" → must NOT be merged away
        List<PlaceholderItem> items = List.of(new Single("a", "top"), new Single("a_b", "child"));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        List<PlaceholderNode> children = root.getDisplayChildren();
        assertEquals(1, children.size());
        PlaceholderNode aNode = children.get(0);
        assertEquals("a", aNode.getLabel());
        assertEquals(NodeType.LEAF_FOLDER, aNode.getType());
        assertNotNull(aNode.getLeaf());
        assertEquals("top", aNode.getLeaf().description());
    }

    @Test
    void testCompressionStopsAtSeriesNode() {
        // A series node has content, so its chain should not be compressed through it
        Series series = new Series("a_b", 1, 2, "", List.of("a_b_1", "a_b_2"));
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of(series));
        List<PlaceholderNode> children = root.getDisplayChildren();
        assertEquals(1, children.size());
        // "a" → "b" compressed to "a_b"; stops because "b" has a series
        assertEquals("a_b", children.get(0).getLabel());
        assertEquals(NodeType.SERIES, children.get(0).getType());
    }

    @Test
    void testPartialCompressionWhenDeepChainFansOut() {
        // "a_b_c" and "a_b_d" → "a_b" is a 2-child folder (not compressed), "a" compressed into it
        List<PlaceholderItem> items = List.of(
                new Single("a_b_c", ""), new Single("a_b_d", ""));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        List<PlaceholderNode> children = root.getDisplayChildren();
        assertEquals(1, children.size());
        PlaceholderNode abNode = children.get(0);
        assertEquals("a_b", abNode.getLabel()); // "a" compressed with its only child "b"
        assertEquals(NodeType.FOLDER, abNode.getType());
        List<PlaceholderNode> grandchildren = abNode.getDisplayChildren();
        assertEquals(2, grandchildren.size());
        assertEquals("c", grandchildren.get(0).getLabel());
        assertEquals("d", grandchildren.get(1).getLabel());
    }

    // -------------------------------------------------------------------------
    // NodeType
    // -------------------------------------------------------------------------

    @Test
    void testNodeTypeLeaf() {
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of(new Single("x", "")));
        assertEquals(NodeType.LEAF, root.getDisplayChildren().get(0).getType());
    }

    @Test
    void testNodeTypeFolder() {
        List<PlaceholderItem> items = List.of(new Single("a_x", ""), new Single("a_y", ""));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        assertEquals(NodeType.FOLDER, root.getDisplayChildren().get(0).getType());
    }

    @Test
    void testNodeTypeSeries() {
        Series s = new Series("s", 1, 2, "", List.of("s_1", "s_2"));
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of(s));
        assertEquals(NodeType.SERIES, root.getDisplayChildren().get(0).getType());
    }

    @Test
    void testNodeTypeLeafFolder() {
        List<PlaceholderItem> items = List.of(new Single("a", ""), new Single("a_x", ""));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        assertEquals(NodeType.LEAF_FOLDER, root.getDisplayChildren().get(0).getType());
    }

    @Test
    void testNodeTypeLeafSeries() {
        // Same path is both a plain Single and the stem of a Series
        List<PlaceholderItem> items = List.of(
                new Single("count", "total"),
                new Series("count", 1, 2, "", List.of("count_1", "count_2")));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        PlaceholderNode node = root.getDisplayChildren().get(0);
        assertEquals(NodeType.LEAF_SERIES, node.getType());
        assertNotNull(node.getLeaf());
        assertNotNull(node.getSeries());
    }

    // -------------------------------------------------------------------------
    // Sorting
    // -------------------------------------------------------------------------

    @Test
    void testDisplayChildrenSortedAlphabetically() {
        List<PlaceholderItem> items = List.of(
                new Single("zebra", ""), new Single("apple", ""), new Single("mango", ""));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        List<PlaceholderNode> children = root.getDisplayChildren();
        assertEquals("apple", children.get(0).getLabel());
        assertEquals("mango", children.get(1).getLabel());
        assertEquals("zebra", children.get(2).getLabel());
    }

    // -------------------------------------------------------------------------
    // totalPlaceholderCount
    // -------------------------------------------------------------------------

    @Test
    void testTotalCountLeaf() {
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of(new Single("deaths", "")));
        assertEquals(1, root.totalPlaceholderCount());
    }

    @Test
    void testTotalCountSeries() {
        Series s = new Series("m", 1, 5, "", List.of("m_1", "m_2", "m_3", "m_4", "m_5"));
        PlaceholderNode root = PlaceholderNode.buildTrie(List.of(s));
        assertEquals(5, root.totalPlaceholderCount());
    }

    @Test
    void testTotalCountMixed() {
        // 1 plain leaf + series of 3 = 4 total
        List<PlaceholderItem> items = List.of(
                new Single("deaths", ""),
                new Series("member", 1, 3, "", List.of("member_1", "member_2", "member_3")));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        assertEquals(4, root.totalPlaceholderCount());
    }

    @Test
    void testTotalCountRecursive() {
        // Multiple levels: "a_x" (leaf), "a_y_1", "a_y_2" (series) — all under "a" subtree
        List<PlaceholderItem> items = List.of(
                new Single("a_x", ""),
                new Series("a_y", 1, 2, "", List.of("a_y_1", "a_y_2")));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);
        // root total = 1 (a_x) + 2 (series) = 3
        assertEquals(3, root.totalPlaceholderCount());
        // "a" folder also = 3
        PlaceholderNode aNode = root.getDisplayChildren().get(0);
        assertEquals(3, aNode.totalPlaceholderCount());
    }

    // -------------------------------------------------------------------------
    // Navigation — getDisplayChildren on a drilled-into node
    // -------------------------------------------------------------------------

    @Test
    void testDrillIntoCompressedNode() {
        // Build: root → "island" → "member" → ["name" SERIES, "rank" SERIES]
        List<PlaceholderItem> items = List.of(
                new Series("island_member_name", 1, 3, "", List.of("island_member_name_1", "island_member_name_2", "island_member_name_3")),
                new Series("island_member_rank", 1, 3, "", List.of("island_member_rank_1", "island_member_rank_2", "island_member_rank_3")));
        PlaceholderNode root = PlaceholderNode.buildTrie(items);

        // Root should show one compressed FOLDER "island_member"
        List<PlaceholderNode> rootChildren = root.getDisplayChildren();
        assertEquals(1, rootChildren.size());
        PlaceholderNode islandMember = rootChildren.get(0);
        assertEquals("island_member", islandMember.getLabel());
        assertEquals(NodeType.FOLDER, islandMember.getType());

        // Drilling into "island_member" shows the two SERIES nodes
        List<PlaceholderNode> drillChildren = islandMember.getDisplayChildren();
        assertEquals(2, drillChildren.size());
        assertEquals("name", drillChildren.get(0).getLabel());
        assertEquals("rank", drillChildren.get(1).getLabel());
        assertEquals(NodeType.SERIES, drillChildren.get(0).getType());
        assertEquals(NodeType.SERIES, drillChildren.get(1).getType());

        // Series nodes retain their full stems
        assertEquals("island_member_name", drillChildren.get(0).getSeries().stem());
        assertEquals("island_member_rank", drillChildren.get(1).getSeries().stem());
    }
}
