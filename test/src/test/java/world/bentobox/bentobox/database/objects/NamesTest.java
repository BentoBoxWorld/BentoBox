/**
 * 
 */
package world.bentobox.bentobox.database.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Test;

/**
 * @author tastybento
 *
 */
public class NamesTest {

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Names#Names()}.
     */
    @Test
    public void testNames() {
        assertNotNull(new Names());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Names#Names(java.lang.String, java.util.UUID)}.
     */
    @Test
    public void testNamesStringUUID() {
        assertNotNull(new Names("name", UUID.randomUUID()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Names#getUniqueId()}.
     */
    @Test
    public void testGetUniqueId() {
        Names name = new Names("name", UUID.randomUUID());
        assertEquals("name", name.getUniqueId());
        name.setUniqueId("random");
        assertEquals("random", name.getUniqueId());
    }
    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Names#getUuid()}.
     */
    @Test
    public void testGetUuid() {
        
        Names name = new Names();
        assertNull(name.getUuid());
        UUID t = UUID.randomUUID();
        name.setUuid(t);
        assertEquals(t, name.getUuid());
    }



}
