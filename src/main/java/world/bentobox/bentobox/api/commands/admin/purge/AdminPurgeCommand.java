package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandDeletedEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class AdminPurgeCommand extends CompositeCommand implements Listener {

    private static final Long YEAR2000 = 946713600L;
    private int count;
    private boolean inPurge;
    private boolean scanning;
    private boolean toBeConfirmed;
    private Iterator<String> it;
    private User user;
    private Set<String> islands = new HashSet<>();

    public AdminPurgeCommand(CompositeCommand parent) {
        super(parent, "purge");
        getAddon().registerListener(this);
    }

    @Override
    public void setup() {
        setPermission("admin.purge");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.parameters");
        setDescription("commands.admin.purge.description");
        new AdminPurgeStatusCommand(this);
        new AdminPurgeStopCommand(this);
        new AdminPurgeUnownedCommand(this);
        new AdminPurgeProtectCommand(this);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (scanning) {
            user.sendMessage("commands.admin.purge.scanning-in-progress");
            return false;
        }
        if (inPurge) {
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, this.getTopLabel());
            return false;
        }
        if (args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.get(0).equalsIgnoreCase("confirm") && toBeConfirmed && this.user.equals(user)) {
            removeIslands();
            return true;
        }
        // Clear tbc
        toBeConfirmed = false;
        islands.clear();
        this.user = user;
        try {
            int days = Integer.parseInt(args.get(0));
            if (days < 1) {
                user.sendMessage("commands.admin.purge.days-one-or-more");
                return false;
            }
            user.sendMessage("commands.admin.purge.scanning");
            scanning = true;
            getOldIslands(days).thenAccept(islandSet -> {
                user.sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER,
                        String.valueOf(islandSet.size()));
                if (!islandSet.isEmpty()) {
                    toBeConfirmed = true;
                    user.sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, this.getTopLabel());
                    islands = islandSet;
                } else {
                    user.sendMessage("commands.admin.purge.none-found");
                }
                scanning = false;
            });

        } catch (NumberFormatException e) {
            user.sendMessage("commands.admin.purge.number-error");
            return false;
        }
        return true;
    }

    void removeIslands() {
        inPurge = true;
        user.sendMessage("commands.admin.purge.see-console-for-status", TextVariables.LABEL, this.getTopLabel());
        it = islands.iterator();
        count = 0;
        // Delete first island
        deleteIsland();
    }

    private void deleteIsland() {
        if (it.hasNext()) {
            getIslands().getIslandById(it.next()).ifPresent(i -> {
                getIslands().deleteIsland(i, true, null);
                count++;
                String percentage = String.format("%.1f", (((float) count)/getPurgeableIslandsCount() * 100));
                getPlugin().log(count + " islands purged out of " + getPurgeableIslandsCount() + " (" + percentage + " %)");
            });
        } else {
            user.sendMessage("commands.admin.purge.completed");
            inPurge = false;
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    void onIslandDeleted(IslandDeletedEvent e) {
        if (inPurge) {
            deleteIsland();
        }
    }

    /**
     * Gets a set of islands that are older than the parameter in days
     * @param days days
     * @return set of islands
     */
    CompletableFuture<Set<String>> getOldIslands(int days) {
        CompletableFuture<Set<String>> result = new CompletableFuture<>();
        // Process islands in one pass, logging and adding to the set if applicable
        getPlugin().getIslands().getIslandsASync().thenAccept(list -> {
            user.sendMessage("commands.admin.purge.total-islands", TextVariables.NUMBER, String.valueOf(list.size()));
            Set<String> oldIslands = new HashSet<>();
            list.stream()
                .filter(i -> !i.isSpawn()).filter(i -> !i.getPurgeProtected())
                .filter(i -> i.getWorld() != null) // to handle currently unloaded world islands
                    .filter(i -> i.getWorld().equals(this.getWorld())) // Island needs to be in this world
                    .filter(Island::isOwned) // The island needs to be owned
                    .filter(i -> i.getMemberSet().stream().allMatch(member -> checkLastLoginTimestamp(days, member)))
                .forEach(i -> {
                    // Add the unique island ID to the set
                    oldIslands.add(i.getUniqueId());
                    getPlugin().log("Will purge island at " + Util.xyz(i.getCenter().toVector()) + " in "
                            + i.getWorld().getName());
                    // Log each member's last login information
                    i.getMemberSet().forEach(member -> {
                            Long timestamp = getPlayers().getLastLoginTimestamp(member);
                            Date lastLogin = new Date(timestamp);
                        BentoBox.getInstance()
                                .log("Player " + BentoBox.getInstance().getPlayers().getName(member)
                                        + " last logged in "
                                            + (int) ((System.currentTimeMillis() - timestamp) / 1000 / 3600 / 24)
                                        + " days ago. " + lastLogin);
                    });
                    BentoBox.getInstance().log("+-----------------------------------------+");
                });
            result.complete(oldIslands);
        });
        return result;
    }

    private boolean checkLastLoginTimestamp(int days, UUID member) {
        long daysInMilliseconds = days * 24L * 3600 * 1000; // Calculate days in milliseconds
        Long lastLoginTimestamp = getPlayers().getLastLoginTimestamp(member);
        // If no valid last login time is found or it's before the year 2000, try to fetch from Bukkit
        if (lastLoginTimestamp == null || lastLoginTimestamp < YEAR2000) {
            lastLoginTimestamp = Bukkit.getOfflinePlayer(member).getLastPlayed();

            // If still invalid, set the current timestamp to mark the user for eventual purging
            if (lastLoginTimestamp < YEAR2000) {
                getPlayers().setLoginTimeStamp(member, System.currentTimeMillis());
                return false; // User will be purged in the future
            } else {
                // Otherwise, update the last login timestamp with the valid value from Bukkit
                getPlayers().setLoginTimeStamp(member, lastLoginTimestamp);
            }
        }
        // Check if the difference between now and the last login is greater than the allowed days
        return System.currentTimeMillis() - lastLoginTimestamp > daysInMilliseconds;
    }


    /**
     * @return the inPurge
     */
    boolean isInPurge() {
        return inPurge;
    }

    /**
     * Stop the purge
     */
    void stop() {
        inPurge = false;
    }

    /**
     * @param user the user to set
     */
    void setUser(User user) {
        this.user = user;
    }

    /**
     * @param islands the islands to set
     */
    void setIslands(Set<String> islands) {
        this.islands = islands;
    }

    /**
     * Returns the amount of purged islands.
     * @return the amount of islands that have been purged.
     * @since 1.13.0
     */
    int getPurgedIslandsCount() {
        return this.count;
    }

    /**
     * Returns the amount of islands that can be purged.
     * @return the amount of islands that can be purged.
     * @since 1.13.0
     */
    int getPurgeableIslandsCount() {
        return this.islands.size();
    }
}
