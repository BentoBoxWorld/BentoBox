package world.bentobox.bentobox.api.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Applies the settings a game mode asks for to a copy of an addon's settings. Game modes ask
 * with {@link world.bentobox.bentobox.api.addons.GameModeAddon#getRequestedSettings(String)},
 * using the paths of the addon's config file as keys. The addon that owns the settings decides
 * whether to honor a request and uses the copy in that game mode's worlds only, so the admin's
 * own config is never changed.
 *
 * @author tastybento
 * @since 3.25.0
 */
public final class RequestedSettings {

    private RequestedSettings() {
    }

    /**
     * What came of a set of requests.
     * @param <T> the settings class
     * @param settings a copy of the settings with the accepted requests applied
     * @param applied the accepted requests, as "path: value", for telling the admin
     * @param refused the requests that could not be applied, with the reason
     */
    public record Result<T>(@NonNull T settings, @NonNull List<String> applied, @NonNull List<String> refused) {
    }

    /**
     * Copies settings and applies requests to the copy. A request is refused if no setting has
     * that path, the path is not one that may be requested, or the value is of the wrong type.
     * A request that matches the current value is accepted but not listed as applied.
     *
     * @param <T> the settings class, which must have a constructor with no arguments
     * @param settings the settings as the admin configured them. They are not changed.
     * @param requests config path to requested value
     * @param requestable says which config paths may be requested
     * @return the copy and what was applied and refused
     */
    @NonNull
    public static <T> Result<T> apply(@NonNull T settings, @NonNull Map<String, Object> requests,
            @NonNull Predicate<String> requestable) {
        List<String> applied = new ArrayList<>();
        List<String> refused = new ArrayList<>();
        T copy;
        try {
            copy = copy(settings);
        } catch (ReflectiveOperationException | RuntimeException e) {
            requests.keySet().forEach(k -> refused.add(k + " (settings cannot be copied: " + e + ")"));
            return new Result<>(settings, applied, refused);
        }
        requests.forEach((path, value) -> {
            Field field = requestable.test(path) ? find(copy.getClass(), path) : null;
            if (field == null) {
                refused.add(path + " (not a setting that can be requested)");
                return;
            }
            try {
                Object converted = convert(field.getType(), value);
                if (converted == null) {
                    refused.add(path + " (" + value + " is not a valid value)");
                } else if (!converted.equals(field.get(copy))) {
                    field.set(copy, converted); // NOSONAR the field is made accessible on purpose
                    applied.add(path + ": " + value);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                refused.add(path + " (" + e + ")");
            }
        });
        return new Result<>(copy, applied, refused);
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(T settings) throws ReflectiveOperationException {
        var constructor = settings.getClass().getDeclaredConstructor();
        constructor.setAccessible(true); // NOSONAR
        T copy = (T) constructor.newInstance();
        for (Class<?> c = settings.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
                    f.setAccessible(true); // NOSONAR
                    f.set(copy, f.get(settings)); // NOSONAR
                }
            }
        }
        return copy;
    }

    private static Field find(Class<?> clazz, String path) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                ConfigEntry entry = f.getAnnotation(ConfigEntry.class);
                if (entry != null && entry.path().equals(path) && !Modifier.isStatic(f.getModifiers())
                        && !Modifier.isFinal(f.getModifiers())) {
                    f.setAccessible(true); // NOSONAR
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Turns a requested value into the type of the setting. Only simple settings can be
     * requested: booleans, numbers, strings and enums.
     * @return the value, or null if it does not fit
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object convert(Class<?> type, Object value) {
        if (value == null) {
            return null;
        }
        if (type == boolean.class || type == Boolean.class) {
            return value instanceof Boolean ? value : null;
        }
        if (value instanceof Number n) {
            return convertNumber(type, n);
        }
        if (type == String.class) {
            return value.toString();
        }
        if (type.isEnum()) {
            return type.isInstance(value) ? value : convertEnum((Class<Enum>) type, value.toString());
        }
        return null;
    }

    private static Object convertNumber(Class<?> type, Number n) {
        if (type == int.class || type == Integer.class) {
            return n.intValue();
        } else if (type == long.class || type == Long.class) {
            return n.longValue();
        } else if (type == double.class || type == Double.class) {
            return n.doubleValue();
        } else if (type == float.class || type == Float.class) {
            return n.floatValue();
        } else if (type == String.class) {
            return n.toString();
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object convertEnum(Class<Enum> type, String name) {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException _) {
            return null;
        }
    }
}
