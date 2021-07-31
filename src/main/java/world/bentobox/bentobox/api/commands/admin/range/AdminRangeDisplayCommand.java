package world.bentobox.bentobox.api.commands.admin.range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * @author Poslovitch
 */
public class AdminRangeDisplayCommand extends CompositeCommand {

    // Command aliases
    private static final String DISPLAY = "display";
    private static final String SHOW = "show";
    private static final String HIDE = "hide";

    // Map of users to which ranges must be displayed
    private Map<User, Integer> displayRanges = new HashMap<>();

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

        if (!displayRanges.containsKey(user)) {
            switch (label) {
                case DISPLAY, SHOW -> showZones(user);
                case HIDE -> user.sendMessage("commands.admin.range.display.already-off");
                default -> showHelp(this, user);
            }
        } else {
            switch (label) {
                case DISPLAY, HIDE -> hideZones(user);
                case SHOW -> user.sendMessage("commands.admin.range.display.already-on");
                default -> showHelp(this, user);
            }
        }

        return true;
    }

    private void showZones(User user) {
        user.sendMessage("commands.admin.range.display.showing");
        user.sendMessage("commands.admin.range.display.hint");
        displayRanges.put(user, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), () -> {
            if (!user.getPlayer().isOnline()) {
                hideZones(user);
            }

            getIslands().getIslandAt(user.getLocation()).ifPresent(island -> {
                // Draw the island protected area
                drawZone(user, Particle.BARRIER, null, island, island.getProtectionRange());

                // Draw the default protected area if island protected zone is different
                if (island.getProtectionRange() != getPlugin().getIWM().getIslandProtectionRange(getWorld())) {
                    drawZone(user, Particle.VILLAGER_HAPPY, null, island, getPlugin().getIWM().getIslandProtectionRange(getWorld()));
                }

                // Draw the island area
                drawZone(user, Particle.REDSTONE, new Particle.DustOptions(Color.GRAY, 1.0F), island, island.getRange());
            });
        }, 20, 30));
    }

    private void hideZones(User user) {
        user.sendMessage("commands.admin.range.display.hiding");
        Bukkit.getScheduler().cancelTask(displayRanges.get(user));
        displayRanges.remove(user);
    }

    private void drawZone(User user, Particle particle, Particle.DustOptions dustOptions, Island island, int range) {
        Location center = island.getProtectionCenter();
        // Get player Y coordinate
        int playerY = user.getPlayer().getLocation().getBlockY() + 1;

        // Draw 3 "stages" (one line below, at and above player's y coordinate)
        for (int stage = -1 ; stage <= 1 ; stage++) {
            for (int i = -range ; i <= range ; i++) {
                user.spawnParticle(particle, dustOptions, center.getBlockX() + i, playerY + stage, center.getBlockZ() + range);
                user.spawnParticle(particle, dustOptions, center.getBlockX() + i, playerY + stage, center.getBlockZ() - range);
                user.spawnParticle(particle, dustOptions, center.getBlockX() + range, playerY + stage, center.getBlockZ() + i);
                user.spawnParticle(particle, dustOptions, center.getBlockX() - range, playerY + stage, center.getBlockZ() + i);
            }
        }
    }
}
