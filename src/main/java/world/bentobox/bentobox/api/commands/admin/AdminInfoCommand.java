package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.IslandInfo;
import world.bentobox.bentobox.util.Util;

/**
 * Shows admin info about an island.
 * <p>
 * {@code /[admin] info} shows the island the admin is standing on.
 * {@code /[admin] info <player>} shows every island the player has in this world.
 * {@code /[admin] info <player> <island name>} shows just that island; island names
 * and home names are resolved the same way as {@code /[admin] tp}.
 */
public class AdminInfoCommand extends CompositeCommand {

    public AdminInfoCommand(CompositeCommand parent) {
        super(parent, "info");
    }

    @Override
    public void setup() {
        setPermission("mod.info");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.info.parameters");
        setDescription("commands.admin.info.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty() && !user.isPlayer()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // If there are no args, then the player wants info on the island at this location
        if (args.isEmpty()) {
            getIslands().getIslandAt(user.getLocation()).ifPresentOrElse(i -> new IslandInfo(i).showAdminInfo(user, getAddon()), () ->
            user.sendMessage("commands.admin.info.no-island"));
            return true;
        }
        // Get target player
        UUID targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        List<Island> islands = getIslands().getIslands(getWorld(), targetUUID);
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        if (args.size() == 1) {
            // Show info for every island this player has in this world
            islands.forEach(island -> new IslandInfo(island).showAdminInfo(user, getAddon()));
            return true;
        }
        // They named the island they want info on
        Map<String, IslandGoCommand.IslandInfo> names = IslandGoCommand.getNameIslandMap(User.getInstance(targetUUID), getWorld());
        final String typed = String.join(" ", args.subList(1, args.size()));
        final String name = IslandGoCommand.resolveName(typed, names.keySet());
        if (name == null) {
            user.sendMessage("commands.island.go.unknown-home");
            user.sendMessage("commands.island.sethome.homes-are");
            names.keySet()
                    .forEach(n -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, n));
            return false;
        }
        new IslandInfo(names.get(name).island()).showAdminInfo(user, getAddon());
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        if (args.size() == 1) {
            List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
            return Optional.of(Util.tabLimit(options, lastArg));
        }
        // Offer the target player's island and home names
        UUID targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(IslandGoCommand.getNameIslandMap(User.getInstance(targetUUID), getWorld()).keySet());
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
