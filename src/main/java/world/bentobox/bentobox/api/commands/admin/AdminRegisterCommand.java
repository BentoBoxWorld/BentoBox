package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class AdminRegisterCommand extends ConfirmableCommand {

    public AdminRegisterCommand(CompositeCommand parent) {
        super(parent, "register");
    }

    @Override
    public void setup() {
        setPermission("admin.register");
        setOnlyPlayer(true);
        setParametersHelp("commands.admin.register.parameters");
        setDescription("commands.admin.register.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Get target
        UUID targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-island");
            return false;
        }
        if (getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("commands.admin.register.cannot-register-team-player");
            return false;
        }

        // Check if this spot is still being deleted
        Location closestIsland = getClosestIsland(user.getLocation());
        if (getPlugin().getIslandDeletionManager().inDeletion(closestIsland)) {
            user.sendMessage("commands.admin.register.in-deletion");
            return false;
        }
        // Check if island is owned
        Optional<Island> island = getIslands().getIslandAt(user.getLocation());
        if (island.map(Island::isOwned).orElse(false)) {
            user.sendMessage("commands.admin.register.already-owned");
            return false;
        }
        // Check if island is spawn
        if (island.map(Island::isSpawn).orElse(false)) {
            askConfirmation(user, user.getTranslation("commands.admin.register.island-is-spawn"), () -> register(user, args.get(0), targetUUID, island, closestIsland));
            return false;
        }
        return register(user, args.get(0),targetUUID, island, closestIsland);
    }

    private boolean register(User user, String targetName, UUID targetUUID, Optional<Island> island, Location closestIsland) {
        // Register island if it exists
        if (!island.map(i -> {
            // Island exists
            getIslands().setOwner(user, targetUUID, i);
            if (i.isSpawn()) {
                getIslands().clearSpawn(i.getWorld());
            }
            user.sendMessage("commands.admin.register.registered-island", "[xyz]", Util.xyz(i.getCenter().toVector()),
                    TextVariables.NAME, targetName);
            user.sendMessage("general.success");
            // Build and call event
            IslandEvent.builder()
            .island(i)
            .location(i.getCenter())
            .reason(IslandEvent.Reason.REGISTERED)
            .involvedPlayer(targetUUID)
            .admin(true)
            .build();
            IslandEvent.builder()
            .island(i)
            .involvedPlayer(targetUUID)
            .admin(true)
            .reason(IslandEvent.Reason.RANK_CHANGE)
            .rankChange(RanksManager.VISITOR_RANK, RanksManager.OWNER_RANK)
            .build();
            return true;
        }).orElse(false)) {
            // Island does not exist - this is a reservation
            this.askConfirmation(user, user.getTranslation("commands.admin.register.no-island-here"), () -> {
                // Make island here
                Island i = getIslands().createIsland(closestIsland, targetUUID);
                if (i == null) {
                    user.sendMessage("commands.admin.register.cannot-make-island");
                    return;
                }
                getIslands().setOwner(user, targetUUID, i);
                i.setReserved(true);
                i.getCenter().getBlock().setType(Material.BEDROCK);
                user.sendMessage("commands.admin.register.reserved-island", "[xyz]", Util.xyz(i.getCenter().toVector()),
                        TextVariables.NAME, targetName);
                // Build and fire event
                IslandEvent.builder()
                .island(i)
                .location(i.getCenter())
                .reason(IslandEvent.Reason.RESERVED)
                .involvedPlayer(targetUUID)
                .admin(true)
                .build();
            });
            return false;
        }
        return true;
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

    /**
     * This returns the coordinate of where an island should be on the grid.
     *
     * @param location - location to check
     * @return Location of where an island should be on a grid in this world
     */
    public Location getClosestIsland(Location location) {
        int dist = getIWM().getIslandDistance(getWorld()) * 2;
        long x = Math.round((double) location.getBlockX() / dist) * dist + getIWM().getIslandXOffset(getWorld());
        long z = Math.round((double) location.getBlockZ() / dist) * dist + getIWM().getIslandZOffset(getWorld());
        long y = getIWM().getIslandHeight(getWorld());
        return new Location(location.getWorld(), x, y, z);
    }

}