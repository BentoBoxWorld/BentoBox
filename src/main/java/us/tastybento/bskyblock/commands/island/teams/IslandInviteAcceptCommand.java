package us.tastybento.bskyblock.commands.island.teams;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;

public class IslandInviteAcceptCommand extends AbstractIslandTeamCommandArgument {

    public IslandInviteAcceptCommand() {
        super("accept");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Check team perm and get variables set
        if (!checkTeamPerm()) return true;
        if(!inviteList.containsKey(playerUUID))
                return true;
        // Check if player has been invited
        if (!inviteList.containsKey(playerUUID)) {
            player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.NoOneInvitedYou"));
            return true;
        }
        // Check if player is already in a team
        if (getPlayers().inTeam(playerUUID)) {
            player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.YouAreAlreadyOnATeam"));
            return true;
        }
        // Get the team leader
        UUID prospectiveTeamLeaderUUID = inviteList.get(playerUUID);
        if (!getIslands().hasIsland(prospectiveTeamLeaderUUID)) {
            player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.InvalidInvite"));
            inviteList.remove(playerUUID);
            return true;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Invite is valid");
        // Fire event so add-ons can run commands, etc.
        TeamEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(prospectiveTeamLeaderUUID))
                .reason(TeamReason.JOIN)
                .involvedPlayer(playerUUID)
                .build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;
        // Remove the invite
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Removing player from invite list");
        inviteList.remove(playerUUID);
        // Put player into Spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        // Get the player's island - may be null if the player has no island
        Island island = getIslands().getIsland(playerUUID);
        // Get the team's island
        Island teamIsland = getIslands().getIsland(prospectiveTeamLeaderUUID);
        // Clear the player's inventory
        player.getInventory().clear();
        // Move player to team's island
        Location newHome = getIslands().getSafeHomeLocation(prospectiveTeamLeaderUUID, 1);
        player.teleport(newHome);
        // Remove player as owner of the old island
        getIslands().removePlayer(playerUUID);
        // Add the player as a team member of the new island
        getIslands().setJoinTeam(teamIsland, playerUUID);
        // Set the player's home
        getPlayers().setHomeLocation(playerUUID, player.getLocation());
        // Delete the old island
        getIslands().deleteIsland(island, true);
        // Set the cooldown
        setResetWaitTime(player);
        // Reset deaths
        if (Settings.teamJoinDeathReset) {
            getPlayers().setDeaths(playerUUID, 0);
        }
        // Put player back into normal mode
        player.setGameMode(GameMode.SURVIVAL);

        player.sendMessage(ChatColor.GREEN + getLocale(sender).get("invite.youHaveJoinedAnIsland").replace("[label]", Settings.ISLANDCOMMAND));

        if (plugin.getServer().getPlayer(inviteList.get(playerUUID)) != null) {
            plugin.getServer().getPlayer(inviteList.get(playerUUID)).sendMessage(
                    ChatColor.GREEN + getLocale(sender).get("invite.hasJoinedYourIsland").replace("[name]", player.getName()));
        }
        getIslands().save(false);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: After save " + getIslands().getIsland(prospectiveTeamLeaderUUID).getMembers().toString());
        return true;
    }


    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
