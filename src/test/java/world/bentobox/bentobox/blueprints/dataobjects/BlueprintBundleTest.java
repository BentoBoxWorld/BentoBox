package world.bentobox.bentobox.blueprints.dataobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests for {@link BlueprintBundle} icon field parsing.
 * @author tastybento
 */
class BlueprintBundleTest extends CommonTestSetup {

    private BlueprintBundle bundle;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        bundle = new BlueprintBundle();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Default icon should be PAPER.
     */
    @Test
    void testDefaultIcon() {
        assertEquals(Material.PAPER, bundle.getIcon());
    }

    /**
     * Default getIconItemStack should return a PAPER ItemStack.
     */
    @Test
    void testDefaultIconItemStack() {
        ItemStack is = bundle.getIconItemStack();
        assertNotNull(is);
        assertEquals(Material.PAPER, is.getType());
    }

    /**
     * Plain material name (e.g. "DIAMOND") should resolve correctly.
     */
    @Test
    void testPlainMaterialName() {
        bundle.setIcon("DIAMOND");
        assertEquals(Material.DIAMOND, bundle.getIcon());
        ItemStack is = bundle.getIconItemStack();
        assertNotNull(is);
        assertEquals(Material.DIAMOND, is.getType());
    }

    /**
     * Setting icon via Material enum should store correctly.
     */
    @Test
    void testSetIconMaterial() {
        bundle.setIcon(Material.GOLD_INGOT);
        assertEquals(Material.GOLD_INGOT, bundle.getIcon());
        ItemStack is = bundle.getIconItemStack();
        assertNotNull(is);
        assertEquals(Material.GOLD_INGOT, is.getType());
    }

    /**
     * Setting icon via Material enum with null should fall back to PAPER.
     */
    @Test
    void testSetIconMaterialNull() {
        bundle.setIcon((Material) null);
        assertEquals(Material.PAPER, bundle.getIcon());
    }

    /**
     * Setting icon via String with null should fall back to PAPER.
     */
    @Test
    void testSetIconStringNull() {
        bundle.setIcon((String) null);
        assertEquals(Material.PAPER, bundle.getIcon());
    }

    /**
     * Vanilla namespaced material key (e.g. "minecraft:diamond") should resolve to the correct Material.
     */
    @Test
    void testNamespacedVanillaMaterial() {
        bundle.setIcon("minecraft:diamond");
        assertEquals(Material.DIAMOND, bundle.getIcon());
        ItemStack is = bundle.getIconItemStack();
        assertNotNull(is);
        assertEquals(Material.DIAMOND, is.getType());
    }

    /**
     * A custom item-model key (namespace:key that is not a vanilla material) should return
     * PAPER as the base material, since the player never sees the base item.
     */
    @Test
    void testCustomItemModelKey() {
        bundle.setIcon("myserver:island_tropical");
        // getIcon() falls back to PAPER for unrecognised model keys
        assertEquals(Material.PAPER, bundle.getIcon());
        // getIconItemStack() returns a PAPER-based item
        ItemStack is = bundle.getIconItemStack();
        assertNotNull(is);
        assertEquals(Material.PAPER, is.getType());
    }

    /**
     * An icon string without a colon that is not a valid material should fall back to PAPER.
     */
    @Test
    void testUnknownMaterialName() {
        bundle.setIcon("NOT_A_REAL_MATERIAL");
        assertEquals(Material.PAPER, bundle.getIcon());
        ItemStack is = bundle.getIconItemStack();
        assertNotNull(is);
        assertEquals(Material.PAPER, is.getType());
    }
}
