package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.versions.ServerCompatibility;

/**
 * Displays information about Gamemodes, Addons and versioning.
 *
 * @author tastybento
 */
public class BentoBoxVersionCommand extends CompositeCommand {

    /**
     * Info command
     * @param parent - command parent
     */
    public BentoBoxVersionCommand(CompositeCommand parent) {
        super(parent, "version", "v", "versions", "addons");
    }

    @Override
    public void setup() {
        // Not used
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        ServerCompatibility.ServerSoftware serverSoftware = ServerCompatibility.getInstance().getServerSoftware(getPlugin().getServer());
        ServerCompatibility.ServerVersion serverVersion = ServerCompatibility.getInstance().getServerVersion(getPlugin().getServer());

        user.sendMessage("commands.bentobox.version.server",
                TextVariables.NAME, serverSoftware != null ? serverSoftware.toString() : user.getTranslation("general.invalid"),
                        TextVariables.VERSION, serverVersion != null ? serverVersion.toString() : user.getTranslation("general.invalid"));
        user.sendMessage("commands.bentobox.version.plugin-version", TextVariables.VERSION, getPlugin().getDescription().getVersion());
        user.sendMessage("commands.bentobox.version.loaded-game-worlds");

        getIWM().getOverWorldNames().entrySet().stream().sorted((o1, o2)-> o1.getKey().compareTo(o2.getKey()))
        .forEach(e -> user.sendMessage("commands.bentobox.version.game-worlds", TextVariables.NAME, e.getKey(), "[addon]", e.getValue()));

        user.sendMessage("commands.bentobox.version.loaded-addons");
        getPlugin().getAddonsManager().getAddons().stream().sorted((o1, o2)->o1.getDescription().getName().toLowerCase().compareTo(o2.getDescription().getName().toLowerCase()))
        .forEach(a -> user.sendMessage("commands.bentobox.version.addon-syntax", TextVariables.NAME, a.getDescription().getName(),
                TextVariables.VERSION, a.getDescription().getVersion()));

        return true;
    }
}
