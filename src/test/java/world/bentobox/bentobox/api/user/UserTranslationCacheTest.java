package world.bentobox.bentobox.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.cache.Cache;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Correctness and micro-benchmark coverage for the legacy-conversion cache that backs
 * {@link User#getTranslation(String, String...)}.
 * <p>
 * The cache stores the result of the pure MiniMessage&nbsp;&rarr;&nbsp;legacy conversion keyed on
 * the raw translated string. These tests prove the cache returns exactly the same output as the
 * uncached path, and quantify how much cheaper a warm (cached) translation is than a cold one —
 * which is the dominant cost when a settings GUI rebuilds its ~30 flags (re-translating the same
 * templates and rank names many times per rebuild).
 *
 * @author tastybento
 */
class UserTranslationCacheTest extends CommonTestSetup {

    private User user;

    /** A representative spread of translated strings: plain, MiniMessage, legacy, mixed, multiline. */
    private static final String[] SAMPLES = {
            "Basic",
            "<gold>Advanced mode</gold>",
            "<gray>Click to switch to the next mode</gray>",
            "&aVisitor&r can now break blocks",
            "<green>[description]</green>\n<gray>Members and above may use this</gray>",
            "<red>Blocked</red> for <yellow>Visitor</yellow>",
            "&e&lSettings&r\n&7Toggle island protections",
            "<bold><aqua>Owner</aqua></bold> only",
            "Coop can use this",
            "<dark_purple>Trusted</dark_purple> and above" };

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = User.getInstance(mockPlayer);
        clearCache();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        clearCache();
        super.tearDown();
    }

    @SuppressWarnings("unchecked")
    private Cache<String, String> getCache() throws Exception {
        Field f = User.class.getDeclaredField("LEGACY_CACHE");
        f.setAccessible(true);
        return (Cache<String, String>) f.get(null);
    }

    private void clearCache() throws Exception {
        getCache().invalidateAll();
    }

    /**
     * The cached translation must be byte-for-byte identical to the uncached conversion for every
     * kind of input (plain, MiniMessage, legacy, mixed, multiline). A miss and a subsequent hit
     * must both equal {@link User#computeLegacy(String)}.
     */
    @Test
    void testCachedResultMatchesUncached() throws Exception {
        for (String raw : SAMPLES) {
            String expected = User.computeLegacy(raw);
            clearCache();
            String miss = user.getTranslation(raw); // cold - computes and caches
            String hit = user.getTranslation(raw); // warm - served from cache
            assertEquals(expected, miss, "cold translation differs for: " + raw);
            assertEquals(expected, hit, "cached translation differs for: " + raw);
        }
    }

    /**
     * Quantifies the conversion cost the cache removes. A cold conversion runs a full
     * MiniMessage&nbsp;&rarr;&nbsp;legacy parse+serialize ({@link User#computeLegacy(String)}); a
     * warm one ({@code convertToLegacy} with the entry present) is a concurrent-map lookup. This
     * is the exact work a settings-panel rebuild repeats for every flag it re-translates.
     * <p>
     * The measurement isolates the conversion step (not the whole {@code getTranslation}) because
     * in this unit-test harness the surrounding {@code translate()} calls run against Mockito
     * mocks whose per-invocation overhead would swamp the real, tiny lookup cost. The conversion
     * step uses no mocks, so its numbers reflect production. A loose 5x margin keeps the assertion
     * non-flaky under CI load; the real gap is far larger. Numbers are printed for the record.
     */
    @Test
    void testCacheIsFasterThanRecomputing() throws Exception {
        final int distinct = SAMPLES.length;
        final int reps = 20_000;

        Method convert = User.class.getDeclaredMethod("convertToLegacy", String.class);
        convert.setAccessible(true);

        // Warm the JIT for both paths.
        for (int i = 0; i < 5_000; i++) {
            User.computeLegacy(SAMPLES[i % distinct]);
        }
        clearCache();
        for (String s : SAMPLES) {
            convert.invoke(user, s); // populate the cache
        }
        for (int i = 0; i < 5_000; i++) {
            convert.invoke(user, SAMPLES[i % distinct]);
        }

        // COLD: every call recomputes - this is what the old code did on every getTranslation.
        long coldNanos = 0;
        {
            long start = System.nanoTime();
            for (int i = 0; i < reps; i++) {
                blackhole(User.computeLegacy(SAMPLES[i % distinct]));
            }
            coldNanos = System.nanoTime() - start;
        }

        // WARM: the cache is populated, so every call is a lookup (the real production hit path).
        long warmNanos = 0;
        {
            long start = System.nanoTime();
            for (int i = 0; i < reps; i++) {
                blackhole((String) convert.invoke(user, SAMPLES[i % distinct]));
            }
            warmNanos = System.nanoTime() - start;
        }

        double coldPerCallUs = coldNanos / (double) reps / 1_000d;
        double warmPerCallUs = warmNanos / (double) reps / 1_000d;
        double speedup = coldNanos / (double) warmNanos;

        // Project onto a settings-panel rebuild: ~500 conversions, but only a handful are distinct
        // (templates + rank names repeated across ~30 flags). Cold pays for all 500; warm pays the
        // full conversion once per distinct string, then lookups.
        final int convsPerRebuild = 500;
        double coldRebuildMs = convsPerRebuild * coldPerCallUs / 1_000d;
        double warmRebuildMs = (distinct * coldPerCallUs + (convsPerRebuild - distinct) * warmPerCallUs) / 1_000d;

        System.out.printf("%n[Translation-conversion cache benchmark] %d distinct strings, %d reps%n",
                distinct, reps);
        System.out.printf("  cold (recompute): %.3f us/conversion%n", coldPerCallUs);
        System.out.printf("  warm (cached):    %.4f us/conversion%n", warmPerCallUs);
        System.out.printf("  per-conversion speedup: %.0fx%n", speedup);
        System.out.printf("  projected %d-conversion rebuild: %.2f ms cold  ->  %.2f ms warm (%.0fx)%n",
                convsPerRebuild, coldRebuildMs, warmRebuildMs, coldRebuildMs / warmRebuildMs);

        assertTrue(speedup > 5.0,
                () -> String.format("Expected cached conversion to be >5x faster, was %.0fx (cold %.3fus, warm %.4fus)",
                        speedup, coldPerCallUs, warmPerCallUs));
    }

    /** Prevents the JIT from optimising away the measured work. */
    private static void blackhole(String s) {
        if (s == null) {
            throw new AssertionError("unexpected null translation");
        }
    }
}
