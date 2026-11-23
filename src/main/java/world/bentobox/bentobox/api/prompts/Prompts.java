package world.bentobox.bentobox.api.prompts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.dialogs.Dialogs;
import world.bentobox.bentobox.api.prompts.InputPrompt.Parsed;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Starting points for asking a player to type something. Each method returns a prompt to
 * configure and then {@link InputPrompt#open() open}.
 * <ul>
 * <li>{@link #text(User)} - one line of text</li>
 * <li>{@link #id(User)} - one line of text, turned into a string that is safe to use as a
 * unique id or file name</li>
 * <li>{@link #lines(User)} - several lines of text, e.g. a description</li>
 * <li>{@link #integer(User, int, int)} and {@link #decimal(User, double, double)} - a number in
 * a range</li>
 * <li>{@link #confirm(User)} - a yes/no confirmation</li>
 * </ul>
 *
 * @author tastybento
 * @since 3.25.0
 */
public final class Prompts {

    /** The most characters a {@link #text(User)} or {@link #id(User)} prompt accepts by default */
    public static final int DEFAULT_TEXT_LENGTH = 256;

    /** The most characters a {@link #lines(User)} prompt accepts by default, line breaks included */
    public static final int DEFAULT_LINES_LENGTH = 2048;

    private static final int NUMBER_LENGTH = 32;

    private static final String NOT_A_NUMBER = "prompts.errors.not-a-number";

    private Prompts() {
        // Utility class
    }

    /**
     * Asks for one line of text. Surrounding spaces are removed. An empty answer is refused
     * unless the prompt is made {@link InputPrompt#optional() optional}.
     *
     * @param user who to ask
     * @return the prompt
     */
    @NonNull
    public static InputPrompt<String> text(@NonNull User user) {
        return new InputPrompt<>(user, false, Parsed::ok, s -> s, DEFAULT_TEXT_LENGTH);
    }

    /**
     * Asks for one line of text and hands back {@link Util#sanitizeInput(String) a sanitized}
     * version of it: lower case, with colors removed and spaces and characters that are not
     * allowed in file names replaced by underscores.
     *
     * @param user who to ask
     * @return the prompt
     */
    @NonNull
    public static InputPrompt<String> id(@NonNull User user) {
        return new InputPrompt<>(user, false, s -> Parsed.ok(Util.sanitizeInput(s)), s -> s, DEFAULT_TEXT_LENGTH);
    }

    /**
     * Asks for several lines of text. In a dialog this is one multi-line box; in chat the
     * player sends one line at a time and then a word to finish. Blank lines at the end are
     * dropped, and an empty answer gives an empty list.
     *
     * @param user who to ask
     * @return the prompt
     */
    @NonNull
    public static InputPrompt<List<String>> lines(@NonNull User user) {
        return new InputPrompt<>(user, true, s -> Parsed.ok(toLines(s)), l -> String.join("\n", l),
                DEFAULT_LINES_LENGTH).optional();
    }

    /**
     * Asks for a whole number.
     *
     * @param user who to ask
     * @param min  the lowest number accepted
     * @param max  the highest number accepted
     * @return the prompt
     */
    @NonNull
    public static InputPrompt<Integer> integer(@NonNull User user, int min, int max) {
        return new InputPrompt<>(user, false, s -> {
            long value;
            try {
                value = Long.parseLong(s.replace("_", "").replace(",", ""));
            } catch (NumberFormatException _) {
                return Parsed.error(user.getTranslation(NOT_A_NUMBER));
            }
            if (value < min || value > max) {
                return Parsed.error(outOfRange(user, String.valueOf(min), String.valueOf(max)));
            }
            return Parsed.ok((int) value);
        }, String::valueOf, NUMBER_LENGTH);
    }

    /**
     * Asks for a number that may have a fractional part.
     *
     * @param user who to ask
     * @param min  the lowest number accepted
     * @param max  the highest number accepted
     * @return the prompt
     */
    @NonNull
    public static InputPrompt<Double> decimal(@NonNull User user, double min, double max) {
        return new InputPrompt<>(user, false, s -> {
            double value;
            try {
                value = Double.parseDouble(s.replace(',', '.'));
            } catch (NumberFormatException _) {
                return Parsed.error(user.getTranslation(NOT_A_NUMBER));
            }
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return Parsed.error(user.getTranslation(NOT_A_NUMBER));
            }
            if (value < min || value > max) {
                return Parsed.error(outOfRange(user, String.valueOf(min), String.valueOf(max)));
            }
            return Parsed.ok(value);
        }, String::valueOf, NUMBER_LENGTH);
    }

    /**
     * Asks the player to confirm something.
     *
     * @param user who to ask
     * @return the prompt
     */
    @NonNull
    public static ConfirmationPrompt confirm(@NonNull User user) {
        return new ConfirmationPrompt(user);
    }

    /**
     * @param user the user
     * @return true if a prompt for this user is shown as a dialog, false if it is asked in chat
     */
    public static boolean usesDialog(@NonNull User user) {
        return user.isPlayer() && Dialogs.isSupported() && BentoBox.getInstance().getSettings().isDialogPrompts();
    }

    private static String outOfRange(User user, String min, String max) {
        return user.getTranslation("prompts.errors.out-of-range", "[min]", min, "[max]", max);
    }

    private static List<String> toLines(String text) {
        List<String> lines = new ArrayList<>(List.of(text.split("\\R", -1)));
        while (!lines.isEmpty() && lines.getLast().isBlank()) {
            lines.removeLast();
        }
        return lines;
    }
}
