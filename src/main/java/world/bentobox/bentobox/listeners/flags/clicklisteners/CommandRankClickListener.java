package world.bentobox.bentobox.listeners.flags.clicklisteners;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class CommandRankClickListener implements ClickHandler {

    private final BentoBox plugin = BentoBox.getInstance();

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

        // Check if has permission
        World w = Objects.requireNonNull(Util.getWorld(panel.getWorld().orElse(user.getWorld())));
        String prefix = plugin.getIWM().getPermissionPrefix(w);
        String reqPerm = prefix + "settings." + Flags.COMMAND_RANKS.getID();
        String allPerms = prefix + "settings.*";
        if (!user.hasPermission(reqPerm) && !user.hasPermission(allPerms)
                && !user.isOp() && !user.hasPermission(prefix + "admin.settings")) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }

        // Get the user's island
        Island island = plugin.getIslands().getIsland(panel.getWorld().orElse(user.getWorld()), user.getUniqueId());
        if (island == null || !island.getOwner().equals(user.getUniqueId())) {
            user.sendMessage("general.errors.not-owner");
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }

        String panelName = user.getTranslation("protection.flags.COMMAND_RANKS.name");
        if (panel.getName().equals(panelName)) {
            // This is a click on the panel
            // Slot relates to the command
            String c = getCommands(panel.getWorld().orElse(user.getWorld())).get(slot);
            // Apply change to panel
            panel.getInventory().setItem(slot, getPanelItem(c, user, panel.getWorld().orElse(user.getWorld())).getItem());
        } else {
            // Open the Sub Settings panel
            openPanel(user, panelName, panel.getWorld().orElse(user.getWorld()));
        }
        return true;
    }

    private void openPanel(User user, String panelName, World world) {
        // Close the current panel
        user.closeInventory();
        // Open a new panel
        PanelBuilder pb = new PanelBuilder();
        pb.user(user).name(panelName).world(world);
        // Make panel items
        getCommands(world).forEach(c -> pb.item(getPanelItem(c, user, world)));
        pb.build();

    }

    /**
     * Gets the rank command panel item
     * @param c - rank string
     * @param user - user
     * @param world - world for this panel
     * @return panel item for this command
     */
    public PanelItem getPanelItem(String c, User user, World world) {
        Island island = plugin.getIslands().getIsland(world, user);
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.name(c);
        pib.clickHandler(new CommandCycleClick(this, c));
        pib.icon(Material.MAP);
        // TODO: use specific layout
        String d = user.getTranslation("protection.panel.flag-item.description-layout", TextVariables.DESCRIPTION, "");
        pib.description(d);
        plugin.getRanksManager().getRanks().forEach((reference, score) -> {
            if (score >= RanksManager.MEMBER_RANK && score < island.getRankCommand(c)) {
                pib.description(user.getTranslation("protection.panel.flag-item.blocked-rank") + user.getTranslation(reference));
            } else if (score <= RanksManager.OWNER_RANK && score > island.getRankCommand(c)) {
                pib.description(user.getTranslation("protection.panel.flag-item.allowed-rank") + user.getTranslation(reference));
            } else if (score == island.getRankCommand(c)) {
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
