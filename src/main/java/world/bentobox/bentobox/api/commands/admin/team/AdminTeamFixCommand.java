package world.bentobox.bentobox.api.commands.admin.team;

import java.util.List;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

public class AdminTeamFixCommand extends CompositeCommand {

  public AdminTeamFixCommand(CompositeCommand parent) {
    super(parent, "fix");
  }

  @Override
  public void setup() {
    setPermission("mod.team");
    setDescription("commands.admin.team.fix.description");
  }

  @Override
  public boolean canExecute(User user, String label, List<String> args) {
    // If args are not right, show help
    if (!args.isEmpty()) {
      showHelp(this, user);
      return false;
    }
    return true;
  }

  @Override
  public boolean execute(User user, String label, List<String> args) {
    getIslands().checkTeams(user, getWorld());
    return true;
  }
}
