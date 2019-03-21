package world.bentobox.bentobox.api.commands.admin.schem;

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
import world.bentobox.bentobox.managers.SchemsManager;
import world.bentobox.bentobox.schems.Clipboard;

public class AdminSchemCommand extends ConfirmableCommand {
    // Clipboards
    private Map<UUID, Clipboard> clipboards;

    // Map containing selection cuboid display tasks
    private Map<User, Integer> displayClipboards;
    private static final Particle PARTICLE = Particle.REDSTONE;
    private static final Particle.DustOptions PARTICLE_DUST_OPTIONS = new Particle.DustOptions(Color.RED, 1.0F);

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
        displayClipboards = new HashMap<>();

        new AdminSchemLoadCommand(this);
        new AdminSchemPasteCommand(this);
        new AdminSchemOriginCommand(this);
        new AdminSchemCopyCommand(this);
        new AdminSchemSaveCommand(this);
        new AdminSchemPos1Command(this);
        new AdminSchemPos2Command(this);
        new AdminSchemListCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }

    protected Map<UUID, Clipboard> getClipboards() {
        return clipboards;
    }

    protected void showClipboard(User user) {
        displayClipboards.putIfAbsent(user, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), () -> {
            if (!user.getPlayer().isOnline()) {
                hideClipboard(user);
            }

            if (clipboards.containsKey(user.getUniqueId())) {
                Clipboard clipboard = clipboards.get(user.getUniqueId());
                if (clipboard.getPos1() != null && clipboard.getPos2() != null) {
                    paintAxis(user, clipboard);
                }
            }

        }, 20, 20));
    }

    private void paintAxis(User user, Clipboard clipboard) {
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

    protected File getSchemsFolder() {
        return new File(getIWM().getDataFolder(getWorld()), SchemsManager.FOLDER_NAME);
    }
}
