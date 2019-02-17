package world.bentobox.bentobox.api.commands.admin.schem;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.schems.Clipboard;

public class AdminSchemOriginCommand extends CompositeCommand {

    public AdminSchemOriginCommand(AdminSchemCommand parent) {
        super(parent, "origin");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.schem.origin.parameters");
        setDescription("commands.admin.schem.origin.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        AdminSchemCommand parent = (AdminSchemCommand) getParent();

        Clipboard clipboard = parent.getClipboards().getOrDefault(user.getUniqueId(), new Clipboard(getPlugin(), parent.getSchemsFolder()));
        if (clipboard.getPos1() == null || clipboard.getPos2() == null) {
            user.sendMessage("commands.admin.schem.need-pos1-pos2");
            return false;
        }

        // Get the block player is looking at
        Block b = user.getPlayer().getLineOfSight(null, 20).stream().filter(x -> !x.getType().equals(Material.AIR)).findFirst().orElse(null);
        if (b != null) {
            clipboard.setOrigin(b.getLocation());
            user.getPlayer().sendBlockChange(b.getLocation(), Material.REDSTONE_BLOCK.createBlockData());
            Bukkit.getScheduler().runTaskLater(getPlugin(),
                    () -> user.getPlayer().sendBlockChange(b.getLocation(), b.getBlockData()), 20L);

            user.sendMessage("general.success");
            return true;
        }

        user.sendMessage("commands.admin.schem.look-at-a-block");
        return false;
    }
}
