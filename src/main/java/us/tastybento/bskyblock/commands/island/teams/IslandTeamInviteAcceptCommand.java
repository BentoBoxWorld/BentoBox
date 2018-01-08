package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.database.objects.Island;

public class IslandTeamInviteAcceptCommand extends AbstractIslandTeamCommand {

    public IslandTeamInviteAcceptCommand(IslandTeamInviteCommand islandTeamInviteCommand) {
        super(islandTeamInviteCommand, "accept");
    }
    
    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setDescription("commands.island.team.invite.accept.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        
        Bukkit.getLogger().info("DEBUG: accept - " + inviteList.toString());
        
        UUID playerUUID = user.getUniqueId();
        if(!inviteList.containsKey(playerUUID))
                return true;
        // Check if player has been invited
        if (!inviteList.containsKey(playerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.none-invited-you");
            return true;
        }
        // Check if player is already in a team
        if (getPlayers().inTeam(playerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
            return true;
        }
        // Get the team leader
        UUID prospectiveTeamLeaderUUID = inviteList.get(playerUUID);
        if (!getIslands().hasIsland(prospectiveTeamLeaderUUID)) {
            user.sendMessage("commands.island.team.invite.errors.invalid-invite");
            inviteList.remove(playerUUID);
            return true;
        }
        if (DEBUG)
            getPlugin().getLogger().info("DEBUG: Invite is valid");
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(prospectiveTeamLeaderUUID))
                .reason(TeamEvent.Reason.JOIN)
                .involvedPlayer(playerUUID)
                .build();
        getPlugin().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;
        // Remove the invite
        if (DEBUG)
            getPlugin().getLogger().info("DEBUG: Removing player from invite list");
        inviteList.remove(playerUUID);
        // Put player into Spectator mode
        user.setGameMode(GameMode.SPECTATOR);
        // Get the player's island - may be null if the player has no island
        Island island = getIslands().getIsland(playerUUID);
        // Get the team's island
        Island teamIsland = getIslands().getIsland(prospectiveTeamLeaderUUID);
        // Clear the player's inventory
        user.getInventory().clear();
        // Move player to team's island
        Location newHome = getIslands().getSafeHomeLocation(prospectiveTeamLeaderUUID, 1);
        user.teleport(newHome);
        // Remove player as owner of the old island
        getIslands().removePlayer(playerUUID);
        // Add the player as a team member of the new island
        getIslands().setJoinTeam(teamIsland, playerUUID);
        // Set the player's home
        getPlayers().setHomeLocation(playerUUID, user.getLocation());
        // Delete the old island
        getIslands().deleteIsland(island, true);
        // Set the cooldown
        setResetWaitTime(user.getPlayer());
        // Reset deaths
        if (getSettings().isTeamJoinDeathReset()) {
            getPlayers().setDeaths(playerUUID, 0);
        }
        // Put player back into normal mode
        user.setGameMode(GameMode.SURVIVAL);

        user.sendMessage("commands.island.team.invite.accept.you-joined-island", "[label]", Constants.ISLANDCOMMAND);
        User inviter = User.getInstance(inviteList.get(playerUUID));
        if (inviter != null) {
            inviter.sendMessage("commands.island.team.invite.accept.name-joined-your-island", "[name]", user.getName());
        }
        getIslands().save(false);
        if (DEBUG)
            getPlugin().getLogger().info("DEBUG: After save " + getIslands().getIsland(prospectiveTeamLeaderUUID).getMembers().toString());
        return true;
    }

}
