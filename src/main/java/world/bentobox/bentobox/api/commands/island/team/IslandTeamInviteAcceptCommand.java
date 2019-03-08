package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;

import com.google.common.collect.BiMap;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * @author tastybento
 */
public class IslandTeamInviteAcceptCommand extends ConfirmableCommand {

    private IslandTeamCommand itc;
    private UUID playerUUID;
    private UUID prospectiveOwnerUUID;

    public IslandTeamInviteAcceptCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "accept");
        this.itc = islandTeamCommand;
    }

    @Override
    public void setup() {
        setPermission("island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.accept.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        playerUUID = user.getUniqueId();
        // Check if player has been invited
        BiMap<UUID, UUID> inviteList = itc.getInviteCommand().getInviteList();
        if (!inviteList.containsKey(playerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.none-invited-you");
            return false;
        }
        // Check if player is already in a team
        if (getIslands().inTeam(getWorld(), playerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
            return false;
        }
        // Get the island owner
        prospectiveOwnerUUID = inviteList.get(playerUUID);
        if (!getIslands().hasIsland(getWorld(), prospectiveOwnerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.invalid-invite");
            inviteList.remove(playerUUID);
            return false;
        }
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands().getIsland(getWorld(), prospectiveOwnerUUID))
                .reason(TeamEvent.Reason.JOIN)
                .involvedPlayer(playerUUID)
                .build();
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        askConfirmation(user, user.getTranslation("commands.island.team.invite.accept.confirmation"), () -> {
            // Remove the invite
            itc.getInviteCommand().getInviteList().remove(playerUUID);
            // Put player into Spectator mode
            user.setGameMode(GameMode.SPECTATOR);
            // Get the player's island - may be null if the player has no island
            Island island = getIslands().getIsland(getWorld(), playerUUID);
            // Get the team's island
            Island teamIsland = getIslands().getIsland(getWorld(), prospectiveOwnerUUID);
            // Move player to team's island
            User prospectiveOwner = User.getInstance(prospectiveOwnerUUID);
            Location newHome = getIslands().getSafeHomeLocation(getWorld(), prospectiveOwner, 1);
            user.teleport(newHome);
            // Remove player as owner of the old island
            getIslands().removePlayer(getWorld(), playerUUID);
            // Remove money inventory etc. for leaving
            cleanPlayer(user);
            // Add the player as a team member of the new island
            getIslands().setJoinTeam(teamIsland, playerUUID);
            // Set the player's home
            getPlayers().setHomeLocation(playerUUID, user.getLocation());
            // Delete the old island
            if (island != null) {
                getIslands().deleteIsland(island, true);
            }
            // TODO Set the cooldown
            // Reset deaths
            if (getIWM().isTeamJoinDeathReset(getWorld())) {
                getPlayers().setDeaths(getWorld(), playerUUID, 0);
            }
            // Put player back into normal mode
            user.setGameMode(getIWM().getDefaultGameMode(getWorld()));

            user.sendMessage("commands.island.team.invite.accept.you-joined-island", TextVariables.LABEL, getTopLabel());
            User inviter = User.getInstance(itc.getInviteCommand().getInviteList().get(playerUUID));
            if (inviter != null) {
                inviter.sendMessage("commands.island.team.invite.accept.name-joined-your-island", TextVariables.NAME, user.getName());
            }
            getIslands().save(teamIsland);
            // Fire event
            IslandBaseEvent e = TeamEvent.builder()
                    .island(getIslands().getIsland(getWorld(), prospectiveOwnerUUID))
                    .reason(TeamEvent.Reason.JOINED)
                    .involvedPlayer(playerUUID)
                    .build();
            Bukkit.getServer().getPluginManager().callEvent(e);
        });

        return true;
    }

    private void cleanPlayer(User user) {
        if (getIWM().isOnLeaveResetEnderChest(getWorld()) || getIWM().isOnJoinResetEnderChest(getWorld())) {
            user.getPlayer().getEnderChest().clear();
        }
        if (getIWM().isOnLeaveResetInventory(getWorld()) || getIWM().isOnJoinResetInventory(getWorld())) {
            user.getPlayer().getInventory().clear();
        }
        if (getSettings().isUseEconomy() && (getIWM().isOnLeaveResetMoney(getWorld()) || getIWM().isOnJoinResetMoney(getWorld()))) {
            getPlugin().getVault().ifPresent(vault -> vault.withdraw(user, vault.getBalance(user)));
        }
    }

}
