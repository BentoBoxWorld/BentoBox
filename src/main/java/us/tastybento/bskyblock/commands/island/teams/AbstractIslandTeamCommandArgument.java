package us.tastybento.bskyblock.commands.island;

import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import us.tastybento.bskyblock.api.commands.CommandArgument;

public abstract class AbstractIslandTeamCommandArgument extends CommandArgument {
    
    protected BiMap<UUID, UUID> inviteList = HashBiMap.create(); 
    
    public AbstractIslandTeamCommandArgument(String label, String... aliases) {
        super(label,aliases);
    }

}
