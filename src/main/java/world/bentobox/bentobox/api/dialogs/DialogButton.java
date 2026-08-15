package world.bentobox.bentobox.api.dialogs;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * A single clickable button in a {@link DialogBuilder dialog}.
 * <p>
 * A button has a label, an optional tooltip shown on hover and an optional
 * {@link #onClick()} callback. The callback is run on the server's main thread,
 * with the {@link User} who clicked, when the button is pressed.
 *
 * @author tastybento
 * @since 3.21.0
 */
public class DialogButton {

    /**
     * Width of a button that has not asked for one. A button left at this width is built
     * without an explicit width at all, so the client lays it out however it normally
     * would.
     *
     * @since 3.22.3
     */
    public static final int DEFAULT_WIDTH = 150;

    /** Narrowest button the client accepts */
    private static final int MIN_WIDTH = 1;

    /** Widest button the client accepts */
    private static final int MAX_WIDTH = 1024;

    private final Component label;
    private final @Nullable Component tooltip;
    private final int width;
    private final @Nullable Consumer<User> onClick;

    /**
     * Creates a button from Adventure components.
     *
     * @param label   the button label, not null
     * @param tooltip the hover tooltip, or null for none
     * @param onClick the action to run when clicked, or null for a button that just closes the dialog
     */
    public DialogButton(@NonNull Component label, @Nullable Component tooltip, @Nullable Consumer<User> onClick) {
        this(label, tooltip, DEFAULT_WIDTH, onClick);
    }

    /**
     * Creates a button of a given width.
     * <p>
     * Width matters when buttons are laid out in a grid with
     * {@link DialogBuilder#columns(int)}: narrow buttons let a row hold more of them
     * before the client squeezes the outer columns off the screen.
     *
     * @param label   the button label, not null
     * @param tooltip the hover tooltip, or null for none
     * @param width   the button width, 1 to 1024, or {@link #DEFAULT_WIDTH} to leave it to
     *                the client
     * @param onClick the action to run when clicked, or null for a button that just closes the dialog
     * @throws IllegalArgumentException if the width is outside 1 to 1024
     * @since 3.22.3
     */
    public DialogButton(@NonNull Component label, @Nullable Component tooltip, int width,
            @Nullable Consumer<User> onClick) {
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new IllegalArgumentException(
                    "Button width must be between " + MIN_WIDTH + " and " + MAX_WIDTH + ", not " + width);
        }
        this.label = label;
        this.tooltip = tooltip;
        this.width = width;
        this.onClick = onClick;
    }

    /**
     * Creates a button from a component label with no tooltip.
     *
     * @param label   the button label, not null
     * @param onClick the action to run when clicked, or null
     */
    public DialogButton(@NonNull Component label, @Nullable Consumer<User> onClick) {
        this(label, null, onClick);
    }

    /**
     * Creates a button whose label is a localized translation for the given user.
     * The reference is translated and parsed to a component so that colors and
     * formatting in the locale entry are honored.
     *
     * @param user      the user whose locale is used, not null
     * @param reference the locale key of the label, not null
     * @param onClick   the action to run when clicked, or null
     * @return a new button
     */
    @NonNull
    public static DialogButton of(@NonNull User user, @NonNull String reference, @Nullable Consumer<User> onClick) {
        return new DialogButton(Util.parseMiniMessageOrLegacy(user.getTranslation(reference)), onClick);
    }

    /**
     * Returns a copy of this button at the given width, so a button made by
     * {@link #of(User, String, Consumer)} can still be sized.
     *
     * @param width the button width, 1 to 1024
     * @return a new button, identical but for its width
     * @throws IllegalArgumentException if the width is outside 1 to 1024
     * @since 3.22.3
     */
    @NonNull
    public DialogButton withWidth(int width) {
        return new DialogButton(label, tooltip, width, onClick);
    }

    /**
     * @return the button label
     */
    @NonNull
    public Component label() {
        return label;
    }

    /**
     * @return the hover tooltip, or null if there is none
     */
    @Nullable
    public Component tooltip() {
        return tooltip;
    }

    /**
     * @return the button width, or {@link #DEFAULT_WIDTH} if none was asked for
     * @since 3.22.3
     */
    public int width() {
        return width;
    }

    /**
     * @return the action to run when the button is clicked, or null if none
     */
    @Nullable
    public Consumer<User> onClick() {
        return onClick;
    }
}
