package world.bentobox.bentobox.blueprints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Bukkit.class})
public class BlueprintClipboardTest {

    private BlueprintClipboard bc;

    @Mock
    private @NonNull Blueprint blueprint;
    @Mock
    private @NonNull User user;
    @Mock
    private BentoBox plugin;
    @Mock
    private World world;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // User
        when(user.getTranslation(Mockito.anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);


        bc = new BlueprintClipboard();

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#BlueprintClipboard(world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testBlueprintClipboardBlueprint() {
        bc = new BlueprintClipboard(blueprint);
        assertNotNull(bc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#BlueprintClipboard()}.
     */
    @Test
    public void testBlueprintClipboard() {
        assertNotNull(bc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#copy(world.bentobox.bentobox.api.user.User, boolean, boolean)}.
     */
    @Test
    public void testCopy() {
        assertFalse(bc.copy(user, false, false));
        verify(user, never()).sendMessage("commands.admin.blueprint.mid-copy");
        verify(user).sendMessage("commands.admin.blueprint.need-pos1-pos2");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getVectors(org.bukkit.util.BoundingBox)}.
     */
    @Test
    public void testGetVectors() {
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
    public void testGetOrigin() {
        assertNull(bc.getOrigin());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getPos1()}.
     */
    @Test
    public void testGetPos1() {
        assertNull(bc.getPos1());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getPos2()}.
     */
    @Test
    public void testGetPos2() {
        assertNull(bc.getPos2());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#isFull()}.
     */
    @Test
    public void testIsFull() {
        assertFalse(bc.isFull());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#setOrigin(org.bukkit.util.Vector)}.
     */
    @Test
    public void testSetOrigin() {
        Vector v = new Vector(1,2,3);
        bc.setOrigin(v);
        assertEquals(v, bc.getOrigin());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#setPos1(org.bukkit.Location)}.
     */
    @Test
    public void testSetPos1() {
        Location l = new Location(world, 1,2,3);
        bc.setPos1(l);
        assertEquals(l, bc.getPos1());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#setPos2(org.bukkit.Location)}.
     */
    @Test
    public void testSetPos2() {
        Location l = new Location(world, 1,2,3);
        bc.setPos2(l);
        assertEquals(l, bc.getPos2());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#getBlueprint()}.
     */
    @Test
    public void testGetBlueprint() {
        assertNull(bc.getBlueprint());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.blueprints.BlueprintClipboard#setBlueprint(world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testSetBlueprint() {
        bc.setBlueprint(blueprint);
        assertEquals(blueprint, bc.getBlueprint());
    }

}
