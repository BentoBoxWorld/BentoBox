package world.bentobox.bentobox.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 * @since 1.17.3
 */
public class IslandInfo {

    private final BentoBox plugin;
    private final Island island;
    private final @Nullable UUID owner;
    private final World world;


    /**
     * Get island Info
     * @param island Island to show info
     */
    public IslandInfo(Island island) {
        this.plugin = BentoBox.getInstance();
        this.island = island;
        this.owner = island.getOwner();
        this.world = island.getWorld();
    }

    /**
     * Shows admin info of this island
     * @param user user asking
     */
    public void showAdminInfo(User user) {
        user.sendMessage("commands.admin.info.title");
        user.sendMessage("commands.admin.info.island-uuid", "[uuid]", island.getUniqueId());
        if (owner == null) {
            user.sendMessage("commands.admin.info.unowned");
        } else {
            user.sendMessage("commands.admin.info.owner", "[owner]", plugin.getPlayers().getName(owner), "[uuid]", owner.toString());

            // Fixes #getLastPlayed() returning 0 when it is the owner's first connection.
            long lastPlayed = (Bukkit.getOfflinePlayer(owner).getLastPlayed() != 0) ?
                    Bukkit.getOfflinePlayer(owner).getLastPlayed() : Bukkit.getOfflinePlayer(owner).getFirstPlayed();
            String formattedDate;
            try {
                String dateTimeFormat = plugin.getLocalesManager().get("commands.admin.info.last-login-date-time-format");
                formattedDate = new SimpleDateFormat(dateTimeFormat).format(new Date(lastPlayed));
            } catch (NullPointerException | IllegalArgumentException ignored) {
                formattedDate = new Date(lastPlayed).toString();
            }
            user.sendMessage("commands.admin.info.last-login","[date]", formattedDate);

            user.sendMessage("commands.admin.info.deaths", "[number]", String.valueOf(plugin.getPlayers().getDeaths(world, owner)));
            String resets = String.valueOf(plugin.getPlayers().getResets(world, owner));
            String total = plugin.getIWM().getResetLimit(world) < 0 ? "Unlimited" : String.valueOf(plugin.getIWM().getResetLimit(world));
            user.sendMessage("commands.admin.info.resets-left", "[number]", resets, "[total]", total);
            // Show team members
            showMembers(user);
        }
        Vector location = island.getProtectionCenter().toVector();
        user.sendMessage("commands.admin.info.island-protection-center", TextVariables.XYZ, Util.xyz(location));
        user.sendMessage("commands.admin.info.island-center", TextVariables.XYZ, Util.xyz(island.getCenter().toVector()));
        user.sendMessage("commands.admin.info.island-coords", "[xz1]", Util.xyz(new Vector(island.getMinX(), 0, island.getMinZ())), "[xz2]", Util.xyz(new Vector(island.getMaxX(), 0, island.getMaxZ())));
        user.sendMessage("commands.admin.info.protection-range", "[range]", String.valueOf(island.getProtectionRange()));
        user.sendMessage("commands.admin.info.max-protection-range", "[range]", String.valueOf(island.getMaxEverProtectionRange()));
        user.sendMessage("commands.admin.info.protection-coords", "[xz1]", Util.xyz(new Vector(island.getMinProtectedX(), 0, island.getMinProtectedZ())), "[xz2]", Util.xyz(new Vector(island.getMaxProtectedX(), 0, island.getMaxProtectedZ())));
        if (island.isSpawn()) {
            user.sendMessage("commands.admin.info.is-spawn");
        }
        if (!island.getBanned().isEmpty()) {
            user.sendMessage("commands.admin.info.banned-players");
            island.getBanned().forEach(u -> user.sendMessage("commands.admin.info.banned-format", TextVariables.NAME, plugin.getPlayers().getName(u)));
        }
        if (island.getPurgeProtected()) {
            user.sendMessage("commands.admin.info.purge-protected");
        }
    }


    /**
     * Shows info of this island to this user.
     * @param user the User who is requesting it
     * @return always true
     */
    public boolean showInfo(User user) {
        user.sendMessage("commands.admin.info.title");
        if (owner == null) {
            user.sendMessage("commands.admin.info.unowned");
        } else {
            user.sendMessage("commands.admin.info.owner", "[owner]", plugin.getPlayers().getName(owner), "[uuid]", owner.toString());
            user.sendMessage("commands.admin.info.deaths", "[number]", String.valueOf(plugin.getPlayers().getDeaths(world, owner)));
            String resets = String.valueOf(plugin.getPlayers().getResets(world, owner));
            String total = plugin.getIWM().getResetLimit(world) < 0 ? "Unlimited" : String.valueOf(plugin.getIWM().getResetLimit(world));
            user.sendMessage("commands.admin.info.resets-left", "[number]", resets, "[total]", total);
            // Show team members
            showMembers(user);
        }
        Vector location = island.getProtectionCenter().toVector();
        user.sendMessage("commands.admin.info.island-center", TextVariables.XYZ, Util.xyz(location));
        user.sendMessage("commands.admin.info.protection-range", "[range]", String.valueOf(island.getProtectionRange()));
        user.sendMessage("commands.admin.info.protection-coords", "[xz1]", Util.xyz(new Vector(island.getMinProtectedX(), 0, island.getMinProtectedZ())), "[xz2]", Util.xyz(new Vector(island.getMaxProtectedX(), 0, island.getMaxProtectedZ())));
        if (island.isSpawn()) {
            user.sendMessage("commands.admin.info.is-spawn");
        }
        if (!island.getBanned().isEmpty()) {
            user.sendMessage("commands.admin.info.banned-players");
            island.getBanned().forEach(u -> user.sendMessage("commands.admin.info.banned-format", TextVariables.NAME, plugin.getPlayers().getName(u)));
        }
        return true;
    }

    /**
     * Shows the members of this island to this user.
     * @param user the User who is requesting it
     */
    public void showMembers(User user) {
        user.sendMessage("commands.admin.info.team-members-title");
        island.getMembers().forEach((u, i) -> {
            if (owner.equals(u)) {
                user.sendMessage("commands.admin.info.team-owner-format", TextVariables.NAME, plugin.getPlayers().getName(u)
                        , "[rank]", user.getTranslation(plugin.getRanksManager().getRank(i)));
            } else if (i > RanksManager.VISITOR_RANK){
                user.sendMessage("commands.admin.info.team-member-format", TextVariables.NAME, plugin.getPlayers().getName(u)
                        , "[rank]", user.getTranslation(plugin.getRanksManager().getRank(i)));
            }
        });
    }
}
