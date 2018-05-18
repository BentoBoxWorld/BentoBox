package us.tastybento.bskyblock.commands.admin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.island.builders.Clipboard;

public class AdminSchemCommand extends CompositeCommand {
    private Map<UUID, Clipboard> clipboards;
    private File schemFolder;

    public AdminSchemCommand(CompositeCommand parent) {
        super(parent, "schem");
    }

    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.schem");
        setParameters("commands.admin.schem.parameters");
        setDescription("commands.admin.schem.description");
        setOnlyPlayer(true);
        clipboards = new HashMap<>();
        schemFolder = new File(getPlugin().getDataFolder(), "schems");
        if (!schemFolder.exists()) {
            schemFolder.mkdirs();
        }
    }

    public boolean execute(User user, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        Clipboard cb = clipboards.getOrDefault(user.getUniqueId(), new Clipboard(getPlugin()));

        if (args.get(0).equalsIgnoreCase("paste")) {
            if (cb.isFull()) {
                cb.paste(user.getLocation());
                user.sendMessage("general.success");
                return true;
            } else {
                user.sendMessage("commands.admin.schem.copy-first");
                return true;
            }
        }

        if (args.get(0).equalsIgnoreCase("load")) {
            if (args.size() == 2) {
                File file = new File(schemFolder, args.get(1) + ".schem"); 
                if (file.exists()) {
                    try {
                        cb.load(file);
                        return true;
                    } catch (Exception e) {
                        user.sendMessage("commands.admin.schem.could-not-load");
                        e.printStackTrace();
                        return false;
                    }  
                } else {
                    user.sendMessage("commands.admin.schem.no-such-file");
                    return false;
                }
            } else {
                showHelp(this, user);
                return false;
            }
        }

        if (args.get(0).equalsIgnoreCase("copy")) {
            return cb.copy(user);
        }

        if (args.get(0).equalsIgnoreCase("save")) {
            if (cb.isFull()) {
                if (args.size() == 2) {
                    File file = new File(schemFolder, args.get(1)); 
                    user.sendMessage("general.success");
                    cb.save(file);
                    return true;
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
            cb.setPos1(user.getLocation());
            user.sendMessage("commands.admin.schem.set-pos1", "[vector]", user.getLocation().toVector().toString());
            clipboards.put(user.getUniqueId(), cb);
            return true;
        }

        if (args.get(0).equalsIgnoreCase("pos2")) {
            cb.setPos2(user.getLocation());
            user.sendMessage("commands.admin.schem.set-pos2", "[vector]", user.getLocation().toVector().toString());
            clipboards.put(user.getUniqueId(), cb);
            return true;
        }

        return false;
    }

}
