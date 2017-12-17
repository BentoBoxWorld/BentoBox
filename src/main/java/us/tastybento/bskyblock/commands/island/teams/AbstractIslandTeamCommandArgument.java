package us.tastybento.bskyblock.commands.island.teams;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public abstract class AbstractIslandTeamCommandArgument extends CommandArgument {
    
    protected final static boolean DEBUG = false;
    protected BiMap<UUID, UUID> inviteList = HashBiMap.create(); 
    // The time a player has to wait until they can reset their island again
    protected HashMap<UUID, Long> resetWaitTime = new HashMap<>();
    protected Set<UUID> leavingPlayers = new HashSet<>();
    protected Set<UUID> kickingPlayers = new HashSet<>();
    
    // TODO: It would be good if these could be auto-provided
    protected User user;
    
    public AbstractIslandTeamCommandArgument(String label, String... aliases) {
        super(label,aliases);
    }

    protected boolean checkTeamPerm() {
        if (!isPlayer(user)) {
            user.sendMessage("general.errors.use-in-game");
            return false;
        }
        if (!user.hasPermission(Settings.PERMPREFIX + "team")) {
            user.sendMessage(ChatColor.RED + "general.errors.no-permission");
            return false;
        }
        return true;
    }
    
    /**
     * Sets a timeout for player into the Hashmap resetWaitTime
     *
     * @param player
     */
    protected void setResetWaitTime(final Player player) {
        resetWaitTime.put(player.getUniqueId(), Calendar.getInstance().getTimeInMillis() + Settings.resetWait * 1000);
    }
}
