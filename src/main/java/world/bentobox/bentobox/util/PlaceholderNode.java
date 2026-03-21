package world.bentobox.bentobox.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.util.PlaceholderGrouper.PlaceholderItem;
import world.bentobox.bentobox.util.PlaceholderGrouper.Series;
import world.bentobox.bentobox.util.PlaceholderGrouper.Single;

/**
 * A node in a trie built from {@code _}-split placeholder identifiers.
 * <p>
 * Each node represents one segment of a placeholder key path. Nodes with exactly
 * one child and no leaf or series are merged (path-compressed) by
 * {@link #getDisplayChildren()} to avoid one-item intermediate folders.
 * </p>
 *
 * @since 3.2.0
 */
public class PlaceholderNode {

    /** The segment label for this node (may span multiple raw segments after path compression). */
    @NonNull
    private String label;

    /** The full path from root to this node (segments joined by {@code _}). */
    @NonNull
    private final String path;

    /** If this path is itself a registered placeholder. */
    @Nullable
    private Single leaf;

    /** If this path is the stem of a numeric series. */
    @Nullable
    private Series series;

    /** Child nodes, keyed by their raw segment string. */
    @NonNull
    private final Map<String, PlaceholderNode> children;

    // -------------------------------------------------------------------------
    // NodeType
    // -------------------------------------------------------------------------

    /**
     * Describes the content of a {@link PlaceholderNode} for GUI rendering.
     */
    public enum NodeType {
        /** A plain single placeholder with no children. */
        LEAF,
        /** A folder with child nodes but no direct placeholder. */
        FOLDER,
        /** Both a direct placeholder and a folder with child nodes. */
        LEAF_FOLDER,
        /** A collapsed numeric series with no children. */
        SERIES,
        /** Both a direct placeholder and a collapsed numeric series. */
        LEAF_SERIES
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private PlaceholderNode(@NonNull String label, @NonNull String path) {
        this.label = label;
        this.path = path;
        this.children = new LinkedHashMap<>();
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Builds a trie from the given list of {@link PlaceholderItem}s.
     *
     * @param items the grouped placeholder items to index.
     * @return the virtual root node whose children represent the top-level segments.
     */
    @NonNull
    public static PlaceholderNode buildTrie(@NonNull List<PlaceholderItem> items) {
        PlaceholderNode root = new PlaceholderNode("", "");
        for (PlaceholderItem item : items) {
            if (item instanceof Single single) {
                insertLeaf(root, single.key().split("_"), single);
            } else if (item instanceof Series ser) {
                insertSeries(root, ser.stem().split("_"), ser);
            }
        }
        return root;
    }

    private static void insertLeaf(PlaceholderNode root, String[] segments, Single single) {
        PlaceholderNode node = root;
        StringBuilder pathBuilder = new StringBuilder();
        for (String seg : segments) {
            if (!pathBuilder.isEmpty()) pathBuilder.append('_');
            pathBuilder.append(seg);
            String segPath = pathBuilder.toString();
            node = node.children.computeIfAbsent(seg, k -> new PlaceholderNode(seg, segPath));
        }
        node.leaf = single;
    }

    private static void insertSeries(PlaceholderNode root, String[] segments, Series ser) {
        PlaceholderNode node = root;
        StringBuilder pathBuilder = new StringBuilder();
        for (String seg : segments) {
            if (!pathBuilder.isEmpty()) pathBuilder.append('_');
            pathBuilder.append(seg);
            String segPath = pathBuilder.toString();
            node = node.children.computeIfAbsent(seg, k -> new PlaceholderNode(seg, segPath));
        }
        node.series = ser;
    }

    // -------------------------------------------------------------------------
    // Tree navigation
    // -------------------------------------------------------------------------

    /**
     * Returns the logical children of this node for display purposes, with path
     * compression applied: any child that is a pure FOLDER with exactly one child
     * has its single grandchild merged upward (recursively) to avoid empty intermediate
     * folders.
     *
     * @return sorted list of display children.
     */
    @NonNull
    public List<PlaceholderNode> getDisplayChildren() {
        List<PlaceholderNode> result = new ArrayList<>();
        for (PlaceholderNode child : children.values()) {
            result.add(compress(child));
        }
        result.sort(java.util.Comparator.comparing(n -> n.label));
        return result;
    }

    /**
     * Path-compresses a node: while it is a pure FOLDER with exactly one child,
     * merges the child's label and contents upward.
     */
    @NonNull
    private static PlaceholderNode compress(@NonNull PlaceholderNode node) {
        PlaceholderNode current = node;
        while (current.leaf == null && current.series == null && current.children.size() == 1) {
            PlaceholderNode onlyChild = current.children.values().iterator().next();
            // Create a merged virtual node combining labels, inheriting child's path and contents
            PlaceholderNode merged = new PlaceholderNode(
                    current.label + "_" + onlyChild.label,
                    onlyChild.path);
            merged.leaf = onlyChild.leaf;
            merged.series = onlyChild.series;
            merged.children.putAll(onlyChild.children);
            current = merged;
        }
        return current;
    }

    // -------------------------------------------------------------------------
    // Counting
    // -------------------------------------------------------------------------

    /**
     * Counts the total number of individual placeholder keys reachable from this node
     * (including its own leaf and all series rawKeys, recursively).
     */
    public int totalPlaceholderCount() {
        int count = 0;
        if (leaf != null) count++;
        if (series != null) count += series.rawKeys().size();
        for (PlaceholderNode child : children.values()) {
            count += child.totalPlaceholderCount();
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    @NonNull
    public String getLabel() { return label; }

    @NonNull
    public String getPath() { return path; }

    @Nullable
    public Single getLeaf() { return leaf; }

    @Nullable
    public Series getSeries() { return series; }

    @NonNull
    public Map<String, PlaceholderNode> getChildren() { return children; }

    public boolean hasChildren() { return !children.isEmpty(); }

    /**
     * Computes the {@link NodeType} based on this node's contents.
     */
    @NonNull
    public NodeType getType() {
        boolean hasLeaf = leaf != null;
        boolean hasSeries = series != null;
        boolean hasKids = !children.isEmpty();

        if (hasLeaf && hasSeries) return NodeType.LEAF_SERIES;
        if (hasLeaf && hasKids)   return NodeType.LEAF_FOLDER;
        if (hasLeaf)              return NodeType.LEAF;
        if (hasSeries)            return NodeType.SERIES;
        return NodeType.FOLDER;
    }
}
