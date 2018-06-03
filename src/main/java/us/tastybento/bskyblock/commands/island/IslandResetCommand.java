package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.api.localization.TextVariables;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.island.NewIsland;

public class IslandResetCommand extends CompositeCommand {

    private Map<UUID, Long> cooldown;
    private Map<UUID, Long> confirm;

    public IslandResetCommand(CompositeCommand islandCommand) {
        super(islandCommand, "reset", "restart");
    }

    @Override
    public void setup() {
        cooldown = new HashMap<>();
        confirm = new HashMap<>();
        setPermission("island.create");
        setOnlyPlayer(true);
        setDescription("commands.island.reset.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // Check cooldown
        if (getSettings().getResetWait() > 0 && onRestartWaitTime(user) > 0 && !user.isOp()) {
            user.sendMessage("general.errors.you-must-wait", TextVariables.NUMBER, String.valueOf(onRestartWaitTime(user)));
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().isOwner(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        if (getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("commands.island.reset.must-remove-members");
            return false;
        }
        if (getSettings().getResetLimit() >= 0 ) {
            if (getPlayers().getResetsLeft(user.getUniqueId()) == 0) {
                user.sendMessage("commands.island.reset.none-left");
                return false;
            } else {
                // Notify how many resets are left
                user.sendMessage("commands.island.reset.resets-left", TextVariables.NUMBER, String.valueOf(getPlayers().getResetsLeft(user.getUniqueId())));
            }
        }
        // Request confirmation
        if (getSettings().isResetConfirmation()) {
            this.askConfirmation(user, () -> resetIsland(user));
            return true;
        } else {
            return resetIsland(user);
        }

    }

    private boolean resetIsland(User user) {
        // Remove the confirmation
        confirm.remove(user.getUniqueId());
        // Reset the island
        Player player = user.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);
        // Get the player's old island
        Island oldIsland = getIslands().getIsland(getWorld(), player.getUniqueId());
        // Remove them from this island (it still exists and will be deleted later)
        getIslands().removePlayer(getWorld(), player.getUniqueId());
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
