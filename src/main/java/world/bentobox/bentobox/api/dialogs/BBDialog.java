package world.bentobox.bentobox.api.dialogs;

import org.eclipse.jdt.annotation.NonNull;

import io.papermc.paper.dialog.Dialog;
import world.bentobox.bentobox.api.user.User;

/**
 * A built dialog, ready to be shown to a player. Create one with a
 * {@link DialogBuilder}.
 *
 * @author tastybento
 * @since 3.21.0
 */
public class BBDialog {

    private final Dialog dialog;

    BBDialog(@NonNull Dialog dialog) {
        this.dialog = dialog;
    }

    /**
     * Shows this dialog to the given user. Has no effect if the user is not an
     * online player.
     *
     * @param user the user to show the dialog to, not null
     */
    public void show(@NonNull User user) {
        if (user.isPlayer() && user.getPlayer() != null) {
            user.getPlayer().showDialog(dialog);
        }
    }

    /**
     * @return the underlying Paper dialog, for advanced use
     */
    @NonNull
    public Dialog getDialog() {
        return dialog;
    }
}
