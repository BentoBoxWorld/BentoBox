package us.tastybento.bskyblock.panels;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.ClickType;
import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.util.Util;

public class LanguagePanel {

    public static void openPanel(Player player) {
        PanelBuilder panelBuilder = new PanelBuilder()
                .setName(Util.getMessage(player, "panel.languages.title"));

        PanelItem test = new PanelItemBuilder()
                .setIcon(new ItemStack(Material.BEDROCK))
                .setName(Util.getMessage(player, "panel.languages.test.name"))
                .setDescription(Util.getMessage(player, "panel.languages.test.description"))
                .setGlow(true)
                .setClickHandler(new PanelItem.ClickHandler() {
                    @Override
                    public boolean onClick(Player player, ClickType click) {
                        player.sendMessage("Hi!");
                        return false;
                    }
                })
                .build();

        PanelItem something = new PanelItemBuilder()
                .setIcon(new ItemStack(Material.ANVIL))
                .setName(Util.getMessage(player, "panel.languages.something.name"))
                .setDescription(Util.getMessage(player, "panel.languages.something.description"))
                .build();

        panelBuilder.addItem(1, test);
        panelBuilder.addItem(3, something);

        Panel panel = panelBuilder.build();

        panel.open(player);
    }
}
