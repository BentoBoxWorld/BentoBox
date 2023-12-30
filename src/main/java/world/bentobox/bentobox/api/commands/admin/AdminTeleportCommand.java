package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

public class AdminTeleportCommand extends CompositeCommand {

    private static final String NOT_SAFE = "general.errors.no-safe-location-found";
    private @Nullable UUID targetUUID;
    private @Nullable User userToTeleport;

    /**
     * @param parent - parent command
     * @param tpCommand - should be "tp", "tpnether" or "tpend"
     */
    public AdminTeleportCommand(CompositeCommand parent, String tpCommand) {
        super(parent, tpCommand);
    }

    @Override
    public void setup() {
        // Permission
        setPermission("admin.tp");
        setParametersHelp("commands.admin.tp.parameters");
        setDescription("commands.admin.tp.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1 && args.size() != 2) {
            this.showHelp(this, user);
            return false;
        }
        // Check for console or not
        if (!user.isPlayer() && args.size() != 2) {
            user.sendMessage("general.errors.use-in-game");
            return false;
        }
        // Convert name to a UUID
        targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Check island exists
        if (!getIslands().hasIsland(getWorld(), targetUUID) && !getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        if (args.size() == 2) {
            // We are trying to teleport another player
            UUID playerToTeleportUUID = Util.getUUID(args.get(1));
            if (playerToTeleportUUID == null) {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(1));
                return false;
            } else {
                userToTeleport = User.getInstance(playerToTeleportUUID);
                if (!userToTeleport.isOnline()) {
                    user.sendMessage("general.errors.offline-player");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        World world = getWorld();
        if (getLabel().equals("tpnether")) {
            world = getPlugin().getIWM().getNetherWorld(getWorld());
        } else if (getLabel().equals("tpend")) {
            world = getPlugin().getIWM().getEndWorld(getWorld());
        }
        if (world == null) {
            user.sendMessage(NOT_SAFE);
            return false;
        }
        Location warpSpot = getSpot(world);
        if (warpSpot == null) {
            user.sendMessage(NOT_SAFE);
            return false;
        }

        // Otherwise, ask the admin to go to a safe spot
        String failureMessage = user.getTranslation("commands.admin.tp.manual", "[location]", warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
                + warpSpot.getBlockZ());
        // Set the player
        Player player = args.size() == 2 ? userToTeleport.getPlayer() : user.getPlayer();
        if (args.size() == 2) {
            failureMessage = userToTeleport.getTranslation(NOT_SAFE);
        }

        // Teleport
        new SafeSpotTeleport.Builder(getPlugin())
        .entity(player)
        .location(warpSpot)
        .failureMessage(failureMessage)
        .thenRun(() -> user.sendMessage("general.success"))
        .build();
        return true;
    }

    private Location getSpot(World world) {
        Island island = getIslands().getIsland(world, targetUUID);
        if (island == null) {
            return null;
        }
        return island.getSpawnPoint(world.getEnvironment()) != null ? island.getSpawnPoint(world.getEnvironment()) : island.getProtectionCenter().toVector().toLocation(world);
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
