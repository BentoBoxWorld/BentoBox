package world.bentobox.bentobox.commands.island;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.NewIsland;

public class IslandResetCommand extends ConfirmableCommand {

    public IslandResetCommand(CompositeCommand islandCommand) {
        super(islandCommand, "reset", "restart");
    }

    @Override
    public void setup() {
        setPermission("island.create");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.reset.parameters");
        setDescription("commands.island.reset.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Check cooldown
        if (getSettings().getResetCooldown() > 0 && checkCooldown(user, null)) {
            return false;
        }

        if (!getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().isOwner(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.not-owner");
            return false;
        }
        if (getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("commands.island.reset.must-remove-members");
            return false;
        }
        if (getIWM().getResetLimit(getWorld()) >= 0 ) {
            int resetsLeft = getIWM().getResetLimit(getWorld()) - getPlayers().getResets(getWorld(), user.getUniqueId());
            if (resetsLeft <= 0) {
                user.sendMessage("commands.island.reset.none-left");
                return false;
            } else {
                // Notify how many resets are left
                user.sendMessage("commands.island.reset.resets-left", TextVariables.NUMBER, String.valueOf(resetsLeft));
            }
        }
        // Default schem is 'island'

        String name = args.isEmpty() ? "island" : args.get(0).toLowerCase(java.util.Locale.ENGLISH);
        if (!args.isEmpty()) {
            // Permission check
            String permission = this.getPermissionPrefix() + "island.create." + name;
            if (!user.isOp() && !user.hasPermission(permission)) {
                user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, permission);
                return false;
            }
            // Check the schem name exists
            Set<String> validNames = getPlugin().getSchemsManager().get(getWorld()).keySet();
            if (!validNames.contains(name)) {
                user.sendMessage("commands.island.create.unknown-schem");
                return false;
            }

        }
        // Request confirmation
        if (getSettings().isResetConfirmation()) {
            this.askConfirmation(user, () -> resetIsland(user, name));
            return true;
        } else {
            return resetIsland(user, name);
        }

    }

    private boolean resetIsland(User user, String name) {
        // Reset the island
        Player player = user.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);
        // Get the player's old island
        Island oldIsland = getIslands().getIsland(getWorld(), player.getUniqueId());
        // Remove them from this island (it still exists and will be deleted later)
        getIslands().removePlayer(getWorld(), player.getUniqueId());
        // Remove money inventory etc.
        if (getIWM().isOnLeaveResetEnderChest(getWorld())) {
            user.getPlayer().getEnderChest().clear();
        }
        if (getIWM().isOnLeaveResetInventory(getWorld())) {
            user.getPlayer().getInventory().clear();
        }
        if (getSettings().isUseEconomy() && getIWM().isOnLeaveResetMoney(getWorld())) {
            getPlugin().getVault().ifPresent(vault -> vault.withdraw(user, vault.getBalance(user)));
        }
        // Add a reset
        getPlayers().addReset(getWorld(), user.getUniqueId());
        // Create new island and then delete the old one
        try {
            NewIsland.builder()
            .player(user)
            .reason(Reason.RESET)
            .oldIsland(oldIsland)
            .name(name)
            .build();
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage("commands.island.create.unable-create-island");
            return false;
        }
        setCooldown(user.getUniqueId(), null, getSettings().getResetCooldown());
        return true;
    }
}
