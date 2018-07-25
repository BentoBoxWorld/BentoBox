package us.tastybento.bskyblock.commands.admin;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

public class AdminClearResetsAllCommand extends CompositeCommand {

    public AdminClearResetsAllCommand(CompositeCommand parent) {
        super(parent, "clearresetsall");
    }

    @Override
    public void setup() {
        setPermission("admin.clearresetsall");
        setDescription("commands.admin.clearresetsall.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (!args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        this.askConfirmation(user, () -> {
            // Set the reset epoch to now
            getIWM().setResetEpoch(getWorld());
            // Reset all current players
            Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).filter(getPlayers()::isKnown).forEach(u -> getPlayers().setResets(getWorld(), u, 0));
            user.sendMessage("general.success");
        });
        return false;
    }

}