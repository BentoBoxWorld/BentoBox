package world.bentobox.bentobox.listeners.flags.clicklisteners;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

/**
 * 
 * @author tastybento
 *
 */
public class CommandRankClickListener implements ClickHandler {

    private final BentoBox plugin = BentoBox.getInstance();
    private Island island;

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.PanelItem.ClickHandler#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)
     */
    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // This click listener is used with TabbedPanel and SettingsTabs only
        TabbedPanel tp = (TabbedPanel)panel;
        SettingsTab st = (SettingsTab)tp.getActiveTab();
        // Get the island for this tab
        island = st.getIsland();

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

        // Check if user has rank enough on the island
        //Island island = plugin.getIslands().getIsland(panel.getWorld().orElse(user.getWorld()), user.getUniqueId());
        if (!island.isAllowed(user, Flags.CHANGE_SETTINGS)) {
            String rank = user.getTranslation(RanksManager.getInstance().getRank(Objects.requireNonNull(island).getRank(user)));
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, rank);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }


        String panelName = user.getTranslation("protection.flags.COMMAND_RANKS.name");
        if (panel.getName().equals(panelName)) {
            // This is a click on the panel
            // Slot relates to the command
            String c = getCommands(panel.getWorld().orElse(user.getWorld()), user).get(slot);
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
        getCommands(world, user).forEach(c -> pb.item(getPanelItem(c, user, world)));
        Panel p = pb.build();
        p.setIsland(island);
    }

    /**
     * Gets the rank command panel item
     * @param c - rank string
     * @param user - user
     * @param world - world for this panel
     * @return panel item for this command
     */
    public PanelItem getPanelItem(String c, User user, World world) {
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.name(user.getTranslation("protection.panel.flag-item.name-layout", TextVariables.NAME, c));
        pib.clickHandler(new CommandCycleClick(this, c));
        pib.icon(Material.MAP);
        String result = "";
        // Remove the first word (everything before the first space)
        String[] words = c.split(" ", 2); // Split into two parts, the first word and the rest
        if (words.length > 1) {
            result = words[1].replace(" ", "-"); // Replace spaces with hyphens
        }
        String ref = "protection.panel.flag-item.command-instructions." + result.toLowerCase(Locale.ENGLISH);
        String commandDescription = user.getTranslationOrNothing(ref);
        String d = user.getTranslation("protection.panel.flag-item.description-layout", TextVariables.DESCRIPTION,
                commandDescription);
        pib.description(d);
        RanksManager.getInstance().getRanks().forEach((reference, score) -> {
            if (score >= RanksManager.MEMBER_RANK && score < island.getRankCommand(c)) {
                pib.description(user.getTranslation("protection.panel.flag-item.blocked-rank") + user.getTranslation(reference));
            } else if (score <= RanksManager.OWNER_RANK && score > island.getRankCommand(c)) {
                pib.description(user.getTranslation("protection.panel.flag-item.allowed-rank") + user.getTranslation(reference));
            } else if (score == island.getRankCommand(c)) {
                pib.description(user.getTranslation("protection.panel.flag-item.minimal-rank") + user.getTranslation(reference));
            }
        });
        pib.invisible(plugin.getIWM().getHiddenFlags(world).contains(CommandCycleClick.COMMAND_RANK_PREFIX + c));
        return pib.build();
    }

    private List<String> getCommands(World world, User user) {
        List<String> hiddenItems = plugin.getIWM().getHiddenFlags(world);
        List<String> result = plugin.getCommandsManager().getCommands().values().stream()
                .filter(c -> c.getWorld() != null && c.getWorld().equals(world)) // Only allow commands in this world
                .filter(c -> c.testPermission(user.getSender())) // Only allow them to see commands they have permission to see
                .flatMap(c -> getCmdRecursively("/", c).stream())
                .filter(label -> user.isOp() || !hiddenItems.contains(CommandCycleClick.COMMAND_RANK_PREFIX + label)) // Hide any hidden commands
                .limit(49) // Silently limit to 49
                .toList();
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
