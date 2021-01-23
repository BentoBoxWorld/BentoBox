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
        assertEquals("[name]", TextVariables.NAME);
        assertEquals("[description]", TextVariables.DESCRIPTION);
        assertEquals("[number]", TextVariables.NUMBER);
        assertEquals("[rank]", TextVariables.RANK);
        assertEquals("[label]", TextVariables.LABEL);
        assertEquals("[permission]", TextVariables.PERMISSION);
        assertEquals("[spawn_here]", TextVariables.SPAWN_HERE);
        assertEquals("[version]", TextVariables.VERSION);
        assertEquals("[start]", TextVariables.START_TEXT);
    }
}
