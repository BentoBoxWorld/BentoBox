package us.tastybento.bskyblock.commands.island.teams;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.Settings;

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
    
    // TODO: It would be good if these could be auto-provided
    protected User user;
    
    public AbstractIslandTeamCommand(CompositeCommand command, String label, String... aliases) {
        super(command, label,aliases);
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
