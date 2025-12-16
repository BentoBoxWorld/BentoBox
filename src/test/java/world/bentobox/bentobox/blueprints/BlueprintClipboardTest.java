package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.HooksManager;

/**
 * @author tastybento
 *
 */
public class BlueprintClipboardTest extends CommonTestSetup {

    private BlueprintClipboard bc;

    @Mock
    private @NonNull Blueprint blueprint;
    @Mock
    private @NonNull User user;
    @Mock
    private BentoBox plugin;

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
        assertFalse(bc.copy(user, false, false, false));
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
