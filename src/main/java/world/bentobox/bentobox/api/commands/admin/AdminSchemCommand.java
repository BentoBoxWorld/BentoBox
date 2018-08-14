package world.bentobox.bentobox.api.commands.admin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.schems.Clipboard;
import world.bentobox.bentobox.util.Util;

public class AdminSchemCommand extends ConfirmableCommand {
    private Map<UUID, Clipboard> clipboards;

    public AdminSchemCommand(CompositeCommand parent) {
        super(parent, "schem");
    }

    @Override
    public void setup() {
        setPermission("admin.schem");
        setParametersHelp("commands.admin.schem.parameters");
        setDescription("commands.admin.schem.description");
        setOnlyPlayer(true);
        clipboards = new HashMap<>();
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        File schemFolder = new File(getIWM().getDataFolder(getWorld()), "schems");
        Clipboard cb = clipboards.getOrDefault(user.getUniqueId(), new Clipboard(getPlugin(), schemFolder));

        if (args.get(0).equalsIgnoreCase("paste")) {
            if (cb.isFull()) {
                cb.pasteClipboard(user.getLocation());
                user.sendMessage("general.success");
                return true;
            } else {
                user.sendMessage("commands.admin.schem.copy-first");
                return false;
            }
        }

        if (args.get(0).equalsIgnoreCase("load")) {
            if (args.size() == 2) {
                if (cb.load(user, args.get(1))) {
                    clipboards.put(user.getUniqueId(), cb);
                    return true;
                }
            } else {
                showHelp(this, user);
                return false;
            }
            return false;
        }

        if (args.get(0).equalsIgnoreCase("origin")) {
            if (cb.getPos1() == null || cb.getPos2() == null) {
                user.sendMessage("commands.admin.schem.need-pos1-pos2");
                return false;
            }
            // Get the block player is looking at
            Block b = user.getPlayer().getLineOfSight(null, 20).stream().filter(x -> !x.getType().equals(Material.AIR)).findFirst().orElse(null);
            if (b != null) {
                cb.setOrigin(b.getLocation());
                user.getPlayer().sendBlockChange(b.getLocation(), Material.REDSTONE_BLOCK.createBlockData());
                Bukkit.getScheduler().runTaskLater(getPlugin(),
                        () -> user.getPlayer().sendBlockChange(b.getLocation(), b.getBlockData()), 20L);

                user.sendMessage("general.success");
                return true;
            } else {
                user.sendMessage("commands.admin.schem.look-at-a-block");
                return false;
            }
        }

        if (args.get(0).equalsIgnoreCase("copy")) {
            boolean copyAir = (args.size() == 2 && args.get(1).equalsIgnoreCase("air"));
            return cb.copy(user, copyAir);
        }

        if (args.get(0).equalsIgnoreCase("save")) {
            if (cb.isFull()) {
                if (args.size() == 2) {
                    // Check if file exists
                    File newFile = new File(schemFolder, args.get(1) + ".schem");
                    if (newFile.exists()) {
                        user.sendMessage("commands.admin.schem.file-exists");
                        this.askConfirmation(user, () -> cb.save(user, args.get(1)));
                        return false;
                    } else {
                        return cb.save(user, args.get(1));
                    }
                } else {
                    showHelp(this, user);
                    return false;
                }
            } else {
                user.sendMessage("commands.admin.schem.copy-first");
                return false;
            }
        }

        if (args.get(0).equalsIgnoreCase("pos1")) {
            if (user.getLocation().equals(cb.getPos2())) {
                user.sendMessage("commands.admin.schem.set-different-pos");
                return false;
            }
            cb.setPos1(user.getLocation());
            user.sendMessage("commands.admin.schem.set-pos1", "[vector]", Util.xyz(user.getLocation().toVector()));
            clipboards.put(user.getUniqueId(), cb);
            return true;
        }

        if (args.get(0).equalsIgnoreCase("pos2")) {
            if (user.getLocation().equals(cb.getPos1())) {
                user.sendMessage("commands.admin.schem.set-different-pos");
                return false;
            }
            cb.setPos2(user.getLocation());
            user.sendMessage("commands.admin.schem.set-pos2", "[vector]", Util.xyz(user.getLocation().toVector()));
            clipboards.put(user.getUniqueId(), cb);
            return true;
        }

        return false;
    }

}
