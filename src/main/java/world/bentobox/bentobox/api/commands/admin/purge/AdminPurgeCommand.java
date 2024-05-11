package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    private int count;
    private boolean inPurge;
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
            islands = getOldIslands(days);
            user.sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, String.valueOf(islands.size()));
            if (!islands.isEmpty()) {
                toBeConfirmed = true;
                user.sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, this.getTopLabel());
                return false;
            }
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
    Set<String> getOldIslands(int days) {
        long currentTimeMillis = System.currentTimeMillis();
        double daysInMilliseconds = days * 1000 * 3600 * 24;
        Set<String> oldIslands = new HashSet<>();

        // Process islands in one pass, logging and adding to the set if applicable
        getPlugin().getIslands().getIslands().stream()
                .filter(i -> !i.isSpawn()).filter(i -> !i.getPurgeProtected())
                .filter(i -> i.getWorld().equals(this.getWorld())).filter(Island::isOwned).filter(
                        i -> i.getMemberSet().stream()
                                .allMatch(member -> (currentTimeMillis
                                        - Bukkit.getOfflinePlayer(member).getLastPlayed()) > daysInMilliseconds))
                .forEach(i -> {
                    // Add the unique island ID to the set
                    oldIslands.add(i.getUniqueId());
                    BentoBox.getInstance().log("Will purge island at " + Util.xyz(i.getCenter().toVector()) + " in "
                            + i.getWorld().getName());
                    // Log each member's last login information
                    i.getMemberSet().forEach(member -> {
                        Date lastLogin = new Date(Bukkit.getOfflinePlayer(member).getLastPlayed());
                        BentoBox.getInstance()
                                .log("Player " + BentoBox.getInstance().getPlayers().getName(member)
                                        + " last logged in "
                                        + (int) ((currentTimeMillis - Bukkit.getOfflinePlayer(member).getLastPlayed())
                                                / 1000 / 3600 / 24)
                                        + " days ago. " + lastLogin);
                    });
                    BentoBox.getInstance().log("+-----------------------------------------+");
                });

        return oldIslands;
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
