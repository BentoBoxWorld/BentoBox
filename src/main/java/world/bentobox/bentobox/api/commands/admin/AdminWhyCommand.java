package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

public class AdminWhyCommand extends ConfirmableCommand {

    public AdminWhyCommand(CompositeCommand parent) {
        super(parent, "why");
    }

    @Override
    public void setup() {
        setPermission("admin.why");
        setParametersHelp("commands.admin.why.parameters");
        setDescription("commands.admin.why.description");
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
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Set meta data on player
        User target = User.getInstance(targetUUID);
        if (!target.isOnline()) {
            user.sendMessage("general.errors.offline-player");
            return false;
        }
        // Determine the debug mode and toggle if required
        boolean newValue = !target.getPlayer().getMetadata(getWorld().getName() + "_why_debug").stream()
                .filter(p -> p.getOwningPlugin().equals(getPlugin())).findFirst().map(MetadataValue::asBoolean).orElse(false);
        if (newValue) {
            user.sendMessage("commands.admin.why.turning-on", TextVariables.NAME, target.getName());
        } else {
            user.sendMessage("commands.admin.why.turning-off", TextVariables.NAME, target.getName());
        }
        // Set the debug meta
        target.getPlayer().setMetadata(getWorld().getName() + "_why_debug", new FixedMetadataValue(getPlugin(), newValue));
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