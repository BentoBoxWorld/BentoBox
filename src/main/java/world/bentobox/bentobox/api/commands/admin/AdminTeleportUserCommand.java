package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * Enables admins to teleport a player to another player's island, nether or end islands, 
 * <p>
 * For example /acid tp lspvicky tastybento [island name] would teleport lspvicky to tastybento's [named] island
 * 
 */
public class AdminTeleportUserCommand extends CompositeCommand {

    private static final String NOT_SAFE = "general.errors.no-safe-location-found";
    private Location warpSpot;
    private @Nullable UUID targetUUID;
    private User toBeTeleported;

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
        if (args.isEmpty() || args.size() == 1) {
            this.showHelp(this, user);
            return false;
        }
        // Convert first name to a UUID
        UUID teleportee = Util.getUUID(args.getFirst());
        if (teleportee == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        // Check online
        toBeTeleported = User.getInstance(teleportee);
        if (!toBeTeleported.isOnline()) {
            user.sendMessage("general.errors.offline-player");
            return false;
        }

        // Convert second name to a UUID
        targetUUID = Util.getUUID(args.get(1));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }

        // Check island exists
        if (!getIslands().hasIsland(getWorld(), targetUUID) && !getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        World world = getWorld();
        if (getLabel().equals("tpusernether")) {
            world = getPlugin().getIWM().getNetherWorld(getWorld());
        } else if (getLabel().equals("tpuserend")) {
            world = getPlugin().getIWM().getEndWorld(getWorld());
        }
        if (world == null) {
            user.sendMessage(NOT_SAFE);
            return false;
        }
        // Get default location if there are no arguments
        warpSpot = getSpot(world);
        if (warpSpot == null) {
            user.sendMessage(NOT_SAFE);
            return false;
        }
        if (args.size() == 2) {
            return true;
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
            return false;
        } else if (names.size() > 1) {
            IslandInfo info = names.get(name);
            Island island = info.island();
            warpSpot = island.getSpawnPoint(world.getEnvironment()) != null
                    ? island.getSpawnPoint(world.getEnvironment())
                    : island.getProtectionCenter().toVector().toLocation(world);
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Objects.requireNonNull(warpSpot);
        // Otherwise, ask the admin to go to a safe spot
        String failureMessage = user.getTranslation("commands.admin.tp.manual", "[location]", warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
                + warpSpot.getBlockZ());
        // Set the player
        Player player = toBeTeleported.getPlayer();
        if (args.size() == 2) {
            failureMessage = user.getTranslation(NOT_SAFE);
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
        assert targetUUID != null;
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
