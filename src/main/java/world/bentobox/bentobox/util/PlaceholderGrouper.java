package world.bentobox.bentobox.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that groups a flat set of placeholder identifiers into
 * {@link PlaceholderItem} objects, collapsing numeric-suffix series (e.g.
 * {@code island_member_name_1} … {@code island_member_name_50}) into a single
 * {@link Series} item.
 * <p>
 * A <em>series</em> is defined as two or more placeholder identifiers that share
 * the same stem and differ only in a trailing {@code _<integer>} suffix.
 * Single-occurrence numeric-suffix keys are treated as plain {@link Single} items.
 * If a stem is itself also registered as a standalone key, both a {@link Single}
 * and a {@link Series} are emitted for that stem.
 * </p>
 *
 * @since 3.2.0
 */
public final class PlaceholderGrouper {

    private static final Pattern NUMERIC_SUFFIX = Pattern.compile("^(.+?)_(\\d+)$");

    private PlaceholderGrouper() {}

    // -------------------------------------------------------------------------
    // Public API types
    // -------------------------------------------------------------------------

    /**
     * Sealed supertype for items produced by {@link #group}.
     */
    public sealed interface PlaceholderItem permits Single, Series {
        /** The display key shown in the GUI or dump (may contain {@code {N}} for series). */
        String displayKey();
        /** A plain-English description, possibly blank. */
        String description();
    }

    /**
     * A single, standalone placeholder identifier.
     */
    public record Single(String key, String description) implements PlaceholderItem {
        @Override
        public String displayKey() { return key; }
    }

    /**
     * A collapsed numeric series of placeholder identifiers sharing a common stem.
     *
     * @param stem        the common prefix, e.g. {@code island_member_name}
     * @param min         the lowest observed numeric suffix
     * @param max         the highest observed numeric suffix
     * @param description a representative plain-English description (from the smallest-index member)
     * @param rawKeys     the complete list of raw placeholder identifiers in this series, sorted
     */
    public record Series(String stem, int min, int max, String description, List<String> rawKeys)
            implements PlaceholderItem {
        @Override
        public String displayKey() { return stem + "_{N}"; }
    }

    // -------------------------------------------------------------------------
    // Grouping logic
    // -------------------------------------------------------------------------

    /**
     * Groups a set of placeholder identifiers, collapsing numeric series.
     *
     * @param keys             the raw placeholder identifiers to group.
     * @param descriptionLookup a function that returns the description for a given key.
     * @return a list of {@link PlaceholderItem}s sorted by their {@link PlaceholderItem#displayKey()}.
     */
    public static List<PlaceholderItem> group(Set<String> keys,
            Function<String, Optional<String>> descriptionLookup) {
        // 1. Separate numeric-suffix keys from plain keys, grouping by stem
        Map<String, List<Integer>> stemToIndices = new HashMap<>();
        List<String> plainKeys = new ArrayList<>();

        for (String key : keys) {
            Matcher m = NUMERIC_SUFFIX.matcher(key);
            if (m.matches()) {
                String stem = m.group(1);
                int index = Integer.parseInt(m.group(2));
                stemToIndices.computeIfAbsent(stem, k -> new ArrayList<>()).add(index);
            } else {
                plainKeys.add(key);
            }
        }

        // 2. Build output list
        List<PlaceholderItem> result = new ArrayList<>();

        // Add plain keys as Singles
        for (String key : plainKeys) {
            String desc = descriptionLookup.apply(key).orElse("");
            result.add(new Single(key, desc));
        }

        // Process each stem group
        for (Map.Entry<String, List<Integer>> entry : stemToIndices.entrySet()) {
            String stem = entry.getKey();
            List<Integer> indices = entry.getValue().stream().sorted().toList();

            if (indices.size() == 1) {
                // Only one numeric entry → treat as plain Single
                String rawKey = stem + "_" + indices.getFirst();
                String desc = descriptionLookup.apply(rawKey).orElse("");
                result.add(new Single(rawKey, desc));
            } else {
                // Two or more → it's a Series
                int min = indices.getFirst();
                int max = indices.getLast();
                // Use description from the smallest-indexed member, stripping the " #N" suffix
                String firstKey = stem + "_" + min;
                String desc = descriptionLookup.apply(firstKey)
                        .map(PlaceholderGrouper::stripTrailingHashNumber)
                        .orElse("");
                List<String> rawKeys = indices.stream().map(i -> stem + "_" + i).toList();
                result.add(new Series(stem, min, max, desc, rawKeys));
            }
        }

        // 3. Sort by display key
        result.sort(java.util.Comparator.comparing(PlaceholderItem::displayKey));
        return result;
    }

    /**
     * Strips trailing {@code #<number>} patterns from a description string so that
     * a series description reads generically.
     * <p>
     * E.g. {@code "Name of island member #1 (ranked member or above)"}
     * → {@code "Name of island member (ranked member or above)"}
     * </p>
     */
    static String stripTrailingHashNumber(String description) {
        return description.replaceAll("\\s*#\\d+", "").trim();
    }
}
