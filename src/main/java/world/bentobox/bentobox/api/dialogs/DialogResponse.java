package world.bentobox.bentobox.api.dialogs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;

import io.papermc.paper.dialog.DialogResponseView;

/**
 * What a player entered in a dialog's {@link DialogField fields}, handed to a
 * {@link DialogButton#submit(net.kyori.adventure.text.Component, java.util.function.BiConsumer)
 * submit button} when it is clicked.
 * <p>
 * Values are looked up by field key. A key with no value (no such field, or a value of
 * the wrong type) gives an empty {@link Optional}, so callers decide the fallback.
 * <ul>
 * <li>{@link DialogField.Text} - {@link #getText(String)}</li>
 * <li>{@link DialogField.Number} - {@link #getNumber(String)}, or {@link #getInt(String)} for
 * whole numbers</li>
 * <li>{@link DialogField.Toggle} - {@link #getBoolean(String)}</li>
 * <li>{@link DialogField.Choice} - {@link #getChoice(String)}, the chosen option's id</li>
 * </ul>
 * Use {@link #of(Map)} to make one in tests.
 *
 * @author tastybento
 * @since 3.24.0
 */
public final class DialogResponse {

    /** A response with no values, for buttons on dialogs that have no fields */
    public static final DialogResponse EMPTY = new DialogResponse(key -> null);

    private final Function<String, Object> lookup;

    private DialogResponse(Function<String, Object> lookup) {
        this.lookup = lookup;
    }

    /**
     * Wraps the response Paper hands to a click callback.
     *
     * @param view the Paper response, or null for none
     * @return the response
     */
    static DialogResponse from(DialogResponseView view) {
        if (view == null) {
            return EMPTY;
        }
        // Paper reads each type separately and returns null if the key holds another type.
        // Text first: choice ids are delivered as text.
        return new DialogResponse(key -> {
            String text = view.getText(key);
            if (text != null) {
                return text;
            }
            Float number = view.getFloat(key);
            if (number != null) {
                return number;
            }
            return view.getBoolean(key);
        });
    }

    /**
     * Makes a response from a map of field keys to values, for tests or for code that
     * collects values some other way. Values should be {@link String}, a {@link java.lang.Number}
     * or {@link Boolean}.
     *
     * @param values the values by field key
     * @return the response
     */
    @NonNull
    public static DialogResponse of(@NonNull Map<String, ?> values) {
        Map<String, Object> copy = new HashMap<>(values);
        return new DialogResponse(copy::get);
    }

    /**
     * @param key the field key
     * @return the text entered in a text field
     */
    @NonNull
    public Optional<String> getText(@NonNull String key) {
        return lookup.apply(key) instanceof String s ? Optional.of(s) : Optional.empty();
    }

    /**
     * @param key the field key
     * @return the id of the option chosen in a choice field
     */
    @NonNull
    public Optional<String> getChoice(@NonNull String key) {
        return getText(key);
    }

    /**
     * @param key the field key
     * @return the value of a number field
     */
    @NonNull
    public Optional<Float> getNumber(@NonNull String key) {
        return lookup.apply(key) instanceof java.lang.Number n ? Optional.of(n.floatValue()) : Optional.empty();
    }

    /**
     * @param key the field key
     * @return the value of a number field, rounded to the nearest whole number
     */
    @NonNull
    public Optional<Integer> getInt(@NonNull String key) {
        return getNumber(key).map(Math::round);
    }

    /**
     * @param key the field key
     * @return whether a toggle field is on
     */
    @NonNull
    public Optional<Boolean> getBoolean(@NonNull String key) {
        return lookup.apply(key) instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }
}
