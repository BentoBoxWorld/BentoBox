package world.bentobox.bentobox.util;

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
}
