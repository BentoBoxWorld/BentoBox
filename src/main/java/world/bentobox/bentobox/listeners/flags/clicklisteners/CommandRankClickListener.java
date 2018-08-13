/**
 * 
 */
package world.bentobox.bentobox.listeners.flags.clicklisteners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class CommandRankClickListener implements ClickHandler {
    
    private BentoBox plugin = BentoBox.getInstance();

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.PanelItem.ClickHandler#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)
     */
    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!user.inWorld()) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        IslandWorldManager iwm = plugin.getIWM();
        String reqPerm = iwm.getPermissionPrefix(Util.getWorld(user.getWorld())) + ".admin.settings.COMMAND_RANKS";
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", "[permission]", reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }

        String panelName = user.getTranslation("protection.flags.COMMAND_RANKS.name");
        if (panel.getName().equals(panelName)) {
            // This is a click on the panel
            // Slot relates to the command
            String c = getCommands(user.getWorld()).get(slot);
            // Apply change to panel
            panel.getInventory().setItem(slot, getPanelItem(c, user).getItem());
        } else {
            // Open the Sub Settings panel
            openPanel(user, panelName);
        }
        return true;
    }

    private void openPanel(User user, String panelName) {
        // Close the current panel
        user.closeInventory();
        // Open a new panel
        PanelBuilder pb = new PanelBuilder();
        pb.user(user).name(panelName);
        // Make panel items
        getCommands(user.getWorld()).forEach(c -> pb.item(getPanelItem(c, user)));
        pb.build();

    }

    /**
     * Gets the rank command panel item
     * @param c - rank string
     * @param user - user
     * @return panel item for this command
     */
    public PanelItem getPanelItem(String c, User user) {
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.name(c);
        pib.clickHandler(new CommandCycleClick(this, c));
        pib.icon(Material.MAP);
        // TODO: use specific layout
        String d = user.getTranslation("protection.panel.flag-item.description-layout", TextVariables.DESCRIPTION, "");
        pib.description(d);
        plugin.getRanksManager().getRanks().forEach((reference, score) -> {
            if (score >= RanksManager.MEMBER_RANK && score < plugin.getSettings().getRankCommand(c)) {
                pib.description(user.getTranslation("protection.panel.flag-item.blocked-rank") + user.getTranslation(reference));
            } else if (score <= RanksManager.OWNER_RANK && score > plugin.getSettings().getRankCommand(c)) {
                pib.description(user.getTranslation("protection.panel.flag-item.allowed-rank") + user.getTranslation(reference));
            } else if (score == plugin.getSettings().getRankCommand(c)) {
                pib.description(user.getTranslation("protection.panel.flag-item.minimal-rank") + user.getTranslation(reference));
            }
        });
        return pib.build();
    }

    private List<String> getCommands(World world) {
        List<String> result = new ArrayList<>();
        plugin.getCommandsManager().getCommands().values().stream()
        .filter(c -> c.getWorld() != null &&  c.getWorld().equals(world))
        .forEach(c -> result.addAll(getCmdRecursively("/", c)));        
        if (result.size() > 49) {
            Bukkit.getLogger().severe("Number of rank setting commands is too big for GUI");
            result.subList(49, result.size()).clear();
        }
        return result;
    }

    /**
     * Recursively traverses the command tree looking for any configurable rank command and returns a string list of commands
     * @param labels - preceding command's label list
     * @param cc - composite command
     * @return string list of commands
     */
    private List<String> getCmdRecursively(String labels, CompositeCommand cc) {
        List<String> result = new ArrayList<>();
        String newLabel = labels + cc.getName();
        if (cc.isConfigurableRankCommand()) {
            result.add(newLabel);
        }
        cc.getSubCommands().values().forEach(s -> result.addAll(getCmdRecursively(newLabel + " ", s)));     
        return result;
    }

}
