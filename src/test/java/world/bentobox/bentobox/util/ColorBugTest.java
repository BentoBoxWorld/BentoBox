package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import world.bentobox.bentobox.CommonTestSetup;

public class ColorBugTest extends CommonTestSetup {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test
    void testMultiLineColorPreservation() {
        // Simulate what happens in Flag.toPanelItem - double translation
        // First, inner description gets color-translated (as user.getTranslation(descRef) does)
        String innerDesc = Util.translateColorCodes("&a Toggle interaction with all containers.\n&a Includes: Barrel, bee hive, brewing stand,\n&a chest, composter, dispenser, dropper,");
        System.out.println("Step 1 (inner desc):");
        printLines(innerDesc);
        
        // Then the layout also gets color-translated (as user.getTranslation(layoutRef, desc) does)  
        String layout = "&a " + innerDesc + "\n\n&e Left Click &7 to cycle downwards.";
        String result = Util.translateColorCodes(layout);
        System.out.println("\nStep 2 (layout + desc):");
        printLines(result);
        
        // Split by newlines as PanelItemBuilder.description does
        String[] lines = result.split("\n");
        System.out.println("\nLines after split:");
        for (int i = 0; i < lines.length; i++) {
            System.out.println("  Line " + i + ": '" + lines[i] + "'");
        }
        
        // All description lines (1, 2, 3) should start with §a (green)
        char sectionSign = '\u00A7';
        assertTrue(lines[0].startsWith(sectionSign + "a"), "Line 1 should start with §a");
        assertTrue(lines[1].startsWith(sectionSign + "a"), "Line 2 should start with §a");  
        assertTrue(lines[2].startsWith(sectionSign + "a"), "Line 3 should start with §a");
    }
    
    private static void printLines(String s) {
        String[] lines = s.split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.println("  Line " + i + ": '" + lines[i].replace("\u00A7", "§") + "'");
        }
    }
}
