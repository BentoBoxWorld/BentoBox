package world.bentobox.bentobox.api.dialogs;

/**
 * Utility helpers for the Dialogs API.
 *
 * @author tastybento
 * @since 3.21.0
 */
public final class Dialogs {

    private static final boolean SUPPORTED = classPresent("io.papermc.paper.dialog.Dialog");

    private Dialogs() {
        // Utility class
    }

    /**
     * Whether the running server supports Paper's dialog system. Dialogs were
     * added in Minecraft 26; on older servers this returns {@code false} and
     * callers should fall back to chat or panel presentation.
     *
     * @return true if dialogs can be shown
     */
    public static boolean isSupported() {
        return SUPPORTED;
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, Dialogs.class.getClassLoader());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
