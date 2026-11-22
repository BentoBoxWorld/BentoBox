package world.bentobox.bentobox.api.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.dialogs.DialogField.Choice.Option;
import world.bentobox.bentobox.api.user.User;

/**
 * Tests the dialog form API: {@link DialogField}, {@link DialogResponse}, submit
 * {@link DialogButton buttons} and the form parts of {@link DialogBuilder} that can be
 * checked without the server's dialog registry.
 *
 * @author tastybento
 */
class DialogFieldTest extends CommonTestSetup {

    private static final Component LABEL = Component.text("Label");
    private User user;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = User.getInstance(mockPlayer);
    }

    // ---- Text ----

    @Test
    void testTextDefaults() {
        DialogField.Text t = DialogField.text("name", LABEL, "Bob");
        assertEquals("name", t.key());
        assertEquals("Bob", t.initial());
        assertEquals(DialogField.Text.DEFAULT_MAX_LENGTH, t.maxLength());
        assertFalse(t.isMultiline());
        assertTrue(t.labelVisible());
        assertEquals(DialogField.DEFAULT_WIDTH, t.width());
    }

    @Test
    void testTextNullInitialIsEmpty() {
        assertEquals("", DialogField.text("name", LABEL, null).initial());
    }

    @Test
    void testTextCopies() {
        DialogField.Text t = DialogField.text("desc", LABEL, "").maxLength(256).multiline(4).labelVisible(false)
                .width(300);
        assertEquals(256, t.maxLength());
        assertEquals(4, t.maxLines());
        assertTrue(t.isMultiline());
        assertFalse(t.labelVisible());
        assertEquals(300, t.width());
    }

    @Test
    void testTextLongInitialCanBeGivenALargerLimit() {
        String longText = "x".repeat(90);
        DialogField.Text t = DialogField.text("desc", LABEL, longText);
        assertEquals(90, t.maxLength());
        assertEquals(512, t.maxLength(512).maxLength());
        assertEquals(longText, t.maxLength(512).initial());
    }

    @Test
    void testTextInitialLongerThanMaxThrows() {
        DialogField.Text t = DialogField.text("name", LABEL, "0123456789");
        assertThrows(IllegalArgumentException.class, () -> t.maxLength(5));
    }

    @Test
    void testTextBadLimitsThrow() {
        DialogField.Text t = DialogField.text("name", LABEL, "");
        assertThrows(IllegalArgumentException.class, () -> t.maxLength(0));
        assertThrows(IllegalArgumentException.class, () -> t.multiline(0));
        assertThrows(IllegalArgumentException.class, () -> t.width(0));
        assertThrows(IllegalArgumentException.class, () -> t.width(DialogField.MAX_WIDTH + 1));
    }

    @Test
    void testTextLocalizedLabel() {
        DialogField.Text t = DialogField.text(user, "name", "some.reference", "x");
        assertEquals("x", t.initial());
        // The locales manager mock echoes the reference back
        assertTrue(t.label().toString().contains("some.reference"));
    }

    // ---- Keys ----

    @Test
    void testBadKeysThrow() {
        assertThrows(IllegalArgumentException.class, () -> DialogField.text("has space", LABEL, ""));
        assertThrows(IllegalArgumentException.class, () -> DialogField.toggle("dash-key", LABEL, true));
        assertThrows(IllegalArgumentException.class, () -> DialogField.number("", LABEL, 0, 1, 0));
        assertThrows(NullPointerException.class, () -> DialogField.toggle(null, LABEL, true));
    }

    @Test
    void testGoodKeys() {
        assertEquals("Mob_Limit_2", DialogField.toggle("Mob_Limit_2", LABEL, true).key());
    }

    // ---- Number ----

    @Test
    void testNumber() {
        DialogField.Number n = DialogField.number("chance", LABEL, 0, 100, 25).step(0.5f).labelFormat("fmt").width(250);
        assertEquals(0f, n.min());
        assertEquals(100f, n.max());
        assertEquals(25f, n.initial());
        assertEquals(0.5f, n.step());
        assertEquals("fmt", n.labelFormat());
        assertEquals(250, n.width());
    }

    @Test
    void testNumberFactoryClampsInitial() {
        assertEquals(100f, DialogField.number("n", LABEL, 0, 100, 500).initial());
        assertEquals(0f, DialogField.number("n", LABEL, 0, 100, -5).initial());
    }

    @Test
    void testNumberReversedRange() {
        // A slider may run high to low
        assertEquals(5f, DialogField.number("n", LABEL, 10, 0, 5).initial());
    }

    @Test
    void testNumberBadValuesThrow() {
        assertThrows(IllegalArgumentException.class, () -> new DialogField.Number("n", LABEL, 0, 10, 11, 0, null, 200));
        DialogField.Number n = DialogField.number("n", LABEL, 0, 10, 1);
        assertThrows(IllegalArgumentException.class, () -> n.step(-1));
    }

    // ---- Toggle ----

    @Test
    void testToggle() {
        DialogField.Toggle t = DialogField.toggle(user, "on", "ref", true);
        assertTrue(t.initial());
        assertEquals(DialogField.DEFAULT_WIDTH, t.width());
    }

    // ---- Choice ----

    @Test
    void testChoiceDefaultsToFirstOption() {
        DialogField.Choice c = DialogField.choice("mode", LABEL, List.of(Option.of("A"), Option.of("B")), null);
        assertEquals("A", c.initialId());
        assertEquals(2, c.options().size());
    }

    @Test
    void testChoiceInitial() {
        DialogField.Choice c = DialogField.choice(user, "mode", "ref",
                List.of(Option.of(user, "A", "ref.a"), Option.of("B")), "B").labelVisible(false).width(120);
        assertEquals("B", c.initialId());
        assertFalse(c.labelVisible());
        assertEquals(120, c.width());
    }

    @Test
    void testChoiceOptionsAreCopied() {
        List<Option> options = new ArrayList<>(List.of(Option.of("A")));
        DialogField.Choice c = DialogField.choice("mode", LABEL, options, null);
        options.add(Option.of("B"));
        assertEquals(1, c.options().size());
        List<Option> copy = c.options();
        Option extra = Option.of("C");
        assertThrows(UnsupportedOperationException.class, () -> copy.add(extra));
    }

    @Test
    void testChoiceBadOptionsThrow() {
        List<Option> none = List.of();
        assertThrows(IllegalArgumentException.class, () -> DialogField.choice("mode", LABEL, none, null));
        List<Option> dupes = List.of(Option.of("A"), Option.of("A"));
        assertThrows(IllegalArgumentException.class, () -> DialogField.choice("mode", LABEL, dupes, null));
        List<Option> ab = List.of(Option.of("A"), Option.of("B"));
        assertThrows(IllegalArgumentException.class, () -> DialogField.choice("mode", LABEL, ab, "C"));
    }

    // ---- Response ----

    @Test
    void testResponseTypes() {
        DialogResponse r = DialogResponse.of(Map.of("name", "Bob", "chance", 12.6f, "on", true, "count", 3));
        assertEquals(Optional.of("Bob"), r.getText("name"));
        assertEquals(Optional.of("Bob"), r.getChoice("name"));
        assertEquals(Optional.of(12.6f), r.getNumber("chance"));
        assertEquals(Optional.of(13), r.getInt("chance"));
        assertEquals(Optional.of(3f), r.getNumber("count"));
        assertEquals(Optional.of(true), r.getBoolean("on"));
    }

    @Test
    void testResponseWrongTypeOrMissingIsEmpty() {
        DialogResponse r = DialogResponse.of(Map.of("name", "Bob"));
        assertTrue(r.getNumber("name").isEmpty());
        assertTrue(r.getBoolean("name").isEmpty());
        assertTrue(r.getText("missing").isEmpty());
        assertTrue(DialogResponse.EMPTY.getText("name").isEmpty());
    }

    @Test
    void testResponseFromNullView() {
        assertSame(DialogResponse.EMPTY, DialogResponse.from(null));
    }

    // ---- Buttons ----

    @Test
    void testSubmitButtonGetsResponse() {
        AtomicReference<String> got = new AtomicReference<>();
        DialogButton b = DialogButton.submit(Component.text("Save"),
                (u, r) -> got.set(u.getName() + ":" + r.getText("name").orElse("?")));
        assertTrue(b.isSubmit());
        b.onSubmit().accept(user, DialogResponse.of(Map.of("name", "Bob")));
        assertEquals("tastybento:Bob", got.get());
        // onClick runs the same action with an empty response
        b.onClick().accept(user);
        assertEquals("tastybento:?", got.get());
    }

    @Test
    void testPlainButtonIgnoresResponse() {
        AtomicReference<User> clicked = new AtomicReference<>();
        DialogButton b = new DialogButton(Component.text("Go"), clicked::set);
        assertFalse(b.isSubmit());
        b.onSubmit().accept(user, DialogResponse.of(Map.of("x", "y")));
        assertSame(user, clicked.get());
    }

    @Test
    void testLocalizedSubmitButtonCopies() {
        DialogButton b = DialogButton.submit(user, "general.buttons.save", (u, r) -> {}).withWidth(60)
                .withTooltip(Component.text("tip"));
        assertTrue(b.isSubmit());
        assertEquals(60, b.width());
        assertEquals(Component.text("tip"), b.tooltip());
    }

    // ---- Builder ----

    @Test
    void testBuilderInputsKeepOrder() {
        DialogBuilder builder = new DialogBuilder().input(DialogField.text("a", LABEL, ""))
                .input(DialogField.toggle("b", LABEL, false));
        assertEquals(List.of("a", "b"), builder.getInputs().stream().map(DialogField::key).toList());
    }

    @Test
    void testBuilderDuplicateKeyThrows() {
        DialogBuilder builder = new DialogBuilder().input(DialogField.text("a", LABEL, ""));
        DialogField dupe = DialogField.toggle("a", LABEL, false);
        assertThrows(IllegalArgumentException.class, () -> builder.input(dupe));
    }

    @Test
    void testBuilderTypesAreExclusive() {
        DialogButton ok = new DialogButton(Component.text("OK"), null);
        DialogBuilder notice = new DialogBuilder().notice(ok);
        assertThrows(IllegalStateException.class, () -> notice.button(ok));
        assertThrows(IllegalStateException.class, () -> notice.confirmation(ok, ok));
        DialogBuilder menu = new DialogBuilder().button(ok);
        assertThrows(IllegalStateException.class, () -> menu.notice(ok));
        assertThrows(IllegalStateException.class, () -> menu.confirmation(ok, ok));
        DialogBuilder confirm = new DialogBuilder().confirmation(ok, ok);
        assertThrows(IllegalStateException.class, () -> confirm.button(ok));
        assertThrows(IllegalStateException.class, () -> confirm.notice(ok));
    }

    @Test
    void testBuilderItemBodyAndAfterActionChain() {
        DialogBuilder builder = new DialogBuilder().body(new ItemStack(Material.STONE), Component.text("Stone"))
                .body(new ItemStack(Material.DIRT), null).afterAction(DialogBuilder.AfterAction.WAIT_FOR_RESPONSE);
        // Still needs a button before it can be built
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void testAfterActionNamesMatchPaper() {
        // build() maps by name, so every value must exist in Paper's enum
        for (DialogBuilder.AfterAction a : DialogBuilder.AfterAction.values()) {
            assertEquals(a.name(),
                    io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction.valueOf(a.name()).name());
        }
    }
}
