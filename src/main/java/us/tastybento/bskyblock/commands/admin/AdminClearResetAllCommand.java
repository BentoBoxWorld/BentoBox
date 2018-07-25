package us.tastybento.bskyblock.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;

public class AdminClearResetAllCommand extends CompositeCommand {

    public AdminClearResetAllCommand(CompositeCommand parent) {
        super(parent, "clearresetall");
    }

    @Override
    public void setup() {
        setPermission("admin.clearresetall");
        setParameters("commands.admin.clearreset.parameters");
        setDescription("commands.admin.clearreset.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (!args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        // Set the reset epoch to now
        getIWM().setResetEpoch(getWorld());
        // Reset all current players
        Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).filter(getPlayers()::isKnown).forEach(u -> getPlayers().setResets(getWorld(), u, 0));
        user.sendMessage("general.success");
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}