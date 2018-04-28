package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.island.NewIsland;

public class IslandResetCommand extends CompositeCommand {

    private Map<UUID, Long> cooldown;
    private Set<UUID> confirm;

    public IslandResetCommand(CompositeCommand islandCommand) {
        super(islandCommand, "reset", "restart");
    }

    @Override
    public void setup() {
        cooldown = new HashMap<>();
        confirm = new HashSet<>();
        setPermission(Constants.PERMPREFIX + "island.create");
        setOnlyPlayer(true);
        setDescription("commands.island.reset.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // Check cooldown
        if (getSettings().getResetWait() > 0 && onRestartWaitTime(user) > 0 && !user.isOp()) {
            user.sendMessage("general.errors.you-must-wait", "[seconds]", String.valueOf(onRestartWaitTime(user)));
            return false;
        }
        if (!getIslands().hasIsland(user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().isOwner(user.getUniqueId())) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        if (getPlayers().inTeam(user.getUniqueId())) {
            user.sendMessage("commands.island.reset.must-remove-members");
            return false;
        }
        if (getSettings().getResetLimit() >= 0 ) {
            if (getPlayers().getResetsLeft(user.getUniqueId()) == 0) {
                user.sendMessage("commands.island.reset.none-left");
                return false;
            } else {
                // Notify how many resets are left
                user.sendMessage("commands.island.reset.resets-left", "[number]", String.valueOf(getPlayers().getResetsLeft(user.getUniqueId()))); 
            }
        }
        // Check confirmation or reset immediately if no confirmation required
        if (!getSettings().isResetConfirmation() || (confirm.contains(user.getUniqueId()) && args.size() == 1 && args.get(0).equalsIgnoreCase("confirm"))) {
            // Reset the island
            Player player = user.getPlayer();
            player.setGameMode(GameMode.SPECTATOR);
            // Get the player's old island
            Island oldIsland = getIslands().getIsland(player.getUniqueId());
            // Remove them from this island (it still exists and will be deleted later)
            getIslands().removePlayer(player.getUniqueId());
            // Create new island and then delete the old one
            try {
                NewIsland.builder()
                .player(player)
                .reason(Reason.RESET)
                .oldIsland(oldIsland)
                .build();
            } catch (IOException e) {
                getPlugin().logError("Could not create island for player. " + e.getMessage());
                user.sendMessage("commands.island.create.unable-create-island");
            }
            setCooldown(user);
            return true;
        } else {
            // Require confirmation
            user.sendMessage("commands.island.reset.confirm", "[label]", Constants.ISLANDCOMMAND, "[seconds]", String.valueOf(getSettings().getConfirmationTime()));
            confirm.add(user.getUniqueId());
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> confirm.remove(user.getUniqueId()), getSettings().getConfirmationTime() * 20L);
            return true;
        }
    }

    private int onRestartWaitTime(User user) {
        if (!cooldown.containsKey(user.getUniqueId())) {
            return 0;
        }
        return (int) ((System.currentTimeMillis() - cooldown.get(user.getUniqueId()) / 1000));
    }

    private void setCooldown(User user) {
        cooldown.put(user.getUniqueId(), System.currentTimeMillis() + (getSettings().getResetLimit() * 1000L));
    }
}
