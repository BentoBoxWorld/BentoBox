package world.bentobox.bentobox.api.dialogs;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * An input field in a {@link DialogBuilder dialog}: a text box, a number slider, a
 * toggle, or a choice between options. Add fields with {@link DialogBuilder#input(DialogField)}
 * and read what the player entered from the {@link DialogResponse} handed to a
 * {@link DialogButton#submit(Component, java.util.function.BiConsumer) submit button}.
 * <p>
 * Each field has a {@link #key()} that identifies its value in the response. Keys may
 * only contain letters, digits and underscores, because the client uses them as
 * template names, and they must be unique within one dialog.
 * <p>
 * Fields are plain immutable values with no Paper types in them, so building and
 * checking them works anywhere, including unit tests. They become Paper dialog inputs
 * only when the dialog is {@link DialogBuilder#build() built}.
 * <p>
 * Example - a small form:
 * <pre>{@code
 * new DialogBuilder()
 *     .title(user, "myaddon.edit.title")
 *     .input(DialogField.text(user, "name", "myaddon.edit.name", current.getName()).maxLength(32))
 *     .input(DialogField.number(user, "chance", "myaddon.edit.chance", 0, 100, current.getChance()).step(0.5f))
 *     .input(DialogField.toggle(user, "enabled", "myaddon.edit.enabled", current.isEnabled()))
 *     .button(DialogButton.submit(user, "general.buttons.save", (u, response) -> {
 *         current.setName(response.getText("name").orElse(""));
 *         current.setChance(response.getNumber("chance").orElse(0f));
 *         current.setEnabled(response.getBoolean("enabled").orElse(false));
 *     }))
 *     .build()
 *     .show(user);
 * }</pre>
 *
 * @author tastybento
 * @since 3.24.0
 */
public sealed interface DialogField permits DialogField.Text, DialogField.Number, DialogField.Toggle, DialogField.Choice {

    /** What a field key may contain */
    Pattern KEY_PATTERN = Pattern.compile("\\w+");

    /** Widest input the client accepts */
    int MAX_WIDTH = 1024;

    /** Width of an input that has not asked for one, matching the client's default */
    int DEFAULT_WIDTH = 200;

    /**
     * @return the key that identifies this field's value in the {@link DialogResponse}
     */
    @NonNull
    String key();

    /**
     * @return the label shown with the field
     */
    @NonNull
    Component label();

    /**
     * @return the width of the field, 1 to {@link #MAX_WIDTH}
     */
    int width();

    // ------------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------------

    /**
     * A single-line text box.
     *
     * @param key     the field key, letters, digits and underscores only
     * @param label   the label shown with the box
     * @param initial the text the box starts with, or null for empty
     * @return a text field with a 32 character limit; change it with {@link Text#maxLength(int)}
     */
    @NonNull
    static Text text(@NonNull String key, @NonNull Component label, @Nullable String initial) {
        return new Text(key, label, initial == null ? "" : initial, Text.DEFAULT_MAX_LENGTH, 0, true, DEFAULT_WIDTH);
    }

    /**
     * A single-line text box whose label is a localized translation for the given user.
     *
     * @param user      the user whose locale is used
     * @param key       the field key, letters, digits and underscores only
     * @param reference the locale key of the label
     * @param initial   the text the box starts with, or null for empty
     * @return a text field
     */
    @NonNull
    static Text text(@NonNull User user, @NonNull String key, @NonNull String reference, @Nullable String initial) {
        return text(key, translate(user, reference), initial);
    }

    /**
     * A number slider.
     *
     * @param key     the field key, letters, digits and underscores only
     * @param label   the label shown with the slider
     * @param min     the lowest value
     * @param max     the highest value
     * @param initial the starting value; clamped to {@code min..max}
     * @return a number field with no step; set one with {@link Number#step(float)}
     */
    @NonNull
    static Number number(@NonNull String key, @NonNull Component label, float min, float max, float initial) {
        return new Number(key, label, min, max, Math.clamp(initial, Math.min(min, max), Math.max(min, max)), 0f, null,
                DEFAULT_WIDTH);
    }

    /**
     * A number slider whose label is a localized translation for the given user.
     *
     * @param user      the user whose locale is used
     * @param key       the field key, letters, digits and underscores only
     * @param reference the locale key of the label
     * @param min       the lowest value
     * @param max       the highest value
     * @param initial   the starting value; clamped to {@code min..max}
     * @return a number field
     */
    @NonNull
    static Number number(@NonNull User user, @NonNull String key, @NonNull String reference, float min, float max,
            float initial) {
        return number(key, translate(user, reference), min, max, initial);
    }

    /**
     * An on/off toggle.
     *
     * @param key     the field key, letters, digits and underscores only
     * @param label   the label shown with the toggle
     * @param initial whether it starts on
     * @return a toggle field
     */
    @NonNull
    static Toggle toggle(@NonNull String key, @NonNull Component label, boolean initial) {
        return new Toggle(key, label, initial);
    }

    /**
     * An on/off toggle whose label is a localized translation for the given user.
     *
     * @param user      the user whose locale is used
     * @param key       the field key, letters, digits and underscores only
     * @param reference the locale key of the label
     * @param initial   whether it starts on
     * @return a toggle field
     */
    @NonNull
    static Toggle toggle(@NonNull User user, @NonNull String key, @NonNull String reference, boolean initial) {
        return toggle(key, translate(user, reference), initial);
    }

    /**
     * A choice of one option from a list. The client shows it as a button that cycles
     * through the options, so it suits short lists; for long ones use a panel.
     *
     * @param key       the field key, letters, digits and underscores only
     * @param label     the label shown with the choice
     * @param options   the options, at least one, with unique ids
     * @param initialId the id of the option selected at the start, or null for the first
     * @return a choice field
     */
    @NonNull
    static Choice choice(@NonNull String key, @NonNull Component label, @NonNull List<Choice.Option> options,
            @Nullable String initialId) {
        String initial = initialId;
        if (initial == null) {
            // An empty list is rejected by the constructor, so "" never reaches a dialog
            initial = options.isEmpty() ? "" : options.getFirst().id();
        }
        return new Choice(key, label, options, initial, true, DEFAULT_WIDTH);
    }

    /**
     * A choice whose label is a localized translation for the given user.
     *
     * @param user      the user whose locale is used
     * @param key       the field key, letters, digits and underscores only
     * @param reference the locale key of the label
     * @param options   the options, at least one, with unique ids
     * @param initialId the id of the option selected at the start, or null for the first
     * @return a choice field
     */
    @NonNull
    static Choice choice(@NonNull User user, @NonNull String key, @NonNull String reference,
            @NonNull List<Choice.Option> options, @Nullable String initialId) {
        return choice(key, translate(user, reference), options, initialId);
    }

    private static Component translate(User user, String reference) {
        return Util.parseMiniMessageOrLegacy(user.getTranslation(reference));
    }

    private static void checkCommon(String key, Component label, int width) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "A dialog field key may only contain letters, digits and underscores, not '" + key + "'");
        }
        if (width < 1 || width > MAX_WIDTH) {
            throw new IllegalArgumentException("Field width must be between 1 and " + MAX_WIDTH + ", not " + width);
        }
    }

    // ------------------------------------------------------------------------
    // Field types
    // ------------------------------------------------------------------------

    /**
     * A text box.
     *
     * @param key          the field key
     * @param label        the label
     * @param initial      the starting text, never null
     * @param maxLength    the most characters the player may type, at least 1
     * @param maxLines     for a multi-line box, the most lines it holds; 0 for a single line
     * @param labelVisible whether the label is shown
     * @param width        the width, 1 to {@link DialogField#MAX_WIDTH}
     */
    record Text(@NonNull String key, @NonNull Component label, @NonNull String initial, int maxLength, int maxLines,
            boolean labelVisible, int width) implements DialogField {

        /** Character limit of a text field that has not asked for one, matching the client's default */
        public static final int DEFAULT_MAX_LENGTH = 32;

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if the key, width, length or lines are out of range,
         *                                  or the initial text is longer than the limit
         */
        public Text {
            checkCommon(key, label, width);
            Objects.requireNonNull(initial, "initial");
            if (maxLength < 1) {
                throw new IllegalArgumentException("A text field needs a max length of at least 1, not " + maxLength);
            }
            if (maxLines < 0) {
                throw new IllegalArgumentException("Max lines cannot be negative: " + maxLines);
            }
            if (initial.length() > maxLength) {
                throw new IllegalArgumentException("Initial text is " + initial.length()
                        + " characters, longer than the field's max length of " + maxLength);
            }
        }

        /**
         * @param maxLength the most characters the player may type, at least 1
         * @return a copy with this limit
         */
        @NonNull
        public Text maxLength(int maxLength) {
            return new Text(key, label, initial, maxLength, maxLines, labelVisible, width);
        }

        /**
         * Makes this a multi-line text box.
         *
         * @param maxLines the most lines it holds, at least 1
         * @return a copy that is multi-line
         */
        @NonNull
        public Text multiline(int maxLines) {
            if (maxLines < 1) {
                throw new IllegalArgumentException("A multi-line field needs at least 1 line, not " + maxLines);
            }
            return new Text(key, label, initial, maxLength, maxLines, labelVisible, width);
        }

        /**
         * @param labelVisible whether the label is shown
         * @return a copy with the label shown or hidden
         */
        @NonNull
        public Text labelVisible(boolean labelVisible) {
            return new Text(key, label, initial, maxLength, maxLines, labelVisible, width);
        }

        /**
         * @param width the width, 1 to {@link DialogField#MAX_WIDTH}
         * @return a copy at this width
         */
        @NonNull
        public Text width(int width) {
            return new Text(key, label, initial, maxLength, maxLines, labelVisible, width);
        }

        /**
         * @return true if this is a multi-line box
         */
        public boolean isMultiline() {
            return maxLines > 0;
        }
    }

    /**
     * A number slider.
     *
     * @param key         the field key
     * @param label       the label
     * @param min         the lowest value
     * @param max         the highest value
     * @param initial     the starting value
     * @param step        the gap between values, or 0 for a smooth slider
     * @param labelFormat a translation key for the label, with {@code %1$s} for the label and
     *                    {@code %2$s} for the value, or null for the client's default
     *                    ("label: value")
     * @param width       the width, 1 to {@link DialogField#MAX_WIDTH}
     */
    record Number(@NonNull String key, @NonNull Component label, float min, float max, float initial, float step,
            @Nullable String labelFormat, int width) implements DialogField {

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if the key or width is out of range, the step is
         *                                  negative, or the initial value is outside the range
         */
        public Number {
            checkCommon(key, label, width);
            if (step < 0) {
                throw new IllegalArgumentException("Step cannot be negative: " + step);
            }
            if (initial < Math.min(min, max) || initial > Math.max(min, max)) {
                throw new IllegalArgumentException("Initial value " + initial + " is outside " + min + ".." + max);
            }
        }

        /**
         * @param step the gap between values, e.g. 1 for whole numbers, or 0 for a smooth slider
         * @return a copy with this step
         */
        @NonNull
        public Number step(float step) {
            return new Number(key, label, min, max, initial, step, labelFormat, width);
        }

        /**
         * @param labelFormat a translation key with {@code %1$s} for the label and {@code %2$s}
         *                    for the value, or null for the client's default
         * @return a copy with this label format
         */
        @NonNull
        public Number labelFormat(@Nullable String labelFormat) {
            return new Number(key, label, min, max, initial, step, labelFormat, width);
        }

        /**
         * @param width the width, 1 to {@link DialogField#MAX_WIDTH}
         * @return a copy at this width
         */
        @NonNull
        public Number width(int width) {
            return new Number(key, label, min, max, initial, step, labelFormat, width);
        }
    }

    /**
     * An on/off toggle. The client always shows it as a checkbox, so it has no width of
     * its own; {@link #width()} reports {@link DialogField#DEFAULT_WIDTH}.
     *
     * @param key     the field key
     * @param label   the label
     * @param initial whether it starts on
     */
    record Toggle(@NonNull String key, @NonNull Component label, boolean initial) implements DialogField {

        /**
         * Checks the key.
         *
         * @throws IllegalArgumentException if the key is not valid
         */
        public Toggle {
            checkCommon(key, label, DEFAULT_WIDTH);
        }

        @Override
        public int width() {
            return DEFAULT_WIDTH;
        }
    }

    /**
     * A choice of one option from a list.
     *
     * @param key          the field key
     * @param label        the label
     * @param options      the options, at least one, with unique ids
     * @param initialId    the id of the option selected at the start
     * @param labelVisible whether the label is shown
     * @param width        the width, 1 to {@link DialogField#MAX_WIDTH}
     */
    record Choice(@NonNull String key, @NonNull Component label, @NonNull List<Option> options,
            @NonNull String initialId, boolean labelVisible, int width) implements DialogField {

        /**
         * Checks the values and takes an immutable copy of the options.
         *
         * @throws IllegalArgumentException if the key or width is out of range, there are no
         *                                  options, two share an id, or the initial id is not one of them
         */
        public Choice {
            checkCommon(key, label, width);
            options = List.copyOf(options);
            if (options.isEmpty()) {
                throw new IllegalArgumentException("A choice needs at least one option");
            }
            if (options.stream().map(Option::id).distinct().count() != options.size()) {
                throw new IllegalArgumentException("Choice option ids must be unique");
            }
            final String initial = initialId;
            if (options.stream().noneMatch(o -> o.id().equals(initial))) {
                throw new IllegalArgumentException("Initial option '" + initialId + "' is not one of the options");
            }
        }

        /**
         * @param labelVisible whether the label is shown
         * @return a copy with the label shown or hidden
         */
        @NonNull
        public Choice labelVisible(boolean labelVisible) {
            return new Choice(key, label, options, initialId, labelVisible, width);
        }

        /**
         * @param width the width, 1 to {@link DialogField#MAX_WIDTH}
         * @return a copy at this width
         */
        @NonNull
        public Choice width(int width) {
            return new Choice(key, label, options, initialId, labelVisible, width);
        }

        /**
         * One option in a {@link Choice}.
         *
         * @param id      the value the response reports when this option is chosen
         * @param display what the player sees
         */
        public record Option(@NonNull String id, @NonNull Component display) {

            /**
             * Checks the values.
             */
            public Option {
                Objects.requireNonNull(id, "id");
                Objects.requireNonNull(display, "display");
            }

            /**
             * An option whose display text is a localized translation for the given user.
             *
             * @param user      the user whose locale is used
             * @param id        the value the response reports for this option
             * @param reference the locale key of the display text
             * @return the option
             */
            @NonNull
            public static Option of(@NonNull User user, @NonNull String id, @NonNull String reference) {
                return new Option(id, translate(user, reference));
            }

            /**
             * An option that shows its id as plain text.
             *
             * @param id the value, also shown to the player
             * @return the option
             */
            @NonNull
            public static Option of(@NonNull String id) {
                return new Option(id, Component.text(id));
            }
        }
    }
}
