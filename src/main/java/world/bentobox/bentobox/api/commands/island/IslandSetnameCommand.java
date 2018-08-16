package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
public class IslandSetnameCommand extends CompositeCommand {

    public IslandSetnameCommand(CompositeCommand islandCommand) {
        super(islandCommand, "setname");
    }

    @Override
    public void setup() {
        setPermission("island.name");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.setname.parameters");
        setDescription("commands.island.setname.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        UUID playerUUID = user.getUniqueId();

        if (!getIslands().hasIsland(getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        if (!getIslands().isOwner(getWorld(), playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        // Explain command
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        // Naming the island - join all the arguments with spaces.
        String name = String.join(" ", args);

        // Check if the name isn't too short or too long
        if (name.length() < getSettings().getNameMinLength()) {
            user.sendMessage("commands.island.setname.too-short", TextVariables.NUMBER,  String.valueOf(getSettings().getNameMinLength()));
            return false;
        }
        if (name.length() > getSettings().getNameMaxLength()) {
            user.sendMessage("commands.island.setname.too-long", TextVariables.NUMBER, String.valueOf(getSettings().getNameMaxLength()));
            return false;
        }

        // Set the name
        if (user.isOp() || user.hasPermission(this.getPermissionPrefix() + ".island.name.format")) {
            getIslands().getIsland(getWorld(), playerUUID).setName(ChatColor.translateAlternateColorCodes('&', name));
        } else {
            getIslands().getIsland(getWorld(), playerUUID).setName(name);
        }

        user.sendMessage("general.success");
        return true;
    }

}
