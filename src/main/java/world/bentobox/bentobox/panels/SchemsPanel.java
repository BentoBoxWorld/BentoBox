package world.bentobox.bentobox.panels;

import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.island.IslandCreateCommand;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Creates schems panels
 * @author barpec12
 */
public class SchemsPanel {

    private static final String SCHEMS_PANEL = "schems.panel-";

    private SchemsPanel() {}

    /**
     * Dynamically creates the schems panel.
     * @param user - user to show panel to
     * @param world - world
     * @param islandCreateCommand - command to create an island
     */
    public static void openPanel(User user, World world, IslandCreateCommand islandCreateCommand) {
        PanelBuilder panelBuilder = new PanelBuilder()
                .name(user.getTranslation(SCHEMS_PANEL + "title"));

        for (String name : BentoBox.getInstance().getSchemsManager().get(world).keySet()) {
            PanelItemBuilder panelItemBuilder = new PanelItemBuilder();

            ItemStack item = new ItemStack(Material.GRASS_BLOCK); //TODO make this configurable
            panelItemBuilder.icon(item);

            panelItemBuilder.name(ChatColor.WHITE + fancySchemDisplayName(user, name)) //TODO make this configurable
                    .clickHandler((panel, u, click, slot) -> {
                        islandCreateCommand.execute(user, "create", Collections.singletonList(name));
                        return true;
                    });

            panelBuilder.item(panelItemBuilder.build());
        }

        panelBuilder.build().open(user);
    }

    /**
     * Returns a properly capitalized String of the schem name.
     * @param user - the User
     * @param schemName - name of the schem to get the display name from
     * @return properly capitalized String of the schem name
     */
    private static String fancySchemDisplayName(User user, String schemName) {
        // Set the first letter to an uppercase, to make it nice and fancy :D
        schemName = schemName.substring(0,1).toUpperCase() + schemName.substring(1);

        return schemName;
    }
}
