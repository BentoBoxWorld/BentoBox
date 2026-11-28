package world.bentobox.bentobox.api.prompts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Asks a player to confirm something before it is done. Make one with
 * {@link Prompts#confirm(User)}:
 * <pre>{@code
 * Prompts.confirm(user)
 *     .message("myaddon.wipe.warning")
 *     .onConfirm(this::wipe)
 *     .open();
 * }</pre>
 * Shown as a dialog with confirm and cancel buttons where dialogs are available and enabled.
 * Otherwise the player is asked in chat and must type the confirm word; anything else
 * cancels.
 * <p>
 * For confirming a command, use
 * {@link world.bentobox.bentobox.api.commands.ConfirmableCommand} instead.
 *
 * @author tastybento
 * @since 3.25.0
 */
public final class ConfirmationPrompt {

    private static final String CONFIRM_BUTTON = "commands.confirmation.buttons.confirm";
    private static final String CANCEL_BUTTON = "commands.confirmation.buttons.cancel";

    private final User user;
    private final List<Component> message = new ArrayList<>();
    private Component title;
    private Runnable onConfirm = () -> {
        // Nothing by default
    };
    private Runnable onCancel = () -> {
        // Nothing by default
    };

    ConfirmationPrompt(@NonNull User user) {
        this.user = user;
        this.title = translate("commands.confirmation.title");
    }

    /**
     * Sets the heading. The default asks "Are you sure?".
     *
     * @param title the title
     * @return this prompt
     */
    @NonNull
    public ConfirmationPrompt title(@NonNull Component title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the heading from a translation for the player being asked.
     *
     * @param reference the locale key
     * @param variables optional translation variables
     * @return this prompt
     */
    @NonNull
    public ConfirmationPrompt title(@NonNull String reference, String... variables) {
        return title(translate(reference, variables));
    }

    /**
     * Adds a line saying what is about to happen. Do not describe how to confirm; that
     * depends on how the prompt is shown and is added for you.
     *
     * @param line the line
     * @return this prompt
     */
    @NonNull
    public ConfirmationPrompt message(@NonNull Component line) {
        this.message.add(line);
        return this;
    }

    /**
     * Adds a line saying what is about to happen, from a translation for the player being
     * asked.
     *
     * @param reference the locale key
     * @param variables optional translation variables
     * @return this prompt
     */
    @NonNull
    public ConfirmationPrompt message(@NonNull String reference, String... variables) {
        return message(translate(reference, variables));
    }

    /**
     * Sets what to do when the player confirms.
     *
     * @param onConfirm run on the main thread
     * @return this prompt
     */
    @NonNull
    public ConfirmationPrompt onConfirm(@NonNull Runnable onConfirm) {
        this.onConfirm = onConfirm;
        return this;
    }

    /**
     * Sets what to do when the player declines or, in chat, does not answer in time. It does
     * not run when a dialog is dismissed with Escape.
     *
     * @param onCancel run on the main thread
     * @return this prompt
     */
    @NonNull
    public ConfirmationPrompt onCancel(@NonNull Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    /**
     * Asks the player. Does nothing if the user is not an online player. Any chat prompt the
     * player still has waiting is dropped.
     */
    public void open() {
        if (!user.isPlayer()) {
            return;
        }
        ChatInputListener.stop(user.getUniqueId());
        if (Prompts.usesDialog(user) && showDialog()) {
            return;
        }
        askInChat();
    }

    private boolean showDialog() {
        try {
            DialogBuilder builder = new DialogBuilder().title(title);
            message.forEach(builder::body);
            if (message.isEmpty()) {
                builder.body(user, "commands.confirmation.dialog-body");
            }
            builder.confirmation(DialogButton.of(user, CONFIRM_BUTTON, u -> onConfirm.run()),
                    DialogButton.of(user, CANCEL_BUTTON, u -> cancelled())).build().show(user);
            return true;
        } catch (Exception | LinkageError e) {
            BentoBox.getInstance()
                    .logError("Could not show a confirmation dialog, asking in chat instead: " + e.getMessage());
            return false;
        }
    }

    private void askInChat() {
        user.sendMessage(title);
        message.forEach(user::sendMessage);
        String confirmWord = Util.stripColor(user.getTranslation("prompts.chat.confirm-word")).strip();
        user.sendMessage("prompts.chat.confirm-instructions", "[confirm]", confirmWord);
        ChatInputListener.listen(user, text -> {
            if (text.equalsIgnoreCase(confirmWord)) {
                onConfirm.run();
            } else {
                cancelled();
            }
            return true;
        }, onCancel);
    }

    private void cancelled() {
        user.sendMessage("general.errors.command-cancelled");
        onCancel.run();
    }

    private Component translate(String reference, String... variables) {
        return Util.parseMiniMessageOrLegacy(user.getTranslation(reference, variables));
    }
}
