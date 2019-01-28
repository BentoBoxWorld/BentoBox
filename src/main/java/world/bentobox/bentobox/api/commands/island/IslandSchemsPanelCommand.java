package world.bentobox.bentobox.api.commands.island;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.SchemsPanel;

import java.util.List;

/**
 * @author barpec12
 */
public class IslandSchemsPanelCommand extends CompositeCommand {



    private IslandCreateCommand islandCreateCommand;

    /**
     * Command to open a schems menu
     * @param islandCommand - parent command
     * @param islandCreateCommand - command to create an island
     */
    public IslandSchemsPanelCommand(CompositeCommand islandCommand, IslandCreateCommand islandCreateCommand) {
        super(islandCommand, "schems", "schemspanel");
        this.islandCreateCommand = islandCreateCommand;
    }

    @Override
    public void setup() {
        setPermission("island.schemspanel");
        setOnlyPlayer(true);
        setDescription("commands.island.schemspanel.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        SchemsPanel.openPanel(user, getWorld(), islandCreateCommand);
        return true;
    }
}
