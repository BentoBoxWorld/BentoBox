package world.bentobox.bentobox.api.dialogs;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

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
}
