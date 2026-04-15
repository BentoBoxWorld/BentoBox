package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
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

        // Inspect the children of the round-tripped component: only the "ALL " segment
        // may be bold. Bold must NOT leak into "the settings to their".
        StringBuilder boldText = new StringBuilder();
        collectBoldText(finalComp, false, boldText);
        assertEquals("ALL ", boldText.toString(),
                "Bold should only apply to 'ALL ', not leak into following segments");
    }

    private static void collectBoldText(Component component, boolean inheritedBold, StringBuilder out) {
        collectDecoratedText(component, TextDecoration.BOLD, inheritedBold, out);
    }

    private static void collectDecoratedText(Component component, TextDecoration deco, boolean inherited, StringBuilder out) {
        TextDecoration.State state = component.decoration(deco);
        boolean effective = state == TextDecoration.State.TRUE
                || (state == TextDecoration.State.NOT_SET && inherited);
        if (component instanceof net.kyori.adventure.text.TextComponent text && effective) {
            out.append(text.content());
        }
        for (Component child : component.children()) {
            collectDecoratedText(child, deco, effective, out);
        }
    }

    /**
     * The same round-trip leak that affected bold also affected italic, underlined, strikethrough,
     * and obfuscated, because Adventure's LegacyComponentSerializer never emits §r when *any*
     * decoration transitions from on to off across siblings. Verify each decoration in turn.
     */
    @Test
    void testRoundTripNoDecorationLeaksAcrossSiblings() {
        TextDecoration[] decos = {
                TextDecoration.BOLD,
                TextDecoration.ITALIC,
                TextDecoration.UNDERLINED,
                TextDecoration.STRIKETHROUGH,
                TextDecoration.OBFUSCATED
        };
        String[] tags = {"bold", "italic", "underlined", "strikethrough", "obfuscated"};

        for (int i = 0; i < decos.length; i++) {
            String tag = tags[i];
            String original = "<green>before </green><red><" + tag + ">MID </" + tag + "></red><green>after</green>";
            Component comp = Util.parseMiniMessage(original);
            String legacy = Util.componentToLegacy(comp);
            Component finalComp = Util.parseMiniMessageOrLegacy(legacy);

            String plainText = PlainTextComponentSerializer.plainText().serialize(finalComp);
            assertEquals("before MID after", plainText, "plain text mismatch for " + tag);

            StringBuilder decoratedText = new StringBuilder();
            collectDecoratedText(finalComp, decos[i], false, decoratedText);
            assertEquals("MID ", decoratedText.toString(),
                    tag + " should only apply to 'MID ', not leak into following segments");
        }
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

    /**
     * Regression for <a href="https://github.com/BentoBoxWorld/AOneBlock/issues/495">
     * AOneBlock#495</a>: a MiniMessage locale string with a space between two colored
     * segments must retain the space after going through the componentToLegacy →
     * legacyToMiniMessage round-trip used by {@code User.sendMessage}.
     */
    @Test
    void testInterSegmentSpacePreservedAcrossRoundTrip() {
        String mm = "<red>Slow down.</red> <green>Click slower.</green>";
        Component c = Util.parseMiniMessage(mm);
        String legacy = Util.componentToLegacy(c);

        // componentToLegacy emits the §r-space-§a pattern for the inter-segment whitespace
        assertTrue(legacy.contains("\u00A7r "),
                "expected reset-then-space in legacy form, got: " + legacy);

        // Round-trip back through legacyToMiniMessage — the space must survive
        String mmAgain = Util.legacyToMiniMessage(legacy);
        assertFalse(mmAgain.contains("</red><green>"),
                "space was eaten between segments: " + mmAgain);

        Component c2 = Util.parseMiniMessage(mmAgain);
        String plain = PlainTextComponentSerializer.plainText().serialize(c2);
        assertEquals("Slow down. Click slower.", plain);
    }

    /**
     * Backwards compat: the legacy locale hack of stripping one space after a
     * color/decoration code must still work. Old locale files use {@code &c This is red}
     * so primitive auto-translators wouldn't glue the code onto the word.
     */
    @Test
    void testLegacyLocaleHackStillStripsSpaceAfterColorCode() {
        String mm = Util.legacyToMiniMessage("&c This is red");
        assertEquals("<red>This is red</red>", mm);
    }

    /**
     * The locale hack must NOT apply to {@code &r}. &amp;r is a format terminator and any
     * following space is intentional literal whitespace.
     */
    @Test
    void testLegacyResetDoesNotStripFollowingSpace() {
        String mm = Util.legacyToMiniMessage("&cHello&r world");
        String plain = PlainTextComponentSerializer.plainText().serialize(Util.parseMiniMessage(mm));
        assertEquals("Hello world", plain);
    }

    /**
     * Regression for <a href="https://github.com/BentoBoxWorld/AOneBlock/issues/495">
     * AOneBlock#495</a>: a "Page [page] of [total]" template with alternating colors.
     * The round-trip emits {@code §7Page §e1§7 of §e4}. The second {@code §7} is
     * preceded by a digit, not by a boundary, so the space after it is content and
     * must be preserved.
     */
    @Test
    void testMidTextColorCodeDoesNotStripContentSpace() {
        String mm = "<gray>Page </gray><yellow>1</yellow><gray> of </gray><yellow>4</yellow>";
        // Forward: MiniMessage → componentToLegacy
        Component c = Util.parseMiniMessage(mm);
        String legacy = Util.componentToLegacy(c);
        // Round-trip: legacy → Component
        Component finalComp = Util.parseMiniMessageOrLegacy(legacy);
        String plain = PlainTextComponentSerializer.plainText().serialize(finalComp);
        assertEquals("Page 1 of 4", plain);
    }

    /**
     * Same scenario exercised through {@code replaceLegacyCodesInline} (the mixed-content
     * path used when MiniMessage templates contain legacy-coded variable substitutions).
     */
    @Test
    void testMidTextCodeInReplaceLegacyCodesInlinePreservesSpace() {
        // Mixed content: MiniMessage tags with legacy codes embedded (e.g. from a variable).
        String mixed = "<gray>Page </gray>&e1&7 of &e4";
        String result = Util.replaceLegacyCodesInline(mixed);
        // The &7 here is preceded by "1" (mid-text), so the space after it must survive.
        String plain = PlainTextComponentSerializer.plainText().serialize(Util.parseMiniMessage(result));
        assertEquals("Page 1 of 4", plain);
    }

    /**
     * {@code stripSpaceAfterColorCodes} (used by the deprecated {@code translateColorCodes}
     * pure-legacy path) must use the same boundary rule.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testStripSpaceAfterColorCodesRespectsBoundary() {
        // Boundary cases: strip applies
        assertEquals("\u00A7cHello", Util.stripSpaceAfterColorCodes("\u00A7c Hello"));
        assertEquals("\u00A7l\u00A7cBold", Util.stripSpaceAfterColorCodes("\u00A7l\u00A7c Bold"));
        // Mid-text: must NOT strip
        assertEquals("\u00A77Page \u00A7e1\u00A77 of \u00A7e4",
                Util.stripSpaceAfterColorCodes("\u00A77Page \u00A7e1\u00A77 of \u00A7e4"));
        // Reset must NOT strip
        assertEquals("\u00A7r world", Util.stripSpaceAfterColorCodes("\u00A7r world"));
    }

    /**
     * Regression for <a href="https://github.com/BentoBoxWorld/BentoBox/issues/2943">BentoBox#2943</a>:
     * hex colors using the {@code &#RRGGBB} format were broken because
     * {@code translateColorCodes} serialises them to the BungeeCord
     * {@code §x§R§R§G§G§B§B} format, which {@code legacyToMiniMessage} (and
     * {@code replaceLegacyCodesInline}) did not recognise.  The colour was then
     * corrupted to a sequence of named colours (&amp;2, &amp;3, …) instead of
     * the intended hex value.
     */
    @Test
    void testBungeeCordHexFormatRoundTrip() {
        // Simulate the full path: user writes &#238af0&l in a locale file.
        // convertToLegacy (pure-legacy path) calls translateColorCodes which
        // serialises to §x§2§3§8§a§f§0§l.  sendRawMessage then calls
        // parseMiniMessageOrLegacy which must reconstruct the original colour.
        String bungeeHex = "\u00A7x\u00A72\u00A73\u00A78\u00A7a\u00A7f\u00A70\u00A7l";
        Component component = Util.parseMiniMessageOrLegacy(bungeeHex + "test");
        // The component must carry the original hex colour, not a named-colour approximation.
        net.kyori.adventure.text.format.TextColor color = component.children().isEmpty()
                ? component.color()
                : component.children().get(0).color();
        assertNotNull(color, "Expected a colour on the component");
        assertEquals(0x238af0, color.value(), "Hex colour #238af0 must survive the BungeeCord round-trip");
    }

    /**
     * {@code &#RRGGBB&l} (the raw user-written format) must also round-trip correctly
     * through {@code legacyToMiniMessage}.
     */
    @Test
    void testRawHexWithBoldRoundTrip() {
        String mm = Util.legacyToMiniMessage("&#238af0&lBold text");
        Component component = Util.parseMiniMessage(mm);
        // Must have the correct hex colour
        net.kyori.adventure.text.format.TextColor color = component.color() != null
                ? component.color()
                : component.children().isEmpty() ? null : component.children().get(0).color();
        assertNotNull(color, "Expected a colour");
        assertEquals(0x238af0, color.value());
    }
}
