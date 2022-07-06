package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;


public class AdminBlueprintCopyCommand extends CompositeCommand
{
    public AdminBlueprintCopyCommand(AdminBlueprintCommand parent)
    {
        super(parent, "copy");
    }


    @Override
    public void setup()
    {
        inheritPermission();
        setParametersHelp("commands.admin.blueprint.copy.parameters");
        setDescription("commands.admin.blueprint.copy.description");
    }


    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        if (args.size() > 2)
        {
            this.showHelp(this, user);
            return false;
        }

        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();

        BlueprintClipboard clipboard =
            parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());

        boolean copyAir = args.stream().anyMatch(key -> key.equalsIgnoreCase("air"));
        boolean copyBiome = args.stream().anyMatch(key -> key.equalsIgnoreCase("biome"));

        return clipboard.copy(user, copyAir, copyBiome);
    }


    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args)
    {
        return Optional.of(List.of("air", "biome"));
    }
}
