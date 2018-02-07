package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.database.managers.island.NewIsland;
import us.tastybento.bskyblock.database.objects.Island;

public class IslandResetCommand extends CompositeCommand {

    private static final boolean DEBUG = false;

    public IslandResetCommand(CompositeCommand islandCommand) {
        super(islandCommand, "reset", "restart");
    }
    
    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "island.create");
        this.setOnlyPlayer(true);
        this.setDescription("commands.island.reset.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (!getIslands().hasIsland(user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return true;
        }
        if (!getIslands().isOwner(user.getUniqueId())) {
            user.sendMessage("general.errors.not-leader");
            return false; 
        }
        if (getPlugin().getPlayers().inTeam(user.getUniqueId())) {
            user.sendMessage("commands.island.reset.must-remove-members");
            return true;
        }
        Player player = user.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);
        // Get the player's old island
        Island oldIsland = getIslands().getIsland(player.getUniqueId());
        if (DEBUG)
            getPlugin().getLogger().info("DEBUG: old island is at " + oldIsland.getCenter().getBlockX() + "," + oldIsland.getCenter().getBlockZ());
        // Remove them from this island (it still exists and will be deleted later)
        getIslands().removePlayer(player.getUniqueId());
        if (DEBUG)
            getPlugin().getLogger().info("DEBUG: old island's owner is " + oldIsland.getOwner());
        // Create new island and then delete the old one
        if (DEBUG)
            getPlugin().getLogger().info("DEBUG: making new island ");
        try {
            NewIsland.builder(getPlugin())
            .player(player)
            .reason(Reason.RESET)
            .oldIsland(oldIsland)
            .build();
        } catch (IOException e) {
            getPlugin().getLogger().severe("Could not create island for player. " + e.getMessage());
            user.sendMessage("commands.island.create.unable-create-island");
        }
        return true;
    }

}
