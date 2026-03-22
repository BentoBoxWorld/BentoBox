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
        user.sendRawMessage("(" + Bukkit.getVersion() + ")");
        user.sendMessage("commands.bentobox.version.plugin-version", TextVariables.VERSION, getPlugin().getPluginMeta().getVersion());
        user.sendMessage("commands.bentobox.version.database", "[database]", getSettings().getDatabaseType().toString());
        user.sendMessage("commands.bentobox.version.loaded-game-worlds");

        getIWM().getOverWorldNames().entrySet().stream().sorted(Map.Entry.comparingByKey())
        .forEach(e -> {
            String worlds = buildWorldsList(user, e.getKey());
            user.sendMessage(user.getTranslation("commands.bentobox.version.game-world", TextVariables.NAME, e.getKey(), "[addon]", e.getValue(),
                    "[worlds]", worlds));
        });

        user.sendMessage("commands.bentobox.version.loaded-addons");
        getPlugin().getAddonsManager().getAddons().stream().sorted(Comparator.comparing(o -> o.getDescription().getName().toLowerCase(Locale.ENGLISH)))
        .forEach(a -> user.sendMessage("commands.bentobox.version.addon-syntax", TextVariables.NAME, a.getDescription().getName(),
                TextVariables.VERSION, a.getDescription().getVersion(), "[state]", a.getState().toString()));

        return true;
    }

    private String buildWorldsList(User user, String worldName) {
        String worlds = user.getTranslation("general.worlds.overworld");
        Optional<GameModeAddon> addonOptional = getIWM().getAddon(Bukkit.getWorld(worldName));
        if (addonOptional.isEmpty()) {
            return worlds;
        }
        GameModeAddon addon = addonOptional.get();
        worlds += dimensionSuffix(user, "general.worlds.nether",
                addon.getNetherWorld() != null && getIWM().isNetherGenerate(addon.getOverWorld()),
                getIWM().isNetherIslands(addon.getOverWorld()));
        worlds += dimensionSuffix(user, "general.worlds.the-end",
                addon.getEndWorld() != null && getIWM().isEndGenerate(addon.getOverWorld()),
                getIWM().isEndIslands(addon.getOverWorld()));
        return worlds;
    }

    private String dimensionSuffix(User user, String translationKey,
            boolean isGenerated, boolean isIslands) {
        if (!isGenerated) {
            return "";
        }
        return ", " + user.getTranslation(translationKey) + (isIslands ? "" : "*");
    }
}
