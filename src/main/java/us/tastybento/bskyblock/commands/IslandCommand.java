package us.tastybento.bskyblock.commands;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.island.IslandAboutCommand;
import us.tastybento.bskyblock.commands.island.IslandCreateCommand;
import us.tastybento.bskyblock.commands.island.IslandGoCommand;
import us.tastybento.bskyblock.commands.island.IslandResetCommand;
import us.tastybento.bskyblock.commands.island.IslandSetnameCommand;
import us.tastybento.bskyblock.commands.island.IslandSethomeCommand;
import us.tastybento.bskyblock.commands.island.IslandResetnameCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandInviteAcceptCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandInviteRejectCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandLeaveCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamInviteCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamPromoteCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamUninviteCommand;
import us.tastybento.bskyblock.config.Settings;

public class IslandCommand extends CompositeCommand {

    public IslandCommand() {
        super(Settings.ISLANDCOMMAND, "is");
        this.setUsage("island.usage");
        this.setOnlyPlayer(true);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        // Permission
        this.setPermission(Settings.PERMPREFIX + "island");
        // Set up subcommands
        new IslandAboutCommand(this);
        new IslandCreateCommand(this);
        new IslandGoCommand(this);
        new IslandResetCommand(this);
        new IslandSetnameCommand(this);
        new IslandSethomeCommand(this);
        new IslandResetnameCommand(this);
        // Team commands
        new IslandTeamCommand(this);
        new IslandTeamInviteCommand(this);
        new IslandInviteAcceptCommand(this);
        new IslandInviteRejectCommand(this);
        new IslandLeaveCommand(this);
        new IslandTeamPromoteCommand(this);
        new IslandTeamUninviteCommand(this);
        
    }

    @Override
    public boolean execute(User user, List<String> args) {
        user.sendLegacyMessage("You successfully did /is !");
        if (!plugin.getIslands().hasIsland(user.getUniqueId())) {
            return this.getSubCommand("create").execute(user, args);
        }
        // Currently, just go home
        return this.getSubCommand("go").execute(user, args);
    }

}
