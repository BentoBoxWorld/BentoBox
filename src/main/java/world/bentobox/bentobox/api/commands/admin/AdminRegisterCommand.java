package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class AdminRegisterCommand extends ConfirmableCommand {

    private Island island;
    private Location closestIsland;
    private @Nullable UUID targetUUID;

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
    public boolean canExecute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Check world
        if (!getWorld().equals(user.getWorld())) {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
        // Get target
        targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Check if this spot is still being deleted
        closestIsland = Util.getClosestIsland(user.getLocation());
        if (getPlugin().getIslandDeletionManager().inDeletion(closestIsland)) {
            user.sendMessage("commands.admin.register.in-deletion");
            return false;
        }
        // Check if island is owned
        Optional<Island> opIsland = getIslands().getIslandAt(user.getLocation());
        if (opIsland.isEmpty()) {
            // Reserve spot
            this.askConfirmation(user, user.getTranslation("commands.admin.register.no-island-here"),
                    () -> reserve(user, args.get(0)));
            return false;
        }
        island = opIsland.get();
        if (targetUUID.equals(island.getOwner())) {
            user.sendMessage("commands.admin.register.already-owned");
            return false;
        }
        // Check if island is spawn
        if (island.isSpawn()) {
            askConfirmation(user, user.getTranslation("commands.admin.register.island-is-spawn"),
                    () -> register(user, args.get(0)));
            return false;
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        register(user, args.get(0));
        return true;
    }

    /**
     * Reserve a spot for a target
     * @param user user doing the reserving
     * @param targetName target name
     */
    void reserve(User user, String targetName) {
        Objects.requireNonNull(closestIsland);
        Objects.requireNonNull(targetUUID);
        // Island does not exist - this is a reservation
        // Make island here
        Island i = getIslands().createIsland(closestIsland, targetUUID);
        if (i == null) {
            user.sendMessage("commands.admin.register.cannot-make-island");
            return;
        }
        getIslands().setOwner(user, targetUUID, i, RanksManager.VISITOR_RANK);
        i.setReserved(true);
        i.getCenter().getBlock().setType(Material.BEDROCK);
        user.sendMessage("commands.admin.register.reserved-island", TextVariables.XYZ,
                Util.xyz(i.getCenter().toVector()), TextVariables.NAME, targetName);
        // Build and fire event
        IslandEvent.builder().island(i).location(i.getCenter()).reason(IslandEvent.Reason.RESERVED)
                .involvedPlayer(targetUUID).admin(true).build();
    }

    /**
     * Register the island to a target
     * @param user user doing the registering
     * @param targetName name of target
     */
    void register(User user, String targetName) {
        Objects.requireNonNull(closestIsland);
        Objects.requireNonNull(targetUUID);
        Objects.requireNonNull(island);
        // Island exists
        getIslands().setOwner(user, targetUUID, island, RanksManager.VISITOR_RANK);
        if (island.isSpawn()) {
            getIslands().clearSpawn(island.getWorld());
        }
        // Remove deletion status if it has been assigned.
        island.setDeleted(false);
        user.sendMessage("commands.admin.register.registered-island", TextVariables.XYZ,
                Util.xyz(island.getCenter().toVector()), TextVariables.NAME, targetName);
        user.sendMessage("general.success");
        // Build and call event
        IslandEvent.builder().island(island).location(island.getCenter()).reason(IslandEvent.Reason.REGISTERED)
                .involvedPlayer(targetUUID).admin(true).build();
        IslandEvent.builder().island(island).involvedPlayer(targetUUID).admin(true)
                .reason(IslandEvent.Reason.RANK_CHANGE).rankChange(RanksManager.VISITOR_RANK, RanksManager.OWNER_RANK)
                .build();
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
