package world.bentobox.bentobox.commands;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.versions.ServerCompatibility;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Displays information about Gamemodes, Addons and versioning.
 *
 * @author tastybento
 */
public class BentoBoxVersionCommand extends CompositeCommand {

    private static final String GAMEWORLD_COLOR_ISLANDS = "&a";
    private static final String GAMEWORLD_COLOR_EXISTS_NO_ISLANDS = "&6";
    private static final String GAMEWORLD_COLOR_NOT_EXIST = "&c";

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
        ServerCompatibility.ServerSoftware serverSoftware = ServerCompatibility.getInstance().getServerSoftware();
        ServerCompatibility.ServerVersion serverVersion = ServerCompatibility.getInstance().getServerVersion();

        user.sendMessage("commands.bentobox.version.server",
                TextVariables.NAME, serverSoftware != null ? serverSoftware.toString() : user.getTranslation("general.invalid"),
                        TextVariables.VERSION, serverVersion != null ? serverVersion.toString() : user.getTranslation("general.invalid"));
        user.sendMessage("commands.bentobox.version.plugin-version", TextVariables.VERSION, getPlugin().getDescription().getVersion());
        user.sendMessage("commands.bentobox.version.loaded-game-worlds");

        getIWM().getOverWorldNames().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
        .forEach(e -> {
            String netherColor = GAMEWORLD_COLOR_ISLANDS;
            String endColor = GAMEWORLD_COLOR_ISLANDS;

            // It should be present, but let's stay safe.
            Optional<GameModeAddon> addonOptional = getIWM().getAddon(Bukkit.getWorld(e.getKey()));
            if (addonOptional.isPresent()) {
                GameModeAddon addon = addonOptional.get();
                /* Get the colors
                   &a = dimension exists and contains islands
                   &6 = dimension exists but is vanilla
                   &c = dimension does not exist
                */
                // Get the nether color
                if (addon.getNetherWorld() == null || !getIWM().isNetherGenerate(addon.getNetherWorld())) {
                    netherColor = GAMEWORLD_COLOR_NOT_EXIST;
                } else if (!getIWM().isNetherIslands(addon.getNetherWorld())) {
                    netherColor = GAMEWORLD_COLOR_EXISTS_NO_ISLANDS;
                }

                // Get the nether color
                if (addon.getEndWorld() == null || !getIWM().isEndGenerate(addon.getEndWorld())) {
                    endColor = GAMEWORLD_COLOR_NOT_EXIST;
                } else if (!getIWM().isEndIslands(addon.getEndWorld())) {
                    endColor = GAMEWORLD_COLOR_EXISTS_NO_ISLANDS;
                }
            }

            user.sendMessage(user.getTranslation("commands.bentobox.version.game-world", TextVariables.NAME, e.getKey(), "[addon]", e.getValue(),
                    "[nether_color]", netherColor, "[end_color]", endColor));
        });

        user.sendMessage("commands.bentobox.version.loaded-addons");
        getPlugin().getAddonsManager().getAddons().stream().sorted(Comparator.comparing(o -> o.getDescription().getName().toLowerCase()))
        .forEach(a -> user.sendMessage("commands.bentobox.version.addon-syntax", TextVariables.NAME, a.getDescription().getName(),
                TextVariables.VERSION, a.getDescription().getVersion(), "[state]", a.getState().toString()));

        return true;
    }
}
