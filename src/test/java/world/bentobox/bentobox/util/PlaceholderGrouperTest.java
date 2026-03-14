package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.util.PlaceholderGrouper.PlaceholderItem;
import world.bentobox.bentobox.util.PlaceholderGrouper.Series;
import world.bentobox.bentobox.util.PlaceholderGrouper.Single;

/**
 * Tests for {@link PlaceholderGrouper}.
 */
class PlaceholderGrouperTest {

    /** A description lookup that always returns empty — used when descriptions are not under test. */
    private static final Function<String, Optional<String>> NO_DESC = k -> Optional.empty();

    // -------------------------------------------------------------------------
    // Basic grouping
    // -------------------------------------------------------------------------

    @Test
    void testGroupEmpty() {
        List<PlaceholderItem> result = PlaceholderGrouper.group(Set.of(), NO_DESC);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGroupSinglePlainKey() {
        List<PlaceholderItem> result = PlaceholderGrouper.group(Set.of("deaths"), NO_DESC);
        assertEquals(1, result.size());
        assertInstanceOf(Single.class, result.get(0));
        assertEquals("deaths", ((Single) result.get(0)).key());
        assertEquals("deaths", result.get(0).displayKey());
    }

    @Test
    void testGroupMultiSegmentPlainKey() {
        // Underscores in non-numeric keys must NOT trigger series detection
        List<PlaceholderItem> result = PlaceholderGrouper.group(Set.of("island_level"), NO_DESC);
        assertEquals(1, result.size());
        assertInstanceOf(Single.class, result.get(0));
        assertEquals("island_level", ((Single) result.get(0)).key());
    }

    // -------------------------------------------------------------------------
    // Numeric-suffix series detection
    // -------------------------------------------------------------------------

    @Test
    void testSingleNumericSuffixTreatedAsSingle() {
        // Only one key with a numeric suffix — not enough for a series
        List<PlaceholderItem> result = PlaceholderGrouper.group(Set.of("member_name_1"), NO_DESC);
        assertEquals(1, result.size());
        assertInstanceOf(Single.class, result.get(0));
        assertEquals("member_name_1", ((Single) result.get(0)).key());
    }

    @Test
    void testTwoNumericSuffixKeysFormSeries() {
        List<PlaceholderItem> result = PlaceholderGrouper.group(
                Set.of("member_1", "member_2"), NO_DESC);
        assertEquals(1, result.size());
        assertInstanceOf(Series.class, result.get(0));
        Series s = (Series) result.get(0);
        assertEquals("member", s.stem());
        assertEquals(1, s.min());
        assertEquals(2, s.max());
        assertEquals("member_{N}", s.displayKey());
    }

    @Test
    void testSeriesRawKeysSorted() {
        // rawKeys must be in ascending numeric order regardless of Set iteration order
        List<PlaceholderItem> result = PlaceholderGrouper.group(
                Set.of("member_3", "member_1", "member_2"), NO_DESC);
        Series s = (Series) result.get(0);
        assertEquals(List.of("member_1", "member_2", "member_3"), s.rawKeys());
    }

    @Test
    void testSeriesMinMaxTrackedCorrectly() {
        List<PlaceholderItem> result = PlaceholderGrouper.group(
                Set.of("item_5", "item_10", "item_1"), NO_DESC);
        Series s = (Series) result.get(0);
        assertEquals(1, s.min());
        assertEquals(10, s.max());
        assertEquals(3, s.rawKeys().size());
    }

    @Test
    void testLargeSeries() {
        var keys = new java.util.HashSet<String>();
        for (int i = 1; i <= 50; i++) keys.add("island_member_name_" + i);
        List<PlaceholderItem> result = PlaceholderGrouper.group(keys, NO_DESC);
        assertEquals(1, result.size());
        assertInstanceOf(Series.class, result.get(0));
        Series s = (Series) result.get(0);
        assertEquals("island_member_name", s.stem());
        assertEquals(1, s.min());
        assertEquals(50, s.max());
        assertEquals(50, s.rawKeys().size());
    }

    // -------------------------------------------------------------------------
    // Stem also registered as a plain key
    // -------------------------------------------------------------------------

    @Test
    void testStemAlsoRegisteredAsSingle() {
        // "island_count" is a plain placeholder AND the stem for a series
        Set<String> keys = Set.of("island_count", "island_count_1", "island_count_2");
        List<PlaceholderItem> result = PlaceholderGrouper.group(keys, NO_DESC);
        assertEquals(2, result.size());
        long singles = result.stream().filter(i -> i instanceof Single).count();
        long series  = result.stream().filter(i -> i instanceof Series).count();
        assertEquals(1, singles, "expected exactly one Single");
        assertEquals(1, series,  "expected exactly one Series");
    }

    // -------------------------------------------------------------------------
    // Sorting
    // -------------------------------------------------------------------------

    @Test
    void testOutputSortedByDisplayKey() {
        Set<String> keys = Set.of("zebra", "apple", "mango_1", "mango_2");
        List<PlaceholderItem> result = PlaceholderGrouper.group(keys, NO_DESC);
        // "apple" < "mango_{N}" < "zebra"
        assertEquals("apple",    result.get(0).displayKey());
        assertEquals("mango_{N}", result.get(1).displayKey());
        assertEquals("zebra",    result.get(2).displayKey());
    }

    // -------------------------------------------------------------------------
    // Description look-up
    // -------------------------------------------------------------------------

    @Test
    void testDescriptionLookupUsedForSingles() {
        List<PlaceholderItem> result = PlaceholderGrouper.group(
                Set.of("deaths"),
                k -> Optional.of("Number of deaths for " + k));
        assertEquals("Number of deaths for deaths", result.get(0).description());
    }

    @Test
    void testSeriesDescriptionTakenFromSmallestIndex() {
        Set<String> keys = Set.of("member_1", "member_2", "member_3");
        List<PlaceholderItem> result = PlaceholderGrouper.group(keys, k -> switch (k) {
            case "member_1" -> Optional.of("Name of member #1");
            case "member_2" -> Optional.of("Name of member #2");
            default         -> Optional.empty();
        });
        Series s = (Series) result.get(0);
        // "#1" should have been stripped from the description
        assertEquals("Name of member", s.description());
    }

    @Test
    void testSeriesDescriptionEmptyWhenNoDescriptionRegistered() {
        List<PlaceholderItem> result = PlaceholderGrouper.group(
                Set.of("item_1", "item_2"), NO_DESC);
        assertEquals("", ((Series) result.get(0)).description());
    }

    // -------------------------------------------------------------------------
    // stripTrailingHashNumber (package-private helper)
    // -------------------------------------------------------------------------

    @Test
    void testStripTrailingHashNumberSimple() {
        assertEquals("Name of member",
                PlaceholderGrouper.stripTrailingHashNumber("Name of member #1"));
    }

    @Test
    void testStripTrailingHashNumberWithParenthetical() {
        assertEquals("Name of member (ranked)",
                PlaceholderGrouper.stripTrailingHashNumber("Name of member #1 (ranked)"));
    }

    @Test
    void testStripTrailingHashNumberNoNumber() {
        assertEquals("No number here",
                PlaceholderGrouper.stripTrailingHashNumber("No number here"));
    }

    @Test
    void testStripTrailingHashNumberOnlyNumber() {
        assertEquals("",
                PlaceholderGrouper.stripTrailingHashNumber("  #42  "));
    }

    @Test
    void testStripTrailingHashNumberMultipleNumbers() {
        // All " #<digit>" occurrences are removed
        assertEquals("Member of island",
                PlaceholderGrouper.stripTrailingHashNumber("Member #1 of island #2"));
    }
}
