package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final String SECONDS_PLACEHOLDER = "[seconds]";
    private Map<UUID, Long> cooldown;
    private Map<UUID, Long> confirm;

    public IslandResetCommand(CompositeCommand islandCommand) {
        super(islandCommand, "reset", "restart");
    }

    @Override
    public void setup() {
        cooldown = new HashMap<>();
        confirm = new HashMap<>();
        setPermission(Constants.PERMPREFIX + "island.create");
        setOnlyPlayer(true);
        setDescription("commands.island.reset.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // Check cooldown
        if (getSettings().getResetWait() > 0 && onRestartWaitTime(user) > 0 && !user.isOp()) {
            user.sendMessage("general.errors.you-must-wait", SECONDS_PLACEHOLDER, String.valueOf(onRestartWaitTime(user)));
            return false;
        }
        if (!getIslands().hasIsland(user.getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().isOwner(user.getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        if (getIslands().inTeam(user.getWorld(), user.getUniqueId())) {
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
        // Check for non-confirm command
        if (!args.isEmpty() && !(confirm.containsKey(user.getUniqueId()) && args.get(0).equalsIgnoreCase("confirm"))) {
            showHelp(this, user);
            return false;
        }

        // Check confirmation or reset immediately if no confirmation required
        if (!getSettings().isResetConfirmation() || (confirm.containsKey(user.getUniqueId()) && args.size() == 1 && args.get(0).equalsIgnoreCase("confirm"))) {
            return resetIsland(user);
        }
        
        // Confirmation required        
        if (!confirm.containsKey(user.getUniqueId())) {
            requestConfirmation(user);
        } else {
            // Show how many seconds left to confirm
            int time = (int)((confirm.get(user.getUniqueId()) - System.currentTimeMillis()) / 1000D);
            user.sendMessage("commands.island.reset.confirm", "[label]", Constants.ISLANDCOMMAND, SECONDS_PLACEHOLDER, String.valueOf(time));
        }
        return true;
    }

    private void requestConfirmation(User user) {
        user.sendMessage("commands.island.reset.confirm", "[label]", Constants.ISLANDCOMMAND, SECONDS_PLACEHOLDER, String.valueOf(getSettings().getConfirmationTime()));
        // Require confirmation          
        confirm.put(user.getUniqueId(), System.currentTimeMillis() + getSettings().getConfirmationTime() * 1000L);
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (confirm.containsKey(user.getUniqueId())) {
                user.sendMessage("commands.island.reset.cancelled");
                confirm.remove(user.getUniqueId());
            }
        }, getSettings().getConfirmationTime() * 20L);       
    }

    private boolean resetIsland(User user) {
        // Remove the confirmation
        confirm.remove(user.getUniqueId());
        // Reset the island
        Player player = user.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);
        // Get the player's old island
        Island oldIsland = getIslands().getIsland(user.getWorld(), player.getUniqueId());
        // Remove them from this island (it still exists and will be deleted later)
        getIslands().removePlayer(user.getWorld(), player.getUniqueId());
        // Create new island and then delete the old one
        try {
            NewIsland.builder()
            .player(user)
            .reason(Reason.RESET)
            .oldIsland(oldIsland)
            .build();
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage("commands.island.create.unable-create-island");
            return false;
        }
        setCooldown(user);
        return true;
    }

    private int onRestartWaitTime(User user) {
        if (!cooldown.containsKey(user.getUniqueId())) {
            return 0;
        }
        return (int) (System.currentTimeMillis() - cooldown.get(user.getUniqueId()) / 1000);
    }

    private void setCooldown(User user) {
        cooldown.put(user.getUniqueId(), System.currentTimeMillis() + (getSettings().getResetLimit() * 1000L));
    }
}
