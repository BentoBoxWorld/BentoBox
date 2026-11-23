package world.bentobox.bentobox.api.prompts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.dialogs.DialogField;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Asks a player to type a value. Make one with {@link Prompts}, describe it, then
 * {@link #open() open} it:
 * <pre>{@code
 * Prompts.integer(user, 1, 64)
 *     .title("myaddon.amount.title")
 *     .initial(current)
 *     .onSubmit(amount -> setAmount(amount))
 *     .open();
 * }</pre>
 * The prompt is a dialog with a text box where dialogs are available and enabled, and a
 * question in chat otherwise. In both cases an answer that cannot be read, or that a
 * {@link #validate(Function) check} refuses, is explained to the player and asked for again,
 * so {@link #onSubmit(Consumer)} only ever receives a good value. Callbacks run on the main
 * thread.
 * <p>
 * {@link #onCancel(Runnable)} runs when the player presses the cancel button, types the
 * cancel word in chat, or does not answer a chat prompt in time. It does not run when a
 * dialog is dismissed with Escape, because the server is not told about that.
 *
 * @param <T> the type of value asked for
 * @author tastybento
 * @since 3.25.0
 */
public final class InputPrompt<T> {

    /**
     * What a typed answer was read as.
     *
     * @param value the value, or null if the text could not be read
     * @param error the already translated reason it could not be read, or null if it could
     * @param <T>   the type of value
     */
    record Parsed<T>(@Nullable T value, @Nullable String error) {
        static <T> Parsed<T> ok(@NonNull T value) {
            return new Parsed<>(value, null);
        }

        static <T> Parsed<T> error(@NonNull String error) {
            return new Parsed<>(null, error);
        }
    }

    /** The key of the dialog's one field */
    static final String FIELD = "value";
    /** The most lines a multi-line dialog box holds */
    private static final int MAX_LINES = 64;
    private static final int LINE_WIDTH = 200;
    private static final int LINES_WIDTH = 300;
    private static final String CANCEL = "[cancel]";
    private static final String DONE = "[done]";

    private final User user;
    private final boolean multiline;
    private final Function<String, Parsed<T>> parser;
    private final Function<T, String> formatter;
    private final List<Component> message = new ArrayList<>();
    private final List<Function<T, String>> checks = new ArrayList<>();
    private Component title = Component.empty();
    private @Nullable Component label;
    private String initial = "";
    private int maxLength;
    private boolean required = true;
    private String submitButton = "prompts.buttons.submit";
    private String cancelButton = "commands.confirmation.buttons.cancel";
    private Consumer<T> onSubmit = value -> {
        // Nothing by default
    };
    private Runnable onCancel = () -> {
        // Nothing by default
    };

    InputPrompt(@NonNull User user, boolean multiline, @NonNull Function<String, Parsed<T>> parser,
            @NonNull Function<T, String> formatter, int maxLength) {
        this.user = user;
        this.multiline = multiline;
        this.parser = parser;
        this.formatter = formatter;
        this.maxLength = maxLength;
    }

    /**
     * Sets the heading: the dialog's title, or the first line sent in chat.
     *
     * @param title the title
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> title(@NonNull Component title) {
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
    public InputPrompt<T> title(@NonNull String reference, String... variables) {
        return title(translate(reference, variables));
    }

    /**
     * Adds a line of explanation, shown under the heading. Do not describe how to answer or
     * cancel here; that depends on how the prompt is shown and is added for you.
     *
     * @param line the line
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> message(@NonNull Component line) {
        this.message.add(line);
        return this;
    }

    /**
     * Adds a line of explanation from a translation for the player being asked.
     *
     * @param reference the locale key
     * @param variables optional translation variables
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> message(@NonNull String reference, String... variables) {
        return message(translate(reference, variables));
    }

    /**
     * Labels the dialog's text box. Without a label the box is shown on its own. Not used in
     * chat.
     *
     * @param reference the locale key
     * @param variables optional translation variables
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> label(@NonNull String reference, String... variables) {
        this.label = translate(reference, variables);
        return this;
    }

    /**
     * Sets the value the dialog's text box starts with, so the player can edit what is there
     * instead of typing it again. Not used in chat.
     *
     * @param value the starting value, or null for none
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> initial(@Nullable T value) {
        this.initial = value == null ? "" : formatter.apply(value);
        return this;
    }

    /**
     * Sets the most characters the answer may have. A longer answer is refused.
     *
     * @param maxLength the limit, at least 1
     * @return this prompt
     * @throws IllegalArgumentException if the limit is less than 1
     */
    @NonNull
    public InputPrompt<T> maxLength(int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("A prompt needs a max length of at least 1, not " + maxLength);
        }
        this.maxLength = maxLength;
        return this;
    }

    /**
     * Lets the player give an empty answer. By default single-line prompts refuse one.
     *
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> optional() {
        this.required = false;
        return this;
    }

    /**
     * Adds a check on the value. Checks run in the order added, after the answer has been
     * read as a value, and the first one to object stops the rest.
     *
     * @param check returns null if the value is fine, or else an already translated message
     *              telling the player what is wrong with it
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> validate(@NonNull Function<T, @Nullable String> check) {
        this.checks.add(check);
        return this;
    }

    /**
     * Sets the text of the dialog's submit button. Not used in chat.
     *
     * @param reference the locale key
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> submitButton(@NonNull String reference) {
        this.submitButton = reference;
        return this;
    }

    /**
     * Sets the text of the dialog's cancel button. Not used in chat.
     *
     * @param reference the locale key
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> cancelButton(@NonNull String reference) {
        this.cancelButton = reference;
        return this;
    }

    /**
     * Sets what to do with the value once the player has given a good one.
     *
     * @param onSubmit receives the value, on the main thread
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> onSubmit(@NonNull Consumer<T> onSubmit) {
        this.onSubmit = onSubmit;
        return this;
    }

    /**
     * Sets what to do if the player backs out, e.g. reopen the panel they came from.
     *
     * @param onCancel run on the main thread
     * @return this prompt
     */
    @NonNull
    public InputPrompt<T> onCancel(@NonNull Runnable onCancel) {
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
        if (Prompts.usesDialog(user) && showDialog(initial, null)) {
            return;
        }
        askInChat();
    }

    /**
     * Reads an answer, checks it and, if it is good, delivers it.
     *
     * @param raw what the player typed
     * @return null if the answer was accepted, otherwise the message to show the player
     */
    @Nullable
    String submit(@NonNull String raw) {
        String text = multiline ? raw : raw.strip();
        if (text.length() > maxLength) {
            return user.getTranslation("prompts.errors.too-long", TextVariables.NUMBER, String.valueOf(maxLength));
        }
        if (required && text.isBlank()) {
            return user.getTranslation("prompts.errors.empty");
        }
        Parsed<T> parsed = parser.apply(text);
        if (parsed.value() == null) {
            return parsed.error();
        }
        for (Function<T, String> check : checks) {
            String error = check.apply(parsed.value());
            if (error != null) {
                return error;
            }
        }
        onSubmit.accept(parsed.value());
        return null;
    }

    /**
     * Shows the dialog.
     *
     * @param text  what the text box starts with
     * @param error a message about the last answer, or null if this is the first time of asking
     * @return false if the dialog could not be shown, in which case the caller asks in chat
     */
    private boolean showDialog(String text, @Nullable String error) {
        try {
            DialogBuilder builder = new DialogBuilder().title(title);
            message.forEach(builder::body);
            if (error != null) {
                builder.body(Util.parseMiniMessageOrLegacy(error));
            }
            DialogField.Text field = DialogField.text(FIELD, label == null ? Component.empty() : label, text)
                    .maxLength(Math.max(maxLength, text.length())).labelVisible(label != null)
                    .width(multiline ? LINES_WIDTH : LINE_WIDTH);
            builder.input(multiline ? field.multiline(MAX_LINES) : field)
                    .confirmation(DialogButton.submit(user, submitButton,
                            (u, response) -> answeredInDialog(response.getText(FIELD).orElse(""))),
                            DialogButton.of(user, cancelButton, u -> onCancel.run()));
            builder.build().show(user);
            return true;
        } catch (Exception | LinkageError e) {
            BentoBox.getInstance().logError("Could not show a prompt dialog, asking in chat instead: " + e.getMessage());
            return false;
        }
    }

    private void answeredInDialog(String raw) {
        String error = submit(raw);
        if (error != null && !showDialog(raw, error)) {
            user.sendRawMessage(error);
            askInChat();
        }
    }

    private void askInChat() {
        if (!Component.empty().equals(title)) {
            user.sendMessage(title);
        }
        message.forEach(user::sendMessage);
        String cancelWord = word("prompts.chat.cancel-word");
        String doneWord = word("prompts.chat.done-word");
        user.sendMessage(multiline ? "prompts.chat.lines-instructions" : "prompts.chat.instructions", CANCEL,
                cancelWord, DONE, doneWord);
        List<String> lines = new ArrayList<>();
        ChatInputListener.listen(user, text -> answeredInChat(text, lines, cancelWord, doneWord), onCancel);
    }

    /**
     * @return true if the prompt is finished
     */
    private boolean answeredInChat(String text, List<String> lines, String cancelWord, String doneWord) {
        if (text.equalsIgnoreCase(cancelWord)) {
            user.sendMessage("general.errors.command-cancelled");
            onCancel.run();
            return true;
        }
        String raw = text;
        if (multiline) {
            if (!text.equalsIgnoreCase(doneWord)) {
                lines.add(text);
                return false;
            }
            raw = String.join("\n", lines);
        }
        String error = submit(raw);
        if (error == null) {
            return true;
        }
        // Start again: the player cannot go back and edit lines already sent
        lines.clear();
        user.sendRawMessage(error);
        return false;
    }

    private String word(String reference) {
        return Util.stripColor(user.getTranslation(reference)).strip();
    }

    private Component translate(String reference, String... variables) {
        return Util.parseMiniMessageOrLegacy(user.getTranslation(reference, variables));
    }
}
