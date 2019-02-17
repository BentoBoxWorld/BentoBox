package world.bentobox.bentobox.api.localization;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test class just to check that these constants don't accidentally change
 * @author tastybento
 */
public class TextVariablesTest {

    @Test
    public void test() {
        assertEquals(TextVariables.NAME, "[name]");
        assertEquals(TextVariables.DESCRIPTION, "[description]");
        assertEquals(TextVariables.NUMBER, "[number]");
        assertEquals(TextVariables.RANK, "[rank]");
        assertEquals(TextVariables.LABEL, "[label]");
        assertEquals(TextVariables.PERMISSION, "[permission]");
        assertEquals(TextVariables.SPAWN_HERE, "[spawn_here]");
        assertEquals(TextVariables.VERSION, "[version]");
        assertEquals(TextVariables.START_TEXT, "[start]");
    }
}
