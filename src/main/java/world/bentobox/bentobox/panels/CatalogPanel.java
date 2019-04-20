package world.bentobox.bentobox.panels;

import org.bukkit.Material;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * @since 1.5.0
 * @author Poslovitch
 */
public class CatalogPanel {

    private static final String LOCALE_REF = "catalog.panel.";
    private static final int[] PANES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};

    private CatalogPanel() {}

    public static void openPanel(@NonNull User user) {
        BentoBox plugin = BentoBox.getInstance();

        PanelBuilder builder = new PanelBuilder()
                .name(user.getTranslation(LOCALE_REF + "title"))
                .size(54);

        // Setup header and corners
        for (int i : PANES) {
            builder.item(i, new PanelItemBuilder().icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE).name(" ").build());
        }

        builder.build().open(user);
    }
}
