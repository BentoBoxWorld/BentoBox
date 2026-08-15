package world.bentobox.bentobox.api.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;

/**
 * Tests the Dialogs API builder (#3021).
 * <p>
 * Note: {@link DialogBuilder#build()} of a complete dialog needs the server's
 * dialog registry provider (Paper {@code DialogInstancesProvider}), which
 * MockBukkit does not supply - much like NMS paste handling. Those paths are
 * therefore exercised on a live server, not here. This test covers the pure
 * logic: support detection, button holders and pre-factory validation.
 *
 * @author tastybento
 */
class DialogBuilderTest extends CommonTestSetup {

    private User user;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = User.getInstance(mockPlayer);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link Dialogs#isSupported()}. The Paper dialog classes are on
     * the compile/test classpath, so support must be reported.
     */
    @Test
    void testDialogsSupported() {
        assertTrue(Dialogs.isSupported());
    }

    /**
     * Test method for {@link DialogButton}.
     */
    @Test
    void testDialogButtonHoldsFields() {
        AtomicReference<User> clicked = new AtomicReference<>();
        DialogButton b = new DialogButton(Component.text("Go"), Component.text("tip"), clicked::set);
        assertNotNull(b.label());
        assertNotNull(b.tooltip());
        assertNotNull(b.onClick());
        b.onClick().accept(user);
        // The callback ran with our user
        assertSame(user, clicked.get());
    }

    /**
     * Test method for {@link DialogButton} with no tooltip.
     */
    @Test
    void testDialogButtonNoTooltip() {
        DialogButton b = new DialogButton(Component.text("Go"), null);
        assertNotNull(b.label());
        assertTrue(b.tooltip() == null);
        assertTrue(b.onClick() == null);
    }

    /**
     * Test method for {@link DialogBuilder#build()} - a dialog with neither a
     * confirmation nor any button is invalid, and this is rejected before any
     * server-side factory is touched.
     */
    @Test
    void testBuildNoButtonsThrows() {
        DialogBuilder builder = new DialogBuilder().title(Component.text("Empty"));
        assertThrows(IllegalStateException.class, builder::build);
    }

    /**
     * Test method for {@link DialogButton#width()}. A button that never asks for a width
     * reports the default, which is the signal to build it without one.
     */
    @Test
    void testDialogButtonDefaultWidth() {
        assertEquals(DialogButton.DEFAULT_WIDTH, new DialogButton(Component.text("Go"), null).width());
        assertEquals(DialogButton.DEFAULT_WIDTH,
                DialogButton.of(user, "general.buttons.confirm", null).width());
    }

    /**
     * Test method for {@link DialogButton#DialogButton(Component, Component, int, Consumer)}.
     */
    @Test
    void testDialogButtonKeepsItsWidth() {
        DialogButton b = new DialogButton(Component.text("*"), Component.text("tip"), 24, null);
        assertEquals(24, b.width());
        assertNotNull(b.tooltip());
    }

    /**
     * Test method for {@link DialogButton#withWidth(int)} - a button from the locale
     * factory can still be sized, and the copy keeps everything else.
     */
    @Test
    void testWithWidthCopiesTheRest() {
        AtomicReference<User> clicked = new AtomicReference<>();
        DialogButton original = new DialogButton(Component.text("Go"), Component.text("tip"), clicked::set);
        DialogButton narrow = original.withWidth(30);
        assertEquals(30, narrow.width());
        assertSame(original.label(), narrow.label());
        assertSame(original.tooltip(), narrow.tooltip());
        assertSame(original.onClick(), narrow.onClick());
        // The original is untouched
        assertEquals(DialogButton.DEFAULT_WIDTH, original.width());
        narrow.onClick().accept(user);
        assertSame(user, clicked.get());
    }

    /**
     * Test method for {@link DialogButton} widths outside what the client accepts.
     */
    @Test
    void testDialogButtonWidthOutOfRangeThrows() {
        Component label = Component.text("*");
        assertThrows(IllegalArgumentException.class, () -> new DialogButton(label, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new DialogButton(label, null, -5, null));
        assertThrows(IllegalArgumentException.class, () -> new DialogButton(label, null, 1025, null));
        // The ends of the range are fine
        assertEquals(1, new DialogButton(label, null, 1, null).width());
        assertEquals(1024, new DialogButton(label, null, 1024, null).width());
    }

    /**
     * Test method for {@link DialogBuilder#columns(int)}.
     */
    @Test
    void testColumnsIsFluentAndValidated() {
        DialogBuilder builder = new DialogBuilder().title(Component.text("Grid"));
        assertSame(builder, builder.columns(13));
        assertThrows(IllegalArgumentException.class, () -> builder.columns(0));
        assertThrows(IllegalArgumentException.class, () -> builder.columns(-1));
    }
}
