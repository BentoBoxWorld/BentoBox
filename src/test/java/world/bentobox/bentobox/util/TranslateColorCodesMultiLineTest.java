package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests that {@link Util#translateColorCodes(String)} preserves per-line color codes
 * in multi-line strings.  Adventure's LegacyComponentSerializer used to omit repeated
 * §X codes for consecutive same-color segments, causing all lore lines after the first
 * to lose their color when the string was split on {@code \n}.
 */
class TranslateColorCodesMultiLineTest extends CommonTestSetup {

    private static final char SECT = '\u00A7';

    @Test
    void testMultiLineAllLinesPreserveColorCode() {
        String input = "&a Toggle interaction with all containers.\n&a Includes: Barrel, bee hive, brewing stand,\n&a chest, composter, dispenser, dropper,";
        String result = Util.translateColorCodes(input);
        String[] lines = result.split("\n");

        char sect = '\u00A7';
        assertTrue(lines[0].startsWith(sect + "a"), "Line 1 should start with §a, got: " + lines[0]);
        assertTrue(lines[1].startsWith(sect + "a"), "Line 2 should start with §a, got: " + lines[1]);
        assertTrue(lines[2].startsWith(sect + "a"), "Line 3 should start with §a, got: " + lines[2]);
    }

    /**
     * Simulates the double-translation that occurs in Flag.toPanelItem: the description
     * reference is translated first, then embedded as a variable in a layout string that
     * is also translated.  Every description line must still be green after both passes.
     */
    @Test
    void testDoubleTranslationPreservesColorCodes() {
        // First pass: description reference (as returned by user.getTranslation(descRef))
        String innerDesc = Util.translateColorCodes(
                "&a Toggle interaction with all containers.\n&a Includes: Barrel, bee hive, brewing stand,\n&a chest, composter, dispenser, dropper,");

        // Second pass: layout that embeds the already-translated description
        String layout = "&a " + innerDesc + "\n\n&e Left Click &7 to cycle downwards.";
        String result = Util.translateColorCodes(layout);

        String[] lines = result.split("\n");
        char sect = '\u00A7';
        assertTrue(lines[0].startsWith(sect + "a"), "Line 1 should start with §a after double-translation, got: " + lines[0]);
        assertTrue(lines[1].startsWith(sect + "a"), "Line 2 should start with §a after double-translation, got: " + lines[1]);
        assertTrue(lines[2].startsWith(sect + "a"), "Line 3 should start with §a after double-translation, got: " + lines[2]);
    }

    /**
     * Tests that hex color codes followed by legacy color codes both work correctly.
     * Previously, hex codes were converted to §x§R§R... format before the &-based
     * LegacyComponentSerializer processed them, corrupting the hex color and breaking
     * any subsequent legacy color codes.
     */
    @Test
    void testHexColorFollowedByLegacyColor() {
        String input = "&#FF0000Red &aGreen";
        String result = Util.translateColorCodes(input);
        // The result should contain §a for legacy green
        assertTrue(result.contains(SECT + "a"), "Legacy &a should be translated after hex color, got: " + result);
        // The result should contain the hex color in §x§F§F§0§0§0§0 format
        String hexExpected = SECT + "x" + SECT + "f" + SECT + "f" + SECT + "0" + SECT + "0" + SECT + "0" + SECT + "0";
        assertTrue(result.contains(hexExpected), "Hex color &#FF0000 should be present, got: " + result);
        assertTrue(result.contains("Red"), "Text after hex color should be present, got: " + result);
        assertTrue(result.contains("Green"), "Text after legacy color should be present, got: " + result);
    }

    /**
     * Tests that 3-digit shorthand hex codes are expanded to 6-digit and work correctly.
     */
    @Test
    void testShortHexColorExpanded() {
        String input = "&#F00Red &aGreen";
        String result = Util.translateColorCodes(input);
        // &#F00 should be expanded to &#FF0000
        String hexExpected = SECT + "x" + SECT + "f" + SECT + "f" + SECT + "0" + SECT + "0" + SECT + "0" + SECT + "0";
        assertTrue(result.contains(hexExpected), "Short hex &#F00 should be expanded to full hex, got: " + result);
        assertTrue(result.contains(SECT + "a"), "Legacy &a should work after short hex color, got: " + result);
    }

    /**
     * Tests that a standalone hex color code without subsequent legacy codes works.
     */
    @Test
    void testStandaloneHexColor() {
        String input = "&#FF0000Red text";
        String result = Util.translateColorCodes(input);
        String hexExpected = SECT + "x" + SECT + "f" + SECT + "f" + SECT + "0" + SECT + "0" + SECT + "0" + SECT + "0";
        assertTrue(result.contains(hexExpected), "Standalone hex color should work, got: " + result);
        assertTrue(result.contains("Red"), "Text should be present, got: " + result);
    }

    /**
     * Tests that legacy color codes still work on their own (no hex present).
     */
    @Test
    void testLegacyOnlyUnchanged() {
        String input = "&aGreen &bAqua";
        String result = Util.translateColorCodes(input);
        assertEquals(SECT + "aGreen " + SECT + "bAqua", result);
    }
}
