package world.bentobox.bentobox.panels;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.user.User;

/**
 * @since 1.5.0
 * @author Poslovitch
 */
public class CatalogPanel {

    public static void openPanel(@NonNull User user) {
        user.sendRawMessage("open the catalog (it's coming soon!)");
    }
}
