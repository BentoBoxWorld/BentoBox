package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests that {@link Util#legacyToMiniMessage(String)} produces properly nested MiniMessage tags,
 * especially when legacy decoration codes (bold, italic, etc.) carry through color code changes.
 */
class LegacyToMiniMessageTest extends CommonTestSetup {

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

    /**
     * When bold carries through a color change (no §r reset), the MiniMessage output
     * must still produce properly nested tags so that MiniMessage does not render
     * closing tags as literal text.
     */
    @Test
    void testBoldCarriesThroughColorChange() {
        // §c§l = red bold, then §a = green (bold continues in legacy)
        String legacy = "§aResets §c§lALL §athe settings to their§r";
        String miniMsg = Util.legacyToMiniMessage(legacy);

        // Parse the MiniMessage output and check no tags appear as literal text
        Component comp = Util.parseMiniMessage(miniMsg);
        String plainText = PlainTextComponentSerializer.plainText().serialize(comp);

        assertFalse(plainText.contains("</bold>"),
                "Plain text should not contain literal </bold> tag but was: " + plainText);
        assertTrue(plainText.contains("Resets"),
                "Expected content 'Resets' missing: " + plainText);
        assertTrue(plainText.contains("ALL"),
                "Expected content 'ALL' missing: " + plainText);
        assertTrue(plainText.contains("the settings to their"),
                "Expected content 'the settings to their' missing: " + plainText);
    }

    /**
     * When §r is present, the output should have properly nested tags with bold NOT
     * continuing through the color change.
     */
    @Test
    void testBoldStopsAtReset() {
        String legacy = "§aResets §c§lALL §r§athe settings to their§r";
        String miniMsg = Util.legacyToMiniMessage(legacy);

        Component comp = Util.parseMiniMessage(miniMsg);
        String plainText = PlainTextComponentSerializer.plainText().serialize(comp);

        assertFalse(plainText.contains("</bold>"),
                "Plain text should not contain literal </bold> tag but was: " + plainText);
        assertEquals("Resets ALL the settings to their", plainText);
    }

    /**
     * The full MiniMessage → legacy → MiniMessage roundtrip must not produce literal tags
     * in the final output.
     */
    @Test
    void testRoundTripDoesNotProduceLiteralTags() {
        String original = "<green>Resets </green><red><bold>ALL </bold></red><green>the settings to their</green>";

        // MiniMessage → Component → Legacy
        Component comp = Util.parseMiniMessage(original);
        String legacy = Util.componentToLegacy(comp);

        // Legacy → MiniMessage → Component
        Component finalComp = Util.parseMiniMessageOrLegacy(legacy);
        String plainText = PlainTextComponentSerializer.plainText().serialize(finalComp);

        assertFalse(plainText.contains("</bold>"),
                "Round-trip should not produce literal </bold>: " + plainText);
        assertFalse(plainText.contains("<bold>"),
                "Round-trip should not produce literal <bold>: " + plainText);
        assertEquals("Resets ALL the settings to their", plainText);
    }

    /**
     * Multiple decorations (bold + italic) carrying through a color change should
     * all be properly nested.
     */
    @Test
    void testMultipleDecorationsCarryThroughColorChange() {
        // §c§l§o = red bold italic, then §a = green (both bold and italic continue)
        String legacy = "§c§l§oText1 §aText2§r";
        String miniMsg = Util.legacyToMiniMessage(legacy);

        Component comp = Util.parseMiniMessage(miniMsg);
        String plainText = PlainTextComponentSerializer.plainText().serialize(comp);

        assertFalse(plainText.contains("</bold>"),
                "Plain text should not contain literal </bold>: " + plainText);
        assertFalse(plainText.contains("</italic>"),
                "Plain text should not contain literal </italic>: " + plainText);
        assertEquals("Text1 Text2", plainText);
    }

    /**
     * Hex color codes (&#RRGGBB) with bold should also produce proper nesting.
     */
    @Test
    void testBoldCarriesThroughHexColor() {
        String legacy = "§lBold &#FF0000Red§r";
        String miniMsg = Util.legacyToMiniMessage(legacy);

        Component comp = Util.parseMiniMessage(miniMsg);
        String plainText = PlainTextComponentSerializer.plainText().serialize(comp);

        assertFalse(plainText.contains("</bold>"),
                "Plain text should not contain literal </bold>: " + plainText);
        assertEquals("Bold Red", plainText);
    }
}
