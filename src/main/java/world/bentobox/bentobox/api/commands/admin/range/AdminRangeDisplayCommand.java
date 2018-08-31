package world.bentobox.bentobox.api.commands.admin.range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
public class AdminRangeDisplayCommand extends CompositeCommand {

    // Command aliases
    private static final String DISPLAY = "display";
    private static final String SHOW = "show";
    private static final String HIDE = "hide";

    // Map of users to which ranges must be displayed
    private Map<User, Integer> display = new HashMap<>();

    public AdminRangeDisplayCommand(CompositeCommand parent) {
        super(parent, DISPLAY, SHOW, HIDE);
    }

    @Override
    public void setup() {
        setPermission("admin.range.display");
        setDescription("commands.admin.range.display.description");
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // According to the label used to execute the command, there is a different behaviour
        // - display : toggle on/off
        // - show : only set on - and send "error" if already on
        // - hide : only set off - same if already off

        if (!display.containsKey(user)) {
            switch (label) {
            case DISPLAY:
            case SHOW:
                showZones(user);
                break;
            case HIDE:
                user.sendMessage("commands.admin.range.display.already-off");
                break;
            default:
                showHelp(this, user);
                break;
            }
        } else {
            switch (label) {
            case DISPLAY:
            case HIDE:
                hideZones(user);
                break;
            case SHOW:
                user.sendMessage("commands.admin.range.display.already-on");
                break;
            default:
                showHelp(this, user);
                break;
            }
        }

        return true;
    }

    private void showZones(User user) {
        user.sendMessage("commands.admin.range.display.showing");
        user.sendMessage("commands.admin.range.display.hint");
        display.put(user, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), () -> {
            if (!user.getPlayer().isOnline()) {
                hideZones(user);
            }

            getIslands().getIslandAt(user.getLocation()).ifPresent(island -> {
                // Draw the island protected area
                drawZone(user.getPlayer(), Particle.BARRIER, null, island.getCenter(), island.getProtectionRange());

                // Draw the default protected area if island protected zone is different
                if (island.getProtectionRange() != getPlugin().getIWM().getIslandProtectionRange(getWorld())) {
                    drawZone(user.getPlayer(), Particle.VILLAGER_HAPPY, null, island.getCenter(), getPlugin().getIWM().getIslandProtectionRange(getWorld()));
                }

                // Draw the island area
                drawZone(user.getPlayer(), Particle.REDSTONE, new Particle.DustOptions(Color.GRAY, 1.0F),island.getCenter(), island.getRange());
            });
        }, 20, 30));
    }

    private void hideZones(User user) {
        user.sendMessage("commands.admin.range.display.hiding");
        Bukkit.getScheduler().cancelTask(display.get(user));
        display.remove(user);
    }

    private void drawZone(Player player, Particle particle, Particle.DustOptions dustOptions, Location center, int range) {
        if (particle.equals(Particle.REDSTONE) && dustOptions == null) {
            // Security check that will avoid later unexpected exceptions.
            throw new IllegalArgumentException("A non-null Particle.DustOptions must be provided when using Particle.REDSTONE.");
        }

        // Get player Y coordinate
        int playerY = player.getLocation().getBlockY() + 1;

        // Draw 3 "stages" (one line below, at and above player's y coordinate)
        for (int stage = -1 ; stage <= 1 ; stage++) {
            for (int i = -range ; i <= range ; i++) {
                spawnParticle(player, particle, dustOptions, center.getBlockX() + i, playerY + stage, center.getBlockZ() + range);
                spawnParticle(player, particle, dustOptions, center.getBlockX() + i, playerY + stage, center.getBlockZ() - range);
                spawnParticle(player, particle, dustOptions, center.getBlockX() + range, playerY + stage, center.getBlockZ() + i);
                spawnParticle(player, particle, dustOptions, center.getBlockX() - range, playerY + stage, center.getBlockZ() + i);
            }
        }
    }

    private void spawnParticle(Player player, Particle particle, Particle.DustOptions dustOptions, int x, int y, int z) {
        // Check if this particle is beyond the viewing distance of the server
        if (player.getLocation().toVector().distanceSquared(new Vector(x,y,z)) < (Bukkit.getServer().getViewDistance()*256*Bukkit.getServer().getViewDistance())) {
            if (particle.equals(Particle.REDSTONE)) {
                player.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 1, dustOptions);
            } else {
                player.spawnParticle(particle, x, y, z, 1);
            }
        }
    }
}
