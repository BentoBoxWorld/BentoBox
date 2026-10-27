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
import world.bentobox.bentobox.util.TaskScheduler;

public class AdminRegisterCommand extends ConfirmableCommand {

    private record RegisterTarget(Island island, UUID player) { }

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
        return resolveTarget(user, args) != null;
    }

    private @Nullable RegisterTarget resolveTarget(User user, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return null;
        }
        // Check world
        if (!getWorld().equals(user.getWorld())) {
            user.sendMessage("general.errors.wrong-world");
            return null;
        }
        // Get target
        UUID targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return null;
        }
        // Check if this spot is being deleted
        Location closestIsland = Util.getClosestIsland(user.getLocation());
        boolean inDeletion = getPlugin().getIslandDeletionManager().inDeletion(closestIsland);
        // Check if island is owned
        Optional<Island> opIsland = getIslands().getIslandAt(user.getLocation());
        if (opIsland.isEmpty()) {
            // Reserve spot. Capture the context now so the deferred confirmation
            // acts on the same target/location even if the admin moves first.
            final UUID uuid = targetUUID;
            final Location loc = closestIsland;
            final String name = args.getFirst();
            this.askConfirmation(user, user.getTranslation("commands.admin.register.no-island-here"),
                    () -> reserve(user, uuid, loc, name));
            return null;
        }
        Island island = opIsland.get();
        if (targetUUID.equals(island.getOwner())) {
            user.sendMessage("commands.admin.register.already-owned");
            return null;
        }
        // Island is pending deletion - registering it will cancel the deletion, so confirm first
        if (inDeletion) {
            confirmRegister(user, "commands.admin.register.in-deletion", args.getFirst(), targetUUID, island);
            return null;
        }
        // Check if island is spawn
        if (island.isSpawn()) {
            confirmRegister(user, "commands.admin.register.island-is-spawn", args.getFirst(), targetUUID, island);
            return null;
        }

        return new RegisterTarget(island, targetUUID);
    }

    /**
     * Ask the user to confirm registering the island they are on. The target and
     * island are captured now so the deferred action stays stable even if
     * {@link #canExecute(User, String, List)} re-runs before confirmation.
     *
     * @param user user doing the registering
     * @param messageKey confirmation prompt translation key
     * @param targetName name of the target player
     * @param uuid target player UUID
     * @param targetIsland island selected for registration
     */
    private void confirmRegister(User user, String messageKey, String targetName, UUID uuid, Island targetIsland) {
        UUID owner = targetIsland.getOwner();
        askConfirmation(user, user.getTranslation(messageKey), () -> {
            if (!Objects.equals(owner, targetIsland.getOwner())
                    || getIslands().getIslandAt(targetIsland.getCenter()).filter(targetIsland::equals).isEmpty()) {
                user.sendMessage("commands.admin.register.cannot-make-island");
                return;
            }
            register(user, uuid, targetIsland, targetName);
        });
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        RegisterTarget target = resolveTarget(user, args);
        if (target == null) return false;
        register(user, target.player(), target.island(), args.getFirst());
        return true;
    }

    /**
     * Reserve a spot for a target
     * @param user user doing the reserving
     * @param targetUUID UUID of the target player
     * @param closestIsland location to reserve
     * @param targetName target name
     */
    void reserve(User user, UUID targetUUID, Location closestIsland, String targetName) {
        Objects.requireNonNull(closestIsland);
        Objects.requireNonNull(targetUUID);
        if (getIslands().getIslandAt(closestIsland).isPresent()
                || getPlugin().getIslandDeletionManager().inDeletion(closestIsland)) {
            user.sendMessage("commands.admin.register.cannot-make-island");
            return;
        }
        // Island does not exist - this is a reservation
        // Make island here
        Island i = getIslands().createIsland(closestIsland, targetUUID);
        if (i == null) {
            user.sendMessage("commands.admin.register.cannot-make-island");
            return;
        }
        getIslands().setOwner(user, targetUUID, i, RanksManager.VISITOR_RANK);
        i.setReserved(true);
        if (TaskScheduler.isFolia()) {
            Location center = i.getCenter().clone();
            java.util.concurrent.atomic.AtomicBoolean markerPlaced = new java.util.concurrent.atomic.AtomicBoolean();
            Util.getChunkAtAsync(center)
                    .thenCompose(chunk -> TaskScheduler.regionFuture(getPlugin(), center,
                            () -> {
                                if (getIslands().getIslandAt(center).filter(i::equals).isEmpty()
                                        || !i.isReserved() || !targetUUID.equals(i.getOwner())) {
                                    throw new IllegalStateException("Reservation changed before marker placement");
                                }
                                center.getBlock().setType(Material.BEDROCK);
                                markerPlaced.set(true);
                            }))
                    .thenRun(() -> completeReservation(user, targetUUID, targetName, i))
                    .exceptionally(error -> {
                        if (markerPlaced.get()) {
                            // The island is real; only the announcement failed. Do not delete it,
                            // but tell the admin the state it is in.
                            getPlugin().logError("Reserved island " + i.getUniqueId()
                                    + " was created but could not be announced: " + error.getMessage());
                            TaskScheduler.user(getPlugin(), user, () -> user.sendMessage(
                                    "commands.admin.register.reserved-island", TextVariables.XYZ,
                                    Util.xyz(i.getCenter().toVector()), TextVariables.NAME, targetName));
                            return null;
                        }
                        getPlugin().logError("Reserved island " + i.getUniqueId()
                                + " could not place its marker: " + error.getMessage());
                        // The reservation was never announced: drop it so the state matches the
                        // message. Re-check inside the region thread to avoid a TOCTOU window.
                        TaskScheduler.regionFuture(getPlugin(), i.getCenter(), () -> {
                            if (!i.isReserved() || !targetUUID.equals(i.getOwner())) {
                                return;
                            }
                            getIslands().deleteIsland(i, true, targetUUID);
                            if (i.isOwned()) {
                                getPlugin().logError("Reserved island " + i.getUniqueId()
                                        + " could not be deleted, orphaning it");
                                getIslands().orphanIsland(i);
                            }
                        }).exceptionally(deleteError -> {
                            getPlugin().logError("Reserved island " + i.getUniqueId()
                                    + " could not be deleted: " + deleteError.getMessage());
                            return null;
                        });
                        TaskScheduler.user(getPlugin(), user,
                                () -> user.sendMessage("commands.admin.register.cannot-make-island"));
                        return null;
                    });
            return;
        }
        i.getCenter().getBlock().setType(Material.BEDROCK);
        completeReservation(user, targetUUID, targetName, i);
    }

    private void completeReservation(User user, UUID targetUUID, String targetName, Island i) {
        Runnable notify = () -> user.sendMessage("commands.admin.register.reserved-island", TextVariables.XYZ,
                Util.xyz(i.getCenter().toVector()), TextVariables.NAME, targetName);
        if (TaskScheduler.isFolia()) TaskScheduler.user(getPlugin(), user, notify);
        else notify.run();
        // Build and fire event
        IslandEvent.builder().island(i).location(i.getCenter()).reason(IslandEvent.Reason.RESERVED)
                .involvedPlayer(targetUUID).admin(true).build();
    }

    /**
     * Register the island to a target
     * @param user user doing the registering
     * @param targetUUID UUID of the target player
     * @param island island to register
     * @param targetName name of target
     */
    void register(User user, UUID targetUUID, Island island, String targetName) {
        Objects.requireNonNull(targetUUID);
        Objects.requireNonNull(island);
        // Island exists
        getIslands().setOwner(user, targetUUID, island, RanksManager.VISITOR_RANK);
        if (island.isSpawn()) {
            getIslands().clearSpawn(island.getWorld());
        }
        // Remove deletion status if it has been assigned so the island is not
        // reaped by the region-file purge and stays owned by the new player.
        getIslands().undeleteIsland(island);
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
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}
