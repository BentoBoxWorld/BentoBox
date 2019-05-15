package world.bentobox.bentobox.panels;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento, Poslovitch
 * @since 1.5.0
 */
public class BlueprintManagementPanel {

    private static final String LOCALE_REF = "blueprint-management";

    private BlueprintManagementPanel() {}

    public static void openPanel(@NonNull User user, @NonNull GameModeAddon addon) {
        PanelBuilder builder = new PanelBuilder()
                .name(user.getTranslation(LOCALE_REF + "title"))
                .size(54);



        builder.build().open(user);
    }
}
