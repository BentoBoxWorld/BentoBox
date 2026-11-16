package world.bentobox.bentobox.blueprints;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.bukkit.block.Biome;
import org.bukkit.block.sign.Side;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;

/**
 * Compact storage for blueprints whose blocks define every position of their box.
 * <p>
 * A blueprint copied with air captures every block in the copy box, and most of
 * those blocks are usually plain air (or water, for ocean builds). Instead of
 * storing each one, the most common plain block is stored once as the blueprint's
 * {@link Blueprint#getFill() fill}, together with the box corners. When pasting, every
 * position in the box that has no explicit block gets the fill block.
 * <p>
 * Blueprints without a fill are pasted exactly as before, so older blueprint
 * files keep working.
 *
 * @author tastybento
 * @since 3.23.8
 */
public final class BlueprintFill {

    private BlueprintFill() {
        // Utility class
    }

    /**
     * The block data and biome that identify a plain block
     */
    private record FillKey(String blockData, @Nullable Biome biome) {
        boolean matches(BlueprintBlock b) {
            return isPlain(b) && Objects.equals(blockData, b.getBlockData()) && Objects.equals(biome, b.getBiome());
        }
    }

    /**
     * Compacts a blueprint if, and only if, its blocks define every position of their
     * bounding box. The most common plain block (usually air) becomes the fill block and
     * is removed from the block map. The result pastes identically to the original.
     * <p>
     * Blueprints that do not cover their whole box (for example, ones copied without air,
     * where a missing block means "leave the world alone") are left untouched, as are
     * blueprints that are already compact.
     *
     * @param bp blueprint to compact
     * @return true if the blueprint was compacted
     */
    public static boolean compact(@NonNull Blueprint bp) {
        Map<Vector, BlueprintBlock> blocks = bp.getBlocks();
        if (bp.getFill() != null || blocks == null || blocks.isEmpty()) {
            return false;
        }
        // Find the bounds of the blocks
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Vector v : blocks.keySet()) {
            if (!isBlockVector(v)) {
                return false;
            }
            minX = Math.min(minX, v.getBlockX());
            minY = Math.min(minY, v.getBlockY());
            minZ = Math.min(minZ, v.getBlockZ());
            maxX = Math.max(maxX, v.getBlockX());
            maxY = Math.max(maxY, v.getBlockY());
            maxZ = Math.max(maxZ, v.getBlockZ());
        }
        // The keys are distinct block positions inside the box, so they fill it only if there are as many as the volume
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume != blocks.size()) {
            return false;
        }
        FillKey best = mostCommonPlainBlock(blocks.values());
        if (best == null) {
            return false;
        }
        BlueprintBlock fill = new BlueprintBlock(best.blockData());
        fill.setBiome(best.biome());
        blocks.values().removeIf(best::matches);
        bp.setFill(fill);
        bp.setFillMin(new Vector(minX, minY, minZ));
        bp.setFillMax(new Vector(maxX, maxY, maxZ));
        return true;
    }

    @Nullable
    private static FillKey mostCommonPlainBlock(Collection<BlueprintBlock> blocks) {
        Map<FillKey, Integer> counts = new HashMap<>();
        for (BlueprintBlock b : blocks) {
            if (isPlain(b)) {
                counts.merge(new FillKey(b.getBlockData(), b.getBiome()), 1, Integer::sum);
            }
        }
        // Only worth it if at least two blocks are replaced
        return counts.entrySet().stream().filter(e -> e.getValue() > 1).max(Entry.comparingByValue())
                .map(Entry::getKey).orElse(null);
    }

    /**
     * A plain block is one defined only by its block data and biome
     * @param b block
     * @return true if the block carries no other state
     */
    static boolean isPlain(BlueprintBlock b) {
        return b.getBlockData() != null
                && b.getSignLines(Side.FRONT) == null && b.getSignLines(Side.BACK) == null
                && !b.isGlowingText(Side.FRONT) && !b.isGlowingText(Side.BACK)
                && b.getInventory().isEmpty()
                && b.getCreatureSpawner() == null
                && b.getTrialSpawner() == null
                && b.getItemsAdderBlock() == null
                && (b.getBannerPatterns() == null || b.getBannerPatterns().isEmpty());
    }

    private static boolean isBlockVector(Vector v) {
        return v.getX() == Math.floor(v.getX()) && v.getY() == Math.floor(v.getY())
                && v.getZ() == Math.floor(v.getZ());
    }

    /**
     * @param bp blueprint
     * @return true if the blueprint has a fill block and box
     */
    static boolean hasFill(@NonNull Blueprint bp) {
        return bp.getFill() != null && bp.getFillMin() != null && bp.getFillMax() != null;
    }

    /**
     * Counts the positions that will receive the fill block when pasting
     * @param bp blueprint
     * @return number of fill positions, 0 if the blueprint has no fill
     */
    static long countFill(@NonNull Blueprint bp) {
        if (!hasFill(bp)) {
            return 0;
        }
        Vector min = bp.getFillMin();
        Vector max = bp.getFillMax();
        long volume = (long) (max.getBlockX() - min.getBlockX() + 1) * (max.getBlockY() - min.getBlockY() + 1)
                * (max.getBlockZ() - min.getBlockZ() + 1);
        Map<Vector, BlueprintBlock> blocks = bp.getBlocks();
        long explicit = blocks == null ? 0 : blocks.keySet().stream().filter(v -> isInside(v, min, max)).count();
        return volume - explicit;
    }

    private static boolean isInside(Vector v, Vector min, Vector max) {
        return v.getBlockX() >= min.getBlockX() && v.getBlockX() <= max.getBlockX()
                && v.getBlockY() >= min.getBlockY() && v.getBlockY() <= max.getBlockY()
                && v.getBlockZ() >= min.getBlockZ() && v.getBlockZ() <= max.getBlockZ();
    }

    /**
     * Lazily iterates every position in the fill box that has no explicit block, paired
     * with the fill block. The same fill block instance is returned for every position.
     * @param bp blueprint
     * @return iterator, empty if the blueprint has no fill
     */
    static Iterator<Entry<Vector, BlueprintBlock>> fillIterator(@NonNull Blueprint bp) {
        if (!hasFill(bp)) {
            return Collections.emptyIterator();
        }
        return new FillIterator(bp.getFill(), bp.getFillMin(), bp.getFillMax(),
                bp.getBlocks() == null ? Map.of() : bp.getBlocks());
    }

    private static final class FillIterator implements Iterator<Entry<Vector, BlueprintBlock>> {
        private final BlueprintBlock fill;
        private final Map<Vector, BlueprintBlock> blocks;
        private final int minX;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private int x;
        private int y;
        private int z;
        private Vector next;

        FillIterator(BlueprintBlock fill, Vector min, Vector max, Map<Vector, BlueprintBlock> blocks) {
            this.fill = fill;
            this.blocks = blocks;
            this.minX = min.getBlockX();
            this.minZ = min.getBlockZ();
            this.maxX = max.getBlockX();
            this.maxY = max.getBlockY();
            this.maxZ = max.getBlockZ();
            this.x = minX;
            this.y = min.getBlockY();
            this.z = minZ;
            advance();
        }

        /**
         * Moves to the next position without an explicit block, or sets next to null
         */
        private void advance() {
            next = null;
            while (next == null && y <= maxY) {
                Vector v = new Vector(x, y, z);
                // Step to the following position: z, then x, then y
                if (++z > maxZ) {
                    z = minZ;
                    if (++x > maxX) {
                        x = minX;
                        y++;
                    }
                }
                if (!blocks.containsKey(v)) {
                    next = v;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Entry<Vector, BlueprintBlock> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            Entry<Vector, BlueprintBlock> result = Map.entry(next, fill);
            advance();
            return result;
        }
    }
}
