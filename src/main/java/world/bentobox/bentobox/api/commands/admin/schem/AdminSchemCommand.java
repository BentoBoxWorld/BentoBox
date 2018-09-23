package world.bentobox.bentobox.api.commands.admin.schem;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.schems.Clipboard;

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

        new AdminSchemLoadCommand(this);
        new AdminSchemPasteCommand(this);
        new AdminSchemOriginCommand(this);
        new AdminSchemCopyCommand(this);
        new AdminSchemSaveCommand(this);
        new AdminSchemPos1Command(this);
        new AdminSchemPos2Command(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }

    Map<UUID, Clipboard> getClipboards() {
        return clipboards;
    }

    File getSchemsFolder() {
        return new File(getIWM().getDataFolder(getWorld()), "schems");
    }
}
