package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

/**
 * Tests the part of {@link BlueprintPreview} that works out which blocks to draw.
 */
class BlueprintPreviewTest {

    private static final String STONE = "stone";
    private static final String GLASS = "glass";

    private static Map<Vector, String> cube(int side, String block) {
        Map<Vector, String> blocks = new HashMap<>();
        for (int x = 0; x < side; x++) {
            for (int y = 0; y < side; y++) {
                for (int z = 0; z < side; z++) {
                    blocks.put(new Vector(x, y, z), block);
                }
            }
        }
        return blocks;
    }

    @Test
    void surfaceDropsBuriedBlocks() {
        Map<Vector, String> shown = BlueprintPreview.surface(cube(3, STONE), STONE::equals);
        assertEquals(26, shown.size());
        assertFalse(shown.containsKey(new Vector(1, 1, 1)));
    }

    @Test
    void surfaceKeepsBlocksBehindGlass() {
        Map<Vector, String> blocks = cube(3, GLASS);
        blocks.put(new Vector(1, 1, 1), STONE);
        Map<Vector, String> shown = BlueprintPreview.surface(blocks, STONE::equals);
        assertEquals(27, shown.size());
    }

    @Test
    void coarsenMergesBlocksAndKeepsTheHighest() {
        Map<Vector, String> blocks = cube(2, STONE);
        blocks.put(new Vector(0, 1, 0), GLASS);
        blocks.put(new Vector(1, 1, 0), GLASS);
        blocks.put(new Vector(0, 1, 1), GLASS);
        blocks.put(new Vector(1, 1, 1), GLASS);
        Map<Vector, String> cells = BlueprintPreview.coarsen(blocks, 2);
        assertEquals(Map.of(new Vector(0, 0, 0), GLASS), cells);
    }

    @Test
    void coarsenHandlesNegativeCoordinates() {
        Map<Vector, String> blocks = Map.of(new Vector(-1, 0, 0), STONE, new Vector(0, 0, 0), STONE);
        assertEquals(2, BlueprintPreview.coarsen(blocks, 2).size());
    }

    @Test
    void planUsesFullDetailWhenItFits() {
        BlueprintPreview.Plan<String> plan = BlueprintPreview.plan(cube(4, STONE), 100, STONE::equals);
        assertEquals(1, plan.factor());
        assertEquals(56, plan.cells().size());
    }

    @Test
    void planCoarsensUntilUnderTheLimit() {
        BlueprintPreview.Plan<String> plan = BlueprintPreview.plan(cube(8, STONE), 60, STONE::equals);
        assertEquals(2, plan.factor());
        assertEquals(56, plan.cells().size());
        assertTrue(plan.cells().size() <= 60);
    }
}
