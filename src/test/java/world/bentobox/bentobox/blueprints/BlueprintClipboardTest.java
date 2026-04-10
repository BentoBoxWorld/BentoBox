package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.HooksManager;

/**
 * @author tastybento
 *
 */
class BlueprintClipboardTest extends CommonTestSetup {

    private BlueprintClipboard bc;

    @Mock
    private @NonNull Blueprint blueprint;
    @Mock
    private @NonNull User user;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Required for NamespacedKey
        when(plugin.getName()).thenReturn("BentoBox");
        // Hooks
        HooksManager hooksManager = mock(HooksManager.class);
        when(hooksManager.getHook(anyString())).thenReturn(Optional.empty());
        when(plugin.getHooks()).thenReturn(hooksManager);

        // User
        when(user.getTranslation(Mockito.anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);


        bc = new BlueprintClipboard();

    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#BlueprintClipboard(world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    void testBlueprintClipboardBlueprint() {
        bc = new BlueprintClipboard(blueprint);
        assertNotNull(bc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#BlueprintClipboard()}.
     */
    @Test
    void testBlueprintClipboard() {
        assertNotNull(bc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#copy(world.bentobox.bentobox.api.user.User, boolean, boolean)}.
     */
    @Test
    void testCopy() {
        assertFalse(bc.copy(user, false, false, false));
        verify(user, never()).sendMessage("commands.admin.blueprint.mid-copy");
        verify(user).sendMessage("commands.admin.blueprint.need-pos1-pos2");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getVectors(org.bukkit.util.BoundingBox)}.
     */
    @Test
    void testGetVectors() {
        BoundingBox bb = new BoundingBox(10.5, 10.5, 10.5, 19.5, 19.5, 19.5);
        List<Vector> list = bc.getVectors(bb);
        assertEquals(1000, list.size());

        bb = new BoundingBox(19.5, 19.5, 19.5, 10.5, 10.5, 10.5);
        list = bc.getVectors(bb);
        assertEquals(1000, list.size());

        bb = new BoundingBox(-10.5, -10.5, -10.5, -19.5, -19.5, -19.5);
        list = bc.getVectors(bb);
        assertEquals(1000, list.size());

        bb = new BoundingBox(-19.5, -19.5, -19.5, -10.5, -10.5, -10.5);
        list = bc.getVectors(bb);
        assertEquals(1000, list.size());

        bb = new BoundingBox(-5.5, -5.5, -5.5, 3.5, 3.5, 3.5);
        list = bc.getVectors(bb);
        assertEquals(1000, list.size());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getOrigin()}.
     */
    @Test
    void testGetOrigin() {
        assertNull(bc.getOrigin());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getPos1()}.
     */
    @Test
    void testGetPos1() {
        assertNull(bc.getPos1());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getPos2()}.
     */
    @Test
    void testGetPos2() {
        assertNull(bc.getPos2());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#isFull()}.
     */
    @Test
    void testIsFull() {
        assertFalse(bc.isFull());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#setOrigin(org.bukkit.util.Vector)}.
     */
    @Test
    void testSetOrigin() {
        Vector v = new Vector(1,2,3);
        bc.setOrigin(v);
        assertEquals(v, bc.getOrigin());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#setPos1(org.bukkit.Location)}.
     */
    @Test
    void testSetPos1() {
        Location l = new Location(world, 1,2,3);
        bc.setPos1(l);
        assertEquals(l, bc.getPos1());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#setPos2(org.bukkit.Location)}.
     */
    @Test
    void testSetPos2() {
        Location l = new Location(world, 1,2,3);
        bc.setPos2(l);
        assertEquals(l, bc.getPos2());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getBlueprint()}.
     */
    @Test
    void testGetBlueprint() {
        assertNull(bc.getBlueprint());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#setBlueprint(world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    void testSetBlueprint() {
        bc.setBlueprint(blueprint);
        assertEquals(blueprint, bc.getBlueprint());
    }

    // ---- copy edge cases ----

    @Test
    void testCopyNoPos1() {
        bc.setPos2(new Location(world, 10, 64, 10));
        assertFalse(bc.copy(user, false, false, false));
        verify(user).sendMessage("commands.admin.blueprint.need-pos1-pos2");
    }

    @Test
    void testCopyNoPos2() {
        bc.setPos1(new Location(world, 0, 64, 0));
        assertFalse(bc.copy(user, false, false, false));
        verify(user).sendMessage("commands.admin.blueprint.need-pos1-pos2");
    }

    @Test
    void testCopyWithExplicitOriginNullWorld() {
        // When origin is set and world is null, copy should return false
        Location noWorldLoc1 = mock(Location.class);
        when(noWorldLoc1.getWorld()).thenReturn(null);
        when(noWorldLoc1.getBlockY()).thenReturn(64);
        Location noWorldLoc2 = mock(Location.class);
        when(noWorldLoc2.getWorld()).thenReturn(null);
        when(noWorldLoc2.getBlockY()).thenReturn(74);

        // Bypass setPos1/setPos2 which would clear origin
        // Set origin first, then use direct field access via setPos methods
        bc.setPos1(noWorldLoc1);
        bc.setPos2(noWorldLoc2);
        bc.setOrigin(new Vector(0, 64, 0)); // Set origin to avoid user.getLocation() call
        assertFalse(bc.copy(user, false, false, false));
    }

    @Test
    void testCopySuccess() {
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        // Set up pos1 and pos2 with a real world
        bc.setPos1(new Location(world, 0, 64, 0));
        bc.setPos2(new Location(world, 2, 66, 2));

        // Mock the user location for origin
        Location userLoc = mock(Location.class);
        when(userLoc.toVector()).thenReturn(new Vector(1, 65, 1));
        when(user.getLocation()).thenReturn(userLoc);

        // Mock async task
        when(sch.runTaskAsynchronously(any(), any(Runnable.class))).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return mock(BukkitTask.class);
        });
        // Mock the copy task timer
        when(sch.runTaskTimer(any(), any(Runnable.class), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(mock(BukkitTask.class));

        assertTrue(bc.copy(user, false, false, false));
        verify(user).sendMessage("commands.admin.blueprint.copying");
    }

    @Test
    void testCopyWithOriginAlreadySet() {
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        bc.setPos1(new Location(world, 0, 64, 0));
        bc.setPos2(new Location(world, 2, 66, 2));
        bc.setOrigin(new Vector(0, 64, 0));

        when(sch.runTaskAsynchronously(any(), any(Runnable.class))).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return mock(BukkitTask.class);
        });
        when(sch.runTaskTimer(any(), any(Runnable.class), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(mock(BukkitTask.class));

        assertTrue(bc.copy(user, true, true, false));
        // Origin was already set, so user.getLocation() should NOT be called
        verify(user, never()).getLocation();
    }

    // ---- setPos1 / setPos2 height clamping ----

    @Test
    void testSetPos1ClampsToMinHeight() {
        when(world.getMinHeight()).thenReturn(0);
        when(world.getMaxHeight()).thenReturn(256);

        Location l = new Location(world, 10, -10, 10);
        bc.setPos1(l);
        assertEquals(0, bc.getPos1().getBlockY());
    }

    @Test
    void testSetPos1ClampsToMaxHeight() {
        when(world.getMinHeight()).thenReturn(0);
        when(world.getMaxHeight()).thenReturn(256);

        Location l = new Location(world, 10, 300, 10);
        bc.setPos1(l);
        assertEquals(256, bc.getPos1().getBlockY());
    }

    @Test
    void testSetPos2ClampsToMinHeight() {
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        Location l = new Location(world, 10, -100, 10);
        bc.setPos2(l);
        assertEquals(-64, bc.getPos2().getBlockY());
    }

    @Test
    void testSetPos2ClampsToMaxHeight() {
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        Location l = new Location(world, 10, 400, 10);
        bc.setPos2(l);
        assertEquals(320, bc.getPos2().getBlockY());
    }

    @Test
    void testSetPos1NullWorld() {
        // Null world should use defaults: min=0, max=255
        Location l = new Location(null, 10, -5, 10);
        bc.setPos1(l);
        assertEquals(0, bc.getPos1().getBlockY());
    }

    @Test
    void testSetPos2NullWorld() {
        Location l = new Location(null, 10, 300, 10);
        bc.setPos2(l);
        assertEquals(255, bc.getPos2().getBlockY());
    }

    @Test
    void testSetPos1Null() {
        bc.setPos1(null);
        assertNull(bc.getPos1());
    }

    @Test
    void testSetPos2Null() {
        bc.setPos2(null);
        assertNull(bc.getPos2());
    }

    @Test
    void testSetPos1ClearsOrigin() {
        bc.setOrigin(new Vector(1, 2, 3));
        assertNotNull(bc.getOrigin());
        bc.setPos1(new Location(world, 0, 64, 0));
        assertNull(bc.getOrigin());
    }

    @Test
    void testSetPos2ClearsOrigin() {
        bc.setOrigin(new Vector(1, 2, 3));
        assertNotNull(bc.getOrigin());
        bc.setPos2(new Location(world, 0, 64, 0));
        assertNull(bc.getOrigin());
    }

    // ---- isFull ----

    @Test
    void testIsFullAfterSetBlueprint() {
        assertFalse(bc.isFull());
        bc.setBlueprint(blueprint);
        assertTrue(bc.isFull());
    }

    // ---- setBlueprint returns this (fluent) ----

    @Test
    void testSetBlueprintReturnsSelf() {
        BlueprintClipboard result = bc.setBlueprint(blueprint);
        assertEquals(bc, result);
    }

    // ---- getVectors edge cases ----

    @Test
    void testGetVectorsSingleBlock() {
        BoundingBox bb = new BoundingBox(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        List<Vector> list = bc.getVectors(bb);
        assertEquals(1, list.size());
    }

    @Test
    void testGetVectorsSmallArea() {
        BoundingBox bb = new BoundingBox(0.5, 0.5, 0.5, 2.5, 2.5, 2.5);
        List<Vector> list = bc.getVectors(bb);
        assertEquals(27, list.size()); // 3x3x3
    }
}
