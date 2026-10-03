package world.bentobox.bentobox.api.commands.admin.deaths;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.player.PlayerDeathsChangedEvent;
import world.bentobox.bentobox.api.events.player.PlayerDeathsChangedEvent.Action;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * @since 1.8.0
 * @author Poslovitch
 */
public class AdminDeathsAddCommand extends CompositeCommand {

    public AdminDeathsAddCommand(AdminDeathsCommand parent) {
        super(parent, "add");
    }

    @Override
    public void setup() {
        setPermission("admin.deaths.add");
        setDescription("commands.admin.deaths.add.description");
        setParametersHelp("commands.admin.deaths.add.parameters");
    }

    @Override
    public boolean execute(User user, String label, @NonNull List<String> args) {
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }

        UUID targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
        } else if (!Util.isInteger(args.get(1), true) || Integer.parseInt(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
        } else {
            int oldDeaths = getPlayers().getDeaths(getWorld(), targetUUID);
            int amount = Integer.parseInt(args.get(1));
            getPlayers().setDeaths(getWorld(), targetUUID, oldDeaths + amount);
            int newDeaths = getPlayers().getDeaths(getWorld(), targetUUID);
            Bukkit.getPluginManager().callEvent(new PlayerDeathsChangedEvent(getWorld(), targetUUID, Action.ADD,
                    amount, oldDeaths, newDeaths));
            user.sendMessage("commands.admin.deaths.add.success",
                    TextVariables.NAME, args.getFirst(), TextVariables.NUMBER, args.get(1),
                    "[total]", String.valueOf(newDeaths));
            return true;
        }

        return false;
    }
}
