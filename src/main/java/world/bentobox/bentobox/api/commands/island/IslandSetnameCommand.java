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
        // Explain command
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        UUID playerUUID = user.getUniqueId();

        if (!getIslands().hasIsland(getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().isOwner(getWorld(), playerUUID)) {
            user.sendMessage("general.errors.not-owner");
            return false;
        }

        // Naming the island - join all the arguments with spaces.
        String name = String.join(" ", args);

        // Check if the name isn't too short or too long
        if (name.length() < getSettings().getNameMinLength() || ChatColor.stripColor(name).isEmpty()) {
            user.sendMessage("commands.island.setname.name-too-short", TextVariables.NUMBER, String.valueOf(getSettings().getNameMinLength()));
            return false;
        }
        if (name.length() > getSettings().getNameMaxLength()) {
            user.sendMessage("commands.island.setname.name-too-long", TextVariables.NUMBER, String.valueOf(getSettings().getNameMaxLength()));
            return false;
        }

        // Apply colors
        if (user.hasPermission(getPermissionPrefix() + "island.name.format")) {
            name = ChatColor.translateAlternateColorCodes('&', name);
        }

        // Check if the name doesn't already exist in the gamemode
        if (getSettings().isNameUniqueness() && getIslands().nameExists(getWorld(), name)) {
            user.sendMessage("commands.island.setname.name-already-exists");
            return false;
        }

        // Everything's good!
        getIslands().getIsland(getWorld(), playerUUID).setName(name);
        user.sendMessage("commands.island.setname.success", TextVariables.NAME, name);
        return true;
    }
}
