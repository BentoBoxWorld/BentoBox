package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.block.Biome;
import org.bukkit.block.sign.Side;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;

/**
 * Tests for {@link BlueprintFill}
 */
class BlueprintFillTest {

    private static final String AIR = "minecraft:air";
    private static final String WATER = "minecraft:water[level=0]";
    private static final String STONE = "minecraft:stone";

    /**
     * Builds a blueprint whose blocks fill the box from (minX,minY,minZ) to (maxX,maxY,maxZ),
     * using the given default block data except where overridden
     */
    private static Blueprint fullBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String def,
            Map<Vector, BlueprintBlock> overrides) {
        Map<Vector, BlueprintBlock> blocks = new LinkedHashMap<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Vector v = new Vector(x, y, z);
                    blocks.put(v, overrides.getOrDefault(v, new BlueprintBlock(def)));
                }
            }
        }
        Blueprint bp = new Blueprint();
        bp.setBlocks(blocks);
        return bp;
    }

    /**
     * Expands a blueprint into what a paste would place: every explicit block plus every fill position
     */
    private static Map<Vector, String> pasted(Blueprint bp) {
        Map<Vector, String> result = new HashMap<>();
        Iterator<Entry<Vector, BlueprintBlock>> it = BlueprintFill.fillIterator(bp);
        while (it.hasNext()) {
            Entry<Vector, BlueprintBlock> e = it.next();
            assertTrue(result.put(e.getKey(), e.getValue().getBlockData()) == null, "Fill position repeated " + e.getKey());
        }
        bp.getBlocks().forEach((v, b) -> assertNull(result.put(v, b.getBlockData()), "Fill overlaps block " + v));
        return result;
    }

    @Test
    void testCompactAirPastesIdentically() {
        Map<Vector, BlueprintBlock> overrides = Map.of(new Vector(0, 0, 0), new BlueprintBlock(STONE),
                new Vector(1, 2, -1), new BlueprintBlock(STONE));
        Blueprint bp = fullBox(-2, -1, -3, 2, 3, 1, AIR, overrides);
        Map<Vector, String> before = pasted(bp);
        int volume = before.size();

        assertTrue(BlueprintFill.compact(bp));

        assertEquals(AIR, bp.getFill().getBlockData());
        assertEquals(new Vector(-2, -1, -3), bp.getFillMin());
        assertEquals(new Vector(2, 3, 1), bp.getFillMax());
        assertEquals(2, bp.getBlocks().size());
        assertEquals(volume - 2, BlueprintFill.countFill(bp));
        assertEquals(before, pasted(bp));
    }

    @Test
    void testCompactPicksWaterWhenMostCommon() {
        Map<Vector, BlueprintBlock> overrides = new HashMap<>();
        // Top layer is air, the rest water
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                overrides.put(new Vector(x, 3, z), new BlueprintBlock(AIR));
            }
        }
        Blueprint bp = fullBox(0, 0, 0, 2, 3, 2, WATER, overrides);
        Map<Vector, String> before = pasted(bp);

        assertTrue(BlueprintFill.compact(bp));

        assertEquals(WATER, bp.getFill().getBlockData());
        assertEquals(9, bp.getBlocks().size());
        assertEquals(before, pasted(bp));
    }

    @Test
    void testNoCompactWhenBoxNotFullyDefined() {
        // Copied without air: a missing position means "leave the world alone"
        Blueprint bp = fullBox(0, 0, 0, 3, 3, 3, AIR, Map.of());
        bp.getBlocks().remove(new Vector(1, 1, 1));

        assertFalse(BlueprintFill.compact(bp));
        assertNull(bp.getFill());
        assertEquals(63, bp.getBlocks().size());
    }

    @Test
    void testNoCompactWhenAlreadyCompact() {
        Blueprint bp = fullBox(0, 0, 0, 2, 2, 2, AIR, Map.of(new Vector(0, 0, 0), new BlueprintBlock(STONE)));
        assertTrue(BlueprintFill.compact(bp));
        BlueprintBlock fill = bp.getFill();
        assertFalse(BlueprintFill.compact(bp));
        assertSame(fill, bp.getFill());
    }

    @Test
    void testNoCompactEmptyOrNullBlocks() {
        Blueprint bp = new Blueprint();
        assertFalse(BlueprintFill.compact(bp));
        bp.setBlocks(new HashMap<>());
        assertFalse(BlueprintFill.compact(bp));
    }

    @Test
    void testNoCompactWhenNothingRepeats() {
        Blueprint bp = fullBox(0, 0, 0, 0, 0, 1, AIR, Map.of(new Vector(0, 0, 0), new BlueprintBlock(STONE)));
        assertFalse(BlueprintFill.compact(bp));
    }

    @Test
    void testBlocksWithStateAreNeverFill() {
        BlueprintBlock chest = new BlueprintBlock(AIR);
        chest.setInventory(Map.of(0, mock(org.bukkit.inventory.ItemStack.class)));
        BlueprintBlock sign = new BlueprintBlock(AIR);
        sign.setSignLines(Side.FRONT, List.of("a", "b", "c", "d"));
        Map<Vector, BlueprintBlock> overrides = Map.of(new Vector(0, 0, 0), chest, new Vector(1, 0, 0), sign);
        Blueprint bp = fullBox(0, 0, 0, 2, 2, 2, AIR, overrides);

        assertTrue(BlueprintFill.compact(bp));

        assertEquals(2, bp.getBlocks().size());
        assertSame(chest, bp.getBlocks().get(new Vector(0, 0, 0)));
        assertSame(sign, bp.getBlocks().get(new Vector(1, 0, 0)));
    }

    @Test
    void testBiomeKeptForNonFillBiome() {
        Biome plains = mock(Biome.class);
        Biome ocean = mock(Biome.class);
        Blueprint bp = fullBox(0, 0, 0, 2, 2, 2, AIR, Map.of());
        bp.getBlocks().values().forEach(b -> b.setBiome(plains));
        bp.getBlocks().get(new Vector(2, 2, 2)).setBiome(ocean);

        assertTrue(BlueprintFill.compact(bp));

        assertSame(plains, bp.getFill().getBiome());
        assertEquals(1, bp.getBlocks().size());
        assertSame(ocean, bp.getBlocks().get(new Vector(2, 2, 2)).getBiome());
    }

    @Test
    void testFillIteratorAndCountWithoutFill() {
        Blueprint bp = fullBox(0, 0, 0, 1, 1, 1, AIR, Map.of());
        assertFalse(BlueprintFill.fillIterator(bp).hasNext());
        assertEquals(0, BlueprintFill.countFill(bp));
    }

    @Test
    void testFillIteratorWithNullBlocks() {
        Blueprint bp = new Blueprint();
        bp.setFill(new BlueprintBlock(AIR));
        bp.setFillMin(new Vector(0, 0, 0));
        bp.setFillMax(new Vector(1, 1, 1));
        Iterator<Entry<Vector, BlueprintBlock>> it = BlueprintFill.fillIterator(bp);
        int n = 0;
        while (it.hasNext()) {
            assertSame(bp.getFill(), it.next().getValue());
            n++;
        }
        assertEquals(8, n);
        assertEquals(8, BlueprintFill.countFill(bp));
    }
}
