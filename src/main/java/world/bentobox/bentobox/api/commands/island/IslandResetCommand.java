package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.List;

import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.panels.IslandCreationPanel;

/**
 * @author tastybento
 */
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
    public boolean canExecute(User user, String label, List<String> args) {
        // Check cooldown
        if (getSettings().getResetCooldown() > 0 && checkCooldown(user)) {
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
        int resetsLeft = getPlayers().getResetsLeft(getWorld(), user.getUniqueId());
        if (resetsLeft != -1) {
            // Resets are not unlimited here
            if (resetsLeft == 0) {
                // No resets allowed
                user.sendMessage("commands.island.reset.none-left");
                return false;
            } else {
                // Still some resets left
                // Notify how many resets are left
                user.sendMessage("commands.island.reset.resets-left", TextVariables.NUMBER, String.valueOf(resetsLeft));
            }
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Permission check if the name is not the default one
        if (!args.isEmpty()) {
            String name = getPlugin().getBlueprintsManager().validate((GameModeAddon)getAddon(), args.get(0).toLowerCase(java.util.Locale.ENGLISH));
            if (name == null || name.isEmpty()) {
                // The blueprint name is not valid.
                user.sendMessage("commands.island.create.unknown-blueprint");
                return false;
            }
            if (!getPlugin().getBlueprintsManager().checkPerm(getAddon(), user, args.get(0))) {
                return false;
            }
            return resetIsland(user, name);
        } else {
            // Show panel after confirmation
            if (getPlugin().getSettings().isResetConfirmation()) {
                this.askConfirmation(user, () -> IslandCreationPanel.openPanel(this, user, label));
            } else {
                IslandCreationPanel.openPanel(this, user, label);
            }
            return true;
        }
    }


    private boolean resetIsland(User user, String name) {
        // Reset the island
        Player player = user.getPlayer();
        user.sendMessage("commands.island.create.creating-island");
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
            .addon((GameModeAddon)getAddon())
            .oldIsland(oldIsland)
            .name(name)
            .build();
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage("commands.island.create.unable-create-island");
            return false;
        }
        setCooldown(user.getUniqueId(), getSettings().getResetCooldown());
        return true;
    }
}
