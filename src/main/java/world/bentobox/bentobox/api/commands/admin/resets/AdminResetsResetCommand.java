package world.bentobox.bentobox.api.commands.admin.resets;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

public class AdminResetsResetCommand extends ConfirmableCommand {

    public AdminResetsResetCommand(CompositeCommand parent) {
        super(parent, "reset");
    }

    @Override
    public void setup() {
        inheritPermission();
        setDescription("commands.admin.resets.reset.description");
        setParametersHelp("commands.admin.resets.reset.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        // Check if this is @a - therefore, we're resetting everyone's resets :)
        if (args.get(0).equalsIgnoreCase("@a")) {
            this.askConfirmation(user, () -> {
                // Set the reset epoch to now
                getIWM().setResetEpoch(getWorld());
                // Reset all current players
                Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).filter(getPlayers()::isKnown).forEach(u -> getPlayers().setResets(getWorld(), u, 0));
                user.sendMessage("commands.admin.resets.reset.success-everyone");
            });
            return true;
        } else {
            // Then, it may be a player
            UUID targetUUID = Util.getUUID(args.get(0));
            if (targetUUID == null) {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
                return false;
            } else {
                getPlayers().setResets(getWorld(), targetUUID, 0);
                user.sendMessage("commands.admin.resets.reset.success", TextVariables.NAME, args.get(0));
                return true;
            }
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(Collections.singletonList("@a"));
    }
}
