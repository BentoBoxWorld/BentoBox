package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;

public class AdminBlueprintOriginCommand extends CompositeCommand {

    public AdminBlueprintOriginCommand(AdminBlueprintCommand parent) {
        super(parent, "origin");
    }

    @Override
    public void setup() {
        inheritPermission();
        setParametersHelp("commands.admin.blueprint.origin.parameters");
        setDescription("commands.admin.blueprint.origin.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();

        BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());
        if (clipboard.getPos1() == null || clipboard.getPos2() == null) {
            user.sendMessage("commands.admin.blueprint.need-pos1-pos2");
            return false;
        }

        // Get the block player is looking at
        Block b = user.getPlayer().getLineOfSight(null, 20).stream().filter(x -> !x.getType().equals(Material.AIR)).findFirst().orElse(null);
        if (b != null) {
            clipboard.setOrigin(b.getLocation().toVector());
            user.getPlayer().sendBlockChange(b.getLocation(), Material.REDSTONE_BLOCK.createBlockData());
            Bukkit.getScheduler().runTaskLater(getPlugin(),
                    () -> user.getPlayer().sendBlockChange(b.getLocation(), b.getBlockData()), 20L);

            user.sendMessage("general.success");
            return true;
        }

        user.sendMessage("commands.admin.blueprint.look-at-a-block");
        return false;
    }
}
