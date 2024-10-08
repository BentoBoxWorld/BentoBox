package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link Flags#ENTER_EXIT_MESSAGES} flag and {@link world.bentobox.bentobox.api.events.island.IslandExitEvent} and {@link world.bentobox.bentobox.api.events.island.IslandEnterEvent}.
 * @author tastybento
 */
public class EnterExitListener extends FlagListener {

    private static final Vector XZ = new Vector(1,0,1);
    private static final String ISLAND_MESSAGE = "protection.flags.ENTER_EXIT_MESSAGES.island";

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        handleEnterExit(User.getInstance(e.getPlayer()), e.getFrom(), e.getTo(), e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        handleEnterExit(User.getInstance(e.getPlayer()), e.getFrom(), e.getTo(), e);
    }

    private void handleEnterExit(@NonNull User user, @NonNull Location from, @Nullable Location to,
            @NonNull PlayerMoveEvent e) {
        // Only process if there is a change in X or Z coords
        if (from.getWorld() != null && to != null && from.getWorld().equals(to.getWorld())
                && from.toVector().multiply(XZ).equals(to.toVector().multiply(XZ))) {
            return;
        }

        Optional<Island> islandFrom = getIslands().getProtectedIslandAt(from);
        Optional<Island> islandTo = to == null ? Optional.empty() : getIslands().getProtectedIslandAt(to);

        /*
         * Options:
         *
         * from = empty, to = island - entering
         * from = island1, to = island2 - leaving 1, entering 2
         * from = island, to = empty - leaving
         * from = empty, to = empty
         * from = island, to = island
         */
        if (islandFrom.equals(islandTo)) {
            return;
        }

        islandFrom.ifPresent(i -> {
            // Fire the IslandExitEvent
            new IslandEvent.IslandEventBuilder()
            .island(i)
            .oldIsland(islandTo.orElse(null))
            .involvedPlayer(user.getUniqueId())
            .reason(IslandEvent.Reason.EXIT)
            .admin(false)
            .location(user.getLocation())
            .rawEvent(e)
            .build();

            sendExitNotification(user, i);
        });

        islandTo.ifPresent(i -> {
            // Fire the IslandEnterEvent
            new IslandEvent.IslandEventBuilder()
            .island(i)
            .oldIsland(islandFrom.orElse(null))
            .involvedPlayer(user.getUniqueId())
            .reason(IslandEvent.Reason.ENTER)
            .admin(false)
            .location(user.getLocation())
            .rawEvent(e)
            .build();

            sendEnterNotification(user, i);
        });
    }

    /**
     * Sends a notification to this user telling them they exited this island.
     * @param user the User to send the notification to, not null.
     * @param island the island the user exited, not null.
     * @since 1.4.0
     */
    private void sendExitNotification(@NonNull User user, @NonNull Island island) {
        // Only process if ENTER_EXIT_MESSAGES is enabled
        if (!Flags.ENTER_EXIT_MESSAGES.isSetForWorld(island.getWorld())) {
            return;
        }

        // Send message if island is owned by someone
        if (island.isOwned()) {
            // Leave messages are always specific to this world
            String islandMessage = user.getTranslation(island.getWorld(), ISLAND_MESSAGE, TextVariables.NAME, getPlugin().getPlayers().getName(island.getOwner()));
            // Send specific message if the player is member of this island
            if (island.inTeam(user.getUniqueId())) {
                user.notify(island.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.now-leaving-your-island", TextVariables.NAME, (island.getName() != null) ? island.getName() : islandMessage);
            } else {
                user.notify(island.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.now-leaving", TextVariables.NAME, (island.getName() != null) ? island.getName() : islandMessage);
            }
        }
        // Send message if island is unowned, but has a name
        else if (island.getName() != null) {
            user.notify(island.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.now-leaving", TextVariables.NAME, island.getName());
        }
    }

    /**
     * Sends a notification to this user telling them they entered this island.
     * @param user the User to send the notification to, not null.
     * @param island the island the user entered, not null.
     * @since 1.4.0
     */
    private void sendEnterNotification(@NonNull User user, @NonNull Island island) {
        // Only process if ENTER_EXIT_MESSAGES is enabled
        if (!Flags.ENTER_EXIT_MESSAGES.isSetForWorld(island.getWorld())) {
            return;
        }
        // Send message if island is owned by someone
        if (island.isOwned()) {
            // Enter messages are always specific to this world
            String islandMessage = user.getTranslation(island.getWorld(), ISLAND_MESSAGE, TextVariables.NAME, getPlugin().getPlayers().getName(island.getOwner()));
            // Send specific message if the player is member of this island
            if (island.inTeam(user.getUniqueId())) {
                user.notify(island.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.now-entering-your-island", TextVariables.NAME, (island.getName() != null) ? island.getName() : islandMessage);
            } else {
                user.notify(island.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.now-entering", TextVariables.NAME, (island.getName() != null) ? island.getName() : islandMessage);
            }
        }
        // Send message if island is unowned, but has a name
        else if (island.getName() != null) {
            user.notify(island.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.now-entering", TextVariables.NAME, island.getName());
        }
    }
}
