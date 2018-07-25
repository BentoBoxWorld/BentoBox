package us.tastybento.bskyblock.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;

public class AdminClearResetCommand extends CompositeCommand {

    public AdminClearResetCommand(CompositeCommand parent) {
        super(parent, "clearreset");
    }

    @Override
    public void setup() {
        setPermission("admin.clearreset");
        setParameters("commands.admin.clearreset.parameters");
        setDescription("commands.admin.clearreset.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Get target
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Clear resets
        user.sendMessage("commands.admin.clearreset.cleared");
        getPlayers().setResets(getWorld(), user.getUniqueId(), 0);
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