package us.tastybento.bskyblock.commands.island.teams;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;

/**
 * A safe common space for team commands to share data
 * @author tastybento
 *
 */
public abstract class AbstractIslandTeamCommand extends CompositeCommand {

    protected final static boolean DEBUG = false;
    protected static BiMap<UUID, UUID> inviteList = HashBiMap.create();
    // The time a player has to wait until they can reset their island again
    protected static HashMap<UUID, Long> resetWaitTime = new HashMap<>();
    protected static Set<UUID> leavingPlayers = new HashSet<>();
    protected static Set<UUID> kickingPlayers = new HashSet<>();

    public AbstractIslandTeamCommand(CompositeCommand command, String label, String... aliases) {
        super(command, label,aliases);
    }

    /**
     * Sets a timeout for player into the Hashmap resetWaitTime
     *
     * @param player - the player
     */
    protected void setResetWaitTime(final Player player) {
        resetWaitTime.put(player.getUniqueId(), Calendar.getInstance().getTimeInMillis() + getSettings().getResetWait() * 1000);
    }
    
    protected int getMaxTeamSize(User user) {
        return getMaxPermSize(user, "team.maxsize.", getSettings().getMaxTeamSize());
    }
    
    protected int getMaxRangeSize(User user) {
        return getMaxPermSize(user, "island.range.", getSettings().getIslandProtectionRange());
    }
    
    /**
     * Get the max size based on permissions
     * @param playerUUID - the player's UUID
     * @return the max permission for this perm
     */
    private int getMaxPermSize(User user, String perm, int maxSize) {
        for (PermissionAttachmentInfo perms : user.getEffectivePermissions()) {
            if (perms.getPermission().startsWith(Constants.PERMPREFIX + perm)) {
                if (perms.getPermission().contains(Constants.PERMPREFIX + perm + "*")) {
                    maxSize = getSettings().getMaxTeamSize();
                    break;
                } else {
                    // Get the max value should there be more than one
                    String[] spl = perms.getPermission().split(Constants.PERMPREFIX + perm);
                    if (spl.length > 1) {
                        if (!NumberUtils.isDigits(spl[1])) {
                            getPlugin().getLogger().severe("Player " + user.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
                        } else {
                            maxSize = Math.max(maxSize, Integer.valueOf(spl[1]));
                        }
                    }
                }
            }
            // Do some sanity checking
            if (maxSize < 1) {
                maxSize = 1;
            }
        }
        return maxSize;
    }
}
