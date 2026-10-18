package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand.IslandInfo;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.TaskScheduler;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * Enables admins to teleport a player to another player's island, nether or end islands, 
 * <p>
 * For example /acid tp lspvicky tastybento [island name] would teleport lspvicky to tastybento's [named] island
 * 
 */
public class AdminTeleportUserCommand extends CompositeCommand {

    private static final String NOT_SAFE = "general.errors.no-safe-location-found";
    private record TeleportTarget(User teleportee, Location location) { }

    /**
     * @param parent - parent command
     * @param tpCommand - should be "tpuser", "tpusernether" or "tpuserend"
     */
    public AdminTeleportUserCommand(CompositeCommand parent, String tpCommand) {
        super(parent, tpCommand);
    }

    @Override
    public void setup() {
        // Permission
        setPermission("admin.tpuser");
        setParametersHelp("commands.admin.tpuser.parameters");
        setDescription("commands.admin.tpuser.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        return resolveTarget(user, args) != null;
    }

    private @Nullable TeleportTarget resolveTarget(User user, List<String> args) {
        if (args.size() < 2) {
            this.showHelp(this, user);
            return null;
        }
        // Convert first name to a UUID
        UUID teleportee = Util.getUUID(args.getFirst());
        if (teleportee == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return null;
        }
        // Check online
        User toBeTeleported = User.getInstance(teleportee);
        if (!toBeTeleported.isOnline()) {
            user.sendMessage("general.errors.offline-player");
            return null;
        }

        // Convert second name to a UUID
        UUID targetUUID = Util.getUUID(args.get(1));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(1));
            return null;
        }

        // Check island exists
        if (!getIslands().hasIsland(getWorld(), targetUUID) && !getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return null;
        }

        World world = getWorld();
        if (getLabel().equals("tpusernether")) {
            world = getPlugin().getIWM().getNetherWorld(getWorld());
        } else if (getLabel().equals("tpuserend")) {
            world = getPlugin().getIWM().getEndWorld(getWorld());
        }
        if (world == null) {
            user.sendMessage(NOT_SAFE);
            return null;
        }
        // Get default location if there are no arguments
        Location warpSpot = getSpot(world, targetUUID);
        if (warpSpot == null) {
            user.sendMessage(NOT_SAFE);
            return null;
        }
        if (args.size() == 2) {
            return new TeleportTarget(toBeTeleported, warpSpot);
        }

        // They named the island to go to
        Map<String, world.bentobox.bentobox.api.commands.island.IslandGoCommand.IslandInfo> names = IslandGoCommand
                .getNameIslandMap(User.getInstance(targetUUID), getWorld());
        final String name = String.join(" ", args.subList(2, args.size()));
        if (!names.containsKey(name)) {
            // Failed home name check
            user.sendMessage("commands.island.go.unknown-home");
            user.sendMessage("commands.island.sethome.homes-are");
            names.keySet()
                    .forEach(n -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, n));
            return null;
        } else if (names.size() > 1) {
            IslandInfo info = names.get(name);
            Island island = info.island();
            warpSpot = island.getSpawnPoint(world.getEnvironment()) != null
                    ? island.getSpawnPoint(world.getEnvironment())
                    : island.getProtectionCenter().toVector().toLocation(world);
        }
        return new TeleportTarget(toBeTeleported, warpSpot);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        TeleportTarget target = resolveTarget(user, args);
        if (target == null) return false;
        Location warpSpot = target.location();
        // Otherwise, ask the admin to go to a safe spot
        String failureMessage = user.getTranslation("commands.admin.tp.manual", "[location]", warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
                + warpSpot.getBlockZ());
        // Set the player
        Player player = target.teleportee().getPlayer();
        if (player == null) {
            user.sendMessage("general.errors.offline-player");
            return false;
        }
        if (args.size() == 2) {
            failureMessage = user.getTranslation(NOT_SAFE);
        }

        // Teleport
        new SafeSpotTeleport.Builder(getPlugin())
        .entity(player)
        .location(warpSpot)
        .failureMessage(failureMessage)
        .thenRun(() -> TaskScheduler.user(getPlugin(), user, () -> user.sendMessage("general.success")))
        .build();
        return true;
    }

    private Location getSpot(World world, UUID targetUUID) {
        Island island = getIslands().getIsland(world, targetUUID);
        if (island == null) {
            return null;
        }
        return island.getSpawnPoint(world.getEnvironment()) != null ? island.getSpawnPoint(world.getEnvironment()) : island.getProtectionCenter().toVector().toLocation(world);
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        if (args.size() == 2 || args.size() == 3) {
            return Optional.of(Util.tabLimit(new ArrayList<>(Util.getOnlinePlayerList(user)), lastArg));
        }

        if (args.size() == 4) {
            UUID target = Util.getUUID(args.get(2));
            return target == null ? Optional.empty()
                    : Optional
                            .of(Util.tabLimit(
                                    new ArrayList<>(IslandGoCommand
                                            .getNameIslandMap(User.getInstance(target), getWorld()).keySet()),
                                    lastArg));
        }
        return Optional.empty();
    }

}
