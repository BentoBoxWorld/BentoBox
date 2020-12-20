package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class IslandTeamKickCommand extends ConfirmableCommand {

  public IslandTeamKickCommand(CompositeCommand islandTeamCommand) {
    super(islandTeamCommand, "kick");
  }

  @Override
  public void setup() {
    setPermission("island.team.kick");
    setOnlyPlayer(true);
    setParametersHelp("commands.island.team.kick.parameters");
    setDescription("commands.island.team.kick.description");
    setConfigurableRankCommand();
  }

  @Override
  public boolean execute(User user, String label, List<String> args) {
    if (!getIslands().inTeam(getWorld(), user.getUniqueId())) {
      user.sendMessage("general.errors.no-team");
      return false;
    }
    if (!user.getUniqueId().equals(getOwner(getWorld(), user))) {
      user.sendMessage("general.errors.not-owner");
      return false;
    }
    // Check rank to use command
    Island island = getIslands().getIsland(getWorld(), user);
    int rank = Objects.requireNonNull(island).getRank(user);
    if (rank < island.getRankCommand(getUsage())) {
      user.sendMessage(
        "general.errors.insufficient-rank",
        TextVariables.RANK,
        user.getTranslation(getPlugin().getRanksManager().getRank(rank))
      );
      return false;
    }
    // If args are not right, show help
    if (args.size() != 1) {
      showHelp(this, user);
      return false;
    }
    // Get target
    UUID targetUUID = getPlayers().getUUID(args.get(0));
    if (targetUUID == null) {
      user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
      return false;
    }
    if (targetUUID.equals(user.getUniqueId())) {
      user.sendMessage("commands.island.team.kick.cannot-kick");
      return false;
    }
    if (!getIslands().getMembers(getWorld(), user.getUniqueId()).contains(targetUUID)) {
      user.sendMessage("general.errors.not-in-team");
      return false;
    }
    if (!getSettings().isKickConfirmation()) {
      kick(user, targetUUID);
      return true;
    } else {
      askConfirmation(user, () -> kick(user, targetUUID));
      return false;
    }
  }

  private void kick(User user, UUID targetUUID) {
    User target = User.getInstance(targetUUID);
    target.sendMessage(
      "commands.island.team.kick.owner-kicked",
      TextVariables.GAMEMODE,
      getAddon().getDescription().getName()
    );
    Island oldIsland = getIslands().getIsland(getWorld(), targetUUID);
    getIslands().removePlayer(getWorld(), targetUUID);
    // Execute commands when leaving
    Util.runCommands(target, getIWM().getOnLeaveCommands(oldIsland.getWorld()), "leave");
    // Remove money inventory etc.
    if (getIWM().isOnLeaveResetEnderChest(getWorld())) {
      if (target.isOnline()) {
        target.getPlayer().getEnderChest().clear();
      } else {
        getPlayers().getPlayer(targetUUID).addToPendingKick(getWorld());
        getPlayers().save(targetUUID);
      }
    }
    if (
      getIWM().isOnLeaveResetInventory(getWorld()) &&
      !getIWM().isKickedKeepInventory(getWorld())
    ) {
      if (target.isOnline()) {
        target.getPlayer().getInventory().clear();
      } else {
        getPlayers().getPlayer(targetUUID).addToPendingKick(getWorld());
        getPlayers().save(targetUUID);
      }
    }
    if (getSettings().isUseEconomy() && getIWM().isOnLeaveResetMoney(getWorld())) {
      getPlugin()
        .getVault()
        .ifPresent(vault -> vault.withdraw(target, vault.getBalance(target)));
    }
    // Reset the health
    if (getIWM().isOnLeaveResetHealth(getWorld())) {
      Util.resetHealth(target.getPlayer());
    }

    // Reset the hunger
    if (getIWM().isOnLeaveResetHunger(getWorld())) {
      target.getPlayer().setFoodLevel(20);
    }

    // Reset the XP
    if (getIWM().isOnLeaveResetXP(getWorld())) {
      target.getPlayer().setTotalExperience(0);
    }
    user.sendMessage(
      "commands.island.team.kick.success",
      TextVariables.NAME,
      target.getName()
    );
    // Fire event
    TeamEvent
      .builder()
      .island(oldIsland)
      .reason(TeamEvent.Reason.KICK)
      .involvedPlayer(targetUUID)
      .build();
    IslandEvent
      .builder()
      .island(oldIsland)
      .involvedPlayer(user.getUniqueId())
      .admin(false)
      .reason(IslandEvent.Reason.RANK_CHANGE)
      .rankChange(oldIsland.getRank(user), RanksManager.VISITOR_RANK)
      .build();

    // Add cooldown for this player and target
    if (getSettings().getInviteCooldown() > 0 && getParent() != null) {
      // Get the invite class from the parent
      getParent()
        .getSubCommand("invite")
        .ifPresent(
          c ->
            c.setCooldown(
              oldIsland.getUniqueId(),
              targetUUID.toString(),
              getSettings().getInviteCooldown() * 60
            )
        );
    }
  }

  @Override
  public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
    Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
    if (island != null) {
      List<String> options = island
        .getMemberSet()
        .stream()
        .filter(uuid -> island.getRank(uuid) >= RanksManager.MEMBER_RANK)
        .map(Bukkit::getOfflinePlayer)
        .map(OfflinePlayer::getName)
        .collect(Collectors.toList());

      String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";
      return Optional.of(Util.tabLimit(options, lastArg));
    } else {
      return Optional.empty();
    }
  }
}
