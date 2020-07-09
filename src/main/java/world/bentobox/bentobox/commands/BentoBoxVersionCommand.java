package world.bentobox.bentobox.commands;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.versions.ServerCompatibility;
import world.bentobox.bentobox.versions.ServerCompatibility.ServerSoftware;

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
        super(parent, "version", "v", "ver", "versions", "addons");
    }

    @Override
    public void setup() {
        setPermission("bentobox.version");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        ServerCompatibility.ServerSoftware serverSoftware = ServerCompatibility.getInstance().getServerSoftware();
        ServerCompatibility.ServerVersion serverVersion = ServerCompatibility.getInstance().getServerVersion();

        user.sendMessage("commands.bentobox.version.server",
                TextVariables.NAME, serverSoftware.equals(ServerSoftware.UNKNOWN) ? user.getTranslation("general.invalid") + " (" + serverSoftware.getName() + ")" : serverSoftware.toString(),
                        TextVariables.VERSION, serverVersion != null ? serverVersion.toString() : user.getTranslation("general.invalid"));
        user.sendMessage("commands.bentobox.version.plugin-version", TextVariables.VERSION, getPlugin().getDescription().getVersion());
        user.sendMessage("commands.bentobox.version.database", "[database]", getSettings().getDatabaseType().toString());
        user.sendMessage("commands.bentobox.version.loaded-game-worlds");

        getIWM().getOverWorldNames().entrySet().stream().sorted(Map.Entry.comparingByKey())
        .forEach(e -> {
            String worlds = user.getTranslation("general.worlds.overworld");

            // It should be present, but let's stay safe.
            Optional<GameModeAddon> addonOptional = getIWM().getAddon(Bukkit.getWorld(e.getKey()));
            if (addonOptional.isPresent()) {
                GameModeAddon addon = addonOptional.get();
                /*
                 * If the dimension is generated, it is displayed.
                 * If the dimension is not made up of islands, a '*' is appended to its name.
                 */
                // Append the nether
                if (addon.getNetherWorld() != null && getIWM().isNetherGenerate(addon.getOverWorld())) {
                    worlds += ", " + user.getTranslation("general.worlds.nether");
                    if (!getIWM().isNetherIslands(addon.getOverWorld())) {
                        worlds += "*";
                    }
                }

                // Append the End
                if (addon.getEndWorld() != null && getIWM().isEndGenerate(addon.getOverWorld())) {
                    worlds += ", " + user.getTranslation("general.worlds.the-end");
                    if (!getIWM().isEndIslands(addon.getOverWorld())) {
                        worlds += "*";
                    }
                }
            }

            user.sendMessage(user.getTranslation("commands.bentobox.version.game-world", TextVariables.NAME, e.getKey(), "[addon]", e.getValue(),
                    "[worlds]", worlds));
        });

        user.sendMessage("commands.bentobox.version.loaded-addons");
        getPlugin().getAddonsManager().getAddons().stream().sorted(Comparator.comparing(o -> o.getDescription().getName().toLowerCase(Locale.ENGLISH)))
        .forEach(a -> user.sendMessage("commands.bentobox.version.addon-syntax", TextVariables.NAME, a.getDescription().getName(),
                TextVariables.VERSION, a.getDescription().getVersion(), "[state]", a.getState().toString()));

        return true;
    }
}
