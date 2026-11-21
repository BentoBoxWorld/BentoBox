package world.bentobox.bentobox.api.dialogs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
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
 * {@link #confirmation(DialogButton, DialogButton) confirmation} (two buttons),
 * a {@link #notice(DialogButton) notice} (one button), or one or more
 * {@link #button(DialogButton) buttons} (a multi-action menu).
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
 * <p>
 * A multi-action dialog can be laid out as a grid rather than the default two-wide
 * list, by asking for {@link #columns(int) columns} and giving the buttons a
 * {@link DialogButton#DialogButton(Component, Component, int, Consumer) width}:
 * <pre>{@code
 * DialogBuilder grid = new DialogBuilder().title(user, "mygame.map.title").columns(9);
 * for (Tile tile : tiles) {
 *     grid.button(new DialogButton(tile.glyph(), tile.tooltip(), 26, u -> select(u, tile)));
 * }
 * grid.build().show(user);
 * }</pre>
 *
 * <p>
 * A dialog can also be a form: add {@link #input(DialogField) fields} and give it a
 * {@link DialogButton#submit(Component, BiConsumer) submit button}, which receives what
 * the player entered. See {@link DialogField} for an example.
 *
 * @author tastybento
 * @since 3.21.0
 */
public class DialogBuilder {

    /**
     * What the client does after a button is clicked.
     *
     * @since 3.24.0
     */
    public enum AfterAction {
        /** Close the dialog. The default. */
        CLOSE,
        /** Leave the dialog open, e.g. so a form can be submitted again after a correction */
        NONE,
        /**
         * Show a "waiting for response" screen until the server shows another dialog or
         * closes it, e.g. while a form is checked, then re-shown with errors or replaced by
         * a result
         */
        WAIT_FOR_RESPONSE
    }

    /** A line of body text */
    private record TextLine(Component text) {}

    /** An item shown in the body, with optional text beside it */
    private record ItemLine(ItemStack item, @Nullable Component description) {}


    /** How long a button's server-side click callback stays valid after the dialog is shown. */
    private static final Duration CALLBACK_LIFETIME = Duration.ofMinutes(10);

    /**
     * Columns a multi-action dialog falls into when it does not ask for a number. This is
     * the client's own default, and a dialog left at it is built without stating a column
     * count at all.
     *
     * @since 3.22.3
     */
    public static final int DEFAULT_COLUMNS = 2;

    private Component title = Component.empty();
    private final List<Object> body = new ArrayList<>();
    private final List<DialogField> inputs = new ArrayList<>();
    private AfterAction afterAction = AfterAction.CLOSE;
    private DialogButton noticeButton;
    private boolean escapable = true;
    private boolean pause = false;
    private int columns = DEFAULT_COLUMNS;

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
        this.body.add(new TextLine(line));
        return this;
    }

    /**
     * Adds an item to the body, shown as it looks in an inventory, with optional text
     * beside it - e.g. the block or mob a form is about.
     *
     * @param item        the item, not null; a copy is shown
     * @param description text shown beside the item, or null for none
     * @return this builder
     * @since 3.24.0
     */
    public DialogBuilder body(@NonNull ItemStack item, @Nullable Component description) {
        this.body.add(new ItemLine(item.clone(), description));
        return this;
    }

    /**
     * Adds an input field, shown below the body in the order added. Read what the player
     * entered from the {@link DialogResponse} given to a
     * {@link DialogButton#submit(Component, BiConsumer) submit button}.
     *
     * @param field the field, not null
     * @return this builder
     * @throws IllegalArgumentException if another field already uses the same key
     * @since 3.24.0
     */
    public DialogBuilder input(@NonNull DialogField field) {
        if (inputs.stream().anyMatch(f -> f.key().equals(field.key()))) {
            throw new IllegalArgumentException("Two dialog fields share the key '" + field.key() + "'");
        }
        this.inputs.add(field);
        return this;
    }

    /**
     * @return the fields added so far, in order
     * @since 3.24.0
     */
    @NonNull
    public List<DialogField> getInputs() {
        return List.copyOf(inputs);
    }

    /**
     * Sets what the client does after any button is clicked. Defaults to
     * {@link AfterAction#CLOSE}.
     *
     * @param afterAction the action, not null
     * @return this builder
     * @since 3.24.0
     */
    public DialogBuilder afterAction(@NonNull AfterAction afterAction) {
        this.afterAction = afterAction;
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
        checkType(noticeButton == null && buttons.isEmpty());
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
        checkType(noticeButton == null && yesButton == null);
        this.buttons.add(button);
        return this;
    }

    /**
     * Makes this a notice: a message with a single button, e.g. to report the result of
     * an action. Mutually exclusive with {@link #confirmation(DialogButton, DialogButton)}
     * and {@link #button(DialogButton)}.
     *
     * @param ok the button, not null
     * @return this builder
     * @since 3.24.0
     */
    public DialogBuilder notice(@NonNull DialogButton ok) {
        checkType(yesButton == null && buttons.isEmpty());
        this.noticeButton = ok;
        return this;
    }

    private static void checkType(boolean ok) {
        if (!ok) {
            throw new IllegalStateException("A dialog is either a confirmation(), a notice() or a list of button()s");
        }
    }

    /**
     * Lays the buttons of a multi-action dialog out in this many columns, so they form a
     * grid rather than the default two-wide list. Defaults to {@link #DEFAULT_COLUMNS}.
     * <p>
     * How wide a grid actually fits depends on how wide its buttons are — see
     * {@link DialogButton#DialogButton(Component, Component, int, Consumer) the width
     * constructor}. Ask for more than the screen holds and the client squeezes the outer
     * columns off it, so a map-like grid wants narrow buttons.
     * <p>
     * Has no effect on a {@link #confirmation(DialogButton, DialogButton) confirmation}
     * dialog, whose two buttons are laid out by the client.
     *
     * @param columns the number of columns, at least 1
     * @return this builder
     * @throws IllegalArgumentException if columns is less than 1
     * @since 3.22.3
     */
    public DialogBuilder columns(int columns) {
        if (columns < 1) {
            throw new IllegalArgumentException("A dialog needs at least one column, not " + columns);
        }
        this.columns = columns;
        return this;
    }

    /**
     * Builds the dialog.
     *
     * @return the built dialog, ready to {@link BBDialog#show(User) show}
     * @throws IllegalStateException if no confirmation, notice or button was set
     */
    @NonNull
    public BBDialog build() {
        boolean confirmation = yesButton != null && noButton != null;
        if (!confirmation && noticeButton == null && buttons.isEmpty()) {
            throw new IllegalStateException("A dialog needs a confirmation(), a notice() or at least one button()");
        }

        List<DialogBody> bodyLines = body.stream().map(DialogBuilder::toBody).toList();
        DialogBase base = DialogBase.builder(title).canCloseWithEscape(escapable).pause(pause)
                .afterAction(DialogBase.DialogAfterAction.valueOf(afterAction.name())).body(bodyLines)
                .inputs(inputs.stream().map(DialogBuilder::toInput).toList()).build();

        DialogType type;
        if (confirmation) {
            type = DialogType.confirmation(toActionButton(yesButton), toActionButton(noButton));
        } else if (noticeButton != null) {
            type = DialogType.notice(toActionButton(noticeButton));
        } else {
            type = multiAction();
        }

        Dialog dialog = Dialog.create(factory -> factory.empty().base(base).type(type));
        return new BBDialog(dialog);
    }

    private MultiActionType multiAction() {
        MultiActionType.Builder type = DialogType.multiAction(buttons.stream().map(this::toActionButton).toList());
        // A dialog that never asked for a column count is built without one, so the client
        // keeps deciding — this API does not pin the default it happens to use today
        if (columns != DEFAULT_COLUMNS) {
            type.columns(columns);
        }
        return type.build();
    }

    private ActionButton toActionButton(DialogButton button) {
        ActionButton.Builder b = ActionButton.builder(button.label())
                .action(DialogAction.customClick(
                        (view, audience) -> runOnMainThread(button.onSubmit(), DialogResponse.from(view), audience),
                        ClickCallback.Options.builder().uses(1).lifetime(CALLBACK_LIFETIME).build()));
        if (button.tooltip() != null) {
            b.tooltip(button.tooltip());
        }
        if (button.width() != DialogButton.DEFAULT_WIDTH) {
            b.width(button.width());
        }
        return b.build();
    }

    /**
     * Runs a button callback on the server's main thread with the clicking user.
     * Server-side dialog callbacks may be delivered off the main thread, so BentoBox
     * / addon code (which is not thread-safe) is dispatched back to it.
     */
    private static void runOnMainThread(BiConsumer<User, DialogResponse> action, DialogResponse response,
            Audience audience) {
        if (action == null || !(audience instanceof Player player)) {
            return;
        }
        User user = User.getInstance(player);
        Bukkit.getScheduler().runTask(BentoBox.getInstance(), () -> action.accept(user, response));
    }

    private static DialogBody toBody(Object line) {
        if (line instanceof ItemLine(ItemStack item, Component description)) {
            var b = DialogBody.item(item);
            if (description != null) {
                b.description(DialogBody.plainMessage(description));
            }
            return b.build();
        }
        return DialogBody.plainMessage(((TextLine) line).text());
    }

    private static DialogInput toInput(DialogField field) {
        return switch (field) {
        case DialogField.Text t -> {
            TextDialogInput.Builder b = DialogInput.text(t.key(), t.label()).initial(t.initial())
                    .maxLength(t.maxLength()).labelVisible(t.labelVisible()).width(t.width());
            if (t.isMultiline()) {
                b.multiline(TextDialogInput.MultilineOptions.create(t.maxLines(), null));
            }
            yield b.build();
        }
        case DialogField.Number n -> {
            var b = DialogInput.numberRange(n.key(), n.label(), n.min(), n.max()).initial(n.initial())
                    .width(n.width());
            if (n.step() > 0) {
                b.step(n.step());
            }
            if (n.labelFormat() != null) {
                b.labelFormat(n.labelFormat());
            }
            yield b.build();
        }
        case DialogField.Toggle t -> DialogInput.bool(t.key(), t.label()).initial(t.initial()).build();
        case DialogField.Choice c -> DialogInput.singleOption(c.key(), c.label(), c.options().stream()
                .map(o -> SingleOptionDialogInput.OptionEntry.create(o.id(), o.display(), o.id().equals(c.initialId())))
                .toList()).labelVisible(c.labelVisible()).width(c.width()).build();
        };
    }
}
