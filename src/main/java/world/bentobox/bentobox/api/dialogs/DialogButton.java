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

    private final Component label;
    private final @Nullable Component tooltip;
    private final @Nullable Consumer<User> onClick;

    /**
     * Creates a button from Adventure components.
     *
     * @param label   the button label, not null
     * @param tooltip the hover tooltip, or null for none
     * @param onClick the action to run when clicked, or null for a button that just closes the dialog
     */
    public DialogButton(@NonNull Component label, @Nullable Component tooltip, @Nullable Consumer<User> onClick) {
        this.label = label;
        this.tooltip = tooltip;
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
     * @return the action to run when the button is clicked, or null if none
     */
    @Nullable
    public Consumer<User> onClick() {
        return onClick;
    }
}
