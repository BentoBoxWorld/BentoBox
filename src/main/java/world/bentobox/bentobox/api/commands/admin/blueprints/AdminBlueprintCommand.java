package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.panels.BlueprintManagementPanel;

public class AdminBlueprintCommand extends ConfirmableCommand {
    // Clipboards
    private Map<UUID, BlueprintClipboard> clipboards;

    // Map containing selection cuboid display tasks
    private Map<User, Integer> displayClipboards;
    private static final Particle PARTICLE = Particle.REDSTONE;
    private static final Particle.DustOptions PARTICLE_DUST_OPTIONS = new Particle.DustOptions(Color.RED, 1.0F);

    public AdminBlueprintCommand(CompositeCommand parent) {
        super(parent, "blueprint", "bp", "blu");
    }

    @Override
    public void setup() {
        setPermission("admin.blueprint");
        setParametersHelp("commands.admin.blueprint.parameters");
        setDescription("commands.admin.blueprint.description");
        setOnlyPlayer(true);

        clipboards = new HashMap<>();
        displayClipboards = new HashMap<>();

        new AdminBlueprintLoadCommand(this);
        new AdminBlueprintPasteCommand(this);
        new AdminBlueprintOriginCommand(this);
        new AdminBlueprintCopyCommand(this);
        new AdminBlueprintSaveCommand(this);
        new AdminBlueprintRenameCommand(this);
        new AdminBlueprintDeleteCommand(this);
        new AdminBlueprintPos1Command(this);
        new AdminBlueprintPos2Command(this);
        new AdminBlueprintListCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        new BlueprintManagementPanel(getPlugin(), user, getAddon()).openPanel();
        return true;
    }

    protected Map<UUID, BlueprintClipboard> getClipboards() {
        return clipboards;
    }

    protected void showClipboard(User user) {
        displayClipboards.putIfAbsent(user, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), () -> {
            if (!user.getPlayer().isOnline()) {
                hideClipboard(user);
            }

            if (clipboards.containsKey(user.getUniqueId())) {
                BlueprintClipboard clipboard = clipboards.get(user.getUniqueId());
                if (clipboard.getPos1() != null && clipboard.getPos2() != null) {
                    paintAxis(user, clipboard);
                }
            }

        }, 20, 20));
    }

    private void paintAxis(User user, BlueprintClipboard clipboard) {
        int minX = Math.min(clipboard.getPos1().getBlockX(), clipboard.getPos2().getBlockX());
        int minY = Math.min(clipboard.getPos1().getBlockY(), clipboard.getPos2().getBlockY());
        int minZ = Math.min(clipboard.getPos1().getBlockZ(), clipboard.getPos2().getBlockZ());
        int maxX = Math.max(clipboard.getPos1().getBlockX(), clipboard.getPos2().getBlockX());
        int maxY = Math.max(clipboard.getPos1().getBlockY(), clipboard.getPos2().getBlockY());
        int maxZ = Math.max(clipboard.getPos1().getBlockZ(), clipboard.getPos2().getBlockZ());

        // Drawing x-axes
        for (int x = minX; x <= maxX; x++) {
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, x, minY, minZ);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, x, maxY, minZ);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, x, minY, maxZ);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, x, maxY, maxZ);
        }

        // Drawing y-axes
        for (int y = minY; y <= maxY; y++) {
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, minX, y, minZ);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, maxX, y, minZ);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, minX, y, maxZ);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, maxX, y, maxZ);
        }

        // Drawing z-axes
        for (int z = minZ; z <= maxZ; z++) {
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, minX, minY, z);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, maxX, minY, z);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, minX, maxY, z);
            user.spawnParticle(PARTICLE, PARTICLE_DUST_OPTIONS, maxX, maxY, z);
        }

        // Drawing origin
        if (clipboard.getOrigin() != null) {
            user.spawnParticle(Particle.VILLAGER_HAPPY, null, clipboard.getOrigin().getBlockX() + 0.5, clipboard.getOrigin().getBlockY() + 0.5, clipboard.getOrigin().getBlockZ() + 0.5);
        }

    }

    protected void hideClipboard(User user) {
        if (displayClipboards.containsKey(user)) {
            Bukkit.getScheduler().cancelTask(displayClipboards.get(user));
            displayClipboards.remove(user);
        }
    }

    protected File getBlueprintsFolder() {
        return new File(getIWM().getDataFolder(getWorld()), BlueprintsManager.FOLDER_NAME);
    }
}
