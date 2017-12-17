package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.managers.island.NewIsland;
import us.tastybento.bskyblock.database.objects.Island;

public class IslandResetCommand extends CommandArgument {

    private static final boolean DEBUG = false;

    public IslandResetCommand() {
        super("reset", "restart");
    }

    @Override
    public boolean execute(User user, String[] args) {
        if (!isPlayer(user)) {
            user.sendMessage("general.errors.use-in-game");
            return true;
        }
        Player player = (Player)user;
        if (!player.hasPermission(Settings.PERMPREFIX + "island.reset")) {
            user.sendMessage("general.errors.no-permission");
            return true;
        }
        if (!getIslands().hasIsland(player.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return true;
        }
        if (!getIslands().isOwner(player.getUniqueId())) {
            return false; 
        }
        if (inTeam(player)) {
            user.sendMessage("island.reset.MustRemovePlayers");
            return true;
        }

        player.setGameMode(GameMode.SPECTATOR);
        // Get the player's old island
        Island oldIsland = getIslands().getIsland(player.getUniqueId());
        if (DEBUG)
            plugin.getLogger().info("DEBUG: old island is at " + oldIsland.getCenter().getBlockX() + "," + oldIsland.getCenter().getBlockZ());
        // Remove them from this island (it still exists and will be deleted later)
        getIslands().removePlayer(player.getUniqueId());
        if (DEBUG)
            plugin.getLogger().info("DEBUG: old island's owner is " + oldIsland.getOwner());
        // Create new island and then delete the old one
        if (DEBUG)
            plugin.getLogger().info("DEBUG: making new island ");
        try {
            NewIsland.builder()
            .player(player)
            .reason(Reason.RESET)
            .oldIsland(oldIsland)
            .build();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create island for player.");
            user.sendMessage(ChatColor.RED + "general.errors.general");
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }
}
