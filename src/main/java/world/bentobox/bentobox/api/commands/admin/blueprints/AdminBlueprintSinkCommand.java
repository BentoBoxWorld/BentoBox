package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;


public class AdminBlueprintSinkCommand extends CompositeCommand
{
    public AdminBlueprintSinkCommand(AdminBlueprintCommand parent)
    {
        super(parent, "sink");
    }


    @Override
    public void setup()
    {
        setPermission("admin.blueprint.sink");
        setDescription("commands.admin.blueprint.sink.description");
    }


    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();
        if (args.isEmpty()) {
            BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(),
                    v -> new BlueprintClipboard());
            if (clipboard.isFull()) {
                // Clipboard loaded - toggle sink
                clipboard.getBlueprint().setSink(!clipboard.getBlueprint().isSink());
                user.sendMessage("commands.admin.blueprint.sink.status", "[status]",
                        clipboard.getBlueprint().isSink() ? user.getTranslation("commands.admin.blueprint.sink.sink")
                                : user.getTranslation("commands.admin.blueprint.sink.not-sink"));
                return true;
            } else {
                user.sendMessage("commands.admin.blueprint.sink.no-clipboard");
                return false;
            }
        } else {
            this.showHelp(this, user);
            return false;
        }
    }


    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args)
    {
        return Optional.of(List.of("air", "biome", "nowater", "sink"));
    }
}
