package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

/**
 * Tests the parts of {@link BlueprintPreview} that need no server.
 */
class BlueprintPreviewTest {

    @Test
    void boundsHoldEveryBlock() {
        BoundingBox box = BlueprintPreview.bounds(List.of(new Vector(-2, 0, 3), new Vector(4, 5, -1)));
        assertEquals(new BoundingBox(-2, 0, -1, 5, 6, 4), box);
    }

    @Test
    void boundsOfOneBlockIsThatBlock() {
        assertEquals(new BoundingBox(1, 2, 3, 2, 3, 4), BlueprintPreview.bounds(List.of(new Vector(1, 2, 3))));
    }

    @Test
    void clampLeavesAPlayerInsideAlone() {
        BoundingBox bounds = new BoundingBox(0, 0, 0, 10, 10, 10);
        assertNull(BlueprintPreview.clamp(new Location(null, 5, 5, 5), bounds));
    }

    @Test
    void clampPullsAPlayerBackInside() {
        BoundingBox bounds = new BoundingBox(0, 0, 0, 10, 10, 10);
        Location inside = BlueprintPreview.clamp(new Location(null, 12, -3, 5, 90F, 10F), bounds);
        assertEquals(new Location(null, 9.5, 0.5, 5, 90F, 10F), inside);
    }
}
