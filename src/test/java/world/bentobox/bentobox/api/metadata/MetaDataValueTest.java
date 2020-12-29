package world.bentobox.bentobox.api.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class MetaDataValueTest {

    /**
     * Test method for {@link world.bentobox.bentobox.api.metadata.MetaDataValue#asInt()}.
     */
    @Test
    public void testAsInt() {
        MetaDataValue mdv = new MetaDataValue(123);
        assertEquals(123, mdv.asInt());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.metadata.MetaDataValue#asFloat()}.
     */
    @Test
    public void testAsFloat() {
        MetaDataValue mdv = new MetaDataValue(123.34F);
        assertEquals(123.34F, mdv.asFloat(), 0F);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.metadata.MetaDataValue#asDouble()}.
     */
    @Test
    public void testAsDouble() {
        MetaDataValue mdv = new MetaDataValue(123.3444D);
        assertEquals(123.3444D, mdv.asDouble(), 0D);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.metadata.MetaDataValue#asLong()}.
     */
    @Test
    public void testAsLong() {
        MetaDataValue mdv = new MetaDataValue(123456L);
        assertEquals(123456L, mdv.asLong());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.metadata.MetaDataValue#asShort()}.
     */
    @Test
    public void testAsShort() {
        MetaDataValue mdv = new MetaDataValue((short)12);
        assertEquals((short)12, mdv.asShort());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.metadata.MetaDataValue#asByte()}.
     */
    @Test
    public void testAsByte() {
        MetaDataValue mdv = new MetaDataValue((byte)12);
        assertEquals((byte)12, mdv.asByte());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.metadata.MetaDataValue#asBoolean()}.
     */
    @Test
    public void testAsBoolean() {
        MetaDataValue mdv = new MetaDataValue(false);
        assertFalse(mdv.asBoolean());
        mdv = new MetaDataValue(true);
        assertTrue(mdv.asBoolean());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.metadata.MetaDataValue#asString()}.
     */
    @Test
    public void testAsString() {
        MetaDataValue mdv = new MetaDataValue("a string");
        assertEquals("a string", mdv.asString());
    }

}
