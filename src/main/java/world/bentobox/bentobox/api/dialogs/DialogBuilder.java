package world.bentobox.bentobox.api.dialogs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Fluent builder for modal {@link BBDialog dialogs}, in the spirit of
 * {@code PanelBuilder}.
 * <p>
 * A dialog needs a {@link #title(Component) title} and either a
 * {@link #confirmation(DialogButton, DialogButton) confirmation} (two buttons)
 * or one or more {@link #button(DialogButton) buttons} (a multi-action menu).
 * Titles and body lines are Adventure {@link Component Components}; the
 * {@code title}/{@code body} overloads that take a {@link User} translate a
 * locale key for that user and parse colors/formatting.
 * <p>
 * Example:
 * <pre>{@code
 * new DialogBuilder()
 *     .title(user, "commands.island.reset.confirm-title")
 *     .body(user, "commands.island.reset.confirm-body")
 *     .confirmation(
 *         DialogButton.of(user, "general.buttons.confirm", u -> doReset(u)),
 *         DialogButton.of(user, "general.buttons.cancel", null))
 *     .build()
 *     .show(user);
 * }</pre>
 *
 * @author tastybento
 * @since 3.21.0
 */
public class DialogBuilder {

    /** How long a button's server-side click callback stays valid after the dialog is shown. */
    private static final Duration CALLBACK_LIFETIME = Duration.ofMinutes(10);

    private Component title = Component.empty();
    private final List<Component> body = new ArrayList<>();
    private boolean escapable = true;
    private boolean pause = false;

    private DialogButton yesButton;
    private DialogButton noButton;
    private final List<DialogButton> buttons = new ArrayList<>();

    /**
     * Sets the dialog title from a component.
     *
     * @param title the title, not null
     * @return this builder
     */
    public DialogBuilder title(@NonNull Component title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the dialog title from a localized translation for the given user.
     *
     * @param user      the user whose locale is used, not null
     * @param reference the locale key, not null
     * @param variables optional translation variables
     * @return this builder
     */
    public DialogBuilder title(@NonNull User user, @NonNull String reference, String... variables) {
        return title(Util.parseMiniMessageOrLegacy(user.getTranslation(reference, variables)));
    }

    /**
     * Adds a line of body text from a component.
     *
     * @param line the body line, not null
     * @return this builder
     */
    public DialogBuilder body(@NonNull Component line) {
        this.body.add(line);
        return this;
    }

    /**
     * Adds a line of body text from a localized translation for the given user.
     *
     * @param user      the user whose locale is used, not null
     * @param reference the locale key, not null
     * @param variables optional translation variables
     * @return this builder
     */
    public DialogBuilder body(@NonNull User user, @NonNull String reference, String... variables) {
        return body(Util.parseMiniMessageOrLegacy(user.getTranslation(reference, variables)));
    }

    /**
     * Sets whether the player can dismiss the dialog by pressing Escape. Defaults
     * to {@code true}. Set to {@code false} for onboarding flows the player must
     * answer, but note this is hostile for routine confirmations.
     *
     * @param escapable true if Escape dismisses the dialog
     * @return this builder
     */
    public DialogBuilder escapable(boolean escapable) {
        this.escapable = escapable;
        return this;
    }

    /**
     * Sets whether the dialog pauses the game. This only has an effect in
     * single-player; on a server it is ignored. Defaults to {@code false}.
     *
     * @param pause true to pause the game while the dialog is open
     * @return this builder
     */
    public DialogBuilder pause(boolean pause) {
        this.pause = pause;
        return this;
    }

    /**
     * Makes this a two-button confirmation dialog. Mutually exclusive with
     * {@link #button(DialogButton)}.
     *
     * @param yes the affirmative button, not null
     * @param no  the negative button, not null
     * @return this builder
     */
    public DialogBuilder confirmation(@NonNull DialogButton yes, @NonNull DialogButton no) {
        this.yesButton = yes;
        this.noButton = no;
        return this;
    }

    /**
     * Adds a button to a multi-action menu dialog. Mutually exclusive with
     * {@link #confirmation(DialogButton, DialogButton)}.
     *
     * @param button the button, not null
     * @return this builder
     */
    public DialogBuilder button(@NonNull DialogButton button) {
        this.buttons.add(button);
        return this;
    }

    /**
     * Builds the dialog.
     *
     * @return the built dialog, ready to {@link BBDialog#show(User) show}
     * @throws IllegalStateException if neither a confirmation nor any button was set
     */
    @NonNull
    public BBDialog build() {
        boolean confirmation = yesButton != null && noButton != null;
        if (!confirmation && buttons.isEmpty()) {
            throw new IllegalStateException("A dialog needs either a confirmation() or at least one button()");
        }

        List<PlainMessageDialogBody> bodyLines = body.stream().map(DialogBody::plainMessage).toList();
        DialogBase base = DialogBase.builder(title).canCloseWithEscape(escapable).pause(pause)
                .afterAction(DialogBase.DialogAfterAction.CLOSE).body(bodyLines).build();

        DialogType type = confirmation ? DialogType.confirmation(toActionButton(yesButton), toActionButton(noButton))
                : DialogType.multiAction(buttons.stream().map(this::toActionButton).toList()).build();

        Dialog dialog = Dialog.create(factory -> factory.empty().base(base).type(type));
        return new BBDialog(dialog);
    }

    private ActionButton toActionButton(DialogButton button) {
        ActionButton.Builder b = ActionButton.builder(button.label())
                .action(DialogAction.customClick((view, audience) -> runOnMainThread(button.onClick(), audience),
                        ClickCallback.Options.builder().uses(1).lifetime(CALLBACK_LIFETIME).build()));
        if (button.tooltip() != null) {
            b.tooltip(button.tooltip());
        }
        return b.build();
    }

    /**
     * Runs a button callback on the server's main thread with the clicking user.
     * Server-side dialog callbacks may be delivered off the main thread, so BentoBox
     * / addon code (which is not thread-safe) is dispatched back to it.
     */
    private static void runOnMainThread(Consumer<User> onClick, Audience audience) {
        if (onClick == null || !(audience instanceof Player player)) {
            return;
        }
        User user = User.getInstance(player);
        Bukkit.getScheduler().runTask(BentoBox.getInstance(), () -> onClick.accept(user));
    }
}
